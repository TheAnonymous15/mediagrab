package com.example.dwn.player.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * ============================================
 * MULTI-LAYER AUDIO MODULE
 * ============================================
 *
 * Supports multiple simultaneous audio tracks with:
 * - Per-layer volume control
 * - Per-layer pan control (stereo positioning)
 * - Mute/Solo functionality
 * - Individual FX chains per layer
 *
 * Use cases:
 * - Practice sessions (play along with backing track)
 * - Ambient mixing
 * - Podcast overlays
 * - Remix previews
 */
class MultiLayerAudioModule(private val context: Context) {

    companion object {
        const val MAX_LAYERS = 8
    }

    private val layers = mutableMapOf<String, AudioLayer>()

    private val _layersState = MutableStateFlow<List<AudioLayerState>>(emptyList())
    val layersState: StateFlow<List<AudioLayerState>> = _layersState.asStateFlow()

    private var masterVolume = 1f

    // ============================================
    // LAYER MANAGEMENT
    // ============================================

    /**
     * Create a new audio layer
     * @return Layer ID or empty string if max layers reached
     */
    fun createLayer(name: String = "Layer ${layers.size + 1}"): String {
        if (layers.size >= MAX_LAYERS) {
            return ""
        }

        val layerId = UUID.randomUUID().toString()
        val layer = AudioLayer(
            id = layerId,
            name = name,
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
            },
            volume = 1f,
            pan = 0f,
            isMuted = false,
            isSolo = false
        )

        layers[layerId] = layer
        updateState()
        return layerId
    }

    /**
     * Remove a layer
     */
    fun removeLayer(layerId: String) {
        layers.remove(layerId)?.player?.release()
        updateState()
    }

    /**
     * Remove all layers
     */
    fun removeAllLayers() {
        layers.values.forEach { it.player.release() }
        layers.clear()
        updateState()
    }

    /**
     * Get layer count
     */
    fun getLayerCount(): Int = layers.size

    /**
     * Get layer by ID
     */
    fun getLayer(layerId: String): AudioLayerState? {
        return layers[layerId]?.toState()
    }

    // ============================================
    // MEDIA LOADING
    // ============================================

    /**
     * Load media into a layer
     */
    fun loadMedia(layerId: String, uri: Uri, onPrepared: ((Boolean) -> Unit)? = null) {
        val layer = layers[layerId] ?: return

        try {
            layer.player.reset()
            layer.player.setDataSource(context, uri)
            layer.player.setOnPreparedListener {
                layer.isPrepared = true
                updateState()
                onPrepared?.invoke(true)
            }
            layer.player.setOnErrorListener { _, _, _ ->
                layer.isPrepared = false
                updateState()
                onPrepared?.invoke(false)
                true
            }
            layer.player.prepareAsync()
        } catch (e: Exception) {
            onPrepared?.invoke(false)
        }
    }

    /**
     * Load media from file path
     */
    fun loadMedia(layerId: String, filePath: String, onPrepared: ((Boolean) -> Unit)? = null) {
        loadMedia(layerId, Uri.parse(filePath), onPrepared)
    }

    // ============================================
    // PLAYBACK CONTROL
    // ============================================

    /**
     * Play a specific layer
     */
    fun playLayer(layerId: String) {
        val layer = layers[layerId] ?: return
        if (layer.isPrepared && !layer.player.isPlaying && !layer.isMuted) {
            layer.player.start()
            updateState()
        }
    }

    /**
     * Pause a specific layer
     */
    fun pauseLayer(layerId: String) {
        val layer = layers[layerId] ?: return
        if (layer.player.isPlaying) {
            layer.player.pause()
            updateState()
        }
    }

    /**
     * Stop a specific layer
     */
    fun stopLayer(layerId: String) {
        val layer = layers[layerId] ?: return
        layer.player.stop()
        layer.isPrepared = false
        updateState()
    }

    /**
     * Seek a specific layer
     */
    fun seekLayer(layerId: String, positionMs: Long) {
        val layer = layers[layerId] ?: return
        if (layer.isPrepared) {
            layer.player.seekTo(positionMs.toInt())
            updateState()
        }
    }

    /**
     * Play all layers simultaneously
     */
    fun playAllLayers() {
        layers.values
            .filter { it.isPrepared && !it.isMuted && !it.player.isPlaying }
            .forEach { it.player.start() }
        updateState()
    }

    /**
     * Pause all layers
     */
    fun pauseAllLayers() {
        layers.values
            .filter { it.player.isPlaying }
            .forEach { it.player.pause() }
        updateState()
    }

    /**
     * Stop all layers
     */
    fun stopAllLayers() {
        layers.values.forEach { layer ->
            layer.player.stop()
            layer.isPrepared = false
        }
        updateState()
    }

    /**
     * Seek all layers to same position (synchronized)
     */
    fun seekAllLayers(positionMs: Long) {
        layers.values
            .filter { it.isPrepared }
            .forEach { it.player.seekTo(positionMs.toInt()) }
        updateState()
    }

    // ============================================
    // VOLUME & PAN CONTROL
    // ============================================

    /**
     * Set master volume for all layers
     */
    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
        layers.values.forEach { applyLayerVolume(it) }
    }

    /**
     * Set volume for a specific layer (0.0 to 1.0)
     */
    fun setLayerVolume(layerId: String, volume: Float) {
        val layer = layers[layerId] ?: return
        layer.volume = volume.coerceIn(0f, 1f)
        applyLayerVolume(layer)
        updateState()
    }

    /**
     * Set pan for a specific layer (-1.0 = left, 0.0 = center, 1.0 = right)
     */
    fun setLayerPan(layerId: String, pan: Float) {
        val layer = layers[layerId] ?: return
        layer.pan = pan.coerceIn(-1f, 1f)
        applyLayerVolume(layer)
        updateState()
    }

    private fun applyLayerVolume(layer: AudioLayer) {
        if (layer.isMuted) {
            layer.player.setVolume(0f, 0f)
            return
        }

        val effectiveVolume = layer.volume * masterVolume

        // Apply pan using equal-power panning
        val panAngle = (layer.pan + 1f) * 0.5f * (Math.PI / 2f)
        val leftVol = effectiveVolume * kotlin.math.cos(panAngle).toFloat()
        val rightVol = effectiveVolume * kotlin.math.sin(panAngle).toFloat()

        layer.player.setVolume(leftVol, rightVol)
    }

    // ============================================
    // MUTE & SOLO
    // ============================================

    /**
     * Mute/unmute a layer
     */
    fun setLayerMute(layerId: String, muted: Boolean) {
        val layer = layers[layerId] ?: return
        layer.isMuted = muted
        applyLayerVolume(layer)
        updateState()
    }

    /**
     * Toggle mute for a layer
     */
    fun toggleLayerMute(layerId: String) {
        val layer = layers[layerId] ?: return
        setLayerMute(layerId, !layer.isMuted)
    }

    /**
     * Solo a layer (mute all others)
     */
    fun setLayerSolo(layerId: String, solo: Boolean) {
        val layer = layers[layerId] ?: return
        layer.isSolo = solo

        if (solo) {
            // Mute all other layers
            layers.values.filter { it.id != layerId }.forEach { other ->
                other.player.setVolume(0f, 0f)
            }
            // Ensure this layer is audible
            applyLayerVolume(layer)
        } else {
            // Restore all layers based on their mute state
            layers.values.forEach { applyLayerVolume(it) }
        }
        updateState()
    }

    /**
     * Toggle solo for a layer
     */
    fun toggleLayerSolo(layerId: String) {
        val layer = layers[layerId] ?: return
        setLayerSolo(layerId, !layer.isSolo)
    }

    /**
     * Clear all solos
     */
    fun clearAllSolos() {
        layers.values.forEach { layer ->
            layer.isSolo = false
            applyLayerVolume(layer)
        }
        updateState()
    }

    /**
     * Mute all layers
     */
    fun muteAllLayers() {
        layers.values.forEach { layer ->
            layer.isMuted = true
            layer.player.setVolume(0f, 0f)
        }
        updateState()
    }

    /**
     * Unmute all layers
     */
    fun unmuteAllLayers() {
        layers.values.forEach { layer ->
            layer.isMuted = false
            applyLayerVolume(layer)
        }
        updateState()
    }

    // ============================================
    // LAYER FX
    // ============================================

    /**
     * Enable/disable FX chain for a layer
     */
    fun setLayerFXEnabled(layerId: String, enabled: Boolean) {
        val layer = layers[layerId] ?: return
        layer.fxChainEnabled = enabled
        updateState()
    }

    /**
     * Get audio session ID for a layer (for FX attachment)
     */
    fun getLayerAudioSessionId(layerId: String): Int {
        return layers[layerId]?.player?.audioSessionId ?: 0
    }

    // ============================================
    // STATE
    // ============================================

    private fun updateState() {
        _layersState.value = layers.values.map { it.toState() }
    }

    private fun AudioLayer.toState(): AudioLayerState {
        return AudioLayerState(
            id = id,
            name = name,
            volume = volume,
            pan = pan,
            isMuted = isMuted,
            isSolo = isSolo,
            isPlaying = player.isPlaying,
            isPrepared = isPrepared,
            position = if (isPrepared) player.currentPosition.toLong() else 0,
            duration = if (isPrepared) player.duration.toLong() else 0,
            fxChainEnabled = fxChainEnabled
        )
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        removeAllLayers()
    }
}

