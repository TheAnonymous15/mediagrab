package com.example.dwn.data

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages app settings using SharedPreferences
 */
class SettingsManager(private val appContext: Context) {

    private val prefs: SharedPreferences = appContext.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        return AppSettings(
            downloadQuality = prefs.getString(KEY_DOWNLOAD_QUALITY, DownloadQuality.BEST.name)
                ?.let { DownloadQuality.valueOf(it) } ?: DownloadQuality.BEST,
            notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true),
            autoResumeDownloads = prefs.getBoolean(KEY_AUTO_RESUME_DOWNLOADS, true),
            wifiOnlyDownloads = prefs.getBoolean(KEY_WIFI_ONLY_DOWNLOADS, false),
            darkMode = prefs.getBoolean(KEY_DARK_MODE, true)
        )
    }

    fun updateDownloadQuality(quality: DownloadQuality) {
        prefs.edit().putString(KEY_DOWNLOAD_QUALITY, quality.name).apply()
        _settings.value = _settings.value.copy(downloadQuality = quality)
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _settings.value = _settings.value.copy(notificationsEnabled = enabled)
    }

    fun updateAutoResumeDownloads(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RESUME_DOWNLOADS, enabled).apply()
        _settings.value = _settings.value.copy(autoResumeDownloads = enabled)
    }

    fun updateWifiOnlyDownloads(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY_DOWNLOADS, enabled).apply()
        _settings.value = _settings.value.copy(wifiOnlyDownloads = enabled)
    }

    fun updateDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _settings.value = _settings.value.copy(darkMode = enabled)
    }

    /**
     * Check if download is allowed based on WiFi-only setting
     * @return Pair<Boolean, String?> - (canDownload, errorMessage if not allowed)
     */
    fun canDownload(): Pair<Boolean, String?> {
        if (!_settings.value.wifiOnlyDownloads) {
            return Pair(true, null)
        }

        return if (isWifiConnected()) {
            Pair(true, null)
        } else {
            Pair(false, "WiFi-only downloads enabled. Please connect to WiFi or disable this setting.")
        }
    }

    /**
     * Check if device is connected to WiFi
     */
    fun isWifiConnected(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }

    /**
     * Get current network type for display
     */
    fun getNetworkType(): String {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return "No Connection"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "No Connection"
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Unknown"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> "WiFi"
                ConnectivityManager.TYPE_MOBILE -> "Mobile Data"
                ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                else -> if (networkInfo?.isConnected == true) "Connected" else "No Connection"
            }
        }
    }

    fun clearCache(context: Context): Boolean {
        return try {
            context.cacheDir.deleteRecursively()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getCacheSize(context: Context): String {
        val size = getFolderSize(context.cacheDir)
        return formatSize(size)
    }

    private fun getFolderSize(folder: java.io.File): Long {
        var size: Long = 0
        if (folder.isDirectory) {
            folder.listFiles()?.forEach { file ->
                size += if (file.isDirectory) getFolderSize(file) else file.length()
            }
        } else {
            size = folder.length()
        }
        return size
    }

    private fun formatSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    companion object {
        private const val PREFS_NAME = "dwn_settings"
        private const val KEY_DOWNLOAD_QUALITY = "download_quality"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_AUTO_RESUME_DOWNLOADS = "auto_resume_downloads"
        private const val KEY_WIFI_ONLY_DOWNLOADS = "wifi_only_downloads"
        private const val KEY_DARK_MODE = "dark_mode"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

data class AppSettings(
    val downloadQuality: DownloadQuality = DownloadQuality.BEST,
    val notificationsEnabled: Boolean = true,
    val autoResumeDownloads: Boolean = true,
    val wifiOnlyDownloads: Boolean = false,
    val darkMode: Boolean = true
)

enum class DownloadQuality(val label: String, val description: String, val ytDlpFormat: String) {
    BEST("Best", "Highest available quality", "bestvideo+bestaudio/best"),
    HIGH("High", "720p / 256kbps", "bestvideo[height<=720]+bestaudio/best[height<=720]"),
    MEDIUM("Medium", "480p / 192kbps", "bestvideo[height<=480]+bestaudio/best[height<=480]"),
    LOW("Low", "360p / 128kbps", "bestvideo[height<=360]+bestaudio/best[height<=360]")
}

