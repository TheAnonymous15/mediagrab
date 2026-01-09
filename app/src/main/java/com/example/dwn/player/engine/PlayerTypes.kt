package com.example.dwn.player.engine

import android.media.MediaPlayer
import android.net.Uri

/**
 * ============================================
 * PLAYER TYPES & DATA CLASSES
 * ============================================
 *
 * Core data types used across all player modules
 */

// ============================================
// PLAYBACK STATE
// ============================================

data class PlaybackState(
    val isPlaying: Boolean = false,
    val isPrepared: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val bufferPercent: Int = 0,
    val speed: Float = 1f,
    val volume: Float = 1f,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false
) {
    val progress: Float
        get() = if (duration > 0) position.toFloat() / duration else 0f
}

enum class RepeatMode {
    OFF,
    ONE,
    ALL
}

// ============================================
// MEDIA ITEM
// ============================================

data class MediaItem(
    val id: String,
    val uri: Uri,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0,
    val artworkUri: Uri? = null,
    val type: MediaType = MediaType.AUDIO
)

enum class MediaType {
    AUDIO,
    VIDEO,
    STREAM
}

// ============================================
// PLAYER ERROR
// ============================================

data class PlayerError(
    val code: Int,
    val message: String
) {
    companion object {
        const val PLAYBACK_ERROR = 1
        const val FILE_NOT_FOUND = 2
        const val NETWORK_ERROR = 3
        const val PERMISSION_DENIED = 4
        const val MAX_LAYERS_REACHED = 5
        const val ANALYSIS_FAILED = 6
    }
}

// ============================================
// LISTENING MODES
// ============================================

enum class ListeningMode {
    NORMAL,
    FOCUS,
    NIGHT,
    WORKOUT,
    PODCAST,
    LOW_LATENCY,
    CAR,
    AUDIO_FOCUS,    // Screen dimmed, audio enhancements active
    VISUAL_FOCUS    // Minimal audio FX, enhanced visuals
}

// ============================================
// OUTPUT TYPES
// ============================================

enum class OutputType {
    SPEAKER,
    HEADPHONES,
    BLUETOOTH,
    USB,
    VIRTUAL
}

// ============================================
// PLAYER MODULES
// ============================================

enum class PlayerModule {
    CORE,           // Core playback (always enabled)
    FX,             // Audio effects
    EQUALIZER,      // Equalizer
    MULTI_LAYER,    // Multi-track playback
    AUTOMATION,     // FX automation
    ANALYSIS,       // Audio analysis
    TIMELINE,       // Graph-based timeline
    LOOPING,        // Smart looping
    LYRICS,         // Lyrics display
    REMIX,          // Remix capabilities
    OUTPUT_ROUTING  // Advanced output routing
}

// ============================================
// PLAYER LISTENER INTERFACE
// ============================================

interface PlayerListener {
    fun onPlaybackStateChanged(state: PlaybackState) {}
    fun onMediaChanged(media: MediaItem?) {}
    fun onPlaylistChanged(playlist: List<MediaItem>) {}
    fun onError(error: PlayerError) {}
    fun onAudioAnalysisReady(analysis: AudioAnalysis) {}
    fun onTimelineGraphReady(graph: TimelineGraphData) {}
    fun onOutputChanged(outputType: OutputType) {}
    fun onListeningModeChanged(mode: ListeningMode) {}
}

// ============================================
// AUDIO ANALYSIS TYPES
// ============================================

data class AudioAnalysis(
    val bpm: Int = 0,
    val key: String = "",
    val loudnessLufs: Double = 0.0,
    val peakDb: Double = 0.0,
    val silenceRegions: List<LongRange> = emptyList(),
    val energyProfile: List<Float> = emptyList(),
    val sceneChanges: List<Long> = emptyList(),
    val dialogueSegments: List<LongRange> = emptyList(),
    val beatPositions: List<Long> = emptyList()
)

// ============================================
// TIMELINE GRAPH TYPES
// ============================================

data class TimelineGraphData(
    val loudnessData: List<Float>,
    val frequencyEnergyData: List<Float>,
    val intensityData: List<Float>,
    val markers: List<JumpMarker>,
    val duration: Long
)

data class JumpMarker(
    val timeMs: Long,
    val type: MarkerType,
    val label: String = ""
)

enum class MarkerType {
    INTRO,
    VERSE,
    CHORUS,
    DROP,
    BRIDGE,
    OUTRO,
    SILENCE,
    SCENE_CHANGE,
    BEAT,
    CUSTOM
}

// ============================================
// BOOKMARK
// ============================================

data class Bookmark(
    val id: String,
    val position: Long,
    val name: String,
    val color: Int = 0xFFFFFFFF.toInt()
)

// ============================================
// MULTI-LAYER AUDIO TYPES
// ============================================

internal data class AudioLayer(
    val id: String,
    val name: String,
    val player: MediaPlayer,
    var volume: Float = 1f,
    var pan: Float = 0f,
    var isMuted: Boolean = false,
    var isSolo: Boolean = false,
    var isPrepared: Boolean = false,
    var fxChainEnabled: Boolean = true
)

data class AudioLayerState(
    val id: String,
    val name: String,
    val volume: Float = 1f,
    val pan: Float = 0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val isPlaying: Boolean = false,
    val isPrepared: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val fxChainEnabled: Boolean = true
)

// ============================================
// FX AUTOMATION TYPES
// ============================================

data class AutomationPoint(
    val timeMs: Long,
    val value: Float
)

data class AutomationCurve(
    val paramName: String,
    val points: List<AutomationPoint>,
    val interpolation: InterpolationType = InterpolationType.LINEAR
)

enum class InterpolationType {
    LINEAR,
    SMOOTH,
    STEP
}

// ============================================
// OUTPUT PROFILE
// ============================================

data class OutputProfile(
    val name: String,
    val outputType: OutputType,
    val eqEnabled: Boolean = true,
    val eqBandLevels: List<Int> = emptyList(),
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudnessGain: Int = 0,
    val reverbPreset: Int = 0,
    val compressorEnabled: Boolean = false,
    val limiterEnabled: Boolean = false
)

// ============================================
// AUDIO FX STATE
// ============================================

data class AudioFXState(
    val eqEnabled: Boolean = false,
    val eqPresetIndex: Int = 0,
    val eqBandLevels: List<Int> = emptyList(),
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 0,
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Int = 0,
    val loudnessEnhancerEnabled: Boolean = false,
    val loudnessGain: Int = 0,
    val reverbEnabled: Boolean = false,
    val reverbPreset: Int = 0,
    val compressorEnabled: Boolean = false,
    val compressorThreshold: Float = -20f,
    val compressorRatio: Float = 4f,
    val limiterEnabled: Boolean = false,
    val limiterThreshold: Float = -1f
)

// ============================================
// LOOP STATE
// ============================================

data class LoopState(
    val isEnabled: Boolean = false,
    val startMs: Long = 0,
    val endMs: Long = 0,
    val loopType: LoopType = LoopType.TIME,
    val beats: Int = 0,
    val bars: Int = 0
)

enum class LoopType {
    TIME,
    BEATS,
    BARS
}

