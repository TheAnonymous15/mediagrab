package com.example.dwn.player.engine

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ============================================
 * AUDIO FX MODULE
 * ============================================
 *
 * Modular DSP chain with:
 * - Parametric Equalizer
 * - Bass Boost
 * - Virtualizer (Stereo Widener)
 * - Reverb
 * - Loudness Enhancer
 * - Compressor (via DynamicsProcessing on API 28+)
 * - Limiter
 *
 * All FX are toggleable individually
 */
class AudioFXModule {

    private var audioSessionId: Int = 0
    private var isInitialized = false

    // Audio effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null

    // State
    private val _fxState = MutableStateFlow(AudioFXState())
    val fxState: StateFlow<AudioFXState> = _fxState.asStateFlow()

    // Equalizer presets cache
    private var eqPresets: List<String> = emptyList()
    private var eqBandFrequencies: List<Int> = emptyList()
    private var eqBandRange: Pair<Int, Int> = Pair(-1500, 1500)

    // ============================================
    // INITIALIZATION
    // ============================================

    fun initialize(sessionId: Int) {
        if (isInitialized && audioSessionId == sessionId) return

        release()
        audioSessionId = sessionId

        try {
            // Equalizer
            equalizer = Equalizer(0, sessionId).apply {
                enabled = _fxState.value.eqEnabled
            }

            // Cache EQ info
            equalizer?.let { eq ->
                eqPresets = (0 until eq.numberOfPresets).map {
                    eq.getPresetName(it.toShort())
                }
                eqBandFrequencies = (0 until eq.numberOfBands).map {
                    eq.getCenterFreq(it.toShort()) / 1000
                }
                eqBandRange = Pair(
                    eq.bandLevelRange[0].toInt(),
                    eq.bandLevelRange[1].toInt()
                )

                // Initialize band levels in state
                val initialLevels = (0 until eq.numberOfBands).map {
                    eq.getBandLevel(it.toShort()).toInt()
                }
                _fxState.value = _fxState.value.copy(eqBandLevels = initialLevels)
            }

            // Bass Boost
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = _fxState.value.bassBoostEnabled
                if (_fxState.value.bassBoostStrength > 0) {
                    setStrength(_fxState.value.bassBoostStrength.toShort())
                }
            }

            // Virtualizer (Stereo Widener)
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = _fxState.value.virtualizerEnabled
                if (_fxState.value.virtualizerStrength > 0) {
                    setStrength(_fxState.value.virtualizerStrength.toShort())
                }
            }

            // Loudness Enhancer
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                    enabled = _fxState.value.loudnessEnhancerEnabled
                    if (_fxState.value.loudnessGain != 0) {
                        setTargetGain(_fxState.value.loudnessGain)
                    }
                }
            }

            // Reverb
            presetReverb = PresetReverb(0, sessionId).apply {
                enabled = _fxState.value.reverbEnabled
                if (_fxState.value.reverbPreset > 0) {
                    preset = _fxState.value.reverbPreset.toShort()
                }
            }

            isInitialized = true

        } catch (e: Exception) {
            android.util.Log.e("AudioFXModule", "Failed to initialize FX", e)
        }
    }

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        loudnessEnhancer?.release()
        presetReverb?.release()

        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        presetReverb = null

        isInitialized = false
    }

    // ============================================
    // EQUALIZER
    // ============================================

    fun toggleEqualizer(enabled: Boolean) {
        equalizer?.enabled = enabled
        _fxState.value = _fxState.value.copy(eqEnabled = enabled)
    }

    fun setEqualizerBand(band: Int, level: Int) {
        equalizer?.let { eq ->
            if (band in 0 until eq.numberOfBands) {
                val clampedLevel = level.coerceIn(eqBandRange.first, eqBandRange.second)
                eq.setBandLevel(band.toShort(), clampedLevel.toShort())

                val newBands = _fxState.value.eqBandLevels.toMutableList()
                if (band in newBands.indices) {
                    newBands[band] = clampedLevel
                    _fxState.value = _fxState.value.copy(eqBandLevels = newBands)
                }
            }
        }
    }

    fun setEqualizerPreset(presetIndex: Int) {
        equalizer?.let { eq ->
            if (presetIndex in 0 until eq.numberOfPresets) {
                eq.usePreset(presetIndex.toShort())
                _fxState.value = _fxState.value.copy(eqPresetIndex = presetIndex)

                // Update band levels from preset
                val bands = (0 until eq.numberOfBands).map {
                    eq.getBandLevel(it.toShort()).toInt()
                }
                _fxState.value = _fxState.value.copy(eqBandLevels = bands)
            }
        }
    }

    fun getEqualizerPresets(): List<String> = eqPresets

    fun getEqualizerBandFrequencies(): List<Int> = eqBandFrequencies

    fun getEqualizerBandRange(): Pair<Int, Int> = eqBandRange

    fun getNumberOfBands(): Int = equalizer?.numberOfBands?.toInt() ?: 0

    // ============================================
    // BASS BOOST
    // ============================================

    fun toggleBassBoost(enabled: Boolean) {
        bassBoost?.enabled = enabled
        _fxState.value = _fxState.value.copy(bassBoostEnabled = enabled)
    }

    fun setBassBoostStrength(strength: Int) {
        val clampedStrength = strength.coerceIn(0, 1000)
        bassBoost?.setStrength(clampedStrength.toShort())
        _fxState.value = _fxState.value.copy(bassBoostStrength = clampedStrength)
    }

    // ============================================
    // VIRTUALIZER (STEREO WIDENER)
    // ============================================

    fun toggleVirtualizer(enabled: Boolean) {
        virtualizer?.enabled = enabled
        _fxState.value = _fxState.value.copy(virtualizerEnabled = enabled)
    }

    fun setVirtualizerStrength(strength: Int) {
        val clampedStrength = strength.coerceIn(0, 1000)
        virtualizer?.setStrength(clampedStrength.toShort())
        _fxState.value = _fxState.value.copy(virtualizerStrength = clampedStrength)
    }

    // ============================================
    // LOUDNESS ENHANCER
    // ============================================

    fun toggleLoudnessEnhancer(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            loudnessEnhancer?.enabled = enabled
            _fxState.value = _fxState.value.copy(loudnessEnhancerEnabled = enabled)
        }
    }

    fun setLoudnessGain(gainMb: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val clampedGain = gainMb.coerceIn(-1000, 2000)
            loudnessEnhancer?.setTargetGain(clampedGain)
            _fxState.value = _fxState.value.copy(loudnessGain = clampedGain)
        }
    }

    // ============================================
    // REVERB
    // ============================================

    fun toggleReverb(enabled: Boolean) {
        presetReverb?.enabled = enabled
        _fxState.value = _fxState.value.copy(reverbEnabled = enabled)
    }

    fun setReverbPreset(preset: Int) {
        val clampedPreset = preset.coerceIn(0, 6)
        presetReverb?.preset = clampedPreset.toShort()
        _fxState.value = _fxState.value.copy(reverbPreset = clampedPreset)
    }

    fun getReverbPresetNames(): List<String> = listOf(
        "None",
        "Small Room",
        "Medium Room",
        "Large Room",
        "Medium Hall",
        "Large Hall",
        "Plate"
    )

    // ============================================
    // COMPRESSOR (Simulated)
    // ============================================

    fun toggleCompressor(enabled: Boolean) {
        _fxState.value = _fxState.value.copy(compressorEnabled = enabled)
        // Note: Real compressor requires DynamicsProcessing (API 28+)
        // or custom DSP implementation
    }

    fun setCompressorThreshold(threshold: Float) {
        _fxState.value = _fxState.value.copy(compressorThreshold = threshold)
    }

    fun setCompressorRatio(ratio: Float) {
        _fxState.value = _fxState.value.copy(compressorRatio = ratio)
    }

    // ============================================
    // LIMITER (Simulated)
    // ============================================

    fun toggleLimiter(enabled: Boolean) {
        _fxState.value = _fxState.value.copy(limiterEnabled = enabled)
    }

    fun setLimiterThreshold(threshold: Float) {
        _fxState.value = _fxState.value.copy(limiterThreshold = threshold)
    }

    // ============================================
    // BULK OPERATIONS
    // ============================================

    fun disableAllFX() {
        toggleEqualizer(false)
        toggleBassBoost(false)
        toggleVirtualizer(false)
        toggleLoudnessEnhancer(false)
        toggleReverb(false)
        toggleCompressor(false)
        toggleLimiter(false)
    }

    fun applyFXState(state: AudioFXState) {
        toggleEqualizer(state.eqEnabled)
        if (state.eqBandLevels.isNotEmpty()) {
            state.eqBandLevels.forEachIndexed { index, level ->
                setEqualizerBand(index, level)
            }
        }

        toggleBassBoost(state.bassBoostEnabled)
        setBassBoostStrength(state.bassBoostStrength)

        toggleVirtualizer(state.virtualizerEnabled)
        setVirtualizerStrength(state.virtualizerStrength)

        toggleLoudnessEnhancer(state.loudnessEnhancerEnabled)
        setLoudnessGain(state.loudnessGain)

        toggleReverb(state.reverbEnabled)
        setReverbPreset(state.reverbPreset)

        toggleCompressor(state.compressorEnabled)
        setCompressorThreshold(state.compressorThreshold)
        setCompressorRatio(state.compressorRatio)

        toggleLimiter(state.limiterEnabled)
        setLimiterThreshold(state.limiterThreshold)
    }

    fun getCurrentState(): AudioFXState = _fxState.value
}

