package com.example.dwn.data

import androidx.room.*
import java.util.UUID

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String = "",
    val fileName: String = "",
    val mediaType: String, // "MP3" or "MP4"
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Float = 0f,
    val filePath: String = "",
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val thumbnailUrl: String = "",
    val playCount: Int = 0, // Track number of times played
    val lastPlayedAt: Long? = null // Track when last played
)

class Converters {
    @TypeConverter
    fun fromStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
}

