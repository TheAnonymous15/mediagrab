package com.example.dwn.podcast.streaming

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import android.view.SurfaceView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

private const val TAG = "RTMPStreamer"

// ============================================
// STREAM CONFIGURATION
// ============================================

data class StreamConfig(
    val rtmpUrl: String,
    val streamKey: String,
    val videoWidth: Int = 1280,
    val videoHeight: Int = 720,
    val videoBitrate: Int = 2500000,
    val videoFps: Int = 30,
    val audioBitrate: Int = 128000,
    val audioSampleRate: Int = 44100,
    val audioChannels: Int = 2,
    val enableVideo: Boolean = true,
    val enableAudio: Boolean = true
)

data class StreamStats(
    val isStreaming: Boolean = false,
    val duration: Long = 0,
    val bitrate: Int = 0,
    val fps: Float = 0f,
    val droppedFrames: Int = 0,
    val sentBytes: Long = 0,
    val viewerCount: Int = 0,
    val health: StreamHealth = StreamHealth.GOOD
)

enum class StreamHealth {
    EXCELLENT, GOOD, FAIR, POOR, DISCONNECTED
}

sealed class StreamEvent {
    data object Connected : StreamEvent()
    data object Disconnected : StreamEvent()
    data class Error(val message: String) : StreamEvent()
    data class BitrateChanged(val bitrate: Int) : StreamEvent()
    data class ViewersChanged(val count: Int) : StreamEvent()
}

// ============================================
// RTMP STREAMER (Simplified implementation)
// ============================================

class PodcastRTMPStreamer(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _stats = MutableStateFlow(StreamStats())
    val stats: StateFlow<StreamStats> = _stats.asStateFlow()

    private val _events = MutableStateFlow<StreamEvent?>(null)
    val events: StateFlow<StreamEvent?> = _events.asStateFlow()

    private var streamConfig: StreamConfig? = null
    private var streamJob: Job? = null
    private var startTime: Long = 0

    // Camera
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Encoders
    private var videoEncoder: MediaCodec? = null
    private var audioEncoder: MediaCodec? = null

    // ============================================
    // CAMERA SETUP
    // ============================================

    fun setupCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean = true
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .setTargetResolution(Size(1280, 720))
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Camera selector
            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )

                Log.d(TAG, "Camera setup complete")
            } catch (e: Exception) {
                Log.e(TAG, "Camera setup failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView, useFrontCamera: Boolean) {
        setupCamera(lifecycleOwner, previewView, useFrontCamera)
    }

    // ============================================
    // STREAMING
    // ============================================

    fun startStreaming(config: StreamConfig): Boolean {
        if (_isStreaming.value) {
            Log.w(TAG, "Already streaming")
            return false
        }

        streamConfig = config
        startTime = System.currentTimeMillis()

        // Setup encoders
        if (!setupEncoders(config)) {
            Log.e(TAG, "Failed to setup encoders")
            return false
        }

        _isStreaming.value = true
        _stats.value = StreamStats(isStreaming = true)

        // Start streaming job
        streamJob = scope.launch {
            try {
                // Connect to RTMP server
                val fullUrl = "${config.rtmpUrl}/${config.streamKey}"
                Log.d(TAG, "Connecting to: $fullUrl")

                _events.value = StreamEvent.Connected

                // Simulate streaming (in production, use RootEncoder library)
                var frameCount = 0
                while (isActive && _isStreaming.value) {
                    delay(33) // ~30 FPS
                    frameCount++

                    // Update stats
                    val duration = System.currentTimeMillis() - startTime
                    _stats.value = _stats.value.copy(
                        duration = duration,
                        fps = frameCount.toFloat() / (duration / 1000f),
                        bitrate = config.videoBitrate + config.audioBitrate,
                        sentBytes = (frameCount * 10000L), // Simulated
                        health = calculateHealth()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Streaming error", e)
                _events.value = StreamEvent.Error(e.message ?: "Unknown error")
            } finally {
                cleanupEncoders()
            }
        }

        Log.d(TAG, "Streaming started")
        return true
    }

    fun stopStreaming() {
        if (!_isStreaming.value) return

        _isStreaming.value = false
        streamJob?.cancel()
        cleanupEncoders()

        _stats.value = StreamStats()
        _events.value = StreamEvent.Disconnected

        Log.d(TAG, "Streaming stopped")
    }

    private fun setupEncoders(config: StreamConfig): Boolean {
        try {
            // Video encoder
            if (config.enableVideo) {
                val videoFormat = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_VIDEO_AVC,
                    config.videoWidth,
                    config.videoHeight
                ).apply {
                    setInteger(MediaFormat.KEY_BIT_RATE, config.videoBitrate)
                    setInteger(MediaFormat.KEY_FRAME_RATE, config.videoFps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
                    setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                }

                videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                videoEncoder?.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }

            // Audio encoder
            if (config.enableAudio) {
                val audioFormat = MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_AAC,
                    config.audioSampleRate,
                    config.audioChannels
                ).apply {
                    setInteger(MediaFormat.KEY_BIT_RATE, config.audioBitrate)
                    setInteger(MediaFormat.KEY_AAC_PROFILE,
                        MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                }

                audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
                audioEncoder?.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }

            videoEncoder?.start()
            audioEncoder?.start()

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup encoders", e)
            cleanupEncoders()
            return false
        }
    }

    private fun cleanupEncoders() {
        try {
            videoEncoder?.stop()
            videoEncoder?.release()
            videoEncoder = null

            audioEncoder?.stop()
            audioEncoder?.release()
            audioEncoder = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up encoders", e)
        }
    }

    private fun calculateHealth(): StreamHealth {
        val stats = _stats.value
        return when {
            stats.fps >= 28 && stats.droppedFrames < 10 -> StreamHealth.EXCELLENT
            stats.fps >= 25 && stats.droppedFrames < 50 -> StreamHealth.GOOD
            stats.fps >= 20 && stats.droppedFrames < 100 -> StreamHealth.FAIR
            else -> StreamHealth.POOR
        }
    }

    // ============================================
    // STREAM CONTROLS
    // ============================================

    fun setMicrophoneMuted(muted: Boolean) {
        Log.d(TAG, "Microphone muted: $muted")
    }

    fun setVideoEnabled(enabled: Boolean) {
        Log.d(TAG, "Video enabled: $enabled")
    }

    fun setBitrate(bitrate: Int) {
        // Adaptive bitrate
        Log.d(TAG, "Bitrate set to: $bitrate")
        _events.value = StreamEvent.BitrateChanged(bitrate)
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        stopStreaming()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        scope.cancel()
    }
}

// ============================================
// MULTI-PLATFORM STREAMER
// ============================================

class MultiPlatformStreamer(private val context: Context) {

    private val streamers = mutableMapOf<String, PodcastRTMPStreamer>()

    private val _activeStreams = MutableStateFlow<List<String>>(emptyList())
    val activeStreams: StateFlow<List<String>> = _activeStreams.asStateFlow()

    fun addPlatform(platformId: String, config: StreamConfig): Boolean {
        if (streamers.containsKey(platformId)) return false

        val streamer = PodcastRTMPStreamer(context)
        streamers[platformId] = streamer
        return true
    }

    fun removePlatform(platformId: String) {
        streamers[platformId]?.release()
        streamers.remove(platformId)
        _activeStreams.value = _activeStreams.value - platformId
    }

    fun startAllStreams() {
        streamers.forEach { (id, streamer) ->
            // Would start each with their config
        }
    }

    fun stopAllStreams() {
        streamers.forEach { (_, streamer) ->
            streamer.stopStreaming()
        }
        _activeStreams.value = emptyList()
    }

    fun release() {
        stopAllStreams()
        streamers.values.forEach { it.release() }
        streamers.clear()
    }
}

