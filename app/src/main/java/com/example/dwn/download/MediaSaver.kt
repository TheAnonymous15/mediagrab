package com.example.dwn.download

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

private const val TAG = "MediaSaver"
private const val MAX_FILENAME_LENGTH = 100

/**
 * Handles saving downloaded media files to the appropriate system folders
 * (Music for audio, Movies for video) using MediaStore API on Android 10+
 * and direct file access on older versions.
 */
class MediaSaver(private val context: Context) {

    /**
     * Sanitizes a filename by removing invalid characters and truncating if too long
     */
    fun sanitizeFileName(name: String, maxLength: Int = MAX_FILENAME_LENGTH): String {
        val sanitized = name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("[\\x00-\\x1F]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        return if (sanitized.length > maxLength) {
            sanitized.take(maxLength - 3) + "..."
        } else {
            sanitized
        }
    }

    /**
     * Save audio file to Music folder
     */
    fun saveToMusicFolder(sourceFile: File): String {
        val originalName = sourceFile.nameWithoutExtension
        val extension = sourceFile.extension
        val sanitizedName = sanitizeFileName(originalName)
        val fileName = "$sanitizedName.$extension"

        Log.d(TAG, "saveToMusicFolder: Saving ${sourceFile.name} as $fileName")
        Log.d(TAG, "saveToMusicFolder: Source file size: ${sourceFile.length()} bytes")
        Log.d(TAG, "saveToMusicFolder: Android SDK: ${Build.VERSION.SDK_INT}")

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMusicFolderMediaStore(sourceFile, fileName)
        } else {
            saveToMusicFolderLegacy(sourceFile, fileName)
        }
    }

    /**
     * Save video file to Movies folder
     */
    fun saveToMoviesFolder(sourceFile: File): String {
        val originalName = sourceFile.nameWithoutExtension
        val extension = sourceFile.extension
        val sanitizedName = sanitizeFileName(originalName)
        val fileName = "$sanitizedName.$extension"

        Log.d(TAG, "saveToMoviesFolder: Saving ${sourceFile.name} as $fileName")
        Log.d(TAG, "saveToMoviesFolder: Source file size: ${sourceFile.length()} bytes")
        Log.d(TAG, "saveToMoviesFolder: Android SDK: ${Build.VERSION.SDK_INT}")

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMoviesFolderMediaStore(sourceFile, fileName)
        } else {
            saveToMoviesFolderLegacy(sourceFile, fileName)
        }
    }

    // Android 10+ using MediaStore
    private fun saveToMusicFolderMediaStore(sourceFile: File, fileName: String): String {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri == null) {
                Log.e(TAG, "saveToMusicFolder: MediaStore insert returned null URI")
                throw Exception("Failed to create MediaStore entry - null URI returned")
            }

            Log.d(TAG, "saveToMusicFolder: MediaStore URI created: $uri")

            resolver.openOutputStream(uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    val bytesCopied = inputStream.copyTo(outputStream)
                    Log.d(TAG, "saveToMusicFolder: Copied $bytesCopied bytes to MediaStore")
                }
            } ?: run {
                Log.e(TAG, "saveToMusicFolder: Failed to open output stream for $uri")
                throw Exception("Failed to open output stream for MediaStore")
            }

            // Mark as complete (not pending)
            contentValues.clear()
            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
            val rowsUpdated = resolver.update(uri, contentValues, null, null)
            Log.d(TAG, "saveToMusicFolder: Updated IS_PENDING=0, rows affected: $rowsUpdated")

            // Notify MediaStore
            notifyMediaScanner(uri.toString(), "audio/mpeg")

            Log.d(TAG, "saveToMusicFolder: Successfully saved $fileName via MediaStore")
            return fileName
        } catch (e: Exception) {
            Log.e(TAG, "saveToMusicFolder: MediaStore save failed", e)
            return saveToMusicFolderFallback(sourceFile, fileName, e)
        }
    }

    private fun saveToMusicFolderFallback(sourceFile: File, fileName: String, originalError: Exception): String {
        Log.d(TAG, "saveToMusicFolder: Trying fallback to app's external directory")
        val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (fallbackDir != null) {
            if (!fallbackDir.exists()) fallbackDir.mkdirs()
            val destFile = File(fallbackDir, fileName)
            sourceFile.copyTo(destFile, overwrite = true)
            Log.d(TAG, "saveToMusicFolder: Saved to fallback: ${destFile.absolutePath}")
            return fileName
        } else {
            throw Exception("MediaStore failed and fallback directory unavailable: ${originalError.message}")
        }
    }

    private fun saveToMusicFolderLegacy(sourceFile: File, fileName: String): String {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (!musicDir.exists()) musicDir.mkdirs()
        val destFile = File(musicDir, fileName)
        sourceFile.copyTo(destFile, overwrite = true)

        notifyMediaScanner(destFile.absolutePath, "audio/mpeg")

        Log.d(TAG, "saveToMusicFolder: Saved to ${destFile.absolutePath}")
        return fileName
    }

    // Android 10+ using MediaStore for video
    private fun saveToMoviesFolderMediaStore(sourceFile: File, fileName: String): String {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri == null) {
                Log.e(TAG, "saveToMoviesFolder: MediaStore insert returned null URI")
                throw Exception("Failed to create MediaStore entry - null URI returned")
            }

            Log.d(TAG, "saveToMoviesFolder: MediaStore URI created: $uri")

            resolver.openOutputStream(uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    val bytesCopied = inputStream.copyTo(outputStream)
                    Log.d(TAG, "saveToMoviesFolder: Copied $bytesCopied bytes to MediaStore")
                }
            } ?: run {
                Log.e(TAG, "saveToMoviesFolder: Failed to open output stream for $uri")
                throw Exception("Failed to open output stream for MediaStore")
            }

            // Mark as complete (not pending)
            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            val rowsUpdated = resolver.update(uri, contentValues, null, null)
            Log.d(TAG, "saveToMoviesFolder: Updated IS_PENDING=0, rows affected: $rowsUpdated")

            // Notify MediaStore
            notifyMediaScanner(uri.toString(), "video/mp4")

            Log.d(TAG, "saveToMoviesFolder: Successfully saved $fileName via MediaStore")
            return fileName
        } catch (e: Exception) {
            Log.e(TAG, "saveToMoviesFolder: MediaStore save failed", e)
            return saveToMoviesFolderFallback(sourceFile, fileName, e)
        }
    }

    private fun saveToMoviesFolderFallback(sourceFile: File, fileName: String, originalError: Exception): String {
        Log.d(TAG, "saveToMoviesFolder: Trying fallback to app's external directory")
        val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        if (fallbackDir != null) {
            if (!fallbackDir.exists()) fallbackDir.mkdirs()
            val destFile = File(fallbackDir, fileName)
            sourceFile.copyTo(destFile, overwrite = true)
            Log.d(TAG, "saveToMoviesFolder: Saved to fallback: ${destFile.absolutePath}")
            return fileName
        } else {
            throw Exception("MediaStore failed and fallback directory unavailable: ${originalError.message}")
        }
    }

    private fun saveToMoviesFolderLegacy(sourceFile: File, fileName: String): String {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        if (!moviesDir.exists()) moviesDir.mkdirs()
        val destFile = File(moviesDir, fileName)
        sourceFile.copyTo(destFile, overwrite = true)

        notifyMediaScanner(destFile.absolutePath, "video/mp4")

        Log.d(TAG, "saveToMoviesFolder: Saved to ${destFile.absolutePath}")
        return fileName
    }

    private fun notifyMediaScanner(path: String, mimeType: String) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(path),
                arrayOf(mimeType)
            ) { scannedPath, uri ->
                Log.d(TAG, "MediaScanner indexed: $scannedPath -> $uri")
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaScanner notification failed", e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MediaSaver? = null

        fun getInstance(context: Context): MediaSaver {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaSaver(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

