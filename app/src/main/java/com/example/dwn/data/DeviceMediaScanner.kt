package com.example.dwn.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "UnifiedMediaScanner"

/**
 * Data class representing a media item from the device
 */
data class DeviceMediaItem(
    val id: Long,
    val name: String,
    val path: String,
    val uri: Uri,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val mimeType: String,
    val artist: String? = null,
    val album: String? = null
)

/**
 * State holder for scanned media
 */
data class MediaScanState(
    val audioFiles: List<DeviceMediaItem> = emptyList(),
    val videoFiles: List<DeviceMediaItem> = emptyList(),
    val isScanning: Boolean = false,
    val hasPermission: Boolean = false,
    val lastScanTime: Long = 0
)

/**
 * Intelligent Permission Helper - Handles Android version-specific permission logic
 */
object MediaPermissionHelper {

    private const val ANDROID_9_PIE = 28
    private const val ANDROID_10_Q = 29
    private const val ANDROID_11_R = 30
    private const val ANDROID_12_S = 31
    private const val ANDROID_13_TIRAMISU = 33
    private const val ANDROID_14_U = 34
    private const val ANDROID_15 = 35

    /**
     * Check if MANAGE_EXTERNAL_STORAGE permission is granted (Android 11+)
     * This gives full access to all files on the device
     */
    fun hasManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val result = Environment.isExternalStorageManager()
            Log.d(TAG, "hasManageStoragePermission (Android 11+): $result")
            result
        } else {
            // On Android 10 and below, this permission doesn't exist
            Log.d(TAG, "hasManageStoragePermission: Not applicable for SDK ${Build.VERSION.SDK_INT}")
            true
        }
    }

    /**
     * Check if we should use MANAGE_EXTERNAL_STORAGE (Android 11+)
     */
    fun shouldUseManageStorage(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    fun isAndroid13OrHigher(): Boolean {
        val result = Build.VERSION.SDK_INT >= ANDROID_13_TIRAMISU
        Log.d(TAG, "isAndroid13OrHigher: SDK=${Build.VERSION.SDK_INT}, TIRAMISU=$ANDROID_13_TIRAMISU, result=$result")
        return result
    }

    fun isAndroid10OrHigher(): Boolean {
        return Build.VERSION.SDK_INT >= ANDROID_10_Q
    }

    fun isAndroid11OrHigher(): Boolean {
        return Build.VERSION.SDK_INT >= ANDROID_11_R
    }

    fun getAudioPermission(): String {
        return if (Build.VERSION.SDK_INT >= ANDROID_13_TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun getVideoPermission(): String {
        return if (Build.VERSION.SDK_INT >= ANDROID_13_TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun getRequiredMediaPermissions(): List<String> {
        Log.d(TAG, "getRequiredMediaPermissions: SDK_INT=${Build.VERSION.SDK_INT}")
        return when {
            Build.VERSION.SDK_INT >= ANDROID_13_TIRAMISU -> {
                Log.d(TAG, "Android 13+ detected - using READ_MEDIA_AUDIO and READ_MEDIA_VIDEO")
                listOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            }
            Build.VERSION.SDK_INT >= ANDROID_10_Q -> {
                Log.d(TAG, "Android 10-12 detected - using READ_EXTERNAL_STORAGE only")
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                Log.d(TAG, "Android 9 or below detected - using READ and WRITE EXTERNAL_STORAGE")
                listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    fun hasAudioPermission(context: Context): Boolean {
        // First check MANAGE_EXTERNAL_STORAGE for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            Log.d(TAG, "hasAudioPermission: MANAGE_EXTERNAL_STORAGE granted - full access")
            return true
        }

        val permission = getAudioPermission()
        val status = ContextCompat.checkSelfPermission(context, permission)
        val result = status == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "hasAudioPermission: checking '$permission' = $result (status=$status, GRANTED=${PackageManager.PERMISSION_GRANTED})")
        return result
    }

    fun hasVideoPermission(context: Context): Boolean {
        // First check MANAGE_EXTERNAL_STORAGE for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            Log.d(TAG, "hasVideoPermission: MANAGE_EXTERNAL_STORAGE granted - full access")
            return true
        }

        val permission = getVideoPermission()
        val status = ContextCompat.checkSelfPermission(context, permission)
        val result = status == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "hasVideoPermission: checking '$permission' = $result (status=$status, GRANTED=${PackageManager.PERMISSION_GRANTED})")
        return result
    }

    fun hasAnyMediaPermission(context: Context): Boolean {
        // First check MANAGE_EXTERNAL_STORAGE for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            Log.d(TAG, "hasAnyMediaPermission: MANAGE_EXTERNAL_STORAGE granted - full access")
            return true
        }

        val hasAudio = hasAudioPermission(context)
        val hasVideo = hasVideoPermission(context)
        val result = hasAudio || hasVideo
        Log.d(TAG, "hasAnyMediaPermission: hasAudio=$hasAudio, hasVideo=$hasVideo, result=$result")
        return result
    }

    fun getMissingMediaPermissions(context: Context): List<String> {
        val required = getRequiredMediaPermissions()
        val missing = required.filter { permission ->
            val status = ContextCompat.checkSelfPermission(context, permission)
            val isGranted = status == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Checking permission '$permission': status=$status, granted=$isGranted")
            !isGranted
        }
        Log.d(TAG, "getMissingMediaPermissions: required=$required, missing=$missing")
        return missing
    }

    fun logPermissionStatus(context: Context) {
        Log.d(TAG, "╔══════════════════════════════════════════╗")
        Log.d(TAG, "║       PERMISSION STATUS REPORT           ║")
        Log.d(TAG, "╠══════════════════════════════════════════╣")
        Log.d(TAG, "║ Device Info:")
        Log.d(TAG, "║   Android SDK: ${Build.VERSION.SDK_INT}")
        Log.d(TAG, "║   Android Version: ${Build.VERSION.RELEASE}")
        Log.d(TAG, "║   Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        Log.d(TAG, "╠══════════════════════════════════════════╣")
        Log.d(TAG, "║ Version Detection:")
        Log.d(TAG, "║   Is Android 13+ (API 33+): ${Build.VERSION.SDK_INT >= ANDROID_13_TIRAMISU}")
        Log.d(TAG, "║   Is Android 11+ (API 30+): ${Build.VERSION.SDK_INT >= ANDROID_11_R}")
        Log.d(TAG, "║   Is Android 10+ (API 29+): ${Build.VERSION.SDK_INT >= ANDROID_10_Q}")
        Log.d(TAG, "╠══════════════════════════════════════════╣")

        // Check MANAGE_EXTERNAL_STORAGE for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manageStorage = Environment.isExternalStorageManager()
            Log.d(TAG, "║ MANAGE_EXTERNAL_STORAGE: ${if (manageStorage) "✓ GRANTED" else "✗ DENIED"}")
            if (manageStorage) {
                Log.d(TAG, "║   (Full file access enabled)")
            }
        }

        Log.d(TAG, "╠══════════════════════════════════════════╣")
        Log.d(TAG, "║ Required Permissions: ${getRequiredMediaPermissions()}")
        Log.d(TAG, "╠══════════════════════════════════════════╣")
        Log.d(TAG, "║ Permission Status:")

        if (Build.VERSION.SDK_INT < ANDROID_13_TIRAMISU) {
            val storageStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            Log.d(TAG, "║   READ_EXTERNAL_STORAGE: ${if (storageStatus == PackageManager.PERMISSION_GRANTED) "✓ GRANTED" else "✗ DENIED"}")
        }

        if (Build.VERSION.SDK_INT >= ANDROID_13_TIRAMISU) {
            val audioStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
            val videoStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
            Log.d(TAG, "║   READ_MEDIA_AUDIO: ${if (audioStatus == PackageManager.PERMISSION_GRANTED) "✓ GRANTED" else "✗ DENIED"}")
            Log.d(TAG, "║   READ_MEDIA_VIDEO: ${if (videoStatus == PackageManager.PERMISSION_GRANTED) "✓ GRANTED" else "✗ DENIED"}")
        }

        Log.d(TAG, "╠══════════════════════════════════════════╣")
        Log.d(TAG, "║ Missing Permissions: ${getMissingMediaPermissions(context)}")
        Log.d(TAG, "║ Has Any Media Permission: ${hasAnyMediaPermission(context)}")
        Log.d(TAG, "╚══════════════════════════════════════════╝")
    }
}

/**
 * Unified Media Scanner - Singleton class for scanning device media
 */
class UnifiedMediaScanner private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: UnifiedMediaScanner? = null

        fun getInstance(context: Context): UnifiedMediaScanner {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UnifiedMediaScanner(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val _scanState = MutableStateFlow(MediaScanState())
    val scanState: StateFlow<MediaScanState> = _scanState.asStateFlow()

    private val cacheValidityMs = 5 * 60 * 1000L

    fun hasMediaPermission(): Boolean {
        MediaPermissionHelper.logPermissionStatus(context)

        // First check MANAGE_EXTERNAL_STORAGE for Android 11+ (gives full access)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            Log.d(TAG, "hasMediaPermission: MANAGE_EXTERNAL_STORAGE granted - full file access")
            _scanState.value = _scanState.value.copy(hasPermission = true)
            return true
        }

        val result = if (Build.VERSION.SDK_INT >= 33) {
            val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            val videoGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Android 13+ direct check: audio=$audioGranted, video=$videoGranted")
            audioGranted || videoGranted
        } else {
            val storageGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Android 12 and below direct check: storage=$storageGranted")
            storageGranted
        }

        Log.d(TAG, "hasMediaPermission final result: $result")
        _scanState.value = _scanState.value.copy(hasPermission = result)
        return result
    }

    fun hasAudioPermission(): Boolean {
        // Check MANAGE_EXTERNAL_STORAGE first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            return true
        }
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasVideoPermission(): Boolean {
        // Check MANAGE_EXTERNAL_STORAGE first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            return true
        }
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if MANAGE_EXTERNAL_STORAGE is needed and not granted
     */
    fun needsManageStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()
    }

    fun getRequiredPermissions(): List<String> {
        return MediaPermissionHelper.getRequiredMediaPermissions()
    }

    fun getMissingPermissions(): List<String> {
        return MediaPermissionHelper.getMissingMediaPermissions(context)
    }

    private fun isCacheValid(): Boolean {
        val state = _scanState.value
        return state.lastScanTime > 0 &&
                (System.currentTimeMillis() - state.lastScanTime) < cacheValidityMs &&
                (state.audioFiles.isNotEmpty() || state.videoFiles.isNotEmpty())
    }

    suspend fun scanAllMedia(forceRefresh: Boolean = false): MediaScanState {
        Log.d(TAG, "╔══════════════════════════════════════════╗")
        Log.d(TAG, "║         scanAllMedia CALLED              ║")
        Log.d(TAG, "╠══════════════════════════════════════════╣")
        Log.d(TAG, "║ forceRefresh: $forceRefresh")
        Log.d(TAG, "║ Current state: audio=${_scanState.value.audioFiles.size}, video=${_scanState.value.videoFiles.size}")
        Log.d(TAG, "╚══════════════════════════════════════════╝")

        val hasPermission = hasMediaPermission()
        Log.d(TAG, "scanAllMedia: hasPermission=$hasPermission")

        if (!hasPermission) {
            Log.w(TAG, "scanAllMedia: No permission - aborting scan")
            return _scanState.value.copy(hasPermission = false)
        }

        if (!forceRefresh && isCacheValid()) {
            Log.d(TAG, "scanAllMedia: Returning cached data")
            return _scanState.value
        }

        Log.d(TAG, "scanAllMedia: Starting scan...")
        _scanState.value = _scanState.value.copy(isScanning = true, hasPermission = true)

        return withContext(Dispatchers.IO) {
            Log.d(TAG, "scanAllMedia: Entering IO coroutine")

            val audioFiles = scanAudioFilesInternal()
            Log.d(TAG, "scanAllMedia: Audio scan returned ${audioFiles.size} files")

            val videoFiles = scanVideoFilesInternal()
            Log.d(TAG, "scanAllMedia: Video scan returned ${videoFiles.size} files")

            val newState = MediaScanState(
                audioFiles = audioFiles,
                videoFiles = videoFiles,
                isScanning = false,
                hasPermission = true,
                lastScanTime = System.currentTimeMillis()
            )

            _scanState.value = newState
            Log.d(TAG, "scanAllMedia: State updated - audio=${audioFiles.size}, video=${videoFiles.size}")
            newState
        }
    }

    suspend fun refreshMedia(): MediaScanState {
        return scanAllMedia(forceRefresh = true)
    }

    suspend fun getAudioFiles(forceRefresh: Boolean = false): List<DeviceMediaItem> {
        if (!hasAudioPermission()) return emptyList()
        if (forceRefresh || _scanState.value.audioFiles.isEmpty()) scanAllMedia(forceRefresh)
        return _scanState.value.audioFiles
    }

    suspend fun getVideoFiles(forceRefresh: Boolean = false): List<DeviceMediaItem> {
        if (!hasVideoPermission()) return emptyList()
        if (forceRefresh || _scanState.value.videoFiles.isEmpty()) scanAllMedia(forceRefresh)
        return _scanState.value.videoFiles
    }

    fun clearCache() {
        _scanState.value = MediaScanState(hasPermission = hasMediaPermission())
    }

    /**
     * Trigger MediaStore to rescan common media directories
     * This helps when files exist but aren't indexed yet
     * Returns the number of files found and requested for scanning
     */
    private fun triggerMediaScan(): Int {
        try {
            Log.d(TAG, "Triggering MediaStore rescan...")

            // Common directories to scan
            val dirsToScan = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS)
            )

            // Collect all media files from these directories
            val allMediaFiles = mutableListOf<String>()

            for (dir in dirsToScan) {
                if (dir.exists() && dir.canRead()) {
                    Log.d(TAG, "Scanning directory for files: ${dir.absolutePath}")
                    collectMediaFilesRecursively(dir, allMediaFiles)
                } else {
                    Log.d(TAG, "Cannot access directory: ${dir.absolutePath} (exists=${dir.exists()}, canRead=${dir.canRead()})")
                }
            }

            Log.d(TAG, "Found ${allMediaFiles.size} media files to request MediaStore scan")

            if (allMediaFiles.isEmpty()) {
                Log.w(TAG, "No media files found in common directories!")
                return 0
            }

            // Use CountDownLatch to wait for scanning to complete
            val latch = java.util.concurrent.CountDownLatch(allMediaFiles.size)
            var scannedCount = 0

            MediaScannerConnection.scanFile(
                context,
                allMediaFiles.toTypedArray(),
                null
            ) { path, uri ->
                scannedCount++
                if (scannedCount <= 10) {
                    Log.d(TAG, "MediaScanner indexed ($scannedCount/${allMediaFiles.size}): $path -> $uri")
                }
                latch.countDown()
            }

            // Wait up to 30 seconds for all files to be scanned
            Log.d(TAG, "Waiting for MediaScanner to index ${allMediaFiles.size} files...")
            val completed = latch.await(30, java.util.concurrent.TimeUnit.SECONDS)
            Log.d(TAG, "MediaScanner wait completed: $completed, scanned $scannedCount files")

            return allMediaFiles.size
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering media scan", e)
            return 0
        }
    }

    /**
     * Recursively collect media files from a directory
     */
    private fun collectMediaFilesRecursively(dir: File, files: MutableList<String>, depth: Int = 0) {
        // Limit recursion depth to avoid going too deep
        if (depth > 5) return

        if (!dir.exists() || !dir.canRead()) return

        // Skip hidden and system directories
        if (dir.name.startsWith(".") || dir.name == "Android") return

        try {
            dir.listFiles()?.forEach { file ->
                when {
                    file.isDirectory -> {
                        collectMediaFilesRecursively(file, files, depth + 1)
                    }
                    file.isFile -> {
                        val ext = file.extension.lowercase()
                        if (ext in audioExtensions || ext in videoExtensions) {
                            files.add(file.absolutePath)
                            if (files.size <= 20) {
                                Log.d(TAG, "Found media file: ${file.absolutePath}")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory: ${dir.absolutePath}", e)
        }
    }

    // Audio file extensions
    private val audioExtensions = setOf(
        "mp3", "m4a", "aac", "wav", "flac", "ogg", "wma", "opus", "amr", "3gp"
    )

    // Video file extensions
    private val videoExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "m4v", "ts"
    )

    // Scan audio files - uses MediaStore (required for Android 10+)
    private suspend fun scanAudioFilesInternal(): List<DeviceMediaItem> = withContext(Dispatchers.IO) {
        Log.d(TAG, "========== AUDIO SCAN STARTED ==========")
        Log.d(TAG, "SDK Version: ${Build.VERSION.SDK_INT}")

        // Check MANAGE_EXTERNAL_STORAGE first (gives full access on Android 11+)
        val hasManageStorage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()
        Log.d(TAG, "MANAGE_EXTERNAL_STORAGE granted: $hasManageStorage")

        val hasPermission = if (hasManageStorage) {
            true
        } else if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        Log.d(TAG, "scanAudioFilesInternal: hasPermission=$hasPermission")
        if (!hasPermission) {
            Log.w(TAG, "No permission - returning empty list")
            return@withContext emptyList()
        }

        // First try MediaStore without triggering rescan
        var mediaStoreFiles = scanAudioFromMediaStore()
        Log.d(TAG, "Initial MediaStore returned ${mediaStoreFiles.size} audio files")

        // If MediaStore is empty, trigger a rescan and try again
        if (mediaStoreFiles.isEmpty()) {
            Log.d(TAG, "MediaStore empty - triggering MediaStore rescan...")
            val filesFound = triggerMediaScan()

            if (filesFound > 0) {
                Log.d(TAG, "Rescan found $filesFound files - querying MediaStore again...")
                // Small delay to let MediaStore process the new files
                Thread.sleep(1000)
                mediaStoreFiles = scanAudioFromMediaStore()
                Log.d(TAG, "After rescan, MediaStore returned ${mediaStoreFiles.size} audio files")
            }
        }

        // If still empty and we have MANAGE_EXTERNAL_STORAGE, try file system directly
        if (mediaStoreFiles.isEmpty() && hasManageStorage) {
            Log.d(TAG, "MediaStore still empty - using direct file system scan with MANAGE_EXTERNAL_STORAGE")
            val fileSystemFiles = scanAudioFromFileSystem()
            Log.d(TAG, "File system scan returned ${fileSystemFiles.size} audio files")
            return@withContext fileSystemFiles
        }

        // If still empty on Android 9 or below, try file system
        if (mediaStoreFiles.isEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.d(TAG, "MediaStore still empty on Android 9- falling back to file system scan")
            val fileSystemFiles = scanAudioFromFileSystem()
            Log.d(TAG, "File system scan returned ${fileSystemFiles.size} audio files")
            return@withContext fileSystemFiles
        }

        // NEW: For Android 10-12, if MediaStore is still empty, try direct file system scan
        // READ_EXTERNAL_STORAGE on Android 10-12 should allow reading from Music/Download directories
        if (mediaStoreFiles.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < 33) {
            Log.d(TAG, "MediaStore empty on Android 10-12 - trying limited file system scan")
            val fileSystemFiles = scanAudioFromFileSystemLimited()
            Log.d(TAG, "Limited file system scan returned ${fileSystemFiles.size} audio files")
            return@withContext fileSystemFiles
        }

        mediaStoreFiles
    }

    // Scan video files - uses MediaStore (required for Android 10+)
    private suspend fun scanVideoFilesInternal(): List<DeviceMediaItem> = withContext(Dispatchers.IO) {
        Log.d(TAG, "========== VIDEO SCAN STARTED ==========")
        Log.d(TAG, "SDK Version: ${Build.VERSION.SDK_INT}")

        // Check MANAGE_EXTERNAL_STORAGE first (gives full access on Android 11+)
        val hasManageStorage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()
        Log.d(TAG, "MANAGE_EXTERNAL_STORAGE granted: $hasManageStorage")

        val hasPermission = if (hasManageStorage) {
            true
        } else if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        Log.d(TAG, "scanVideoFilesInternal: hasPermission=$hasPermission")
        if (!hasPermission) {
            Log.w(TAG, "No permission - returning empty list")
            return@withContext emptyList()
        }

        // Use MediaStore - this is the ONLY reliable way on Android 10+
        val mediaStoreFiles = scanVideoFromMediaStore()
        Log.d(TAG, "MediaStore returned ${mediaStoreFiles.size} video files")

        // If MediaStore is empty and we have MANAGE_EXTERNAL_STORAGE, try file system directly
        if (mediaStoreFiles.isEmpty() && hasManageStorage) {
            Log.d(TAG, "MediaStore empty - using direct file system scan with MANAGE_EXTERNAL_STORAGE")
            val fileSystemFiles = scanVideoFromFileSystem()
            Log.d(TAG, "File system scan returned ${fileSystemFiles.size} video files")
            return@withContext fileSystemFiles
        }

        // If MediaStore is empty on Android 9 or below, try file system
        if (mediaStoreFiles.isEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.d(TAG, "MediaStore empty on Android 9- falling back to file system scan")
            val fileSystemFiles = scanVideoFromFileSystem()
            Log.d(TAG, "File system scan returned ${fileSystemFiles.size} video files")
            return@withContext fileSystemFiles
        }

        // NEW: For Android 10-12, if MediaStore is still empty, try direct file system scan
        if (mediaStoreFiles.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < 33) {
            Log.d(TAG, "MediaStore empty on Android 10-12 - trying limited file system scan")
            val fileSystemFiles = scanVideoFromFileSystemLimited()
            Log.d(TAG, "Limited file system scan returned ${fileSystemFiles.size} video files")
            return@withContext fileSystemFiles
        }

        mediaStoreFiles
    }

    // MediaStore audio scan
    private fun scanAudioFromMediaStore(): List<DeviceMediaItem> {
        val audioFiles = mutableListOf<DeviceMediaItem>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM
        )

        try {
            context.contentResolver.query(collection, projection, null, null, "${MediaStore.Audio.Media.DATE_ADDED} DESC")?.use { cursor ->
                Log.d(TAG, "MediaStore Audio cursor count: ${cursor.count}")

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idColumn)
                        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                        audioFiles.add(DeviceMediaItem(
                            id = id,
                            name = cursor.getString(nameColumn) ?: "Unknown",
                            path = "",
                            uri = contentUri,
                            duration = cursor.getLong(durationColumn),
                            size = cursor.getLong(sizeColumn),
                            dateAdded = cursor.getLong(dateColumn),
                            mimeType = cursor.getString(mimeColumn) ?: "audio/*",
                            artist = cursor.getString(artistColumn),
                            album = cursor.getString(albumColumn)
                        ))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading audio row", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore audio scan error", e)
        }

        return audioFiles
    }

    // MediaStore video scan
    private fun scanVideoFromMediaStore(): List<DeviceMediaItem> {
        val videoFiles = mutableListOf<DeviceMediaItem>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE
        )

        try {
            context.contentResolver.query(collection, projection, null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC")?.use { cursor ->
                Log.d(TAG, "MediaStore Video cursor count: ${cursor.count}")

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idColumn)
                        val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                        videoFiles.add(DeviceMediaItem(
                            id = id,
                            name = cursor.getString(nameColumn) ?: "Unknown",
                            path = "",
                            uri = contentUri,
                            duration = cursor.getLong(durationColumn),
                            size = cursor.getLong(sizeColumn),
                            dateAdded = cursor.getLong(dateColumn),
                            mimeType = cursor.getString(mimeColumn) ?: "video/*"
                        ))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading video row", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore video scan error", e)
        }

        return videoFiles
    }

    // Direct file system scan for audio files
    private fun scanAudioFromFileSystem(): List<DeviceMediaItem> {
        val audioFiles = mutableListOf<DeviceMediaItem>()
        var fileId = 1L

        Log.d(TAG, "Starting file system audio scan...")

        // Get all storage directories to scan
        val dirsToScan = getStorageDirectories()
        Log.d(TAG, "Directories to scan: ${dirsToScan.map { it.absolutePath }}")

        for (dir in dirsToScan) {
            Log.d(TAG, "Scanning directory: ${dir.absolutePath}")
            scanDirectoryForFiles(dir, audioExtensions) { file ->
                try {
                    val uri = Uri.fromFile(file)
                    val mimeType = getMimeType(file.extension) ?: "audio/*"

                    audioFiles.add(DeviceMediaItem(
                        id = fileId++,
                        name = file.name,
                        path = file.absolutePath,
                        uri = uri,
                        duration = 0L, // Can't get duration without MediaMetadataRetriever
                        size = file.length(),
                        dateAdded = file.lastModified() / 1000,
                        mimeType = mimeType,
                        artist = null,
                        album = file.parentFile?.name
                    ))

                    if (audioFiles.size <= 10) {
                        Log.d(TAG, "Found audio: ${file.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing audio file: ${file.absolutePath}", e)
                }
            }
        }

        Log.d(TAG, "File system audio scan complete: ${audioFiles.size} files found")
        return audioFiles
    }

    // Direct file system scan for video files
    private fun scanVideoFromFileSystem(): List<DeviceMediaItem> {
        val videoFiles = mutableListOf<DeviceMediaItem>()
        var fileId = 100000L // Start at different ID to avoid conflicts

        Log.d(TAG, "Starting file system video scan...")

        // Get all storage directories to scan
        val dirsToScan = getStorageDirectories()

        for (dir in dirsToScan) {
            scanDirectoryForFiles(dir, videoExtensions) { file ->
                try {
                    val uri = Uri.fromFile(file)
                    val mimeType = getMimeType(file.extension) ?: "video/*"

                    videoFiles.add(DeviceMediaItem(
                        id = fileId++,
                        name = file.name,
                        path = file.absolutePath,
                        uri = uri,
                        duration = 0L,
                        size = file.length(),
                        dateAdded = file.lastModified() / 1000,
                        mimeType = mimeType
                    ))

                    if (videoFiles.size <= 10) {
                        Log.d(TAG, "Found video: ${file.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing video file: ${file.absolutePath}", e)
                }
            }
        }

        Log.d(TAG, "File system video scan complete: ${videoFiles.size} files found")
        return videoFiles
    }

    /**
     * Limited file system scan for Android 10-12
     * Only scans standard media directories that are accessible with READ_EXTERNAL_STORAGE
     * This is a fallback when MediaStore doesn't return any files
     */
    private fun scanAudioFromFileSystemLimited(): List<DeviceMediaItem> {
        val audioFiles = mutableListOf<DeviceMediaItem>()
        var fileId = 200000L

        Log.d(TAG, "Starting LIMITED file system audio scan for Android 10-12...")

        // Only scan standard media directories that should be accessible
        val dirsToScan = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS)
        )

        for (dir in dirsToScan) {
            if (!dir.exists()) {
                Log.d(TAG, "Directory doesn't exist: ${dir.absolutePath}")
                continue
            }
            if (!dir.canRead()) {
                Log.d(TAG, "Cannot read directory: ${dir.absolutePath}")
                continue
            }

            Log.d(TAG, "Limited scan - scanning: ${dir.absolutePath}")
            scanDirectoryForFilesLimited(dir, audioExtensions, maxDepth = 3) { file ->
                try {
                    // Use content:// URI via FileProvider or file URI
                    val uri = Uri.fromFile(file)
                    val mimeType = getMimeType(file.extension) ?: "audio/*"

                    audioFiles.add(DeviceMediaItem(
                        id = fileId++,
                        name = file.name,
                        path = file.absolutePath,
                        uri = uri,
                        duration = 0L,
                        size = file.length(),
                        dateAdded = file.lastModified() / 1000,
                        mimeType = mimeType,
                        artist = null,
                        album = file.parentFile?.name
                    ))

                    if (audioFiles.size <= 10) {
                        Log.d(TAG, "Limited scan found audio: ${file.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing audio file: ${file.absolutePath}", e)
                }
            }
        }

        Log.d(TAG, "Limited file system audio scan complete: ${audioFiles.size} files found")
        return audioFiles
    }

    /**
     * Limited file system scan for video files on Android 10-12
     */
    private fun scanVideoFromFileSystemLimited(): List<DeviceMediaItem> {
        val videoFiles = mutableListOf<DeviceMediaItem>()
        var fileId = 300000L

        Log.d(TAG, "Starting LIMITED file system video scan for Android 10-12...")

        // Only scan standard media directories
        val dirsToScan = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        )

        for (dir in dirsToScan) {
            if (!dir.exists() || !dir.canRead()) {
                Log.d(TAG, "Cannot access directory: ${dir.absolutePath}")
                continue
            }

            Log.d(TAG, "Limited scan - scanning: ${dir.absolutePath}")
            scanDirectoryForFilesLimited(dir, videoExtensions, maxDepth = 3) { file ->
                try {
                    val uri = Uri.fromFile(file)
                    val mimeType = getMimeType(file.extension) ?: "video/*"

                    videoFiles.add(DeviceMediaItem(
                        id = fileId++,
                        name = file.name,
                        path = file.absolutePath,
                        uri = uri,
                        duration = 0L,
                        size = file.length(),
                        dateAdded = file.lastModified() / 1000,
                        mimeType = mimeType
                    ))

                    if (videoFiles.size <= 10) {
                        Log.d(TAG, "Limited scan found video: ${file.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing video file: ${file.absolutePath}", e)
                }
            }
        }

        Log.d(TAG, "Limited file system video scan complete: ${videoFiles.size} files found")
        return videoFiles
    }

    /**
     * Limited depth directory scan - avoids going too deep
     */
    private fun scanDirectoryForFilesLimited(
        directory: File,
        extensions: Set<String>,
        maxDepth: Int,
        currentDepth: Int = 0,
        onFileFound: (File) -> Unit
    ) {
        if (currentDepth > maxDepth) return
        if (!directory.exists() || !directory.canRead()) return
        if (directory.name.startsWith(".")) return

        val skipDirs = setOf("Android", "lost+found", "cache", "thumbnails", ".thumbnails")
        if (directory.name in skipDirs) return

        try {
            val files = directory.listFiles() ?: return

            for (file in files) {
                if (file.isDirectory) {
                    scanDirectoryForFilesLimited(file, extensions, maxDepth, currentDepth + 1, onFileFound)
                } else if (file.isFile) {
                    val ext = file.extension.lowercase()
                    if (ext in extensions && file.length() > 0) {
                        onFileFound(file)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception in limited scan: ${directory.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error in limited scan: ${directory.absolutePath}", e)
        }
    }

    // Get all storage directories to scan
    private fun getStorageDirectories(): List<File> {
        val dirs = mutableListOf<File>()

        // Primary external storage
        val externalStorage = Environment.getExternalStorageDirectory()
        if (externalStorage.exists() && externalStorage.canRead()) {
            dirs.add(externalStorage)
            Log.d(TAG, "Added external storage: ${externalStorage.absolutePath}")
        }

        // Common media directories
        val commonDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS)
        )

        for (dir in commonDirs) {
            if (dir.exists() && dir.canRead() && !dirs.contains(dir)) {
                dirs.add(dir)
                Log.d(TAG, "Added common directory: ${dir.absolutePath}")
            }
        }

        // App-specific external directories
        context.getExternalFilesDirs(null).forEach { dir ->
            dir?.parentFile?.parentFile?.let { appDataDir ->
                if (appDataDir.exists() && appDataDir.canRead()) {
                    Log.d(TAG, "Found app data dir: ${appDataDir.absolutePath}")
                }
            }
        }

        return dirs
    }

    // Recursively scan directory for files with specific extensions
    private fun scanDirectoryForFiles(
        directory: File,
        extensions: Set<String>,
        onFileFound: (File) -> Unit
    ) {
        if (!directory.exists() || !directory.canRead()) {
            Log.d(TAG, "Cannot read directory: ${directory.absolutePath}")
            return
        }

        // Skip hidden directories (starting with .)
        if (directory.name.startsWith(".")) {
            Log.d(TAG, "Skipping hidden directory: ${directory.name}")
            return
        }

        // Skip system directories
        val skipDirs = setOf("Android", "lost+found", "cache", "thumbnails", ".thumbnails")
        if (directory.name in skipDirs) {
            Log.d(TAG, "Skipping system directory: ${directory.name}")
            return
        }

        try {
            val files = directory.listFiles() ?: return

            for (file in files) {
                if (file.isDirectory) {
                    // Recursively scan subdirectories
                    scanDirectoryForFiles(file, extensions, onFileFound)
                } else if (file.isFile) {
                    // Check file extension
                    val ext = file.extension.lowercase()
                    if (ext in extensions && file.length() > 0) {
                        onFileFound(file)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception scanning: ${directory.absolutePath}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning directory: ${directory.absolutePath}", e)
        }
    }

    // Get MIME type from file extension
    private fun getMimeType(extension: String): String? {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
    }
}

fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }
}
