package com.example.dwn.radio

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.lang.reflect.Method

/**
 * FMRadioManager - Comprehensive FM Radio with Online Fallback
 *
 * Features:
 * - Hardware FM radio detection and control
 * - Automatic station scanning
 * - Signal strength monitoring
 * - Seamless online fallback when FM signal is weak
 * - Reverse fallback when FM signal is restored
 * - RDS data parsing
 * - Preset and favorite management
 */
class FMRadioManager(
    private val context: Context,
    private val onlineRadioManager: RadioManager? = null
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // ============================================
    // STATE FLOWS
    // ============================================

    private val _state = MutableStateFlow(FMRadioState())
    val state: StateFlow<FMRadioState> = _state.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentFrequency = MutableStateFlow(87.5f)
    val currentFrequency: StateFlow<Float> = _currentFrequency.asStateFlow()

    private val _signalStrength = MutableStateFlow(0)
    val signalStrength: StateFlow<Int> = _signalStrength.asStateFlow()

    private val _signalQuality = MutableStateFlow(SignalQuality.NO_SIGNAL)
    val signalQuality: StateFlow<SignalQuality> = _signalQuality.asStateFlow()

    private val _isFallbackActive = MutableStateFlow(false)
    val isFallbackActive: StateFlow<Boolean> = _isFallbackActive.asStateFlow()

    private val _rdsInfo = MutableStateFlow<RDSInfo?>(null)
    val rdsInfo: StateFlow<RDSInfo?> = _rdsInfo.asStateFlow()

    // Signal history for analysis
    private val _signalHistory = MutableStateFlow<List<SignalEvent>>(emptyList())
    val signalHistory: StateFlow<List<SignalEvent>> = _signalHistory.asStateFlow()

    // ============================================
    // HARDWARE DETECTION
    // ============================================

    private var fmHardwareAvailable = false
    private var headphoneReceiver: BroadcastReceiver? = null
    private var signalMonitorJob: Job? = null

    // FM frequency band
    private var currentBand = FMBand.WORLDWIDE

    // Station mappings for fallback
    private val stationMappings = mutableMapOf<Float, StationMapping>()

    // Audio focus
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    // ============================================
    // INITIALIZATION
    // ============================================

    init {
        checkFMHardware()
        loadStationMappings()
        registerHeadphoneReceiver()
    }

    /**
     * Check if device has FM radio hardware
     */
    @SuppressLint("PrivateApi")
    private fun checkFMHardware() {
        scope.launch {
            try {
                // Try to detect FM hardware through various methods
                val hasFM = detectFMHardware()
                fmHardwareAvailable = hasFM

                val headphonesConnected = checkHeadphonesConnected()

                val status = when {
                    !hasFM -> FMHardwareStatus.UNAVAILABLE
                    !headphonesConnected -> FMHardwareStatus.NO_ANTENNA
                    else -> FMHardwareStatus.AVAILABLE
                }

                _state.value = _state.value.copy(
                    hardwareStatus = status,
                    headphoneConnected = headphonesConnected
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    hardwareStatus = FMHardwareStatus.UNAVAILABLE,
                    error = "FM hardware check failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Detect FM radio hardware presence
     */
    @SuppressLint("PrivateApi")
    private fun detectFMHardware(): Boolean {
        return try {
            // Method 1: Check for FM receiver service
            val pmClass = Class.forName("android.content.pm.PackageManager")
            val hasSystemFeature = context.packageManager.hasSystemFeature("android.hardware.fmradio")
            if (hasSystemFeature) return true

            // Method 2: Try to access FM receiver class (MediaTek, Qualcomm, etc.)
            val fmReceiverClasses = listOf(
                "android.media.FmReceiver",
                "com.mediatek.fmradio.FmRadioService",
                "com.qualcomm.fmradio.FMRadioService",
                "com.broadcom.fm.fmreceiver.FmReceiver"
            )

            for (className in fmReceiverClasses) {
                try {
                    Class.forName(className)
                    return true
                } catch (e: ClassNotFoundException) {
                    continue
                }
            }

            // Method 3: Check device features
            val features = context.packageManager.systemAvailableFeatures
            for (feature in features) {
                if (feature.name?.contains("fm", ignoreCase = true) == true) {
                    return true
                }
            }

            // For demo purposes, return true if Android version supports it
            // In real implementation, this would be more rigorous
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if wired headphones are connected (required as antenna)
     */
    private fun checkHeadphonesConnected(): Boolean {
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
     * Register receiver for headphone connect/disconnect events
     */
    private fun registerHeadphoneReceiver() {
        headphoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    AudioManager.ACTION_HEADSET_PLUG -> {
                        val state = intent.getIntExtra("state", 0)
                        val connected = state == 1
                        onHeadphonesChanged(connected)
                    }
                }
            }
        }

        context.registerReceiver(
            headphoneReceiver,
            IntentFilter(AudioManager.ACTION_HEADSET_PLUG)
        )
    }

    /**
     * Handle headphone connection changes
     */
    private fun onHeadphonesChanged(connected: Boolean) {
        _state.value = _state.value.copy(
            headphoneConnected = connected,
            hardwareStatus = when {
                !fmHardwareAvailable -> FMHardwareStatus.UNAVAILABLE
                !connected -> FMHardwareStatus.NO_ANTENNA
                else -> FMHardwareStatus.AVAILABLE
            }
        )

        // If FM is playing and headphones disconnected, trigger fallback
        if (!connected && _state.value.isPlaying && !_state.value.isFallbackActive) {
            triggerOnlineFallback(FallbackReason.NO_ANTENNA)
        }

        // If headphones reconnected and was in fallback, consider reverse fallback
        if (connected && _state.value.isFallbackActive && _state.value.autoFallbackEnabled) {
            scope.launch {
                delay(2000) // Wait for signal to stabilize
                checkReverseFallback()
            }
        }
    }

    /**
     * Load station mappings from storage
     */
    private fun loadStationMappings() {
        // Load default mappings
        defaultFMStationMappings.forEach { mapping ->
            stationMappings[mapping.fmFrequency] = mapping
        }

        // TODO: Load user-defined mappings from persistent storage
    }

    // ============================================
    // FREQUENCY TUNING
    // ============================================

    /**
     * Tune to a specific frequency
     */
    fun tuneToFrequency(frequency: Float) {
        if (frequency !in currentBand.minFrequency..currentBand.maxFrequency) {
            _state.value = _state.value.copy(
                error = "Frequency out of range: $frequency MHz"
            )
            return
        }

        scope.launch {
            _state.value = _state.value.copy(isSeeking = true)
            _currentFrequency.value = frequency

            // Simulate tuning delay
            delay(200)

            // Update signal (simulated - real implementation would read from hardware)
            updateSignalStrength()

            // Check for known station at this frequency
            val knownStation = findStationAtFrequency(frequency)

            _state.value = _state.value.copy(
                currentFrequency = frequency,
                currentStation = knownStation ?: FMStation(frequency = frequency),
                isSeeking = false,
                isTuned = true
            )

            // Start signal monitoring
            startSignalMonitoring()
        }
    }

    /**
     * Step up frequency
     */
    fun stepUp() {
        val newFreq = (_currentFrequency.value + currentBand.stepSize)
            .coerceAtMost(currentBand.maxFrequency)
        tuneToFrequency(newFreq)
    }

    /**
     * Step down frequency
     */
    fun stepDown() {
        val newFreq = (_currentFrequency.value - currentBand.stepSize)
            .coerceAtLeast(currentBand.minFrequency)
        tuneToFrequency(newFreq)
    }

    /**
     * Seek next station
     */
    fun seekUp() {
        scope.launch {
            _state.value = _state.value.copy(isSeeking = true)

            var freq = _currentFrequency.value + currentBand.stepSize
            var foundStation = false

            while (freq <= currentBand.maxFrequency && !foundStation) {
                _currentFrequency.value = freq
                delay(50) // Simulate scanning

                val signalStrength = simulateSignalStrength(freq)
                if (signalStrength > 40) {
                    foundStation = true
                    tuneToFrequency(freq)
                } else {
                    freq += currentBand.stepSize
                }
            }

            if (!foundStation) {
                // Wrap around to start of band
                tuneToFrequency(currentBand.minFrequency)
            }

            _state.value = _state.value.copy(isSeeking = false)
        }
    }

    /**
     * Seek previous station
     */
    fun seekDown() {
        scope.launch {
            _state.value = _state.value.copy(isSeeking = true)

            var freq = _currentFrequency.value - currentBand.stepSize
            var foundStation = false

            while (freq >= currentBand.minFrequency && !foundStation) {
                _currentFrequency.value = freq
                delay(50)

                val signalStrength = simulateSignalStrength(freq)
                if (signalStrength > 40) {
                    foundStation = true
                    tuneToFrequency(freq)
                } else {
                    freq -= currentBand.stepSize
                }
            }

            if (!foundStation) {
                tuneToFrequency(currentBand.maxFrequency)
            }

            _state.value = _state.value.copy(isSeeking = false)
        }
    }

    // ============================================
    // STATION SCANNING
    // ============================================

    /**
     * Scan for all stations in band
     */
    fun scanAllStations(): Flow<Float> = flow {
        _state.value = _state.value.copy(isScanning = true, scannedStations = emptyList())

        val startTime = System.currentTimeMillis()
        val foundStations = mutableListOf<FMStation>()

        var freq = currentBand.minFrequency
        while (freq <= currentBand.maxFrequency) {
            emit(freq) // Emit progress

            val signalStrength = simulateSignalStrength(freq)
            if (signalStrength > 35) {
                val station = FMStation(
                    frequency = freq,
                    name = "FM ${String.format("%.1f", freq)}",
                    lastSignalStrength = signalStrength,
                    lastSignalQuality = getSignalQuality(signalStrength)
                )
                foundStations.add(station)
            }

            freq += currentBand.stepSize
            delay(30) // Scanning delay
        }

        val scanDuration = System.currentTimeMillis() - startTime

        _state.value = _state.value.copy(
            isScanning = false,
            scannedStations = foundStations
        )
    }

    /**
     * Find station at specific frequency
     */
    private fun findStationAtFrequency(frequency: Float): FMStation? {
        // Check presets
        _state.value.presets.find {
            kotlin.math.abs(it.frequency - frequency) < 0.05f
        }?.let { return it }

        // Check favorites
        _state.value.favorites.find {
            kotlin.math.abs(it.frequency - frequency) < 0.05f
        }?.let { return it }

        // Check scanned stations
        _state.value.scannedStations.find {
            kotlin.math.abs(it.frequency - frequency) < 0.05f
        }?.let { return it }

        return null
    }

    // ============================================
    // SIGNAL MONITORING
    // ============================================

    /**
     * Start continuous signal monitoring
     */
    private fun startSignalMonitoring() {
        signalMonitorJob?.cancel()
        signalMonitorJob = scope.launch {
            while (isActive && _state.value.isPlaying) {
                updateSignalStrength()
                delay(1000) // Check every second
            }
        }
    }

    /**
     * Update signal strength and quality
     */
    private fun updateSignalStrength() {
        val strength = simulateSignalStrength(_currentFrequency.value)
        val quality = getSignalQuality(strength)
        val noiseLevel = simulateNoiseLevel(strength)

        _signalStrength.value = strength
        _signalQuality.value = quality

        // Record signal event
        val event = SignalEvent(
            frequency = _currentFrequency.value,
            strength = strength,
            quality = quality,
            noiseFloor = noiseLevel
        )

        val history = _signalHistory.value.takeLast(59) + event
        _signalHistory.value = history

        // Check if signal is stable
        val isStable = checkSignalStability(history)

        _state.value = _state.value.copy(
            signalStrength = strength,
            signalQuality = quality,
            isSignalStable = isStable,
            noiseLevel = noiseLevel
        )

        // Check for fallback conditions
        if (_state.value.autoFallbackEnabled && !_state.value.isFallbackActive) {
            checkFallbackConditions(strength, quality, isStable)
        }

        // Check for reverse fallback conditions
        if (_state.value.isFallbackActive && _state.value.autoFallbackEnabled) {
            checkReverseFallback()
        }
    }

    /**
     * Simulate signal strength (replace with actual hardware reading)
     */
    private fun simulateSignalStrength(frequency: Float): Int {
        // Simulate based on frequency - certain frequencies have "stations"
        val baseStrength = when {
            frequency in 88.0f..88.5f -> 75
            frequency in 91.0f..91.5f -> 80
            frequency in 95.0f..95.5f -> 85
            frequency in 98.0f..98.5f -> 90
            frequency in 101.0f..101.5f -> 70
            frequency in 104.0f..104.5f -> 65
            frequency in 107.0f..107.5f -> 60
            else -> (Math.random() * 30).toInt() // Random weak signal
        }

        // Add some variation
        val variation = (Math.random() * 10 - 5).toInt()
        return (baseStrength + variation).coerceIn(0, 100)
    }

    /**
     * Simulate noise level
     */
    private fun simulateNoiseLevel(signalStrength: Int): Float {
        // Higher signal = lower noise
        return (100 - signalStrength) / 100f * 0.5f
    }

    /**
     * Get signal quality from strength
     */
    private fun getSignalQuality(strength: Int): SignalQuality {
        return when {
            strength >= 80 -> SignalQuality.EXCELLENT
            strength >= 60 -> SignalQuality.GOOD
            strength >= 45 -> SignalQuality.FAIR
            strength >= 30 -> SignalQuality.WEAK
            strength >= 15 -> SignalQuality.POOR
            else -> SignalQuality.NO_SIGNAL
        }
    }

    /**
     * Check if signal is stable
     */
    private fun checkSignalStability(history: List<SignalEvent>): Boolean {
        if (history.size < 5) return true

        val recent = history.takeLast(5)
        val avgStrength = recent.map { it.strength }.average()
        val variance = recent.map { (it.strength - avgStrength).let { d -> d * d } }.average()

        // Signal is stable if variance is low
        return variance < 100
    }

    // ============================================
    // ONLINE FALLBACK
    // ============================================

    /**
     * Check conditions for triggering online fallback
     */
    private fun checkFallbackConditions(strength: Int, quality: SignalQuality, isStable: Boolean) {
        val threshold = _state.value.signalThreshold

        val shouldFallback = when {
            strength < threshold -> true
            quality == SignalQuality.NO_SIGNAL -> true
            quality == SignalQuality.POOR && !isStable -> true
            !_state.value.headphoneConnected -> true
            else -> false
        }

        if (shouldFallback && hasInternetConnection()) {
            val reason = when {
                !_state.value.headphoneConnected -> FallbackReason.NO_ANTENNA
                strength < 10 -> FallbackReason.NO_SIGNAL
                strength < threshold -> FallbackReason.WEAK_SIGNAL
                !isStable -> FallbackReason.SIGNAL_FLUCTUATION
                else -> FallbackReason.WEAK_SIGNAL
            }
            triggerOnlineFallback(reason)
        }
    }

    /**
     * Trigger online fallback
     */
    private fun triggerOnlineFallback(reason: FallbackReason) {
        scope.launch {
            // Find matching online station
            val mapping = stationMappings[_currentFrequency.value]
            val onlineStation = if (mapping != null) {
                // Use mapped station
                RadioStation(
                    type = StationType.TRADITIONAL,
                    name = mapping.fmStationName,
                    streamUrl = mapping.onlineStreamUrl,
                    streamQuality = StreamQuality.HIGH
                )
            } else {
                // Find similar genre station or use default
                findSimilarOnlineStation() ?: getDefaultOnlineStation()
            }

            _state.value = _state.value.copy(
                isFallbackActive = true,
                fallbackReason = reason.displayName,
                fallbackStation = onlineStation
            )

            _isFallbackActive.value = true

            // Start online playback
            onlineRadioManager?.play(onlineStation)
        }
    }

    /**
     * Find similar online station based on genre
     */
    private fun findSimilarOnlineStation(): RadioStation? {
        val currentStation = _state.value.currentStation
        val genre = currentStation?.genre

        // Find online station with same genre
        // This would search the online radio database
        return null
    }

    /**
     * Get default fallback station
     */
    private fun getDefaultOnlineStation(): RadioStation {
        return RadioStation(
            type = StationType.TRADITIONAL,
            name = "Online Radio",
            description = "Fallback stream",
            streamUrl = "https://stream.example.com/fallback",
            streamQuality = StreamQuality.HIGH
        )
    }

    /**
     * Check conditions for reverse fallback (back to FM)
     */
    private fun checkReverseFallback() {
        if (!_state.value.isFallbackActive) return
        if (!_state.value.headphoneConnected) return

        val strength = _signalStrength.value
        val threshold = _state.value.signalThreshold + 10 // Higher threshold for reverse

        if (strength > threshold && checkSignalStability(_signalHistory.value)) {
            triggerReverseFallback(ReverseFallbackReason.SIGNAL_RESTORED)
        }

        // Also reverse if internet is lost
        if (!hasInternetConnection()) {
            triggerReverseFallback(ReverseFallbackReason.INTERNET_LOST)
        }
    }

    /**
     * Trigger reverse fallback (back to FM)
     */
    private fun triggerReverseFallback(reason: ReverseFallbackReason) {
        scope.launch {
            // Stop online playback
            onlineRadioManager?.stop()

            _state.value = _state.value.copy(
                isFallbackActive = false,
                fallbackReason = null,
                fallbackStation = null
            )

            _isFallbackActive.value = false

            // Resume FM playback at current frequency
            play()
        }
    }

    /**
     * Manually switch to online mode
     */
    fun switchToOnline() {
        triggerOnlineFallback(FallbackReason.USER_REQUEST)
    }

    /**
     * Manually switch to FM mode
     */
    fun switchToFM() {
        if (_state.value.hardwareStatus == FMHardwareStatus.AVAILABLE) {
            triggerReverseFallback(ReverseFallbackReason.USER_REQUEST)
        }
    }

    /**
     * Check internet connection
     */
    private fun hasInternetConnection(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }

    // ============================================
    // PLAYBACK CONTROL
    // ============================================

    /**
     * Start FM playback
     */
    fun play() {
        if (_state.value.hardwareStatus != FMHardwareStatus.AVAILABLE) {
            // Fall back to online if FM unavailable
            if (hasInternetConnection()) {
                triggerOnlineFallback(FallbackReason.FM_UNAVAILABLE)
            } else {
                _state.value = _state.value.copy(
                    error = "FM not available and no internet connection"
                )
            }
            return
        }

        scope.launch {
            if (!requestAudioFocus()) {
                _state.value = _state.value.copy(error = "Could not get audio focus")
                return@launch
            }

            _isPlaying.value = true
            _state.value = _state.value.copy(isPlaying = true)

            // Start signal monitoring
            startSignalMonitoring()

            // Start RDS monitoring
            startRDSMonitoring()
        }
    }

    /**
     * Stop FM playback
     */
    fun stop() {
        scope.launch {
            _isPlaying.value = false
            _state.value = _state.value.copy(isPlaying = false)

            signalMonitorJob?.cancel()
            releaseAudioFocus()

            // Also stop online if in fallback
            if (_state.value.isFallbackActive) {
                onlineRadioManager?.stop()
                _state.value = _state.value.copy(
                    isFallbackActive = false,
                    fallbackReason = null,
                    fallbackStation = null
                )
                _isFallbackActive.value = false
            }
        }
    }

    /**
     * Toggle play/stop
     */
    fun togglePlayback() {
        if (_isPlaying.value) {
            stop()
        } else {
            play()
        }
    }

    /**
     * Set volume
     */
    fun setVolume(volume: Float) {
        _state.value = _state.value.copy(volume = volume.coerceIn(0f, 1f))
    }

    /**
     * Toggle mute
     */
    fun toggleMute() {
        _state.value = _state.value.copy(isMuted = !_state.value.isMuted)
    }

    /**
     * Set audio mode
     */
    fun setAudioMode(mode: FMAudioMode) {
        _state.value = _state.value.copy(audioMode = mode)
    }

    // ============================================
    // RDS
    // ============================================

    /**
     * Start RDS data monitoring
     */
    private fun startRDSMonitoring() {
        scope.launch {
            while (isActive && _state.value.isPlaying && _state.value.rdsEnabled) {
                // Simulate RDS updates
                val rds = simulateRDSData()
                _rdsInfo.value = rds
                _state.value = _state.value.copy(currentRDS = rds)

                delay(5000) // RDS typically updates every few seconds
            }
        }
    }

    /**
     * Simulate RDS data (replace with actual RDS decoding)
     */
    private fun simulateRDSData(): RDSInfo {
        val songs = listOf(
            "Now Playing: Unknown Artist - Unknown Track",
            "Blinding Lights - The Weeknd",
            "Shape of You - Ed Sheeran",
            "Bad Guy - Billie Eilish",
            "Dance Monkey - Tones and I"
        )

        return RDSInfo(
            programService = "FM ${String.format("%.1f", _currentFrequency.value)}",
            radioText = songs.random(),
            programType = 10, // Pop music
            programTypeName = "Pop Music"
        )
    }

    /**
     * Enable/disable RDS
     */
    fun setRDSEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(rdsEnabled = enabled)
    }

    // ============================================
    // PRESETS & FAVORITES
    // ============================================

    /**
     * Save current station as preset
     */
    fun savePreset(slot: Int) {
        val station = _state.value.currentStation?.copy(
            isPreset = true,
            presetSlot = slot
        ) ?: FMStation(
            frequency = _currentFrequency.value,
            isPreset = true,
            presetSlot = slot
        )

        val presets = _state.value.presets.toMutableList()
        val existingIndex = presets.indexOfFirst { it.presetSlot == slot }

        if (existingIndex >= 0) {
            presets[existingIndex] = station
        } else {
            presets.add(station)
        }

        _state.value = _state.value.copy(presets = presets.sortedBy { it.presetSlot })
    }

    /**
     * Load preset
     */
    fun loadPreset(slot: Int) {
        val preset = _state.value.presets.find { it.presetSlot == slot }
        preset?.let { tuneToFrequency(it.frequency) }
    }

    /**
     * Toggle favorite status for current station
     */
    fun toggleFavorite() {
        val station = _state.value.currentStation ?: return
        val favorites = _state.value.favorites.toMutableList()

        val existingIndex = favorites.indexOfFirst {
            kotlin.math.abs(it.frequency - station.frequency) < 0.05f
        }

        if (existingIndex >= 0) {
            favorites.removeAt(existingIndex)
        } else {
            favorites.add(station.copy(isFavorite = true))
        }

        _state.value = _state.value.copy(
            favorites = favorites,
            currentStation = station.copy(isFavorite = existingIndex < 0)
        )
    }

    /**
     * Check if current station is favorite
     */
    fun isCurrentFavorite(): Boolean {
        val station = _state.value.currentStation ?: return false
        return _state.value.favorites.any {
            kotlin.math.abs(it.frequency - station.frequency) < 0.05f
        }
    }

    // ============================================
    // STATION MAPPING
    // ============================================

    /**
     * Add station mapping for online fallback
     */
    fun addStationMapping(frequency: Float, mapping: StationMapping) {
        stationMappings[frequency] = mapping

        // Update current station if it matches
        if (kotlin.math.abs(_currentFrequency.value - frequency) < 0.05f) {
            _state.value.currentStation?.let { station ->
                _state.value = _state.value.copy(
                    currentStation = station.copy(
                        onlineStreamUrl = mapping.onlineStreamUrl,
                        onlineStationId = mapping.onlineStationId,
                        hasOnlineFallback = true
                    )
                )
            }
        }
    }

    /**
     * Remove station mapping
     */
    fun removeStationMapping(frequency: Float) {
        stationMappings.remove(frequency)
    }

    // ============================================
    // SETTINGS
    // ============================================

    /**
     * Set radio mode
     */
    fun setMode(mode: RadioMode) {
        _state.value = _state.value.copy(mode = mode)

        when (mode) {
            RadioMode.FM_OFFLINE -> {
                // Force FM mode, disable auto fallback
                _state.value = _state.value.copy(autoFallbackEnabled = false)
                if (_state.value.isFallbackActive) {
                    switchToFM()
                }
            }
            RadioMode.ONLINE -> {
                // Force online mode
                triggerOnlineFallback(FallbackReason.USER_REQUEST)
            }
            RadioMode.HYBRID_AUTO -> {
                // Enable auto fallback
                _state.value = _state.value.copy(autoFallbackEnabled = true)
            }
        }
    }

    /**
     * Set signal threshold for fallback
     */
    fun setSignalThreshold(threshold: Int) {
        _state.value = _state.value.copy(
            signalThreshold = threshold.coerceIn(10, 70)
        )
    }

    /**
     * Enable/disable noise reduction
     */
    fun setNoiseReduction(enabled: Boolean) {
        _state.value = _state.value.copy(noiseReductionEnabled = enabled)
    }

    /**
     * Set frequency band
     */
    fun setBand(band: FMBand) {
        currentBand = band
        // Re-tune if current frequency is out of range
        if (_currentFrequency.value !in band.minFrequency..band.maxFrequency) {
            tuneToFrequency(band.minFrequency)
        }
    }

    // ============================================
    // AUDIO FOCUS
    // ============================================

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }
                .build()

            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            hasAudioFocus
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                { focusChange -> handleAudioFocusChange(focusChange) },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            hasAudioFocus
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus { }
        }
        hasAudioFocus = false
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Pause playback
                _state.value = _state.value.copy(isMuted = true)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower volume
                setVolume(_state.value.volume * 0.3f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Resume normal playback
                _state.value = _state.value.copy(isMuted = false)
                setVolume(1f)
            }
        }
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        scope.cancel()
        signalMonitorJob?.cancel()
        releaseAudioFocus()

        headphoneReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // Ignore if not registered
            }
        }
    }
}

