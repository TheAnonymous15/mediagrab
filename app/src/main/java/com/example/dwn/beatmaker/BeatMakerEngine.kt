package com.example.dwn.beatmaker

import android.content.Context
import android.media.*
import android.media.audiofx.*
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

/**
 * SUPER NEXT-GEN BEAT MAKER ENGINE
 * Professional-grade audio-first beat creation engine
 */
class BeatMakerEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Audio configuration
    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    // Main audio track for playback
    private var masterAudioTrack: AudioTrack? = null

    // Sound pools for instruments
    private var drumSoundPool: SoundPool? = null
    private var synthSoundPool: SoundPool? = null

    // Loaded sound IDs
    private val loadedSounds = mutableMapOf<String, Int>()

    // State flows
    private val _projectState = MutableStateFlow(BeatProject())
    val projectState: StateFlow<BeatProject> = _projectState.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _mixerState = MutableStateFlow(MixerState())
    val mixerState: StateFlow<MixerState> = _mixerState.asStateFlow()

    // Sequencer state
    private var sequencerJob: Job? = null
    private var currentStep = 0
    private var isPlaying = false

    // Recording state
    private var audioRecorder: AudioRecord? = null
    private var isRecording = false
    private var recordingBuffer = mutableListOf<Short>()

    init {
        initSoundPools()
        initMasterAudio()
    }

    private fun initSoundPools() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        drumSoundPool = SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(audioAttributes)
            .build()

        synthSoundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load default drum kit sounds (generated programmatically)
        loadDefaultDrumKit()
    }

    private fun initMasterAudio() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        masterAudioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun loadDefaultDrumKit() {
        // In a real app, these would be actual audio files
        // For now, we'll generate synthetic sounds
        scope.launch {
            // Placeholder for drum sound loading
            // The sounds will be generated synthetically when played
        }
    }

    // ==========================================
    // PROJECT MANAGEMENT
    // ==========================================

    fun newProject(name: String = "Untitled Beat", bpm: Int = 120, timeSignature: TimeSignature = TimeSignature.FOUR_FOUR) {
        _projectState.value = BeatProject(
            name = name,
            bpm = bpm,
            timeSignature = timeSignature,
            tracks = listOf(
                Track(id = "drums", name = "Drums", type = TrackType.DRUMS, color = 0xFFFF6B6B.toInt()),
                Track(id = "bass", name = "Bass", type = TrackType.BASS, color = 0xFF4ECDC4.toInt()),
                Track(id = "synth", name = "Synth", type = TrackType.SYNTH, color = 0xFFFFE66D.toInt()),
                Track(id = "pad", name = "Pad", type = TrackType.AMBIENT, color = 0xFFAA66FF.toInt())
            )
        )
    }

    fun setProjectName(name: String) {
        _projectState.value = _projectState.value.copy(name = name)
    }

    fun setBPM(bpm: Int) {
        _projectState.value = _projectState.value.copy(bpm = bpm.coerceIn(40, 300))
    }

    fun setSwing(swing: Float) {
        _projectState.value = _projectState.value.copy(swing = swing.coerceIn(0f, 1f))
    }

    // ==========================================
    // TRACK MANAGEMENT
    // ==========================================

    fun addTrack(type: TrackType, name: String? = null): String {
        val id = "track_${System.currentTimeMillis()}"
        val trackName = name ?: "${type.name.lowercase().replaceFirstChar { it.uppercase() }} ${_projectState.value.tracks.size + 1}"
        val color = when (type) {
            TrackType.DRUMS -> 0xFFFF6B6B.toInt()
            TrackType.BASS -> 0xFF4ECDC4.toInt()
            TrackType.SYNTH -> 0xFFFFE66D.toInt()
            TrackType.KEYS -> 0xFF66B2FF.toInt()
            TrackType.GUITAR -> 0xFFFF9F43.toInt()
            TrackType.VOICE -> 0xFFFF66B2.toInt()
            TrackType.AMBIENT -> 0xFFAA66FF.toInt()
            TrackType.SAMPLE -> 0xFF66FFB2.toInt()
        }

        val newTrack = Track(
            id = id,
            name = trackName,
            type = type,
            color = color
        )

        _projectState.value = _projectState.value.copy(
            tracks = _projectState.value.tracks + newTrack
        )

        return id
    }

    fun removeTrack(trackId: String) {
        _projectState.value = _projectState.value.copy(
            tracks = _projectState.value.tracks.filter { it.id != trackId }
        )
    }

    fun setTrackVolume(trackId: String, volume: Float) {
        updateTrack(trackId) { it.copy(volume = volume.coerceIn(0f, 1f)) }
    }

    fun setTrackPan(trackId: String, pan: Float) {
        updateTrack(trackId) { it.copy(pan = pan.coerceIn(-1f, 1f)) }
    }

    fun setTrackMute(trackId: String, muted: Boolean) {
        updateTrack(trackId) { it.copy(isMuted = muted) }
    }

    fun setTrackSolo(trackId: String, solo: Boolean) {
        updateTrack(trackId) { it.copy(isSolo = solo) }
    }

    private fun updateTrack(trackId: String, update: (Track) -> Track) {
        _projectState.value = _projectState.value.copy(
            tracks = _projectState.value.tracks.map {
                if (it.id == trackId) update(it) else it
            }
        )
    }

    // ==========================================
    // PATTERN / STEP SEQUENCER
    // ==========================================

    fun setStep(trackId: String, patternIndex: Int, step: Int, note: Note?) {
        updateTrack(trackId) { track ->
            val patterns = track.patterns.toMutableList()
            while (patterns.size <= patternIndex) {
                patterns.add(Pattern())
            }

            val pattern = patterns[patternIndex]
            val steps = pattern.steps.toMutableList()
            while (steps.size <= step) {
                steps.add(null)
            }
            steps[step] = note

            patterns[patternIndex] = pattern.copy(steps = steps)
            track.copy(patterns = patterns)
        }
    }

    fun toggleStep(trackId: String, patternIndex: Int, step: Int, defaultNote: Note = Note()) {
        val track = _projectState.value.tracks.find { it.id == trackId } ?: return
        val pattern = track.patterns.getOrNull(patternIndex)
        val currentNote = pattern?.steps?.getOrNull(step)

        setStep(trackId, patternIndex, step, if (currentNote == null) defaultNote else null)
    }

    fun setStepVelocity(trackId: String, patternIndex: Int, step: Int, velocity: Float) {
        updateTrack(trackId) { track ->
            val patterns = track.patterns.toMutableList()
            if (patternIndex < patterns.size) {
                val pattern = patterns[patternIndex]
                val steps = pattern.steps.toMutableList()
                if (step < steps.size && steps[step] != null) {
                    steps[step] = steps[step]!!.copy(velocity = velocity.coerceIn(0f, 1f))
                    patterns[patternIndex] = pattern.copy(steps = steps)
                }
            }
            track.copy(patterns = patterns)
        }
    }

    fun setPatternLength(trackId: String, patternIndex: Int, length: Int) {
        updateTrack(trackId) { track ->
            val patterns = track.patterns.toMutableList()
            while (patterns.size <= patternIndex) {
                patterns.add(Pattern())
            }
            patterns[patternIndex] = patterns[patternIndex].copy(length = length.coerceIn(4, 64))
            track.copy(patterns = patterns)
        }
    }

    // ==========================================
    // PIANO ROLL
    // ==========================================

    fun addPianoRollNote(trackId: String, patternIndex: Int, note: PianoRollNote) {
        updateTrack(trackId) { track ->
            val patterns = track.patterns.toMutableList()
            while (patterns.size <= patternIndex) {
                patterns.add(Pattern())
            }
            val pattern = patterns[patternIndex]
            patterns[patternIndex] = pattern.copy(
                pianoRollNotes = pattern.pianoRollNotes + note
            )
            track.copy(patterns = patterns)
        }
    }

    fun removePianoRollNote(trackId: String, patternIndex: Int, noteId: String) {
        updateTrack(trackId) { track ->
            val patterns = track.patterns.toMutableList()
            if (patternIndex < patterns.size) {
                val pattern = patterns[patternIndex]
                patterns[patternIndex] = pattern.copy(
                    pianoRollNotes = pattern.pianoRollNotes.filter { it.id != noteId }
                )
            }
            track.copy(patterns = patterns)
        }
    }

    // ==========================================
    // PLAYBACK CONTROL
    // ==========================================

    fun play() {
        if (isPlaying) return
        isPlaying = true

        _playbackState.value = _playbackState.value.copy(isPlaying = true)

        sequencerJob = scope.launch {
            val project = _projectState.value
            val msPerStep = (60000.0 / project.bpm / 4).toLong() // 16th notes

            while (isActive && isPlaying) {
                val stepStartTime = System.currentTimeMillis()

                // Apply swing
                val swingDelay = if (currentStep % 2 == 1) {
                    (msPerStep * project.swing * 0.5).toLong()
                } else 0L

                if (swingDelay > 0) delay(swingDelay)

                // Trigger sounds for current step
                triggerStep(currentStep)

                // Update playback position
                _playbackState.value = _playbackState.value.copy(
                    currentStep = currentStep,
                    currentBeat = currentStep / 4,
                    currentBar = currentStep / 16
                )

                // Wait for next step
                val elapsed = System.currentTimeMillis() - stepStartTime
                val waitTime = msPerStep - elapsed - swingDelay
                if (waitTime > 0) delay(waitTime)

                // Advance step
                currentStep = (currentStep + 1) % getPatternLength()
            }
        }
    }

    fun pause() {
        isPlaying = false
        sequencerJob?.cancel()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun stop() {
        pause()
        currentStep = 0
        _playbackState.value = _playbackState.value.copy(
            currentStep = 0,
            currentBeat = 0,
            currentBar = 0
        )
    }

    fun togglePlayPause() {
        if (isPlaying) pause() else play()
    }

    fun seekToStep(step: Int) {
        currentStep = step.coerceIn(0, getPatternLength() - 1)
        _playbackState.value = _playbackState.value.copy(
            currentStep = currentStep,
            currentBeat = currentStep / 4,
            currentBar = currentStep / 16
        )
    }

    private fun getPatternLength(): Int {
        val project = _projectState.value
        return project.tracks.maxOfOrNull { track ->
            track.patterns.firstOrNull()?.length ?: 16
        } ?: 16
    }

    private fun triggerStep(step: Int) {
        val project = _projectState.value
        val hasSolo = project.tracks.any { it.isSolo }

        project.tracks.forEach { track ->
            // Skip if muted or if there's a solo track and this isn't it
            if (track.isMuted) return@forEach
            if (hasSolo && !track.isSolo) return@forEach

            val pattern = track.patterns.firstOrNull() ?: return@forEach
            val note = pattern.steps.getOrNull(step % pattern.length) ?: return@forEach

            // Play the note
            playNote(track, note)
        }
    }

    private fun playNote(track: Track, note: Note) {
        val volume = track.volume * note.velocity * _mixerState.value.masterVolume
        val leftVol = volume * (1f - track.pan.coerceAtLeast(0f))
        val rightVol = volume * (1f + track.pan.coerceAtMost(0f))

        // Use SoundPool for playback
        when (track.type) {
            TrackType.DRUMS -> playDrumSound(note.pitch, leftVol, rightVol)
            TrackType.BASS -> playSynthSound(note.pitch, leftVol, rightVol, "bass")
            TrackType.SYNTH -> playSynthSound(note.pitch, leftVol, rightVol, "synth")
            TrackType.KEYS -> playSynthSound(note.pitch, leftVol, rightVol, "keys")
            else -> playSynthSound(note.pitch, leftVol, rightVol, "default")
        }
    }

    private fun playDrumSound(pitch: Int, leftVol: Float, rightVol: Float) {
        // Generate and play a synthetic drum sound
        scope.launch(Dispatchers.IO) {
            val sound = generateDrumSound(pitch)
            playGeneratedSound(sound, leftVol, rightVol)
        }
    }

    private fun playSynthSound(pitch: Int, leftVol: Float, rightVol: Float, type: String) {
        scope.launch(Dispatchers.IO) {
            val sound = generateSynthSound(pitch, type)
            playGeneratedSound(sound, leftVol, rightVol)
        }
    }

    private fun generateDrumSound(pitch: Int): ShortArray {
        val duration = 0.15 // seconds
        val samples = (sampleRate * duration).toInt()
        val sound = ShortArray(samples)

        when (pitch % 4) {
            0 -> { // Kick
                for (i in 0 until samples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 60.0 * exp(-t * 30)
                    val envelope = exp(-t * 15)
                    val sample = sin(2 * PI * freq * t) * envelope
                    sound[i] = (sample * 32767 * 0.8).toInt().toShort()
                }
            }
            1 -> { // Snare
                for (i in 0 until samples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = exp(-t * 20)
                    val tone = sin(2 * PI * 200 * t) * 0.5
                    val noise = (Math.random() * 2 - 1) * 0.5
                    val sample = (tone + noise) * envelope
                    sound[i] = (sample * 32767 * 0.7).toInt().toShort()
                }
            }
            2 -> { // Hi-hat closed
                for (i in 0 until samples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = exp(-t * 50)
                    val noise = (Math.random() * 2 - 1)
                    sound[i] = (noise * envelope * 32767 * 0.4).toInt().toShort()
                }
            }
            3 -> { // Hi-hat open
                for (i in 0 until samples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = exp(-t * 10)
                    val noise = (Math.random() * 2 - 1)
                    sound[i] = (noise * envelope * 32767 * 0.35).toInt().toShort()
                }
            }
        }

        return sound
    }

    private fun generateSynthSound(pitch: Int, type: String): ShortArray {
        val duration = 0.3 // seconds
        val samples = (sampleRate * duration).toInt()
        val sound = ShortArray(samples)

        // Convert MIDI pitch to frequency
        val freq = 440.0 * 2.0.pow((pitch - 69) / 12.0)

        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = when {
                t < 0.01 -> t / 0.01 // Attack
                t < 0.05 -> 1.0 - (t - 0.01) / 0.04 * 0.3 // Decay
                t < duration - 0.1 -> 0.7 // Sustain
                else -> 0.7 * (duration - t) / 0.1 // Release
            }

            val sample = when (type) {
                "bass" -> sin(2 * PI * freq * t) * 0.7 + sin(4 * PI * freq * t) * 0.3
                "synth" -> {
                    val saw = (2 * (t * freq - floor(t * freq + 0.5)))
                    saw * 0.5 + sin(2 * PI * freq * t) * 0.5
                }
                "keys" -> sin(2 * PI * freq * t) * 0.6 + sin(4 * PI * freq * t) * 0.25 + sin(6 * PI * freq * t) * 0.15
                else -> sin(2 * PI * freq * t)
            }

            sound[i] = (sample * envelope * 32767 * 0.6).toInt().coerceIn(-32768, 32767).toShort()
        }

        return sound
    }

    private fun playGeneratedSound(sound: ShortArray, leftVol: Float, rightVol: Float) {
        try {
            val stereoSound = ShortArray(sound.size * 2)
            for (i in sound.indices) {
                stereoSound[i * 2] = (sound[i] * leftVol).toInt().toShort()
                stereoSound[i * 2 + 1] = (sound[i] * rightVol).toInt().toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(stereoSound.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(stereoSound, 0, stereoSound.size)
            audioTrack.play()

            // Release after playback
            scope.launch {
                delay(500)
                audioTrack.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================
    // MIXER CONTROL
    // ==========================================

    fun setMasterVolume(volume: Float) {
        _mixerState.value = _mixerState.value.copy(masterVolume = volume.coerceIn(0f, 1f))
    }

    fun setMasterEQ(band: Int, gain: Float) {
        val eq = _mixerState.value.masterEQ.toMutableList()
        while (eq.size <= band) eq.add(0f)
        eq[band] = gain.coerceIn(-12f, 12f)
        _mixerState.value = _mixerState.value.copy(masterEQ = eq)
    }

    // ==========================================
    // TRACK FX
    // ==========================================

    fun addTrackFX(trackId: String, fxType: FXType) {
        updateTrack(trackId) { track ->
            if (track.fxChain.size < 6) {
                val fx = TrackFX(
                    id = "fx_${System.currentTimeMillis()}",
                    type = fxType
                )
                track.copy(fxChain = track.fxChain + fx)
            } else track
        }
    }

    fun removeTrackFX(trackId: String, fxId: String) {
        updateTrack(trackId) { track ->
            track.copy(fxChain = track.fxChain.filter { it.id != fxId })
        }
    }

    fun setFXEnabled(trackId: String, fxId: String, enabled: Boolean) {
        updateTrack(trackId) { track ->
            track.copy(fxChain = track.fxChain.map { fx ->
                if (fx.id == fxId) fx.copy(isEnabled = enabled) else fx
            })
        }
    }

    fun setFXParameter(trackId: String, fxId: String, param: String, value: Float) {
        updateTrack(trackId) { track ->
            track.copy(fxChain = track.fxChain.map { fx ->
                if (fx.id == fxId) {
                    val params = fx.parameters.toMutableMap()
                    params[param] = value
                    fx.copy(parameters = params)
                } else fx
            })
        }
    }

    // ==========================================
    // PRESETS
    // ==========================================

    fun loadDrumPreset(preset: DrumPreset) {
        val drumTrack = _projectState.value.tracks.find { it.type == TrackType.DRUMS }
        if (drumTrack != null) {
            val pattern = Pattern(
                steps = preset.pattern.map { if (it) Note() else null },
                length = preset.pattern.size
            )
            updateTrack(drumTrack.id) { it.copy(patterns = listOf(pattern)) }
        }
    }

    fun getGenrePresets(): List<GenrePreset> = listOf(
        GenrePreset("Hip Hop", 90, listOf(true, false, false, false, true, false, false, false, true, false, false, false, true, false, true, false)),
        GenrePreset("House", 128, listOf(true, false, false, false, true, false, false, false, true, false, false, false, true, false, false, false)),
        GenrePreset("Trap", 140, listOf(true, false, false, true, false, false, true, false, false, true, false, false, true, false, false, true)),
        GenrePreset("Drum & Bass", 174, listOf(true, false, false, false, false, false, true, false, false, false, true, false, false, false, true, false)),
        GenrePreset("Lo-Fi", 85, listOf(true, false, false, false, true, false, false, true, false, false, true, false, true, false, false, false)),
        GenrePreset("R&B", 95, listOf(true, false, false, false, false, true, false, false, true, false, false, false, false, true, false, false)),
        GenrePreset("Reggaeton", 100, listOf(true, false, false, true, false, false, true, false, true, false, false, true, false, false, true, false)),
        GenrePreset("EDM", 130, listOf(true, false, true, false, true, false, true, false, true, false, true, false, true, false, true, false))
    )

    // ==========================================
    // CLEANUP
    // ==========================================

    fun release() {
        stop()
        scope.cancel()
        drumSoundPool?.release()
        synthSoundPool?.release()
        masterAudioTrack?.release()
        audioRecorder?.release()
    }
}

// ==========================================
// DATA CLASSES
// ==========================================

data class BeatProject(
    val id: String = "project_${System.currentTimeMillis()}",
    val name: String = "Untitled Beat",
    val bpm: Int = 120,
    val timeSignature: TimeSignature = TimeSignature.FOUR_FOUR,
    val swing: Float = 0f,
    val key: MusicalKey = MusicalKey.C_MAJOR,
    val tracks: List<Track> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

data class Track(
    val id: String,
    val name: String,
    val type: TrackType,
    val color: Int,
    val volume: Float = 0.8f,
    val pan: Float = 0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val patterns: List<Pattern> = listOf(Pattern()),
    val fxChain: List<TrackFX> = emptyList(),
    val isExpanded: Boolean = true
)

data class Pattern(
    val id: String = "pattern_${System.currentTimeMillis()}",
    val name: String = "Pattern 1",
    val steps: List<Note?> = List(16) { null },
    val pianoRollNotes: List<PianoRollNote> = emptyList(),
    val length: Int = 16
)

data class Note(
    val pitch: Int = 60, // MIDI pitch (60 = C4)
    val velocity: Float = 0.8f,
    val duration: Float = 1f // In steps
)

data class PianoRollNote(
    val id: String = "note_${System.currentTimeMillis()}",
    val pitch: Int,
    val startStep: Float,
    val duration: Float,
    val velocity: Float = 0.8f
)

data class TrackFX(
    val id: String,
    val type: FXType,
    val isEnabled: Boolean = true,
    val parameters: Map<String, Float> = getDefaultParams(type)
)

enum class TrackType {
    DRUMS, BASS, SYNTH, KEYS, GUITAR, VOICE, AMBIENT, SAMPLE
}

enum class FXType {
    EQ, COMPRESSOR, LIMITER, REVERB, DELAY, CHORUS, FILTER, DISTORTION, AUTOTUNE
}

enum class TimeSignature(val beats: Int, val noteValue: Int) {
    THREE_FOUR(3, 4),
    FOUR_FOUR(4, 4),
    SIX_EIGHT(6, 8)
}

enum class MusicalKey {
    C_MAJOR, C_MINOR, D_MAJOR, D_MINOR, E_MAJOR, E_MINOR,
    F_MAJOR, F_MINOR, G_MAJOR, G_MINOR, A_MAJOR, A_MINOR,
    B_MAJOR, B_MINOR
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val isRecording: Boolean = false,
    val currentStep: Int = 0,
    val currentBeat: Int = 0,
    val currentBar: Int = 0,
    val loopStart: Int = 0,
    val loopEnd: Int = 16,
    val isLooping: Boolean = true
)

data class MixerState(
    val masterVolume: Float = 0.8f,
    val masterEQ: List<Float> = listOf(0f, 0f, 0f, 0f, 0f),
    val masterCompression: Float = 0f,
    val masterLimiter: Boolean = true
)

data class DrumPreset(
    val name: String,
    val pattern: List<Boolean>
)

data class GenrePreset(
    val name: String,
    val bpm: Int,
    val kickPattern: List<Boolean>
)

private fun getDefaultParams(type: FXType): Map<String, Float> = when (type) {
    FXType.EQ -> mapOf("low" to 0f, "mid" to 0f, "high" to 0f)
    FXType.COMPRESSOR -> mapOf("threshold" to -10f, "ratio" to 4f, "attack" to 10f, "release" to 100f)
    FXType.LIMITER -> mapOf("threshold" to -3f, "release" to 50f)
    FXType.REVERB -> mapOf("size" to 0.5f, "damping" to 0.5f, "mix" to 0.3f)
    FXType.DELAY -> mapOf("time" to 0.25f, "feedback" to 0.3f, "mix" to 0.3f)
    FXType.CHORUS -> mapOf("rate" to 0.5f, "depth" to 0.5f, "mix" to 0.3f)
    FXType.FILTER -> mapOf("cutoff" to 0.5f, "resonance" to 0.3f, "type" to 0f)
    FXType.DISTORTION -> mapOf("drive" to 0.3f, "tone" to 0.5f, "mix" to 0.5f)
    FXType.AUTOTUNE -> mapOf("speed" to 0.5f, "key" to 0f)
}

