package com.example.dwn.player.audio

import android.content.Context
import android.media.audiofx.*
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

private const val TAG = "EqualizationSystem"

/**
 * EQUALIZATION SYSTEM - Feature Set 2
 *
 * 2.1 Graphic Equalizer (10, 15, 31 band modes)
 * 2.2 Parametric Equalizer (unlimited bands)
 * 2.3 Advanced EQ (Dynamic, Linear-phase, Mid/Side)
 */

// ============================================
// 2.1 GRAPHIC EQUALIZER
// ============================================

data class GraphicEQBand(
    val index: Int,
    val frequency: Int,      // Center frequency in Hz
    val gain: Float,         // Gain in dB (-24 to +24)
    val isMuted: Boolean = false,
    val isSoloed: Boolean = false
)

data class GraphicEQState(
    val isEnabled: Boolean = false,
    val mode: GraphicEQMode = GraphicEQMode.BAND_10,
    val bands: List<GraphicEQBand> = emptyList(),
    val gainRange: Float = 24f,  // +/- dB
    val preampGain: Float = 0f
)

enum class GraphicEQMode(val bandCount: Int, val label: String) {
    BAND_5(5, "5-Band"),
    BAND_10(10, "10-Band"),
    BAND_15(15, "15-Band"),
    BAND_31(31, "31-Band")
}

class GraphicEqualizer(private val context: Context) {

    private var equalizer: Equalizer? = null
    private var audioSessionId: Int = 0

    private val _state = MutableStateFlow(GraphicEQState())
    val state: StateFlow<GraphicEQState> = _state.asStateFlow()

    // Standard frequency centers for different band modes
    private val frequencies5Band = intArrayOf(60, 230, 910, 3600, 14000)
    private val frequencies10Band = intArrayOf(31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    private val frequencies15Band = intArrayOf(25, 40, 63, 100, 160, 250, 400, 630, 1000, 1600, 2500, 4000, 6300, 10000, 16000)
    private val frequencies31Band = intArrayOf(
        20, 25, 31, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630,
        800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000, 12500, 16000, 20000
    )

    private val prefs by lazy {
        context.getSharedPreferences("graphic_eq_prefs", Context.MODE_PRIVATE)
    }

    fun initialize(sessionId: Int) {
        try {
            audioSessionId = sessionId
            equalizer = Equalizer(0, sessionId)

            // Load saved mode
            val savedMode = prefs.getString("eq_mode", GraphicEQMode.BAND_10.name)
            val mode = GraphicEQMode.valueOf(savedMode ?: GraphicEQMode.BAND_10.name)

            setMode(mode)
            loadSavedSettings()

            Log.d(TAG, "Graphic EQ initialized - Session: $sessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Graphic EQ", e)
        }
    }

    fun setMode(mode: GraphicEQMode) {
        val frequencies = when (mode) {
            GraphicEQMode.BAND_5 -> frequencies5Band
            GraphicEQMode.BAND_10 -> frequencies10Band
            GraphicEQMode.BAND_15 -> frequencies15Band
            GraphicEQMode.BAND_31 -> frequencies31Band
        }

        val bands = frequencies.mapIndexed { index, freq ->
            val savedGain = prefs.getFloat("band_${mode.name}_$index", 0f)
            GraphicEQBand(
                index = index,
                frequency = freq,
                gain = savedGain
            )
        }

        _state.value = _state.value.copy(
            mode = mode,
            bands = bands
        )

        prefs.edit().putString("eq_mode", mode.name).apply()
        applyToHardware()
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        val clampedGain = gainDb.coerceIn(-_state.value.gainRange, _state.value.gainRange)

        val bands = _state.value.bands.toMutableList()
        if (bandIndex < bands.size) {
            bands[bandIndex] = bands[bandIndex].copy(gain = clampedGain)
            _state.value = _state.value.copy(bands = bands)

            prefs.edit().putFloat("band_${_state.value.mode.name}_$bandIndex", clampedGain).apply()
            applyToHardware()
        }
    }

    fun setBandMuted(bandIndex: Int, muted: Boolean) {
        val bands = _state.value.bands.toMutableList()
        if (bandIndex < bands.size) {
            bands[bandIndex] = bands[bandIndex].copy(isMuted = muted)
            _state.value = _state.value.copy(bands = bands)
            applyToHardware()
        }
    }

    fun setBandSoloed(bandIndex: Int, soloed: Boolean) {
        val bands = _state.value.bands.toMutableList()
        if (bandIndex < bands.size) {
            // If soloing, unsolo others
            if (soloed) {
                bands.forEachIndexed { i, band ->
                    bands[i] = band.copy(isSoloed = i == bandIndex)
                }
            } else {
                bands[bandIndex] = bands[bandIndex].copy(isSoloed = false)
            }
            _state.value = _state.value.copy(bands = bands)
            applyToHardware()
        }
    }

    fun setPreampGain(gainDb: Float) {
        _state.value = _state.value.copy(preampGain = gainDb.coerceIn(-24f, 24f))
        prefs.edit().putFloat("preamp_gain", _state.value.preampGain).apply()
        applyToHardware()  // Apply preamp changes to hardware
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isEnabled = enabled)
        equalizer?.enabled = enabled
        prefs.edit().putBoolean("eq_enabled", enabled).apply()
    }

    fun resetToFlat() {
        val bands = _state.value.bands.map { it.copy(gain = 0f, isMuted = false, isSoloed = false) }
        _state.value = _state.value.copy(bands = bands, preampGain = 0f)

        val editor = prefs.edit()
        bands.forEachIndexed { index, _ ->
            editor.putFloat("band_${_state.value.mode.name}_$index", 0f)
        }
        editor.putFloat("preamp_gain", 0f)
        editor.apply()

        applyToHardware()
    }

    private fun applyToHardware() {
        equalizer?.let { eq ->
            val numberOfBands = eq.numberOfBands.toInt()
            val state = _state.value

            // Check if any band is soloed
            val hasSolo = state.bands.any { it.isSoloed }

            state.bands.forEachIndexed { index, band ->
                if (index < numberOfBands) {
                    val effectiveGain = when {
                        band.isMuted -> -24f
                        hasSolo && !band.isSoloed -> -24f
                        else -> band.gain
                    }

                    // Convert dB to millibel and apply preamp
                    val totalGain = effectiveGain + state.preampGain
                    val millibels = (totalGain * 100).toInt().toShort()

                    try {
                        eq.setBandLevel(index.toShort(), millibels)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not set band $index level", e)
                    }
                }
            }
        }
    }

    private fun loadSavedSettings() {
        val enabled = prefs.getBoolean("eq_enabled", false)
        val preamp = prefs.getFloat("preamp_gain", 0f)

        _state.value = _state.value.copy(
            isEnabled = enabled,
            preampGain = preamp
        )

        equalizer?.enabled = enabled
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing equalizer", e)
        }
        equalizer = null
    }
}

// ============================================
// 2.2 PARAMETRIC EQUALIZER
// ============================================

data class ParametricBand(
    val id: Int,
    val frequency: Float,    // Hz (20 - 20000)
    val gain: Float,         // dB (-24 to +24)
    val q: Float,            // Q factor (0.1 to 10)
    val filterType: FilterType = FilterType.BELL,
    val slope: FilterSlope = FilterSlope.DB_12,
    val isEnabled: Boolean = true
)

enum class FilterType(val label: String) {
    BELL("Bell"),
    LOW_SHELF("Low Shelf"),
    HIGH_SHELF("High Shelf"),
    NOTCH("Notch"),
    HIGH_PASS("High Pass"),
    LOW_PASS("Low Pass"),
    BAND_PASS("Band Pass"),
    ALL_PASS("All Pass")
}

enum class FilterSlope(val dbPerOctave: Int, val label: String) {
    DB_6(6, "6 dB/oct"),
    DB_12(12, "12 dB/oct"),
    DB_18(18, "18 dB/oct"),
    DB_24(24, "24 dB/oct"),
    DB_36(36, "36 dB/oct"),
    DB_48(48, "48 dB/oct"),
    DB_96(96, "96 dB/oct")
}

data class ParametricEQState(
    val isEnabled: Boolean = false,
    val bands: List<ParametricBand> = emptyList(),
    val autoGainCompensation: Boolean = true
)

class ParametricEqualizer {

    private val _state = MutableStateFlow(ParametricEQState())
    val state: StateFlow<ParametricEQState> = _state.asStateFlow()

    private var nextBandId = 0

    fun addBand(
        frequency: Float = 1000f,
        gain: Float = 0f,
        q: Float = 1f,
        filterType: FilterType = FilterType.BELL
    ): Int {
        val band = ParametricBand(
            id = nextBandId++,
            frequency = frequency.coerceIn(20f, 20000f),
            gain = gain.coerceIn(-24f, 24f),
            q = q.coerceIn(0.1f, 10f),
            filterType = filterType
        )

        _state.value = _state.value.copy(
            bands = _state.value.bands + band
        )

        return band.id
    }

    fun removeBand(bandId: Int) {
        _state.value = _state.value.copy(
            bands = _state.value.bands.filter { it.id != bandId }
        )
    }

    fun updateBand(
        bandId: Int,
        frequency: Float? = null,
        gain: Float? = null,
        q: Float? = null,
        filterType: FilterType? = null,
        slope: FilterSlope? = null,
        enabled: Boolean? = null
    ) {
        val bands = _state.value.bands.map { band ->
            if (band.id == bandId) {
                band.copy(
                    frequency = frequency?.coerceIn(20f, 20000f) ?: band.frequency,
                    gain = gain?.coerceIn(-24f, 24f) ?: band.gain,
                    q = q?.coerceIn(0.1f, 10f) ?: band.q,
                    filterType = filterType ?: band.filterType,
                    slope = slope ?: band.slope,
                    isEnabled = enabled ?: band.isEnabled
                )
            } else band
        }
        _state.value = _state.value.copy(bands = bands)
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isEnabled = enabled)
    }

    fun setAutoGainCompensation(enabled: Boolean) {
        _state.value = _state.value.copy(autoGainCompensation = enabled)
    }

    // Calculate frequency response at given frequency
    fun getResponseAt(frequency: Float): Float {
        if (!_state.value.isEnabled) return 0f

        var totalGain = 0f
        for (band in _state.value.bands) {
            if (band.isEnabled) {
                totalGain += calculateBandResponse(band, frequency)
            }
        }

        // Apply auto gain compensation
        if (_state.value.autoGainCompensation) {
            val avgGain = _state.value.bands
                .filter { it.isEnabled }
                .map { it.gain }
                .average()
                .toFloat()
            totalGain -= avgGain * 0.5f
        }

        return totalGain
    }

    private fun calculateBandResponse(band: ParametricBand, frequency: Float): Float {
        val ratio = frequency / band.frequency
        val logRatio = ln(ratio)

        return when (band.filterType) {
            FilterType.BELL -> {
                val bandwidth = 1f / band.q
                val x = logRatio / bandwidth
                band.gain * exp(-x * x)
            }
            FilterType.LOW_SHELF -> {
                if (ratio < 1) band.gain * (1 - ratio.pow(2))
                else 0f
            }
            FilterType.HIGH_SHELF -> {
                if (ratio > 1) band.gain * (1 - (1/ratio).pow(2))
                else 0f
            }
            FilterType.NOTCH -> {
                val bandwidth = 1f / band.q
                val x = logRatio / bandwidth
                -band.gain.absoluteValue * exp(-x * x * 10)
            }
            FilterType.HIGH_PASS -> {
                if (ratio < 1) -24f * (1 - ratio) else 0f
            }
            FilterType.LOW_PASS -> {
                if (ratio > 1) -24f * (ratio - 1) else 0f
            }
            FilterType.BAND_PASS -> {
                val bandwidth = 1f / band.q
                val x = logRatio / bandwidth
                if (abs(x) < 1) band.gain * (1 - abs(x)) else 0f
            }
            FilterType.ALL_PASS -> 0f
        }
    }

    fun clearAllBands() {
        _state.value = _state.value.copy(bands = emptyList())
        nextBandId = 0
    }
}

// ============================================
// 2.3 ADVANCED EQ FEATURES
// ============================================

data class DynamicEQBand(
    val id: Int,
    val frequency: Float,
    val threshold: Float,    // dB level to trigger
    val ratio: Float,        // Compression ratio
    val attack: Float,       // ms
    val release: Float,      // ms
    val maxGain: Float,      // Maximum gain change
    val direction: DynamicDirection = DynamicDirection.CUT
)

enum class DynamicDirection {
    CUT,    // Reduce when above threshold
    BOOST   // Increase when below threshold
}

data class AdvancedEQState(
    // Dynamic EQ
    val dynamicEQEnabled: Boolean = false,
    val dynamicBands: List<DynamicEQBand> = emptyList(),

    // Linear Phase
    val linearPhaseEnabled: Boolean = false,
    val linearPhaseLatencyMs: Float = 20f,

    // Mid/Side
    val midSideEnabled: Boolean = false,
    val midGain: Float = 0f,
    val sideGain: Float = 0f,

    // Auto Gain
    val autoGainEnabled: Boolean = true
)

class AdvancedEQ {

    private val _state = MutableStateFlow(AdvancedEQState())
    val state: StateFlow<AdvancedEQState> = _state.asStateFlow()

    // Dynamic EQ
    fun setDynamicEQEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(dynamicEQEnabled = enabled)
    }

    fun addDynamicBand(
        frequency: Float,
        threshold: Float = -20f,
        ratio: Float = 4f,
        attack: Float = 10f,
        release: Float = 100f,
        maxGain: Float = 12f,
        direction: DynamicDirection = DynamicDirection.CUT
    ): Int {
        val id = (_state.value.dynamicBands.maxOfOrNull { it.id } ?: 0) + 1
        val band = DynamicEQBand(id, frequency, threshold, ratio, attack, release, maxGain, direction)
        _state.value = _state.value.copy(
            dynamicBands = _state.value.dynamicBands + band
        )
        return id
    }

    // Linear Phase EQ (introduces latency for phase-perfect processing)
    fun setLinearPhaseEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(linearPhaseEnabled = enabled)
    }

    fun setLinearPhaseLatency(latencyMs: Float) {
        _state.value = _state.value.copy(
            linearPhaseLatencyMs = latencyMs.coerceIn(5f, 100f)
        )
    }

    // Mid/Side Processing
    fun setMidSideEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(midSideEnabled = enabled)
    }

    fun setMidGain(gainDb: Float) {
        _state.value = _state.value.copy(midGain = gainDb.coerceIn(-24f, 24f))
    }

    fun setSideGain(gainDb: Float) {
        _state.value = _state.value.copy(sideGain = gainDb.coerceIn(-24f, 24f))
    }

    // Auto Gain Compensation
    fun setAutoGainEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(autoGainEnabled = enabled)
    }
}

