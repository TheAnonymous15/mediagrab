package com.example.dwn.download

import android.content.Context
import android.util.Log
import com.example.dwn.MediaType
import com.example.dwn.data.DownloadDao
import com.example.dwn.data.DownloadItem
import com.example.dwn.data.DownloadStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "DownloadManager"

/**
 * Main entry point for the download system.
 * Acts as a facade coordinating:
 * - DownloadExecutor: Handles actual download execution
 * - DownloadQueue: Manages queued downloads
 * - MediaSaver: Saves files to appropriate folders
 */
class DownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Component instances
    private val mediaSaver = MediaSaver.getInstance(context)
    private val executor = DownloadExecutor(context, downloadDao, mediaSaver)
    private val queue = DownloadQueue(context, downloadDao, executor)

    // Current download tracking
    private val _currentDownloadId = MutableStateFlow<String?>(null)
    val currentDownloadId: StateFlow<String?> = _currentDownloadId

    // Expose queue state
    val downloadQueue: StateFlow<List<QueuedDownload>> = queue.downloadQueue

    // ==================== Queue Operations ====================

    fun addToQueue(url: String, mediaType: MediaType): String {
        return queue.addToQueue(url, mediaType)
    }

    fun removeFromQueue(id: String) {
        queue.removeFromQueue(id)
    }

    fun clearCompletedFromQueue() {
        queue.clearCompletedFromQueue()
    }

    fun retryQueuedDownload(id: String) {
        queue.retryQueuedDownload(id)
    }

    // ==================== Direct Download Operations ====================

    /**
     * Start download immediately - downloads exactly what the user requests.
     * No playlist checking - uses --no-playlist flag to download single items.
     */
    suspend fun smartDownload(
        url: String,
        mediaType: MediaType,
        onProgress: ProgressCallback,
        onComplete: CompleteCallback
    ): String? {
        // Direct download - no playlist checking
        onProgress(0f, "🚀 Starting download...")
        return startDownload(url, mediaType, onProgress, onComplete)
    }

    /**
     * Start a single download - downloads exactly what the user requested
     */
    suspend fun startDownload(
        url: String,
        mediaType: MediaType,
        onProgress: ProgressCallback,
        onComplete: CompleteCallback
    ): String? {
        // Check if already downloaded
        val existingCompleted = downloadDao.getCompletedDownloadByUrlAndType(url, mediaType.name)
        if (existingCompleted != null) {
            onComplete(false, "⚠️ Already downloaded as ${mediaType.name}: ${existingCompleted.fileName}", existingCompleted)
            return null
        }

        // Check for paused/failed download
        val existingDownload = downloadDao.getDownloadByUrlAndType(url, mediaType.name)
        val downloadItem = if (existingDownload != null && existingDownload.status in listOf(
                DownloadStatus.PAUSED,
                DownloadStatus.FAILED,
                DownloadStatus.PENDING
            )) {
            existingDownload.copy(status = DownloadStatus.DOWNLOADING)
        } else {
            DownloadItem(
                url = url,
                mediaType = mediaType.name,
                status = DownloadStatus.DOWNLOADING
            )
        }

        downloadDao.insertDownload(downloadItem)
        _currentDownloadId.value = downloadItem.id

        val job = scope.launch {
            try {
                executor.executeDownload(downloadItem, mediaType, onProgress, onComplete)
            } finally {
                if (_currentDownloadId.value == downloadItem.id) {
                    _currentDownloadId.value = null
                }
            }
        }
        executor.activeDownloads[downloadItem.id] = job
        executor.downloadProgress[downloadItem.id] = MutableStateFlow(0f)

        return downloadItem.id
    }

    // ==================== Download Control ====================

    fun pauseDownload(downloadId: String) {
        executor.cancelDownload(downloadId)
        scope.launch {
            downloadDao.updateStatus(downloadId, DownloadStatus.PAUSED)
        }
    }

    fun cancelDownload(downloadId: String) {
        executor.cancelDownload(downloadId)
        scope.launch {
            downloadDao.updateStatus(downloadId, DownloadStatus.CANCELLED)
            java.io.File(context.cacheDir, "downloads/$downloadId").deleteRecursively()
        }
    }

    suspend fun resumeDownload(
        downloadId: String,
        onProgress: ProgressCallback,
        onComplete: CompleteCallback
    ) {
        val download = downloadDao.getDownloadById(downloadId) ?: return
        val mediaType = MediaType.valueOf(download.mediaType)

        downloadDao.updateStatus(downloadId, DownloadStatus.DOWNLOADING)
        _currentDownloadId.value = downloadId

        val job = scope.launch {
            try {
                executor.executeDownload(download, mediaType, onProgress, onComplete)
            } finally {
                if (_currentDownloadId.value == downloadId) {
                    _currentDownloadId.value = null
                }
            }
        }
        executor.activeDownloads[downloadId] = job
    }

    suspend fun deleteDownload(downloadId: String) {
        pauseDownload(downloadId)
        downloadDao.deleteDownloadById(downloadId)
        java.io.File(context.cacheDir, "downloads/$downloadId").deleteRecursively()
    }

    // ==================== Status Queries ====================

    fun getProgress(downloadId: String): StateFlow<Float>? = executor.getProgress(downloadId)

    fun isDownloadActive(downloadId: String): Boolean = executor.isDownloadActive(downloadId)

    // ==================== Lifecycle ====================

    fun cleanup() {
        queue.cleanup()
        executor.cleanup()
        scope.cancel()
    }

    companion object {
        @Volatile
        private var INSTANCE: DownloadManager? = null

        fun getInstance(context: Context, downloadDao: DownloadDao): DownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadManager(context.applicationContext, downloadDao).also {
                    INSTANCE = it
                }
            }
        }
    }
}

