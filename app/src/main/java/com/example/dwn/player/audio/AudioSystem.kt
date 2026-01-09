package com.example.dwn.player.audio

import android.bluetooth.BluetoothA2dp
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlin.math.pow

private const val TAG = "AudioSystem"

/**
 * FX ROUTING & SIGNAL FLOW - Feature Set 8
 * PLATFORM & SYSTEM FEATURES - Feature Set 9
 * RELIABILITY & SAFETY - Feature Set 14
 *
 * - Drag-and-drop FX chain
 * - Parallel processing buses
 * - Dry/wet mix per FX
 * - A/B comparison
 * - Device detection
 * - Bluetooth codec awareness
 * - Hearing protection
 * - Hard limiter failsafe
 */

// ============================================
// FX CHAIN / SIGNAL ROUTING
// ============================================

data class FXChainNode(
    val id: String,
    val type: FXType,
    val isEnabled: Boolean = true,
    val isBypassed: Boolean = false,
    val wetDryMix: Float = 1f,
    val inputGain: Float = 0f,         // dB
    val outputGain: Float = 0f,        // dB
    val position: Int = 0
)

enum class FXType(val label: String, val category: FXCategory) {
    // EQ
    GRAPHIC_EQ("Graphic EQ", FXCategory.EQ),
    PARAMETRIC_EQ("Parametric EQ", FXCategory.EQ),

    // Dynamics
    COMPRESSOR("Compressor", FXCategory.DYNAMICS),
    MULTIBAND_COMP("Multiband Compressor", FXCategory.DYNAMICS),
    LIMITER("Limiter", FXCategory.DYNAMICS),
    NOISE_GATE("Noise Gate", FXCategory.DYNAMICS),
    EXPANDER("Expander", FXCategory.DYNAMICS),

    // Spatial
    REVERB("Reverb", FXCategory.SPATIAL),
    DELAY("Delay", FXCategory.SPATIAL),
    STEREO_WIDENER("Stereo Widener", FXCategory.SPATIAL),
    VIRTUALIZER("Virtualizer", FXCategory.SPATIAL),

    // Harmonic
    DISTORTION("Distortion", FXCategory.HARMONIC),
    SATURATION("Saturation", FXCategory.HARMONIC),
    EXCITER("Exciter", FXCategory.HARMONIC),

    // Modulation
    CHORUS("Chorus", FXCategory.MODULATION),
    FLANGER("Flanger", FXCategory.MODULATION),
    PHASER("Phaser", FXCategory.MODULATION),
    TREMOLO("Tremolo", FXCategory.MODULATION),

    // Utility
    GAIN("Gain", FXCategory.UTILITY),
    BASS_BOOST("Bass Boost", FXCategory.UTILITY),
    BITCRUSHER("Bitcrusher", FXCategory.UTILITY)
}

enum class FXCategory(val label: String) {
    EQ("Equalization"),
    DYNAMICS("Dynamics"),
    SPATIAL("Spatial"),
    HARMONIC("Harmonic"),
    MODULATION("Modulation"),
    UTILITY("Utility")
}

data class FXChainState(
    val nodes: List<FXChainNode> = emptyList(),
    val masterInputGain: Float = 0f,
    val masterOutputGain: Float = 0f,
    val isABEnabled: Boolean = false,
    val activeChain: Char = 'A',       // 'A' or 'B'
    val chainB: List<FXChainNode> = emptyList()
)

class FXChainManager {

    private val _state = MutableStateFlow(FXChainState())
    val state: StateFlow<FXChainState> = _state.asStateFlow()

    private var nodeIdCounter = 0

    fun addNode(type: FXType, position: Int? = null): String {
        val id = "fx_${nodeIdCounter++}"
        val pos = position ?: _state.value.nodes.size

        val node = FXChainNode(
            id = id,
            type = type,
            position = pos
        )

        val nodes = _state.value.nodes.toMutableList()
        nodes.add(pos.coerceIn(0, nodes.size), node)

        // Update positions
        nodes.forEachIndexed { index, n ->
            nodes[index] = n.copy(position = index)
        }

        _state.value = _state.value.copy(nodes = nodes)
        return id
    }

    fun removeNode(nodeId: String) {
        val nodes = _state.value.nodes.filter { it.id != nodeId }
            .mapIndexed { index, node -> node.copy(position = index) }
        _state.value = _state.value.copy(nodes = nodes)
    }

    fun moveNode(nodeId: String, newPosition: Int) {
        val nodes = _state.value.nodes.toMutableList()
        val nodeIndex = nodes.indexOfFirst { it.id == nodeId }
        if (nodeIndex < 0) return

        val node = nodes.removeAt(nodeIndex)
        val targetPos = newPosition.coerceIn(0, nodes.size)
        nodes.add(targetPos, node)

        // Update positions
        nodes.forEachIndexed { index, n ->
            nodes[index] = n.copy(position = index)
        }

        _state.value = _state.value.copy(nodes = nodes)
    }

    fun updateNode(nodeId: String, updates: FXChainNode.() -> FXChainNode) {
        val nodes = _state.value.nodes.map { node ->
            if (node.id == nodeId) node.updates() else node
        }
        _state.value = _state.value.copy(nodes = nodes)
    }

    fun setNodeEnabled(nodeId: String, enabled: Boolean) {
        updateNode(nodeId) { copy(isEnabled = enabled) }
    }

    fun setNodeBypassed(nodeId: String, bypassed: Boolean) {
        updateNode(nodeId) { copy(isBypassed = bypassed) }
    }

    fun setNodeWetDry(nodeId: String, mix: Float) {
        updateNode(nodeId) { copy(wetDryMix = mix.coerceIn(0f, 1f)) }
    }

    fun setMasterInputGain(gainDb: Float) {
        _state.value = _state.value.copy(masterInputGain = gainDb.coerceIn(-24f, 24f))
    }

    fun setMasterOutputGain(gainDb: Float) {
        _state.value = _state.value.copy(masterOutputGain = gainDb.coerceIn(-24f, 24f))
    }

    // A/B Comparison
    fun enableABComparison(enabled: Boolean) {
        _state.value = _state.value.copy(isABEnabled = enabled)
    }

    fun switchToChain(chain: Char) {
        if (chain == 'A' || chain == 'B') {
            _state.value = _state.value.copy(activeChain = chain)
        }
    }

    fun copyChainAToB() {
        _state.value = _state.value.copy(chainB = _state.value.nodes.map { it.copy() })
    }

    fun getActiveNodes(): List<FXChainNode> {
        return if (_state.value.isABEnabled && _state.value.activeChain == 'B') {
            _state.value.chainB
        } else {
            _state.value.nodes
        }
    }

    // Preset chain configurations
    fun loadDefaultChain() {
        _state.value = _state.value.copy(nodes = emptyList())
        addNode(FXType.GRAPHIC_EQ)
        addNode(FXType.BASS_BOOST)
        addNode(FXType.VIRTUALIZER)
        addNode(FXType.REVERB)
        addNode(FXType.LIMITER)
    }

    fun clearChain() {
        _state.value = _state.value.copy(nodes = emptyList())
    }
}

// ============================================
// AUDIO DEVICE MANAGER
// ============================================

data class AudioDeviceState(
    val currentDevice: AudioDeviceProfile? = null,
    val availableDevices: List<AudioDeviceProfile> = emptyList(),
    val bluetoothCodec: BluetoothCodec? = null,
    val sampleRate: Int = 44100,
    val bitDepth: Int = 16
)

data class AudioDeviceProfile(
    val id: Int,
    val name: String,
    val type: AudioDeviceType,
    val isActive: Boolean = false,
    val impedance: Int? = null,         // Ohms (if detectable)
    val maxSampleRate: Int = 48000,
    val supportsHighRes: Boolean = false
)

enum class AudioDeviceType(val label: String, val icon: String) {
    SPEAKER("Speaker", "🔊"),
    WIRED_HEADPHONES("Wired Headphones", "🎧"),
    WIRED_HEADSET("Wired Headset", "🎤"),
    BLUETOOTH_A2DP("Bluetooth Audio", "📶"),
    BLUETOOTH_LE("Bluetooth LE", "📶"),
    USB_HEADSET("USB Audio", "🔌"),
    USB_DAC("USB DAC", "🎚️"),
    HDMI("HDMI", "📺"),
    LINE_OUT("Line Out", "➡️"),
    AUX("AUX", "🔗"),
    UNKNOWN("Unknown", "❓")
}

enum class BluetoothCodec(val label: String, val bitrate: String, val quality: Int) {
    SBC("SBC", "328 kbps", 1),
    AAC("AAC", "256 kbps", 2),
    APTX("aptX", "352 kbps", 3),
    APTX_HD("aptX HD", "576 kbps", 4),
    LDAC("LDAC", "990 kbps", 5),
    LC3("LC3", "Variable", 4),
    UNKNOWN("Unknown", "?", 0)
}

class AudioDeviceManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _state = MutableStateFlow(AudioDeviceState())
    val state: StateFlow<AudioDeviceState> = _state.asStateFlow()

    fun refreshDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val profiles = devices.map { device ->
                AudioDeviceProfile(
                    id = device.id,
                    name = device.productName?.toString() ?: getDefaultName(device.type),
                    type = mapDeviceType(device.type),
                    isActive = false,
                    maxSampleRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        device.sampleRates.maxOrNull() ?: 48000
                    } else 48000,
                    supportsHighRes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        device.sampleRates.any { it >= 96000 }
                    } else false
                )
            }

            _state.value = _state.value.copy(availableDevices = profiles)
        }

        detectBluetoothCodec()
    }

    private fun mapDeviceType(androidType: Int): AudioDeviceType {
        return when (androidType) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioDeviceType.SPEAKER
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioDeviceType.WIRED_HEADPHONES
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> AudioDeviceType.WIRED_HEADSET
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AudioDeviceType.BLUETOOTH_A2DP
            AudioDeviceInfo.TYPE_USB_HEADSET -> AudioDeviceType.USB_HEADSET
            AudioDeviceInfo.TYPE_USB_DEVICE -> AudioDeviceType.USB_DAC
            AudioDeviceInfo.TYPE_HDMI -> AudioDeviceType.HDMI
            AudioDeviceInfo.TYPE_LINE_ANALOG -> AudioDeviceType.LINE_OUT
            AudioDeviceInfo.TYPE_AUX_LINE -> AudioDeviceType.AUX
            else -> AudioDeviceType.UNKNOWN
        }
    }

    private fun getDefaultName(type: Int): String {
        return mapDeviceType(type).label
    }

    private fun detectBluetoothCodec() {
        // Bluetooth codec detection requires system permissions on most devices
        // This is a simplified placeholder
        _state.value = _state.value.copy(bluetoothCodec = null)
    }

    fun getCurrentDevice(): AudioDeviceProfile? {
        return _state.value.currentDevice ?: _state.value.availableDevices.firstOrNull()
    }
}

// ============================================
// HEARING PROTECTION & SAFETY
// ============================================

data class SafetySettings(
    val hearingProtectionEnabled: Boolean = true,
    val maxVolumeDb: Float = 85f,          // Safe listening level
    val warningVolumeDb: Float = 80f,
    val exposureTimeMinutes: Int = 480,     // 8 hours at 85dB
    val hardLimiterEnabled: Boolean = true,
    val hardLimiterCeiling: Float = -0.1f,  // dBFS
    val autoVolumeRollback: Boolean = true,
    val crashSafeEnabled: Boolean = true
)

data class SafetyState(
    val settings: SafetySettings = SafetySettings(),
    val currentExposure: Float = 0f,        // Cumulative exposure
    val isWarningActive: Boolean = false,
    val isLimiting: Boolean = false,
    val peakLevel: Float = -Float.MAX_VALUE,
    val sessionDuration: Long = 0           // ms
)

class SafetyManager {

    private val _state = MutableStateFlow(SafetyState())
    val state: StateFlow<SafetyState> = _state.asStateFlow()

    private var sessionStartTime = System.currentTimeMillis()
    private var cumulativeDose = 0f

    fun updateSettings(
        hearingProtection: Boolean? = null,
        maxVolumeDb: Float? = null,
        warningVolumeDb: Float? = null,
        exposureTime: Int? = null,
        hardLimiter: Boolean? = null,
        limiterCeiling: Float? = null,
        autoRollback: Boolean? = null,
        crashSafe: Boolean? = null
    ) {
        val current = _state.value.settings
        _state.value = _state.value.copy(
            settings = current.copy(
                hearingProtectionEnabled = hearingProtection ?: current.hearingProtectionEnabled,
                maxVolumeDb = maxVolumeDb?.coerceIn(60f, 100f) ?: current.maxVolumeDb,
                warningVolumeDb = warningVolumeDb?.coerceIn(60f, 95f) ?: current.warningVolumeDb,
                exposureTimeMinutes = exposureTime?.coerceIn(60, 960) ?: current.exposureTimeMinutes,
                hardLimiterEnabled = hardLimiter ?: current.hardLimiterEnabled,
                hardLimiterCeiling = limiterCeiling?.coerceIn(-6f, 0f) ?: current.hardLimiterCeiling,
                autoVolumeRollback = autoRollback ?: current.autoVolumeRollback,
                crashSafeEnabled = crashSafe ?: current.crashSafeEnabled
            )
        )
    }

    fun processLevel(levelDb: Float) {
        val settings = _state.value.settings

        // Update peak
        if (levelDb > _state.value.peakLevel) {
            _state.value = _state.value.copy(peakLevel = levelDb)
        }

        // Update session duration
        val currentTime = System.currentTimeMillis()
        val sessionDuration = currentTime - sessionStartTime

        // Calculate exposure dose (simplified)
        // Based on NIOSH criteria: halve allowed time for every 3dB increase
        if (levelDb > 70f) {
            val timeWeight = 2.0.pow(((levelDb - 85) / 3).toDouble()).toFloat()
            cumulativeDose += timeWeight / 60f // Per minute
        }

        val warningActive = levelDb >= settings.warningVolumeDb
        val isLimiting = settings.hardLimiterEnabled && levelDb >= settings.maxVolumeDb

        _state.value = _state.value.copy(
            currentExposure = cumulativeDose,
            isWarningActive = warningActive,
            isLimiting = isLimiting,
            sessionDuration = sessionDuration
        )
    }

    fun applyHardLimit(sample: Float): Float {
        val settings = _state.value.settings
        if (!settings.hardLimiterEnabled) return sample

        val ceiling = 10.0.pow((settings.hardLimiterCeiling / 20).toDouble()).toFloat()
        return sample.coerceIn(-ceiling, ceiling)
    }

    fun resetSession() {
        sessionStartTime = System.currentTimeMillis()
        cumulativeDose = 0f
        _state.value = _state.value.copy(
            currentExposure = 0f,
            peakLevel = -Float.MAX_VALUE,
            sessionDuration = 0
        )
    }

    fun getExposurePercentage(): Float {
        val maxExposure = _state.value.settings.exposureTimeMinutes.toFloat()
        return (cumulativeDose / maxExposure * 100).coerceIn(0f, 100f)
    }

    fun shouldShowWarning(): Boolean {
        return _state.value.isWarningActive || getExposurePercentage() > 80f
    }
}

// ============================================
// UNDO/REDO HISTORY
// ============================================

data class AudioState(
    val timestamp: Long = System.currentTimeMillis(),
    val eqBands: Map<Int, Float> = emptyMap(),
    val effects: Map<String, Any> = emptyMap(),
    val presetId: String? = null
)

class UndoRedoManager(private val maxHistory: Int = 50) {

    private val undoStack = mutableListOf<AudioState>()
    private val redoStack = mutableListOf<AudioState>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    fun saveState(state: AudioState) {
        undoStack.add(state)
        if (undoStack.size > maxHistory) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateFlags()
    }

    fun undo(): AudioState? {
        if (undoStack.size <= 1) return null

        val current = undoStack.removeLast()
        redoStack.add(current)
        updateFlags()

        return undoStack.lastOrNull()
    }

    fun redo(): AudioState? {
        if (redoStack.isEmpty()) return null

        val state = redoStack.removeLast()
        undoStack.add(state)
        updateFlags()

        return state
    }

    private fun updateFlags() {
        _canUndo.value = undoStack.size > 1
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateFlags()
    }
}

