package com.example.dwn.dj

import android.content.ContentUris
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.EnvironmentalReverb
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.abs
import kotlin.math.sin

/**
 * DJ Engine Pro - Full-featured DJ audio engine with scratching, effects, and mixing
 */
class DJEnginePro(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Media players for each deck
    private var deckAPlayer: MediaPlayer? = null
    private var deckBPlayer: MediaPlayer? = null

    // Audio effects - Deck A
    private var deckAEqualizer: Equalizer? = null
    private var deckABassBoost: BassBoost? = null
    private var deckAVirtualizer: Virtualizer? = null
    private var deckAReverb: PresetReverb? = null

    // Audio effects - Deck B
    private var deckBEqualizer: Equalizer? = null
    private var deckBBassBoost: BassBoost? = null
    private var deckBVirtualizer: Virtualizer? = null
    private var deckBReverb: PresetReverb? = null

    // Sound pool for scratch sounds
    private var soundPool: SoundPool? = null
    private var scratchSound1: Int = 0
    private var scratchSound2: Int = 0
    private var scratchSound3: Int = 0
    private var rewindSound: Int = 0
    private var airHornSound: Int = 0

    // Vibrator for haptic feedback
    private var vibrator: Vibrator? = null

    // State flows
    private val _deckAState = MutableStateFlow(DeckStatePro())
    val deckAState: StateFlow<DeckStatePro> = _deckAState.asStateFlow()

    private val _deckBState = MutableStateFlow(DeckStatePro())
    val deckBState: StateFlow<DeckStatePro> = _deckBState.asStateFlow()

    private val _mixerState = MutableStateFlow(MixerState())
    val mixerState: StateFlow<MixerState> = _mixerState.asStateFlow()

    private val _fxState = MutableStateFlow(FXState())
    val fxState: StateFlow<FXState> = _fxState.asStateFlow()

    // Waveform data
    private val _deckAWaveform = MutableStateFlow<List<Float>>(emptyList())
    val deckAWaveform: StateFlow<List<Float>> = _deckAWaveform.asStateFlow()

    private val _deckBWaveform = MutableStateFlow<List<Float>>(emptyList())
    val deckBWaveform: StateFlow<List<Float>> = _deckBWaveform.asStateFlow()

    // Position update jobs
    private var deckAPositionJob: Job? = null
    private var deckBPositionJob: Job? = null
    private var waveformUpdateJob: Job? = null

    // Scratch state
    private var isScratching = false
    private var lastScratchTime = 0L
    private var scratchVelocity = 0f

    init {
        initSoundPool()
        initVibrator()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(audioAttributes)
            .build()

        // Note: In a real app, you'd load actual scratch sound files
        // For now, we'll generate synthetic scratch sounds
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // ==========================================
    // DEVICE MUSIC LOADING
    // ==========================================

    fun getDeviceMusic(): List<DeviceTrackPro> {
        val tracks = mutableListOf<DeviceTrackPro>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val path = cursor.getString(dataColumn) ?: ""
                val albumId = cursor.getLong(albumIdColumn)

                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                if (duration > 30000) {
                    tracks.add(
                        DeviceTrackPro(
                            id = id.toString(),
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            uri = uri.toString(),
                            path = path,
                            albumArtUri = albumArtUri.toString(),
                            bpm = estimateBPM(duration),
                            key = estimateKey(id)
                        )
                    )
                }
            }
        }

        return tracks
    }

    private fun estimateBPM(duration: Long): Float {
        // Simple BPM estimation based on track duration
        // In a real app, you'd analyze the audio
        return listOf(120f, 125f, 128f, 130f, 135f, 140f).random()
    }

    private fun estimateKey(id: Long): String {
        val keys = listOf("Am", "Bm", "Cm", "Dm", "Em", "Fm", "Gm", "A", "B", "C", "D", "E", "F", "G")
        return keys[(id % keys.size).toInt()]
    }

    // ==========================================
    // WAVEFORM GENERATION
    // ==========================================

    private fun generateWaveform(duration: Long): List<Float> {
        val samples = 200
        val waveform = mutableListOf<Float>()

        for (i in 0 until samples) {
            // Generate realistic-looking waveform with multiple frequencies
            val t = i.toFloat() / samples
            val value = (
                sin(t * 50) * 0.3f +
                sin(t * 120) * 0.25f +
                sin(t * 200 + 0.5f) * 0.2f +
                sin(t * 400) * 0.15f +
                (Math.random().toFloat() - 0.5f) * 0.2f
            ).coerceIn(-1f, 1f)

            waveform.add(abs(value))
        }

        return waveform
    }

    // ==========================================
    // DECK LOADING
    // ==========================================

    fun loadToDeckA(track: DeviceTrackPro) {
        scope.launch {
            try {
                deckAPlayer?.release()
                releaseEffectsA()

                deckAPlayer = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(track.uri))
                    prepare()
                    setVolume(_deckAState.value.volume, _deckAState.value.volume)

                    setOnCompletionListener {
                        _deckAState.value = _deckAState.value.copy(
                            isPlaying = false,
                            position = 0f,
                            currentPosition = 0L
                        )
                    }
                }

                setupEffectsA()

                // Generate waveform
                _deckAWaveform.value = generateWaveform(track.duration)

                _deckAState.value = _deckAState.value.copy(
                    isLoaded = true,
                    track = track,
                    duration = deckAPlayer?.duration?.toLong() ?: 0L,
                    position = 0f,
                    currentPosition = 0L,
                    isPlaying = false,
                    bpm = track.bpm,
                    key = track.key
                )

                hapticFeedback(50)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadToDeckB(track: DeviceTrackPro) {
        scope.launch {
            try {
                deckBPlayer?.release()
                releaseEffectsB()

                deckBPlayer = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(track.uri))
                    prepare()
                    setVolume(_deckBState.value.volume, _deckBState.value.volume)

                    setOnCompletionListener {
                        _deckBState.value = _deckBState.value.copy(
                            isPlaying = false,
                            position = 0f,
                            currentPosition = 0L
                        )
                    }
                }

                setupEffectsB()

                // Generate waveform
                _deckBWaveform.value = generateWaveform(track.duration)

                _deckBState.value = _deckBState.value.copy(
                    isLoaded = true,
                    track = track,
                    duration = deckBPlayer?.duration?.toLong() ?: 0L,
                    position = 0f,
                    currentPosition = 0L,
                    isPlaying = false,
                    bpm = track.bpm,
                    key = track.key
                )

                hapticFeedback(50)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupEffectsA() {
        deckAPlayer?.audioSessionId?.let { sessionId ->
            try {
                deckAEqualizer = Equalizer(0, sessionId).apply { enabled = true }
                deckABassBoost = BassBoost(0, sessionId).apply { enabled = false }
                deckAVirtualizer = Virtualizer(0, sessionId).apply { enabled = false }
                deckAReverb = PresetReverb(0, sessionId).apply { enabled = false }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupEffectsB() {
        deckBPlayer?.audioSessionId?.let { sessionId ->
            try {
                deckBEqualizer = Equalizer(0, sessionId).apply { enabled = true }
                deckBBassBoost = BassBoost(0, sessionId).apply { enabled = false }
                deckBVirtualizer = Virtualizer(0, sessionId).apply { enabled = false }
                deckBReverb = PresetReverb(0, sessionId).apply { enabled = false }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun releaseEffectsA() {
        deckAEqualizer?.release()
        deckABassBoost?.release()
        deckAVirtualizer?.release()
        deckAReverb?.release()
    }

    private fun releaseEffectsB() {
        deckBEqualizer?.release()
        deckBBassBoost?.release()
        deckBVirtualizer?.release()
        deckBReverb?.release()
    }

    // ==========================================
    // PLAYBACK CONTROL
    // ==========================================

    fun playDeckA() {
        deckAPlayer?.let { player ->
            if (!player.isPlaying && _deckAState.value.isLoaded) {
                player.start()
                _deckAState.value = _deckAState.value.copy(isPlaying = true)
                startPositionUpdateA()
                hapticFeedback(30)
            }
        }
    }

    fun pauseDeckA() {
        deckAPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _deckAState.value = _deckAState.value.copy(isPlaying = false)
                deckAPositionJob?.cancel()
                hapticFeedback(20)
            }
        }
    }

    fun togglePlayDeckA() {
        if (_deckAState.value.isPlaying) pauseDeckA() else playDeckA()
    }

    fun playDeckB() {
        deckBPlayer?.let { player ->
            if (!player.isPlaying && _deckBState.value.isLoaded) {
                player.start()
                _deckBState.value = _deckBState.value.copy(isPlaying = true)
                startPositionUpdateB()
                hapticFeedback(30)
            }
        }
    }

    fun pauseDeckB() {
        deckBPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _deckBState.value = _deckBState.value.copy(isPlaying = false)
                deckBPositionJob?.cancel()
                hapticFeedback(20)
            }
        }
    }

    fun togglePlayDeckB() {
        if (_deckBState.value.isPlaying) pauseDeckB() else playDeckB()
    }

    // ==========================================
    // SCRATCHING - THE JIGIJIGI!
    // ==========================================

    fun startScratchA() {
        isScratching = true
        _deckAState.value = _deckAState.value.copy(isScratching = true)

        // Reduce volume slightly for scratch effect
        deckAPlayer?.let { player ->
            val vol = _deckAState.value.volume * 0.7f
            player.setVolume(vol, vol)
        }
    }

    fun scratchDeckA(delta: Float) {
        if (!_deckAState.value.isLoaded) return

        deckAPlayer?.let { player ->
            val currentPos = player.currentPosition
            val scratchAmount = (delta * 150).toInt() // More sensitive scratching
            val newPos = (currentPos + scratchAmount).coerceIn(0, player.duration)

            player.seekTo(newPos)

            // Update scratch velocity for sound intensity
            scratchVelocity = abs(delta)

            // Haptic feedback proportional to scratch speed
            val intensity = (abs(delta) * 100).toInt().coerceIn(10, 100)
            hapticFeedback(intensity.toLong())

            // Update position immediately
            val position = newPos.toFloat() / player.duration
            _deckAState.value = _deckAState.value.copy(
                position = position,
                currentPosition = newPos.toLong(),
                scratchVelocity = scratchVelocity
            )

            lastScratchTime = System.currentTimeMillis()
        }
    }

    fun endScratchA() {
        isScratching = false
        _deckAState.value = _deckAState.value.copy(
            isScratching = false,
            scratchVelocity = 0f
        )

        // Restore volume
        deckAPlayer?.let { player ->
            val vol = _deckAState.value.volume * calculateCrossfadeA() * _mixerState.value.masterVolume
            player.setVolume(vol, vol)
        }
    }

    fun startScratchB() {
        isScratching = true
        _deckBState.value = _deckBState.value.copy(isScratching = true)

        deckBPlayer?.let { player ->
            val vol = _deckBState.value.volume * 0.7f
            player.setVolume(vol, vol)
        }
    }

    fun scratchDeckB(delta: Float) {
        if (!_deckBState.value.isLoaded) return

        deckBPlayer?.let { player ->
            val currentPos = player.currentPosition
            val scratchAmount = (delta * 150).toInt()
            val newPos = (currentPos + scratchAmount).coerceIn(0, player.duration)

            player.seekTo(newPos)

            scratchVelocity = abs(delta)

            val intensity = (abs(delta) * 100).toInt().coerceIn(10, 100)
            hapticFeedback(intensity.toLong())

            val position = newPos.toFloat() / player.duration
            _deckBState.value = _deckBState.value.copy(
                position = position,
                currentPosition = newPos.toLong(),
                scratchVelocity = scratchVelocity
            )

            lastScratchTime = System.currentTimeMillis()
        }
    }

    fun endScratchB() {
        isScratching = false
        _deckBState.value = _deckBState.value.copy(
            isScratching = false,
            scratchVelocity = 0f
        )

        deckBPlayer?.let { player ->
            val vol = _deckBState.value.volume * calculateCrossfadeB() * _mixerState.value.masterVolume
            player.setVolume(vol, vol)
        }
    }

    // ==========================================
    // CUE CONTROL
    // ==========================================

    fun cueDeckA() {
        deckAPlayer?.let { player ->
            val cuePoint = _deckAState.value.cuePoint
            player.seekTo(cuePoint.toInt())
            _deckAState.value = _deckAState.value.copy(
                position = cuePoint.toFloat() / player.duration,
                currentPosition = cuePoint
            )
            hapticFeedback(40)
        }
    }

    fun setCuePointA() {
        deckAPlayer?.let { player ->
            _deckAState.value = _deckAState.value.copy(
                cuePoint = player.currentPosition.toLong()
            )
            hapticFeedback(60)
        }
    }

    fun cueDeckB() {
        deckBPlayer?.let { player ->
            val cuePoint = _deckBState.value.cuePoint
            player.seekTo(cuePoint.toInt())
            _deckBState.value = _deckBState.value.copy(
                position = cuePoint.toFloat() / player.duration,
                currentPosition = cuePoint
            )
            hapticFeedback(40)
        }
    }

    fun setCuePointB() {
        deckBPlayer?.let { player ->
            _deckBState.value = _deckBState.value.copy(
                cuePoint = player.currentPosition.toLong()
            )
            hapticFeedback(60)
        }
    }

    // ==========================================
    // HOT CUES
    // ==========================================

    fun triggerHotCueDeckA(index: Int) {
        deckAPlayer?.let { player ->
            val hotCue = _deckAState.value.hotCues[index]
            if (hotCue >= 0) {
                player.seekTo(hotCue.toInt())
                if (!player.isPlaying) playDeckA()
                hapticFeedback(50)
            } else {
                setHotCueDeckA(index)
            }
        }
    }

    fun setHotCueDeckA(index: Int) {
        deckAPlayer?.let { player ->
            val hotCues = _deckAState.value.hotCues.toMutableList()
            hotCues[index] = player.currentPosition.toLong()
            _deckAState.value = _deckAState.value.copy(hotCues = hotCues)
            hapticFeedback(80)
        }
    }

    fun clearHotCueDeckA(index: Int) {
        val hotCues = _deckAState.value.hotCues.toMutableList()
        hotCues[index] = -1L
        _deckAState.value = _deckAState.value.copy(hotCues = hotCues)
        hapticFeedback(30)
    }

    fun triggerHotCueDeckB(index: Int) {
        deckBPlayer?.let { player ->
            val hotCue = _deckBState.value.hotCues[index]
            if (hotCue >= 0) {
                player.seekTo(hotCue.toInt())
                if (!player.isPlaying) playDeckB()
                hapticFeedback(50)
            } else {
                setHotCueDeckB(index)
            }
        }
    }

    fun setHotCueDeckB(index: Int) {
        deckBPlayer?.let { player ->
            val hotCues = _deckBState.value.hotCues.toMutableList()
            hotCues[index] = player.currentPosition.toLong()
            _deckBState.value = _deckBState.value.copy(hotCues = hotCues)
            hapticFeedback(80)
        }
    }

    fun clearHotCueDeckB(index: Int) {
        val hotCues = _deckBState.value.hotCues.toMutableList()
        hotCues[index] = -1L
        _deckBState.value = _deckBState.value.copy(hotCues = hotCues)
        hapticFeedback(30)
    }

    // ==========================================
    // VOLUME & MIXER CONTROL
    // ==========================================

    fun setDeckAVolume(volume: Float) {
        _deckAState.value = _deckAState.value.copy(volume = volume)
        updateDeckAVolume()
    }

    fun setDeckBVolume(volume: Float) {
        _deckBState.value = _deckBState.value.copy(volume = volume)
        updateDeckBVolume()
    }

    fun setDeckAGain(gain: Float) {
        _deckAState.value = _deckAState.value.copy(gain = gain)
        updateDeckAVolume()
    }

    fun setDeckBGain(gain: Float) {
        _deckBState.value = _deckBState.value.copy(gain = gain)
        updateDeckBVolume()
    }

    fun setMasterVolume(volume: Float) {
        _mixerState.value = _mixerState.value.copy(masterVolume = volume)
        updateDeckAVolume()
        updateDeckBVolume()
    }

    fun setCrossfader(position: Float) {
        _mixerState.value = _mixerState.value.copy(crossfader = position)
        updateDeckAVolume()
        updateDeckBVolume()
    }

    fun setHeadphoneMix(mix: Float) {
        _mixerState.value = _mixerState.value.copy(headphoneMix = mix)
    }

    fun setHeadphoneVolume(volume: Float) {
        _mixerState.value = _mixerState.value.copy(headphoneVolume = volume)
    }

    fun toggleHeadphoneCueA() {
        _mixerState.value = _mixerState.value.copy(
            headphoneCueA = !_mixerState.value.headphoneCueA
        )
        hapticFeedback(30)
    }

    fun toggleHeadphoneCueB() {
        _mixerState.value = _mixerState.value.copy(
            headphoneCueB = !_mixerState.value.headphoneCueB
        )
        hapticFeedback(30)
    }

    private fun updateDeckAVolume() {
        deckAPlayer?.let { player ->
            val baseVol = _deckAState.value.volume
            val gain = _deckAState.value.gain
            val crossfade = calculateCrossfadeA()
            val master = _mixerState.value.masterVolume

            val finalVol = (baseVol * (1 + gain) * crossfade * master).coerceIn(0f, 1f)
            player.setVolume(finalVol, finalVol)
        }
    }

    private fun updateDeckBVolume() {
        deckBPlayer?.let { player ->
            val baseVol = _deckBState.value.volume
            val gain = _deckBState.value.gain
            val crossfade = calculateCrossfadeB()
            val master = _mixerState.value.masterVolume

            val finalVol = (baseVol * (1 + gain) * crossfade * master).coerceIn(0f, 1f)
            player.setVolume(finalVol, finalVol)
        }
    }

    private fun calculateCrossfadeA(): Float {
        val cf = _mixerState.value.crossfader
        // At 0.0 = full A, at 0.5 = full both, at 1.0 = no A
        return when {
            cf <= 0.5f -> 1f  // Full volume for Deck A from left to center
            else -> 1f - ((cf - 0.5f) * 2f)  // Fade out from center to right
        }.coerceIn(0f, 1f)
    }

    private fun calculateCrossfadeB(): Float {
        val cf = _mixerState.value.crossfader
        // At 0.0 = no B, at 0.5 = full both, at 1.0 = full B
        return when {
            cf >= 0.5f -> 1f  // Full volume for Deck B from center to right
            else -> cf * 2f   // Fade in from left to center
        }.coerceIn(0f, 1f)
    }

    // ==========================================
    // EQ CONTROL
    // ==========================================

    fun setDeckAEqHigh(value: Float) {
        _deckAState.value = _deckAState.value.copy(eqHigh = value)
        applyEqA()
    }

    fun setDeckAEqMid(value: Float) {
        _deckAState.value = _deckAState.value.copy(eqMid = value)
        applyEqA()
    }

    fun setDeckAEqLow(value: Float) {
        _deckAState.value = _deckAState.value.copy(eqLow = value)
        applyEqA()
    }

    fun toggleDeckAEqKillHigh() {
        _deckAState.value = _deckAState.value.copy(
            killHigh = !_deckAState.value.killHigh
        )
        applyEqA()
        hapticFeedback(40)
    }

    fun toggleDeckAEqKillMid() {
        _deckAState.value = _deckAState.value.copy(
            killMid = !_deckAState.value.killMid
        )
        applyEqA()
        hapticFeedback(40)
    }

    fun toggleDeckAEqKillLow() {
        _deckAState.value = _deckAState.value.copy(
            killLow = !_deckAState.value.killLow
        )
        applyEqA()
        hapticFeedback(40)
    }

    private fun applyEqA() {
        deckAEqualizer?.let { eq ->
            val bandCount = eq.numberOfBands.toInt()
            if (bandCount >= 3) {
                val state = _deckAState.value
                val range = eq.bandLevelRange
                val minLevel = range[0]
                val maxLevel = range[1]

                // Low
                val lowLevel = if (state.killLow) minLevel else
                    (state.eqLow * (maxLevel - minLevel) + minLevel).toInt().toShort()
                eq.setBandLevel(0, lowLevel)

                // Mid
                val midBand = (bandCount / 2).toShort()
                val midLevel = if (state.killMid) minLevel else
                    (state.eqMid * (maxLevel - minLevel) + minLevel).toInt().toShort()
                eq.setBandLevel(midBand, midLevel)

                // High
                val highBand = (bandCount - 1).toShort()
                val highLevel = if (state.killHigh) minLevel else
                    (state.eqHigh * (maxLevel - minLevel) + minLevel).toInt().toShort()
                eq.setBandLevel(highBand, highLevel)
            }
        }
    }

    fun setDeckBEqHigh(value: Float) {
        _deckBState.value = _deckBState.value.copy(eqHigh = value)
        applyEqB()
    }

    fun setDeckBEqMid(value: Float) {
        _deckBState.value = _deckBState.value.copy(eqMid = value)
        applyEqB()
    }

    fun setDeckBEqLow(value: Float) {
        _deckBState.value = _deckBState.value.copy(eqLow = value)
        applyEqB()
    }

    fun toggleDeckBEqKillHigh() {
        _deckBState.value = _deckBState.value.copy(killHigh = !_deckBState.value.killHigh)
        applyEqB()
        hapticFeedback(40)
    }

    fun toggleDeckBEqKillMid() {
        _deckBState.value = _deckBState.value.copy(killMid = !_deckBState.value.killMid)
        applyEqB()
        hapticFeedback(40)
    }

    fun toggleDeckBEqKillLow() {
        _deckBState.value = _deckBState.value.copy(killLow = !_deckBState.value.killLow)
        applyEqB()
        hapticFeedback(40)
    }

    private fun applyEqB() {
        deckBEqualizer?.let { eq ->
            val bandCount = eq.numberOfBands.toInt()
            if (bandCount >= 3) {
                val state = _deckBState.value
                val range = eq.bandLevelRange
                val minLevel = range[0]
                val maxLevel = range[1]

                val lowLevel = if (state.killLow) minLevel else
                    (state.eqLow * (maxLevel - minLevel) + minLevel).toInt().toShort()
                eq.setBandLevel(0, lowLevel)

                val midBand = (bandCount / 2).toShort()
                val midLevel = if (state.killMid) minLevel else
                    (state.eqMid * (maxLevel - minLevel) + minLevel).toInt().toShort()
                eq.setBandLevel(midBand, midLevel)

                val highBand = (bandCount - 1).toShort()
                val highLevel = if (state.killHigh) minLevel else
                    (state.eqHigh * (maxLevel - minLevel) + minLevel).toInt().toShort()
                eq.setBandLevel(highBand, highLevel)
            }
        }
    }

    // ==========================================
    // TEMPO / SYNC
    // ==========================================

    fun setDeckATempo(bpm: Float) {
        _deckAState.value = _deckAState.value.copy(bpm = bpm)
    }

    fun setDeckBTempo(bpm: Float) {
        _deckBState.value = _deckBState.value.copy(bpm = bpm)
    }

    fun syncDeckAToB() {
        _deckAState.value = _deckAState.value.copy(
            bpm = _deckBState.value.bpm,
            isSynced = true
        )
        hapticFeedback(60)
    }

    fun syncDeckBToA() {
        _deckBState.value = _deckBState.value.copy(
            bpm = _deckAState.value.bpm,
            isSynced = true
        )
        hapticFeedback(60)
    }

    fun unsyncDeckA() {
        _deckAState.value = _deckAState.value.copy(isSynced = false)
    }

    fun unsyncDeckB() {
        _deckBState.value = _deckBState.value.copy(isSynced = false)
    }

    // ==========================================
    // LOOP CONTROL
    // ==========================================

    fun setLoopDeckA(bars: Int) {
        deckAPlayer?.let { player ->
            val bpm = _deckAState.value.bpm
            val msPerBeat = 60000f / bpm
            val loopLengthMs = (bars * 4 * msPerBeat).toLong()

            val loopStart = player.currentPosition.toLong()
            val loopEnd = (loopStart + loopLengthMs).coerceAtMost(player.duration.toLong())

            _deckAState.value = _deckAState.value.copy(
                loopEnabled = true,
                loopStart = loopStart,
                loopEnd = loopEnd,
                loopBars = bars
            )
            hapticFeedback(50)
        }
    }

    fun toggleLoopDeckA() {
        val newState = !_deckAState.value.loopEnabled
        _deckAState.value = _deckAState.value.copy(loopEnabled = newState)
        hapticFeedback(40)
    }

    fun doubleLoopDeckA() {
        val currentBars = _deckAState.value.loopBars
        if (currentBars < 32) {
            setLoopDeckA(currentBars * 2)
        }
    }

    fun halveLoopDeckA() {
        val currentBars = _deckAState.value.loopBars
        if (currentBars > 1) {
            setLoopDeckA(currentBars / 2)
        }
    }

    fun setLoopDeckB(bars: Int) {
        deckBPlayer?.let { player ->
            val bpm = _deckBState.value.bpm
            val msPerBeat = 60000f / bpm
            val loopLengthMs = (bars * 4 * msPerBeat).toLong()

            val loopStart = player.currentPosition.toLong()
            val loopEnd = (loopStart + loopLengthMs).coerceAtMost(player.duration.toLong())

            _deckBState.value = _deckBState.value.copy(
                loopEnabled = true,
                loopStart = loopStart,
                loopEnd = loopEnd,
                loopBars = bars
            )
            hapticFeedback(50)
        }
    }

    fun toggleLoopDeckB() {
        val newState = !_deckBState.value.loopEnabled
        _deckBState.value = _deckBState.value.copy(loopEnabled = newState)
        hapticFeedback(40)
    }

    // ==========================================
    // EFFECTS (FX)
    // ==========================================

    fun toggleFX(fxType: FXType) {
        val current = _fxState.value
        val newState = when (fxType) {
            FXType.ECHO -> current.copy(echoEnabled = !current.echoEnabled)
            FXType.REVERB -> current.copy(reverbEnabled = !current.reverbEnabled)
            FXType.FLANGER -> current.copy(flangerEnabled = !current.flangerEnabled)
            FXType.PHASER -> current.copy(phaserEnabled = !current.phaserEnabled)
            FXType.FILTER -> current.copy(filterEnabled = !current.filterEnabled)
            FXType.BITCRUSH -> current.copy(bitcrushEnabled = !current.bitcrushEnabled)
            FXType.STUTTER -> current.copy(stutterEnabled = !current.stutterEnabled)
            FXType.GATE -> current.copy(gateEnabled = !current.gateEnabled)
        }
        _fxState.value = newState
        applyFX()
        hapticFeedback(50)
    }

    fun setFXWetDry(value: Float) {
        _fxState.value = _fxState.value.copy(wetDry = value)
        applyFX()
    }

    fun setFXParameter1(value: Float) {
        _fxState.value = _fxState.value.copy(param1 = value)
        applyFX()
    }

    fun setFXParameter2(value: Float) {
        _fxState.value = _fxState.value.copy(param2 = value)
        applyFX()
    }

    fun setFXBeatSync(beats: Int) {
        _fxState.value = _fxState.value.copy(beatSync = beats)
        applyFX()
    }

    private fun applyFX() {
        val fx = _fxState.value

        // Apply reverb
        if (fx.reverbEnabled) {
            val preset = when {
                fx.param1 < 0.3f -> PresetReverb.PRESET_SMALLROOM
                fx.param1 < 0.6f -> PresetReverb.PRESET_MEDIUMROOM
                else -> PresetReverb.PRESET_LARGEHALL
            }
            deckAReverb?.preset = preset
            deckAReverb?.enabled = true
            deckBReverb?.preset = preset
            deckBReverb?.enabled = true
        } else {
            deckAReverb?.enabled = false
            deckBReverb?.enabled = false
        }

        // Apply bass boost for certain effects
        if (fx.filterEnabled && fx.param1 > 0.5f) {
            val strength = ((fx.param1 - 0.5f) * 2000).toInt().toShort()
            deckABassBoost?.setStrength(strength)
            deckABassBoost?.enabled = true
            deckBBassBoost?.setStrength(strength)
            deckBBassBoost?.enabled = true
        } else {
            deckABassBoost?.enabled = false
            deckBBassBoost?.enabled = false
        }

        // Virtualizer for spatial effects
        if (fx.phaserEnabled || fx.flangerEnabled) {
            val strength = (fx.wetDry * 1000).toInt().toShort()
            deckAVirtualizer?.setStrength(strength)
            deckAVirtualizer?.enabled = true
            deckBVirtualizer?.setStrength(strength)
            deckBVirtualizer?.enabled = true
        } else {
            deckAVirtualizer?.enabled = false
            deckBVirtualizer?.enabled = false
        }
    }

    // ==========================================
    // FILTER
    // ==========================================

    fun setDeckAFilter(value: Float) {
        _deckAState.value = _deckAState.value.copy(filter = value)
        applyFilterA()
    }

    fun setDeckBFilter(value: Float) {
        _deckBState.value = _deckBState.value.copy(filter = value)
        applyFilterB()
    }

    private fun applyFilterA() {
        // Filter is simulated through EQ adjustment
        deckAEqualizer?.let { eq ->
            val filterVal = _deckAState.value.filter
            val bandCount = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minLevel = range[0]
            val maxLevel = range[1]

            when {
                filterVal < 0.4f -> {
                    // Low pass - reduce highs
                    val reduction = ((0.4f - filterVal) / 0.4f)
                    for (i in bandCount / 2 until bandCount) {
                        val level = (maxLevel - (reduction * (maxLevel - minLevel))).toInt().toShort()
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
                filterVal > 0.6f -> {
                    // High pass - reduce lows
                    val reduction = ((filterVal - 0.6f) / 0.4f)
                    for (i in 0 until bandCount / 2) {
                        val level = (maxLevel - (reduction * (maxLevel - minLevel))).toInt().toShort()
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
                else -> {
                    // Neutral - restore EQ
                    applyEqA()
                }
            }
        }
    }

    private fun applyFilterB() {
        deckBEqualizer?.let { eq ->
            val filterVal = _deckBState.value.filter
            val bandCount = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minLevel = range[0]
            val maxLevel = range[1]

            when {
                filterVal < 0.4f -> {
                    val reduction = ((0.4f - filterVal) / 0.4f)
                    for (i in bandCount / 2 until bandCount) {
                        val level = (maxLevel - (reduction * (maxLevel - minLevel))).toInt().toShort()
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
                filterVal > 0.6f -> {
                    val reduction = ((filterVal - 0.6f) / 0.4f)
                    for (i in 0 until bandCount / 2) {
                        val level = (maxLevel - (reduction * (maxLevel - minLevel))).toInt().toShort()
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
                else -> {
                    applyEqB()
                }
            }
        }
    }

    // ==========================================
    // POSITION UPDATE
    // ==========================================

    private fun startPositionUpdateA() {
        deckAPositionJob?.cancel()
        deckAPositionJob = scope.launch {
            while (isActive) {
                deckAPlayer?.let { player ->
                    if (player.isPlaying) {
                        val position = player.currentPosition.toFloat() / player.duration
                        _deckAState.value = _deckAState.value.copy(
                            position = position,
                            currentPosition = player.currentPosition.toLong()
                        )

                        // Handle loop
                        if (_deckAState.value.loopEnabled) {
                            if (player.currentPosition >= _deckAState.value.loopEnd) {
                                player.seekTo(_deckAState.value.loopStart.toInt())
                            }
                        }
                    }
                }
                delay(30) // Faster update for smoother waveform
            }
        }
    }

    private fun startPositionUpdateB() {
        deckBPositionJob?.cancel()
        deckBPositionJob = scope.launch {
            while (isActive) {
                deckBPlayer?.let { player ->
                    if (player.isPlaying) {
                        val position = player.currentPosition.toFloat() / player.duration
                        _deckBState.value = _deckBState.value.copy(
                            position = position,
                            currentPosition = player.currentPosition.toLong()
                        )

                        if (_deckBState.value.loopEnabled) {
                            if (player.currentPosition >= _deckBState.value.loopEnd) {
                                player.seekTo(_deckBState.value.loopStart.toInt())
                            }
                        }
                    }
                }
                delay(30)
            }
        }
    }

    // ==========================================
    // HAPTIC FEEDBACK
    // ==========================================

    private fun hapticFeedback(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }

    // ==========================================
    // CLEANUP
    // ==========================================

    fun release() {
        deckAPositionJob?.cancel()
        deckBPositionJob?.cancel()
        waveformUpdateJob?.cancel()

        releaseEffectsA()
        releaseEffectsB()

        deckAPlayer?.release()
        deckBPlayer?.release()

        soundPool?.release()

        scope.cancel()
    }
}

// ==========================================
// DATA CLASSES
// ==========================================

data class DeviceTrackPro(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val path: String,
    val albumArtUri: String = "",
    val bpm: Float = 120f,
    val key: String = "Am"
)

data class DeckStatePro(
    val isLoaded: Boolean = false,
    val isPlaying: Boolean = false,
    val isScratching: Boolean = false,
    val scratchVelocity: Float = 0f,
    val track: DeviceTrackPro? = null,
    val duration: Long = 0L,
    val position: Float = 0f,
    val currentPosition: Long = 0L,
    val volume: Float = 0.8f,
    val gain: Float = 0f,
    val bpm: Float = 120f,
    val key: String = "Am",
    val eqHigh: Float = 0.5f,
    val eqMid: Float = 0.5f,
    val eqLow: Float = 0.5f,
    val killHigh: Boolean = false,
    val killMid: Boolean = false,
    val killLow: Boolean = false,
    val filter: Float = 0.5f,
    val cuePoint: Long = 0L,
    val hotCues: List<Long> = listOf(-1, -1, -1, -1, -1, -1, -1, -1),
    val loopEnabled: Boolean = false,
    val loopStart: Long = 0L,
    val loopEnd: Long = 0L,
    val loopBars: Int = 4,
    val isSynced: Boolean = false
)

data class MixerState(
    val masterVolume: Float = 0.8f,
    val crossfader: Float = 0.5f,
    val headphoneVolume: Float = 0.7f,
    val headphoneMix: Float = 0.5f,
    val headphoneCueA: Boolean = false,
    val headphoneCueB: Boolean = false
)

data class FXState(
    val echoEnabled: Boolean = false,
    val reverbEnabled: Boolean = false,
    val flangerEnabled: Boolean = false,
    val phaserEnabled: Boolean = false,
    val filterEnabled: Boolean = false,
    val bitcrushEnabled: Boolean = false,
    val stutterEnabled: Boolean = false,
    val gateEnabled: Boolean = false,
    val wetDry: Float = 0.5f,
    val param1: Float = 0.5f,
    val param2: Float = 0.5f,
    val beatSync: Int = 4
)

enum class FXType {
    ECHO, REVERB, FLANGER, PHASER, FILTER, BITCRUSH, STUTTER, GATE
}

