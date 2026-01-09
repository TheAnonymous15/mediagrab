package com.example.dwn.radio

import java.util.UUID

// ============================================
// 📻 MY RADIO STATION - BROADCAST SYSTEM
// ============================================

/**
 * User's own radio station configuration
 */
data class MyRadioStation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tagline: String = "",
    val description: String = "",

    // Branding
    val logoUri: String? = null,
    val coverUri: String? = null,
    val accentColor: Long = 0xFFE91E63,

    // Classification
    val genres: List<RadioGenre> = emptyList(),
    val tags: List<String> = emptyList(),
    val language: String = "en",

    // Status
    val isLive: Boolean = false,
    val isPublic: Boolean = true,
    val listenerCount: Int = 0,
    val peakListeners: Int = 0,

    // Schedule
    val schedule: BroadcastSchedule? = null,

    // Stats
    val totalBroadcastTime: Long = 0L,
    val totalShows: Int = 0,
    val followers: Int = 0,

    // Monetization
    val tipsEnabled: Boolean = false,
    val subscriptionEnabled: Boolean = false,
    val ticketedShowsEnabled: Boolean = false,

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val lastLiveAt: Long? = null
)

/**
 * Broadcast schedule for regular shows
 */
data class BroadcastSchedule(
    val timezone: String = "UTC",
    val shows: List<ScheduledShow> = emptyList()
)

data class ScheduledShow(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val dayOfWeek: Int,  // 1-7 (Monday-Sunday)
    val startTime: String,  // "HH:mm"
    val durationMinutes: Int = 60,
    val isRecurring: Boolean = true,
    val notifyFollowers: Boolean = true
)

// ============================================
// 🎙️ LIVE BROADCAST SESSION
// ============================================

/**
 * Active broadcast session
 */
data class BroadcastSession(
    val id: String = UUID.randomUUID().toString(),
    val stationId: String,
    val title: String,
    val description: String = "",

    // Status
    val status: BroadcastStatus = BroadcastStatus.PREPARING,
    val startedAt: Long? = null,
    val endedAt: Long? = null,

    // Audio
    val audioSource: AudioSource = AudioSource.MICROPHONE,
    val isRecording: Boolean = true,
    val recordingUri: String? = null,

    // Engagement
    val currentListeners: Int = 0,
    val peakListeners: Int = 0,
    val totalLikes: Int = 0,
    val chatEnabled: Boolean = true,
    val chatMessages: List<ChatMessage> = emptyList(),

    // Interactive
    val activePoll: BroadcastPoll? = null,
    val qnaEnabled: Boolean = false,
    val questions: List<ListenerQuestion> = emptyList(),

    // Segments
    val segments: List<BroadcastSegment> = emptyList(),
    val currentSegmentIndex: Int = 0
)

enum class BroadcastStatus {
    PREPARING,
    LIVE,
    PAUSED,
    ENDED
}

enum class AudioSource {
    MICROPHONE,
    SYSTEM_AUDIO,
    MIXED,
    MEDIA_FILE,
    PLAYLIST
}

/**
 * Broadcast segment (part of a show)
 */
data class BroadcastSegment(
    val id: String = UUID.randomUUID().toString(),
    val type: SegmentType,
    val title: String,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val duration: Long = 0L,

    // Content
    val mediaUri: String? = null,
    val playlistId: String? = null,

    // Permissions
    val allowClipping: Boolean = true,
    val allowSaving: Boolean = true
)

enum class SegmentType {
    VOICE,           // Host talking
    MUSIC,           // Playing music
    PODCAST_CLIP,    // Playing podcast clip
    VOICE_DROP,      // Pre-recorded voice drop
    SOUND_EFFECT,    // Sound effect/jingle
    INTERVIEW,       // Guest interview
    CALLER,          // Live caller
    AD_BREAK,        // Advertisement
    MIXED            // Voice over music
}

// ============================================
// 👥 LISTENER INTERACTION
// ============================================

/**
 * Chat message during broadcast
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val senderAvatar: String? = null,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isHost: Boolean = false,
    val isModerator: Boolean = false,
    val isPinned: Boolean = false,
    val isTip: Boolean = false,
    val tipAmount: Double = 0.0
)

/**
 * Live poll during broadcast
 */
data class BroadcastPoll(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val options: List<PollOption>,
    val startedAt: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 60,
    val isActive: Boolean = true,
    val totalVotes: Int = 0
)

data class PollOption(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val votes: Int = 0,
    val percentage: Float = 0f
)

/**
 * Listener question for Q&A
 */
data class ListenerQuestion(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val question: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAnswered: Boolean = false,
    val upvotes: Int = 0
)

// ============================================
// 🎛️ BROADCAST CONTROLS
// ============================================

/**
 * Audio mixer settings for broadcast
 */
data class BroadcastMixer(
    val micVolume: Float = 1f,
    val micMuted: Boolean = false,
    val mediaVolume: Float = 0.7f,
    val mediaMuted: Boolean = false,
    val masterVolume: Float = 1f,

    // Effects
    val noiseGateEnabled: Boolean = true,
    val compressorEnabled: Boolean = true,
    val eqEnabled: Boolean = false,
    val reverbEnabled: Boolean = false,
    val reverbAmount: Float = 0.1f,

    // Voice
    val voiceEnhancement: Boolean = true,
    val autoDucking: Boolean = true,  // Lower music when speaking
    val duckingAmount: Float = 0.3f
)

/**
 * Sound board for quick sounds
 */
data class SoundBoard(
    val sounds: List<SoundBoardItem> = defaultSoundBoardItems
)

data class SoundBoardItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String,
    val color: Long = 0xFFE91E63,
    val soundUri: String? = null,
    val isBuiltIn: Boolean = true
)

val defaultSoundBoardItems = listOf(
    SoundBoardItem(name = "Airhorn", icon = "📯", color = 0xFFFF5722),
    SoundBoardItem(name = "Applause", icon = "👏", color = 0xFF4CAF50),
    SoundBoardItem(name = "Laugh", icon = "😂", color = 0xFFFFC107),
    SoundBoardItem(name = "Drum Roll", icon = "🥁", color = 0xFF9C27B0),
    SoundBoardItem(name = "Sad Trombone", icon = "🎺", color = 0xFF2196F3),
    SoundBoardItem(name = "Ding", icon = "🔔", color = 0xFFFF9800),
    SoundBoardItem(name = "Record Scratch", icon = "💿", color = 0xFFE91E63),
    SoundBoardItem(name = "Crickets", icon = "🦗", color = 0xFF8BC34A),
    SoundBoardItem(name = "Wow", icon = "😮", color = 0xFF00BCD4),
    SoundBoardItem(name = "Intro", icon = "🎬", color = 0xFF673AB7),
    SoundBoardItem(name = "Outro", icon = "👋", color = 0xFFFF5722),
    SoundBoardItem(name = "Custom 1", icon = "🎵", color = 0xFF607D8B, isBuiltIn = false)
)

// ============================================
// 📊 BROADCAST ANALYTICS
// ============================================

data class BroadcastAnalytics(
    val sessionId: String,
    val totalListeners: Int = 0,
    val peakListeners: Int = 0,
    val averageListeners: Float = 0f,
    val totalDuration: Long = 0L,
    val totalChatMessages: Int = 0,
    val totalLikes: Int = 0,
    val totalTips: Double = 0.0,
    val newFollowers: Int = 0,
    val clipsSaved: Int = 0,
    val listenerRetention: Float = 0f,
    val listenerLocations: Map<String, Int> = emptyMap(),
    val peakMoments: List<PeakMoment> = emptyList()
)

data class PeakMoment(
    val timestamp: Long,
    val listenerCount: Int,
    val segmentTitle: String? = null
)

// ============================================
// 📁 ARCHIVED SHOWS
// ============================================

data class ArchivedShow(
    val id: String = UUID.randomUUID().toString(),
    val stationId: String,
    val sessionId: String,
    val title: String,
    val description: String = "",
    val recordingUri: String,
    val thumbnailUri: String? = null,
    val duration: Long,
    val broadcastedAt: Long,
    val listenerCount: Int = 0,
    val isPublic: Boolean = true,
    val chapters: List<ShowChapter> = emptyList(),
    val highlights: List<ShowHighlight> = emptyList(),
    val playCount: Int = 0
)

data class ShowChapter(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startTime: Long,
    val endTime: Long
)

data class ShowHighlight(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startTime: Long,
    val duration: Long,
    val clipUri: String? = null
)

// ============================================
// 🎯 RADIO STUDIO STATE
// ============================================

data class RadioStudioState(
    // My station
    val myStation: MyRadioStation? = null,
    val hasStation: Boolean = false,

    // Current broadcast
    val currentSession: BroadcastSession? = null,
    val isLive: Boolean = false,
    val broadcastDuration: Long = 0L,

    // Controls
    val mixer: BroadcastMixer = BroadcastMixer(),
    val soundBoard: SoundBoard = SoundBoard(),

    // Content
    val playlist: List<PlaylistItem> = emptyList(),
    val currentTrackIndex: Int = -1,

    // Archive
    val archivedShows: List<ArchivedShow> = emptyList(),

    // Analytics
    val liveAnalytics: BroadcastAnalytics? = null,

    // UI
    val isLoading: Boolean = false,
    val error: String? = null
)

data class PlaylistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val artist: String = "",
    val duration: Long,
    val uri: String,
    val thumbnailUri: String? = null,
    val isPlaying: Boolean = false,
    val isQueued: Boolean = false
)

