package com.example.dwn.player.audio

import android.content.Context
import android.media.audiofx.*
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private const val TAG = "SuperEqualizer"

/**
 * SUPER EQUALIZER & FX - MASTER CONTROLLER
 *
 * Unified API for all audio processing features:
 * 1. Core Audio Engine
 * 2. Equalization System (Graphic, Parametric, Advanced)
 * 3. Dynamics Processing (Compressor, Limiter, Gate, Expander)
 * 4. Spatial & Time FX (Reverb, Delay, Stereo, Virtualizer)
 * 5. Harmonic & Modulation FX
 * 6. AI / Smart Processing
 * 7. Analysis & Visualization
 * 8. FX Routing & Signal Flow
 * 9. Platform & System Features
 * 10. Presets & Data
 * 11. UI/UX (handled in UI layer)
 * 12. Special Modes
 * 13. Developer Features (Debug, Profiling)
 * 14. Reliability & Safety
 */

data class SuperEqualizerState(
    val isEnabled: Boolean = false,
    val isInitialized: Boolean = false,

    // Current levels
    val currentPreset: AudioPreset? = null,
    val specialMode: SpecialMode? = null,

    // Meters
    val levelMeter: LevelMeterData = LevelMeterData(),
    val spectrumData: SpectrumData = SpectrumData(),
    val stereoField: StereoFieldData = StereoFieldData(),

    // Analysis
    val audioAnalysis: AudioAnalysis = AudioAnalysis(),

    // Safety
    val safetyState: SafetyState = SafetyState(),

    // System
    val audioDevice: AudioDeviceProfile? = null,
    val bluetoothCodec: BluetoothCodec? = null,
    val latencyMs: Float = 0f,
    val cpuLoad: Float = 0f
)

class SuperEqualizer(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Core Systems
    private val audioEngine = AudioEngine(context)

    // Equalization (Feature 2)
    val graphicEQ = GraphicEqualizer(context)
    val parametricEQ = ParametricEqualizer()
    val advancedEQ = AdvancedEQ()

    // Dynamics (Feature 3)
    val compressor = Compressor()
    val multibandCompressor = MultibandCompressor()
    val limiter = Limiter()
    val noiseGate = NoiseGate()
    val expander = Expander()

    // Spatial FX (Feature 4)
    val reverb = ReverbProcessor(context)
    val delay = DelayProcessor()
    val stereoWidener = StereoWidener()
    val virtualizer = VirtualizerProcessor(context)
    val haasEffect = HaasEffect()
    val bassBoost = BassBoostProcessor(context)
    val loudnessEnhancer = LoudnessEnhancerProcessor(context)

    // Harmonic FX (Feature 5)
    val distortion = Distortion()
    val saturation = Saturation()
    val exciter = Exciter()
    val chorus = Chorus()
    val flanger = Flanger()
    val phaser = Phaser()
    val tremolo = Tremolo()
    val autoPan = AutoPan()
    val bitcrusher = Bitcrusher()

    // Analysis (Features 6 & 7)
    val spectrumAnalyzer = SpectrumAnalyzer(context)
    val levelMeter = LevelMeter()
    val stereoFieldAnalyzer = StereoFieldAnalyzer()
    val smartProcessor = SmartAudioProcessor()
    val loudnessNormalizer = LoudnessNormalizer()

    // Routing (Feature 8)
    val fxChain = FXChainManager()
    val undoRedo = UndoRedoManager()

    // System (Feature 9)
    val deviceManager = AudioDeviceManager(context)

    // Presets (Feature 10)
    val presetManager = PresetManager(context)

    // Special Modes (Feature 12)
    val specialModeProcessor = SpecialModeProcessor()

    // Safety (Feature 14)
    val safetyManager = SafetyManager()

    // Master state
    private val _state = MutableStateFlow(SuperEqualizerState())
    val state: StateFlow<SuperEqualizerState> = _state.asStateFlow()

    private var audioSessionId: Int = 0
    private var isInitialized = false

    // ============================================
    // INITIALIZATION
    // ============================================

    fun initialize(sessionId: Int) {
        if (isInitialized && audioSessionId == sessionId) return

        try {
            audioSessionId = sessionId

            // Initialize audio engine
            audioEngine.initialize(sessionId)

            // Initialize EQ
            graphicEQ.initialize(sessionId)

            // Initialize spatial FX
            reverb.initialize(sessionId)
            virtualizer.initialize(sessionId)
            bassBoost.initialize(sessionId)
            loudnessEnhancer.initialize(sessionId)

            // Initialize analysis
            spectrumAnalyzer.initialize(sessionId)

            // Initialize multiband compressor
            multibandCompressor.initialize(4)

            // Load default FX chain
            fxChain.loadDefaultChain()

            // Refresh device info
            deviceManager.refreshDevices()

            // Load last preset
            presetManager.currentPreset.value?.let { applyPreset(it) }

            isInitialized = true
            _state.value = _state.value.copy(isInitialized = true)

            // Start monitoring
            startMonitoring()

            Log.d(TAG, "SuperEqualizer initialized - Session: $sessionId")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SuperEqualizer", e)
        }
    }

    private fun startMonitoring() {
        // Collect spectrum data
        scope.launch {
            spectrumAnalyzer.spectrumData.collect { data ->
                _state.value = _state.value.copy(spectrumData = data)

                // Feed to smart processor for analysis
                if (smartProcessor.autoEQEnabled.value) {
                    smartProcessor.analyzeAudio(data)
                }
            }
        }

        // Collect level meter data
        scope.launch {
            levelMeter.data.collect { data ->
                _state.value = _state.value.copy(levelMeter = data)

                // Feed to safety manager
                val avgLevel = (data.peakL + data.peakR) / 2
                safetyManager.processLevel(avgLevel)
            }
        }

        // Collect stereo field data
        scope.launch {
            stereoFieldAnalyzer.data.collect { data ->
                _state.value = _state.value.copy(stereoField = data)
            }
        }

        // Collect smart analysis
        scope.launch {
            smartProcessor.analysis.collect { analysis ->
                _state.value = _state.value.copy(audioAnalysis = analysis)
            }
        }

        // Collect safety state
        scope.launch {
            safetyManager.state.collect { safety ->
                _state.value = _state.value.copy(safetyState = safety)
            }
        }

        // Collect CPU load
        scope.launch {
            audioEngine.cpuLoad.collect { load ->
                _state.value = _state.value.copy(cpuLoad = load)
            }
        }

        // Collect latency
        scope.launch {
            audioEngine.latencyMs.collect { latency ->
                _state.value = _state.value.copy(latencyMs = latency)
            }
        }
    }

    // ============================================
    // MASTER CONTROLS
    // ============================================

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isEnabled = enabled)
        graphicEQ.setEnabled(enabled)
        spectrumAnalyzer.setEnabled(enabled)
    }

    fun applyPreset(preset: AudioPreset) {
        // Save current state for undo
        saveStateForUndo()

        // Apply EQ bands
        preset.eqBands.forEach { (freq, gain) ->
            val bandIndex = graphicEQ.state.value.bands.indexOfFirst {
                it.frequency == freq || kotlin.math.abs(it.frequency - freq) < 50
            }
            if (bandIndex >= 0) {
                graphicEQ.setBandGain(bandIndex, gain)
            }
        }

        // Apply preamp
        graphicEQ.setPreampGain(preset.preampGain)

        // Apply bass boost (uses Android's BassBoost)
        if (preset.bassBoost > 0) {
            bassBoost.updateSettings(
                enabled = true,
                strength = preset.bassBoost
            )
        } else {
            bassBoost.updateSettings(enabled = false)
        }

        // Apply virtualizer
        if (preset.virtualizer > 0) {
            virtualizer.updateSettings(
                enabled = true,
                strength = preset.virtualizer
            )
        } else {
            virtualizer.updateSettings(enabled = false)
        }

        // Apply reverb preset
        if (preset.reverbPreset != "None") {
            val reverbPresetEnum = ReverbPreset.entries.find { it.label == preset.reverbPreset }
            if (reverbPresetEnum != null) {
                reverb.setPreset(reverbPresetEnum)
                reverb.updateSettings(enabled = true)
            }
        } else {
            reverb.updateSettings(enabled = false)
        }

        // Apply compressor if enabled in preset
        if (preset.compressorEnabled) {
            compressor.updateSettings(enabled = true, threshold = -20f, ratio = 4f)
        }

        // Apply limiter if enabled in preset
        if (preset.limiterEnabled) {
            limiter.updateSettings(enabled = true, ceiling = -0.3f)
        }

        // Apply stereo width
        if (preset.stereoWidth != 1f) {
            stereoWidener.updateSettings(enabled = true, width = preset.stereoWidth)
        }

        // Apply special mode if present
        preset.specialMode?.let { activateSpecialMode(it) }

        // Update state
        presetManager.selectPreset(preset)
        _state.value = _state.value.copy(
            currentPreset = preset,
            specialMode = preset.specialMode
        )

        Log.d(TAG, "Applied preset: ${preset.name}")
    }

    fun activateSpecialMode(mode: SpecialMode?) {
        specialModeProcessor.activateMode(mode)
        _state.value = _state.value.copy(specialMode = mode)

        if (mode == null) {
            // Reset to default state
            Log.d(TAG, "Deactivated special mode")
            return
        }

        val settings = specialModeProcessor.getModeSettings(mode)
        Log.d(TAG, "Activating special mode: ${mode.label} with settings: $settings")

        // Apply mode-specific settings
        when (mode) {
            SpecialMode.PODCAST -> {
                // Voice clarity and compression
                compressor.updateSettings(
                    enabled = true,
                    threshold = settings["compressorThreshold"] as? Float ?: -20f,
                    ratio = settings["compressorRatio"] as? Float ?: 3f,
                    attack = 10f,
                    release = 100f
                )
                // Reduce bass, boost mids for voice clarity
                graphicEQ.state.value.bands.forEachIndexed { index, band ->
                    val gain = when {
                        band.frequency < 150 -> -3f
                        band.frequency in 500..4000 -> 3f
                        else -> 0f
                    }
                    graphicEQ.setBandGain(index, gain)
                }
            }
            SpecialMode.GAMING -> {
                // Enhanced spatial audio for footsteps
                virtualizer.updateSettings(
                    enabled = true,
                    strength = settings["virtualizerStrength"] as? Int ?: 800
                )
                // Boost frequencies for footsteps and gunshots
                graphicEQ.state.value.bands.forEachIndexed { index, band ->
                    val gain = when {
                        band.frequency in 60..100 -> 2f
                        band.frequency in 2000..3000 -> 4f
                        band.frequency in 6000..10000 -> 3f
                        else -> 0f
                    }
                    graphicEQ.setBandGain(index, gain)
                }
            }
            SpecialMode.CALL -> {
                // Voice enhancement
                compressor.updateSettings(enabled = true, threshold = -25f, ratio = 2f)
                graphicEQ.state.value.bands.forEachIndexed { index, band ->
                    val gain = when {
                        band.frequency < 100 -> -6f  // Reduce rumble
                        band.frequency in 1000..4000 -> 4f  // Boost voice
                        else -> 0f
                    }
                    graphicEQ.setBandGain(index, gain)
                }
            }
            SpecialMode.KARAOKE -> {
                // Reduce center channel (vocal removal effect)
                stereoWidener.updateSettings(enabled = true, width = 2f, midGain = -12f)
                reverb.updateSettings(enabled = true, roomSize = 0.6f, wetDryMix = 0.3f)
                reverb.setPreset(ReverbPreset.MEDIUM_HALL)
            }
            SpecialMode.MUSIC_MASTERING -> {
                // Professional mastering chain
                compressor.updateSettings(enabled = true, threshold = -18f, ratio = 3f)
                limiter.updateSettings(enabled = true, ceiling = -0.3f)
                loudnessEnhancer.updateSettings(enabled = true, gainMb = 300)
            }
            SpecialMode.NIGHT_MODE -> {
                // Compress dynamics for quiet listening
                compressor.updateSettings(
                    enabled = true,
                    threshold = settings["compressorThreshold"] as? Float ?: -30f,
                    ratio = settings["compressorRatio"] as? Float ?: 6f
                )
                limiter.updateSettings(enabled = true, ceiling = -6f)
                // Reduce harsh highs
                graphicEQ.state.value.bands.forEachIndexed { index, band ->
                    if (band.frequency > 8000) {
                        graphicEQ.setBandGain(index, -3f)
                    }
                }
            }
            SpecialMode.WORKOUT -> {
                // Heavy bass boost for energy
                bassBoost.updateSettings(enabled = true, strength = 800)
                loudnessEnhancer.updateSettings(enabled = true, gainMb = 500)
                graphicEQ.state.value.bands.forEachIndexed { index, band ->
                    val gain = when {
                        band.frequency < 100 -> 6f
                        band.frequency in 100..300 -> 4f
                        else -> 0f
                    }
                    graphicEQ.setBandGain(index, gain)
                }
            }
            SpecialMode.MEDITATION -> {
                // Smooth, warm sound
                reverb.updateSettings(enabled = true, roomSize = 0.8f, wetDryMix = 0.4f)
                reverb.setPreset(ReverbPreset.LARGE_HALL)
                graphicEQ.state.value.bands.forEachIndexed { index, band ->
                    val gain = when {
                        band.frequency < 100 -> 2f
                        band.frequency > 10000 -> -3f
                        else -> 0f
                    }
                    graphicEQ.setBandGain(index, gain)
                }
            }
            SpecialMode.SLEEP -> {
                // Very soft, muted sound
                limiter.updateSettings(enabled = true, ceiling = -12f)
                graphicEQ.state.value.bands.forEachIndexed { index, band ->
                    val gain = when {
                        band.frequency < 100 -> 1f
                        band.frequency in 3000..6000 -> -4f
                        band.frequency > 10000 -> -6f
                        else -> 0f
                    }
                    graphicEQ.setBandGain(index, gain)
                }
            }
        }
    }

    // ============================================
    // QUICK ACTIONS
    // ============================================

    fun resetAll() {
        saveStateForUndo()

        graphicEQ.resetToFlat()
        parametricEQ.clearAllBands()

        // Reset dynamics
        compressor.updateSettings(enabled = false)
        limiter.updateSettings(enabled = false)
        noiseGate.updateSettings(enabled = false)

        // Reset spatial
        reverb.updateSettings(enabled = false)
        delay.updateSettings(enabled = false)
        virtualizer.updateSettings(enabled = false, strength = 0)
        stereoWidener.updateSettings(enabled = false)
        bassBoost.updateSettings(enabled = false, strength = 0)
        loudnessEnhancer.updateSettings(enabled = false, gainMb = 0)

        // Reset harmonic
        distortion.updateSettings(enabled = false)
        saturation.updateSettings(enabled = false)
        chorus.updateSettings(enabled = false)

        specialModeProcessor.activateMode(null)

        _state.value = _state.value.copy(
            currentPreset = null,
            specialMode = null
        )

        Log.d(TAG, "Reset all effects")
    }

    fun enableAutoEQ(enabled: Boolean) {
        smartProcessor.setAutoEQEnabled(enabled)

        if (enabled) {
            // Apply suggested EQ curve
            val suggestedCurve = smartProcessor.getSuggestedEQCurve()
            suggestedCurve.forEach { (freq, gain) ->
                val bandIndex = graphicEQ.state.value.bands.indexOfFirst {
                    kotlin.math.abs(it.frequency - freq.toInt()) < 100
                }
                if (bandIndex >= 0) {
                    graphicEQ.setBandGain(bandIndex, gain)
                }
            }
        }
    }

    // ============================================
    // UNDO/REDO
    // ============================================

    private fun saveStateForUndo() {
        val currentState = AudioState(
            eqBands = graphicEQ.state.value.bands.associate { it.frequency to it.gain },
            presetId = _state.value.currentPreset?.id
        )
        undoRedo.saveState(currentState)
    }

    fun undo() {
        undoRedo.undo()?.let { state ->
            state.eqBands.forEach { (freq, gain) ->
                val bandIndex = graphicEQ.state.value.bands.indexOfFirst { it.frequency == freq }
                if (bandIndex >= 0) {
                    graphicEQ.setBandGain(bandIndex, gain)
                }
            }
        }
    }

    fun redo() {
        undoRedo.redo()?.let { state ->
            state.eqBands.forEach { (freq, gain) ->
                val bandIndex = graphicEQ.state.value.bands.indexOfFirst { it.frequency == freq }
                if (bandIndex >= 0) {
                    graphicEQ.setBandGain(bandIndex, gain)
                }
            }
        }
    }

    // ============================================
    // EXPORT/IMPORT
    // ============================================

    fun exportCurrentSettings(): String {
        val preset = _state.value.currentPreset ?: AudioPreset(
            id = "export_${System.currentTimeMillis()}",
            name = "Exported Settings",
            category = PresetCategory.CUSTOM,
            isCustom = true,
            eqBands = graphicEQ.state.value.bands.associate { it.frequency to it.gain },
            preampGain = graphicEQ.state.value.preampGain
        )
        return presetManager.exportPreset(preset)
    }

    fun importSettings(json: String): Boolean {
        return presetManager.importPreset(json)?.let { preset ->
            applyPreset(preset)
            true
        } ?: false
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        scope.cancel()

        graphicEQ.release()
        reverb.release()
        virtualizer.release()
        bassBoost.release()
        loudnessEnhancer.release()
        spectrumAnalyzer.release()
        audioEngine.release()

        isInitialized = false
        _state.value = SuperEqualizerState()

        Log.d(TAG, "SuperEqualizer released")
    }

    // ============================================
    // DEBUG / DEVELOPER FEATURES (Feature 13)
    // ============================================

    fun getDebugInfo(): Map<String, Any> {
        return mapOf(
            "audioSessionId" to audioSessionId,
            "isInitialized" to isInitialized,
            "cpuLoad" to _state.value.cpuLoad,
            "latencyMs" to _state.value.latencyMs,
            "fxChainNodes" to fxChain.state.value.nodes.size,
            "activePreset" to (_state.value.currentPreset?.name ?: "None"),
            "specialMode" to (_state.value.specialMode?.label ?: "None"),
            "graphicEQBands" to graphicEQ.state.value.bands.size,
            "graphicEQMode" to graphicEQ.state.value.mode.label,
            "parametricEQBands" to parametricEQ.state.value.bands.size,
            "safetyExposure" to safetyManager.getExposurePercentage(),
            "peakLevel" to _state.value.levelMeter.peakL,
            "lufs" to _state.value.levelMeter.lufs
        )
    }

    fun logPerformanceMetrics() {
        val metrics = """
            |=== SuperEqualizer Performance ===
            |CPU Load: ${(_state.value.cpuLoad * 100).toInt()}%
            |Latency: ${_state.value.latencyMs}ms
            |Active FX: ${fxChain.state.value.nodes.count { it.isEnabled }}
            |Peak Level: ${_state.value.levelMeter.peakL} dB
            |LUFS: ${_state.value.levelMeter.lufs}
            |Safety Exposure: ${safetyManager.getExposurePercentage()}%
            |================================
        """.trimMargin()

        Log.d(TAG, metrics)
    }
}

