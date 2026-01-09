package com.example.dwn.context

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Calendar

/**
 * ContextMediaManager - Manages context detection and mode switching
 *
 * This manager monitors device sensors, audio output, screen state, and other
 * signals to automatically switch between audio modes for optimal experience.
 */
class ContextMediaManager(
    private val context: Context
) : SensorEventListener {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // System services
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    // ============================================
    // STATE
    // ============================================

    private val _state = MutableStateFlow(ContextMediaState())
    val state: StateFlow<ContextMediaState> = _state.asStateFlow()

    private val _signals = MutableStateFlow(ContextSignals())
    val signals: StateFlow<ContextSignals> = _signals.asStateFlow()

    private val _activeMode = MutableStateFlow(ContextMode.AUTO)
    val activeMode: StateFlow<ContextMode> = _activeMode.asStateFlow()

    private val _configuration = MutableStateFlow(
        defaultModeConfigurations[ContextMode.AUTO]
            ?: ModeConfiguration(ContextMode.AUTO, ModeAudioProfile(), ModeUIProfile())
    )
    val configuration: StateFlow<ModeConfiguration> = _configuration.asStateFlow()

    // Motion detection
    private var accelerometerValues = FloatArray(3)
    private var lastAccelUpdate = 0L
    private val motionHistory = mutableListOf<Float>()
    private var stepCount = 0

    // ============================================
    // INITIALIZATION
    // ============================================

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        // Monitor audio output changes
        registerAudioDeviceCallback()

        // Monitor screen state
        registerScreenStateReceiver()

        // Start motion detection
        startMotionDetection()

        // Start time-based monitoring
        startTimeMonitoring()

        // Initial context detection
        detectCurrentContext()
    }

    // ============================================
    // AUDIO OUTPUT DETECTION
    // ============================================

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            updateAudioOutput()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            updateAudioOutput()
        }
    }

    private fun registerAudioDeviceCallback() {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        updateAudioOutput()
    }

    private fun updateAudioOutput() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        var outputType = AudioOutputType.SPEAKER
        var bluetoothType: BluetoothDeviceType? = null
        var isHeadphones = false
        var isCasting = false

        for (device in devices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> {
                    outputType = AudioOutputType.WIRED_HEADPHONES
                    isHeadphones = true
                }
                AudioDeviceInfo.TYPE_USB_HEADSET -> {
                    outputType = AudioOutputType.USB_AUDIO
                    isHeadphones = true
                }
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                    bluetoothType = detectBluetoothDeviceType()
                    outputType = when (bluetoothType) {
                        BluetoothDeviceType.EARBUDS -> AudioOutputType.BLUETOOTH_EARBUDS
                        BluetoothDeviceType.HEADPHONES -> AudioOutputType.BLUETOOTH_HEADPHONES
                        BluetoothDeviceType.SPEAKER -> AudioOutputType.BLUETOOTH_SPEAKER
                        BluetoothDeviceType.CAR_AUDIO -> AudioOutputType.BLUETOOTH_CAR
                        else -> AudioOutputType.BLUETOOTH_HEADPHONES
                    }
                    isHeadphones = bluetoothType == BluetoothDeviceType.EARBUDS ||
                                   bluetoothType == BluetoothDeviceType.HEADPHONES
                }
                AudioDeviceInfo.TYPE_HDMI,
                AudioDeviceInfo.TYPE_HDMI_ARC -> {
                    outputType = AudioOutputType.HDMI
                    isCasting = true
                }
            }
        }

        // Check for casting
        // Note: Real casting detection would need MediaRouter

        _signals.value = _signals.value.copy(
            audioOutput = outputType,
            bluetoothDeviceType = bluetoothType,
            isHeadphonesConnected = isHeadphones,
            isCasting = isCasting
        )

        evaluateAndSwitchMode()
    }

    private fun detectBluetoothDeviceType(): BluetoothDeviceType {
        // This is a simplified detection
        // Real implementation would check BluetoothDevice.getBluetoothClass()
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter?.isEnabled == true) {
                // Check connected devices
                val bondedDevices = bluetoothAdapter.bondedDevices
                for (device in bondedDevices) {
                    val deviceClass = device.bluetoothClass
                    if (deviceClass != null) {
                        return when (deviceClass.majorDeviceClass) {
                            BluetoothClass.Device.Major.AUDIO_VIDEO -> {
                                when {
                                    deviceClass.hasService(BluetoothClass.Service.AUDIO) -> {
                                        val name = device.name?.lowercase() ?: ""
                                        when {
                                            name.contains("car") || name.contains("auto") -> BluetoothDeviceType.CAR_AUDIO
                                            name.contains("bud") || name.contains("pod") || name.contains("air") -> BluetoothDeviceType.EARBUDS
                                            name.contains("speaker") || name.contains("soundbar") -> BluetoothDeviceType.SPEAKER
                                            else -> BluetoothDeviceType.HEADPHONES
                                        }
                                    }
                                    else -> BluetoothDeviceType.UNKNOWN
                                }
                            }
                            else -> BluetoothDeviceType.UNKNOWN
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            // Bluetooth permission not granted
        }
        return BluetoothDeviceType.UNKNOWN
    }

    // ============================================
    // SCREEN STATE DETECTION
    // ============================================

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    _signals.value = _signals.value.copy(isScreenOn = true)
                }
                Intent.ACTION_SCREEN_OFF -> {
                    _signals.value = _signals.value.copy(isScreenOn = false)
                }
            }
            evaluateAndSwitchMode()
        }
    }

    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(screenStateReceiver, filter)

        // Initial state
        _signals.value = _signals.value.copy(
            isScreenOn = powerManager.isInteractive
        )
    }

    // ============================================
    // MOTION DETECTION
    // ============================================

    private fun startMotionDetection() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        stepDetector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastAccelUpdate > 200) {  // 5Hz
                    lastAccelUpdate = currentTime

                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // Calculate magnitude of acceleration (minus gravity)
                    val magnitude = kotlin.math.sqrt(x * x + y * y + z * z) - 9.8f
                    motionHistory.add(kotlin.math.abs(magnitude))

                    // Keep last 50 samples (10 seconds at 5Hz)
                    if (motionHistory.size > 50) {
                        motionHistory.removeAt(0)
                    }

                    // Analyze motion
                    analyzeMotion()
                }
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                stepCount++
                // Walking detected
                if (_signals.value.motionState != MotionState.WALKING &&
                    _signals.value.motionState != MotionState.RUNNING) {
                    _signals.value = _signals.value.copy(motionState = MotionState.WALKING)
                    evaluateAndSwitchMode()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun analyzeMotion() {
        if (motionHistory.size < 10) return

        val avgMotion = motionHistory.takeLast(20).average().toFloat()
        val maxMotion = motionHistory.takeLast(20).maxOrNull() ?: 0f

        val motionState = when {
            avgMotion < 0.5f && maxMotion < 1f -> MotionState.STATIONARY
            avgMotion < 2f && maxMotion < 4f -> MotionState.WALKING
            avgMotion < 5f -> MotionState.RUNNING
            avgMotion > 5f && maxMotion > 10f -> MotionState.IN_VEHICLE // High sustained motion
            else -> MotionState.UNKNOWN
        }

        if (_signals.value.motionState != motionState) {
            _signals.value = _signals.value.copy(
                motionState = motionState,
                isMovingFast = motionState == MotionState.IN_VEHICLE || motionState == MotionState.RUNNING
            )
            evaluateAndSwitchMode()
        }
    }

    // ============================================
    // TIME-BASED DETECTION
    // ============================================

    private fun startTimeMonitoring() {
        scope.launch {
            while (isActive) {
                updateTimeOfDay()
                delay(60000)  // Check every minute
            }
        }
    }

    private fun updateTimeOfDay() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val timeOfDay = when (hour) {
            in 5..10 -> TimeOfDay.MORNING
            in 11..16 -> TimeOfDay.DAY
            in 17..20 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }

        if (_signals.value.timeOfDay != timeOfDay) {
            _signals.value = _signals.value.copy(
                timeOfDay = timeOfDay,
                hourOfDay = hour
            )
            evaluateAndSwitchMode()
        }
    }

    // ============================================
    // MODE EVALUATION & SWITCHING
    // ============================================

    private fun detectCurrentContext() {
        updateAudioOutput()
        updateTimeOfDay()
    }

    private fun evaluateAndSwitchMode() {
        val currentState = _state.value
        val signals = _signals.value

        // If mode is locked, don't switch
        if (currentState.isLocked) return

        // If not in AUTO mode, respect user choice
        if (_activeMode.value != ContextMode.AUTO) return

        // Evaluate best mode based on signals
        val suggestedMode = determineBestMode(signals)

        if (suggestedMode != currentState.resolvedMode) {
            switchToMode(suggestedMode, isAutomatic = true)
        }
    }

    private fun determineBestMode(signals: ContextSignals): ContextMode {
        // Priority-based mode selection

        // 1. Casting takes highest priority
        if (signals.isCasting || signals.audioOutput == AudioOutputType.CAST_DEVICE) {
            return ContextMode.CAST
        }

        // 2. Car audio = Drive mode
        if (signals.audioOutput == AudioOutputType.BLUETOOTH_CAR ||
            (signals.motionState == MotionState.IN_VEHICLE && signals.isMovingFast)) {
            return ContextMode.DRIVE
        }

        // 3. Night time
        if (signals.timeOfDay == TimeOfDay.NIGHT && signals.hourOfDay in 22..23 || signals.hourOfDay in 0..5) {
            return ContextMode.NIGHT
        }

        // 4. Motion-based
        when (signals.motionState) {
            MotionState.WALKING -> {
                if (signals.isHeadphonesConnected) return ContextMode.WALK
            }
            MotionState.RUNNING -> {
                if (signals.isHeadphonesConnected) return ContextMode.WORKOUT
            }
            MotionState.IN_VEHICLE -> {
                return ContextMode.COMMUTE
            }
            else -> {}
        }

        // 5. Default based on audio output
        return when (signals.audioOutput) {
            AudioOutputType.BLUETOOTH_EARBUDS,
            AudioOutputType.BLUETOOTH_HEADPHONES,
            AudioOutputType.WIRED_HEADPHONES -> ContextMode.WALK
            AudioOutputType.BLUETOOTH_SPEAKER -> ContextMode.FOCUS
            else -> ContextMode.FOCUS
        }
    }

    // ============================================
    // PUBLIC API
    // ============================================

    /**
     * Manually set a mode
     */
    fun setMode(mode: ContextMode) {
        _activeMode.value = mode

        if (mode == ContextMode.AUTO) {
            evaluateAndSwitchMode()
        } else {
            switchToMode(mode, isAutomatic = false)
        }
    }

    /**
     * Lock the current mode (prevent auto-switching)
     */
    fun lockMode(locked: Boolean) {
        _state.value = _state.value.copy(isLocked = locked)
    }

    /**
     * Get configuration for a specific mode
     */
    fun getConfiguration(mode: ContextMode): ModeConfiguration {
        return _state.value.preferences.customConfigurations[mode]
            ?: defaultModeConfigurations[mode]
            ?: ModeConfiguration(mode, ModeAudioProfile(), ModeUIProfile())
    }

    /**
     * Update custom configuration for a mode
     */
    fun updateConfiguration(mode: ContextMode, config: ModeConfiguration) {
        val currentPrefs = _state.value.preferences
        val updatedConfigs = currentPrefs.customConfigurations.toMutableMap()
        updatedConfigs[mode] = config

        _state.value = _state.value.copy(
            preferences = currentPrefs.copy(customConfigurations = updatedConfigs)
        )

        // If this is the active mode, apply changes
        if (_state.value.resolvedMode == mode) {
            _configuration.value = config
        }
    }

    /**
     * Save device-specific audio profile
     */
    fun saveDeviceProfile(deviceId: String, profile: ModeAudioProfile) {
        val currentPrefs = _state.value.preferences
        val updatedProfiles = currentPrefs.deviceProfiles.toMutableMap()
        updatedProfiles[deviceId] = profile

        _state.value = _state.value.copy(
            preferences = currentPrefs.copy(deviceProfiles = updatedProfiles)
        )
    }

    private fun switchToMode(mode: ContextMode, isAutomatic: Boolean) {
        val config = getConfiguration(mode)

        _state.value = _state.value.copy(
            resolvedMode = mode,
            configuration = config,
            lastModeChange = System.currentTimeMillis()
        )

        _configuration.value = config

        // Log mode change
        logModeChange(mode, if (isAutomatic) TriggerType.AUDIO_OUTPUT else TriggerType.MANUAL)
    }

    private fun logModeChange(mode: ContextMode, trigger: TriggerType) {
        val currentPrefs = _state.value.preferences
        val history = currentPrefs.modeHistory.toMutableList()

        history.add(0, ModeHistoryEntry(
            mode = mode,
            timestamp = System.currentTimeMillis(),
            trigger = trigger
        ))

        // Keep last 100 entries
        if (history.size > 100) {
            history.removeAt(history.lastIndex)
        }

        _state.value = _state.value.copy(
            preferences = currentPrefs.copy(modeHistory = history)
        )
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        scope.cancel()
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        sensorManager.unregisterListener(this)
        try {
            context.unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {}
    }
}

