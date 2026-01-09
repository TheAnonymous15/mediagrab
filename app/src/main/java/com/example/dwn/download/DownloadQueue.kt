package com.example.dwn.download

import android.content.Context
import android.util.Log
import com.example.dwn.MediaType
import com.example.dwn.data.DownloadDao
import com.example.dwn.data.DownloadItem
import com.example.dwn.data.DownloadStatus
import com.example.dwn.data.SettingsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "DownloadQueue"
private const val MAX_CONCURRENT_DOWNLOADS = 3

/**
 * Manages the download queue and concurrent download processing.
 * Handles:
 * - Adding/removing items from queue
 * - Processing queue with concurrent download limits
 * - WiFi-only download settings
 * - Retry logic for failed downloads
 */
class DownloadQueue(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val executor: DownloadExecutor
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeDownloads = ConcurrentHashMap<String, Job>()

    private val _downloadQueue = MutableStateFlow<List<QueuedDownload>>(emptyList())
    val downloadQueue: StateFlow<List<QueuedDownload>> = _downloadQueue

    /**
     * Add a URL to the download queue
     */
    fun addToQueue(url: String, mediaType: MediaType): String {
        val queuedDownload = QueuedDownload(
            url = url,
            mediaType = mediaType,
            status = QueueStatus.QUEUED,
            statusMessage = "⏳ Queued for download"
        )
        _downloadQueue.update { currentQueue ->
            currentQueue + queuedDownload
        }
        processQueue()
        return queuedDownload.id
    }

    /**
     * Remove an item from the queue
     */
    fun removeFromQueue(id: String) {
        _downloadQueue.update { currentQueue ->
            currentQueue.filter { it.id != id }
        }
        activeDownloads[id]?.cancel()
        activeDownloads.remove(id)
    }

    /**
     * Clear all completed items from the queue
     */
    fun clearCompletedFromQueue() {
        _downloadQueue.update { currentQueue ->
            currentQueue.filter { it.status != QueueStatus.COMPLETED }
        }
    }

    /**
     * Retry a failed download
     */
    fun retryQueuedDownload(id: String) {
        updateQueueItem(id) {
            it.copy(status = QueueStatus.QUEUED, progress = 0f, statusMessage = "Queued for retry")
        }
        processQueue()
    }

    /**
     * Process the queue - start downloads up to max concurrent limit
     */
    fun processQueue() {
        scope.launch {
            val activeCount = _downloadQueue.value.count {
                it.status == QueueStatus.DOWNLOADING || it.status == QueueStatus.CHECKING
            }

            if (activeCount < MAX_CONCURRENT_DOWNLOADS) {
                val nextInQueue = _downloadQueue.value.firstOrNull { it.status == QueueStatus.QUEUED }
                nextInQueue?.let { queued ->
                    startQueuedDownload(queued)
                }
            }
        }
    }

    private suspend fun startQueuedDownload(queued: QueuedDownload) {
        // Check WiFi-only setting
        val settingsManager = SettingsManager.getInstance(context)
        val (canDownload, errorMessage) = settingsManager.canDownload()

        if (!canDownload) {
            updateQueueItem(queued.id) {
                it.copy(
                    status = QueueStatus.QUEUED,
                    statusMessage = errorMessage ?: "Waiting for WiFi..."
                )
            }
            return
        }

        // Update status to downloading
        updateQueueItem(queued.id) {
            it.copy(status = QueueStatus.DOWNLOADING, statusMessage = "Starting...")
        }

        val job = scope.launch {
            try {
                // Check if already downloaded
                val existingCompleted = downloadDao.getCompletedDownloadByUrlAndType(
                    queued.url,
                    queued.mediaType.name
                )
                if (existingCompleted != null) {
                    updateQueueItem(queued.id) {
                        it.copy(
                            status = QueueStatus.COMPLETED,
                            statusMessage = "Already downloaded",
                            progress = 1f
                        )
                    }
                    delay(2000)
                    removeFromQueue(queued.id)
                    processQueue()
                    return@launch
                }

                // Create download item
                val downloadItem = DownloadItem(
                    url = queued.url,
                    mediaType = queued.mediaType.name,
                    status = DownloadStatus.DOWNLOADING
                )
                downloadDao.insertDownload(downloadItem)

                // Execute download
                executor.executeDownload(
                    downloadItem = downloadItem,
                    mediaType = queued.mediaType,
                    onProgress = { progress, message ->
                        // Determine status based on message content
                        val newStatus = when {
                            message.contains("Merging", ignoreCase = true) ||
                            message.contains("Converting", ignoreCase = true) ||
                            message.contains("Extracting", ignoreCase = true) ||
                            message.contains("Processing", ignoreCase = true) -> QueueStatus.PROCESSING

                            message.contains("Saving", ignoreCase = true) ||
                            message.contains("Moving", ignoreCase = true) ||
                            message.contains("Finding", ignoreCase = true) ||
                            message.contains("Music folder", ignoreCase = true) ||
                            message.contains("Movies folder", ignoreCase = true) ||
                            message.contains("Cleaning", ignoreCase = true) ||
                            message.contains("Updating library", ignoreCase = true) -> QueueStatus.SAVING

                            else -> QueueStatus.DOWNLOADING
                        }

                        updateQueueItem(queued.id) {
                            it.copy(
                                status = newStatus,
                                progress = progress,
                                statusMessage = message,
                                title = downloadItem.title.ifEmpty {
                                    if (message.startsWith("⬇️") || message.startsWith("🚀")) "" else message
                                }
                            )
                        }
                    },
                    onComplete = { success, message, _ ->
                        updateQueueItem(queued.id) {
                            it.copy(
                                status = if (success) QueueStatus.COMPLETED else QueueStatus.FAILED,
                                statusMessage = message,
                                progress = if (success) 1f else it.progress
                            )
                        }
                        scope.launch {
                            delay(3000) // Show completed status briefly
                            if (success) {
                                removeFromQueue(queued.id)
                            }
                            processQueue()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Queue download error", e)
                updateQueueItem(queued.id) {
                    it.copy(status = QueueStatus.FAILED, statusMessage = "Error: ${e.message}")
                }
                processQueue()
            }
        }
        activeDownloads[queued.id] = job
    }

    private fun updateQueueItem(id: String, update: (QueuedDownload) -> QueuedDownload) {
        _downloadQueue.update { currentQueue ->
            currentQueue.map { if (it.id == id) update(it) else it }
        }
    }

    fun cleanup() {
        activeDownloads.values.forEach { it.cancel() }
        scope.cancel()
    }
}

