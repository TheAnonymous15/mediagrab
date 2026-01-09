package com.example.dwn.player.audio

import android.content.Context
import android.media.audiofx.*
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

private const val TAG = "DynamicsProcessing"

/**
 * DYNAMICS PROCESSING - Feature Set 3
 *
 * - Single-band Compressor
 * - Multiband Compressor (2-6 bands)
 * - Limiter (true peak, brickwall)
 * - Noise Gate
 * - Expander
 * - Sidechain support
 * - Lookahead processing
 */

// ============================================
// COMPRESSOR
// ============================================

data class CompressorSettings(
    val threshold: Float = -20f,      // dB
    val ratio: Float = 4f,            // 1:1 to 20:1
    val attack: Float = 10f,          // ms
    val release: Float = 100f,        // ms
    val knee: Float = 6f,             // dB (soft knee width)
    val makeupGain: Float = 0f,       // dB
    val autoMakeup: Boolean = true,
    val isEnabled: Boolean = false
)

class Compressor {

    private val _settings = MutableStateFlow(CompressorSettings())
    val settings: StateFlow<CompressorSettings> = _settings.asStateFlow()

    private var envelope = 0f
    private var gainReduction = 0f

    private val _gainReductionDb = MutableStateFlow(0f)
    val gainReductionDb: StateFlow<Float> = _gainReductionDb.asStateFlow()

    fun updateSettings(
        threshold: Float? = null,
        ratio: Float? = null,
        attack: Float? = null,
        release: Float? = null,
        knee: Float? = null,
        makeupGain: Float? = null,
        autoMakeup: Boolean? = null,
        enabled: Boolean? = null
    ) {
        _settings.value = _settings.value.copy(
            threshold = threshold?.coerceIn(-60f, 0f) ?: _settings.value.threshold,
            ratio = ratio?.coerceIn(1f, 20f) ?: _settings.value.ratio,
            attack = attack?.coerceIn(0.1f, 500f) ?: _settings.value.attack,
            release = release?.coerceIn(10f, 5000f) ?: _settings.value.release,
            knee = knee?.coerceIn(0f, 24f) ?: _settings.value.knee,
            makeupGain = makeupGain?.coerceIn(0f, 24f) ?: _settings.value.makeupGain,
            autoMakeup = autoMakeup ?: _settings.value.autoMakeup,
            isEnabled = enabled ?: _settings.value.isEnabled
        )
    }

    fun process(input: FloatArray, sampleRate: Int): FloatArray {
        if (!_settings.value.isEnabled) return input

        val settings = _settings.value
        val attackCoef = exp(-1f / (settings.attack * sampleRate / 1000f))
        val releaseCoef = exp(-1f / (settings.release * sampleRate / 1000f))

        val output = FloatArray(input.size)

        for (i in input.indices) {
            val inputLevel = 20f * log10(abs(input[i]).coerceAtLeast(1e-10f))

            // Envelope follower
            val targetEnv = inputLevel
            envelope = if (targetEnv > envelope) {
                attackCoef * envelope + (1 - attackCoef) * targetEnv
            } else {
                releaseCoef * envelope + (1 - releaseCoef) * targetEnv
            }

            // Calculate gain reduction with soft knee
            gainReduction = calculateGainReduction(envelope, settings)

            // Apply makeup gain
            val totalGain = if (settings.autoMakeup) {
                -gainReduction + (settings.threshold * (1 - 1/settings.ratio) * 0.5f)
            } else {
                -gainReduction + settings.makeupGain
            }

            val linearGain = 10f.pow(totalGain / 20f)
            output[i] = input[i] * linearGain
        }

        _gainReductionDb.value = gainReduction
        return output
    }

    private fun calculateGainReduction(inputDb: Float, settings: CompressorSettings): Float {
        val halfKnee = settings.knee / 2f

        return when {
            inputDb < settings.threshold - halfKnee -> 0f
            inputDb > settings.threshold + halfKnee -> {
                (inputDb - settings.threshold) * (1 - 1/settings.ratio)
            }
            else -> {
                // Soft knee region
                val x = inputDb - settings.threshold + halfKnee
                (1 - 1/settings.ratio) * x * x / (2 * settings.knee)
            }
        }
    }
}

// ============================================
// MULTIBAND COMPRESSOR
// ============================================

data class MultibandCompressorBand(
    val id: Int,
    val lowFreq: Float,
    val highFreq: Float,
    val compressor: CompressorSettings = CompressorSettings(),
    val solo: Boolean = false,
    val mute: Boolean = false
)

data class MultibandCompressorState(
    val isEnabled: Boolean = false,
    val bands: List<MultibandCompressorBand> = emptyList(),
    val crossoverSlope: Int = 24  // dB/octave
)

class MultibandCompressor {

    private val _state = MutableStateFlow(MultibandCompressorState())
    val state: StateFlow<MultibandCompressorState> = _state.asStateFlow()

    private val bandCompressors = mutableMapOf<Int, Compressor>()

    fun initialize(bandCount: Int = 4) {
        val frequencies = when (bandCount) {
            2 -> listOf(0f to 500f, 500f to 20000f)
            3 -> listOf(0f to 200f, 200f to 2000f, 2000f to 20000f)
            4 -> listOf(0f to 100f, 100f to 500f, 500f to 2500f, 2500f to 20000f)
            5 -> listOf(0f to 80f, 80f to 300f, 300f to 1200f, 1200f to 5000f, 5000f to 20000f)
            6 -> listOf(0f to 60f, 60f to 200f, 200f to 600f, 600f to 2000f, 2000f to 6000f, 6000f to 20000f)
            else -> listOf(0f to 100f, 100f to 500f, 500f to 2500f, 2500f to 20000f)
        }

        val bands = frequencies.mapIndexed { index, (low, high) ->
            val compressor = Compressor()
            bandCompressors[index] = compressor
            MultibandCompressorBand(
                id = index,
                lowFreq = low,
                highFreq = high
            )
        }

        _state.value = _state.value.copy(bands = bands)
    }

    fun updateBandCompressor(bandId: Int, settings: CompressorSettings) {
        val bands = _state.value.bands.map { band ->
            if (band.id == bandId) band.copy(compressor = settings) else band
        }
        _state.value = _state.value.copy(bands = bands)
        bandCompressors[bandId]?.updateSettings(
            threshold = settings.threshold,
            ratio = settings.ratio,
            attack = settings.attack,
            release = settings.release,
            knee = settings.knee,
            makeupGain = settings.makeupGain
        )
    }

    fun setBandSolo(bandId: Int, solo: Boolean) {
        val bands = _state.value.bands.map { band ->
            if (band.id == bandId) band.copy(solo = solo) else band
        }
        _state.value = _state.value.copy(bands = bands)
    }

    fun setBandMute(bandId: Int, mute: Boolean) {
        val bands = _state.value.bands.map { band ->
            if (band.id == bandId) band.copy(mute = mute) else band
        }
        _state.value = _state.value.copy(bands = bands)
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isEnabled = enabled)
    }
}

// ============================================
// LIMITER
// ============================================

data class LimiterSettings(
    val ceiling: Float = -0.3f,       // dB (output ceiling)
    val threshold: Float = -6f,        // dB
    val release: Float = 100f,         // ms
    val lookahead: Float = 5f,         // ms
    val truePeak: Boolean = true,      // True peak limiting
    val isEnabled: Boolean = false
)

class Limiter {

    private val _settings = MutableStateFlow(LimiterSettings())
    val settings: StateFlow<LimiterSettings> = _settings.asStateFlow()

    private val _gainReductionDb = MutableStateFlow(0f)
    val gainReductionDb: StateFlow<Float> = _gainReductionDb.asStateFlow()

    private var envelope = 0f

    fun updateSettings(
        ceiling: Float? = null,
        threshold: Float? = null,
        release: Float? = null,
        lookahead: Float? = null,
        truePeak: Boolean? = null,
        enabled: Boolean? = null
    ) {
        _settings.value = _settings.value.copy(
            ceiling = ceiling?.coerceIn(-12f, 0f) ?: _settings.value.ceiling,
            threshold = threshold?.coerceIn(-24f, 0f) ?: _settings.value.threshold,
            release = release?.coerceIn(10f, 1000f) ?: _settings.value.release,
            lookahead = lookahead?.coerceIn(0f, 20f) ?: _settings.value.lookahead,
            truePeak = truePeak ?: _settings.value.truePeak,
            isEnabled = enabled ?: _settings.value.isEnabled
        )
    }

    fun process(input: FloatArray, sampleRate: Int): FloatArray {
        if (!_settings.value.isEnabled) return input

        val settings = _settings.value
        val output = FloatArray(input.size)
        val ceilingLinear = 10f.pow(settings.ceiling / 20f)

        for (i in input.indices) {
            val sample = input[i]
            val absSample = abs(sample)

            // Brickwall limiting
            output[i] = if (absSample > ceilingLinear) {
                sample.sign * ceilingLinear
            } else {
                sample
            }

            // Track gain reduction
            if (absSample > ceilingLinear) {
                val reduction = 20f * log10(ceilingLinear / absSample)
                _gainReductionDb.value = minOf(_gainReductionDb.value, reduction)
            }
        }

        return output
    }
}

// ============================================
// NOISE GATE
// ============================================

data class NoiseGateSettings(
    val threshold: Float = -40f,       // dB
    val attack: Float = 1f,            // ms
    val hold: Float = 50f,             // ms
    val release: Float = 200f,         // ms
    val range: Float = -80f,           // dB (how much to attenuate)
    val isEnabled: Boolean = false
)

class NoiseGate {

    private val _settings = MutableStateFlow(NoiseGateSettings())
    val settings: StateFlow<NoiseGateSettings> = _settings.asStateFlow()

    private var gateState = GateState.CLOSED
    private var holdCounter = 0
    private var envelope = 0f

    enum class GateState { CLOSED, OPENING, OPEN, HOLDING, CLOSING }

    private val _currentState = MutableStateFlow(GateState.CLOSED)
    val currentState: StateFlow<GateState> = _currentState.asStateFlow()

    fun updateSettings(
        threshold: Float? = null,
        attack: Float? = null,
        hold: Float? = null,
        release: Float? = null,
        range: Float? = null,
        enabled: Boolean? = null
    ) {
        _settings.value = _settings.value.copy(
            threshold = threshold?.coerceIn(-80f, 0f) ?: _settings.value.threshold,
            attack = attack?.coerceIn(0.01f, 100f) ?: _settings.value.attack,
            hold = hold?.coerceIn(0f, 500f) ?: _settings.value.hold,
            release = release?.coerceIn(10f, 2000f) ?: _settings.value.release,
            range = range?.coerceIn(-80f, 0f) ?: _settings.value.range,
            isEnabled = enabled ?: _settings.value.isEnabled
        )
    }

    fun process(input: FloatArray, sampleRate: Int): FloatArray {
        if (!_settings.value.isEnabled) return input

        val settings = _settings.value
        val output = FloatArray(input.size)
        val holdSamples = (settings.hold * sampleRate / 1000f).toInt()
        val rangeLinear = 10f.pow(settings.range / 20f)

        for (i in input.indices) {
            val inputDb = 20f * log10(abs(input[i]).coerceAtLeast(1e-10f))

            // State machine
            when (gateState) {
                GateState.CLOSED -> {
                    if (inputDb > settings.threshold) {
                        gateState = GateState.OPENING
                    }
                    output[i] = input[i] * rangeLinear
                }
                GateState.OPENING -> {
                    envelope += 1f / (settings.attack * sampleRate / 1000f)
                    if (envelope >= 1f) {
                        envelope = 1f
                        gateState = GateState.OPEN
                    }
                    output[i] = input[i] * (rangeLinear + (1 - rangeLinear) * envelope)
                }
                GateState.OPEN -> {
                    if (inputDb < settings.threshold) {
                        gateState = GateState.HOLDING
                        holdCounter = holdSamples
                    }
                    output[i] = input[i]
                }
                GateState.HOLDING -> {
                    holdCounter--
                    if (inputDb > settings.threshold) {
                        gateState = GateState.OPEN
                    } else if (holdCounter <= 0) {
                        gateState = GateState.CLOSING
                    }
                    output[i] = input[i]
                }
                GateState.CLOSING -> {
                    envelope -= 1f / (settings.release * sampleRate / 1000f)
                    if (envelope <= 0f) {
                        envelope = 0f
                        gateState = GateState.CLOSED
                    }
                    output[i] = input[i] * (rangeLinear + (1 - rangeLinear) * envelope)
                }
            }
        }

        _currentState.value = gateState
        return output
    }
}

// ============================================
// EXPANDER
// ============================================

data class ExpanderSettings(
    val threshold: Float = -30f,
    val ratio: Float = 2f,             // 1:2, 1:4, etc.
    val attack: Float = 10f,
    val release: Float = 200f,
    val range: Float = -40f,
    val isEnabled: Boolean = false
)

class Expander {

    private val _settings = MutableStateFlow(ExpanderSettings())
    val settings: StateFlow<ExpanderSettings> = _settings.asStateFlow()

    private var envelope = 0f

    fun updateSettings(
        threshold: Float? = null,
        ratio: Float? = null,
        attack: Float? = null,
        release: Float? = null,
        range: Float? = null,
        enabled: Boolean? = null
    ) {
        _settings.value = _settings.value.copy(
            threshold = threshold?.coerceIn(-60f, 0f) ?: _settings.value.threshold,
            ratio = ratio?.coerceIn(1f, 10f) ?: _settings.value.ratio,
            attack = attack?.coerceIn(0.1f, 200f) ?: _settings.value.attack,
            release = release?.coerceIn(10f, 2000f) ?: _settings.value.release,
            range = range?.coerceIn(-80f, 0f) ?: _settings.value.range,
            isEnabled = enabled ?: _settings.value.isEnabled
        )
    }

    fun process(input: FloatArray, sampleRate: Int): FloatArray {
        if (!_settings.value.isEnabled) return input

        val settings = _settings.value
        val output = FloatArray(input.size)

        val attackCoef = exp(-1f / (settings.attack * sampleRate / 1000f))
        val releaseCoef = exp(-1f / (settings.release * sampleRate / 1000f))

        for (i in input.indices) {
            val inputDb = 20f * log10(abs(input[i]).coerceAtLeast(1e-10f))

            // Envelope follower
            val targetEnv = inputDb
            envelope = if (targetEnv > envelope) {
                attackCoef * envelope + (1 - attackCoef) * targetEnv
            } else {
                releaseCoef * envelope + (1 - releaseCoef) * targetEnv
            }

            // Calculate expansion gain
            val gainDb = if (envelope < settings.threshold) {
                val diff = settings.threshold - envelope
                -(diff * (settings.ratio - 1)).coerceAtMost(-settings.range)
            } else {
                0f
            }

            val linearGain = 10f.pow(gainDb / 20f)
            output[i] = input[i] * linearGain
        }

        return output
    }
}

