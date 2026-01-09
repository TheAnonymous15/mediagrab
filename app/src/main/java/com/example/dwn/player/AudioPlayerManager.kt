package com.example.dwn.player

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.example.dwn.data.SettingsManager
import com.example.dwn.player.audio.SuperEqualizer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

private const val TAG = "AudioPlayerManager"

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentFileName: String = "",
    val currentFileId: String? = null,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffleEnabled: Boolean = false,
    val queue: List<QueueItem> = emptyList(),
    val currentQueueIndex: Int = -1,
    val audioSessionId: Int = 0,
    val currentAudioDevice: String = "Phone Speaker",
    val isBluetoothConnected: Boolean = false
)

data class QueueItem(
    val id: String,
    val fileName: String,
    val isVideo: Boolean = false
)

data class AudioDeviceState(
    val name: String,
    val type: Int,
    val isConnected: Boolean
)

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var notificationManager: MediaNotificationManager? = null
    private var mediaActionReceiver: MediaActionReceiver? = null
    private var bluetoothReceiver: BroadcastReceiver? = null

    // URI storage for device media queue playback
    private val uriMap = mutableMapOf<String, android.net.Uri>()

    // Legacy Equalizer (for backward compatibility)
    private var _equalizerManager: EqualizerManager? = null
    val equalizerManager: EqualizerManager
        get() {
            if (_equalizerManager == null) {
                _equalizerManager = EqualizerManager(context)
            }
            return _equalizerManager!!
        }

    // Super Equalizer (advanced audio processing)
    private var _superEqualizer: SuperEqualizer? = null
    val superEqualizer: SuperEqualizer
        get() {
            if (_superEqualizer == null) {
                _superEqualizer = SuperEqualizer(context)
            }
            return _superEqualizer!!
        }

    // Audio Focus
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    // Volume detection for auto pause/resume
    private var volumeContentObserver: ContentObserver? = null
    private var wasPlayingBeforeVolumeZero = false
    private var lastKnownVolume = -1

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState: StateFlow<AudioPlayerState> = _playerState

    private val _connectedDevices = MutableStateFlow<List<AudioDeviceState>>(emptyList())
    val connectedDevices: StateFlow<List<AudioDeviceState>> = _connectedDevices

    private var updateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Shuffle indices for randomized playback
    private var shuffledIndices: List<Int> = emptyList()

    // Audio focus change listener
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss - stop playback
                Log.d(TAG, "Audio focus lost permanently")
                pause()
                hasAudioFocus = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss - pause
                Log.d(TAG, "Audio focus lost temporarily")
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Can duck - lower volume (we'll just pause for simplicity)
                Log.d(TAG, "Audio focus loss - can duck")
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Gained focus - resume if was playing
                Log.d(TAG, "Audio focus gained")
                hasAudioFocus = true
                mediaPlayer?.setVolume(1f, 1f)
                if (_playerState.value.isPaused) {
                    resume()
                }
            }
        }
    }

    init {
        setupNotification()
        setupAudioFocus()
        setupBluetoothReceiver()
        setupVolumeObserver()
        updateConnectedDevices()
    }

    // Volume observer to detect when media volume becomes zero or non-zero
    private fun setupVolumeObserver() {
        lastKnownVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        volumeContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

                // Only react if volume actually changed
                if (currentVolume != lastKnownVolume) {
                    Log.d(TAG, "Media volume changed: $lastKnownVolume -> $currentVolume")

                    // Volume went to zero - pause if playing
                    if (currentVolume == 0 && lastKnownVolume > 0) {
                        if (_playerState.value.isPlaying) {
                            Log.d(TAG, "Volume zero - auto pausing playback")
                            wasPlayingBeforeVolumeZero = true
                            pause()
                        }
                    }
                    // Volume increased from zero - resume if was playing before
                    else if (currentVolume > 0 && lastKnownVolume == 0) {
                        if (wasPlayingBeforeVolumeZero && !_playerState.value.isPlaying) {
                            Log.d(TAG, "Volume restored - auto resuming playback")
                            wasPlayingBeforeVolumeZero = false
                            resume()
                        }
                    }

                    lastKnownVolume = currentVolume
                }
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                volumeContentObserver!!
            )
            Log.d(TAG, "Volume observer registered")
        } catch (e: Exception) {
            Log.w(TAG, "Could not register volume observer: ${e.message}")
        }
    }

    private fun setupBluetoothReceiver() {
        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        Log.d(TAG, "Bluetooth device connected: ${device?.name}")
                        updateConnectedDevices()
                        _playerState.value = _playerState.value.copy(
                            isBluetoothConnected = true,
                            currentAudioDevice = device?.name ?: "Bluetooth Device"
                        )
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        Log.d(TAG, "Bluetooth device disconnected")
                        updateConnectedDevices()
                        // Pause playback when Bluetooth disconnects
                        if (_playerState.value.isPlaying) {
                            pause()
                        }
                        _playerState.value = _playerState.value.copy(
                            isBluetoothConnected = false,
                            currentAudioDevice = "Phone Speaker"
                        )
                    }
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                        // Headphones unplugged - pause playback
                        Log.d(TAG, "Audio becoming noisy - pausing")
                        if (_playerState.value.isPlaying) {
                            pause()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(bluetoothReceiver, filter)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not register Bluetooth receiver: ${e.message}")
        }
    }

    private fun updateConnectedDevices() {
        val devices = mutableListOf<AudioDeviceState>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in audioDevices) {
                val deviceName = when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth (A2DP)"
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth (SCO)"
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
                    AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
                    AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone Speaker"
                    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
                    else -> device.productName?.toString() ?: "Unknown Device"
                }
                devices.add(AudioDeviceState(deviceName, device.type, true))
            }
        }

        _connectedDevices.value = devices

        // Update current device in player state
        val bluetoothDevice = devices.find {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
        val wiredDevice = devices.find {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }

        val currentDevice = bluetoothDevice?.name ?: wiredDevice?.name ?: "Phone Speaker"
        _playerState.value = _playerState.value.copy(
            currentAudioDevice = currentDevice,
            isBluetoothConnected = bluetoothDevice != null
        )
    }

    private fun setupAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
        }
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.requestAudioFocus(it)
            } ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        Log.d(TAG, "Audio focus request result: $hasAudioFocus")
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
    }

    private fun setupNotification() {
        notificationManager = MediaNotificationManager(context)
        mediaActionReceiver = MediaActionReceiver()

        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(MediaNotificationManager.ACTION_PLAY)
            addAction(MediaNotificationManager.ACTION_PAUSE)
            addAction(MediaNotificationManager.ACTION_STOP)
            addAction(MediaNotificationManager.ACTION_PREVIOUS)
            addAction(MediaNotificationManager.ACTION_NEXT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(mediaActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(mediaActionReceiver, filter)
        }

        // Set callbacks
        MediaActionReceiver.onPlayCallback = { resume() }
        MediaActionReceiver.onPauseCallback = { pause() }
        MediaActionReceiver.onStopCallback = { stop() }
        MediaActionReceiver.onPreviousCallback = { seekBackward() }
        MediaActionReceiver.onNextCallback = { seekForward() }
    }

    fun play(fileId: String, fileName: String, isVideo: Boolean = false, startPosition: Long = 0L) {
        try {
            // If same file, toggle play/pause
            if (_playerState.value.currentFileId == fileId && mediaPlayer != null) {
                if (_playerState.value.isPlaying) {
                    pause()
                } else {
                    resume()
                }
                return
            }

            // Stop previous playback
            stop()

            // Request audio focus - this will pause other apps
            if (!requestAudioFocus()) {
                Log.w(TAG, "Could not gain audio focus")
                // Continue anyway but log warning
            }

            // Determine directory based on file type
            val baseDir = if (isVideo) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            }
            val file = File(baseDir, fileName)

            if (!file.exists()) {
                Log.e(TAG, "File not found: ${file.absolutePath}")
                return
            }

            mediaPlayer = MediaPlayer().apply {
                // Set audio attributes for better Bluetooth support
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                setDataSource(file.absolutePath)
                prepare()

                // Initialize equalizer with this player's audio session
                equalizerManager.initialize(audioSessionId)
                superEqualizer.initialize(audioSessionId)

                // Seek to start position if provided
                if (startPosition > 0) {
                    seekTo(startPosition.toInt())
                }

                start()

                setOnCompletionListener {
                    onTrackComplete()
                }

                // Update state with audio session ID
                _playerState.value = _playerState.value.copy(
                    audioSessionId = audioSessionId
                )
            }

            // Preserve queue and playback mode state
            val currentState = _playerState.value
            _playerState.value = currentState.copy(
                isPlaying = true,
                isPaused = false,
                currentPosition = startPosition,
                duration = mediaPlayer?.duration?.toLong() ?: 0L,
                currentFileName = fileName,
                currentFileId = fileId,
                audioSessionId = mediaPlayer?.audioSessionId ?: 0
            )

            startProgressUpdate()
            updateNotification()

        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
            stop()
        }
    }

    // Play from URI (for device media files)
    fun playFromUri(context: Context, fileId: String, fileName: String, uri: android.net.Uri, startPosition: Long = 0L) {
        try {
            // If same file, toggle play/pause
            if (_playerState.value.currentFileId == fileId && mediaPlayer != null) {
                if (_playerState.value.isPlaying) {
                    pause()
                } else {
                    resume()
                }
                return
            }

            // Stop previous playback
            stop()

            // Request audio focus - this will pause other apps
            if (!requestAudioFocus()) {
                Log.w(TAG, "Could not gain audio focus")
            }

            mediaPlayer = MediaPlayer().apply {
                // Set audio attributes for better Bluetooth support
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                setDataSource(context, uri)
                prepare()

                // Initialize equalizer with this player's audio session
                equalizerManager.initialize(audioSessionId)
                superEqualizer.initialize(audioSessionId)

                // Seek to start position if provided
                if (startPosition > 0) {
                    seekTo(startPosition.toInt())
                }

                start()

                setOnCompletionListener {
                    onTrackComplete()
                }
            }

            // Preserve queue and playback mode state
            val currentState = _playerState.value
            _playerState.value = currentState.copy(
                isPlaying = true,
                isPaused = false,
                currentPosition = startPosition,
                duration = mediaPlayer?.duration?.toLong() ?: 0L,
                currentFileName = fileName,
                currentFileId = fileId,
                audioSessionId = mediaPlayer?.audioSessionId ?: 0
            )

            startProgressUpdate()
            updateNotification()

        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio from URI", e)
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        _playerState.value = _playerState.value.copy(
            isPlaying = false,
            isPaused = true
        )
        stopProgressUpdate()
        updateNotification()
    }

    fun resume() {
        mediaPlayer?.start()
        _playerState.value = _playerState.value.copy(
            isPlaying = true,
            isPaused = false
        )
        startProgressUpdate()
        updateNotification()
    }

    fun stop() {
        stopProgressUpdate()
        notificationManager?.hideNotification()
        abandonAudioFocus()
        mediaPlayer?.apply {
            try {
                if (isPlaying) stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping player: ${e.message}")
            }
            release()
        }
        mediaPlayer = null
        _playerState.value = AudioPlayerState()
    }

    fun seekTo(position: Long) {
        mediaPlayer?.seekTo(position.toInt())
        _playerState.value = _playerState.value.copy(currentPosition = position)
        updateNotification()
    }

    fun seekForward(seconds: Int = 10) {
        val newPosition = (_playerState.value.currentPosition + seconds * 1000)
            .coerceAtMost(_playerState.value.duration)
        seekTo(newPosition)
    }

    fun seekBackward(seconds: Int = 10) {
        val newPosition = (_playerState.value.currentPosition - seconds * 1000)
            .coerceAtLeast(0L)
        seekTo(newPosition)
    }

    // Toggle repeat mode: OFF -> ONE -> ALL -> OFF
    fun toggleRepeatMode() {
        val newMode = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.OFF
        }
        _playerState.value = _playerState.value.copy(repeatMode = newMode)
    }

    // Toggle shuffle
    fun toggleShuffle() {
        val newShuffle = !_playerState.value.isShuffleEnabled
        _playerState.value = _playerState.value.copy(isShuffleEnabled = newShuffle)

        if (newShuffle && _playerState.value.queue.isNotEmpty()) {
            // Generate shuffled indices
            shuffledIndices = _playerState.value.queue.indices.shuffled()
        }
    }

    // Set queue and start playing
    fun playQueue(queue: List<QueueItem>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (queue.isEmpty()) return

        _playerState.value = _playerState.value.copy(
            queue = queue,
            currentQueueIndex = startIndex,
            isShuffleEnabled = shuffle
        )

        if (shuffle) {
            // Shuffle but ensure current item is first
            val indices = queue.indices.toMutableList()
            indices.remove(startIndex)
            shuffledIndices = listOf(startIndex) + indices.shuffled()
        }

        playQueueItem(startIndex)
    }

    // Set queue from URIs (for device media) and start playing
    fun playQueueFromUris(
        context: Context,
        items: List<Triple<String, String, android.net.Uri>>, // id, name, uri
        startIndex: Int = 0,
        shuffle: Boolean = false
    ) {
        if (items.isEmpty()) return

        // Store URIs for later playback
        uriMap.clear()
        items.forEach { (id, _, uri) ->
            uriMap[id] = uri
        }

        // Create queue items
        val queue = items.map { (id, name, _) ->
            QueueItem(id = id, fileName = name, isVideo = false)
        }

        _playerState.value = _playerState.value.copy(
            queue = queue,
            currentQueueIndex = startIndex,
            isShuffleEnabled = shuffle
        )

        if (shuffle) {
            val indices = queue.indices.toMutableList()
            indices.remove(startIndex)
            shuffledIndices = listOf(startIndex) + indices.shuffled()
        }

        // Play using URI
        val item = items[startIndex]
        playFromUri(context, item.first, item.second, item.third)
    }

    // Play specific item in queue
    fun playQueueItem(index: Int) {
        val queue = _playerState.value.queue
        if (index < 0 || index >= queue.size) return

        val item = queue[index]
        _playerState.value = _playerState.value.copy(currentQueueIndex = index)

        // Check if this item has a stored URI (device media)
        val uri = uriMap[item.id]
        if (uri != null) {
            playFromUri(context, item.id, item.fileName, uri)
        } else {
            play(item.id, item.fileName, item.isVideo)
        }
    }

    // Remove item from queue
    fun removeFromQueue(index: Int) {
        val state = _playerState.value
        if (index < 0 || index >= state.queue.size) return

        val newQueue = state.queue.toMutableList()
        newQueue.removeAt(index)

        // Adjust current index if necessary
        val newIndex = when {
            newQueue.isEmpty() -> -1
            index < state.currentQueueIndex -> state.currentQueueIndex - 1
            index == state.currentQueueIndex && index >= newQueue.size -> newQueue.size - 1
            else -> state.currentQueueIndex
        }

        _playerState.value = state.copy(
            queue = newQueue,
            currentQueueIndex = newIndex
        )

        // Update shuffle indices
        if (state.isShuffleEnabled && newQueue.isNotEmpty()) {
            shuffledIndices = shuffledIndices.filter { it < newQueue.size }
        }
    }

    // Clear entire queue
    fun clearQueue() {
        stop()
        _playerState.value = _playerState.value.copy(
            queue = emptyList(),
            currentQueueIndex = -1
        )
        shuffledIndices = emptyList()
    }

    // Play next in queue
    fun playNext() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        val nextIndex = if (state.isShuffleEnabled && shuffledIndices.isNotEmpty()) {
            val currentShufflePos = shuffledIndices.indexOf(state.currentQueueIndex)
            if (currentShufflePos < shuffledIndices.size - 1) {
                shuffledIndices[currentShufflePos + 1]
            } else if (state.repeatMode == RepeatMode.ALL) {
                shuffledIndices = state.queue.indices.shuffled()
                shuffledIndices.firstOrNull() ?: 0
            } else {
                -1
            }
        } else {
            if (state.currentQueueIndex < state.queue.size - 1) {
                state.currentQueueIndex + 1
            } else if (state.repeatMode == RepeatMode.ALL) {
                0
            } else {
                -1
            }
        }

        if (nextIndex >= 0) {
            playQueueItem(nextIndex)
        } else {
            stop()
        }
    }

    // Play previous in queue
    fun playPrevious() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        // If more than 3 seconds played, restart current track
        if (state.currentPosition > 3000) {
            seekTo(0)
            return
        }

        val prevIndex = if (state.isShuffleEnabled && shuffledIndices.isNotEmpty()) {
            val currentShufflePos = shuffledIndices.indexOf(state.currentQueueIndex)
            if (currentShufflePos > 0) {
                shuffledIndices[currentShufflePos - 1]
            } else if (state.repeatMode == RepeatMode.ALL) {
                shuffledIndices.lastOrNull() ?: 0
            } else {
                0
            }
        } else {
            if (state.currentQueueIndex > 0) {
                state.currentQueueIndex - 1
            } else if (state.repeatMode == RepeatMode.ALL) {
                state.queue.size - 1
            } else {
                0
            }
        }

        playQueueItem(prevIndex)
    }

    // Handle track completion
    private fun onTrackComplete() {
        val state = _playerState.value

        when (state.repeatMode) {
            RepeatMode.ONE -> {
                // Replay current track
                seekTo(0)
                resume()
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                // Try to play next
                playNext()
            }
        }
    }

    private fun updateNotification() {
        // Check if notifications are enabled in settings
        val settingsManager = SettingsManager.getInstance(context)
        if (!settingsManager.settings.value.notificationsEnabled) {
            notificationManager?.hideNotification()
            return
        }

        val state = _playerState.value
        if (state.currentFileId != null) {
            notificationManager?.showNotification(
                title = state.currentFileName,
                isPlaying = state.isPlaying,
                currentPosition = state.currentPosition,
                duration = state.duration,
                onPlay = { resume() },
                onPause = { pause() },
                onStop = { stop() },
                onSeekForward = { seekForward() },
                onSeekBackward = { seekBackward() }
            )
        }
    }

    private fun startProgressUpdate() {
        updateJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _playerState.value = _playerState.value.copy(
                                currentPosition = player.currentPosition.toLong()
                            )
                            // Update notification periodically
                            if (_playerState.value.currentPosition % 5000 < 500) {
                                updateNotification()
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error updating progress: ${e.message}")
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressUpdate() {
        updateJob?.cancel()
        updateJob = null
    }

    fun release() {
        stop()
        try {
            context.unregisterReceiver(mediaActionReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering media receiver: ${e.message}")
        }
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering Bluetooth receiver: ${e.message}")
        }
        try {
            volumeContentObserver?.let {
                context.contentResolver.unregisterContentObserver(it)
            }
            volumeContentObserver = null
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering volume observer: ${e.message}")
        }
        _equalizerManager?.release()
        _equalizerManager = null
        notificationManager?.release()
        scope.cancel()
    }
}
