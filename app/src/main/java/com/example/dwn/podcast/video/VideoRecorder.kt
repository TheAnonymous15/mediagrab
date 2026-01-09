package com.example.dwn.podcast.video

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "VideoRecorder"

// ============================================
// VIDEO RECORDING CONFIGURATION
// ============================================

data class VideoConfig(
    val quality: VideoQuality = VideoQuality.HD,
    val aspectRatio: AspectRatioMode = AspectRatioMode.RATIO_16_9,
    val enableAudio: Boolean = true,
    val useFrontCamera: Boolean = true,
    val enableStabilization: Boolean = true,
    val mirrorFrontCamera: Boolean = true
)

enum class VideoQuality {
    SD,         // 480p
    HD,         // 720p
    FHD,        // 1080p
    UHD         // 4K
}

enum class AspectRatioMode {
    RATIO_16_9,
    RATIO_4_3,
    RATIO_1_1
}

data class VideoRecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val duration: Long = 0,
    val fileSize: Long = 0,
    val outputPath: String = ""
)

// ============================================
// VIDEO RECORDER
// ============================================

class PodcastVideoRecorder(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(VideoRecordingState())
    val state: StateFlow<VideoRecordingState> = _state.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private var currentConfig = VideoConfig()
    private var useFrontCamera = true

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val recordingsDir: File by lazy {
        File(context.getExternalFilesDir(null), "Podcasts/Videos").also { it.mkdirs() }
    }

    // ============================================
    // PERMISSION CHECK
    // ============================================

    fun hasPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val audioPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        return cameraPermission && audioPermission
    }

    // ============================================
    // CAMERA INITIALIZATION
    // ============================================

    fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        config: VideoConfig = VideoConfig()
    ) {
        currentConfig = config
        useFrontCamera = config.useFrontCamera

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
                _isInitialized.value = true
                Log.d(TAG, "Camera initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProvider = cameraProvider ?: return

        // Camera selector
        val cameraSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // Preview
        preview = Preview.Builder()
            .setTargetAspectRatio(getAspectRatioInt(currentConfig.aspectRatio))
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // Video capture
        val qualitySelector = QualitySelector.from(
            getQuality(currentConfig.quality),
            FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
        )

        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )

            // Enable stabilization if available
            if (currentConfig.enableStabilization) {
                camera?.cameraControl?.enableTorch(false)
            }

            Log.d(TAG, "Camera use cases bound")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind camera use cases", e)
        }
    }

    // ============================================
    // RECORDING CONTROLS
    // ============================================

    fun startRecording(): Boolean {
        if (_state.value.isRecording) {
            Log.w(TAG, "Already recording")
            return false
        }

        val videoCapture = videoCapture ?: run {
            Log.e(TAG, "Video capture not initialized")
            return false
        }

        // Create output file
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(recordingsDir, "video_$timestamp.mp4")

        // File output options
        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        // Start recording
        recording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .apply {
                if (currentConfig.enableAudio &&
                    PermissionChecker.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PermissionChecker.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }
            .start(cameraExecutor) { event ->
                handleRecordingEvent(event, outputFile.absolutePath)
            }

        _state.value = VideoRecordingState(
            isRecording = true,
            outputPath = outputFile.absolutePath
        )

        // Start duration timer
        startDurationTimer()

        Log.d(TAG, "Recording started: ${outputFile.absolutePath}")
        return true
    }

    fun pauseRecording() {
        if (!_state.value.isRecording || _state.value.isPaused) return

        recording?.pause()
        _state.value = _state.value.copy(isPaused = true)
        Log.d(TAG, "Recording paused")
    }

    fun resumeRecording() {
        if (!_state.value.isRecording || !_state.value.isPaused) return

        recording?.resume()
        _state.value = _state.value.copy(isPaused = false)
        Log.d(TAG, "Recording resumed")
    }

    fun stopRecording(): String? {
        if (!_state.value.isRecording) return null

        val outputPath = _state.value.outputPath

        recording?.stop()
        recording = null

        _state.value = VideoRecordingState()

        Log.d(TAG, "Recording stopped: $outputPath")
        return outputPath
    }

    private fun handleRecordingEvent(event: VideoRecordEvent, outputPath: String) {
        when (event) {
            is VideoRecordEvent.Start -> {
                Log.d(TAG, "Recording started")
            }
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    Log.e(TAG, "Recording error: ${event.error}")
                } else {
                    Log.d(TAG, "Recording finalized: $outputPath")
                }
                _state.value = VideoRecordingState()
            }
            is VideoRecordEvent.Status -> {
                // Update file size
                val stats = event.recordingStats
                _state.value = _state.value.copy(
                    fileSize = stats.numBytesRecorded,
                    duration = stats.recordedDurationNanos / 1_000_000
                )
            }
            is VideoRecordEvent.Pause -> {
                Log.d(TAG, "Recording paused")
            }
            is VideoRecordEvent.Resume -> {
                Log.d(TAG, "Recording resumed")
            }
        }
    }

    private var durationJob: Job? = null

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = scope.launch {
            val startTime = System.currentTimeMillis()
            while (_state.value.isRecording) {
                if (!_state.value.isPaused) {
                    _state.value = _state.value.copy(
                        duration = System.currentTimeMillis() - startTime
                    )
                }
                delay(100)
            }
        }
    }

    // ============================================
    // CAMERA CONTROLS
    // ============================================

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (_state.value.isRecording) {
            Log.w(TAG, "Cannot switch camera while recording")
            return
        }

        useFrontCamera = !useFrontCamera
        currentConfig = currentConfig.copy(useFrontCamera = useFrontCamera)
        bindCameraUseCases(lifecycleOwner, previewView)

        Log.d(TAG, "Switched to ${if (useFrontCamera) "front" else "back"} camera")
    }

    fun setZoom(zoomRatio: Float) {
        camera?.cameraControl?.setZoomRatio(zoomRatio.coerceIn(1f, getMaxZoom()))
    }

    fun getMaxZoom(): Float {
        return camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
    }

    fun setTorch(enabled: Boolean) {
        if (!useFrontCamera) {
            camera?.cameraControl?.enableTorch(enabled)
        }
    }

    fun focus(x: Float, y: Float) {
        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    // ============================================
    // UTILITIES
    // ============================================

    private fun getQuality(quality: VideoQuality): Quality {
        return when (quality) {
            VideoQuality.SD -> Quality.SD
            VideoQuality.HD -> Quality.HD
            VideoQuality.FHD -> Quality.FHD
            VideoQuality.UHD -> Quality.UHD
        }
    }

    private fun getAspectRatioInt(aspectRatio: AspectRatioMode): Int {
        return when (aspectRatio) {
            AspectRatioMode.RATIO_16_9 -> AspectRatio.RATIO_16_9
            AspectRatioMode.RATIO_4_3 -> AspectRatio.RATIO_4_3
            AspectRatioMode.RATIO_1_1 -> AspectRatio.RATIO_4_3 // CameraX doesn't have 1:1
        }
    }

    fun listRecordings(): List<File> {
        return recordingsDir.listFiles()?.filter {
            it.extension == "mp4"
        }?.sortedByDescending {
            it.lastModified()
        } ?: emptyList()
    }

    fun deleteRecording(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recording", e)
            false
        }
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        stopRecording()
        durationJob?.cancel()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        scope.cancel()
    }
}

// ============================================
// MULTI-CAMERA RECORDER (for multiple angles)
// ============================================

class MultiCameraRecorder(private val context: Context) {

    private val recorders = mutableMapOf<String, PodcastVideoRecorder>()

    private val _activeRecorders = MutableStateFlow<List<String>>(emptyList())
    val activeRecorders: StateFlow<List<String>> = _activeRecorders.asStateFlow()

    fun addCamera(id: String): PodcastVideoRecorder {
        val recorder = PodcastVideoRecorder(context)
        recorders[id] = recorder
        return recorder
    }

    fun removeCamera(id: String) {
        recorders[id]?.release()
        recorders.remove(id)
    }

    fun startAllRecordings() {
        val started = mutableListOf<String>()
        recorders.forEach { (id, recorder) ->
            if (recorder.startRecording()) {
                started.add(id)
            }
        }
        _activeRecorders.value = started
    }

    fun stopAllRecordings(): List<String> {
        val outputs = mutableListOf<String>()
        recorders.forEach { (_, recorder) ->
            recorder.stopRecording()?.let { outputs.add(it) }
        }
        _activeRecorders.value = emptyList()
        return outputs
    }

    fun release() {
        stopAllRecordings()
        recorders.values.forEach { it.release() }
        recorders.clear()
    }
}

