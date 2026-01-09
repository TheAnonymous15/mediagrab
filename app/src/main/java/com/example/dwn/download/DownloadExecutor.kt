package com.example.dwn.download

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.dwn.MediaType
import com.example.dwn.data.DownloadDao
import com.example.dwn.data.DownloadItem
import com.example.dwn.data.DownloadStatus
import com.example.dwn.data.DownloadQuality
import com.example.dwn.data.SettingsManager
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

private const val TAG = "DownloadExecutor"

/**
 * Handles the actual download execution using yt-dlp.
 * Responsible for:
 * - Setting up download requests with quality settings
 * - Executing downloads with progress tracking
 * - Handling download cancellation and errors
 */
class DownloadExecutor(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val mediaSaver: MediaSaver
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val activeDownloads = ConcurrentHashMap<String, Job>()
    val downloadProgress = ConcurrentHashMap<String, MutableStateFlow<Float>>()

    /**
     * Execute a download for the given item
     */
    suspend fun executeDownload(
        downloadItem: DownloadItem,
        mediaType: MediaType,
        onProgress: ProgressCallback,
        onComplete: CompleteCallback
    ) {
        try {
            Log.d(TAG, "Starting download: ${downloadItem.url}")

            withContext(Dispatchers.Main) {
                onProgress(0f, "🚀 Initializing download...")
            }

            // Get quality settings
            val settingsManager = SettingsManager.getInstance(context)
            val quality = settingsManager.settings.value.downloadQuality

            withContext(Dispatchers.Main) {
                onProgress(0.01f, "📋 Preparing download settings...")
            }

            // Use app's cache directory
            val cacheDir = File(context.cacheDir, "downloads/${downloadItem.id}")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            withContext(Dispatchers.Main) {
                onProgress(0.02f, "📁 Cache directory ready...")
            }

            val request = buildDownloadRequest(downloadItem.url, mediaType, quality, cacheDir)

            withContext(Dispatchers.Main) {
                onProgress(0.03f, "🔗 Connecting to server...")
            }

            // Start fetching video info asynchronously
            fetchVideoInfoAsync(downloadItem)

            // Execute the download
            var lastProgress = 0f
            val job = coroutineContext[Job]

            YoutubeDL.getInstance().execute(request) { progress, etaInSeconds, output ->
                if (job?.isActive != true) return@execute

                val progressPercent = progress.coerceIn(0f, 100f) / 100f
                downloadProgress[downloadItem.id]?.value = progressPercent

                val statusMsg = buildStatusMessage(progress, lastProgress, etaInSeconds, output)
                if (progress > lastProgress) lastProgress = progress

                runBlocking(Dispatchers.Main) {
                    onProgress(progressPercent, statusMsg)
                }

                // Save progress periodically
                runBlocking {
                    downloadDao.updateProgress(
                        downloadItem.id,
                        progressPercent,
                        (progressPercent * 100).toLong()
                    )
                }
            }

            // Process and save the downloaded file
            val savedFileName = processDownloadedFile(downloadItem, mediaType, cacheDir, onProgress)

            // Update database
            withContext(Dispatchers.Main) {
                onProgress(1f, "✏️ Updating library...")
            }

            val savedPath = when (mediaType) {
                MediaType.MP3 -> "${Environment.DIRECTORY_MUSIC}/$savedFileName"
                MediaType.MP4 -> "${Environment.DIRECTORY_MOVIES}/$savedFileName"
            }

            downloadDao.markCompleted(
                downloadItem.id,
                System.currentTimeMillis(),
                savedPath,
                savedFileName
            )

            // Clean up cache
            withContext(Dispatchers.Main) {
                onProgress(1f, "🧹 Cleaning up...")
            }
            cacheDir.deleteRecursively()

            val completedItem = downloadDao.getDownloadById(downloadItem.id)

            withContext(Dispatchers.Main) {
                onComplete(true, "✅ Saved: $savedFileName", completedItem)
            }

        } catch (e: CancellationException) {
            Log.d(TAG, "Download cancelled: ${downloadItem.id}")
            downloadDao.updateStatus(downloadItem.id, DownloadStatus.PAUSED)
            withContext(Dispatchers.Main) {
                onComplete(false, "⏸️ Download paused", null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            val errorMsg = e.message?.take(100) ?: "Download failed"
            downloadDao.markFailed(downloadItem.id, errorMsg)
            withContext(Dispatchers.Main) {
                onComplete(false, "❌ Error: $errorMsg", null)
            }
        } finally {
            activeDownloads.remove(downloadItem.id)
            downloadProgress.remove(downloadItem.id)
        }
    }

    private fun buildDownloadRequest(
        url: String,
        mediaType: MediaType,
        quality: DownloadQuality,
        cacheDir: File
    ): YoutubeDLRequest {
        val request = YoutubeDLRequest(url)

        when (mediaType) {
            MediaType.MP3 -> {
                request.addOption("--extract-audio")
                request.addOption("--audio-format", "mp3")
                when (quality) {
                    DownloadQuality.BEST -> request.addOption("--audio-quality", "0")
                    DownloadQuality.HIGH -> request.addOption("--audio-quality", "2")
                    DownloadQuality.MEDIUM -> request.addOption("--audio-quality", "5")
                    DownloadQuality.LOW -> request.addOption("--audio-quality", "9")
                }
            }
            MediaType.MP4 -> {
                val formatString = when (quality) {
                    DownloadQuality.BEST -> "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"
                    DownloadQuality.HIGH -> "bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]/best[height<=720][ext=mp4]/best"
                    DownloadQuality.MEDIUM -> "bestvideo[height<=480][ext=mp4]+bestaudio[ext=m4a]/best[height<=480][ext=mp4]/best"
                    DownloadQuality.LOW -> "bestvideo[height<=360][ext=mp4]+bestaudio[ext=m4a]/best[height<=360][ext=mp4]/best"
                }
                request.addOption("-f", formatString)
                request.addOption("--merge-output-format", "mp4")
            }
        }

        request.addOption("-o", "${cacheDir.absolutePath}/%(title).80B.%(ext)s")
        request.addOption("--no-playlist")
        request.addOption("--windows-filenames")
        request.addOption("--restrict-filenames")

        return request
    }

    private fun fetchVideoInfoAsync(downloadItem: DownloadItem) {
        scope.launch {
            try {
                val videoInfo = YoutubeDL.getInstance().getInfo(downloadItem.url)
                val title = mediaSaver.sanitizeFileName(videoInfo.title ?: "Unknown")
                val thumbnail = videoInfo.thumbnail ?: ""
                downloadDao.updateDownload(
                    downloadItem.copy(
                        title = title,
                        thumbnailUrl = thumbnail
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not get video info: ${e.message}")
            }
        }
    }

    private fun buildStatusMessage(
        progress: Float,
        lastProgress: Float,
        etaInSeconds: Long,
        output: String
    ): String {
        return when {
            output.contains("Merging", ignoreCase = true) -> "🔄 Merging audio/video..."
            output.contains("Extracting audio", ignoreCase = true) -> "🎵 Extracting audio..."
            output.contains("Converting", ignoreCase = true) -> "🔄 Converting format..."
            output.contains("Post-processing", ignoreCase = true) -> "⚙️ Post-processing..."
            output.contains("Deleting original", ignoreCase = true) -> "🧹 Cleaning temp files..."
            output.contains("[download]", ignoreCase = true) && progress == 100f -> "📦 Finalizing download..."
            progress > lastProgress -> {
                val eta = if (etaInSeconds > 0) " • ${etaInSeconds}s left" else ""
                "⬇️ Downloading: ${progress.toInt()}%$eta"
            }
            else -> "⬇️ Downloading: ${progress.toInt()}%"
        }
    }

    private suspend fun processDownloadedFile(
        downloadItem: DownloadItem,
        mediaType: MediaType,
        cacheDir: File,
        onProgress: ProgressCallback
    ): String {
        withContext(Dispatchers.Main) {
            onProgress(0.96f, "📂 Finding downloaded file...")
        }

        val downloadedFiles = cacheDir.listFiles { file ->
            file.extension.equals(mediaType.extension, ignoreCase = true) ||
                    (mediaType == MediaType.MP4 && file.extension.equals("mkv", ignoreCase = true))
        }

        if (downloadedFiles.isNullOrEmpty()) {
            throw Exception("No ${mediaType.extension.uppercase()} file found")
        }

        val file = downloadedFiles.first()
        val fileSizeKB = file.length() / 1024
        val fileSizeMB = fileSizeKB / 1024f
        Log.d(TAG, "Found file: ${file.name} (${fileSizeKB}KB)")

        // Update title from filename if not already set
        withContext(Dispatchers.Main) {
            val sizeStr = if (fileSizeMB >= 1) "%.1f MB".format(fileSizeMB) else "${fileSizeKB}KB"
            onProgress(0.97f, "📝 Processing file ($sizeStr)...")
        }

        val titleFromFile = file.nameWithoutExtension
        val currentItem = downloadDao.getDownloadById(downloadItem.id)
        if (currentItem?.title.isNullOrEmpty() || currentItem?.title == "Unknown") {
            downloadDao.updateDownload(
                currentItem?.copy(title = titleFromFile) ?: downloadItem.copy(title = titleFromFile)
            )
        }

        // Save to appropriate folder
        val folderName = if (mediaType == MediaType.MP3) "Music" else "Movies"
        withContext(Dispatchers.Main) {
            onProgress(0.98f, "💾 Saving to $folderName folder...")
        }

        val savedFileName = when (mediaType) {
            MediaType.MP3 -> mediaSaver.saveToMusicFolder(file)
            MediaType.MP4 -> mediaSaver.saveToMoviesFolder(file)
        }

        withContext(Dispatchers.Main) {
            onProgress(0.99f, "✅ Saved: $savedFileName")
        }

        return savedFileName
    }

    fun cancelDownload(downloadId: String) {
        activeDownloads[downloadId]?.cancel()
    }

    fun isDownloadActive(downloadId: String): Boolean = activeDownloads.containsKey(downloadId)

    fun getProgress(downloadId: String): MutableStateFlow<Float>? = downloadProgress[downloadId]

    fun cleanup() {
        scope.cancel()
    }
}

