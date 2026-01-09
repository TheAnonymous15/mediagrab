package com.example.dwn.download

import com.example.dwn.MediaType
import java.util.UUID

/**
 * Data models for the download system
 */

// Queue status enum
enum class QueueStatus {
    QUEUED,
    CHECKING,
    DOWNLOADING,
    PROCESSING,   // Post-download processing (merging, converting)
    SAVING,       // Saving to storage
    COMPLETED,
    FAILED,
    PAUSED,
    CANCELLED
}

// Queued download data class
data class QueuedDownload(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val mediaType: MediaType,
    val status: QueueStatus = QueueStatus.QUEUED,
    val progress: Float = 0f,
    val statusMessage: String = "",
    val title: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

// Download progress callback
typealias ProgressCallback = (Float, String) -> Unit

// Download complete callback
typealias CompleteCallback = (Boolean, String, com.example.dwn.data.DownloadItem?) -> Unit


