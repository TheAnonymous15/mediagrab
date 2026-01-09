package com.example.dwn.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.dwn.MainActivity
import com.example.dwn.MediaType
import com.example.dwn.R
import com.example.dwn.data.DownloadDatabase
import com.example.dwn.data.SettingsManager
import com.example.dwn.download.DownloadManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "NetworkMonitor"
private const val CHANNEL_ID = "network_status_channel"
private const val NOTIFICATION_ID = 2001

/**
 * High-sensitivity real-time network monitor with auto-resume functionality
 * Detects connection changes in < 1 second
 */
class NetworkMonitor private constructor(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Track all active networks for faster detection
    private val activeNetworks = mutableSetOf<Network>()

    private val _isConnected = MutableStateFlow(checkCurrentConnection())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionType = MutableStateFlow(getCurrentConnectionType())
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()

    private val _isWifiConnected = MutableStateFlow(checkWifiConnection())
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()

    // Callbacks for connection changes
    var onConnectionRestored: (() -> Unit)? = null
    var onConnectionLost: (() -> Unit)? = null
    var onWifiConnected: (() -> Unit)? = null

    private var wasConnected = checkCurrentConnection()
    private var wasWifiConnected = checkWifiConnection()

    // High-sensitivity network callback with immediate response
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            Log.d(TAG, "⚡ Network AVAILABLE - instant detection")
            activeNetworks.add(network)
            handleConnectionAvailable()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "⚡ Network LOST - instant detection")
            activeNetworks.remove(network)
            handleConnectionLost()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            // Immediately update connection type
            scope.launch {
                updateConnectionStatus()
            }
        }

        override fun onUnavailable() {
            Log.d(TAG, "⚡ Network UNAVAILABLE")
            handleConnectionLost()
        }

        override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
            Log.d(TAG, "Network blocked status changed: $blocked")
            if (blocked) {
                handleConnectionLost()
            } else {
                handleConnectionAvailable()
            }
        }
    }

    // Default network callback for tracking the primary connection
    private val defaultNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "⚡ Default network available")
            handleConnectionAvailable()
        }

        override fun onLost(network: Network) {
            Log.d(TAG, "⚡ Default network lost")
            // Check if we still have other networks
            if (activeNetworks.isEmpty()) {
                handleConnectionLost()
            }
        }
    }

    private fun handleConnectionAvailable() {
        scope.launch {
            val previouslyConnected = wasConnected
            val previouslyWifi = wasWifiConnected

            updateConnectionStatus()

            val nowConnected = _isConnected.value
            val nowWifi = _isWifiConnected.value

            if (!previouslyConnected && nowConnected) {
                Log.d(TAG, "🔗 Connection RESTORED!")
                wasConnected = true

                // Auto-resume downloads
                autoResumeDownloads()

                onConnectionRestored?.invoke()
            }

            if (!previouslyWifi && nowWifi) {
                Log.d(TAG, "📶 WiFi CONNECTED!")
                wasWifiConnected = true

                val settingsManager = SettingsManager.getInstance(context)
                if (settingsManager.settings.value.wifiOnlyDownloads) {
                    // Resume WiFi-only downloads
                    autoResumeDownloads()
                }
                onWifiConnected?.invoke()
            }
        }
    }

    private fun handleConnectionLost() {
        scope.launch {
            updateConnectionStatus()

            if (wasConnected && !_isConnected.value) {
                Log.d(TAG, "❌ Connection LOST!")
                wasConnected = false
                wasWifiConnected = false
                onConnectionLost?.invoke()
            }
        }
    }

    /**
     * Automatically resume all paused/failed downloads when connection is restored
     */
    private fun autoResumeDownloads() {
        scope.launch(Dispatchers.IO) {
            try {
                val settingsManager = SettingsManager.getInstance(context)

                // Check if auto-resume is enabled
                if (!settingsManager.settings.value.autoResumeDownloads) {
                    Log.d(TAG, "Auto-resume disabled in settings")
                    return@launch
                }

                // Check WiFi-only setting
                if (settingsManager.settings.value.wifiOnlyDownloads && !_isWifiConnected.value) {
                    Log.d(TAG, "WiFi-only enabled but not on WiFi - skipping auto-resume")
                    return@launch
                }

                val database = DownloadDatabase.getDatabase(context)
                val pausedDownloads = database.downloadDao().getPausedDownloads()

                if (pausedDownloads.isNotEmpty()) {
                    Log.d(TAG, "🚀 Auto-resuming ${pausedDownloads.size} download(s)")

                    val downloadManager = DownloadManager.getInstance(context, database.downloadDao())

                    pausedDownloads.forEach { download ->
                        try {
                            val mediaType = MediaType.valueOf(download.mediaType)
                            downloadManager.addToQueue(download.url, mediaType)
                            Log.d(TAG, "Queued: ${download.title}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to queue download: ${download.title}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during auto-resume", e)
            }
        }
    }

    init {
        createNotificationChannel()
        registerNetworkCallbacks()
    }

    private fun createNotificationChannel() {
        val name = "Network Status"
        val descriptionText = "Notifications about internet connectivity"
        val importance = NotificationManager.IMPORTANCE_HIGH // High importance for faster delivery
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableVibration(false)
            setShowBadge(false)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun registerNetworkCallbacks() {
        // High-sensitivity network request for all networks
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        try {
            // Register for all network changes
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

            // Also register default network callback for primary connection tracking
            connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback)

            Log.d(TAG, "✅ Network callbacks registered (high-sensitivity mode)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callbacks", e)
        }
    }

    private fun updateConnectionStatus() {
        _isConnected.value = checkCurrentConnection()
        _connectionType.value = getCurrentConnectionType()
        _isWifiConnected.value = checkWifiConnection()
    }

    private fun checkCurrentConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun checkWifiConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun getCurrentConnectionType(): ConnectionType {
        val network = connectivityManager.activeNetwork ?: return ConnectionType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionType.NONE
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> ConnectionType.VPN
            else -> ConnectionType.OTHER
        }
    }

    fun dismissNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            connectivityManager.unregisterNetworkCallback(defaultNetworkCallback)
            scope.cancel()
            Log.d(TAG, "Network callbacks unregistered")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister network callbacks", e)
        }
    }

    companion object {
        const val EXTRA_RESUME_DOWNLOADS = "extra_resume_downloads"

        @Volatile
        private var INSTANCE: NetworkMonitor? = null

        fun getInstance(context: Context): NetworkMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkMonitor(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

enum class ConnectionType {
    NONE,
    WIFI,
    MOBILE,
    ETHERNET,
    VPN,
    OTHER
}

/**
 * Broadcast receiver for boot completed to start monitoring
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed - initializing network monitor")
            NetworkMonitor.getInstance(context)
        }
    }
}


