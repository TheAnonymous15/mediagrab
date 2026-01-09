package com.example.dwn.radio

import java.util.UUID

// ============================================
// 📻 SUPER ONLINE RADIO DATA MODELS
// ============================================

/**
 * Types of radio stations
 */
enum class StationType(val displayName: String, val icon: String) {
    TRADITIONAL("Online Radio", "📻"),
    SMART_AUTO("Smart Radio", "🤖"),
    PODCAST("Podcast Radio", "🎙️"),
    CREATOR("Creator Station", "🎨"),
    LIVE_EVENT("Live Event", "🎪")
}

/**
 * Station genres
 */
enum class RadioGenre(val displayName: String, val icon: String, val color: Long) {
    POP("Pop", "🎵", 0xFFE91E63),
    ROCK("Rock", "🎸", 0xFFFF5722),
    ELECTRONIC("Electronic", "🎹", 0xFF9C27B0),
    HIP_HOP("Hip Hop", "🎤", 0xFFFFC107),
    JAZZ("Jazz", "🎷", 0xFF795548),
    CLASSICAL("Classical", "🎻", 0xFF607D8B),
    RNB("R&B", "💜", 0xFF673AB7),
    COUNTRY("Country", "🤠", 0xFF8BC34A),
    LATIN("Latin", "💃", 0xFFFF9800),
    WORLD("World", "🌍", 0xFF00BCD4),
    NEWS("News", "📰", 0xFF2196F3),
    TALK("Talk", "💬", 0xFF4CAF50),
    SPORTS("Sports", "⚽", 0xFFF44336),
    COMEDY("Comedy", "😂", 0xFFFFEB3B),
    AMBIENT("Ambient", "🌙", 0xFF3F51B5),
    CHILL("Chill", "😌", 0xFF00BCD4),
    WORKOUT("Workout", "💪", 0xFFE91E63),
    FOCUS("Focus", "🎯", 0xFF9C27B0),
    SLEEP("Sleep", "😴", 0xFF3F51B5),
    KIDS("Kids", "👶", 0xFFFF9800)
}

/**
 * Station mood tags for smart radio
 */
enum class RadioMood(val displayName: String, val icon: String) {
    ENERGETIC("Energetic", "⚡"),
    RELAXED("Relaxed", "😌"),
    FOCUSED("Focused", "🎯"),
    HAPPY("Happy", "😊"),
    MELANCHOLIC("Melancholic", "😢"),
    ROMANTIC("Romantic", "💕"),
    MOTIVATIONAL("Motivational", "💪"),
    PEACEFUL("Peaceful", "☮️"),
    PARTY("Party", "🎉"),
    NOSTALGIC("Nostalgic", "🕰️")
}

/**
 * Stream quality/codec
 */
enum class StreamQuality(val displayName: String, val bitrate: Int) {
    LOW("Low (64 kbps)", 64),
    MEDIUM("Medium (128 kbps)", 128),
    HIGH("High (192 kbps)", 192),
    ULTRA("Ultra (320 kbps)", 320),
    LOSSLESS("Lossless", 1411)
}

enum class StreamCodec {
    MP3, AAC, OGG, OPUS, FLAC
}

// ============================================
// 📻 STATION MODEL
// ============================================

/**
 * Radio Station - Core model
 */
data class RadioStation(
    val id: String = UUID.randomUUID().toString(),
    val type: StationType,
    val name: String,
    val description: String = "",
    val tagline: String = "",

    // Branding
    val logoUrl: String? = null,
    val coverUrl: String? = null,
    val accentColor: Long = 0xFFE91E63,

    // Classification
    val genres: List<RadioGenre> = emptyList(),
    val moods: List<RadioMood> = emptyList(),
    val tags: List<String> = emptyList(),

    // Location
    val country: String? = null,
    val countryCode: String? = null,
    val language: String = "en",

    // Stream info
    val streamUrl: String? = null,
    val streamQuality: StreamQuality = StreamQuality.HIGH,
    val streamCodec: StreamCodec = StreamCodec.AAC,

    // Status
    val isLive: Boolean = true,
    val isOnline: Boolean = true,
    val listenerCount: Int = 0,

    // Creator info (for creator stations)
    val ownerId: String? = null,
    val ownerName: String? = null,
    val ownerAvatar: String? = null,

    // Schedule
    val schedule: StationSchedule? = null,

    // Smart radio config
    val smartConfig: SmartRadioConfig? = null,

    // Permissions
    val allowClipping: Boolean = true,
    val allowSaving: Boolean = true,

    // Engagement
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val rating: Float = 0f,

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null
)

/**
 * Station schedule for programmed content
 */
data class StationSchedule(
    val timezone: String = "UTC",
    val programs: List<ScheduledProgram> = emptyList(),
    val isRecurring: Boolean = true
)

data class ScheduledProgram(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val hostName: String? = null,
    val startTime: String,  // "HH:mm"
    val endTime: String,
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),  // 1 = Monday
    val genre: RadioGenre? = null
)

/**
 * Smart Radio configuration
 */
data class SmartRadioConfig(
    val sourceType: SmartSourceType = SmartSourceType.MIXED,
    val genres: List<RadioGenre> = emptyList(),
    val moods: List<RadioMood> = emptyList(),
    val useVaultMedia: Boolean = true,
    val useListeningHistory: Boolean = true,
    val useContextMode: Boolean = true,
    val seedTracks: List<String> = emptyList(),
    val excludeArtists: List<String> = emptyList(),
    val energyRange: ClosedFloatingPointRange<Float> = 0.3f..0.7f,
    val tempoRange: IntRange = 80..140,
    val transitionStyle: TransitionStyle = TransitionStyle.CROSSFADE
)

enum class SmartSourceType {
    VAULT_ONLY,
    STREAMING_ONLY,
    MIXED
}

enum class TransitionStyle {
    CROSSFADE,
    BEAT_MATCH,
    DJ_VOICE,
    SILENCE,
    SMOOTH
}

// ============================================
// 🎵 NOW PLAYING & SEGMENTS
// ============================================

/**
 * Currently playing content
 */
data class NowPlaying(
    val stationId: String,
    val segmentId: String? = null,

    // Track info (for music)
    val trackTitle: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val albumArt: String? = null,

    // Show info (for talk/podcast)
    val showName: String? = null,
    val hostName: String? = null,
    val episodeTitle: String? = null,

    // Timing
    val startedAt: Long = System.currentTimeMillis(),
    val duration: Long? = null,  // null for live streams
    val currentPosition: Long = 0L,

    // Metadata
    val isLive: Boolean = true,
    val allowClip: Boolean = true,
    val lyrics: String? = null
)

/**
 * Radio segment (for podcast/creator radio)
 */
data class RadioSegment(
    val id: String = UUID.randomUUID().toString(),
    val stationId: String,
    val title: String,
    val description: String = "",
    val hostName: String? = null,

    // Media
    val mediaUri: String,
    val duration: Long,
    val thumbnailUrl: String? = null,

    // Timing
    val scheduledStart: Long? = null,
    val actualStart: Long? = null,

    // Permissions
    val allowClip: Boolean = true,
    val allowSave: Boolean = true,

    // Engagement
    val likeCount: Int = 0,
    val clipCount: Int = 0,

    // Order
    val order: Int = 0
)

// ============================================
// 👤 LISTENER STATE
// ============================================

/**
 * User's listening state
 */
data class ListenerState(
    val userId: String,
    val currentStationId: String? = null,
    val lastPosition: Long = 0L,
    val contextMode: String? = null,  // Context Media Mode
    val volume: Float = 1f,
    val isMuted: Boolean = false,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1f,
    val sleepTimerMinutes: Int? = null,
    val sleepTimerEndTime: Long? = null
)

/**
 * Listening history entry
 */
data class ListeningHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val stationId: String,
    val stationName: String,
    val stationType: StationType,
    val listenedAt: Long = System.currentTimeMillis(),
    val duration: Long = 0L,
    val segmentId: String? = null,
    val trackTitle: String? = null
)

// ============================================
// 💾 SAVED CONTENT
// ============================================

/**
 * Saved radio clip
 */
data class RadioClip(
    val id: String = UUID.randomUUID().toString(),
    val stationId: String,
    val stationName: String,
    val segmentId: String? = null,

    // Content
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val mediaUri: String? = null,

    // Source info
    val trackTitle: String? = null,
    val artistName: String? = null,
    val showName: String? = null,

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),

    // Status
    val isSavedToVault: Boolean = false,
    val vaultItemId: String? = null
)

// ============================================
// 🔍 DISCOVERY
// ============================================

/**
 * Station category for discovery
 */
data class StationCategory(
    val id: String,
    val name: String,
    val icon: String,
    val description: String = "",
    val color: Long = 0xFFE91E63,
    val stationIds: List<String> = emptyList(),
    val isPersonalized: Boolean = false
)

/**
 * Featured content
 */
data class FeaturedRadio(
    val id: String = UUID.randomUUID().toString(),
    val type: FeaturedType,
    val title: String,
    val subtitle: String = "",
    val imageUrl: String? = null,
    val stationId: String? = null,
    val categoryId: String? = null,
    val color: Long = 0xFFE91E63,
    val validUntil: Long? = null
)

enum class FeaturedType {
    STATION,
    CATEGORY,
    LIVE_EVENT,
    MOOD_MIX,
    PERSONALIZED
}

// ============================================
// 🎛️ RADIO STATE
// ============================================

/**
 * Overall radio state
 */
data class RadioState(
    // Playback
    val isPlaying: Boolean = false,
    val currentStation: RadioStation? = null,
    val nowPlaying: NowPlaying? = null,
    val listenerState: ListenerState? = null,

    // Loading
    val isLoading: Boolean = false,
    val isBuffering: Boolean = false,
    val bufferProgress: Float = 0f,

    // Content
    val stations: List<RadioStation> = emptyList(),
    val categories: List<StationCategory> = emptyList(),
    val featured: List<FeaturedRadio> = emptyList(),
    val recentlyPlayed: List<ListeningHistoryEntry> = emptyList(),
    val favorites: List<RadioStation> = emptyList(),

    // Search
    val searchQuery: String = "",
    val searchResults: List<RadioStation> = emptyList(),

    // Smart Radio
    val smartStations: List<RadioStation> = emptyList(),

    // Error
    val error: String? = null
)

// ============================================
// 📊 DEFAULT DATA
// ============================================

val defaultGenreCategories = listOf(
    StationCategory("pop", "Pop Hits", "🎵", "Top pop music stations", 0xFFE91E63),
    StationCategory("rock", "Rock", "🎸", "Classic and modern rock", 0xFFFF5722),
    StationCategory("electronic", "Electronic", "🎹", "EDM, house, techno", 0xFF9C27B0),
    StationCategory("hiphop", "Hip Hop", "🎤", "Rap and hip hop", 0xFFFFC107),
    StationCategory("jazz", "Jazz", "🎷", "Smooth jazz and classics", 0xFF795548),
    StationCategory("classical", "Classical", "🎻", "Orchestra and piano", 0xFF607D8B),
    StationCategory("news", "News & Talk", "📰", "News, talk shows", 0xFF2196F3),
    StationCategory("chill", "Chill", "😌", "Lo-fi, ambient, relax", 0xFF00BCD4)
)

val defaultMoodCategories = listOf(
    StationCategory("focus", "Focus", "🎯", "Music for concentration", 0xFF9C27B0),
    StationCategory("workout", "Workout", "💪", "High energy beats", 0xFFE91E63),
    StationCategory("sleep", "Sleep", "😴", "Calm sounds for sleep", 0xFF3F51B5),
    StationCategory("party", "Party", "🎉", "Dance and party vibes", 0xFFFF5722),
    StationCategory("morning", "Morning", "☀️", "Start your day right", 0xFFFFC107),
    StationCategory("evening", "Evening", "🌙", "Wind down music", 0xFF673AB7)
)

val defaultSmartStations = listOf(
    RadioStation(
        id = "smart_daily_mix",
        type = StationType.SMART_AUTO,
        name = "Daily Mix",
        description = "Personalized based on your listening",
        tagline = "Your personal radio",
        accentColor = 0xFFE91E63,
        genres = emptyList(),
        moods = listOf(RadioMood.HAPPY),
        smartConfig = SmartRadioConfig(
            sourceType = SmartSourceType.MIXED,
            useVaultMedia = true,
            useListeningHistory = true
        )
    ),
    RadioStation(
        id = "smart_discover",
        type = StationType.SMART_AUTO,
        name = "Discover Weekly",
        description = "New music you might like",
        tagline = "Expand your horizons",
        accentColor = 0xFF9C27B0,
        smartConfig = SmartRadioConfig(
            sourceType = SmartSourceType.STREAMING_ONLY,
            useListeningHistory = true
        )
    ),
    RadioStation(
        id = "smart_focus",
        type = StationType.SMART_AUTO,
        name = "Focus Flow",
        description = "Concentration-boosting instrumentals",
        tagline = "Get in the zone",
        accentColor = 0xFF2196F3,
        moods = listOf(RadioMood.FOCUSED),
        smartConfig = SmartRadioConfig(
            moods = listOf(RadioMood.FOCUSED),
            energyRange = 0.3f..0.5f,
            tempoRange = 90..120
        )
    ),
    RadioStation(
        id = "smart_chill",
        type = StationType.SMART_AUTO,
        name = "Chill Vibes",
        description = "Relaxing music for any moment",
        tagline = "Just breathe",
        accentColor = 0xFF00BCD4,
        moods = listOf(RadioMood.RELAXED),
        smartConfig = SmartRadioConfig(
            moods = listOf(RadioMood.RELAXED, RadioMood.PEACEFUL),
            energyRange = 0.1f..0.4f
        )
    ),
    RadioStation(
        id = "smart_workout",
        type = StationType.SMART_AUTO,
        name = "Workout Boost",
        description = "High-energy tracks for exercise",
        tagline = "Push harder",
        accentColor = 0xFFFF5722,
        moods = listOf(RadioMood.ENERGETIC),
        smartConfig = SmartRadioConfig(
            moods = listOf(RadioMood.ENERGETIC, RadioMood.MOTIVATIONAL),
            energyRange = 0.7f..1f,
            tempoRange = 120..180
        )
    ),
    RadioStation(
        id = "smart_sleep",
        type = StationType.SMART_AUTO,
        name = "Sleep Sounds",
        description = "Gentle sounds for restful sleep",
        tagline = "Sweet dreams",
        accentColor = 0xFF3F51B5,
        moods = listOf(RadioMood.PEACEFUL),
        smartConfig = SmartRadioConfig(
            moods = listOf(RadioMood.PEACEFUL),
            energyRange = 0f..0.2f,
            tempoRange = 50..80
        )
    )
)

val sampleTraditionalStations = listOf(
    RadioStation(
        id = "bbc_radio1",
        type = StationType.TRADITIONAL,
        name = "BBC Radio 1",
        description = "The best new music and entertainment",
        tagline = "New music first",
        country = "United Kingdom",
        countryCode = "GB",
        genres = listOf(RadioGenre.POP, RadioGenre.ELECTRONIC),
        streamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_one",
        streamQuality = StreamQuality.HIGH,
        listenerCount = 45000,
        accentColor = 0xFFE91E63
    ),
    RadioStation(
        id = "kexp",
        type = StationType.TRADITIONAL,
        name = "KEXP 90.3",
        description = "Where the music matters",
        tagline = "Seattle's music discovery",
        country = "United States",
        countryCode = "US",
        genres = listOf(RadioGenre.ROCK, RadioGenre.WORLD),
        streamUrl = "https://kexp.streamguys1.com/kexp160.aac",
        streamQuality = StreamQuality.HIGH,
        listenerCount = 12000,
        accentColor = 0xFF4CAF50
    ),
    RadioStation(
        id = "fip",
        type = StationType.TRADITIONAL,
        name = "FIP",
        description = "Éclectique et sans pub",
        tagline = "French eclectic",
        country = "France",
        countryCode = "FR",
        genres = listOf(RadioGenre.JAZZ, RadioGenre.WORLD, RadioGenre.ELECTRONIC),
        streamUrl = "https://icecast.radiofrance.fr/fip-midfi.mp3",
        streamQuality = StreamQuality.MEDIUM,
        listenerCount = 28000,
        accentColor = 0xFFFF9800
    ),
    RadioStation(
        id = "nts1",
        type = StationType.TRADITIONAL,
        name = "NTS Radio 1",
        description = "Don't assume",
        tagline = "London underground",
        country = "United Kingdom",
        countryCode = "GB",
        genres = listOf(RadioGenre.ELECTRONIC, RadioGenre.HIP_HOP),
        streamUrl = "https://stream-relay-geo.ntslive.net/stream",
        streamQuality = StreamQuality.HIGH,
        listenerCount = 8500,
        accentColor = 0xFF000000
    ),
    RadioStation(
        id = "lofi_girl",
        type = StationType.TRADITIONAL,
        name = "Lofi Girl Radio",
        description = "Beats to relax/study to",
        tagline = "24/7 lofi hip hop",
        genres = listOf(RadioGenre.CHILL, RadioGenre.HIP_HOP),
        streamQuality = StreamQuality.HIGH,
        listenerCount = 65000,
        accentColor = 0xFF9C27B0
    ),
    RadioStation(
        id = "classic_fm",
        type = StationType.TRADITIONAL,
        name = "Classic FM",
        description = "The world's greatest music",
        tagline = "Relax with classics",
        country = "United Kingdom",
        countryCode = "GB",
        genres = listOf(RadioGenre.CLASSICAL),
        streamQuality = StreamQuality.HIGH,
        listenerCount = 32000,
        accentColor = 0xFF607D8B
    ),
    RadioStation(
        id = "soma_fm",
        type = StationType.TRADITIONAL,
        name = "SomaFM Groove Salad",
        description = "A nicely chilled plate of ambient beats",
        tagline = "Listener-supported",
        country = "United States",
        countryCode = "US",
        genres = listOf(RadioGenre.AMBIENT, RadioGenre.CHILL),
        streamQuality = StreamQuality.HIGH,
        listenerCount = 15000,
        accentColor = 0xFF8BC34A
    ),
    RadioStation(
        id = "radio_paradise",
        type = StationType.TRADITIONAL,
        name = "Radio Paradise",
        description = "Eclectic online rock radio",
        tagline = "DJ-mixed, ad-free",
        country = "United States",
        countryCode = "US",
        genres = listOf(RadioGenre.ROCK, RadioGenre.POP),
        streamQuality = StreamQuality.ULTRA,
        listenerCount = 22000,
        accentColor = 0xFF2196F3
    )
)

