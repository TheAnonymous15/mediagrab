package com.example.dwn.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Entity to track play statistics for all media types (device media, downloaded files, etc.)
 * This allows unified tracking of play counts across all sources.
 */
@Entity(
    tableName = "media_play_stats",
    indices = [Index(value = ["mediaUri"], unique = true)]
)
data class MediaPlayStats(
    @PrimaryKey
    val id: String, // Unique identifier (e.g., "device_audio_123", "device_video_456", "download_xyz")
    val mediaUri: String, // URI or path of the media
    val mediaType: String, // "AUDIO" or "VIDEO"
    val mediaSource: String, // "DEVICE", "DOWNLOAD", "STREAM"
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val duration: Long = 0L,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val totalPlayDuration: Long = 0L, // Total time spent playing this media
    val completedPlays: Int = 0, // Number of times played to completion (>90%)
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface MediaPlayStatsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(stats: MediaPlayStats)

    @Update
    suspend fun update(stats: MediaPlayStats)

    @Query("SELECT * FROM media_play_stats WHERE id = :id")
    suspend fun getById(id: String): MediaPlayStats?

    @Query("SELECT * FROM media_play_stats WHERE mediaUri = :uri")
    suspend fun getByUri(uri: String): MediaPlayStats?

    /**
     * Increment play count when user taps to play.
     * This is called immediately when playback starts.
     */
    @Query("""
        UPDATE media_play_stats 
        SET playCount = playCount + 1, lastPlayedAt = :playedAt 
        WHERE id = :id
    """)
    suspend fun incrementPlayCount(id: String, playedAt: Long = System.currentTimeMillis())

    /**
     * Record a completed play (when media was played to >90% completion)
     */
    @Query("""
        UPDATE media_play_stats 
        SET completedPlays = completedPlays + 1 
        WHERE id = :id
    """)
    suspend fun incrementCompletedPlays(id: String)

    /**
     * Add to total play duration
     */
    @Query("""
        UPDATE media_play_stats 
        SET totalPlayDuration = totalPlayDuration + :duration 
        WHERE id = :id
    """)
    suspend fun addPlayDuration(id: String, duration: Long)

    /**
     * Get most played media (for favourites - all types)
     */
    @Query("""
        SELECT * FROM media_play_stats 
        WHERE playCount > 0 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getMostPlayed(limit: Int = 50): Flow<List<MediaPlayStats>>

    /**
     * Get most played audio files
     */
    @Query("""
        SELECT * FROM media_play_stats 
        WHERE playCount > 0 AND mediaType = 'AUDIO'
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getMostPlayedAudio(limit: Int = 50): Flow<List<MediaPlayStats>>

    /**
     * Get most played video files
     */
    @Query("""
        SELECT * FROM media_play_stats 
        WHERE playCount > 0 AND mediaType = 'VIDEO'
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getMostPlayedVideo(limit: Int = 50): Flow<List<MediaPlayStats>>

    /**
     * Get most played device media
     */
    @Query("""
        SELECT * FROM media_play_stats 
        WHERE playCount > 0 AND mediaSource = 'DEVICE'
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getMostPlayedDeviceMedia(limit: Int = 50): Flow<List<MediaPlayStats>>

    /**
     * Get most played downloaded media
     */
    @Query("""
        SELECT * FROM media_play_stats 
        WHERE playCount > 0 AND mediaSource = 'DOWNLOAD'
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getMostPlayedDownloads(limit: Int = 50): Flow<List<MediaPlayStats>>

    /**
     * Get play stats for a specific media source
     */
    @Query("SELECT * FROM media_play_stats WHERE mediaSource = :source ORDER BY playCount DESC")
    fun getBySource(source: String): Flow<List<MediaPlayStats>>

    /**
     * Get total play count across all media
     */
    @Query("SELECT SUM(playCount) FROM media_play_stats")
    suspend fun getTotalPlayCount(): Int?

    /**
     * Delete stats for a specific media
     */
    @Query("DELETE FROM media_play_stats WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * Clear all stats
     */
    @Query("DELETE FROM media_play_stats")
    suspend fun clearAll()

    /**
     * Insert or update play stats - ensures the record exists before incrementing
     */
    @Transaction
    suspend fun recordPlay(
        id: String,
        mediaUri: String,
        mediaType: String,
        mediaSource: String,
        title: String,
        artist: String? = null,
        album: String? = null,
        duration: Long = 0L
    ) {
        val existing = getById(id)
        if (existing == null) {
            // Create new record with playCount = 1
            insert(MediaPlayStats(
                id = id,
                mediaUri = mediaUri,
                mediaType = mediaType,
                mediaSource = mediaSource,
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                playCount = 1,
                lastPlayedAt = System.currentTimeMillis()
            ))
        } else {
            // Increment existing
            incrementPlayCount(id)
        }
    }
}

