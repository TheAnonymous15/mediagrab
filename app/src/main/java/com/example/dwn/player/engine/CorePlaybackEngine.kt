package com.example.dwn.player.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * ============================================
 * CORE PLAYBACK ENGINE
 * ============================================
 *
 * Handles basic media playback operations:
 * - Play, Pause, Stop, Seek
 * - Playlist management
 * - Audio focus handling
 * - Playback state management
 */
class CorePlaybackEngine(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val handler = Handler(Looper.getMainLooper())
    internal val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // State flows
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentMedia = MutableStateFlow<MediaItem?>(null)
    val currentMedia: StateFlow<MediaItem?> = _currentMedia.asStateFlow()

    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
    val playlist: StateFlow<List<MediaItem>> = _playlist.asStateFlow()

    private var currentIndex = -1
    private var progressRunnable: Runnable? = null

    // Listeners
    private val listeners = mutableListOf<PlayerListener>()

    // Callback for modules that need to hook into playback
    var onPositionUpdate: ((Long) -> Unit)? = null
    var onPrepared: ((Int) -> Unit)? = null // Passes audio session ID
    var onCompletion: (() -> Unit)? = null

    // ============================================
    // PLAYBACK CONTROL API
    // ============================================

    fun play(media: MediaItem) {
        engineScope.launch {
            try {
                prepareMedia(media)
                _currentMedia.value = media
                listeners.forEach { it.onMediaChanged(media) }
            } catch (e: Exception) {
                notifyError(PlayerError.PLAYBACK_ERROR, e.message ?: "Unknown error")
            }
        }
    }

    fun playUrl(url: String, title: String = "Stream", artist: String = "") {
        val media = MediaItem(
            id = url.hashCode().toString(),
            uri = Uri.parse(url),
            title = title,
            artist = artist,
            type = MediaType.STREAM
        )
        play(media)
    }

    fun playFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            val media = MediaItem(
                id = filePath.hashCode().toString(),
                uri = Uri.fromFile(file),
                title = file.nameWithoutExtension,
                type = if (filePath.endsWith(".mp4") || filePath.endsWith(".mkv"))
                    MediaType.VIDEO else MediaType.AUDIO
            )
            play(media)
        } else {
            notifyError(PlayerError.FILE_NOT_FOUND, "File not found: $filePath")
        }
    }

    fun playPlaylist(items: List<MediaItem>, startIndex: Int = 0) {
        _playlist.value = items
        currentIndex = startIndex
        if (items.isNotEmpty() && startIndex in items.indices) {
            play(items[startIndex])
        }
        listeners.forEach { it.onPlaylistChanged(items) }
    }

    fun resume() {
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                if (requestAudioFocus()) {
                    player.start()
                    updateState(isPlaying = true)
                    startProgressUpdates()
                }
            }
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                updateState(isPlaying = false)
                stopProgressUpdates()
            }
        }
    }

    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) pause() else resume()
    }

    fun stop() {
        mediaPlayer?.stop()
        releasePlayer()
        updateState(isPlaying = false, position = 0)
        stopProgressUpdates()
        abandonAudioFocus()
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        updateState(position = positionMs)
    }

    fun seekToPercent(percent: Float) {
        val duration = _playbackState.value.duration
        if (duration > 0) {
            seekTo((duration * percent.coerceIn(0f, 1f)).toLong())
        }
    }

    fun skipForward(ms: Long = 10000) {
        val newPos = (_playbackState.value.position + ms).coerceAtMost(_playbackState.value.duration)
        seekTo(newPos)
    }

    fun skipBackward(ms: Long = 10000) {
        val newPos = (_playbackState.value.position - ms).coerceAtLeast(0)
        seekTo(newPos)
    }

    fun next() {
        val items = _playlist.value
        if (items.isNotEmpty() && currentIndex < items.size - 1) {
            currentIndex++
            play(items[currentIndex])
        } else if (_playbackState.value.repeatMode == RepeatMode.ALL && items.isNotEmpty()) {
            currentIndex = 0
            play(items[0])
        }
    }

    fun previous() {
        if (_playbackState.value.position > 3000) {
            seekTo(0)
            return
        }

        val items = _playlist.value
        if (items.isNotEmpty() && currentIndex > 0) {
            currentIndex--
            play(items[currentIndex])
        } else if (_playbackState.value.repeatMode == RepeatMode.ALL && items.isNotEmpty()) {
            currentIndex = items.size - 1
            play(items[currentIndex])
        }
    }

    fun setSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(speed) ?: return
            updateState(speed = speed)
        }
    }

    fun setVolume(volume: Float) {
        val vol = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(vol, vol)
        updateState(volume = vol)
    }

    fun setRepeatMode(mode: RepeatMode) {
        updateState(repeatMode = mode)
        mediaPlayer?.isLooping = mode == RepeatMode.ONE
    }

    fun toggleShuffle() {
        val currentShuffle = _playbackState.value.shuffleEnabled
        updateState(shuffleEnabled = !currentShuffle)
        if (!currentShuffle) {
            shufflePlaylist()
        }
    }

    fun isPlaying(): Boolean = _playbackState.value.isPlaying

    fun getAudioSessionId(): Int = mediaPlayer?.audioSessionId ?: 0

    // ============================================
    // INTERNAL METHODS
    // ============================================

    private fun prepareMedia(media: MediaItem) {
        releasePlayer()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            setOnPreparedListener { mp ->
                updateState(
                    duration = mp.duration.toLong(),
                    isPrepared = true
                )
                onPrepared?.invoke(mp.audioSessionId)

                if (requestAudioFocus()) {
                    mp.start()
                    updateState(isPlaying = true)
                    startProgressUpdates()
                }
            }

            setOnCompletionListener {
                handleCompletion()
            }

            setOnErrorListener { _, what, extra ->
                notifyError(PlayerError.PLAYBACK_ERROR, "Error: $what, $extra")
                true
            }

            setOnBufferingUpdateListener { _, percent ->
                updateState(bufferPercent = percent)
            }

            when {
                media.uri.scheme?.startsWith("http") == true -> {
                    setDataSource(media.uri.toString())
                }
                media.uri.scheme == "content" -> {
                    setDataSource(context, media.uri)
                }
                else -> {
                    setDataSource(media.uri.path ?: "")
                }
            }

            prepareAsync()
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        updateState(isPrepared = false)
    }

    private fun handleCompletion() {
        onCompletion?.invoke()

        when (_playbackState.value.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                resume()
            }
            RepeatMode.ALL -> {
                next()
            }
            RepeatMode.OFF -> {
                if (currentIndex < _playlist.value.size - 1) {
                    next()
                } else {
                    updateState(isPlaying = false, position = 0)
                }
            }
        }
    }

    private fun shufflePlaylist() {
        val current = _currentMedia.value
        val shuffled = _playlist.value.shuffled()
        _playlist.value = shuffled
        currentIndex = shuffled.indexOfFirst { it.id == current?.id }
        listeners.forEach { it.onPlaylistChanged(shuffled) }
    }

    // ============================================
    // PROGRESS UPDATES
    // ============================================

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val position = player.currentPosition.toLong()
                        updateState(position = position)
                        onPositionUpdate?.invoke(position)
                    }
                }
                handler.postDelayed(this, 100)
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun stopProgressUpdates() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
    }

    // ============================================
    // AUDIO FOCUS
    // ============================================

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { handleAudioFocusChange(it) }
                .build()

            audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { handleAudioFocusChange(it) },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaPlayer?.setVolume(0.3f, 0.3f)
            AudioManager.AUDIOFOCUS_GAIN -> mediaPlayer?.setVolume(1f, 1f)
        }
    }

    // ============================================
    // STATE MANAGEMENT
    // ============================================

    internal fun updateState(
        isPlaying: Boolean = _playbackState.value.isPlaying,
        isPrepared: Boolean = _playbackState.value.isPrepared,
        position: Long = _playbackState.value.position,
        duration: Long = _playbackState.value.duration,
        bufferPercent: Int = _playbackState.value.bufferPercent,
        speed: Float = _playbackState.value.speed,
        volume: Float = _playbackState.value.volume,
        repeatMode: RepeatMode = _playbackState.value.repeatMode,
        shuffleEnabled: Boolean = _playbackState.value.shuffleEnabled
    ) {
        _playbackState.value = PlaybackState(
            isPlaying = isPlaying,
            isPrepared = isPrepared,
            position = position,
            duration = duration,
            bufferPercent = bufferPercent,
            speed = speed,
            volume = volume,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleEnabled
        )
        listeners.forEach { it.onPlaybackStateChanged(_playbackState.value) }
    }

    internal fun notifyError(code: Int, message: String) {
        listeners.forEach { it.onError(PlayerError(code, message)) }
    }

    // ============================================
    // LISTENER MANAGEMENT
    // ============================================

    fun addListener(listener: PlayerListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: PlayerListener) {
        listeners.remove(listener)
    }

    internal fun notifyListeners(action: (PlayerListener) -> Unit) {
        listeners.forEach(action)
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        stop()
        engineScope.cancel()
    }
}

