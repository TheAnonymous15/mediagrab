package com.example.dwn.player.engine

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ============================================
 * OUTPUT ROUTING MODULE
 * ============================================
 *
 * Advanced output routing with:
 * - Output type detection (speaker, headphones, Bluetooth, USB)
 * - Per-output EQ and FX profiles
 * - Automatic profile switching
 * - Virtual output support
 */
class OutputRoutingModule(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentOutput = MutableStateFlow(OutputType.SPEAKER)
    val currentOutput: StateFlow<OutputType> = _currentOutput.asStateFlow()

    private val outputProfiles = mutableMapOf<OutputType, OutputProfile>()
    private val _currentProfile = MutableStateFlow<OutputProfile?>(null)
    val currentProfile: StateFlow<OutputProfile?> = _currentProfile.asStateFlow()

    // Callback when output changes
    var onOutputChanged: ((OutputType, OutputProfile?) -> Unit)? = null

    // ============================================
    // INITIALIZATION
    // ============================================

    init {
        // Initialize default profiles
        initializeDefaultProfiles()
        // Detect current output
        detectOutput()
    }

    private fun initializeDefaultProfiles() {
        // Speaker profile - enhanced bass, no virtualizer
        outputProfiles[OutputType.SPEAKER] = OutputProfile(
            name = "Speaker",
            outputType = OutputType.SPEAKER,
            eqEnabled = true,
            bassBoost = 500,
            virtualizer = 0,
            loudnessGain = 300
        )

        // Headphones profile - balanced with virtualizer
        outputProfiles[OutputType.HEADPHONES] = OutputProfile(
            name = "Headphones",
            outputType = OutputType.HEADPHONES,
            eqEnabled = true,
            bassBoost = 200,
            virtualizer = 400,
            loudnessGain = 0
        )

        // Bluetooth profile - loudness compensation
        outputProfiles[OutputType.BLUETOOTH] = OutputProfile(
            name = "Bluetooth",
            outputType = OutputType.BLUETOOTH,
            eqEnabled = true,
            bassBoost = 300,
            virtualizer = 200,
            loudnessGain = 400
        )

        // USB profile - high fidelity, minimal processing
        outputProfiles[OutputType.USB] = OutputProfile(
            name = "USB/DAC",
            outputType = OutputType.USB,
            eqEnabled = false,
            bassBoost = 0,
            virtualizer = 0,
            loudnessGain = 0
        )

        // Virtual output - passthrough
        outputProfiles[OutputType.VIRTUAL] = OutputProfile(
            name = "Virtual",
            outputType = OutputType.VIRTUAL,
            eqEnabled = false,
            bassBoost = 0,
            virtualizer = 0,
            loudnessGain = 0
        )
    }

    // ============================================
    // OUTPUT DETECTION
    // ============================================

    /**
     * Detect current audio output type
     */
    fun detectOutput(): OutputType {
        val outputType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            when {
                devices.any {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                } -> OutputType.HEADPHONES

                devices.any {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                } -> OutputType.BLUETOOTH

                devices.any {
                    it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                    it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
                } -> OutputType.USB

                else -> OutputType.SPEAKER
            }
        } else {
            @Suppress("DEPRECATION")
            when {
                audioManager.isWiredHeadsetOn -> OutputType.HEADPHONES
                audioManager.isBluetoothA2dpOn -> OutputType.BLUETOOTH
                else -> OutputType.SPEAKER
            }
        }

        if (_currentOutput.value != outputType) {
            _currentOutput.value = outputType
            _currentProfile.value = outputProfiles[outputType]
            onOutputChanged?.invoke(outputType, _currentProfile.value)
        }

        return outputType
    }

    /**
     * Check if headphones are connected
     */
    fun isHeadphonesConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devices.any {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.isWiredHeadsetOn
        }
    }

    /**
     * Check if Bluetooth audio is connected
     */
    fun isBluetoothConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devices.any {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.isBluetoothA2dpOn
        }
    }

    /**
     * Get available output devices
     */
    fun getAvailableOutputs(): List<OutputType> {
        val outputs = mutableListOf(OutputType.SPEAKER) // Always available

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

            if (devices.any {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
            }) {
                outputs.add(OutputType.HEADPHONES)
            }

            if (devices.any {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }) {
                outputs.add(OutputType.BLUETOOTH)
            }

            if (devices.any {
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }) {
                outputs.add(OutputType.USB)
            }
        }

        return outputs
    }

    // ============================================
    // OUTPUT PROFILES
    // ============================================

    /**
     * Set profile for an output type
     */
    fun setOutputProfile(outputType: OutputType, profile: OutputProfile) {
        outputProfiles[outputType] = profile

        if (_currentOutput.value == outputType) {
            _currentProfile.value = profile
            onOutputChanged?.invoke(outputType, profile)
        }
    }

    /**
     * Get profile for an output type
     */
    fun getOutputProfile(outputType: OutputType): OutputProfile? = outputProfiles[outputType]

    /**
     * Get current profile
     */
    fun getCurrentProfile(): OutputProfile? = _currentProfile.value

    /**
     * Update current profile property
     */
    fun updateCurrentProfile(
        eqEnabled: Boolean? = null,
        eqBandLevels: List<Int>? = null,
        bassBoost: Int? = null,
        virtualizer: Int? = null,
        loudnessGain: Int? = null,
        reverbPreset: Int? = null
    ) {
        val current = _currentProfile.value ?: return
        val outputType = _currentOutput.value

        val updated = current.copy(
            eqEnabled = eqEnabled ?: current.eqEnabled,
            eqBandLevels = eqBandLevels ?: current.eqBandLevels,
            bassBoost = bassBoost ?: current.bassBoost,
            virtualizer = virtualizer ?: current.virtualizer,
            loudnessGain = loudnessGain ?: current.loudnessGain,
            reverbPreset = reverbPreset ?: current.reverbPreset
        )

        outputProfiles[outputType] = updated
        _currentProfile.value = updated
        onOutputChanged?.invoke(outputType, updated)
    }

    /**
     * Reset profile to defaults
     */
    fun resetProfile(outputType: OutputType) {
        initializeDefaultProfiles()
        if (_currentOutput.value == outputType) {
            _currentProfile.value = outputProfiles[outputType]
            onOutputChanged?.invoke(outputType, _currentProfile.value)
        }
    }

    /**
     * Reset all profiles to defaults
     */
    fun resetAllProfiles() {
        initializeDefaultProfiles()
        _currentProfile.value = outputProfiles[_currentOutput.value]
        onOutputChanged?.invoke(_currentOutput.value, _currentProfile.value)
    }

    // ============================================
    // EXPORT/IMPORT
    // ============================================

    /**
     * Export all profiles
     */
    fun exportProfiles(): Map<OutputType, OutputProfile> = outputProfiles.toMap()

    /**
     * Import profiles
     */
    fun importProfiles(profiles: Map<OutputType, OutputProfile>) {
        profiles.forEach { (type, profile) ->
            outputProfiles[type] = profile
        }
        _currentProfile.value = outputProfiles[_currentOutput.value]
    }
}

