package com.example.dwn.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * ============================================
 * PERMISSION MANAGER
 * ============================================
 *
 * Centralized permission handling for all app features:
 * - Storage (read/write media files)
 * - Audio (recording, modify settings)
 * - Camera (podcast/video recording)
 * - Bluetooth (audio devices)
 * - Notifications
 * - Phone state (call handling)
 */
object PermissionManager {

    // ============================================
    // PERMISSION GROUPS
    // ============================================

    /**
     * Essential permissions for basic app functionality
     */
    fun getEssentialPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        android.util.Log.d("PermissionManager", "=== getEssentialPermissions ===")
        android.util.Log.d("PermissionManager", "Build.VERSION.SDK_INT = ${Build.VERSION.SDK_INT}")
        android.util.Log.d("PermissionManager", "Build.VERSION_CODES.TIRAMISU = ${Build.VERSION_CODES.TIRAMISU}")
        android.util.Log.d("PermissionManager", "Is Android 13+: ${Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU}")

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            // Use granular media permissions for Android 13+
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            android.util.Log.d("PermissionManager", "Added Android 13+ permissions: POST_NOTIFICATIONS, READ_MEDIA_AUDIO, READ_MEDIA_VIDEO")
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // Android 9 and below
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            android.util.Log.d("PermissionManager", "Added Android 9 and below permissions: WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE")
        } else {
            // Android 10-12
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            android.util.Log.d("PermissionManager", "Added Android 10-12 permissions: READ_EXTERNAL_STORAGE")
        }

        android.util.Log.d("PermissionManager", "getEssentialPermissions result: $permissions")
        return permissions
    }

    /**
     * Media access permissions for reading audio/video files
     * Uses intelligent version detection
     */
    fun getMediaPermissions(): List<String> {
        android.util.Log.d("PermissionManager", "=== getMediaPermissions ===")
        android.util.Log.d("PermissionManager", "Build.VERSION.SDK_INT = ${Build.VERSION.SDK_INT}")
        android.util.Log.d("PermissionManager", "Build.VERSION_CODES.TIRAMISU = ${Build.VERSION_CODES.TIRAMISU}")
        android.util.Log.d("PermissionManager", "Is Android 13+: ${Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU}")

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.util.Log.d("PermissionManager", "Returning Android 13+ granular media permissions")
            listOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            android.util.Log.d("PermissionManager", "Returning Android 9 and below storage permissions")
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            android.util.Log.d("PermissionManager", "Returning Android 10-12 READ_EXTERNAL_STORAGE")
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        android.util.Log.d("PermissionManager", "getMediaPermissions result: $permissions")
        return permissions
    }

    /**
     * Audio recording permissions for podcast, beat maker, etc.
     */
    fun getAudioRecordingPermissions(): List<String> {
        return listOf(Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Camera permissions for video recording
     */
    fun getCameraPermissions(): List<String> {
        return listOf(Manifest.permission.CAMERA)
    }

    /**
     * Bluetooth permissions for audio devices
     */
    fun getBluetoothPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            listOf(Manifest.permission.BLUETOOTH)
        }
    }

    /**
     * Phone state permission for call handling
     * NOTE: This permission shows a scary "make and manage phone calls" prompt
     * It's only used to detect incoming calls to pause playback
     * We make this optional and don't request it by default
     */
    fun getPhoneStatePermissions(): List<String> {
        return listOf(Manifest.permission.READ_PHONE_STATE)
    }

    /**
     * All permissions needed for full app functionality
     * NOTE: Phone state permission is excluded as it shows a confusing prompt
     */
    fun getAllPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        permissions.addAll(getEssentialPermissions())
        permissions.addAll(getAudioRecordingPermissions())
        permissions.addAll(getCameraPermissions())
        permissions.addAll(getBluetoothPermissions())
        // NOTE: Phone state permission intentionally excluded
        // It causes a scary "make and manage phone calls" dialog
        // The app works fine without it, we just won't auto-pause on calls
        // permissions.addAll(getPhoneStatePermissions())

        return permissions.distinct()
    }

    // ============================================
    // PERMISSION CHECKS
    // ============================================

    /**
     * Check if a single permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
               PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all permissions in a list are granted
     */
    fun arePermissionsGranted(context: Context, permissions: List<String>): Boolean {
        return permissions.all { isPermissionGranted(context, it) }
    }

    /**
     * Get list of permissions that are not yet granted
     */
    fun getMissingPermissions(context: Context, permissions: List<String>): List<String> {
        android.util.Log.d("PermissionManager", "getMissingPermissions checking: $permissions")
        val missing = permissions.filter {
            val granted = isPermissionGranted(context, it)
            android.util.Log.d("PermissionManager", "  $it -> granted=$granted")
            !granted
        }
        android.util.Log.d("PermissionManager", "getMissingPermissions result: $missing")
        return missing
    }

    /**
     * Check if essential permissions are granted
     */
    fun hasEssentialPermissions(context: Context): Boolean {
        return arePermissionsGranted(context, getEssentialPermissions())
    }

    /**
     * Check if audio recording permission is granted
     */
    fun hasAudioRecordingPermission(context: Context): Boolean {
        return arePermissionsGranted(context, getAudioRecordingPermissions())
    }

    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(context: Context): Boolean {
        return arePermissionsGranted(context, getCameraPermissions())
    }

    /**
     * Check if Bluetooth permissions are granted
     */
    fun hasBluetoothPermissions(context: Context): Boolean {
        return arePermissionsGranted(context, getBluetoothPermissions())
    }

    /**
     * Check if all permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        return arePermissionsGranted(context, getAllPermissions())
    }

    // ============================================
    // SPECIAL PERMISSIONS
    // ============================================

    /**
     * Check if MANAGE_EXTERNAL_STORAGE is needed for this Android version
     * It's required on Android 11+ for full file access
     */
    fun needsManageStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    /**
     * Check if app has MANAGE_EXTERNAL_STORAGE permission (Android 11+)
     * This is the "All files access" permission that allows reading all media
     */
    fun hasManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val result = Environment.isExternalStorageManager()
            android.util.Log.d("PermissionManager", "hasManageStoragePermission: $result")
            result
        } else {
            // Not needed on Android 10 and below
            true
        }
    }

    /**
     * Check if we have proper media access
     * On Android 11+, this requires MANAGE_EXTERNAL_STORAGE
     * On Android 10 and below, READ_EXTERNAL_STORAGE is sufficient
     */
    fun hasFullMediaAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ needs MANAGE_EXTERNAL_STORAGE for full access
            Environment.isExternalStorageManager()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ needs granular media permissions
            isPermissionGranted(context, Manifest.permission.READ_MEDIA_AUDIO) ||
            isPermissionGranted(context, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            // Android 10-12 needs READ_EXTERNAL_STORAGE
            isPermissionGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Open settings to grant MANAGE_EXTERNAL_STORAGE permission
     */
    fun openManageStorageSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                context.startActivity(intent)
            }
        }
    }

    /**
     * Open app settings for manual permission granting
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${context.packageName}")
        context.startActivity(intent)
    }

    // ============================================
    // PERMISSION RATIONALE
    // ============================================

    /**
     * Get human-readable description for a permission
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.POST_NOTIFICATIONS ->
                "Show download progress and playback controls"
            Manifest.permission.READ_MEDIA_AUDIO ->
                "Access your music files for playback"
            Manifest.permission.READ_MEDIA_VIDEO ->
                "Access your video files for playback"
            Manifest.permission.READ_MEDIA_IMAGES ->
                "Access album artwork and thumbnails"
            Manifest.permission.READ_EXTERNAL_STORAGE ->
                "Access media files on your device"
            Manifest.permission.WRITE_EXTERNAL_STORAGE ->
                "Save downloaded media to your device"
            Manifest.permission.RECORD_AUDIO ->
                "Record audio for podcasts and beat maker"
            Manifest.permission.CAMERA ->
                "Record video for podcasts"
            Manifest.permission.BLUETOOTH_CONNECT ->
                "Connect to Bluetooth audio devices"
            Manifest.permission.BLUETOOTH_SCAN ->
                "Find nearby Bluetooth audio devices"
            Manifest.permission.BLUETOOTH ->
                "Connect to Bluetooth audio devices"
            Manifest.permission.READ_PHONE_STATE ->
                "Pause playback during phone calls"
            Manifest.permission.MODIFY_AUDIO_SETTINGS ->
                "Control audio settings and equalizer"
            else -> "Required for app functionality"
        }
    }

    /**
     * Get icon name for a permission (for UI display)
     */
    fun getPermissionIcon(permission: String): String {
        return when (permission) {
            Manifest.permission.POST_NOTIFICATIONS -> "notifications"
            Manifest.permission.READ_MEDIA_AUDIO -> "music_note"
            Manifest.permission.READ_MEDIA_VIDEO -> "video_library"
            Manifest.permission.READ_MEDIA_IMAGES -> "image"
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "folder"
            Manifest.permission.RECORD_AUDIO -> "mic"
            Manifest.permission.CAMERA -> "camera"
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH -> "bluetooth"
            Manifest.permission.READ_PHONE_STATE -> "phone"
            else -> "security"
        }
    }

    /**
     * Get feature name that requires this permission
     */
    fun getPermissionFeature(permission: String): String {
        return when (permission) {
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
            Manifest.permission.READ_MEDIA_AUDIO -> "Music Player"
            Manifest.permission.READ_MEDIA_VIDEO -> "Video Player"
            Manifest.permission.READ_MEDIA_IMAGES -> "Album Art"
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Downloads"
            Manifest.permission.RECORD_AUDIO -> "Recording"
            Manifest.permission.CAMERA -> "Video Recording"
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH -> "Bluetooth Audio"
            Manifest.permission.READ_PHONE_STATE -> "Call Handling"
            else -> "App Features"
        }
    }

    // ============================================
    // PERMISSION REQUEST DATA
    // ============================================

    /**
     * Data class for permission request info
     */
    data class PermissionInfo(
        val permission: String,
        val feature: String,
        val description: String,
        val icon: String,
        val isGranted: Boolean
    )

    /**
     * Get detailed info for all permissions
     */
    fun getAllPermissionInfo(context: Context): List<PermissionInfo> {
        return getAllPermissions().map { permission ->
            PermissionInfo(
                permission = permission,
                feature = getPermissionFeature(permission),
                description = getPermissionDescription(permission),
                icon = getPermissionIcon(permission),
                isGranted = isPermissionGranted(context, permission)
            )
        }
    }

    /**
     * Get detailed info for missing permissions only
     */
    fun getMissingPermissionInfo(context: Context): List<PermissionInfo> {
        return getAllPermissionInfo(context).filter { !it.isGranted }
    }
}

