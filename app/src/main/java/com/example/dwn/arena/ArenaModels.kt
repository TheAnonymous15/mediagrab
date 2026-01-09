package com.example.dwn.arena

import java.util.UUID

/**
 * Artists Arena Data Models
 * Audio-first music discovery, promotion, and community engagement platform
 */

// ============================================
// ARTIST IDENTITY
// ============================================

enum class ArtistRole {
    INDEPENDENT_ARTIST,
    PRODUCER,
    DJ,
    PODCASTER,
    BAND,
    LABEL
}

enum class VerificationStatus {
    UNVERIFIED,
    PENDING,
    VERIFIED,
    OFFICIAL
}

data class ArtistProfile(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val displayName: String,
    val handle: String, // @handle
    val bio: String = "",
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val role: ArtistRole = ArtistRole.INDEPENDENT_ARTIST,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val genres: List<String> = emptyList(),
    val moods: List<String> = emptyList(),
    val influences: List<String> = emptyList(),
    val location: String? = null,
    val website: String? = null,
    val socialLinks: Map<String, String> = emptyMap(), // platform -> url
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val trackCount: Int = 0,
    val totalPlays: Long = 0,
    val joinedAt: Long = System.currentTimeMillis(),
    val isFollowing: Boolean = false // For UI state
)

// ============================================
// TRACK & AUDIO
// ============================================

enum class TrackStatus {
    DRAFT,
    PROCESSING,
    PRIVATE,
    PUBLIC,
    SCHEDULED,
    TAKEN_DOWN
}

enum class AudioQuality {
    STANDARD,      // 128 kbps
    HIGH,          // 256 kbps
    LOSSLESS,      // FLAC
    MASTER         // Original upload quality
}

enum class RemixPermission {
    NO_REMIX,
    PARTIAL_REMIX,  // Stems only
    FULL_REMIX,     // Full track
    CREATIVE_COMMONS
}

data class Track(
    val id: String = UUID.randomUUID().toString(),
    val artistId: String,
    val artistName: String,
    val artistHandle: String,
    val artistAvatarUrl: String? = null,
    val isVerified: Boolean = false,
    val title: String,
    val description: String = "",
    val audioUrl: String,
    val waveformData: List<Float> = emptyList(), // Normalized waveform points
    val coverArtUrl: String? = null,
    val duration: Long = 0, // milliseconds
    val genres: List<String> = emptyList(),
    val moods: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val bpm: Int? = null,
    val key: String? = null, // Musical key
    val status: TrackStatus = TrackStatus.DRAFT,
    val remixPermission: RemixPermission = RemixPermission.NO_REMIX,
    val isExplicit: Boolean = false,
    val releaseDate: Long = System.currentTimeMillis(),
    val scheduledReleaseDate: Long? = null,

    // Audio quality info
    val originalFormat: String = "MP3",
    val bitrate: Int = 320, // kbps
    val sampleRate: Int = 44100,
    val loudnessLUFS: Float = -14f,

    // Stats
    val playCount: Long = 0,
    val likeCount: Int = 0,
    val saveCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val remixCount: Int = 0,
    val radioSpins: Int = 0,
    val avgListenDuration: Float = 0f, // Percentage listened
    val replayRate: Float = 0f,

    // User state
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,

    // Remix info
    val isRemix: Boolean = false,
    val originalTrackId: String? = null,
    val originalTrackTitle: String? = null,
    val originalArtistName: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Album(
    val id: String = UUID.randomUUID().toString(),
    val artistId: String,
    val artistName: String,
    val title: String,
    val description: String = "",
    val coverArtUrl: String? = null,
    val trackIds: List<String> = emptyList(),
    val trackCount: Int = 0,
    val totalDuration: Long = 0,
    val genres: List<String> = emptyList(),
    val releaseDate: Long = System.currentTimeMillis(),
    val status: TrackStatus = TrackStatus.DRAFT,
    val playCount: Long = 0,
    val likeCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// ============================================
// INTERACTIONS
// ============================================

data class TrackComment(
    val id: String = UUID.randomUUID().toString(),
    val trackId: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val isArtist: Boolean = false,
    val content: String,
    val timestamp: Long? = null, // Timestamp in track (milliseconds)
    val likeCount: Int = 0,
    val replyCount: Int = 0,
    val isLiked: Boolean = false,
    val parentCommentId: String? = null, // For replies
    val createdAt: Long = System.currentTimeMillis()
)

data class VoiceReaction(
    val id: String = UUID.randomUUID().toString(),
    val trackId: String,
    val userId: String,
    val userName: String,
    val audioUrl: String,
    val duration: Long = 0, // Max 15 seconds
    val timestamp: Long? = null, // Timestamp in track
    val createdAt: Long = System.currentTimeMillis()
)

// ============================================
// REMIX & COLLABORATION
// ============================================

data class RemixLink(
    val id: String = UUID.randomUUID().toString(),
    val originalTrackId: String,
    val remixTrackId: String,
    val remixArtistId: String,
    val remixArtistName: String,
    val attribution: String = "",
    val approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class RemixChallenge(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val originalTrackId: String,
    val hostArtistId: String,
    val hostArtistName: String,
    val startDate: Long,
    val endDate: Long,
    val prizes: List<String> = emptyList(),
    val submissionCount: Int = 0,
    val isActive: Boolean = true
)

// ============================================
// ANALYTICS
// ============================================

data class TrackAnalytics(
    val trackId: String,
    val totalPlays: Long = 0,
    val uniqueListeners: Int = 0,
    val avgListenDuration: Float = 0f,
    val completionRate: Float = 0f,
    val replayRate: Float = 0f,
    val likeRate: Float = 0f,
    val saveRate: Float = 0f,
    val shareRate: Float = 0f,
    val radioSpins: Int = 0,
    val remixCount: Int = 0,
    val topRegions: List<String> = emptyList(),
    val dropOffPoints: List<Float> = emptyList(), // Percentage points where listeners drop
    val peakListeningHours: List<Int> = emptyList(),
    val sourceBreakdown: Map<String, Int> = emptyMap() // feed, search, radio, etc.
)

data class ArtistAnalytics(
    val artistId: String,
    val totalPlays: Long = 0,
    val uniqueListeners: Int = 0,
    val followerGrowth: Int = 0, // This period
    val topTracks: List<String> = emptyList(),
    val avgListenDuration: Float = 0f,
    val radioSpins: Int = 0,
    val remixesOfTracks: Int = 0,
    val periodStart: Long = 0,
    val periodEnd: Long = 0
)

// ============================================
// RADIO INTEGRATION
// ============================================

data class RadioSpin(
    val id: String = UUID.randomUUID().toString(),
    val trackId: String,
    val stationId: String,
    val stationName: String,
    val djId: String? = null,
    val djName: String? = null,
    val listenerCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

// ============================================
// FEED & DISCOVERY
// ============================================

enum class FeedType {
    FOR_YOU,
    FRESH_RELEASES,
    GENRE,
    MOOD,
    LOCAL,
    CURATED,
    FOLLOWING,
    TRENDING
}

data class FeedItem(
    val id: String = UUID.randomUUID().toString(),
    val type: FeedItemType,
    val track: Track? = null,
    val album: Album? = null,
    val artist: ArtistProfile? = null,
    val challenge: RemixChallenge? = null,
    val curatedTitle: String? = null,
    val curatedDescription: String? = null,
    val priority: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

enum class FeedItemType {
    TRACK,
    ALBUM,
    ARTIST_SPOTLIGHT,
    REMIX_CHALLENGE,
    CURATED_COLLECTION,
    RADIO_FEATURE
}

// ============================================
// UPLOAD & PROCESSING
// ============================================

enum class UploadStatus {
    PENDING,
    UPLOADING,
    PROCESSING,
    ANALYZING,
    READY,
    FAILED
}

data class UploadJob(
    val id: String = UUID.randomUUID().toString(),
    val artistId: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long = 0,
    val status: UploadStatus = UploadStatus.PENDING,
    val progress: Float = 0f,
    val errorMessage: String? = null,
    val analysisResult: AudioAnalysisResult? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AudioAnalysisResult(
    val duration: Long = 0,
    val bitrate: Int = 0,
    val sampleRate: Int = 0,
    val channels: Int = 2,
    val loudnessLUFS: Float = -14f,
    val peakLevel: Float = 0f,
    val hasClipping: Boolean = false,
    val detectedBPM: Int? = null,
    val detectedKey: String? = null,
    val waveformData: List<Float> = emptyList(),
    val spectralData: List<Float> = emptyList()
)

// ============================================
// NOTIFICATIONS
// ============================================

enum class ArenaNotificationType {
    NEW_FOLLOWER,
    TRACK_LIKED,
    TRACK_SAVED,
    NEW_COMMENT,
    COMMENT_REPLY,
    REMIX_CREATED,
    RADIO_SPIN,
    MILESTONE_REACHED,
    CHALLENGE_STARTED,
    ARTIST_RELEASE
}

data class ArenaNotification(
    val id: String = UUID.randomUUID().toString(),
    val type: ArenaNotificationType,
    val userId: String,
    val title: String,
    val message: String,
    val imageUrl: String? = null,
    val actionUrl: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// ============================================
// GENRES & MOODS (Predefined)
// ============================================

object ArenaGenres {
    val all = listOf(
        "Hip Hop", "R&B", "Pop", "Rock", "Electronic", "Dance",
        "House", "Techno", "Drum & Bass", "Dubstep", "Trap",
        "Jazz", "Soul", "Funk", "Reggae", "Afrobeats", "Amapiano",
        "Latin", "Classical", "Ambient", "Lo-Fi", "Indie",
        "Metal", "Punk", "Country", "Folk", "Blues", "Gospel",
        "K-Pop", "J-Pop", "Experimental", "World", "Soundtrack"
    )
}

object ArenaMoods {
    val all = listOf(
        "Energetic", "Chill", "Dark", "Uplifting", "Melancholic",
        "Romantic", "Aggressive", "Peaceful", "Nostalgic", "Epic",
        "Dreamy", "Groovy", "Intense", "Relaxing", "Motivating",
        "Sensual", "Ethereal", "Raw", "Smooth", "Hypnotic"
    )
}

// ============================================
// MONETIZATION (Optional per guide)
// ============================================

enum class MonetizationType {
    TIP,
    PAID_RELEASE,
    SUBSCRIPTION,
    LICENSING,
    MERCH_LINK
}

data class TipTransaction(
    val id: String = UUID.randomUUID().toString(),
    val fromUserId: String,
    val toArtistId: String,
    val trackId: String? = null,
    val amount: Double,
    val currency: String = "USD",
    val message: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class PaidRelease(
    val id: String = UUID.randomUUID().toString(),
    val trackId: String,
    val artistId: String,
    val price: Double,
    val currency: String = "USD",
    val isPurchased: Boolean = false,
    val previewDuration: Long = 30_000 // 30 second preview
)

data class ArtistSubscription(
    val id: String = UUID.randomUUID().toString(),
    val artistId: String,
    val subscriberId: String,
    val tier: SubscriptionTier = SubscriptionTier.BASIC,
    val monthlyPrice: Double,
    val startDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

enum class SubscriptionTier {
    BASIC,      // Early access
    SUPPORTER,  // Exclusive content + chat
    VIP         // All perks + direct access
}

// ============================================
// MODERATION & QUALITY CONTROL
// ============================================

enum class ModerationStatus {
    PENDING_REVIEW,
    APPROVED,
    FLAGGED,
    REMOVED,
    APPEALED
}

enum class ReportReason {
    COPYRIGHT_VIOLATION,
    INAPPROPRIATE_CONTENT,
    SPAM,
    HARASSMENT,
    MISLEADING_INFO,
    OTHER
}

data class ContentReport(
    val id: String = UUID.randomUUID().toString(),
    val reporterId: String,
    val contentType: ContentType,
    val contentId: String,
    val reason: ReportReason,
    val description: String = "",
    val status: ModerationStatus = ModerationStatus.PENDING_REVIEW,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null
)

enum class ContentType {
    TRACK,
    COMMENT,
    ARTIST_PROFILE,
    ALBUM
}

data class ModerationAction(
    val id: String = UUID.randomUUID().toString(),
    val moderatorId: String,
    val contentType: ContentType,
    val contentId: String,
    val action: ModeratorAction,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ModeratorAction {
    APPROVE,
    REMOVE,
    WARN,
    BAN_TEMPORARY,
    BAN_PERMANENT
}

// ============================================
// AUDIO PROCESSING & MASTERING
// ============================================

data class MasteringPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val targetLUFS: Float = -14f,
    val eqSettings: Map<String, Float> = emptyMap(),
    val compressionRatio: Float = 4f,
    val limiterCeiling: Float = -1f,
    val stereoWidth: Float = 1f
)

data class AudioProcessingJob(
    val id: String = UUID.randomUUID().toString(),
    val trackId: String,
    val type: ProcessingType,
    val status: ProcessingStatus = ProcessingStatus.QUEUED,
    val progress: Float = 0f,
    val inputUrl: String,
    val outputUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

enum class ProcessingType {
    LOUDNESS_NORMALIZATION,
    MASTERING_ASSIST,
    WAVEFORM_GENERATION,
    SPECTRAL_ANALYSIS,
    BPM_DETECTION,
    KEY_DETECTION,
    STEM_SEPARATION
}

enum class ProcessingStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED
}

// ============================================
// CURATED CONTENT & PLAYLISTS
// ============================================

data class CuratedPlaylist(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val coverArtUrl: String? = null,
    val curatorId: String,
    val curatorName: String,
    val isOfficial: Boolean = false, // Platform curated vs user curated
    val trackIds: List<String> = emptyList(),
    val followerCount: Int = 0,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class LocalScene(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val city: String,
    val country: String,
    val coverImageUrl: String? = null,
    val description: String = "",
    val artistCount: Int = 0,
    val trackCount: Int = 0,
    val featuredArtistIds: List<String> = emptyList(),
    val topGenres: List<String> = emptyList()
)

// ============================================
// SECURITY & ACCESS CONTROL
// ============================================

data class StreamingToken(
    val token: String,
    val trackId: String,
    val userId: String,
    val quality: AudioQuality,
    val expiresAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)

data class RightsDeclaration(
    val id: String = UUID.randomUUID().toString(),
    val trackId: String,
    val artistId: String,
    val ownsAllRights: Boolean = true,
    val hasDistributionRights: Boolean = true,
    val allowsRemix: Boolean = true,
    val allowsRadioPlay: Boolean = true,
    val allowsSampling: Boolean = false,
    val licensingType: LicensingType = LicensingType.STANDARD,
    val thirdPartyCredits: List<String> = emptyList(),
    val agreedToTerms: Boolean = true,
    val declaredAt: Long = System.currentTimeMillis()
)

enum class LicensingType {
    STANDARD,           // Platform default
    CREATIVE_COMMONS,   // CC license
    EXCLUSIVE,          // Exclusive to platform
    CUSTOM              // Custom licensing terms
}

// ============================================
// LIVE EVENTS & LISTENING PARTIES
// ============================================

data class LiveListeningEvent(
    val id: String = UUID.randomUUID().toString(),
    val hostArtistId: String,
    val hostArtistName: String,
    val title: String,
    val description: String = "",
    val trackIds: List<String> = emptyList(),
    val scheduledStart: Long,
    val actualStart: Long? = null,
    val endTime: Long? = null,
    val isLive: Boolean = false,
    val listenerCount: Int = 0,
    val maxListeners: Int = 0,
    val chatEnabled: Boolean = true,
    val coverImageUrl: String? = null
)

data class LiveEventMessage(
    val id: String = UUID.randomUUID().toString(),
    val eventId: String,
    val userId: String,
    val userName: String,
    val isHost: Boolean = false,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
