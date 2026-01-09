package com.example.dwn.context

import android.bluetooth.BluetoothClass
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

// ============================================
// 🎯 CONTEXT MEDIA MODE DATA MODELS
// ============================================

/**
 * Detected context signals from device sensors and state
 */
data class ContextSignals(
    // Audio output
    val audioOutput: AudioOutputType = AudioOutputType.SPEAKER,
    val bluetoothDeviceType: BluetoothDeviceType? = null,
    val isHeadphonesConnected: Boolean = false,
    val isCasting: Boolean = false,

    // Screen & Device
    val isScreenOn: Boolean = true,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,

    // Motion
    val motionState: MotionState = MotionState.STATIONARY,
    val currentSpeed: Float = 0f,  // m/s

    // Environment
    val timeOfDay: TimeOfDay = TimeOfDay.DAY,
    val hourOfDay: Int = 12,
    val ambientNoiseLevel: Float = 0f,  // dB estimate

    // App state
    val isAppInForeground: Boolean = true,
    val isMediaPlaying: Boolean = false,

    // Location hints (privacy-first, no actual location)
    val isInVehicle: Boolean = false,
    val isMovingFast: Boolean = false
)

enum class AudioOutputType {
    SPEAKER,            // Built-in speaker
    WIRED_HEADPHONES,   // 3.5mm or USB-C wired
    BLUETOOTH_EARBUDS,  // TWS earbuds
    BLUETOOTH_HEADPHONES, // Over-ear Bluetooth
    BLUETOOTH_SPEAKER,  // Portable BT speaker
    BLUETOOTH_CAR,      // Car audio system
    CAST_DEVICE,        // Chromecast, AirPlay
    USB_AUDIO,          // USB DAC
    HDMI                // HDMI output
}

enum class BluetoothDeviceType {
    EARBUDS,
    HEADPHONES,
    SPEAKER,
    CAR_AUDIO,
    SOUNDBAR,
    UNKNOWN
}

enum class MotionState {
    STATIONARY,
    WALKING,
    RUNNING,
    CYCLING,
    DRIVING,
    IN_VEHICLE,
    UNKNOWN
}

enum class TimeOfDay {
    MORNING,    // 5am - 11am
    DAY,        // 11am - 5pm
    EVENING,    // 5pm - 9pm
    NIGHT       // 9pm - 5am
}

// ============================================
// 🎨 CONTEXT MODES
// ============================================

enum class ContextMode(
    val displayName: String,
    val icon: String,
    val description: String
) {
    AUTO("Auto", "🔮", "Automatically adapts to your context"),
    WALK("Walk", "🚶", "Optimized for walking with headphones"),
    DRIVE("Drive", "🚗", "Safe audio for driving"),
    FOCUS("Focus", "🎯", "Minimal distractions for concentration"),
    NIGHT("Night", "🌙", "Gentle audio for late hours"),
    CAST("Cast", "📺", "Optimized for casting to external devices"),
    WORKOUT("Workout", "💪", "Energizing audio for exercise"),
    COMMUTE("Commute", "🚇", "Balanced for public transport"),
    CUSTOM("Custom", "⚙️", "Your personalized settings")
}

/**
 * Audio processing settings for each mode
 */
data class ModeAudioProfile(
    // EQ
    val eqEnabled: Boolean = true,
    val eqPreset: ModeEQPreset = ModeEQPreset.FLAT,
    val bassAdjustment: Float = 0f,      // -12 to +12 dB
    val trebleAdjustment: Float = 0f,    // -12 to +12 dB
    val midAdjustment: Float = 0f,       // -12 to +12 dB

    // Dynamics
    val compressionEnabled: Boolean = false,
    val compressionStrength: CompressionStrength = CompressionStrength.OFF,
    val limiterEnabled: Boolean = true,
    val limiterThreshold: Float = -1f,   // dBFS

    // Speech
    val speechEnhancement: Boolean = false,
    val speechEnhancementLevel: Float = 0.5f,
    val dialogueBoost: Boolean = false,

    // Volume
    val autoVolumeLeveling: Boolean = false,
    val loudnessNormalization: Boolean = false,
    val targetLoudness: Float = -14f,    // LUFS
    val maxVolume: Float = 1f,           // 0-1, for night mode

    // FX
    val spatialAudioEnabled: Boolean = true,
    val reverbEnabled: Boolean = false,
    val reverbAmount: Float = 0f
)

enum class ModeEQPreset(val displayName: String) {
    FLAT("Flat"),
    VOICE_CLARITY("Voice Clarity"),
    BASS_REDUCED("Bass Reduced"),
    BASS_BOOST("Bass Boost"),
    TREBLE_BOOST("Treble Boost"),
    WARM("Warm"),
    BRIGHT("Bright"),
    PODCAST("Podcast"),
    MUSIC("Music"),
    MOVIE("Movie"),
    NIGHT("Night Soft")
}

enum class CompressionStrength(val displayName: String, val ratio: Float) {
    OFF("Off", 1f),
    GENTLE("Gentle", 2f),
    MODERATE("Moderate", 4f),
    AGGRESSIVE("Aggressive", 8f),
    BROADCAST("Broadcast", 12f)
}

/**
 * UI adaptation settings for each mode
 */
data class ModeUIProfile(
    val uiComplexity: UIComplexity = UIComplexity.FULL,
    val controlSize: ControlSize = ControlSize.NORMAL,
    val gesturesEnabled: Boolean = true,
    val hapticFeedback: Boolean = true,
    val visualFeedback: Boolean = true,
    val showNotifications: Boolean = true,
    val notificationPriority: NotificationPriority = NotificationPriority.NORMAL,
    val darkModePreference: DarkModePreference = DarkModePreference.SYSTEM,
    val reducedMotion: Boolean = false,
    val autoHideControls: Boolean = false
)

enum class UIComplexity {
    MINIMAL,    // Just play/pause
    SIMPLE,     // Basic controls
    NORMAL,     // Standard UI
    FULL        // All features visible
}

enum class ControlSize {
    COMPACT,
    NORMAL,
    LARGE,
    EXTRA_LARGE
}

enum class NotificationPriority {
    SILENT,
    LOW,
    NORMAL,
    HIGH
}

enum class DarkModePreference {
    LIGHT,
    DARK,
    SYSTEM,
    AUTO_TIME  // Dark at night
}

// ============================================
// 📋 MODE CONFIGURATIONS
// ============================================

/**
 * Complete mode configuration
 */
data class ModeConfiguration(
    val mode: ContextMode,
    val audioProfile: ModeAudioProfile,
    val uiProfile: ModeUIProfile,
    val isLocked: Boolean = false,
    val triggers: List<ModeTrigger> = emptyList()
)

/**
 * Conditions that trigger a mode
 */
data class ModeTrigger(
    val type: TriggerType,
    val value: String,
    val priority: Int = 0
)

enum class TriggerType {
    AUDIO_OUTPUT,
    BLUETOOTH_DEVICE,
    MOTION_STATE,
    TIME_RANGE,
    SCREEN_STATE,
    CASTING,
    MANUAL
}

// ============================================
// 🎛️ DEFAULT MODE CONFIGURATIONS
// ============================================

val defaultModeConfigurations = mapOf(
    ContextMode.WALK to ModeConfiguration(
        mode = ContextMode.WALK,
        audioProfile = ModeAudioProfile(
            eqEnabled = true,
            eqPreset = ModeEQPreset.VOICE_CLARITY,
            bassAdjustment = -3f,
            speechEnhancement = true,
            speechEnhancementLevel = 0.7f,
            compressionEnabled = true,
            compressionStrength = CompressionStrength.GENTLE
        ),
        uiProfile = ModeUIProfile(
            uiComplexity = UIComplexity.SIMPLE,
            gesturesEnabled = true,
            controlSize = ControlSize.LARGE,
            autoHideControls = true
        ),
        triggers = listOf(
            ModeTrigger(TriggerType.MOTION_STATE, "WALKING"),
            ModeTrigger(TriggerType.AUDIO_OUTPUT, "WIRED_HEADPHONES"),
            ModeTrigger(TriggerType.AUDIO_OUTPUT, "BLUETOOTH_EARBUDS")
        )
    ),

    ContextMode.DRIVE to ModeConfiguration(
        mode = ContextMode.DRIVE,
        audioProfile = ModeAudioProfile(
            eqEnabled = true,
            eqPreset = ModeEQPreset.VOICE_CLARITY,
            speechEnhancement = true,
            speechEnhancementLevel = 1f,
            dialogueBoost = true,
            compressionEnabled = true,
            compressionStrength = CompressionStrength.AGGRESSIVE,
            autoVolumeLeveling = true
        ),
        uiProfile = ModeUIProfile(
            uiComplexity = UIComplexity.MINIMAL,
            controlSize = ControlSize.EXTRA_LARGE,
            gesturesEnabled = false,
            showNotifications = false,
            reducedMotion = true
        ),
        triggers = listOf(
            ModeTrigger(TriggerType.AUDIO_OUTPUT, "BLUETOOTH_CAR"),
            ModeTrigger(TriggerType.MOTION_STATE, "DRIVING")
        )
    ),

    ContextMode.FOCUS to ModeConfiguration(
        mode = ContextMode.FOCUS,
        audioProfile = ModeAudioProfile(
            eqEnabled = true,
            eqPreset = ModeEQPreset.FLAT,
            compressionEnabled = false,
            spatialAudioEnabled = false,
            reverbEnabled = false,
            loudnessNormalization = true
        ),
        uiProfile = ModeUIProfile(
            uiComplexity = UIComplexity.MINIMAL,
            showNotifications = false,
            notificationPriority = NotificationPriority.SILENT,
            autoHideControls = true
        ),
        triggers = listOf(
            ModeTrigger(TriggerType.MANUAL, "FOCUS")
        )
    ),

    ContextMode.NIGHT to ModeConfiguration(
        mode = ContextMode.NIGHT,
        audioProfile = ModeAudioProfile(
            eqEnabled = true,
            eqPreset = ModeEQPreset.NIGHT,
            bassAdjustment = -4f,
            compressionEnabled = true,
            compressionStrength = CompressionStrength.MODERATE,
            autoVolumeLeveling = true,
            maxVolume = 0.7f,
            limiterEnabled = true,
            limiterThreshold = -6f
        ),
        uiProfile = ModeUIProfile(
            darkModePreference = DarkModePreference.DARK,
            showNotifications = false,
            notificationPriority = NotificationPriority.SILENT,
            reducedMotion = true,
            hapticFeedback = false
        ),
        triggers = listOf(
            ModeTrigger(TriggerType.TIME_RANGE, "21:00-05:00")
        )
    ),

    ContextMode.CAST to ModeConfiguration(
        mode = ContextMode.CAST,
        audioProfile = ModeAudioProfile(
            eqEnabled = false,
            compressionEnabled = false,
            spatialAudioEnabled = false,
            reverbEnabled = false,
            loudnessNormalization = false
        ),
        uiProfile = ModeUIProfile(
            uiComplexity = UIComplexity.NORMAL,
            visualFeedback = true
        ),
        triggers = listOf(
            ModeTrigger(TriggerType.CASTING, "true"),
            ModeTrigger(TriggerType.AUDIO_OUTPUT, "CAST_DEVICE")
        )
    ),

    ContextMode.WORKOUT to ModeConfiguration(
        mode = ContextMode.WORKOUT,
        audioProfile = ModeAudioProfile(
            eqEnabled = true,
            eqPreset = ModeEQPreset.BASS_BOOST,
            bassAdjustment = 4f,
            compressionEnabled = true,
            compressionStrength = CompressionStrength.MODERATE,
            loudnessNormalization = true,
            targetLoudness = -10f
        ),
        uiProfile = ModeUIProfile(
            uiComplexity = UIComplexity.SIMPLE,
            controlSize = ControlSize.LARGE,
            gesturesEnabled = true,
            autoHideControls = true
        ),
        triggers = listOf(
            ModeTrigger(TriggerType.MOTION_STATE, "RUNNING")
        )
    ),

    ContextMode.COMMUTE to ModeConfiguration(
        mode = ContextMode.COMMUTE,
        audioProfile = ModeAudioProfile(
            eqEnabled = true,
            eqPreset = ModeEQPreset.VOICE_CLARITY,
            speechEnhancement = true,
            speechEnhancementLevel = 0.5f,
            compressionEnabled = true,
            compressionStrength = CompressionStrength.GENTLE,
            loudnessNormalization = true
        ),
        uiProfile = ModeUIProfile(
            uiComplexity = UIComplexity.NORMAL,
            controlSize = ControlSize.NORMAL
        ),
        triggers = listOf(
            ModeTrigger(TriggerType.MOTION_STATE, "IN_VEHICLE")
        )
    )
)

// ============================================
// 💾 USER PREFERENCES
// ============================================

data class ContextModePreferences(
    val autoModeEnabled: Boolean = true,
    val currentMode: ContextMode = ContextMode.AUTO,
    val lockedMode: ContextMode? = null,
    val customConfigurations: Map<ContextMode, ModeConfiguration> = emptyMap(),
    val deviceProfiles: Map<String, ModeAudioProfile> = emptyMap(),
    val modeHistory: List<ModeHistoryEntry> = emptyList(),
    val privacySettings: PrivacySettings = PrivacySettings()
)

data class ModeHistoryEntry(
    val mode: ContextMode,
    val timestamp: Long,
    val trigger: TriggerType,
    val duration: Long = 0
)

data class PrivacySettings(
    val useMotionSensors: Boolean = true,
    val useBluetoothInfo: Boolean = true,
    val useTimeBasedModes: Boolean = true,
    val collectAnalytics: Boolean = false
)

// ============================================
// 📊 CONTEXT STATE
// ============================================

data class ContextMediaState(
    val signals: ContextSignals = ContextSignals(),
    val activeMode: ContextMode = ContextMode.AUTO,
    val resolvedMode: ContextMode = ContextMode.AUTO,  // What AUTO resolves to
    val configuration: ModeConfiguration = defaultModeConfigurations[ContextMode.AUTO]
        ?: ModeConfiguration(ContextMode.AUTO, ModeAudioProfile(), ModeUIProfile()),
    val isLocked: Boolean = false,
    val lastModeChange: Long = System.currentTimeMillis(),
    val suggestedMode: ContextMode? = null,
    val preferences: ContextModePreferences = ContextModePreferences()
)

