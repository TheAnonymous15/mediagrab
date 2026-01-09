package com.example.dwn.player.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * HARMONIC & MODULATION FX - Feature Set 5
 *
 * - Distortion (tube, transistor, digital)
 * - Saturation (tape, analog)
 * - Exciter (harmonic enhancement)
 * - Chorus
 * - Flanger
 * - Phaser
 * - Tremolo
 * - Auto-pan
 * - Bitcrusher
 */

// ============================================
// DISTORTION
// ============================================

data class DistortionSettings(
    val isEnabled: Boolean = false,
    val type: DistortionType = DistortionType.TUBE,
    val drive: Float = 0.5f,           // 0-1
    val tone: Float = 0.5f,            // 0-1 (low-high)
    val mix: Float = 0.5f,             // 0-1
    val outputLevel: Float = 0f        // dB
)

enum class DistortionType(val label: String) {
    TUBE("Tube"),
    TRANSISTOR("Transistor"),
    DIGITAL("Digital"),
    FUZZ("Fuzz"),
    OVERDRIVE("Overdrive"),
    RECTIFIER("Rectifier")
}

class Distortion {

    private val _settings = MutableStateFlow(DistortionSettings())
    val settings: StateFlow<DistortionSettings> = _settings.asStateFlow()

    fun updateSettings(
        enabled: Boolean? = null,
        type: DistortionType? = null,
        drive: Float? = null,
        tone: Float? = null,
        mix: Float? = null,
        outputLevel: Float? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            type = type ?: _settings.value.type,
            drive = drive?.coerceIn(0f, 1f) ?: _settings.value.drive,
            tone = tone?.coerceIn(0f, 1f) ?: _settings.value.tone,
            mix = mix?.coerceIn(0f, 1f) ?: _settings.value.mix,
            outputLevel = outputLevel?.coerceIn(-24f, 12f) ?: _settings.value.outputLevel
        )
    }

    fun process(input: FloatArray): FloatArray {
        if (!_settings.value.isEnabled) return input

        val settings = _settings.value
        val driveAmount = 1 + settings.drive * 10f
        val outputGain = 10f.pow(settings.outputLevel / 20f)

        return input.map { sample ->
            val driven = sample * driveAmount

            val distorted = when (settings.type) {
                DistortionType.TUBE -> {
                    // Tube-style soft clipping
                    if (driven >= 0) {
                        1 - exp(-driven)
                    } else {
                        -1 + exp(driven)
                    }
                }
                DistortionType.TRANSISTOR -> {
                    // Asymmetric clipping
                    tanh(driven * 1.5f)
                }
                DistortionType.DIGITAL -> {
                    // Hard clipping
                    driven.coerceIn(-1f, 1f)
                }
                DistortionType.FUZZ -> {
                    // Extreme clipping with harmonics
                    sign(driven) * (1 - exp(-abs(driven) * 3))
                }
                DistortionType.OVERDRIVE -> {
                    // Smooth overdrive
                    tanh(driven)
                }
                DistortionType.RECTIFIER -> {
                    // Full-wave rectifier style
                    abs(tanh(driven * 2))
                }
            }

            // Mix wet/dry
            val mixed = sample * (1 - settings.mix) + distorted * settings.mix
            mixed * outputGain
        }.toFloatArray()
    }

    private fun tanh(x: Float): Float = kotlin.math.tanh(x.toDouble()).toFloat()
}

// ============================================
// SATURATION
// ============================================

data class SaturationSettings(
    val isEnabled: Boolean = false,
    val type: SaturationType = SaturationType.TAPE,
    val amount: Float = 0.3f,          // 0-1
    val harmonic: Float = 0.5f,        // Even/odd harmonic balance
    val mix: Float = 0.5f
)

enum class SaturationType(val label: String) {
    TAPE("Tape"),
    ANALOG("Analog"),
    TUBE("Tube"),
    TRANSFORMER("Transformer")
}

class Saturation {

    private val _settings = MutableStateFlow(SaturationSettings())
    val settings: StateFlow<SaturationSettings> = _settings.asStateFlow()

    fun updateSettings(
        enabled: Boolean? = null,
        type: SaturationType? = null,
        amount: Float? = null,
        harmonic: Float? = null,
        mix: Float? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            type = type ?: _settings.value.type,
            amount = amount?.coerceIn(0f, 1f) ?: _settings.value.amount,
            harmonic = harmonic?.coerceIn(0f, 1f) ?: _settings.value.harmonic,
            mix = mix?.coerceIn(0f, 1f) ?: _settings.value.mix
        )
    }

    fun process(input: FloatArray): FloatArray {
        if (!_settings.value.isEnabled) return input

        val settings = _settings.value
        val k = settings.amount * 5

        return input.map { sample ->
            val saturated = when (settings.type) {
                SaturationType.TAPE -> {
                    // Tape saturation with compression
                    val x = sample * (1 + k)
                    x / (1 + abs(x))
                }
                SaturationType.ANALOG -> {
                    // Soft saturation
                    tanh(sample * (1 + k * 0.5f))
                }
                SaturationType.TUBE -> {
                    // Tube warmth
                    val x = sample * (1 + k)
                    if (x > 0) 1 - exp(-x) else -1 + exp(x)
                }
                SaturationType.TRANSFORMER -> {
                    // Transformer-style with asymmetry
                    val x = sample * (1 + k)
                    atan(x * 1.5f) / (PI.toFloat() / 2)
                }
            }

            sample * (1 - settings.mix) + saturated * settings.mix
        }.toFloatArray()
    }

    private fun tanh(x: Float): Float = kotlin.math.tanh(x.toDouble()).toFloat()
}

// ============================================
// EXCITER (Harmonic Enhancer)
// ============================================

data class ExciterSettings(
    val isEnabled: Boolean = false,
    val highFreqAmount: Float = 0.3f,  // High frequency enhancement
    val lowFreqAmount: Float = 0.2f,   // Low frequency enhancement (sub harmonics)
    val harmonicBlend: Float = 0.5f,   // Even/odd harmonic mix
    val airFreq: Float = 10000f,       // "Air" band frequency
    val mix: Float = 0.5f
)

class Exciter {

    private val _settings = MutableStateFlow(ExciterSettings())
    val settings: StateFlow<ExciterSettings> = _settings.asStateFlow()

    fun updateSettings(
        enabled: Boolean? = null,
        highFreqAmount: Float? = null,
        lowFreqAmount: Float? = null,
        harmonicBlend: Float? = null,
        airFreq: Float? = null,
        mix: Float? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            highFreqAmount = highFreqAmount?.coerceIn(0f, 1f) ?: _settings.value.highFreqAmount,
            lowFreqAmount = lowFreqAmount?.coerceIn(0f, 1f) ?: _settings.value.lowFreqAmount,
            harmonicBlend = harmonicBlend?.coerceIn(0f, 1f) ?: _settings.value.harmonicBlend,
            airFreq = airFreq?.coerceIn(5000f, 16000f) ?: _settings.value.airFreq,
            mix = mix?.coerceIn(0f, 1f) ?: _settings.value.mix
        )
    }
}

// ============================================
// CHORUS
// ============================================

data class ChorusSettings(
    val isEnabled: Boolean = false,
    val rate: Float = 0.5f,            // Hz (LFO rate)
    val depth: Float = 0.5f,           // 0-1
    val mix: Float = 0.5f,
    val voices: Int = 2,               // Number of chorus voices
    val spread: Float = 0.5f,          // Stereo spread
    val feedback: Float = 0f           // -1 to 1
)

class Chorus {

    private val _settings = MutableStateFlow(ChorusSettings())
    val settings: StateFlow<ChorusSettings> = _settings.asStateFlow()

    private var phase = 0f

    fun updateSettings(
        enabled: Boolean? = null,
        rate: Float? = null,
        depth: Float? = null,
        mix: Float? = null,
        voices: Int? = null,
        spread: Float? = null,
        feedback: Float? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            rate = rate?.coerceIn(0.1f, 10f) ?: _settings.value.rate,
            depth = depth?.coerceIn(0f, 1f) ?: _settings.value.depth,
            mix = mix?.coerceIn(0f, 1f) ?: _settings.value.mix,
            voices = voices?.coerceIn(1, 8) ?: _settings.value.voices,
            spread = spread?.coerceIn(0f, 1f) ?: _settings.value.spread,
            feedback = feedback?.coerceIn(-0.95f, 0.95f) ?: _settings.value.feedback
        )
    }
}

// ============================================
// FLANGER
// ============================================

data class FlangerSettings(
    val isEnabled: Boolean = false,
    val rate: Float = 0.3f,            // Hz
    val depth: Float = 0.5f,           // 0-1
    val feedback: Float = 0.5f,        // -1 to 1
    val mix: Float = 0.5f,
    val manual: Float = 0.5f           // Base delay position
)

class Flanger {

    private val _settings = MutableStateFlow(FlangerSettings())
    val settings: StateFlow<FlangerSettings> = _settings.asStateFlow()

    fun updateSettings(
        enabled: Boolean? = null,
        rate: Float? = null,
        depth: Float? = null,
        feedback: Float? = null,
        mix: Float? = null,
        manual: Float? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            rate = rate?.coerceIn(0.01f, 5f) ?: _settings.value.rate,
            depth = depth?.coerceIn(0f, 1f) ?: _settings.value.depth,
            feedback = feedback?.coerceIn(-0.99f, 0.99f) ?: _settings.value.feedback,
            mix = mix?.coerceIn(0f, 1f) ?: _settings.value.mix,
            manual = manual?.coerceIn(0f, 1f) ?: _settings.value.manual
        )
    }
}

// ============================================
// PHASER
// ============================================

data class PhaserSettings(
    val isEnabled: Boolean = false,
    val rate: Float = 0.5f,            // Hz
    val depth: Float = 0.5f,           // 0-1
    val feedback: Float = 0.5f,        // 0-1
    val stages: Int = 4,               // Number of all-pass stages (2-12)
    val mix: Float = 0.5f,
    val centerFreq: Float = 1000f      // Hz
)

class Phaser {

    private val _settings = MutableStateFlow(PhaserSettings())
    val settings: StateFlow<PhaserSettings> = _settings.asStateFlow()

    fun updateSettings(
        enabled: Boolean? = null,
        rate: Float? = null,
        depth: Float? = null,
        feedback: Float? = null,
        stages: Int? = null,
        mix: Float? = null,
        centerFreq: Float? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            rate = rate?.coerceIn(0.01f, 10f) ?: _settings.value.rate,
            depth = depth?.coerceIn(0f, 1f) ?: _settings.value.depth,
            feedback = feedback?.coerceIn(0f, 0.99f) ?: _settings.value.feedback,
            stages = stages?.coerceIn(2, 12) ?: _settings.value.stages,
            mix = mix?.coerceIn(0f, 1f) ?: _settings.value.mix,
            centerFreq = centerFreq?.coerceIn(100f, 10000f) ?: _settings.value.centerFreq
        )
    }
}

// ============================================
// TREMOLO
// ============================================

data class TremoloSettings(
    val isEnabled: Boolean = false,
    val rate: Float = 5f,              // Hz
    val depth: Float = 0.5f,           // 0-1
    val shape: LFOShape = LFOShape.SINE,
    val stereoPhase: Float = 0f        // 0-180 degrees
)

enum class LFOShape(val label: String) {
    SINE("Sine"),
    TRIANGLE("Triangle"),
    SQUARE("Square"),
    SAW_UP("Saw Up"),
    SAW_DOWN("Saw Down"),
    RANDOM("Random")
}

class Tremolo {

    private val _settings = MutableStateFlow(TremoloSettings())
    val settings: StateFlow<TremoloSettings> = _settings.asStateFlow()

    private var phase = 0f

    fun updateSettings(
        enabled: Boolean? = null,
        rate: Float? = null,
        depth: Float? = null,
        shape: LFOShape? = null,
        stereoPhase: Float? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            rate = rate?.coerceIn(0.1f, 20f) ?: _settings.value.rate,
            depth = depth?.coerceIn(0f, 1f) ?: _settings.value.depth,
            shape = shape ?: _settings.value.shape,
            stereoPhase = stereoPhase?.coerceIn(0f, 180f) ?: _settings.value.stereoPhase
        )
    }

    fun process(input: FloatArray, sampleRate: Int): FloatArray {
        if (!_settings.value.isEnabled) return input

        val settings = _settings.value
        val phaseIncrement = settings.rate / sampleRate

        return input.mapIndexed { i, sample ->
            val lfo = getLFOValue(phase, settings.shape)
            val modulation = 1 - settings.depth * (1 - lfo) * 0.5f
            phase = (phase + phaseIncrement) % 1f
            sample * modulation
        }.toFloatArray()
    }

    private fun getLFOValue(phase: Float, shape: LFOShape): Float {
        return when (shape) {
            LFOShape.SINE -> (sin(phase * 2 * PI) + 1) / 2
            LFOShape.TRIANGLE -> if (phase < 0.5f) phase * 2 else 2 - phase * 2
            LFOShape.SQUARE -> if (phase < 0.5f) 1f else 0f
            LFOShape.SAW_UP -> phase
            LFOShape.SAW_DOWN -> 1 - phase
            LFOShape.RANDOM -> kotlin.random.Random.nextFloat()
        }.toFloat()
    }
}

// ============================================
// AUTO-PAN
// ============================================

data class AutoPanSettings(
    val isEnabled: Boolean = false,
    val rate: Float = 1f,              // Hz
    val depth: Float = 0.5f,           // 0-1
    val shape: LFOShape = LFOShape.SINE,
    val phase: Float = 0f              // Starting phase
)

class AutoPan {

    private val _settings = MutableStateFlow(AutoPanSettings())
    val settings: StateFlow<AutoPanSettings> = _settings.asStateFlow()

    fun updateSettings(
        enabled: Boolean? = null,
        rate: Float? = null,
        depth: Float? = null,
        shape: LFOShape? = null,
        phase: Float? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            rate = rate?.coerceIn(0.1f, 10f) ?: _settings.value.rate,
            depth = depth?.coerceIn(0f, 1f) ?: _settings.value.depth,
            shape = shape ?: _settings.value.shape,
            phase = phase?.coerceIn(0f, 1f) ?: _settings.value.phase
        )
    }
}

// ============================================
// BITCRUSHER
// ============================================

data class BitcrusherSettings(
    val isEnabled: Boolean = false,
    val bitDepth: Int = 8,             // 1-16 bits
    val sampleRateReduction: Float = 1f, // 1 = normal, higher = more crushing
    val mix: Float = 0.5f
)

class Bitcrusher {

    private val _settings = MutableStateFlow(BitcrusherSettings())
    val settings: StateFlow<BitcrusherSettings> = _settings.asStateFlow()

    private var holdSample = 0f
    private var holdCounter = 0

    fun updateSettings(
        enabled: Boolean? = null,
        bitDepth: Int? = null,
        sampleRateReduction: Float? = null,
        mix: Float? = null
    ) {
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            bitDepth = bitDepth?.coerceIn(1, 16) ?: _settings.value.bitDepth,
            sampleRateReduction = sampleRateReduction?.coerceIn(1f, 50f) ?: _settings.value.sampleRateReduction,
            mix = mix?.coerceIn(0f, 1f) ?: _settings.value.mix
        )
    }

    fun process(input: FloatArray): FloatArray {
        if (!_settings.value.isEnabled) return input

        val settings = _settings.value
        val levels = (1 shl settings.bitDepth).toFloat()
        val srFactor = settings.sampleRateReduction.toInt()

        return input.mapIndexed { i, sample ->
            // Sample rate reduction
            if (i % srFactor == 0) {
                holdSample = sample
            }

            // Bit depth reduction
            val crushed = (floor(holdSample * levels) / levels)

            // Mix
            sample * (1 - settings.mix) + crushed * settings.mix
        }.toFloatArray()
    }
}

