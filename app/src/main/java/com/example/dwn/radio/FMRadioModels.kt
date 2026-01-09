package com.example.dwn.radio

import java.util.UUID

// ============================================
// 📻 FM RADIO WITH ONLINE FALLBACK - DATA MODELS
// ============================================

/**
 * Radio system operation mode
 */
enum class RadioMode(val displayName: String, val icon: String) {
    FM_OFFLINE("FM Radio", "📻"),
    ONLINE("Online Radio", "🌐"),
    HYBRID_AUTO("Smart Radio", "🔄")
}

/**
 * FM Radio hardware capability status
 */
enum class FMHardwareStatus(val displayName: String) {
    AVAILABLE("FM Radio Available"),
    UNAVAILABLE("FM Not Supported"),
    NO_ANTENNA("Connect Headphones"),
    DISABLED("FM Disabled"),
    UNKNOWN("Checking...")
}

/**
 * Signal quality level
 */
enum class SignalQuality(val displayName: String, val icon: String, val color: Long) {
    EXCELLENT("Excellent", "📶", 0xFF4CAF50),
    GOOD("Good", "📶", 0xFF8BC34A),
    FAIR("Fair", "📶", 0xFFFFC107),
    WEAK("Weak", "📶", 0xFFFF9800),
    POOR("Poor", "📶", 0xFFFF5722),
    NO_SIGNAL("No Signal", "📵", 0xFFE53935)
}

/**
 * Audio mode for FM reception
 */
enum class FMAudioMode(val displayName: String) {
    STEREO("Stereo"),
    MONO("Mono"),
    AUTO("Auto")
}

/**
 * FM Station data
 */
data class FMStation(
    val id: String = UUID.randomUUID().toString(),
    val frequency: Float,  // MHz (e.g., 98.5)
    val name: String = "",
    val callSign: String = "",
    val genre: RadioGenre? = null,
    val location: String = "",
    val isFavorite: Boolean = false,
    val isPreset: Boolean = false,
    val presetSlot: Int? = null,
    val rdsInfo: RDSInfo? = null,
    // Online fallback mapping
    val onlineStreamUrl: String? = null,
    val onlineStationId: String? = null,
    val hasOnlineFallback: Boolean = false,
    // Signal info (updated dynamically)
    val lastSignalStrength: Int = 0,
    val lastSignalQuality: SignalQuality = SignalQuality.NO_SIGNAL
)

/**
 * RDS (Radio Data System) information
 */
data class RDSInfo(
    val programService: String = "",  // Station name (PS)
    val radioText: String = "",       // Now playing info (RT)
    val programType: Int = 0,         // PTY code
    val programTypeName: String = "", // PTY name
    val trafficAnnouncement: Boolean = false,
    val clockTime: String? = null
)

/**
 * FM Radio state
 */
data class FMRadioState(
    // Mode
    val mode: RadioMode = RadioMode.HYBRID_AUTO,
    val hardwareStatus: FMHardwareStatus = FMHardwareStatus.UNKNOWN,

    // Current tuning
    val currentFrequency: Float = 87.5f,
    val currentStation: FMStation? = null,
    val isTuned: Boolean = false,

    // Signal
    val signalStrength: Int = 0,  // 0-100
    val signalQuality: SignalQuality = SignalQuality.NO_SIGNAL,
    val isSignalStable: Boolean = false,
    val noiseLevel: Float = 0f,

    // Audio
    val audioMode: FMAudioMode = FMAudioMode.AUTO,
    val isStereo: Boolean = false,
    val volume: Float = 1f,
    val isMuted: Boolean = false,

    // Playback
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isScanning: Boolean = false,
    val isSeeking: Boolean = false,

    // Fallback
    val isFallbackActive: Boolean = false,
    val fallbackReason: String? = null,
    val fallbackStation: RadioStation? = null,

    // Stations
    val presets: List<FMStation> = emptyList(),
    val favorites: List<FMStation> = emptyList(),
    val scannedStations: List<FMStation> = emptyList(),
    val recentStations: List<FMStation> = emptyList(),

    // RDS
    val rdsEnabled: Boolean = true,
    val currentRDS: RDSInfo? = null,

    // Settings
    val autoFallbackEnabled: Boolean = true,
    val signalThreshold: Int = 30,  // Signal strength below which fallback triggers
    val noiseReductionEnabled: Boolean = true,
    val headphoneConnected: Boolean = false,

    // Error
    val error: String? = null
)

/**
 * FM frequency band info
 */
data class FMBand(
    val name: String,
    val minFrequency: Float,
    val maxFrequency: Float,
    val stepSize: Float,  // MHz
    val region: String
) {
    companion object {
        val WORLDWIDE = FMBand("Worldwide", 87.5f, 108.0f, 0.1f, "International")
        val JAPAN = FMBand("Japan", 76.0f, 90.0f, 0.1f, "Japan")
        val EUROPE = FMBand("Europe", 87.5f, 108.0f, 0.05f, "Europe")
        val USA = FMBand("USA", 87.9f, 107.9f, 0.2f, "United States")
    }
}

/**
 * Signal monitoring event
 */
data class SignalEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val frequency: Float,
    val strength: Int,
    val quality: SignalQuality,
    val noiseFloor: Float,
    val triggeredFallback: Boolean = false
)

/**
 * Station scan result
 */
data class ScanResult(
    val stations: List<FMStation>,
    val scanDuration: Long,
    val stationsFound: Int,
    val startFrequency: Float,
    val endFrequency: Float
)

/**
 * FM to Online station mapping
 */
data class StationMapping(
    val fmFrequency: Float,
    val fmStationName: String,
    val onlineStreamUrl: String,
    val onlineStationId: String,
    val region: String,
    val isVerified: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Radio EQ preset for FM
 */
data class FMEQPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val bands: Map<Float, Float> = emptyMap(),  // Frequency to gain
    val noiseReduction: Float = 0f,
    val stereoWidth: Float = 1f,
    val bassBoost: Float = 0f
) {
    companion object {
        val FLAT = FMEQPreset(
            name = "Flat",
            description = "No EQ adjustment"
        )

        val FM_ENHANCED = FMEQPreset(
            name = "FM Enhanced",
            description = "Optimized for FM reception",
            bands = mapOf(
                60f to 2f,
                150f to 1f,
                400f to 0f,
                1000f to 1f,
                2400f to 2f,
                6000f to 3f,
                15000f to 2f
            ),
            noiseReduction = 0.3f,
            stereoWidth = 1.1f
        )

        val VOICE_CLARITY = FMEQPreset(
            name = "Voice Clarity",
            description = "Enhanced speech for talk radio",
            bands = mapOf(
                60f to -3f,
                150f to 0f,
                400f to 2f,
                1000f to 3f,
                2400f to 4f,
                6000f to 2f,
                15000f to -1f
            ),
            noiseReduction = 0.5f
        )

        val MUSIC = FMEQPreset(
            name = "Music",
            description = "Full spectrum for music",
            bands = mapOf(
                60f to 4f,
                150f to 2f,
                400f to 0f,
                1000f to 1f,
                2400f to 2f,
                6000f to 3f,
                15000f to 4f
            ),
            bassBoost = 0.2f,
            stereoWidth = 1.2f
        )
    }
}

/**
 * Fallback trigger info
 */
data class FallbackTrigger(
    val reason: FallbackReason,
    val timestamp: Long = System.currentTimeMillis(),
    val fmFrequency: Float,
    val signalStrength: Int,
    val onlineStationName: String?
)

enum class FallbackReason(val displayName: String) {
    WEAK_SIGNAL("Weak FM Signal"),
    NO_SIGNAL("No FM Signal"),
    SIGNAL_FLUCTUATION("Signal Unstable"),
    USER_REQUEST("Manual Switch"),
    FM_UNAVAILABLE("FM Hardware Unavailable"),
    NO_ANTENNA("No Antenna Connected"),
    NETWORK_AVAILABLE("Network Now Available")
}

/**
 * Reverse fallback (Online back to FM) trigger
 */
data class ReverseFallbackTrigger(
    val reason: ReverseFallbackReason,
    val timestamp: Long = System.currentTimeMillis(),
    val fmFrequency: Float,
    val signalStrength: Int
)

enum class ReverseFallbackReason(val displayName: String) {
    SIGNAL_RESTORED("FM Signal Restored"),
    INTERNET_LOST("Internet Connection Lost"),
    USER_REQUEST("Manual Switch"),
    BANDWIDTH_SAVE("Bandwidth Saving Mode")
}

/**
 * FM Radio statistics
 */
data class FMRadioStats(
    val totalListeningTime: Long = 0L,
    val fmListeningTime: Long = 0L,
    val onlineListeningTime: Long = 0L,
    val fallbackCount: Int = 0,
    val favoriteGenre: RadioGenre? = null,
    val mostListenedStation: FMStation? = null,
    val averageSignalStrength: Int = 0
)

// ============================================
// 📻 DEFAULT FM PRESETS & MAPPINGS
// ============================================

val defaultFMStationMappings = listOf(
    StationMapping(
        fmFrequency = 98.5f,
        fmStationName = "Local FM",
        onlineStreamUrl = "https://stream.example.com/radio",
        onlineStationId = "local_fm_online",
        region = "Local"
    )
)

val defaultFMPresets = listOf(
    FMStation(
        frequency = 87.5f,
        name = "Preset 1",
        presetSlot = 1,
        isPreset = true
    ),
    FMStation(
        frequency = 91.1f,
        name = "Preset 2",
        presetSlot = 2,
        isPreset = true
    ),
    FMStation(
        frequency = 95.5f,
        name = "Preset 3",
        presetSlot = 3,
        isPreset = true
    ),
    FMStation(
        frequency = 98.5f,
        name = "Preset 4",
        presetSlot = 4,
        isPreset = true
    ),
    FMStation(
        frequency = 101.1f,
        name = "Preset 5",
        presetSlot = 5,
        isPreset = true
    ),
    FMStation(
        frequency = 105.1f,
        name = "Preset 6",
        presetSlot = 6,
        isPreset = true
    )
)

val fmEQPresets = listOf(
    FMEQPreset.FLAT,
    FMEQPreset.FM_ENHANCED,
    FMEQPreset.VOICE_CLARITY,
    FMEQPreset.MUSIC
)

