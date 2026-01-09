package com.example.dwn.ui.screens

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.dwn.radio.*
import com.example.dwn.service.RadioPlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.util.Locale
import kotlin.math.*

// ============================================
// 🎧 HEADPHONE & NETWORK DETECTION UTILITIES
// ============================================

/**
 * Real-time headphone detection - checks if wired headphones are connected
 * (Required as FM antenna)
 */
@Composable
fun rememberHeadphonesState(): State<Boolean> {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val headphonesConnected = remember { mutableStateOf(checkHeadphonesConnected(audioManager)) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    AudioManager.ACTION_HEADSET_PLUG -> {
                        val state = intent.getIntExtra("state", 0)
                        headphonesConnected.value = state == 1
                    }
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(AudioManager.ACTION_HEADSET_PLUG)
        )

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                headphonesConnected.value = checkHeadphonesConnected(audioManager)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) { /* Ignore */ }
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return headphonesConnected
}

private fun checkHeadphonesConnected(audioManager: AudioManager): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        devices.any { device ->
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
    } else {
        @Suppress("DEPRECATION")
        audioManager.isWiredHeadsetOn
    }
}

/**
 * Network connectivity state with high-precision detection
 */
data class NetworkState(
    val isConnected: Boolean = false,
    val isWifi: Boolean = false,
    val isMobile: Boolean = false,
    val connectionType: String = "None",
    val signalStrength: Int = 0
)

@Composable
fun rememberNetworkState(): State<NetworkState> {
    val context = LocalContext.current
    val networkState = remember { mutableStateOf(checkCurrentNetworkState(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networkState.value = checkCurrentNetworkState(context)
            }

            override fun onLost(network: Network) {
                networkState.value = checkCurrentNetworkState(context)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                networkState.value = checkCurrentNetworkState(context)
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) { /* Handle permission issues */ }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                networkState.value = checkCurrentNetworkState(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } catch (e: Exception) { /* Ignore */ }
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return networkState
}

@Suppress("DEPRECATION")
private fun checkCurrentNetworkState(context: Context): NetworkState {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        if (capabilities != null) {
            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isMobile = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            val signalStrength = when {
                isWifi -> 80 // Approximate WiFi signal
                isMobile -> 70 // Approximate mobile signal
                else -> 0
            }

            NetworkState(
                isConnected = isConnected,
                isWifi = isWifi,
                isMobile = isMobile,
                connectionType = when {
                    isWifi -> "WiFi"
                    isMobile -> "Mobile"
                    else -> "Unknown"
                },
                signalStrength = signalStrength
            )
        } else {
            NetworkState()
        }
    } else {
        val networkInfo = connectivityManager.activeNetworkInfo
        NetworkState(
            isConnected = networkInfo?.isConnected == true,
            isWifi = networkInfo?.type == ConnectivityManager.TYPE_WIFI,
            isMobile = networkInfo?.type == ConnectivityManager.TYPE_MOBILE,
            connectionType = networkInfo?.typeName ?: "None",
            signalStrength = 50
        )
    }
}

/**
 * FM Hardware detection state
 */
data class FMHardwareState(
    val isAvailable: Boolean = false,
    val chipType: String = "Unknown",
    val capabilities: List<String> = emptyList(),
    val detectionMethod: String = "None",
    val isManualOverride: Boolean = false
)

@Composable
fun rememberFMHardwareState(): State<FMHardwareState> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Check if user has manually enabled FM mode - observe SharedPreferences
    val sharedPrefs = remember {
        context.getSharedPreferences("fm_radio_prefs", Context.MODE_PRIVATE)
    }

    var manualFMEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("manual_fm_enabled", false))
    }

    // Observe preference changes
    DisposableEffect(lifecycleOwner) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "manual_fm_enabled") {
                manualFMEnabled = sharedPrefs.getBoolean("manual_fm_enabled", false)
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val detectedState = remember { detectFMHardware(context) }

    // If manual override is enabled and auto-detection failed, return available state
    val hardwareState = remember(manualFMEnabled, detectedState) {
        if (manualFMEnabled && !detectedState.isAvailable) {
            mutableStateOf(FMHardwareState(
                isAvailable = true,
                chipType = "Manual Override",
                capabilities = listOf("User-enabled FM Mode", "Hardware may be available"),
                detectionMethod = "Manual",
                isManualOverride = true
            ))
        } else if (detectedState.isAvailable) {
            // Auto-detection found FM hardware
            mutableStateOf(detectedState)
        } else {
            // No FM hardware detected and manual mode not enabled
            mutableStateOf(detectedState)
        }
    }

    return hardwareState
}

/**
 * Enable/disable manual FM mode override
 */
fun setManualFMMode(context: Context, enabled: Boolean) {
    context.getSharedPreferences("fm_radio_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("manual_fm_enabled", enabled)
        .apply()
}

private fun detectFMHardware(context: Context): FMHardwareState {
    var isAvailable = false
    var chipType = "Unknown"
    var detectionMethod = "None"
    val capabilities = mutableListOf<String>()

    // Method 1: Check for FM receiver system feature
    if (context.packageManager.hasSystemFeature("android.hardware.fmradio")) {
        isAvailable = true
        chipType = "System FM"
        detectionMethod = "System Feature"
        capabilities.add("System FM Feature")
    }

    // Method 2: Try to detect FM receiver classes (comprehensive list)
    val fmReceiverClasses = listOf(
        // MediaTek
        "com.mediatek.fmradio.FmRadioService" to "MediaTek",
        "com.mediatek.fmradio.FmRadioActivity" to "MediaTek",
        "com.mediatek.fm.FmRadioService" to "MediaTek",
        "com.mtk.fmradio.FmRadioService" to "MediaTek",

        // Qualcomm
        "com.qualcomm.fmradio.FMRadioService" to "Qualcomm",
        "com.qualcomm.fmradio.FMRadio" to "Qualcomm",
        "com.qti.fmradio.FMRadioService" to "Qualcomm",
        "com.caf.fmradio.FMRadioService" to "Qualcomm (CAF)",

        // Samsung
        "com.samsung.broadcastradio.BroadcastRadioService" to "Samsung",
        "com.sec.android.app.fm.FMRadioService" to "Samsung",
        "com.samsung.android.app.fm.FMRadioService" to "Samsung",

        // Broadcom
        "com.broadcom.fm.fmreceiver.FmReceiver" to "Broadcom",
        "com.broadcom.fm.FMRadioService" to "Broadcom",

        // Spreadtrum/Unisoc
        "com.spreadtrum.fmradio.FMRadioService" to "Spreadtrum",
        "com.unisoc.fmradio.FMRadioService" to "Unisoc",

        // HiSilicon (Huawei)
        "com.huawei.fmradio.FMRadioService" to "HiSilicon",
        "com.hisilicon.fmradio.FMRadioService" to "HiSilicon",

        // AOSP/Generic
        "android.media.FmReceiver" to "Android Native",
        "com.android.fmradio.FMRadioService" to "AOSP",
        "com.android.fm.FMRadioService" to "AOSP",
        "com.codeaurora.fmradio.FMRadioService" to "CodeAurora",

        // Xiaomi/MIUI
        "com.xiaomi.fmradio.FMRadioService" to "Xiaomi",
        "com.miui.fmradio.FMRadioService" to "MIUI",

        // OPPO/Realme/OnePlus (ColorOS)
        "com.oppo.fmradio.FMRadioService" to "OPPO",
        "com.coloros.fmradio.FMRadioService" to "ColorOS",
        "com.oneplus.fmradio.FMRadioService" to "OnePlus",

        // Vivo (FunTouch)
        "com.vivo.fmradio.FMRadioService" to "Vivo",
        "com.bbk.fmradio.FMRadioService" to "Vivo/BBK",

        // LG
        "com.lge.fmradio.FMRadioService" to "LG",

        // Sony
        "com.sonyericsson.fmradio.FMRadioService" to "Sony",
        "com.sony.fmradio.FMRadioService" to "Sony",

        // Motorola
        "com.motorola.fmradio.FMRadioService" to "Motorola",

        // HTC
        "com.htc.fm.FMRadioService" to "HTC",

        // Nokia
        "com.nokia.fmradio.FMRadioService" to "Nokia",

        // Asus
        "com.asus.fmradio.FMRadioService" to "Asus",

        // Lenovo
        "com.lenovo.fmradio.FMRadioService" to "Lenovo",

        // ZTE
        "com.zte.fmradio.FMRadioService" to "ZTE",

        // Tecno/Infinix/Itel (Transsion)
        "com.transsion.fmradio.FMRadioService" to "Transsion",
        "com.tecno.fmradio.FMRadioService" to "Tecno",
        "com.infinix.fmradio.FMRadioService" to "Infinix"
    )

    if (!isAvailable) {
        for ((className, chip) in fmReceiverClasses) {
            try {
                Class.forName(className)
                isAvailable = true
                chipType = chip
                detectionMethod = "Class Detection"
                capabilities.add("$chip FM Chip")
                break
            } catch (_: ClassNotFoundException) {
                continue
            }
        }
    }

    // Method 3: Check for FM radio apps installed on the device
    if (!isAvailable) {
        val fmPackages = listOf(
            "com.mediatek.fmradio",
            "com.qualcomm.fmradio",
            "com.samsung.android.app.fm",
            "com.sec.android.app.fm",
            "com.android.fmradio",
            "com.xiaomi.fmradio",
            "com.miui.fmradio",
            "com.oppo.fmradio",
            "com.vivo.fmradio",
            "com.huawei.fmradio",
            "com.lge.fmradio",
            "com.sonyericsson.fmradio",
            "com.motorola.fmradio",
            "com.htc.fm",
            "com.asus.fmradio",
            "com.transsion.fmradio",
            "com.tecno.fmradio",
            "com.infinix.fmradio"
        )

        for (pkg in fmPackages) {
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                isAvailable = true
                chipType = "Device FM App"
                detectionMethod = "App Detection"
                capabilities.add("FM App Found: $pkg")
                break
            } catch (_: Exception) {
                continue
            }
        }
    }

    // Method 4: Check device features for any FM-related capabilities
    if (!isAvailable) {
        try {
            val features = context.packageManager.systemAvailableFeatures
            for (feature in features) {
                val featureName = feature.name?.lowercase() ?: continue
                if (featureName.contains("fm") ||
                    featureName.contains("radio") ||
                    featureName.contains("broadcast")) {
                    isAvailable = true
                    chipType = "Device Feature"
                    detectionMethod = "Feature Detection"
                    capabilities.add(feature.name ?: "FM Feature")
                }
            }
        } catch (_: Exception) { }
    }

    // Method 5: Check if device has audio routing capabilities that suggest FM hardware
    if (!isAvailable) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            // Check for speaker routing which is required for FM
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val hasWiredOutput = devices.any {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
            }
            if (hasWiredOutput) {
                capabilities.add("Wired Audio Output Available")
            }
        } catch (_: Exception) { }
    }

    // If still not detected, provide helpful message
    if (!isAvailable) {
        capabilities.add("Auto-detection failed")
        capabilities.add("Enable Manual FM Mode if your device has FM")
    }

    return FMHardwareState(
        isAvailable = isAvailable,
        chipType = chipType,
        capabilities = capabilities,
        detectionMethod = detectionMethod,
        isManualOverride = false
    )
}

// ============================================
// 🎨 FM RADIO COLORS
// ============================================

private object FMColors {
    val fmOrange = Color(0xFFFF6B35)
    val fmRed = Color(0xFFE53935)
    val fmGreen = Color(0xFF4CAF50)
    val fmBlue = Color(0xFF2196F3)
    val fmPurple = Color(0xFF9C27B0)
    val fmCyan = Color(0xFF00BCD4)
    val fmAmber = Color(0xFFFFC107)
    val fmTeal = Color(0xFF009688)

    val signalExcellent = Color(0xFF4CAF50)
    val signalGood = Color(0xFF8BC34A)
    val signalFair = Color(0xFFFFC107)
    val signalWeak = Color(0xFFFF9800)
    val signalPoor = Color(0xFFFF5722)
    val signalNone = Color(0xFFE53935)

    val bgDark = Color(0xFF0A0A0F)
    val bgMid = Color(0xFF101018)
    val surface = Color(0xFF161620)
    val surfaceVariant = Color(0xFF1E1E2A)
    val card = Color(0xFF222230)
    val cardHighlight = Color(0xFF2A2A3A)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B8)
    val textTertiary = Color(0xFF707080)

    val glassWhite = Color(0x14FFFFFF)
    val glassBorder = Color(0x20FFFFFF)

    val dialGlow = Color(0xFFFF6B35)
    val dialNeedle = Color(0xFFE53935)
    val dialMarker = Color(0xFFFFFFFF)
}

// ============================================
// 🌐 ONLINE RADIO DATA CLASSES
// ============================================

data class OnlineRadioStation(
    val stationuuid: String = "",
    val name: String = "",
    val url: String = "",
    val urlResolved: String = "",
    val homepage: String = "",
    val favicon: String = "",
    val country: String = "",
    val countrycode: String = "",
    val state: String = "",
    val language: String = "",
    val languagecodes: String = "",
    val votes: Int = 0,
    val codec: String = "",
    val bitrate: Int = 0,
    val hls: Int = 0,
    val lastcheckok: Int = 1,
    val clickcount: Int = 0,
    val clicktrend: Int = 0,
    val tags: String = "",
    val isFavorite: Boolean = false
)

// Radio Tab Type
enum class RadioTabType {
    OFFLINE_FM,
    ONLINE_RADIO,
    FAVOURITES
}

// ============================================
// 📻 INTERNAL FM RADIO CONTROLLER
// ============================================

/**
 * Internal FM Radio Controller that directly controls the device's FM hardware
 * Uses system broadcasts and service binding to control FM without opening external app
 */
class FMRadioController(private val context: Context) {

    private var isPlaying = false
    private var currentFrequency = 98.5f
    private var audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var serviceConnection: android.content.ServiceConnection? = null
    private var fmServiceBinder: android.os.IBinder? = null

    // Callback interface
    interface FMCallback {
        fun onFMStateChanged(isPlaying: Boolean)
        fun onFrequencyChanged(frequency: Float)
        fun onSignalStrengthChanged(strength: Int)
        fun onRDSDataReceived(stationName: String?, programType: String?, radioText: String?)
        fun onError(message: String)
    }

    private var callback: FMCallback? = null

    fun setCallback(callback: FMCallback) {
        this.callback = callback
    }

    /**
     * Detect FM service package on this device
     */
    fun detectFMPackage(): String? {
        val pm = context.packageManager
        val knownPackages = listOf(
            "com.mediatek.fmradio",
            "com.android.fmradio",
            "com.qualcomm.fmradio",
            "com.samsung.android.app.fm",
            "com.sec.android.app.fm",
            "com.xiaomi.fmradio",
            "com.miui.fmradio",
            "com.oppo.fmradio",
            "com.coloros.fmradio",
            "com.vivo.fmradio",
            "com.huawei.fmradio",
            "com.transsion.fmradio",
            "com.tecno.fmradio"
        )

        for (pkg in knownPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                android.util.Log.d("FMRadio", "Found FM package: $pkg")
                return pkg
            } catch (e: PackageManager.NameNotFoundException) {
                continue
            }
        }
        return null
    }

    private fun areHeadphonesConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devices.any {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                (Build.VERSION.SDK_INT >= 26 && it.type == AudioDeviceInfo.TYPE_USB_HEADSET)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.isWiredHeadsetOn
        }
    }

    /**
     * Start FM playback - binds to FM service and sends play command
     */
    fun play(): Boolean {
        if (!areHeadphonesConnected()) {
            callback?.onError("Connect wired headphones (FM antenna)")
            return false
        }

        val fmPackage = detectFMPackage()
        if (fmPackage == null) {
            callback?.onError("No FM service found")
            return false
        }

        try {
            // Bind to FM service
            bindToFMService(fmPackage)

            // Send FM power on broadcast
            sendFMBroadcast(fmPackage, "POWER_ON")

            // Tune to current frequency
            sendTuneBroadcast(fmPackage, currentFrequency)

            // Route audio
            audioManager.mode = AudioManager.MODE_NORMAL

            isPlaying = true
            callback?.onFMStateChanged(true)
            callback?.onFrequencyChanged(currentFrequency)

            return true
        } catch (e: Exception) {
            android.util.Log.e("FMRadio", "Play failed", e)
            callback?.onError("FM start failed: ${e.message}")
            return false
        }
    }

    private fun bindToFMService(fmPackage: String) {
        val serviceNames = listOf(
            "$fmPackage.FmRadioService",
            "$fmPackage.FmService",
            "$fmPackage.service.FmRadioService"
        )

        for (serviceName in serviceNames) {
            try {
                val intent = Intent().apply {
                    setClassName(fmPackage, serviceName)
                }

                serviceConnection = object : android.content.ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: android.os.IBinder?) {
                        fmServiceBinder = service
                        android.util.Log.d("FMRadio", "FM Service connected: $name")

                        // Try to call play method via reflection
                        try {
                            service?.let { binder ->
                                val serviceClass = binder.javaClass
                                // Try common method names
                                val methods = listOf("powerUp", "openDev", "play", "start", "enableFm")
                                for (methodName in methods) {
                                    try {
                                        val method = serviceClass.getMethod(methodName)
                                        method.invoke(binder)
                                        android.util.Log.d("FMRadio", "Called $methodName successfully")
                                        break
                                    } catch (e: Exception) {
                                        continue
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.d("FMRadio", "Service method call failed: ${e.message}")
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        fmServiceBinder = null
                        android.util.Log.d("FMRadio", "FM Service disconnected")
                    }
                }

                val bound = context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
                if (bound) {
                    android.util.Log.d("FMRadio", "Bound to FM service: $serviceName")
                    return
                }
            } catch (e: Exception) {
                android.util.Log.d("FMRadio", "Failed to bind to $serviceName: ${e.message}")
            }
        }
    }

    private fun sendFMBroadcast(fmPackage: String, action: String) {
        val actions = listOf(
            "$fmPackage.ACTION_$action",
            "android.intent.action.FM_$action",
            "com.android.fm.ACTION_$action"
        )

        for (broadcastAction in actions) {
            try {
                val intent = Intent(broadcastAction).apply {
                    setPackage(fmPackage)
                    putExtra("state", if (action == "POWER_ON") 1 else 0)
                    putExtra("frequency", (currentFrequency * 1000).toInt())
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                continue
            }
        }
    }

    private fun sendTuneBroadcast(fmPackage: String, frequency: Float) {
        val freqKHz = (frequency * 1000).toInt()

        val actions = listOf(
            "$fmPackage.ACTION_TUNE",
            "android.intent.action.FM_TUNE",
            "com.android.fm.ACTION_TUNE"
        )

        for (action in actions) {
            try {
                val intent = Intent(action).apply {
                    setPackage(fmPackage)
                    putExtra("frequency", freqKHz)
                    putExtra("freq", freqKHz)
                    putExtra("station", freqKHz)
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                continue
            }
        }

        // Also try calling tune via service binder
        fmServiceBinder?.let { binder ->
            try {
                val methods = listOf("tune", "tuneRadio", "setFrequency", "setFreq")
                for (methodName in methods) {
                    try {
                        val method = binder.javaClass.getMethod(methodName, Int::class.javaPrimitiveType)
                        method.invoke(binder, freqKHz)
                        android.util.Log.d("FMRadio", "Tuned via $methodName to $freqKHz")
                        break
                    } catch (e: Exception) {
                        continue
                    }
                }
            } catch (e: Exception) {
                android.util.Log.d("FMRadio", "Tune via binder failed: ${e.message}")
            }
        }
    }

    /**
     * Stop FM playback
     */
    fun stop() {
        val fmPackage = detectFMPackage()

        try {
            if (fmPackage != null) {
                sendFMBroadcast(fmPackage, "POWER_OFF")

                // Try to call stop via service binder
                fmServiceBinder?.let { binder ->
                    try {
                        val methods = listOf("powerDown", "closeDev", "stop", "disableFm")
                        for (methodName in methods) {
                            try {
                                val method = binder.javaClass.getMethod(methodName)
                                method.invoke(binder)
                                android.util.Log.d("FMRadio", "Called $methodName successfully")
                                break
                            } catch (e: Exception) {
                                continue
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.d("FMRadio", "Stop via binder failed")
                    }
                }
            }

            // Unbind service
            serviceConnection?.let {
                try {
                    context.unbindService(it)
                } catch (e: Exception) {
                    // Ignore
                }
            }
            serviceConnection = null
            fmServiceBinder = null

        } catch (e: Exception) {
            android.util.Log.e("FMRadio", "Stop failed", e)
        }

        isPlaying = false
        callback?.onFMStateChanged(false)
    }

    /**
     * Tune to a specific frequency
     */
    fun tune(frequency: Float): Boolean {
        if (frequency < 87.5f || frequency > 108.0f) {
            callback?.onError("Frequency must be 87.5-108.0 MHz")
            return false
        }

        currentFrequency = frequency

        val fmPackage = detectFMPackage()
        if (fmPackage != null && isPlaying) {
            sendTuneBroadcast(fmPackage, frequency)
        }

        callback?.onFrequencyChanged(frequency)
        return true
    }

    /**
     * Seek to next station
     */
    fun seekUp(): Float {
        currentFrequency = ((currentFrequency * 10).toInt() + 1) / 10f
        if (currentFrequency > 108f) currentFrequency = 87.5f
        if (isPlaying) tune(currentFrequency)
        callback?.onFrequencyChanged(currentFrequency)
        return currentFrequency
    }

    /**
     * Seek to previous station
     */
    fun seekDown(): Float {
        currentFrequency = ((currentFrequency * 10).toInt() - 1) / 10f
        if (currentFrequency < 87.5f) currentFrequency = 108f
        if (isPlaying) tune(currentFrequency)
        callback?.onFrequencyChanged(currentFrequency)
        return currentFrequency
    }

    /**
     * Set volume
     */
    fun setVolume(volume: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volume * maxVolume).toInt(), 0)
    }

    /**
     * Mute/unmute
     */
    fun setMute(mute: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
                0
            )
        }
    }

    fun isPlaying(): Boolean = isPlaying
    fun getFrequency(): Float = currentFrequency
    fun isFMAvailable(): Boolean = detectFMPackage() != null

    fun release() {
        stop()
    }
}

/**
 * Composable state holder for FM Radio
 */
@Composable
fun rememberFMRadioController(): FMRadioController {
    val context = LocalContext.current
    return remember { FMRadioController(context) }
}

/**
 * Data class to hold FM app info
 */
data class FMRadioApp(
    val packageName: String,
    val appName: String,
    val launchIntent: Intent?
)

/**
 * Get list of installed FM radio apps on the device
 */
fun getInstalledFMRadioApps(context: Context): List<FMRadioApp> {
    val packageManager = context.packageManager
    val fmApps = mutableListOf<FMRadioApp>()

    // Known FM radio package names for various manufacturers
    val knownFMPackages = listOf(
        // MediaTek
        "com.mediatek.fmradio",
        "com.mtk.fmradio",
        // Qualcomm
        "com.qualcomm.fmradio",
        "com.qti.fmradio",
        "com.caf.fmradio",
        // Samsung
        "com.sec.android.app.fm",
        "com.samsung.android.app.fm",
        // Xiaomi/MIUI
        "com.xiaomi.fmradio",
        "com.miui.fmradio",
        "com.miui.fm",
        // OPPO/Realme/OnePlus
        "com.oppo.fmradio",
        "com.coloros.fmradio",
        "com.oneplus.fmradio",
        // Vivo
        "com.vivo.fmradio",
        "com.bbk.fmradio",
        // Huawei
        "com.huawei.fmradio",
        // LG
        "com.lge.fmradio",
        // Sony
        "com.sonyericsson.fmradio",
        "com.sony.fmradio",
        // Motorola
        "com.motorola.fmradio",
        // HTC
        "com.htc.fm",
        // AOSP/Generic
        "com.android.fmradio",
        "com.android.fm",
        // Tecno/Infinix/Itel
        "com.transsion.fmradio",
        "com.tecno.fmradio",
        "com.infinix.fmradio",
        // Other common ones
        "com.codeaurora.fmradio",
        "fm.a2d.sf",
        "com.sonyericsson.fmradio"
    )

    for (pkg in knownFMPackages) {
        try {
            val appInfo = packageManager.getApplicationInfo(pkg, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                fmApps.add(FMRadioApp(pkg, appName, launchIntent))
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // App not installed, continue
        }
    }

    // Also search for any app with "fm" and "radio" in the name
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
    for (resolveInfo in resolveInfoList) {
        val pkgName = resolveInfo.activityInfo.packageName
        val appName = resolveInfo.loadLabel(packageManager).toString().lowercase()

        if ((appName.contains("fm") && appName.contains("radio")) ||
            appName.contains("fmradio") ||
            pkgName.contains("fmradio") ||
            pkgName.contains("fm.radio")) {

            if (fmApps.none { it.packageName == pkgName }) {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkgName)
                if (launchIntent != null) {
                    fmApps.add(FMRadioApp(
                        pkgName,
                        resolveInfo.loadLabel(packageManager).toString(),
                        launchIntent
                    ))
                }
            }
        }
    }

    return fmApps
}

/**
 * Launch the system FM Radio app as fallback
 */
fun launchFMRadioApp(context: Context, frequency: Float? = null): Boolean {
    val fmApps = getInstalledFMRadioApps(context)

    if (fmApps.isEmpty()) {
        Toast.makeText(
            context,
            "No FM Radio app found on this device",
            Toast.LENGTH_LONG
        ).show()
        return false
    }

    val fmApp = fmApps.first()

    try {
        val intent = fmApp.launchIntent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            frequency?.let {
                putExtra("frequency", (it * 1000).toInt())
                putExtra("freq", it)
                putExtra("fm_frequency", it)
            }
        }

        if (intent != null) {
            context.startActivity(intent)
            Toast.makeText(
                context,
                "Opening ${fmApp.appName}...",
                Toast.LENGTH_SHORT
            ).show()
            return true
        }
    } catch (e: Exception) {
        android.util.Log.e("FMRadio", "Failed to launch FM app", e)
    }

    Toast.makeText(
        context,
        "Failed to open FM Radio app",
        Toast.LENGTH_SHORT
    ).show()
    return false
}

/**
 * Check if any FM Radio app is installed
 */
fun hasFMRadioApp(context: Context): Boolean {
    return getInstalledFMRadioApps(context).isNotEmpty()
}

// ============================================
// 🌐 ONLINE RADIO API FUNCTIONS
// ============================================

suspend fun fetchOnlineRadioStations(
    country: String? = null,
    stationName: String? = null
): List<OnlineRadioStation> = withContext(Dispatchers.IO) {
    try {
        val baseUrl = "https://de1.api.radio-browser.info/json/stations"

        // URL encode the parameters
        val encodedCountry = country?.trim()?.let { java.net.URLEncoder.encode(it, "UTF-8") }
        val encodedStation = stationName?.trim()?.let { java.net.URLEncoder.encode(it, "UTF-8") }

        val url = when {
            !encodedCountry.isNullOrBlank() && !encodedStation.isNullOrBlank() ->
                "$baseUrl/search?name=$encodedStation&country=$encodedCountry&limit=100&hidebroken=true&order=clickcount&reverse=true"
            !encodedCountry.isNullOrBlank() ->
                "$baseUrl/bycountry/$encodedCountry?hidebroken=true&order=clickcount&reverse=true"
            !encodedStation.isNullOrBlank() ->
                "$baseUrl/search?name=$encodedStation?hidebroken=true&order=clickcount&reverse=true"
            else ->
                "$baseUrl/bycountry/Kenya?hidebroken=true&order=clickcount&reverse=true"
        }

        android.util.Log.d("OnlineRadio", "Fetching stations from: $url")

        val connection = URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "MediaGrab/1.0")
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.doInput = true

        val responseCode = connection.responseCode
        android.util.Log.d("OnlineRadio", "Response code: $responseCode")

        if (responseCode != 200) {
            android.util.Log.e("OnlineRadio", "HTTP error: $responseCode")
            return@withContext emptyList()
        }

        val response = connection.inputStream.bufferedReader().readText()
        android.util.Log.d("OnlineRadio", "Response length: ${response.length}")

        val jsonArray = JSONArray(response)
        android.util.Log.d("OnlineRadio", "Stations found: ${jsonArray.length()}")

        val stations = mutableListOf<OnlineRadioStation>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val stationUrl = obj.optString("url", "")
            val resolvedUrl = obj.optString("url_resolved", "")

            // Only add stations with playable URLs
            if (stationUrl.isNotBlank() || resolvedUrl.isNotBlank()) {
                stations.add(
                    OnlineRadioStation(
                        stationuuid = obj.optString("stationuuid", ""),
                        name = obj.optString("name", "Unknown Station"),
                        url = stationUrl,
                        urlResolved = resolvedUrl,
                        homepage = obj.optString("homepage", ""),
                        favicon = obj.optString("favicon", ""),
                        country = obj.optString("country", ""),
                        countrycode = obj.optString("countrycode", ""),
                        state = obj.optString("state", ""),
                        language = obj.optString("language", ""),
                        languagecodes = obj.optString("languagecodes", ""),
                        votes = obj.optInt("votes", 0),
                        codec = obj.optString("codec", ""),
                        bitrate = obj.optInt("bitrate", 0),
                        hls = obj.optInt("hls", 0),
                        lastcheckok = obj.optInt("lastcheckok", 1),
                        clickcount = obj.optInt("clickcount", 0),
                        clicktrend = obj.optInt("clicktrend", 0),
                        tags = obj.optString("tags", "")
                    )
                )
            }
        }

        android.util.Log.d("OnlineRadio", "Valid stations: ${stations.size}")
        stations
    } catch (e: Exception) {
        android.util.Log.e("OnlineRadio", "Error fetching stations", e)
        e.printStackTrace()
        emptyList()
    }
}

// ============================================
// 📻 SIMULATED STATION DATABASE
// ============================================

private data class SimulatedStation(
    val frequency: Float,
    val name: String,
    val genre: String,
    val nowPlaying: String,
    val artist: String
)

private val simulatedStations = listOf(
    SimulatedStation(88.5f, "Classic FM", "Classical", "Symphony No. 9", "Beethoven"),
    SimulatedStation(91.3f, "Jazz 91.3", "Jazz", "Take Five", "Dave Brubeck"),
    SimulatedStation(93.7f, "Rock Radio", "Rock", "Bohemian Rhapsody", "Queen"),
    SimulatedStation(95.5f, "Pop Hits", "Pop", "Blinding Lights", "The Weeknd"),
    SimulatedStation(98.1f, "Urban Beats", "Hip Hop", "God's Plan", "Drake"),
    SimulatedStation(100.3f, "Country 100", "Country", "Jolene", "Dolly Parton"),
    SimulatedStation(102.7f, "Electronic Mix", "Electronic", "Levels", "Avicii"),
    SimulatedStation(105.1f, "News Radio", "Talk", "Morning News", "Live"),
    SimulatedStation(107.5f, "Oldies Gold", "Oldies", "Hotel California", "Eagles")
)

// ============================================
// 📻 FM RADIO SCREEN - WITH 3 TABS
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FMRadioScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Current selected tab
    var selectedTab by remember { mutableStateOf(RadioTabType.ONLINE_RADIO) }

    // ============================================
    // 🎧 REAL HARDWARE DETECTION
    // ============================================

    val headphonesState = rememberHeadphonesState()
    val headphonesConnected = headphonesState.value

    val networkState = rememberNetworkState()
    val isNetworkConnected = networkState.value.isConnected
    val networkType = networkState.value.connectionType

    val fmHardwareState = rememberFMHardwareState()
    val isFMHardwareAvailable = fmHardwareState.value.isAvailable
    val fmChipType = fmHardwareState.value.chipType

    val fmStatus = remember(headphonesConnected, isFMHardwareAvailable) {
        when {
            !isFMHardwareAvailable -> "No FM Hardware"
            !headphonesConnected -> "Connect Headphones (Antenna)"
            else -> "FM Ready"
        }
    }

    val shouldUseFallback = remember(headphonesConnected, isFMHardwareAvailable, isNetworkConnected) {
        (!isFMHardwareAvailable || !headphonesConnected) && isNetworkConnected
    }

    // ============================================
    // 📻 FM STATE
    // ============================================
    var currentMode by remember { mutableStateOf(RadioMode.HYBRID_AUTO) }
    var currentFrequency by remember { mutableFloatStateOf(98.5f) }
    var isPlaying by remember { mutableStateOf(false) }
    var isStereo by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(0.8f) }
    var signalStrength by remember { mutableIntStateOf(75) }
    var isFallbackActive by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showEQPresets by remember { mutableStateOf(false) }
    var showFavorites by remember { mutableStateOf(false) }
    var showScanResults by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf<Int?>(null) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var showHardwareStatus by remember { mutableStateOf(false) }

    var rdsStationName by remember { mutableStateOf("RADIO FM") }
    var rdsNowPlaying by remember { mutableStateOf("Tune to a station") }
    var rdsProgramType by remember { mutableStateOf("FM Radio") }
    var rdsArtist by remember { mutableStateOf("") }

    var presets by remember { mutableStateOf(
        listOf(
            FMStation(frequency = 88.5f, name = "Classic FM", presetSlot = 1, isPreset = true),
            FMStation(frequency = 93.7f, name = "Rock Radio", presetSlot = 2, isPreset = true),
            FMStation(frequency = 95.5f, name = "Pop Hits", presetSlot = 3, isPreset = true),
            FMStation(frequency = 98.1f, name = "Urban Beats", presetSlot = 4, isPreset = true),
            FMStation(frequency = 102.7f, name = "Electronic", presetSlot = 5, isPreset = true),
            FMStation(frequency = 107.5f, name = "Oldies", presetSlot = 6, isPreset = true)
        )
    )}

    var favorites by remember { mutableStateOf<List<FMStation>>(emptyList()) }
    var scannedStations by remember { mutableStateOf<List<FMStation>>(emptyList()) }
    var recentStations by remember { mutableStateOf<List<FMStation>>(emptyList()) }
    var selectedEQPreset by remember { mutableStateOf("FM Enhanced") }

    // ============================================
    // 📻 INTEGRATED FM RADIO CONTROLLER
    // ============================================
    val fmController = remember { FMRadioController(context) }
    var fmError by remember { mutableStateOf<String?>(null) }
    var isFMInitialized by remember { mutableStateOf(false) }

    // FM Controller callback
    DisposableEffect(fmController) {
        fmController.setCallback(object : FMRadioController.FMCallback {
            override fun onFMStateChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onFrequencyChanged(frequency: Float) {
                currentFrequency = frequency
            }

            override fun onSignalStrengthChanged(strength: Int) {
                signalStrength = strength
            }

            override fun onRDSDataReceived(stationName: String?, programType: String?, radioText: String?) {
                stationName?.let { rdsStationName = it }
                programType?.let { rdsProgramType = it }
                radioText?.let { rdsNowPlaying = it }
            }

            override fun onError(message: String) {
                fmError = message
                android.util.Log.e("FMRadio", "FM Error: $message")
            }
        })

        onDispose {
            fmController.release()
        }
    }

    // FM Play/Pause function - Opens external FM Radio app
    fun toggleFMPlayback() {
        fmError = null

        // Check if headphones are connected (required for FM antenna)
        if (!headphonesConnected) {
            fmError = "Please connect wired headphones (FM antenna required)"
            Toast.makeText(context, fmError, Toast.LENGTH_LONG).show()
            return
        }

        // Launch the external FM Radio app
        if (hasFMRadioApp(context)) {
            launchFMRadioApp(context, currentFrequency)
        } else {
            fmError = "No FM Radio app found. Try Online Radio instead."
            Toast.makeText(context, fmError, Toast.LENGTH_LONG).show()
        }
    }

    // FM Tune function
    fun tuneFM(frequency: Float) {
        currentFrequency = frequency
        // Station info will be updated by LaunchedEffect on currentFrequency change
    }

    // FM Seek functions
    fun fmSeekUp() {
        if (isSeeking) return
        scope.launch {
            isSeeking = true
            val newFreq = fmController.seekUp()
            currentFrequency = newFreq
            // Station info will be updated by LaunchedEffect on currentFrequency change
            isSeeking = false
        }
    }

    fun fmSeekDown() {
        if (isSeeking) return
        scope.launch {
            isSeeking = true
            val newFreq = fmController.seekDown()
            currentFrequency = newFreq
            // Station info will be updated by LaunchedEffect on currentFrequency change
            isSeeking = false
        }
    }

    // FM Volume and Mute
    fun setFMVolume(vol: Float) {
        volume = vol
        fmController.setVolume(vol)
    }

    fun toggleFMMute() {
        isMuted = !isMuted
        fmController.setMute(isMuted)
    }

    // ============================================
    // 📻 FM APP DETECTION (Fallback)
    // ============================================
    val installedFMApps = remember { getInstalledFMRadioApps(context) }
    val hasFMApp = installedFMApps.isNotEmpty()
    var showFMAppDialog by remember { mutableStateOf(false) }


    // ============================================
    // 🌐 ONLINE RADIO STATE WITH BACKGROUND SERVICE
    // ============================================
    var onlineStations by remember { mutableStateOf<List<OnlineRadioStation>>(emptyList()) }
    var isLoadingOnline by remember { mutableStateOf(false) }
    var onlineSearchCountry by remember { mutableStateOf("Kenya") }
    var onlineSearchStation by remember { mutableStateOf("") }
    var currentOnlineStation by remember { mutableStateOf<OnlineRadioStation?>(null) }
    var isOnlinePlaying by remember { mutableStateOf(false) }
    var onlineFavorites by remember { mutableStateOf<List<OnlineRadioStation>>(emptyList()) }

    // Radio playback service connection
    var radioService by remember { mutableStateOf<RadioPlaybackService?>(null) }
    var isServiceBound by remember { mutableStateOf(false) }

    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as RadioPlaybackService.RadioBinder
                radioService = binder.getService()
                isServiceBound = true

                // Set callback for UI updates
                radioService?.playbackCallback = object : RadioPlaybackService.PlaybackCallback {
                    override fun onPlaybackStateChanged(playing: Boolean) {
                        isOnlinePlaying = playing
                    }

                    override fun onStationChanged(stationName: String, country: String) {
                        // Update UI if needed
                    }

                    override fun onError(message: String) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }

                // Sync current state
                isOnlinePlaying = radioService?.isPlaying() ?: false
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                radioService = null
                isServiceBound = false
            }
        }
    }

    // Bind to service on composition
    DisposableEffect(Unit) {
        val intent = Intent(context, RadioPlaybackService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        onDispose {
            if (isServiceBound) {
                radioService?.playbackCallback = null
                context.unbindService(serviceConnection)
                isServiceBound = false
            }
        }
    }

    // Load default stations on first launch
    LaunchedEffect(Unit) {
        isLoadingOnline = true
        onlineStations = fetchOnlineRadioStations(country = "Kenya")
        isLoadingOnline = false
    }

    // Search online stations
    fun searchOnlineStations() {
        scope.launch {
            isLoadingOnline = true
            onlineStations = fetchOnlineRadioStations(
                country = onlineSearchCountry.takeIf { it.isNotBlank() },
                stationName = onlineSearchStation.takeIf { it.isNotBlank() }
            )
            isLoadingOnline = false
        }
    }

    // Play online station using background service
    fun playOnlineStation(station: OnlineRadioStation) {
        currentOnlineStation = station

        val intent = Intent(context, RadioPlaybackService::class.java).apply {
            action = RadioPlaybackService.ACTION_PLAY
            putExtra(RadioPlaybackService.EXTRA_STATION_URL, station.urlResolved.ifBlank { station.url })
            putExtra(RadioPlaybackService.EXTRA_STATION_NAME, station.name)
            putExtra(RadioPlaybackService.EXTRA_STATION_COUNTRY, station.country)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    // Stop online playback
    fun stopOnlinePlayback() {
        val intent = Intent(context, RadioPlaybackService::class.java).apply {
            action = RadioPlaybackService.ACTION_STOP
        }
        context.startService(intent)
        isOnlinePlaying = false
    }

    // Toggle online favorite
    fun toggleOnlineFavorite(station: OnlineRadioStation) {
        val exists = onlineFavorites.any { it.stationuuid == station.stationuuid }
        onlineFavorites = if (exists) {
            onlineFavorites.filter { it.stationuuid != station.stationuuid }
        } else {
            onlineFavorites + station.copy(isFavorite = true)
        }
    }

    // Signal quality calculation - affected by headphone antenna quality
    val signalQuality = remember(signalStrength, headphonesConnected) {
        if (!headphonesConnected) {
            SignalQuality.NO_SIGNAL
        } else {
            when {
                signalStrength >= 80 -> SignalQuality.EXCELLENT
                signalStrength >= 60 -> SignalQuality.GOOD
                signalStrength >= 40 -> SignalQuality.FAIR
                signalStrength >= 20 -> SignalQuality.WEAK
                signalStrength > 0 -> SignalQuality.POOR
                else -> SignalQuality.NO_SIGNAL
            }
        }
    }

    // Auto-trigger fallback when headphones disconnected
    LaunchedEffect(headphonesConnected, isNetworkConnected) {
        if (!headphonesConnected && isPlaying && currentMode == RadioMode.HYBRID_AUTO) {
            if (isNetworkConnected) {
                // Switch to online fallback
                isFallbackActive = true
                rdsNowPlaying = "Switched to Online Radio (No Antenna)"
            } else {
                // No antenna and no internet - pause playback
                isPlaying = false
                rdsNowPlaying = "Connect headphones or internet"
            }
        } else if (headphonesConnected && isFallbackActive && isFMHardwareAvailable) {
            // Headphones reconnected - check if we should switch back to FM
            delay(2000) // Wait for signal to stabilize
            if (signalStrength > 40) {
                isFallbackActive = false
                rdsNowPlaying = "Returned to FM Radio"
            }
        }
    }

    // Network connectivity change handler
    LaunchedEffect(isNetworkConnected) {
        if (!isNetworkConnected && isFallbackActive) {
            // Lost internet while on online fallback
            if (headphonesConnected && isFMHardwareAvailable) {
                // Switch back to FM
                isFallbackActive = false
                rdsNowPlaying = "Returned to FM (Internet Lost)"
            } else {
                // No FM and no internet
                isPlaying = false
                rdsNowPlaying = "No connection available"
            }
        }
    }

    // Snackbar host for notifications
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar on headphone connection changes
    LaunchedEffect(headphonesConnected) {
        val message = if (headphonesConnected) {
            "🎧 Headphones connected - FM antenna ready"
        } else {
            "⚠️ Headphones disconnected - FM antenna lost"
        }
        snackbarHostState.showSnackbar(
            message = message,
            duration = SnackbarDuration.Short
        )
    }

    // Show snackbar on network changes
    var previousNetworkState by remember { mutableStateOf(isNetworkConnected) }
    LaunchedEffect(isNetworkConnected) {
        if (isNetworkConnected != previousNetworkState) {
            val message = if (isNetworkConnected) {
                "🌐 Network connected ($networkType) - Online radio available"
            } else {
                "📵 Network disconnected - Online radio unavailable"
            }
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            previousNetworkState = isNetworkConnected
        }
    }

    // Find current station info based on frequency
    fun updateStationInfo(freq: Float) {
        val station = simulatedStations.minByOrNull { abs(it.frequency - freq) }
        if (station != null && abs(station.frequency - freq) < 0.3f) {
            rdsStationName = station.name
            rdsNowPlaying = station.nowPlaying
            rdsProgramType = station.genre
            rdsArtist = station.artist
            // Better signal when tuned to actual station (only if headphones connected)
            signalStrength = if (headphonesConnected) {
                (70 + (30 * (1 - abs(station.frequency - freq) / 0.3f))).toInt().coerceIn(0, 100)
            } else {
                0
            }
        } else {
            rdsStationName = "FM ${String.format(Locale.US, "%.1f", freq)}"
            rdsNowPlaying = "Searching..."
            rdsProgramType = "Unknown"
            rdsArtist = ""
            // Weaker signal when not on a station
            signalStrength = if (headphonesConnected) {
                (20 + (Math.random() * 30)).toInt()
            } else {
                0
            }
        }
    }

    // Auto fallback logic
    LaunchedEffect(signalStrength, currentMode, isPlaying) {
        if (currentMode == RadioMode.HYBRID_AUTO && signalStrength < 25 && isPlaying && !isFallbackActive) {
            delay(3000) // Wait 3 seconds before fallback
            if (signalStrength < 25) {
                isFallbackActive = true
            }
        } else if (signalStrength >= 50 && isFallbackActive) {
            delay(2000) // Wait 2 seconds before returning to FM
            if (signalStrength >= 50) {
                isFallbackActive = false
            }
        }
    }

    // Signal fluctuation simulation when playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(2000)
                val fluctuation = (-8..8).random()
                val baseSignal = simulatedStations.minByOrNull { abs(it.frequency - currentFrequency) }?.let {
                    if (abs(it.frequency - currentFrequency) < 0.3f) 75 else 30
                } ?: 30
                signalStrength = (baseSignal + fluctuation).coerceIn(0, 100)
            }
        }
    }

    // Update station info when frequency changes
    LaunchedEffect(currentFrequency) {
        updateStationInfo(currentFrequency)
    }

    // Scanning function
    fun startScan() {
        if (isScanning) return
        scope.launch {
            isScanning = true
            scanProgress = 0f
            scannedStations = emptyList()
            val found = mutableListOf<FMStation>()

            var freq = 87.5f
            while (freq <= 108f) {
                scanProgress = (freq - 87.5f) / (108f - 87.5f)
                currentFrequency = freq

                val station = simulatedStations.find { abs(it.frequency - freq) < 0.2f }
                if (station != null) {
                    found.add(FMStation(
                        frequency = station.frequency,
                        name = station.name,
                        genre = when(station.genre) {
                            "Classical" -> RadioGenre.CLASSICAL
                            "Jazz" -> RadioGenre.JAZZ
                            "Rock" -> RadioGenre.ROCK
                            "Pop" -> RadioGenre.POP
                            "Hip Hop" -> RadioGenre.HIP_HOP
                            "Country" -> RadioGenre.COUNTRY
                            "Electronic" -> RadioGenre.ELECTRONIC
                            else -> null
                        },
                        lastSignalStrength = 75 + (Math.random() * 25).toInt(),
                        lastSignalQuality = SignalQuality.GOOD
                    ))
                }

                freq += 0.1f
                delay(50)
            }

            scannedStations = found
            isScanning = false
            scanProgress = 1f
            showScanResults = true
        }
    }

    // Seek functions
    fun seekUp() {
        if (isSeeking) return
        scope.launch {
            isSeeking = true
            var freq = currentFrequency + 0.1f
            while (freq <= 108f) {
                currentFrequency = freq
                delay(30)
                val station = simulatedStations.find { abs(it.frequency - freq) < 0.2f }
                if (station != null) {
                    currentFrequency = station.frequency
                    break
                }
                freq += 0.1f
            }
            if (freq > 108f) currentFrequency = 87.5f
            isSeeking = false
            updateStationInfo(currentFrequency)
        }
    }

    fun seekDown() {
        if (isSeeking) return
        scope.launch {
            isSeeking = true
            var freq = currentFrequency - 0.1f
            while (freq >= 87.5f) {
                currentFrequency = freq
                delay(30)
                val station = simulatedStations.find { abs(it.frequency - freq) < 0.2f }
                if (station != null) {
                    currentFrequency = station.frequency
                    break
                }
                freq -= 0.1f
            }
            if (freq < 87.5f) currentFrequency = 108f
            isSeeking = false
            updateStationInfo(currentFrequency)
        }
    }

    // Toggle favorite
    fun toggleFavorite() {
        val currentStation = FMStation(
            frequency = currentFrequency,
            name = rdsStationName,
            isFavorite = true
        )

        val exists = favorites.any { abs(it.frequency - currentFrequency) < 0.1f }
        favorites = if (exists) {
            favorites.filter { abs(it.frequency - currentFrequency) >= 0.1f }
        } else {
            favorites + currentStation
        }
    }

    val isFavorite = favorites.any { abs(it.frequency - currentFrequency) < 0.1f }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FMColors.bgDark)
    ) {
        // Animated background
        FMRadioBackground(
            isPlaying = isPlaying || isOnlinePlaying,
            signalStrength = signalStrength,
            isFallbackActive = isFallbackActive || selectedTab == RadioTabType.ONLINE_RADIO
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar - Simplified
            RadioTopBar(
                onBack = onBack,
                selectedTab = selectedTab,
                onSettingsClick = { showSettings = true }
            )

            // Tab Row
            RadioTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            // Tab Content
            when (selectedTab) {
                RadioTabType.OFFLINE_FM -> {
                    OfflineFMTab(
                        currentFrequency = currentFrequency,
                        signalStrength = signalStrength,
                        signalQuality = signalQuality,
                        isPlaying = isPlaying,
                        isStereo = isStereo,
                        isMuted = isMuted,
                        volume = volume,
                        isScanning = isScanning,
                        isSeeking = isSeeking,
                        scanProgress = scanProgress,
                        rdsStationName = rdsStationName,
                        rdsNowPlaying = rdsNowPlaying,
                        rdsArtist = rdsArtist,
                        rdsProgramType = rdsProgramType,
                        headphonesConnected = headphonesConnected,
                        isFMHardwareAvailable = isFMHardwareAvailable,
                        hasFMApp = hasFMApp,
                        installedFMApps = installedFMApps,
                        fmError = fmError,
                        presets = presets,
                        scannedStations = scannedStations,
                        isFavorite = isFavorite,
                        onFrequencyChange = { tuneFM(it) },
                        onPlayPause = { toggleFMPlayback() },
                        onMuteToggle = { toggleFMMute() },
                        onVolumeChange = { setFMVolume(it) },
                        onStereoToggle = { isStereo = !isStereo },
                        onSeekUp = { fmSeekUp() },
                        onSeekDown = { fmSeekDown() },
                        onStartScan = { startScan() },
                        onCancelScan = { isScanning = false },
                        onPresetClick = { preset ->
                            tuneFM(preset.frequency)
                            selectedPreset = preset.presetSlot
                        },
                        onToggleFavorite = { toggleFavorite() },
                        onHardwareStatusClick = { showHardwareStatus = true },
                        onLaunchExternalFM = { launchFMRadioApp(context, currentFrequency) }
                    )
                }
                RadioTabType.ONLINE_RADIO -> {
                    OnlineRadioTab(
                        stations = onlineStations,
                        isLoading = isLoadingOnline,
                        isNetworkConnected = isNetworkConnected,
                        searchCountry = onlineSearchCountry,
                        searchStation = onlineSearchStation,
                        currentStation = currentOnlineStation,
                        isPlaying = isOnlinePlaying,
                        favorites = onlineFavorites,
                        onSearchCountryChange = { onlineSearchCountry = it },
                        onSearchStationChange = { onlineSearchStation = it },
                        onSearch = { searchOnlineStations() },
                        onPlayStation = { playOnlineStation(it) },
                        onStopPlayback = { stopOnlinePlayback() },
                        onToggleFavorite = { toggleOnlineFavorite(it) }
                    )
                }
                RadioTabType.FAVOURITES -> {
                    FavouritesTab(
                        fmFavorites = favorites,
                        onlineFavorites = onlineFavorites,
                        currentFMFrequency = currentFrequency,
                        currentOnlineStation = currentOnlineStation,
                        isFMPlaying = isPlaying,
                        isOnlinePlaying = isOnlinePlaying,
                        onFMStationClick = { station ->
                            selectedTab = RadioTabType.OFFLINE_FM
                            currentFrequency = station.frequency
                            updateStationInfo(station.frequency)
                        },
                        onOnlineStationClick = { station ->
                            selectedTab = RadioTabType.ONLINE_RADIO
                            playOnlineStation(station)
                        },
                        onRemoveFMFavorite = { station ->
                            favorites = favorites.filter { abs(it.frequency - station.frequency) > 0.1f }
                        },
                        onRemoveOnlineFavorite = { station ->
                            onlineFavorites = onlineFavorites.filter { it.stationuuid != station.stationuuid }
                        }
                    )
                }
            }
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        )

        // Now Playing Mini Player (when something is playing)
        if (isOnlinePlaying && currentOnlineStation != null) {
            OnlineRadioMiniPlayer(
                station = currentOnlineStation!!,
                isPlaying = isOnlinePlaying,
                onPlayPause = {
                    if (isOnlinePlaying) stopOnlinePlayback()
                    else currentOnlineStation?.let { playOnlineStation(it) }
                },
                onStop = { stopOnlinePlayback() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    // Hardware Status Dialog
    if (showHardwareStatus) {
        HardwareStatusDialog(
            headphonesConnected = headphonesConnected,
            isFMHardwareAvailable = isFMHardwareAvailable,
            fmChipType = fmChipType,
            fmCapabilities = fmHardwareState.value.capabilities,
            isNetworkConnected = isNetworkConnected,
            networkType = networkType,
            networkSignalStrength = networkState.value.signalStrength,
            fmStatus = fmStatus,
            onDismiss = { showHardwareStatus = false },
            isManualOverride = fmHardwareState.value.isManualOverride,
            detectionMethod = fmHardwareState.value.detectionMethod
        )
    }

    // Settings Sheet
    if (showSettings) {
        FMSettingsSheet(
            currentMode = currentMode,
            headphonesConnected = headphonesConnected,
            onModeChange = { currentMode = it },
            onDismiss = { showSettings = false }
        )
    }
}

// ============================================
// 📻 RADIO TOP BAR
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadioTopBar(
    onBack: () -> Unit,
    selectedTab: RadioTabType,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📻", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Super Radio",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FMColors.textPrimary
                    )
                    Text(
                        text = when (selectedTab) {
                            RadioTabType.OFFLINE_FM -> "Offline FM"
                            RadioTabType.ONLINE_RADIO -> "Online Stations"
                            RadioTabType.FAVOURITES -> "Favourites"
                        },
                        fontSize = 12.sp,
                        color = FMColors.textSecondary
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = FMColors.textPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = FMColors.textPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

// ============================================
// 📻 RADIO TAB ROW
// ============================================

@Composable
private fun RadioTabRow(
    selectedTab: RadioTabType,
    onTabSelected: (RadioTabType) -> Unit
) {
    val tabs = listOf(
        RadioTabType.OFFLINE_FM to "📻 FM Radio",
        RadioTabType.ONLINE_RADIO to "🌐 Online",
        RadioTabType.FAVOURITES to "⭐ Favourites"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(FMColors.surface.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEach { (tab, label) ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) FMColors.fmOrange.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) FMColors.fmOrange else FMColors.textSecondary
                )
            }
        }
    }
}

// ============================================
// 📻 OFFLINE FM TAB
// ============================================

@Composable
private fun OfflineFMTab(
    currentFrequency: Float,
    signalStrength: Int,
    signalQuality: SignalQuality,
    isPlaying: Boolean,
    isStereo: Boolean,
    isMuted: Boolean,
    volume: Float,
    isScanning: Boolean,
    isSeeking: Boolean,
    scanProgress: Float,
    rdsStationName: String,
    rdsNowPlaying: String,
    rdsArtist: String,
    rdsProgramType: String,
    headphonesConnected: Boolean,
    isFMHardwareAvailable: Boolean,
    hasFMApp: Boolean,
    installedFMApps: List<FMRadioApp>,
    fmError: String?,
    presets: List<FMStation>,
    scannedStations: List<FMStation>,
    isFavorite: Boolean,
    onFrequencyChange: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onMuteToggle: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onStereoToggle: () -> Unit,
    onSeekUp: () -> Unit,
    onSeekDown: () -> Unit,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onPresetClick: (FMStation) -> Unit,
    onToggleFavorite: () -> Unit,
    onHardwareStatusClick: () -> Unit,
    onLaunchExternalFM: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // FM Status and Play Card
        item {
            FMPlayCard(
                isPlaying = isPlaying,
                headphonesConnected = headphonesConnected,
                hasFMApp = hasFMApp,
                installedFMApps = installedFMApps,
                fmError = fmError,
                currentFrequency = currentFrequency,
                rdsStationName = rdsStationName,
                onPlayPause = onPlayPause,
                onLaunchExternalFM = onLaunchExternalFM,
                onHardwareStatusClick = onHardwareStatusClick
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Frequency Search
        item {
            FrequencySearchCard(
                currentFrequency = currentFrequency,
                onFrequencyChange = onFrequencyChange
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // FM Dial Display
        item {
            FMDialDisplay(
                frequency = currentFrequency,
                signalStrength = signalStrength,
                signalQuality = signalQuality,
                isPlaying = isPlaying,
                isStereo = isStereo,
                isFallbackActive = false,
                rdsStationName = rdsStationName,
                isSeeking = isSeeking,
                onFrequencyChange = onFrequencyChange
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // RDS Info
        item {
            RDSInfoCard(
                stationName = rdsStationName,
                nowPlaying = if (rdsArtist.isNotEmpty()) "$rdsNowPlaying - $rdsArtist" else rdsNowPlaying,
                programType = rdsProgramType,
                isFallbackActive = false
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Signal Meter
        item {
            SignalStrengthMeter(
                strength = signalStrength,
                quality = signalQuality,
                isFallbackActive = false
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Scanning Progress
        if (isScanning) {
            item {
                ScanningProgressCard(
                    progress = scanProgress,
                    currentFrequency = currentFrequency,
                    stationsFound = scannedStations.size,
                    onCancel = onCancelScan
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Playback Controls
        item {
            PlaybackControls(
                isPlaying = isPlaying,
                isMuted = isMuted,
                volume = volume,
                isStereo = isStereo,
                isScanning = isScanning,
                isFallbackActive = false,
                isSeeking = isSeeking,
                onPlayPause = onPlayPause,
                onMuteToggle = onMuteToggle,
                onVolumeChange = onVolumeChange,
                onStereoToggle = onStereoToggle,
                onScanStart = onStartScan,
                onSeekUp = onSeekUp,
                onSeekDown = onSeekDown
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Frequency Tuner
        item {
            FrequencyTuner(
                frequency = currentFrequency,
                onFrequencyChange = onFrequencyChange
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Preset Buttons
        item {
            PresetButtonsRow(
                presets = presets,
                currentFrequency = currentFrequency,
                selectedPreset = null,
                onPresetClick = onPresetClick,
                onPresetLongClick = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Scanned Stations
        if (scannedStations.isNotEmpty()) {
            item {
                SectionHeader(title = "📡 Scanned Stations", count = scannedStations.size)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(scannedStations) { station ->
                ScannedStationItem(
                    station = station,
                    isSelected = abs(station.frequency - currentFrequency) < 0.1f,
                    onClick = { onFrequencyChange(station.frequency) }
                )
            }
        }
    }
}

// ============================================
// 🔍 FREQUENCY SEARCH CARD
// ============================================

@Composable
private fun FrequencySearchCard(
    currentFrequency: Float,
    onFrequencyChange: (Float) -> Unit
) {
    var searchText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FMColors.surface.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, FMColors.glassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🔍 Search by Frequency",
                color = FMColors.textPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("e.g. 98.5", color = FMColors.textTertiary) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            searchText.toFloatOrNull()?.let { freq ->
                                if (freq in 87.5f..108f) onFrequencyChange(freq)
                            }
                        }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FMColors.textPrimary,
                        unfocusedTextColor = FMColors.textSecondary,
                        focusedBorderColor = FMColors.fmOrange,
                        unfocusedBorderColor = FMColors.glassBorder
                    )
                )
                Button(
                    onClick = {
                        searchText.toFloatOrNull()?.let { freq ->
                            if (freq in 87.5f..108f) onFrequencyChange(freq)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FMColors.fmOrange)
                ) {
                    Text("Tune")
                }
            }
            Text(
                text = "Enter frequency between 87.5 - 108.0 MHz",
                color = FMColors.textTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ============================================
// 📻 FM PLAY CARD - INTEGRATED PLAYER
// ============================================

@Composable
private fun FMPlayCard(
    isPlaying: Boolean,
    headphonesConnected: Boolean,
    hasFMApp: Boolean,
    installedFMApps: List<FMRadioApp>,
    fmError: String?,
    currentFrequency: Float,
    rdsStationName: String,
    onPlayPause: () -> Unit,
    onLaunchExternalFM: () -> Unit,
    onHardwareStatusClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "playing")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying)
                FMColors.fmOrange.copy(alpha = 0.15f)
            else FMColors.surface.copy(alpha = 0.8f)
        ),
        border = BorderStroke(
            1.dp,
            if (isPlaying) FMColors.fmOrange.copy(alpha = 0.5f) else FMColors.glassBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Frequency Display
            Text(
                text = String.format(Locale.US, "%.1f", currentFrequency),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPlaying) FMColors.fmOrange else FMColors.textPrimary
            )
            Text(
                text = "MHz",
                fontSize = 16.sp,
                color = FMColors.textSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Station Name
            Text(
                text = rdsStationName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = FMColors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Headphone Status
            Row(
                modifier = Modifier
                    .background(
                        if (headphonesConnected) FMColors.fmGreen.copy(alpha = 0.1f)
                        else FMColors.fmAmber.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (headphonesConnected) "🎧" else "⚠️",
                    fontSize = 14.sp
                )
                Text(
                    text = if (headphonesConnected) "Antenna Ready" else "Connect Wired Headphones",
                    fontSize = 12.sp,
                    color = if (headphonesConnected) FMColors.fmGreen else FMColors.fmAmber
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error message if any
            fmError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = FMColors.fmRed.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = FMColors.fmRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = error,
                            fontSize = 12.sp,
                            color = FMColors.fmRed
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Info about FM playback
            if (hasFMApp) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = FMColors.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = FMColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Opens ${installedFMApps.firstOrNull()?.appName ?: "FM Radio"} app",
                            fontSize = 11.sp,
                            color = FMColors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Open FM Radio App Button
            Button(
                onClick = onPlayPause,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = headphonesConnected && hasFMApp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FMColors.fmOrange,
                    disabledContainerColor = FMColors.fmOrange.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Radio,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (!headphonesConnected) "Connect Headphones First"
                           else if (!hasFMApp) "No FM App Found"
                           else "Open FM Radio",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Alternative: Online Radio suggestion
            if (!hasFMApp) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "💡 Try the Online Radio tab for internet radio stations",
                    fontSize = 12.sp,
                    color = FMColors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Info button
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onHardwareStatusClick) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = FMColors.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Hardware Status",
                    color = FMColors.textTertiary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ============================================
// 📻 FM APP STATUS CARD
// ============================================

@Composable
private fun FMAppStatusCard(
    hasFMApp: Boolean,
    installedFMApps: List<FMRadioApp>,
    headphonesConnected: Boolean,
    onLaunchFM: () -> Unit,
    onHardwareStatusClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasFMApp) FMColors.fmGreen.copy(alpha = 0.1f)
                            else FMColors.fmAmber.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, if (hasFMApp) FMColors.fmGreen.copy(alpha = 0.3f) else FMColors.fmAmber.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (hasFMApp) FMColors.fmGreen.copy(alpha = 0.2f)
                                else FMColors.fmAmber.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hasFMApp) "📻" else "⚠️",
                            fontSize = 24.sp
                        )
                    }
                    Column {
                        Text(
                            text = if (hasFMApp) "FM Radio Available" else "No FM App Found",
                            color = FMColors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (hasFMApp) {
                                "${installedFMApps.size} FM app(s) detected"
                            } else {
                                "Your device may not have FM radio"
                            },
                            color = FMColors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = onHardwareStatusClick) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = FMColors.textSecondary
                    )
                }
            }

            if (hasFMApp) {
                Spacer(modifier = Modifier.height(12.dp))

                // Headphone status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FMColors.glassWhite, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (headphonesConnected) "🎧" else "⚠️",
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (headphonesConnected)
                            "Headphones connected (FM Antenna ready)"
                        else
                            "Connect wired headphones for FM antenna",
                        color = if (headphonesConnected) FMColors.fmGreen else FMColors.fmAmber,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Launch FM Button
                Button(
                    onClick = onLaunchFM,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FMColors.fmOrange
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open FM Radio App",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Show available FM apps
                if (installedFMApps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Available: ${installedFMApps.joinToString(", ") { it.appName }}",
                        color = FMColors.textTertiary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = FMColors.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "💡 Tips:",
                            color = FMColors.fmAmber,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Some phones have FM hardware but no app\n" +
                                   "• Try installing an FM radio app from Play Store\n" +
                                   "• Use Online Radio tab for internet radio",
                            color = FMColors.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// 📻 FM STATUS CARD
// ============================================

@Composable
private fun FMStatusCard(
    headphonesConnected: Boolean,
    isFMHardwareAvailable: Boolean,
    signalQuality: SignalQuality,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FMColors.surface.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, FMColors.glassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatusChip(
                icon = if (isFMHardwareAvailable) "📻" else "❌",
                label = if (isFMHardwareAvailable) "FM Ready" else "No FM Chip",
                isActive = isFMHardwareAvailable,
                color = if (isFMHardwareAvailable) FMColors.fmGreen else FMColors.fmRed
            )
            StatusChip(
                icon = if (headphonesConnected) "🎧" else "⚠️",
                label = if (headphonesConnected) "Antenna OK" else "No Antenna",
                isActive = headphonesConnected,
                color = if (headphonesConnected) FMColors.fmGreen else FMColors.fmAmber
            )
            StatusChip(
                icon = signalQuality.icon,
                label = signalQuality.displayName,
                isActive = signalQuality != SignalQuality.NO_SIGNAL,
                color = Color(signalQuality.color)
            )
        }
    }
}

@Composable
private fun StatusChip(
    icon: String,
    label: String,
    isActive: Boolean,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isActive) color else FMColors.textTertiary
        )
    }
}

// ============================================
// 🌐 ONLINE RADIO TAB
// ============================================

@Composable
private fun OnlineRadioTab(
    stations: List<OnlineRadioStation>,
    isLoading: Boolean,
    isNetworkConnected: Boolean,
    searchCountry: String,
    searchStation: String,
    currentStation: OnlineRadioStation?,
    isPlaying: Boolean,
    favorites: List<OnlineRadioStation>,
    onSearchCountryChange: (String) -> Unit,
    onSearchStationChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPlayStation: (OnlineRadioStation) -> Unit,
    onStopPlayback: () -> Unit,
    onToggleFavorite: (OnlineRadioStation) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search Card
        OnlineSearchCard(
            country = searchCountry,
            stationName = searchStation,
            onCountryChange = onSearchCountryChange,
            onStationChange = onSearchStationChange,
            onSearch = onSearch,
            isLoading = isLoading,
            isNetworkConnected = isNetworkConnected
        )

        // Check network connectivity first
        if (!isNetworkConnected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📵", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Internet Connection",
                        color = FMColors.fmRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please check your internet connection\nto browse online radio stations",
                        color = FMColors.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = FMColors.fmAmber.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = FMColors.fmAmber
                            )
                            Text(
                                text = "Connect to WiFi or Mobile Data to search stations",
                                color = FMColors.fmAmber,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = FMColors.fmOrange)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading stations...", color = FMColors.textSecondary)
                }
            }
        } else if (stations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📻", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No stations found", color = FMColors.textSecondary)
                    Text("Try a different search", color = FMColors.textTertiary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                item {
                    Text(
                        text = "${stations.size} stations found",
                        modifier = Modifier.padding(16.dp),
                        color = FMColors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                items(stations) { station ->
                    OnlineStationItem(
                        station = station,
                        isCurrentlyPlaying = currentStation?.stationuuid == station.stationuuid && isPlaying,
                        isFavorite = favorites.any { it.stationuuid == station.stationuuid },
                        onPlay = { onPlayStation(station) },
                        onStop = onStopPlayback,
                        onToggleFavorite = { onToggleFavorite(station) }
                    )
                }
            }
        }
    }
}

// ============================================
// 🔍 ONLINE SEARCH CARD
// ============================================

@Composable
private fun OnlineSearchCard(
    country: String,
    stationName: String,
    onCountryChange: (String) -> Unit,
    onStationChange: (String) -> Unit,
    onSearch: () -> Unit,
    isLoading: Boolean,
    isNetworkConnected: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = FMColors.surface.copy(alpha = 0.8f)),
        border = BorderStroke(1.dp, if (isNetworkConnected) FMColors.glassBorder else FMColors.fmRed.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔍 Search Online Stations",
                    color = FMColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                // Network status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isNetworkConnected) FMColors.fmGreen else FMColors.fmRed,
                                CircleShape
                            )
                    )
                    Text(
                        text = if (isNetworkConnected) "Online" else "Offline",
                        color = if (isNetworkConnected) FMColors.fmGreen else FMColors.fmRed,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Country Input
            OutlinedTextField(
                value = country,
                onValueChange = onCountryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Country", color = FMColors.textSecondary) },
                placeholder = { Text("e.g. Kenya, USA, UK", color = FMColors.textTertiary) },
                leadingIcon = {
                    Icon(Icons.Default.Public, null, tint = FMColors.fmOrange)
                },
                singleLine = true,
                enabled = isNetworkConnected,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FMColors.textPrimary,
                    unfocusedTextColor = FMColors.textSecondary,
                    focusedBorderColor = FMColors.fmOrange,
                    unfocusedBorderColor = FMColors.glassBorder,
                    focusedLabelColor = FMColors.fmOrange,
                    disabledTextColor = FMColors.textTertiary,
                    disabledBorderColor = FMColors.glassBorder.copy(alpha = 0.5f),
                    disabledLabelColor = FMColors.textTertiary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Station Name Input
            OutlinedTextField(
                value = stationName,
                onValueChange = onStationChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Station Name (optional)", color = FMColors.textSecondary) },
                placeholder = { Text("e.g. BBC, Kiss FM", color = FMColors.textTertiary) },
                leadingIcon = {
                    Icon(Icons.Default.Radio, null, tint = FMColors.fmOrange)
                },
                singleLine = true,
                enabled = isNetworkConnected,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FMColors.textPrimary,
                    unfocusedTextColor = FMColors.textSecondary,
                    focusedBorderColor = FMColors.fmOrange,
                    unfocusedBorderColor = FMColors.glassBorder,
                    focusedLabelColor = FMColors.fmOrange
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Button
            Button(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && isNetworkConnected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FMColors.fmOrange,
                    disabledContainerColor = FMColors.fmOrange.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else if (!isNetworkConnected) {
                    Icon(Icons.Default.WifiOff, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("No Internet")
                } else {
                    Icon(Icons.Default.Search, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search Stations")
                }
            }
        }
    }
}

// ============================================
// 📻 ONLINE STATION ITEM
// ============================================

@Composable
private fun OnlineStationItem(
    station: OnlineRadioStation,
    isCurrentlyPlaying: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "playing")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { if (isCurrentlyPlaying) onStop() else onPlay() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying)
                FMColors.fmOrange.copy(alpha = 0.15f)
            else FMColors.surface.copy(alpha = 0.6f)
        ),
        border = if (isCurrentlyPlaying)
            BorderStroke(1.dp, FMColors.fmOrange.copy(alpha = pulseAlpha))
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Stop Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isCurrentlyPlaying) FMColors.fmOrange else FMColors.fmOrange.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isCurrentlyPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isCurrentlyPlaying) Color.White else FMColors.fmOrange,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Station Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    color = FMColors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (station.country.isNotBlank()) {
                        Text(
                            text = "🌍 ${station.country}",
                            color = FMColors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    if (station.bitrate > 0) {
                        Text(
                            text = "${station.bitrate}kbps",
                            color = FMColors.textTertiary,
                            fontSize = 10.sp
                        )
                    }
                    if (station.codec.isNotBlank()) {
                        Text(
                            text = station.codec.uppercase(),
                            color = FMColors.fmCyan,
                            fontSize = 10.sp
                        )
                    }
                }
                if (station.tags.isNotBlank()) {
                    Text(
                        text = station.tags.split(",").take(3).joinToString(" • "),
                        color = FMColors.textTertiary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Favorite Button
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) FMColors.fmRed else FMColors.textTertiary
                )
            }
        }
    }
}

// ============================================
// ⭐ FAVOURITES TAB
// ============================================

@Composable
private fun FavouritesTab(
    fmFavorites: List<FMStation>,
    onlineFavorites: List<OnlineRadioStation>,
    currentFMFrequency: Float,
    currentOnlineStation: OnlineRadioStation?,
    isFMPlaying: Boolean,
    isOnlinePlaying: Boolean,
    onFMStationClick: (FMStation) -> Unit,
    onOnlineStationClick: (OnlineRadioStation) -> Unit,
    onRemoveFMFavorite: (FMStation) -> Unit,
    onRemoveOnlineFavorite: (OnlineRadioStation) -> Unit
) {
    val totalFavorites = fmFavorites.size + onlineFavorites.size

    if (totalFavorites == 0) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⭐", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Favourites Yet",
                    color = FMColors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
                Text(
                    text = "Add stations from FM or Online tabs",
                    color = FMColors.textSecondary,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // FM Favorites
            if (fmFavorites.isNotEmpty()) {
                item {
                    SectionHeader(title = "📻 FM Stations", count = fmFavorites.size)
                }
                items(fmFavorites) { station ->
                    FMFavoriteItem(
                        station = station,
                        isPlaying = isFMPlaying && abs(station.frequency - currentFMFrequency) < 0.1f,
                        onClick = { onFMStationClick(station) },
                        onRemove = { onRemoveFMFavorite(station) }
                    )
                }
            }

            // Online Favorites
            if (onlineFavorites.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(title = "🌐 Online Stations", count = onlineFavorites.size)
                }
                items(onlineFavorites) { station ->
                    OnlineFavoriteItem(
                        station = station,
                        isPlaying = isOnlinePlaying && currentOnlineStation?.stationuuid == station.stationuuid,
                        onClick = { onOnlineStationClick(station) },
                        onRemove = { onRemoveOnlineFavorite(station) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FMFavoriteItem(
    station: FMStation,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) FMColors.fmOrange.copy(alpha = 0.15f)
            else FMColors.surface.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(FMColors.fmOrange.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("📻", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    color = FMColors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${station.frequency} MHz",
                    color = FMColors.fmOrange,
                    fontSize = 12.sp
                )
            }
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .background(FMColors.fmGreen, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("PLAYING", color = Color.White, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, null, tint = FMColors.fmRed)
            }
        }
    }
}

@Composable
private fun OnlineFavoriteItem(
    station: OnlineRadioStation,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) FMColors.fmBlue.copy(alpha = 0.15f)
            else FMColors.surface.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(FMColors.fmBlue.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🌐", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    color = FMColors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = station.country,
                    color = FMColors.textSecondary,
                    fontSize = 12.sp
                )
            }
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .background(FMColors.fmGreen, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("PLAYING", color = Color.White, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, null, tint = FMColors.fmRed)
            }
        }
    }
}

// ============================================
// 🎵 ONLINE RADIO MINI PLAYER
// ============================================

@Composable
private fun OnlineRadioMiniPlayer(
    station: OnlineRadioStation,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "mini")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = FMColors.fmOrange.copy(alpha = glowAlpha),
                spotColor = FMColors.fmOrange.copy(alpha = glowAlpha)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = FMColors.card),
        border = BorderStroke(1.dp, FMColors.fmOrange.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playing Animation
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(FMColors.fmOrange.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    // Animated bars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        repeat(3) { i ->
                            val height by transition.animateFloat(
                                initialValue = 8f,
                                targetValue = 20f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(300 + i * 100),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "bar$i"
                            )
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(height.dp)
                                    .background(FMColors.fmOrange, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                } else {
                    Icon(
                        Icons.Default.Radio,
                        null,
                        tint = FMColors.fmOrange,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    color = FMColors.textPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isPlaying) "Now Playing" else "Paused",
                    color = if (isPlaying) FMColors.fmGreen else FMColors.textSecondary,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null,
                    tint = FMColors.fmOrange,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = onStop) {
                Icon(
                    Icons.Default.Stop,
                    null,
                    tint = FMColors.fmRed,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ============================================
// 📱 HARDWARE STATUS DIALOG
// ============================================

@Composable
fun HardwareStatusDialog(
    headphonesConnected: Boolean,
    isFMHardwareAvailable: Boolean,
    fmChipType: String,
    fmCapabilities: List<String>,
    isNetworkConnected: Boolean,
    networkType: String,
    networkSignalStrength: Int,
    fmStatus: String,
    onDismiss: () -> Unit,
    onManualFMToggle: ((Boolean) -> Unit)? = null,
    isManualOverride: Boolean = false,
    detectionMethod: String = "Auto"
) {
    val context = LocalContext.current
    var manualFMEnabled by remember {
        mutableStateOf(
            context.getSharedPreferences("fm_radio_prefs", Context.MODE_PRIVATE)
                .getBoolean("manual_fm_enabled", false)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FMColors.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = FMColors.fmOrange
                )
                Text(
                    "Hardware Status",
                    color = FMColors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // FM Hardware Status
                HardwareStatusSection(
                    title = "📻 FM Radio Hardware",
                    items = listOf(
                        HardwareStatusItem(
                            label = "FM Chip",
                            value = if (isFMHardwareAvailable) fmChipType else "Not Detected",
                            isOk = isFMHardwareAvailable
                        ),
                        HardwareStatusItem(
                            label = "Detection Method",
                            value = if (isManualOverride) "Manual Override" else detectionMethod,
                            isOk = isFMHardwareAvailable
                        ),
                        HardwareStatusItem(
                            label = "Status",
                            value = fmStatus,
                            isOk = isFMHardwareAvailable && headphonesConnected
                        )
                    ) + fmCapabilities.map { cap ->
                        HardwareStatusItem(label = "Info", value = cap, isOk = !cap.contains("failed", ignoreCase = true))
                    }
                )

                // Manual FM Mode Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (manualFMEnabled) FMColors.fmGreen.copy(alpha = 0.1f)
                                        else FMColors.fmAmber.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Manual FM Mode",
                                    color = FMColors.textPrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (manualFMEnabled)
                                        "FM enabled manually - hardware may work"
                                    else
                                        "Enable if your device has FM but wasn't detected",
                                    color = FMColors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = manualFMEnabled,
                                onCheckedChange = { enabled ->
                                    manualFMEnabled = enabled
                                    setManualFMMode(context, enabled)
                                    onManualFMToggle?.invoke(enabled)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = FMColors.fmGreen,
                                    checkedTrackColor = FMColors.fmGreen.copy(alpha = 0.5f),
                                    uncheckedThumbColor = FMColors.textSecondary,
                                    uncheckedTrackColor = FMColors.glassWhite
                                )
                            )
                        }

                        if (!isFMHardwareAvailable && !manualFMEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "💡 Tip: If you have an FM radio app that works on your phone, " +
                                       "enable this to use FM features. Your phone likely has FM hardware " +
                                       "that our auto-detection couldn't find.",
                                color = FMColors.fmAmber,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = FMColors.glassBorder)

                // Antenna Status
                HardwareStatusSection(
                    title = "🎧 Antenna (Headphones)",
                    items = listOf(
                        HardwareStatusItem(
                            label = "Wired Headphones",
                            value = if (headphonesConnected) "Connected" else "Not Connected",
                            isOk = headphonesConnected
                        ),
                        HardwareStatusItem(
                            label = "Antenna Quality",
                            value = if (headphonesConnected) "Good" else "No Antenna",
                            isOk = headphonesConnected
                        )
                    )
                )

                if (!headphonesConnected) {
                    Text(
                        text = "⚠️ Wired headphones are required as FM antenna. " +
                               "Bluetooth headphones won't work for FM radio.",
                        color = FMColors.fmAmber,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                HorizontalDivider(color = FMColors.glassBorder)

                // Network Status
                HardwareStatusSection(
                    title = "🌐 Network (Online Fallback)",
                    items = listOf(
                        HardwareStatusItem(
                            label = "Internet",
                            value = if (isNetworkConnected) "Connected" else "Disconnected",
                            isOk = isNetworkConnected
                        ),
                        HardwareStatusItem(
                            label = "Connection Type",
                            value = networkType,
                            isOk = isNetworkConnected
                        ),
                        HardwareStatusItem(
                            label = "Signal Strength",
                            value = "$networkSignalStrength%",
                            isOk = networkSignalStrength > 50
                        )
                    )
                )

                Text(
                    text = "ℹ️ When FM signal is weak or unavailable, the app can " +
                           "automatically switch to online radio streams.",
                    color = FMColors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = FMColors.fmOrange)
            }
        }
    )
}

data class HardwareStatusItem(
    val label: String,
    val value: String,
    val isOk: Boolean
)

@Composable
fun HardwareStatusSection(
    title: String,
    items: List<HardwareStatusItem>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = FMColors.textPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )

        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.label,
                    color = FMColors.textSecondary,
                    fontSize = 12.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (item.isOk) FMColors.fmGreen else FMColors.fmRed,
                                CircleShape
                            )
                    )
                    Text(
                        text = item.value,
                        color = if (item.isOk) FMColors.fmGreen else FMColors.fmRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun FMRadioBackground(
    isPlaying: Boolean,
    signalStrength: Int,
    isFallbackActive: Boolean
) {
    val transition = rememberInfiniteTransition(label = "bg")

    val pulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 1500 else 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val signalWave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    val baseColor = if (isFallbackActive) FMColors.fmBlue else FMColors.fmOrange
    val signalIntensity = signalStrength / 100f

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Base gradient
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    FMColors.bgDark,
                    Color(0xFF0D0815),
                    FMColors.bgMid,
                    FMColors.bgDark
                )
            )
        )

        if (isPlaying) {
            // Radio wave circles
            for (i in 0..3) {
                val waveOffset = sin(Math.toRadians((signalWave + i * 90).toDouble())).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            baseColor.copy(alpha = 0.05f * pulse * signalIntensity * (1f - i * 0.2f)),
                            Color.Transparent
                        )
                    ),
                    radius = (200f + i * 80f + waveOffset * 20f) * pulse,
                    center = Offset(size.width * 0.5f, size.height * 0.3f)
                )
            }

            // Signal bars emanating effect
            if (!isFallbackActive) {
                for (i in 0..5) {
                    val angle = (signalWave + i * 60) * PI / 180
                    val length = 100f + signalIntensity * 50f
                    drawLine(
                        color = FMColors.fmOrange.copy(alpha = 0.1f * pulse),
                        start = Offset(
                            size.width * 0.5f,
                            size.height * 0.3f
                        ),
                        end = Offset(
                            size.width * 0.5f + (cos(angle) * length).toFloat(),
                            size.height * 0.3f + (sin(angle) * length).toFloat()
                        ),
                        strokeWidth = 2f
                    )
                }
            }
        }
    }
}

// ============================================
// 🎛️ TOP BAR
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FMTopBar(
    currentMode: RadioMode,
    isFallbackActive: Boolean,
    headphonesConnected: Boolean,
    onBack: () -> Unit,
    onModeChange: (RadioMode) -> Unit,
    onSettingsClick: () -> Unit,
    onFavoritesClick: () -> Unit = {},
    onEQClick: () -> Unit = {},
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "📻",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "FM Radio",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FMColors.textPrimary
                    )
                    Text(
                        text = if (isFallbackActive) "🌐 Online Mode" else currentMode.displayName,
                        fontSize = 12.sp,
                        color = if (isFallbackActive) FMColors.fmBlue else FMColors.textSecondary
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = FMColors.textPrimary
                )
            }
        },
        actions = {
            // Favorite toggle
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) FMColors.fmRed else FMColors.textPrimary
                )
            }

            // EQ Button
            IconButton(onClick = onEQClick) {
                Icon(
                    Icons.Default.Equalizer,
                    contentDescription = "Equalizer",
                    tint = FMColors.textPrimary
                )
            }

            // Headphone indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (headphonesConnected) FMColors.fmGreen.copy(alpha = 0.2f)
                        else FMColors.fmRed.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (headphonesConnected) Icons.Default.Headphones else Icons.Default.HeadsetOff,
                    contentDescription = null,
                    tint = if (headphonesConnected) FMColors.fmGreen else FMColors.fmRed,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = FMColors.textPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

// ============================================
// 📊 MODE INDICATOR CARD
// ============================================

@Composable
private fun ModeIndicatorCard(
    currentMode: RadioMode,
    isFallbackActive: Boolean,
    headphonesConnected: Boolean,
    signalQuality: SignalQuality,
    isNetworkConnected: Boolean = false,
    networkType: String = "None",
    isFMHardwareAvailable: Boolean = false,
    onHardwareStatusClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onHardwareStatusClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FMColors.surface.copy(alpha = 0.7f)
        ),
        border = BorderStroke(1.dp, FMColors.glassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Mode
                ModeChip(
                    icon = currentMode.icon,
                    label = if (isFallbackActive) "Online" else currentMode.displayName,
                    isActive = true,
                    color = if (isFallbackActive) FMColors.fmBlue else FMColors.fmOrange
                )

                // Antenna
                ModeChip(
                    icon = if (headphonesConnected) "🎧" else "⚠️",
                    label = if (headphonesConnected) "Antenna OK" else "No Antenna",
                    isActive = headphonesConnected,
                    color = if (headphonesConnected) FMColors.fmGreen else FMColors.fmRed
                )

                // Signal
                ModeChip(
                    icon = signalQuality.icon,
                    label = signalQuality.displayName,
                    isActive = signalQuality != SignalQuality.NO_SIGNAL,
                    color = Color(signalQuality.color)
                )

                // Network Status
                ModeChip(
                    icon = if (isNetworkConnected) "🌐" else "📵",
                    label = if (isNetworkConnected) networkType else "Offline",
                    isActive = isNetworkConnected,
                    color = if (isNetworkConnected) FMColors.fmCyan else FMColors.fmRed
                )
            }

            // Hardware status bar
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        FMColors.glassWhite,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // FM Hardware indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isFMHardwareAvailable) FMColors.fmGreen else FMColors.fmAmber,
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isFMHardwareAvailable) "FM HW" else "SW Only",
                            fontSize = 10.sp,
                            color = FMColors.textSecondary
                        )
                    }

                    Text("•", color = FMColors.textTertiary, fontSize = 10.sp)

                    // Fallback status
                    Text(
                        text = when {
                            isFallbackActive -> "📡 Online Fallback Active"
                            !headphonesConnected && isNetworkConnected -> "🔄 Fallback Ready"
                            headphonesConnected && isFMHardwareAvailable -> "📻 FM Active"
                            else -> "⚠️ Limited Mode"
                        },
                        fontSize = 10.sp,
                        color = when {
                            isFallbackActive -> FMColors.fmBlue
                            !headphonesConnected -> FMColors.fmAmber
                            else -> FMColors.fmGreen
                        }
                    )
                }

                // Tap for details hint
                Text(
                    text = "Tap for details ›",
                    fontSize = 9.sp,
                    color = FMColors.textTertiary
                )
            }
        }
    }
}

@Composable
private fun ModeChip(
    icon: String,
    label: String,
    isActive: Boolean,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color.copy(alpha = if (isActive) 0.2f else 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isActive) color else FMColors.textTertiary,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
        )
    }
}

// ============================================
// 📻 FM DIAL DISPLAY
// ============================================

@Composable
private fun FMDialDisplay(
    frequency: Float,
    signalStrength: Int,
    signalQuality: SignalQuality,
    isPlaying: Boolean,
    isStereo: Boolean,
    isFallbackActive: Boolean,
    rdsStationName: String,
    isSeeking: Boolean = false,
    onFrequencyChange: (Float) -> Unit
) {
    val transition = rememberInfiniteTransition(label = "dial")

    val glowAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = FMColors.card.copy(alpha = 0.9f)
        ),
        border = BorderStroke(
            2.dp,
            Brush.linearGradient(
                listOf(
                    if (isFallbackActive) FMColors.fmBlue else FMColors.fmOrange,
                    FMColors.glassBorder
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Station name from RDS
            Text(
                text = rdsStationName,
                fontSize = 14.sp,
                color = FMColors.textSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Large frequency display
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = String.format("%.1f", frequency),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFallbackActive) FMColors.fmBlue else FMColors.fmOrange,
                    modifier = Modifier.drawBehind {
                        if (isPlaying) {
                            drawCircle(
                                color = (if (isFallbackActive) FMColors.fmBlue else FMColors.fmOrange)
                                    .copy(alpha = glowAlpha * 0.3f),
                                radius = size.width * 0.8f
                            )
                        }
                    }
                )
                Text(
                    text = " MHz",
                    fontSize = 20.sp,
                    color = FMColors.textSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stereo indicator
                StatusBadge(
                    label = if (isStereo) "STEREO" else "MONO",
                    isActive = isStereo,
                    color = FMColors.fmGreen
                )

                // Playing indicator
                StatusBadge(
                    label = if (isPlaying) "ON AIR" else "OFF",
                    isActive = isPlaying,
                    color = FMColors.fmRed
                )

                // Mode indicator
                if (isFallbackActive) {
                    StatusBadge(
                        label = "ONLINE",
                        isActive = true,
                        color = FMColors.fmBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visual dial
            FMVisualDial(
                frequency = frequency,
                isPlaying = isPlaying,
                isFallbackActive = isFallbackActive,
                onFrequencyChange = onFrequencyChange
            )
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    isActive: Boolean,
    color: Color
) {
    val transition = rememberInfiniteTransition(label = "badge")
    val alpha by transition.animateFloat(
        initialValue = if (isActive) 0.7f else 0.3f,
        targetValue = if (isActive) 1f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .background(
                color.copy(alpha = if (isActive) alpha * 0.2f else 0.1f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) color.copy(alpha = alpha) else FMColors.textTertiary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun FMVisualDial(
    frequency: Float,
    isPlaying: Boolean,
    isFallbackActive: Boolean,
    onFrequencyChange: (Float) -> Unit
) {
    val dialColor = if (isFallbackActive) FMColors.fmBlue else FMColors.fmOrange
    val minFreq = 87.5f
    val maxFreq = 108f
    val range = maxFreq - minFreq

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(FMColors.bgDark)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val delta = dragAmount.x / size.width * range
                    onFrequencyChange((frequency + delta).coerceIn(minFreq, maxFreq))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2

            // Draw frequency markers
            for (freq in 88..108) {
                val x = ((freq - minFreq) / range) * width
                val isMainMarker = freq % 2 == 0
                val markerHeight = if (isMainMarker) height * 0.4f else height * 0.25f

                drawLine(
                    color = FMColors.dialMarker.copy(alpha = if (isMainMarker) 0.5f else 0.2f),
                    start = Offset(x, centerY - markerHeight / 2),
                    end = Offset(x, centerY + markerHeight / 2),
                    strokeWidth = if (isMainMarker) 2f else 1f
                )
            }

            // Draw current position needle
            val needleX = ((frequency - minFreq) / range) * width

            // Needle glow
            if (isPlaying) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(dialColor.copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(needleX, centerY),
                        radius = 30f
                    ),
                    center = Offset(needleX, centerY)
                )
            }

            // Needle line
            drawLine(
                color = dialColor,
                start = Offset(needleX, 0f),
                end = Offset(needleX, height),
                strokeWidth = 3f
            )

            // Needle dot
            drawCircle(
                color = dialColor,
                radius = 6f,
                center = Offset(needleX, centerY)
            )
        }

        // Frequency labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (freq in listOf(88, 92, 96, 100, 104, 108)) {
                Text(
                    text = freq.toString(),
                    fontSize = 9.sp,
                    color = FMColors.textTertiary
                )
            }
        }
    }
}

// ============================================
// 📝 RDS INFO CARD
// ============================================

@Composable
private fun RDSInfoCard(
    stationName: String,
    nowPlaying: String,
    programType: String,
    isFallbackActive: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FMColors.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "📡", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFallbackActive) "Stream Info" else "RDS Data",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FMColors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Now playing with marquee effect
            Text(
                text = nowPlaying,
                fontSize = 14.sp,
                color = if (isFallbackActive) FMColors.fmBlue else FMColors.fmOrange,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Genre: $programType",
                fontSize = 12.sp,
                color = FMColors.textSecondary
            )
        }
    }
}

// ============================================
// 📶 SIGNAL STRENGTH METER
// ============================================

@Composable
private fun SignalStrengthMeter(
    strength: Int,
    quality: SignalQuality,
    isFallbackActive: Boolean
) {
    val animatedStrength by animateFloatAsState(
        targetValue = strength / 100f,
        animationSpec = tween(300),
        label = "strength"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FMColors.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isFallbackActive) "🌐" else "📶",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFallbackActive) "Stream Quality" else "Signal Strength",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FMColors.textPrimary
                    )
                }

                Text(
                    text = "$strength%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(quality.color)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Signal bars visualization
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0 until 20) {
                    val barFilled = animatedStrength >= (i + 1) / 20f
                    val barColor = when {
                        i < 4 -> FMColors.signalPoor
                        i < 8 -> FMColors.signalWeak
                        i < 12 -> FMColors.signalFair
                        i < 16 -> FMColors.signalGood
                        else -> FMColors.signalExcellent
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height((12 + i * 1.5).dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (barFilled) barColor
                                else FMColors.bgDark
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = quality.displayName,
                fontSize = 12.sp,
                color = Color(quality.color),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// ============================================
// 🎮 PLAYBACK CONTROLS
// ============================================

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    isMuted: Boolean,
    volume: Float,
    isStereo: Boolean,
    isScanning: Boolean,
    isFallbackActive: Boolean,
    isSeeking: Boolean = false,
    onPlayPause: () -> Unit,
    onMuteToggle: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onStereoToggle: () -> Unit,
    onScanStart: () -> Unit,
    onSeekUp: () -> Unit,
    onSeekDown: () -> Unit
) {
    val accentColor = if (isFallbackActive) FMColors.fmBlue else FMColors.fmOrange

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = FMColors.card.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Seek Down
                IconButton(
                    onClick = onSeekDown,
                    modifier = Modifier
                        .size(48.dp)
                        .background(FMColors.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Seek Down",
                        tint = FMColors.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Scan
                IconButton(
                    onClick = onScanStart,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isScanning) accentColor.copy(alpha = 0.3f)
                            else FMColors.surfaceVariant,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Scan",
                        tint = if (isScanning) accentColor else FMColors.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play/Pause
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.7f))
                            ),
                            CircleShape
                        )
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Stereo toggle
                IconButton(
                    onClick = onStereoToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isStereo) FMColors.fmGreen.copy(alpha = 0.3f)
                            else FMColors.surfaceVariant,
                            CircleShape
                        )
                ) {
                    Text(
                        text = if (isStereo) "ST" else "M",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isStereo) FMColors.fmGreen else FMColors.textSecondary
                    )
                }

                // Seek Up
                IconButton(
                    onClick = onSeekUp,
                    modifier = Modifier
                        .size(48.dp)
                        .background(FMColors.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Seek Up",
                        tint = FMColors.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Volume control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMuteToggle,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Mute",
                        tint = if (isMuted) FMColors.fmRed else FMColors.textPrimary
                    )
                }

                Slider(
                    value = if (isMuted) 0f else volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = FMColors.surfaceVariant
                    )
                )

                Text(
                    text = "${(volume * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = FMColors.textSecondary,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// ============================================
// 🎚️ FREQUENCY TUNER
// ============================================

@Composable
private fun FrequencyTuner(
    frequency: Float,
    onFrequencyChange: (Float) -> Unit
) {
    val minFreq = 87.5f
    val maxFreq = 108f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FMColors.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Fine Tune",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = FMColors.textPrimary
                )
                Text(
                    text = String.format("%.1f MHz", frequency),
                    fontSize = 14.sp,
                    color = FMColors.fmOrange
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "87.5",
                    fontSize = 10.sp,
                    color = FMColors.textTertiary
                )

                Slider(
                    value = frequency,
                    onValueChange = { onFrequencyChange((it * 10).toInt() / 10f) },
                    valueRange = minFreq..maxFreq,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = FMColors.fmOrange,
                        activeTrackColor = FMColors.fmOrange,
                        inactiveTrackColor = FMColors.surfaceVariant
                    )
                )

                Text(
                    text = "108.0",
                    fontSize = 10.sp,
                    color = FMColors.textTertiary
                )
            }

            // Quick step buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StepButton(label = "-1.0", onClick = { onFrequencyChange((frequency - 1f).coerceAtLeast(minFreq)) })
                StepButton(label = "-0.1", onClick = { onFrequencyChange((frequency - 0.1f).coerceAtLeast(minFreq)) })
                StepButton(label = "+0.1", onClick = { onFrequencyChange((frequency + 0.1f).coerceAtMost(maxFreq)) })
                StepButton(label = "+1.0", onClick = { onFrequencyChange((frequency + 1f).coerceAtMost(maxFreq)) })
            }
        }
    }
}

@Composable
private fun StepButton(
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .background(FMColors.surfaceVariant, RoundedCornerShape(8.dp))
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = FMColors.textPrimary
        )
    }
}

// ============================================
// 📻 PRESET BUTTONS
// ============================================

@Composable
private fun PresetButtonsRow(
    presets: List<FMStation>,
    currentFrequency: Float,
    selectedPreset: Int?,
    onPresetClick: (FMStation) -> Unit,
    onPresetLongClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FMColors.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "📻 Presets (Long press to save)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = FMColors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                presets.forEach { preset ->
                    val isSelected = selectedPreset == preset.presetSlot ||
                        abs(preset.frequency - currentFrequency) < 0.05f

                    PresetButton(
                        preset = preset,
                        isSelected = isSelected,
                        onClick = { onPresetClick(preset) },
                        onLongClick = { preset.presetSlot?.let { onPresetLongClick(it) } }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetButton(
    preset: FMStation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isSelected) FMColors.fmOrange
                    else FMColors.surfaceVariant,
                    CircleShape
                )
                .then(
                    if (isSelected) Modifier.border(2.dp, FMColors.fmOrange, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = preset.presetSlot?.toString() ?: "?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else FMColors.textPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format("%.1f", preset.frequency),
            fontSize = 10.sp,
            color = if (isSelected) FMColors.fmOrange else FMColors.textSecondary
        )
    }
}

// ============================================
// 📡 SCANNED STATION ITEM
// ============================================

@Composable
private fun ScannedStationItem(
    station: FMStation,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FMColors.fmOrange.copy(alpha = 0.2f)
            else FMColors.surface.copy(alpha = 0.5f)
        ),
        border = if (isSelected) BorderStroke(1.dp, FMColors.fmOrange) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(FMColors.fmOrange.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📻",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name.ifEmpty { "Station" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = FMColors.textPrimary
                )
                Text(
                    text = String.format("%.1f MHz", station.frequency),
                    fontSize = 12.sp,
                    color = FMColors.textSecondary
                )
            }

            // Signal strength indicator
            Box(
                modifier = Modifier
                    .background(
                        Color(station.lastSignalQuality.color).copy(alpha = 0.2f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${station.lastSignalStrength}%",
                    fontSize = 11.sp,
                    color = Color(station.lastSignalQuality.color)
                )
            }
        }
    }
}

// ============================================
// 🌐 FALLBACK INFO CARD
// ============================================

@Composable
private fun FallbackInfoCard(
    originalFrequency: Float,
    fallbackStationName: String,
    onSwitchToFM: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FMColors.fmBlue.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, FMColors.fmBlue.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🌐", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Online Fallback Active",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FMColors.fmBlue
                    )
                    Text(
                        text = "FM signal too weak at ${String.format("%.1f", originalFrequency)} MHz",
                        fontSize = 12.sp,
                        color = FMColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Now streaming: $fallbackStationName",
                    fontSize = 12.sp,
                    color = FMColors.textPrimary
                )

                TextButton(
                    onClick = onSwitchToFM,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = FMColors.fmOrange
                    )
                ) {
                    Text("Try FM Again")
                }
            }
        }
    }
}

// ============================================
// ⚙️ SETTINGS SHEET
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FMSettingsSheet(
    currentMode: RadioMode,
    headphonesConnected: Boolean,
    onModeChange: (RadioMode) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FMColors.bgMid
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "⚙️ Radio Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = FMColors.textPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Mode Selection
            Text(
                text = "Radio Mode",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = FMColors.textSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            RadioMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModeChange(mode) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == mode,
                        onClick = { onModeChange(mode) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = FMColors.fmOrange
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "${mode.icon} ${mode.displayName}",
                            fontSize = 16.sp,
                            color = FMColors.textPrimary
                        )
                        Text(
                            text = when (mode) {
                                RadioMode.FM_OFFLINE -> "Use FM radio only (requires headphones)"
                                RadioMode.ONLINE -> "Stream online radio only"
                                RadioMode.HYBRID_AUTO -> "Auto-switch between FM and online"
                            },
                            fontSize = 12.sp,
                            color = FMColors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hardware Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = FMColors.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Hardware Status",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = FMColors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🎧 Headphones (Antenna)",
                            fontSize = 14.sp,
                            color = FMColors.textSecondary
                        )
                        Text(
                            text = if (headphonesConnected) "Connected" else "Not Connected",
                            fontSize = 14.sp,
                            color = if (headphonesConnected) FMColors.fmGreen else FMColors.fmRed
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📻 FM Hardware",
                            fontSize = 14.sp,
                            color = FMColors.textSecondary
                        )
                        Text(
                            text = "Available",
                            fontSize = 14.sp,
                            color = FMColors.fmGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ============================================
// 📡 SCANNING PROGRESS CARD
// ============================================

@Composable
private fun ScanningProgressCard(
    progress: Float,
    currentFrequency: Float,
    stationsFound: Int,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FMColors.fmOrange.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, FMColors.fmOrange.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📡 Scanning...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FMColors.fmOrange
                )
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = FMColors.fmRed)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = FMColors.fmOrange,
                trackColor = FMColors.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f MHz", currentFrequency),
                    fontSize = 12.sp,
                    color = FMColors.textSecondary
                )
                Text(
                    text = "$stationsFound stations found",
                    fontSize = 12.sp,
                    color = FMColors.fmGreen
                )
            }
        }
    }
}

// ============================================
// 📋 SECTION HEADER
// ============================================

@Composable
private fun SectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = FMColors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .background(FMColors.fmOrange.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                color = FMColors.fmOrange
            )
        }
    }
}

// ============================================
// 🎛️ EQ PRESET QUICK SELECT
// ============================================

@Composable
private fun EQPresetQuickSelect(
    selectedPreset: String,
    onPresetSelect: (String) -> Unit
) {
    val presets = listOf("Flat", "FM Enhanced", "Voice Clarity", "Music", "Bass Boost")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FMColors.surface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🎛️ Audio Preset",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = FMColors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = { onPresetSelect(preset) },
                        label = { Text(preset, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FMColors.fmOrange.copy(alpha = 0.2f),
                            selectedLabelColor = FMColors.fmOrange
                        )
                    )
                }
            }
        }
    }
}

// ============================================
// ⭐ FAVORITES SHEET
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesSheet(
    favorites: List<FMStation>,
    currentFrequency: Float,
    onStationClick: (FMStation) -> Unit,
    onRemoveFavorite: (FMStation) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FMColors.bgMid
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "⭐ Favorite Stations",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = FMColors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "💔", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No favorites yet",
                            color = FMColors.textSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tap ❤️ to add stations",
                            color = FMColors.textTertiary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(favorites) { station ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStationClick(station) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(FMColors.fmOrange.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "📻", fontSize = 20.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = station.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = FMColors.textPrimary
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f MHz", station.frequency),
                                    fontSize = 13.sp,
                                    color = FMColors.textSecondary
                                )
                            }

                            IconButton(onClick = { onRemoveFavorite(station) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Remove",
                                    tint = FMColors.fmRed.copy(alpha = 0.7f)
                                )
                            }
                        }

                        HorizontalDivider(color = FMColors.glassBorder)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ============================================
// 🎛️ EQ PRESETS SHEET
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EQPresetsSheet(
    selectedPreset: String,
    onPresetSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf(
        Triple("Flat", "No EQ adjustment", "📊"),
        Triple("FM Enhanced", "Optimized for FM reception", "📻"),
        Triple("Voice Clarity", "Enhanced speech for talk radio", "🗣️"),
        Triple("Music", "Full spectrum for music", "🎵"),
        Triple("Bass Boost", "Enhanced low frequencies", "🔊"),
        Triple("Treble Boost", "Enhanced high frequencies", "🔔"),
        Triple("Night Mode", "Soft dynamics for quiet listening", "🌙"),
        Triple("Car Audio", "Optimized for vehicle speakers", "🚗")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FMColors.bgMid
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "🎛️ FM Audio Presets",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = FMColors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(presets) { (name, description, icon) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onPresetSelect(name) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPreset == name)
                                FMColors.fmOrange.copy(alpha = 0.15f)
                            else FMColors.surface
                        ),
                        border = if (selectedPreset == name)
                            BorderStroke(1.dp, FMColors.fmOrange)
                        else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = icon, fontSize = 24.sp)

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedPreset == name) FMColors.fmOrange else FMColors.textPrimary
                                )
                                Text(
                                    text = description,
                                    fontSize = 12.sp,
                                    color = FMColors.textSecondary
                                )
                            }

                            if (selectedPreset == name) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = FMColors.fmOrange
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ============================================
// 📡 SCAN RESULTS SHEET
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanResultsSheet(
    stations: List<FMStation>,
    onStationClick: (FMStation) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FMColors.bgMid
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📡 Scan Results",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = FMColors.textPrimary
                )
                Box(
                    modifier = Modifier
                        .background(FMColors.fmGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${stations.size} stations",
                        fontSize = 14.sp,
                        color = FMColors.fmGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(stations) { station ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onStationClick(station) },
                        colors = CardDefaults.cardColors(
                            containerColor = FMColors.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(FMColors.fmOrange, FMColors.fmRed)
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.0f", station.frequency),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = station.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = FMColors.textPrimary
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f MHz • ${station.genre?.name ?: "Unknown"}", station.frequency),
                                    fontSize = 13.sp,
                                    color = FMColors.textSecondary
                                )
                            }

                            // Signal strength indicator
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(station.lastSignalQuality.color).copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${station.lastSignalStrength}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(station.lastSignalQuality.color)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
