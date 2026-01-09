package com.example.dwn.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status IN ('PENDING', 'DOWNLOADING', 'PAUSED') ORDER BY createdAt DESC")
    fun getActiveDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadItem?

    @Query("SELECT * FROM downloads WHERE url = :url LIMIT 1")
    suspend fun getDownloadByUrl(url: String): DownloadItem?

    @Query("SELECT * FROM downloads WHERE url = :url AND mediaType = :mediaType LIMIT 1")
    suspend fun getDownloadByUrlAndType(url: String, mediaType: String): DownloadItem?

    @Query("SELECT * FROM downloads WHERE url = :url AND status = 'COMPLETED' LIMIT 1")
    suspend fun getCompletedDownloadByUrl(url: String): DownloadItem?

    @Query("SELECT * FROM downloads WHERE url = :url AND mediaType = :mediaType AND status = 'COMPLETED' LIMIT 1")
    suspend fun getCompletedDownloadByUrlAndType(url: String, mediaType: String): DownloadItem?

    @Query("SELECT * FROM downloads WHERE title LIKE '%' || :query || '%' OR fileName LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchDownloads(query: String): Flow<List<DownloadItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadItem)

    @Update
    suspend fun updateDownload(download: DownloadItem)

    @Delete
    suspend fun deleteDownload(download: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: String)

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: DownloadStatus)

    @Query("UPDATE downloads SET progress = :progress, downloadedBytes = :downloadedBytes WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, downloadedBytes: Long)

    @Query("UPDATE downloads SET status = 'COMPLETED', completedAt = :completedAt, filePath = :filePath, fileName = :fileName WHERE id = :id")
    suspend fun markCompleted(id: String, completedAt: Long, filePath: String, fileName: String)

    @Query("UPDATE downloads SET status = 'FAILED', errorMessage = :errorMessage WHERE id = :id")
    suspend fun markFailed(id: String, errorMessage: String)

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE url = :url AND status = 'COMPLETED')")
    suspend fun isAlreadyDownloaded(url: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE url = :url AND mediaType = :mediaType AND status = 'COMPLETED')")
    suspend fun isAlreadyDownloadedWithType(url: String, mediaType: String): Boolean

    @Query("SELECT * FROM downloads WHERE status IN ('PAUSED', 'FAILED') ORDER BY createdAt DESC")
    suspend fun getPausedDownloads(): List<DownloadItem>

    @Query("SELECT COUNT(*) FROM downloads WHERE status IN ('PAUSED', 'FAILED')")
    suspend fun getPausedDownloadsCount(): Int

    // Play count and favourites methods
    @Query("UPDATE downloads SET playCount = playCount + 1, lastPlayedAt = :playedAt WHERE id = :id")
    suspend fun incrementPlayCount(id: String, playedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' AND playCount > 0 ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayedDownloads(limit: Int = 50): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' AND mediaType = 'MP3' ORDER BY completedAt DESC")
    fun getCompletedAudioDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' AND mediaType = 'MP4' ORDER BY completedAt DESC")
    fun getCompletedVideoDownloads(): Flow<List<DownloadItem>>
}
