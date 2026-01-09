package com.example.dwn.remix

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// ============================================
// 🎚️ REMIX STUDIO DATA MODELS
// ============================================

enum class MediaSourceType {
    AUDIO_FILE,
    VIDEO_FILE,
    PODCAST,
    DOWNLOADED_MEDIA,
    AUDIO_ROOM_RECORDING
}

enum class OutputFormat {
    AUDIO_CLIP,          // MP3/AAC audio
    VIDEO_CLIP,          // MP4 video
    SHORT_VERTICAL,      // 9:16 for TikTok/Reels/Shorts
    SHORT_SQUARE,        // 1:1 for Instagram
    AUDIO_ONLY,          // Audio extracted from video
    HIGHLIGHT_REEL       // Multiple clips compiled
}

enum class AspectRatio(val width: Int, val height: Int, val label: String) {
    LANDSCAPE(16, 9, "16:9 Landscape"),
    PORTRAIT(9, 16, "9:16 Portrait"),
    SQUARE(1, 1, "1:1 Square"),
    CINEMATIC(21, 9, "21:9 Cinematic")
}

data class RemixProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Remix",
    val sourceUri: String,
    val sourceType: MediaSourceType,
    val sourceDuration: Long = 0L,  // milliseconds
    val hasVideo: Boolean = false,
    val clips: List<RemixClip> = emptyList(),
    val outputFormat: OutputFormat = OutputFormat.AUDIO_CLIP,
    val aspectRatio: AspectRatio = AspectRatio.LANDSCAPE,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

data class RemixClip(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long,  // milliseconds
    val endTime: Long,
    val label: String = "",
    val isHighlight: Boolean = false,
    val highlightScore: Float = 0f,  // AI confidence
    val processing: ClipProcessing = ClipProcessing(),
    val captions: List<CaptionEntry> = emptyList(),
    val order: Int = 0
)

data class ClipProcessing(
    // Trimming
    val autoTrimSilence: Boolean = true,
    val fadeInMs: Int = 100,
    val fadeOutMs: Int = 200,

    // Audio
    val eqEnabled: Boolean = false,
    val eqPreset: EQPreset = EQPreset.FLAT,
    val compressorEnabled: Boolean = false,
    val compressorPreset: CompressorPreset = CompressorPreset.GENTLE,
    val noiseReductionEnabled: Boolean = false,
    val noiseReductionStrength: Float = 0.5f,
    val voiceEnhancement: VoiceEnhancementPreset = VoiceEnhancementPreset.NONE,
    val loudnessTarget: Float = -14f,  // LUFS

    // Video
    val waveformOverlay: Boolean = false,
    val waveformColor: Long = 0xFFE91E63,
    val waveformPosition: WaveformPosition = WaveformPosition.BOTTOM,
    val autoCaption: Boolean = false,
    val captionStyle: CaptionStyle = CaptionStyle.MODERN
)

enum class EQPreset(val label: String) {
    FLAT("Flat"),
    VOICE_CLARITY("Voice Clarity"),
    BASS_BOOST("Bass Boost"),
    TREBLE_BOOST("Treble Boost"),
    WARM("Warm"),
    BRIGHT("Bright"),
    PODCAST("Podcast"),
    MUSIC("Music")
}

enum class CompressorPreset(val label: String) {
    OFF("Off"),
    GENTLE("Gentle"),
    MODERATE("Moderate"),
    AGGRESSIVE("Aggressive"),
    BROADCAST("Broadcast"),
    PODCAST("Podcast")
}

enum class VoiceEnhancementPreset(val label: String) {
    NONE("None"),
    CLARITY("Clarity"),
    WARMTH("Warmth"),
    PRESENCE("Presence"),
    DE_ESSER("De-esser"),
    FULL_ENHANCE("Full Enhancement")
}

enum class WaveformPosition {
    TOP, CENTER, BOTTOM
}

enum class CaptionStyle(val label: String) {
    SIMPLE("Simple"),
    MODERN("Modern"),
    BOLD("Bold"),
    MINIMAL("Minimal"),
    KARAOKE("Karaoke"),
    ANIMATED("Animated")
}

data class CaptionEntry(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long,
    val text: String,
    val speaker: String? = null,
    val confidence: Float = 1f
)

// ============================================
// 🤖 AI HIGHLIGHT DETECTION
// ============================================

data class DetectedHighlight(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long,
    val endTime: Long,
    val type: HighlightType,
    val confidence: Float,
    val description: String = "",
    val speaker: String? = null
)

enum class HighlightType {
    SPEECH_EMPHASIS,      // Louder, more emphatic speech
    MUSIC_DROP,           // Beat drop or climax
    LAUGHTER,             // Detected laughter
    APPLAUSE,             // Applause or cheering
    SILENCE_BREAK,        // Significant pause then speech
    KEY_PHRASE,           // Important phrase detected
    EMOTION_PEAK,         // High emotional content
    SPEAKER_CHANGE        // Notable speaker transition
}

// ============================================
// 📤 EXPORT CONFIGURATION
// ============================================

data class ExportConfig(
    val format: OutputFormat,
    val aspectRatio: AspectRatio = AspectRatio.LANDSCAPE,
    val quality: ExportQuality = ExportQuality.HIGH,
    val audioCodec: AudioCodec = AudioCodec.AAC,
    val videoCodec: VideoCodec = VideoCodec.H264,
    val includeSubtitles: Boolean = false,
    val subtitleFormat: SubtitleFormat = SubtitleFormat.SRT,
    val embedSubtitles: Boolean = false,
    val metadata: ExportMetadata = ExportMetadata()
)

enum class ExportQuality(val label: String, val audioBitrate: Int, val videoBitrate: Int) {
    LOW("Low", 128, 1000),
    MEDIUM("Medium", 192, 2500),
    HIGH("High", 256, 5000),
    ULTRA("Ultra", 320, 8000)
}

enum class AudioCodec { AAC, MP3, OPUS, FLAC }
enum class VideoCodec { H264, H265, VP9 }
enum class SubtitleFormat { SRT, VTT, ASS }

data class ExportMetadata(
    val title: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val author: String = ""
)

// ============================================
// 📊 EXPORT PRESETS
// ============================================

data class ExportPreset(
    val id: String,
    val name: String,
    val icon: String,
    val config: ExportConfig
)

val defaultExportPresets = listOf(
    ExportPreset(
        id = "tiktok",
        name = "TikTok/Reels",
        icon = "📱",
        config = ExportConfig(
            format = OutputFormat.SHORT_VERTICAL,
            aspectRatio = AspectRatio.PORTRAIT,
            quality = ExportQuality.HIGH
        )
    ),
    ExportPreset(
        id = "instagram",
        name = "Instagram Square",
        icon = "📷",
        config = ExportConfig(
            format = OutputFormat.SHORT_SQUARE,
            aspectRatio = AspectRatio.SQUARE,
            quality = ExportQuality.HIGH
        )
    ),
    ExportPreset(
        id = "youtube",
        name = "YouTube",
        icon = "▶️",
        config = ExportConfig(
            format = OutputFormat.VIDEO_CLIP,
            aspectRatio = AspectRatio.LANDSCAPE,
            quality = ExportQuality.HIGH
        )
    ),
    ExportPreset(
        id = "podcast_clip",
        name = "Podcast Clip",
        icon = "🎙️",
        config = ExportConfig(
            format = OutputFormat.AUDIO_CLIP,
            quality = ExportQuality.HIGH,
            audioCodec = AudioCodec.MP3
        )
    ),
    ExportPreset(
        id = "audiogram",
        name = "Audiogram",
        icon = "📊",
        config = ExportConfig(
            format = OutputFormat.SHORT_SQUARE,
            aspectRatio = AspectRatio.SQUARE,
            quality = ExportQuality.MEDIUM
        )
    )
)

// ============================================
// 🎵 WAVEFORM DATA
// ============================================

data class WaveformData(
    val samples: List<Float>,  // Normalized 0-1 amplitude values
    val duration: Long,
    val sampleRate: Int = 100  // Samples per second
)

// ============================================
// 📋 PROJECT STATE
// ============================================

enum class ProjectState {
    IDLE,
    LOADING,
    ANALYZING,
    PROCESSING,
    EXPORTING,
    ERROR
}

data class RemixStudioState(
    val project: RemixProject? = null,
    val state: ProjectState = ProjectState.IDLE,
    val waveform: WaveformData? = null,
    val detectedHighlights: List<DetectedHighlight> = emptyList(),
    val selectedClipId: String? = null,
    val playbackPosition: Long = 0L,
    val isPlaying: Boolean = false,
    val exportProgress: Float = 0f,
    val errorMessage: String? = null
)

