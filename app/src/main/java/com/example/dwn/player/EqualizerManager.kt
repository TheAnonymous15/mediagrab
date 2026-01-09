package com.example.dwn.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "EqualizerManager"

data class EqualizerBand(
    val index: Int,
    val centerFreq: Int,
    val minLevel: Int,
    val maxLevel: Int,
    var currentLevel: Int
)

data class EqualizerPreset(
    val name: String,
    val index: Short
)

data class EqualizerState(
    val isEnabled: Boolean = false,
    val bands: List<EqualizerBand> = emptyList(),
    val presets: List<EqualizerPreset> = emptyList(),
    val currentPresetIndex: Short = -1,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val reverbPreset: Short = PresetReverb.PRESET_NONE,
    val minBandLevel: Int = -1500,
    val maxBandLevel: Int = 1500
)

class EqualizerManager(private val context: Context) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    private val prefs = context.getSharedPreferences("equalizer_prefs", Context.MODE_PRIVATE)

    // Predefined custom presets
    val customPresets = listOf(
        "Flat" to intArrayOf(0, 0, 0, 0, 0),
        "Bass Boost" to intArrayOf(600, 400, 0, 0, 0),
        "Bass Reducer" to intArrayOf(-600, -400, 0, 0, 0),
        "Treble Boost" to intArrayOf(0, 0, 0, 400, 600),
        "Treble Reducer" to intArrayOf(0, 0, 0, -400, -600),
        "Vocal Boost" to intArrayOf(-200, 0, 400, 400, 0),
        "Rock" to intArrayOf(500, 300, 0, 300, 500),
        "Pop" to intArrayOf(-100, 200, 400, 200, -100),
        "Jazz" to intArrayOf(300, 0, 100, 200, 400),
        "Classical" to intArrayOf(400, 200, -100, 200, 400),
        "Dance" to intArrayOf(500, 400, 200, 0, -100),
        "Hip Hop" to intArrayOf(500, 400, 0, 100, 300),
        "Electronic" to intArrayOf(400, 200, 0, 200, 400),
        "Acoustic" to intArrayOf(400, 200, 100, 300, 200),
        "R&B" to intArrayOf(300, 500, 200, 100, 300),
        "Podcast" to intArrayOf(-200, 0, 300, 300, 0),
        "Deep Bass" to intArrayOf(800, 600, 200, 0, -100),
        "Powerful" to intArrayOf(400, 300, 200, 400, 500)
    )

    // Reverb presets
    val reverbPresets = listOf(
        "None" to PresetReverb.PRESET_NONE,
        "Small Room" to PresetReverb.PRESET_SMALLROOM,
        "Medium Room" to PresetReverb.PRESET_MEDIUMROOM,
        "Large Room" to PresetReverb.PRESET_LARGEROOM,
        "Medium Hall" to PresetReverb.PRESET_MEDIUMHALL,
        "Large Hall" to PresetReverb.PRESET_LARGEHALL,
        "Plate" to PresetReverb.PRESET_PLATE
    )

    fun initialize(audioSessionId: Int) {
        try {
            release() // Release any existing instances
            currentAudioSessionId = audioSessionId

            // Initialize Equalizer
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = prefs.getBoolean("eq_enabled", false)
            }

            // Initialize Bass Boost
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = prefs.getBoolean("eq_enabled", false)
                if (strengthSupported) {
                    setStrength(prefs.getInt("bass_strength", 0).toShort())
                }
            }

            // Initialize Virtualizer
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = prefs.getBoolean("eq_enabled", false)
                if (strengthSupported) {
                    setStrength(prefs.getInt("virtualizer_strength", 0).toShort())
                }
            }

            // Initialize Reverb
            try {
                presetReverb = PresetReverb(0, audioSessionId).apply {
                    enabled = prefs.getBoolean("eq_enabled", false)
                    preset = prefs.getInt("reverb_preset", PresetReverb.PRESET_NONE.toInt()).toShort()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Reverb not supported on this device")
            }

            loadState()
            Log.d(TAG, "Equalizer initialized for session: $audioSessionId")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing equalizer", e)
        }
    }

    private fun loadState() {
        equalizer?.let { eq ->
            val bands = mutableListOf<EqualizerBand>()
            val numberOfBands = eq.numberOfBands

            for (i in 0 until numberOfBands) {
                val band = EqualizerBand(
                    index = i,
                    centerFreq = eq.getCenterFreq(i.toShort()) / 1000,
                    minLevel = eq.bandLevelRange[0].toInt(),
                    maxLevel = eq.bandLevelRange[1].toInt(),
                    currentLevel = prefs.getInt("band_$i", eq.getBandLevel(i.toShort()).toInt())
                )
                bands.add(band)

                // Apply saved level
                eq.setBandLevel(i.toShort(), band.currentLevel.toShort())
            }

            val presets = mutableListOf<EqualizerPreset>()
            for (i in 0 until eq.numberOfPresets) {
                presets.add(EqualizerPreset(eq.getPresetName(i.toShort()), i.toShort()))
            }

            _state.value = EqualizerState(
                isEnabled = eq.enabled,
                bands = bands,
                presets = presets,
                currentPresetIndex = prefs.getInt("current_preset", -1).toShort(),
                bassBoostStrength = prefs.getInt("bass_strength", 0),
                virtualizerStrength = prefs.getInt("virtualizer_strength", 0),
                reverbPreset = prefs.getInt("reverb_preset", PresetReverb.PRESET_NONE.toInt()).toShort(),
                minBandLevel = eq.bandLevelRange[0].toInt(),
                maxBandLevel = eq.bandLevelRange[1].toInt()
            )
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            presetReverb?.enabled = enabled

            _state.value = _state.value.copy(isEnabled = enabled)
            prefs.edit().putBoolean("eq_enabled", enabled).apply()

            Log.d(TAG, "Equalizer enabled: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting equalizer enabled", e)
        }
    }

    fun setBandLevel(bandIndex: Int, level: Int) {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), level.toShort())

            val bands = _state.value.bands.toMutableList()
            if (bandIndex < bands.size) {
                bands[bandIndex] = bands[bandIndex].copy(currentLevel = level)
            }

            _state.value = _state.value.copy(
                bands = bands,
                currentPresetIndex = -1 // Custom settings
            )

            prefs.edit()
                .putInt("band_$bandIndex", level)
                .putInt("current_preset", -1)
                .apply()

        } catch (e: Exception) {
            Log.e(TAG, "Error setting band level", e)
        }
    }

    fun applyPreset(presetIndex: Short) {
        try {
            equalizer?.usePreset(presetIndex)
            loadState()

            _state.value = _state.value.copy(currentPresetIndex = presetIndex)
            prefs.edit().putInt("current_preset", presetIndex.toInt()).apply()

            // Save band levels
            _state.value.bands.forEachIndexed { index, band ->
                prefs.edit().putInt("band_$index", band.currentLevel).apply()
            }

            Log.d(TAG, "Applied preset: $presetIndex")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying preset", e)
        }
    }

    fun applyCustomPreset(presetName: String) {
        val preset = customPresets.find { it.first == presetName }
        if (preset != null) {
            val levels = preset.second
            _state.value.bands.forEachIndexed { index, _ ->
                if (index < levels.size) {
                    setBandLevel(index, levels[index])
                }
            }
            Log.d(TAG, "Applied custom preset: $presetName")
        }
    }

    fun setBassBoost(strength: Int) {
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(strength.toShort())
                _state.value = _state.value.copy(bassBoostStrength = strength)
                prefs.edit().putInt("bass_strength", strength).apply()
                Log.d(TAG, "Bass boost set to: $strength")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting bass boost", e)
        }
    }

    fun setVirtualizer(strength: Int) {
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(strength.toShort())
                _state.value = _state.value.copy(virtualizerStrength = strength)
                prefs.edit().putInt("virtualizer_strength", strength).apply()
                Log.d(TAG, "Virtualizer set to: $strength")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting virtualizer", e)
        }
    }

    fun setReverbPreset(preset: Short) {
        try {
            presetReverb?.preset = preset
            _state.value = _state.value.copy(reverbPreset = preset)
            prefs.edit().putInt("reverb_preset", preset.toInt()).apply()
            Log.d(TAG, "Reverb preset set to: $preset")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting reverb preset", e)
        }
    }

    fun resetToDefault() {
        _state.value.bands.forEachIndexed { index, _ ->
            setBandLevel(index, 0)
        }
        setBassBoost(0)
        setVirtualizer(0)
        setReverbPreset(PresetReverb.PRESET_NONE)
        _state.value = _state.value.copy(currentPresetIndex = -1)
        prefs.edit().putInt("current_preset", -1).apply()
        Log.d(TAG, "Reset to default")
    }

    private var currentAudioSessionId: Int = 0

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            presetReverb?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing equalizer", e)
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
        currentAudioSessionId = 0
    }

    fun getAudioSessionId(): Int {
        return currentAudioSessionId
    }
}

