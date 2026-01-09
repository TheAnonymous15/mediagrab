package com.example.dwn.player.audio

import android.content.Context
import android.media.audiofx.*
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

private const val TAG = "SpatialFX"

/**
 * SPATIAL & TIME FX - Feature Set 4
 *
 * - Reverb (algorithmic + convolution)
 * - Delay (digital, tape, ping-pong)
 * - Echo
 * - Stereo Widener
 * - Haas Effect
 * - Binaural / Spatial Audio
 * - Headphone Virtualization
 */

// ============================================
// REVERB
// ============================================

data class ReverbSettings(
    val isEnabled: Boolean = false,
    val preset: ReverbPreset = ReverbPreset.NONE,
    val roomSize: Float = 0.5f,        // 0-1
    val damping: Float = 0.5f,         // 0-1 (high freq absorption)
    val wetDryMix: Float = 0.3f,       // 0-1
    val preDelay: Float = 20f,         // ms
    val decay: Float = 1.5f,           // seconds
    val diffusion: Float = 0.8f,       // 0-1
    val density: Float = 0.8f          // 0-1
)

enum class ReverbPreset(val label: String, val androidPreset: Short?) {
    NONE("None", PresetReverb.PRESET_NONE),
    SMALL_ROOM("Small Room", PresetReverb.PRESET_SMALLROOM),
    MEDIUM_ROOM("Medium Room", PresetReverb.PRESET_MEDIUMROOM),
    LARGE_ROOM("Large Room", PresetReverb.PRESET_LARGEROOM),
    MEDIUM_HALL("Medium Hall", PresetReverb.PRESET_MEDIUMHALL),
    LARGE_HALL("Large Hall", PresetReverb.PRESET_LARGEHALL),
    PLATE("Plate", PresetReverb.PRESET_PLATE),
    CATHEDRAL("Cathedral", null),
    STADIUM("Stadium", null),
    CAVE("Cave", null),
    CUSTOM("Custom", null)
}

class ReverbProcessor(private val context: Context) {

    private var presetReverb: PresetReverb? = null
    private var environmentalReverb: EnvironmentalReverb? = null

    private val _settings = MutableStateFlow(ReverbSettings())
    val settings: StateFlow<ReverbSettings> = _settings.asStateFlow()

    fun initialize(audioSessionId: Int) {
        try {
            presetReverb = PresetReverb(0, audioSessionId)

            try {
                environmentalReverb = EnvironmentalReverb(0, audioSessionId)
            } catch (e: Exception) {
                Log.w(TAG, "Environmental reverb not available")
            }

            Log.d(TAG, "Reverb initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize reverb", e)
        }
    }

    fun setPreset(preset: ReverbPreset) {
        _settings.value = _settings.value.copy(preset = preset)

        preset.androidPreset?.let { androidPreset ->
            try {
                presetReverb?.preset = androidPreset
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set preset", e)
            }
        }

        // Apply custom settings for non-Android presets
        when (preset) {
            ReverbPreset.CATHEDRAL -> {
                updateSettings(roomSize = 0.95f, decay = 4f, damping = 0.3f)
            }
            ReverbPreset.STADIUM -> {
                updateSettings(roomSize = 1f, decay = 6f, damping = 0.2f)
            }
            ReverbPreset.CAVE -> {
                updateSettings(roomSize = 0.8f, decay = 3f, damping = 0.6f, diffusion = 0.3f)
            }
            else -> {}
        }
    }

    fun updateSettings(
        enabled: Boolean? = null,
        roomSize: Float? = null,
        damping: Float? = null,
        wetDryMix: Float? = null,
        preDelay: Float? = null,
        decay: Float? = null,
        diffusion: Float? = null,
        density: Float? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            roomSize = roomSize?.coerceIn(0f, 1f) ?: _settings.value.roomSize,
            damping = damping?.coerceIn(0f, 1f) ?: _settings.value.damping,
            wetDryMix = wetDryMix?.coerceIn(0f, 1f) ?: _settings.value.wetDryMix,
            preDelay = preDelay?.coerceIn(0f, 200f) ?: _settings.value.preDelay,
            decay = decay?.coerceIn(0.1f, 10f) ?: _settings.value.decay,
            diffusion = diffusion?.coerceIn(0f, 1f) ?: _settings.value.diffusion,
            density = density?.coerceIn(0f, 1f) ?: _settings.value.density
        )

        presetReverb?.enabled = _settings.value.isEnabled
        environmentalReverb?.enabled = _settings.value.isEnabled

        // Apply to environmental reverb if available
        environmentalReverb?.let { reverb ->
            try {
                val settings = _settings.value
                reverb.decayTime = (settings.decay * 1000).toInt()
                reverb.roomLevel = ((settings.roomSize * 2000) - 1000).toInt().toShort()
                reverb.reverbLevel = ((settings.wetDryMix * 2000) - 1000).toInt().toShort()
                reverb.diffusion = (settings.diffusion * 1000).toInt().toShort()
                reverb.density = (settings.density * 1000).toInt().toShort()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update environmental reverb", e)
            }
        }
    }

    fun release() {
        presetReverb?.release()
        environmentalReverb?.release()
        presetReverb = null
        environmentalReverb = null
    }
}

// ============================================
// DELAY
// ============================================

data class DelaySettings(
    val isEnabled: Boolean = false,
    val delayType: DelayType = DelayType.DIGITAL,
    val timeMs: Float = 250f,          // ms
    val feedback: Float = 0.4f,        // 0-1
    val wetDryMix: Float = 0.3f,       // 0-1
    val syncToBpm: Boolean = false,
    val bpm: Float = 120f,
    val noteValue: NoteValue = NoteValue.QUARTER,
    val highCut: Float = 8000f,        // Hz (for tape delay)
    val lowCut: Float = 100f,          // Hz
    val modulation: Float = 0f,        // 0-1 (for analog feel)
    val stereoSpread: Float = 0f       // 0-1 (for ping-pong)
)

enum class DelayType(val label: String) {
    DIGITAL("Digital"),
    TAPE("Tape"),
    ANALOG("Analog"),
    PING_PONG("Ping-Pong"),
    MULTI_TAP("Multi-Tap"),
    REVERSE("Reverse")
}

enum class NoteValue(val label: String, val multiplier: Float) {
    WHOLE("1/1", 4f),
    HALF("1/2", 2f),
    QUARTER("1/4", 1f),
    EIGHTH("1/8", 0.5f),
    SIXTEENTH("1/16", 0.25f),
    DOTTED_QUARTER("1/4.", 1.5f),
    DOTTED_EIGHTH("1/8.", 0.75f),
    TRIPLET_QUARTER("1/4T", 0.667f),
    TRIPLET_EIGHTH("1/8T", 0.333f)
}

class DelayProcessor {

    private val _settings = MutableStateFlow(DelaySettings())
    val settings: StateFlow<DelaySettings> = _settings.asStateFlow()

    // Delay buffers
    private var delayBufferL = FloatArray(0)
    private var delayBufferR = FloatArray(0)
    private var writePos = 0

    fun updateSettings(
        enabled: Boolean? = null,
        delayType: DelayType? = null,
        timeMs: Float? = null,
        feedback: Float? = null,
        wetDryMix: Float? = null,
        syncToBpm: Boolean? = null,
        bpm: Float? = null,
        noteValue: NoteValue? = null,
        highCut: Float? = null,
        lowCut: Float? = null,
        modulation: Float? = null,
        stereoSpread: Float? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            delayType = delayType ?: _settings.value.delayType,
            timeMs = timeMs?.coerceIn(1f, 2000f) ?: _settings.value.timeMs,
            feedback = feedback?.coerceIn(0f, 0.95f) ?: _settings.value.feedback,
            wetDryMix = wetDryMix?.coerceIn(0f, 1f) ?: _settings.value.wetDryMix,
            syncToBpm = syncToBpm ?: _settings.value.syncToBpm,
            bpm = bpm?.coerceIn(20f, 300f) ?: _settings.value.bpm,
            noteValue = noteValue ?: _settings.value.noteValue,
            highCut = highCut?.coerceIn(1000f, 20000f) ?: _settings.value.highCut,
            lowCut = lowCut?.coerceIn(20f, 2000f) ?: _settings.value.lowCut,
            modulation = modulation?.coerceIn(0f, 1f) ?: _settings.value.modulation,
            stereoSpread = stereoSpread?.coerceIn(0f, 1f) ?: _settings.value.stereoSpread
        )
    }

    fun getActualDelayMs(): Float {
        val settings = _settings.value
        return if (settings.syncToBpm) {
            val beatDuration = 60000f / settings.bpm
            beatDuration * settings.noteValue.multiplier
        } else {
            settings.timeMs
        }
    }
}

// ============================================
// STEREO WIDENER
// ============================================

data class StereoWidenerSettings(
    val isEnabled: Boolean = false,
    val width: Float = 1f,             // 0 = mono, 1 = normal, 2 = extra wide
    val midGain: Float = 0f,           // dB
    val sideGain: Float = 0f,          // dB
    val bassWidth: Float = 1f,         // Low freq width
    val crossoverFreq: Float = 300f    // Hz
)

class StereoWidener {

    private val _settings = MutableStateFlow(StereoWidenerSettings())
    val settings: StateFlow<StereoWidenerSettings> = _settings.asStateFlow()

    fun updateSettings(
        enabled: Boolean? = null,
        width: Float? = null,
        midGain: Float? = null,
        sideGain: Float? = null,
        bassWidth: Float? = null,
        crossoverFreq: Float? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            width = width?.coerceIn(0f, 3f) ?: _settings.value.width,
            midGain = midGain?.coerceIn(-12f, 12f) ?: _settings.value.midGain,
            sideGain = sideGain?.coerceIn(-12f, 12f) ?: _settings.value.sideGain,
            bassWidth = bassWidth?.coerceIn(0f, 2f) ?: _settings.value.bassWidth,
            crossoverFreq = crossoverFreq?.coerceIn(50f, 500f) ?: _settings.value.crossoverFreq
        )
    }

    fun processStereo(left: FloatArray, right: FloatArray): Pair<FloatArray, FloatArray> {
        if (!_settings.value.isEnabled) return left to right

        val settings = _settings.value
        val outL = FloatArray(left.size)
        val outR = FloatArray(right.size)

        for (i in left.indices) {
            // Mid/Side encoding
            val mid = (left[i] + right[i]) * 0.5f
            val side = (left[i] - right[i]) * 0.5f

            // Apply width
            val wideSide = side * settings.width

            // Apply gains
            val midGain = 10f.pow(settings.midGain / 20f)
            val sideGain = 10f.pow(settings.sideGain / 20f)

            val processedMid = mid * midGain
            val processedSide = wideSide * sideGain

            // M/S decoding
            outL[i] = processedMid + processedSide
            outR[i] = processedMid - processedSide
        }

        return outL to outR
    }
}

// ============================================
// VIRTUALIZER / 3D AUDIO
// ============================================

data class VirtualizerSettings(
    val isEnabled: Boolean = false,
    val strength: Int = 500,           // 0-1000
    val speakerAngle: Float = 90f,     // degrees (virtual speaker placement)
    val roomSize: Float = 0.5f,        // 0-1
    val headTrackingEnabled: Boolean = false,
    val binauralMode: BinauralMode = BinauralMode.STANDARD
)

enum class BinauralMode(val label: String) {
    STANDARD("Standard"),
    HRTF_GENERIC("HRTF Generic"),
    HRTF_PERSONALIZED("HRTF Personalized"),
    CROSSFEED("Crossfeed"),
    SURROUND_5_1("5.1 Surround"),
    SURROUND_7_1("7.1 Surround")
}

class VirtualizerProcessor(private val context: Context) {

    private var virtualizer: Virtualizer? = null

    private val _settings = MutableStateFlow(VirtualizerSettings())
    val settings: StateFlow<VirtualizerSettings> = _settings.asStateFlow()

    fun initialize(audioSessionId: Int) {
        try {
            virtualizer = Virtualizer(0, audioSessionId)
            Log.d(TAG, "Virtualizer initialized, strength supported: ${virtualizer?.strengthSupported}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize virtualizer", e)
        }
    }

    fun updateSettings(
        enabled: Boolean? = null,
        strength: Int? = null,
        speakerAngle: Float? = null,
        roomSize: Float? = null,
        headTracking: Boolean? = null,
        binauralMode: BinauralMode? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            strength = strength?.coerceIn(0, 1000) ?: _settings.value.strength,
            speakerAngle = speakerAngle?.coerceIn(0f, 180f) ?: _settings.value.speakerAngle,
            roomSize = roomSize?.coerceIn(0f, 1f) ?: _settings.value.roomSize,
            headTrackingEnabled = headTracking ?: _settings.value.headTrackingEnabled,
            binauralMode = binauralMode ?: _settings.value.binauralMode
        )

        virtualizer?.let { v ->
            v.enabled = _settings.value.isEnabled
            if (v.strengthSupported) {
                v.setStrength(_settings.value.strength.toShort())
            }
        }
    }

    fun release() {
        virtualizer?.release()
        virtualizer = null
    }
}

// ============================================
// HAAS EFFECT (Precedence Effect)
// ============================================

data class HaasEffectSettings(
    val isEnabled: Boolean = false,
    val delayMs: Float = 20f,          // 1-40ms typical
    val delayChannel: DelayChannel = DelayChannel.RIGHT,
    val wetDryMix: Float = 0.5f
)

enum class DelayChannel { LEFT, RIGHT }

class HaasEffect {

    private val _settings = MutableStateFlow(HaasEffectSettings())
    val settings: StateFlow<HaasEffectSettings> = _settings.asStateFlow()

    fun updateSettings(
        enabled: Boolean? = null,
        delayMs: Float? = null,
        delayChannel: DelayChannel? = null,
        wetDryMix: Float? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            delayMs = delayMs?.coerceIn(1f, 50f) ?: _settings.value.delayMs,
            delayChannel = delayChannel ?: _settings.value.delayChannel,
            wetDryMix = wetDryMix?.coerceIn(0f, 1f) ?: _settings.value.wetDryMix
        )
    }
}

// ============================================
// BASS BOOST PROCESSOR
// ============================================

data class BassBoostSettings(
    val isEnabled: Boolean = false,
    val strength: Int = 0              // 0-1000
)

class BassBoostProcessor(private val context: Context) {

    private var bassBoost: BassBoost? = null

    private val _settings = MutableStateFlow(BassBoostSettings())
    val settings: StateFlow<BassBoostSettings> = _settings.asStateFlow()

    fun initialize(audioSessionId: Int) {
        try {
            bassBoost = BassBoost(0, audioSessionId)
            Log.d(TAG, "BassBoost initialized, strength supported: ${bassBoost?.strengthSupported}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize BassBoost", e)
        }
    }

    fun updateSettings(
        enabled: Boolean? = null,
        strength: Int? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            strength = strength?.coerceIn(0, 1000) ?: _settings.value.strength
        )

        bassBoost?.let { bb ->
            bb.enabled = _settings.value.isEnabled
            if (bb.strengthSupported) {
                bb.setStrength(_settings.value.strength.toShort())
            }
        }
    }

    fun release() {
        bassBoost?.release()
        bassBoost = null
    }
}

// ============================================
// LOUDNESS ENHANCER PROCESSOR
// ============================================

data class LoudnessEnhancerSettings(
    val isEnabled: Boolean = false,
    val gainMb: Int = 0                // Gain in millibels (100 mb = 1 dB)
)

class LoudnessEnhancerProcessor(private val context: Context) {

    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _settings = MutableStateFlow(LoudnessEnhancerSettings())
    val settings: StateFlow<LoudnessEnhancerSettings> = _settings.asStateFlow()

    fun initialize(audioSessionId: Int) {
        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            Log.d(TAG, "LoudnessEnhancer initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LoudnessEnhancer", e)
        }
    }

    fun updateSettings(
        enabled: Boolean? = null,
        gainMb: Int? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            gainMb = gainMb?.coerceIn(0, 3000) ?: _settings.value.gainMb  // Max ~30dB
        )

        loudnessEnhancer?.let { le ->
            le.enabled = _settings.value.isEnabled
            le.setTargetGain(_settings.value.gainMb)
        }
    }

    fun release() {
        loudnessEnhancer?.release()
        loudnessEnhancer = null
    }
}

