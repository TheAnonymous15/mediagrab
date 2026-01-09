package com.example.dwn.player.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ============================================
 * LISTENING MODE MODULE
 * ============================================
 *
 * Context-aware listening modes that adapt audio:
 * - Focus Mode
 * - Night Mode
 * - Workout Mode
 * - Podcast Mode
 * - Low Latency Mode
 * - Car Mode
 * - Audio Focus Mode
 * - Visual Focus Mode
 *
 * Modes can auto-activate based on:
 * - Time of day
 * - Headphones connected
 * - User behavior
 */
class ListeningModeModule {

    private val _currentMode = MutableStateFlow(ListeningMode.NORMAL)
    val currentMode: StateFlow<ListeningMode> = _currentMode.asStateFlow()

    private val _isAutoModeEnabled = MutableStateFlow(false)
    val isAutoModeEnabled: StateFlow<Boolean> = _isAutoModeEnabled.asStateFlow()

    // Callback to apply mode settings
    var onModeChanged: ((ListeningMode, ModeSettings) -> Unit)? = null

    // Mode settings
    private val modeSettings = mutableMapOf<ListeningMode, ModeSettings>()

    init {
        initializeDefaultSettings()
    }

    private fun initializeDefaultSettings() {
        // Normal - balanced
        modeSettings[ListeningMode.NORMAL] = ModeSettings(
            bassBoost = 0,
            virtualizer = 0,
            loudnessGain = 0,
            eqPreset = "Flat",
            description = "Balanced audio with no enhancements"
        )

        // Focus - enhanced clarity
        modeSettings[ListeningMode.FOCUS] = ModeSettings(
            bassBoost = 200,
            virtualizer = 500,
            loudnessGain = 500,
            eqPreset = "Clarity",
            description = "Enhanced clarity for focused listening"
        )

        // Night - soft dynamics
        modeSettings[ListeningMode.NIGHT] = ModeSettings(
            bassBoost = 300,
            virtualizer = 300,
            loudnessGain = -200,
            eqPreset = "Soft",
            description = "Reduced loudness spikes for late night listening"
        )

        // Workout - high energy
        modeSettings[ListeningMode.WORKOUT] = ModeSettings(
            bassBoost = 800,
            virtualizer = 600,
            loudnessGain = 800,
            eqPreset = "Bass Heavy",
            description = "High energy with enhanced bass for workouts"
        )

        // Podcast - speech clarity
        modeSettings[ListeningMode.PODCAST] = ModeSettings(
            bassBoost = 0,
            virtualizer = 200,
            loudnessGain = 600,
            eqPreset = "Voice",
            description = "Optimized for speech clarity"
        )

        // Low Latency - minimal processing
        modeSettings[ListeningMode.LOW_LATENCY] = ModeSettings(
            bassBoost = 0,
            virtualizer = 0,
            loudnessGain = 0,
            eqPreset = "Bypass",
            disableAllFX = true,
            description = "Minimal latency for musicians and DJs"
        )

        // Car - loud and clear
        modeSettings[ListeningMode.CAR] = ModeSettings(
            bassBoost = 600,
            virtualizer = 300,
            loudnessGain = 700,
            eqPreset = "Car",
            description = "Optimized for car audio systems"
        )

        // Audio Focus - screen dimmed, audio enhanced
        modeSettings[ListeningMode.AUDIO_FOCUS] = ModeSettings(
            bassBoost = 400,
            virtualizer = 600,
            loudnessGain = 500,
            eqPreset = "Immersive",
            description = "Enhanced audio for immersive listening"
        )

        // Visual Focus - minimal audio FX
        modeSettings[ListeningMode.VISUAL_FOCUS] = ModeSettings(
            bassBoost = 100,
            virtualizer = 100,
            loudnessGain = 0,
            eqPreset = "Flat",
            description = "Minimal processing for video watching"
        )
    }

    // ============================================
    // MODE CONTROL
    // ============================================

    /**
     * Set listening mode
     */
    fun setMode(mode: ListeningMode) {
        _currentMode.value = mode
        val settings = modeSettings[mode] ?: return
        onModeChanged?.invoke(mode, settings)
    }

    /**
     * Get current mode
     */
    fun getMode(): ListeningMode = _currentMode.value

    /**
     * Get mode settings
     */
    fun getModeSettings(mode: ListeningMode): ModeSettings? = modeSettings[mode]

    /**
     * Get current mode settings
     */
    fun getCurrentSettings(): ModeSettings? = modeSettings[_currentMode.value]

    /**
     * Update mode settings
     */
    fun updateModeSettings(mode: ListeningMode, settings: ModeSettings) {
        modeSettings[mode] = settings
        if (_currentMode.value == mode) {
            onModeChanged?.invoke(mode, settings)
        }
    }

    /**
     * Reset mode to defaults
     */
    fun resetMode(mode: ListeningMode) {
        initializeDefaultSettings()
        if (_currentMode.value == mode) {
            val settings = modeSettings[mode] ?: return
            onModeChanged?.invoke(mode, settings)
        }
    }

    // ============================================
    // AUTO MODE
    // ============================================

    /**
     * Enable/disable auto mode switching
     */
    fun setAutoModeEnabled(enabled: Boolean) {
        _isAutoModeEnabled.value = enabled
    }

    /**
     * Suggest mode based on context
     */
    fun suggestMode(
        hour: Int,
        isHeadphonesConnected: Boolean,
        isBluetoothConnected: Boolean,
        isInCar: Boolean = false
    ): ListeningMode {
        return when {
            isInCar -> ListeningMode.CAR
            hour in 22..23 || hour in 0..6 -> ListeningMode.NIGHT
            isHeadphonesConnected -> ListeningMode.FOCUS
            isBluetoothConnected -> ListeningMode.NORMAL
            else -> ListeningMode.NORMAL
        }
    }

    /**
     * Auto-apply mode based on context
     */
    fun autoApplyMode(
        hour: Int,
        isHeadphonesConnected: Boolean,
        isBluetoothConnected: Boolean,
        isInCar: Boolean = false
    ) {
        if (!_isAutoModeEnabled.value) return

        val suggestedMode = suggestMode(hour, isHeadphonesConnected, isBluetoothConnected, isInCar)
        if (_currentMode.value != suggestedMode) {
            setMode(suggestedMode)
        }
    }

    // ============================================
    // MODE INFO
    // ============================================

    /**
     * Get all available modes
     */
    fun getAllModes(): List<ListeningMode> = ListeningMode.values().toList()

    /**
     * Get mode description
     */
    fun getModeDescription(mode: ListeningMode): String {
        return modeSettings[mode]?.description ?: ""
    }

    /**
     * Get mode icon name
     */
    fun getModeIconName(mode: ListeningMode): String {
        return when (mode) {
            ListeningMode.NORMAL -> "equalizer"
            ListeningMode.FOCUS -> "headphones"
            ListeningMode.NIGHT -> "nightlight"
            ListeningMode.WORKOUT -> "fitness_center"
            ListeningMode.PODCAST -> "mic"
            ListeningMode.LOW_LATENCY -> "speed"
            ListeningMode.CAR -> "directions_car"
            ListeningMode.AUDIO_FOCUS -> "hearing"
            ListeningMode.VISUAL_FOCUS -> "visibility"
        }
    }
}

/**
 * Settings for a listening mode
 */
data class ModeSettings(
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudnessGain: Int = 0,
    val eqPreset: String = "Flat",
    val disableAllFX: Boolean = false,
    val description: String = ""
)

