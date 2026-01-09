package com.example.dwn.podcast.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

private const val TAG = "PodcastRecorder"

// ============================================
// RECORDING CONFIGURATION
// ============================================

data class RecordingConfig(
    val sampleRate: Int = 48000,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_STEREO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_FLOAT,
    val audioSource: Int = MediaRecorder.AudioSource.MIC,
    val enableNoiseSuppression: Boolean = true,
    val enableEchoCancellation: Boolean = true,
    val enableAutoGainControl: Boolean = true,
    val shadowRecordingEnabled: Boolean = true
)

data class RecordingMetrics(
    val peakLevel: Float = -60f,
    val rmsLevel: Float = -60f,
    val lufsIntegrated: Float = -23f,
    val lufsShortTerm: Float = -23f,
    val lufsMomentary: Float = -23f,
    val isClipping: Boolean = false,
    val clipCount: Int = 0,
    val dcOffset: Float = 0f,
    val crestFactor: Float = 0f
)

data class RecordingSession(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    val config: RecordingConfig = RecordingConfig(),
    val outputPath: String = "",
    val shadowPath: String = "",
    val duration: Long = 0,
    val fileSize: Long = 0
)

// ============================================
// PODCAST AUDIO RECORDER
// ============================================

class PodcastAudioRecorder(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var meteringJob: Job? = null

    // Audio effects
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var autoGainControl: AutomaticGainControl? = null

    // Output streams
    private var primaryOutputStream: BufferedOutputStream? = null
    private var shadowOutputStream: BufferedOutputStream? = null

    // State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _metrics = MutableStateFlow(RecordingMetrics())
    val metrics: StateFlow<RecordingMetrics> = _metrics.asStateFlow()

    private val _session = MutableStateFlow<RecordingSession?>(null)
    val session: StateFlow<RecordingSession?> = _session.asStateFlow()

    private val _waveformData = MutableStateFlow(FloatArray(0))
    val waveformData: StateFlow<FloatArray> = _waveformData.asStateFlow()

    // LUFS calculation buffers
    private val lufsBuffer = mutableListOf<Float>()
    private var totalSamples = 0L
    private var totalSquaredSum = 0.0
    private var clipCount = 0

    // Recording directory
    private val recordingsDir: File by lazy {
        File(context.getExternalFilesDir(null), "Podcasts/Recordings").also { it.mkdirs() }
    }

    private val shadowDir: File by lazy {
        File(context.getExternalFilesDir(null), "Podcasts/Shadow").also { it.mkdirs() }
    }

    // ============================================
    // PERMISSION CHECK
    // ============================================

    fun hasRecordingPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ============================================
    // START RECORDING
    // ============================================

    fun startRecording(config: RecordingConfig = RecordingConfig()): Boolean {
        if (_isRecording.value) {
            Log.w(TAG, "Already recording")
            return false
        }

        if (!hasRecordingPermission()) {
            Log.e(TAG, "No recording permission")
            return false
        }

        try {
            // Calculate buffer size
            val minBufferSize = AudioRecord.getMinBufferSize(
                config.sampleRate,
                config.channelConfig,
                config.audioFormat
            )

            if (minBufferSize == AudioRecord.ERROR_BAD_VALUE || minBufferSize == AudioRecord.ERROR) {
                Log.e(TAG, "Invalid buffer size")
                return false
            }

            val bufferSize = minBufferSize * 4 // Larger buffer for stability

            // Create AudioRecord
            audioRecord = AudioRecord(
                config.audioSource,
                config.sampleRate,
                config.channelConfig,
                config.audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            // Setup audio effects
            setupAudioEffects(config)

            // Create output files
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val primaryFile = File(recordingsDir, "recording_$timestamp.wav")
            val shadowFile = File(shadowDir, "shadow_$timestamp.wav")

            primaryOutputStream = BufferedOutputStream(FileOutputStream(primaryFile))

            if (config.shadowRecordingEnabled) {
                shadowOutputStream = BufferedOutputStream(FileOutputStream(shadowFile))
            }

            // Write WAV headers (placeholder, will update on stop)
            writeWavHeader(primaryOutputStream!!, config, 0)
            shadowOutputStream?.let { writeWavHeader(it, config, 0) }

            // Reset metrics
            lufsBuffer.clear()
            totalSamples = 0L
            totalSquaredSum = 0.0
            clipCount = 0

            // Create session
            _session.value = RecordingSession(
                config = config,
                outputPath = primaryFile.absolutePath,
                shadowPath = if (config.shadowRecordingEnabled) shadowFile.absolutePath else ""
            )

            // Start recording
            audioRecord?.startRecording()
            _isRecording.value = true
            _isPaused.value = false

            // Start recording job
            recordingJob = scope.launch {
                recordAudio(config, bufferSize)
            }

            // Start metering job
            meteringJob = scope.launch {
                while (isActive && _isRecording.value) {
                    updateSessionDuration()
                    delay(100)
                }
            }

            Log.d(TAG, "Recording started: ${primaryFile.absolutePath}")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            stopRecording()
            return false
        }
    }

    // ============================================
    // RECORDING LOOP
    // ============================================

    private suspend fun recordAudio(config: RecordingConfig, bufferSize: Int) {
        val floatBuffer = FloatArray(bufferSize / 4)
        val byteBuffer = ByteArray(bufferSize)
        val channels = if (config.channelConfig == AudioFormat.CHANNEL_IN_STEREO) 2 else 1

        while (_isRecording.value) {
            if (_isPaused.value) {
                delay(50)
                continue
            }

            val floatsRead = audioRecord?.read(
                floatBuffer, 0, floatBuffer.size, AudioRecord.READ_BLOCKING
            ) ?: 0

            if (floatsRead > 0) {
                // Process and analyze audio
                val metrics = analyzeAudio(floatBuffer, floatsRead, config.sampleRate, channels)
                _metrics.value = metrics

                // Update waveform display
                updateWaveform(floatBuffer, floatsRead)

                // Convert float to bytes for WAV
                val bytesWritten = convertFloatToBytes(floatBuffer, floatsRead, byteBuffer)

                // Write to primary output
                primaryOutputStream?.write(byteBuffer, 0, bytesWritten)

                // Write to shadow output
                shadowOutputStream?.write(byteBuffer, 0, bytesWritten)

                totalSamples += floatsRead
            }
        }
    }

    // ============================================
    // AUDIO ANALYSIS
    // ============================================

    private fun analyzeAudio(
        buffer: FloatArray,
        size: Int,
        sampleRate: Int,
        channels: Int
    ): RecordingMetrics {
        var peak = 0f
        var sumSquares = 0.0
        var localClipCount = 0
        var dcSum = 0.0

        for (i in 0 until size) {
            val sample = buffer[i]
            val absSample = abs(sample)

            if (absSample > peak) peak = absSample
            sumSquares += sample * sample
            dcSum += sample

            if (absSample > 0.99f) localClipCount++
        }

        clipCount += localClipCount
        totalSquaredSum += sumSquares

        val rms = sqrt(sumSquares / size).toFloat()
        val peakDb = if (peak > 0) 20 * log10(peak) else -60f
        val rmsDb = if (rms > 0) 20 * log10(rms) else -60f
        val dcOffset = (dcSum / size).toFloat()
        val crestFactor = if (rms > 0) peak / rms else 0f

        // LUFS calculation (simplified K-weighting approximation)
        val kWeightedRms = rms * 1.0f // Simplified - real implementation needs proper K-weighting filter
        lufsBuffer.add(kWeightedRms)

        // Keep last 3 seconds of data for short-term LUFS
        val maxSamples = (sampleRate * 3) / size
        while (lufsBuffer.size > maxSamples) {
            lufsBuffer.removeAt(0)
        }

        // Calculate LUFS values
        val momentaryLufs = calculateLufs(lufsBuffer.takeLast(10))
        val shortTermLufs = calculateLufs(lufsBuffer)
        val integratedLufs = if (totalSamples > 0) {
            val integratedRms = sqrt(totalSquaredSum / totalSamples)
            -0.691 + 10 * log10(integratedRms * integratedRms + 1e-10)
        } else -60.0

        return RecordingMetrics(
            peakLevel = peakDb.coerceIn(-60f, 0f),
            rmsLevel = rmsDb.coerceIn(-60f, 0f),
            lufsIntegrated = integratedLufs.toFloat().coerceIn(-60f, 0f),
            lufsShortTerm = shortTermLufs.coerceIn(-60f, 0f),
            lufsMomentary = momentaryLufs.coerceIn(-60f, 0f),
            isClipping = localClipCount > 0,
            clipCount = clipCount,
            dcOffset = dcOffset,
            crestFactor = crestFactor
        )
    }

    private fun calculateLufs(rmsValues: List<Float>): Float {
        if (rmsValues.isEmpty()) return -60f
        val meanSquare = rmsValues.map { it * it }.average()
        return (-0.691 + 10 * log10(meanSquare + 1e-10)).toFloat()
    }

    private fun updateWaveform(buffer: FloatArray, size: Int) {
        // Downsample for display
        val displaySize = 128
        val step = maxOf(1, size / displaySize)
        val waveform = FloatArray(minOf(displaySize, size / step))

        for (i in waveform.indices) {
            val startIdx = i * step
            var maxVal = 0f
            for (j in 0 until step) {
                if (startIdx + j < size) {
                    val absVal = abs(buffer[startIdx + j])
                    if (absVal > maxVal) maxVal = absVal
                }
            }
            waveform[i] = maxVal
        }

        _waveformData.value = waveform
    }

    // ============================================
    // PAUSE / RESUME
    // ============================================

    fun pauseRecording() {
        if (_isRecording.value && !_isPaused.value) {
            _isPaused.value = true
            Log.d(TAG, "Recording paused")
        }
    }

    fun resumeRecording() {
        if (_isRecording.value && _isPaused.value) {
            _isPaused.value = false
            Log.d(TAG, "Recording resumed")
        }
    }

    // ============================================
    // STOP RECORDING
    // ============================================

    fun stopRecording(): RecordingSession? {
        if (!_isRecording.value) return null

        _isRecording.value = false
        _isPaused.value = false

        recordingJob?.cancel()
        meteringJob?.cancel()

        // Stop AudioRecord
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }

        // Release audio effects
        releaseAudioEffects()

        // Release AudioRecord
        audioRecord?.release()
        audioRecord = null

        // Close output streams and update WAV headers
        val session = _session.value

        try {
            primaryOutputStream?.flush()
            primaryOutputStream?.close()

            shadowOutputStream?.flush()
            shadowOutputStream?.close()

            // Update WAV headers with actual data size
            session?.let { s ->
                updateWavHeader(s.outputPath, s.config)
                if (s.shadowPath.isNotEmpty()) {
                    updateWavHeader(s.shadowPath, s.config)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing streams", e)
        }

        primaryOutputStream = null
        shadowOutputStream = null

        // Update session with final duration and file size
        val finalSession = session?.copy(
            duration = System.currentTimeMillis() - (session.startTime),
            fileSize = session.outputPath.let { File(it).length() }
        )

        _session.value = null
        _metrics.value = RecordingMetrics()
        _waveformData.value = FloatArray(0)

        Log.d(TAG, "Recording stopped: ${finalSession?.outputPath}")
        return finalSession
    }

    // ============================================
    // AUDIO EFFECTS SETUP
    // ============================================

    private fun setupAudioEffects(config: RecordingConfig) {
        val sessionId = audioRecord?.audioSessionId ?: return

        if (config.enableNoiseSuppression && NoiseSuppressor.isAvailable()) {
            try {
                noiseSuppressor = NoiseSuppressor.create(sessionId)
                noiseSuppressor?.enabled = true
                Log.d(TAG, "Noise suppressor enabled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create noise suppressor", e)
            }
        }

        if (config.enableEchoCancellation && AcousticEchoCanceler.isAvailable()) {
            try {
                echoCanceler = AcousticEchoCanceler.create(sessionId)
                echoCanceler?.enabled = true
                Log.d(TAG, "Echo canceler enabled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create echo canceler", e)
            }
        }

        if (config.enableAutoGainControl && AutomaticGainControl.isAvailable()) {
            try {
                autoGainControl = AutomaticGainControl.create(sessionId)
                autoGainControl?.enabled = true
                Log.d(TAG, "Auto gain control enabled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create auto gain control", e)
            }
        }
    }

    private fun releaseAudioEffects() {
        noiseSuppressor?.release()
        noiseSuppressor = null

        echoCanceler?.release()
        echoCanceler = null

        autoGainControl?.release()
        autoGainControl = null
    }

    // ============================================
    // WAV FILE HANDLING
    // ============================================

    private fun writeWavHeader(
        outputStream: OutputStream,
        config: RecordingConfig,
        dataSize: Int
    ) {
        val channels = if (config.channelConfig == AudioFormat.CHANNEL_IN_STEREO) 2 else 1
        val bitsPerSample = 32 // Float = 32 bits
        val byteRate = config.sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        outputStream.write("RIFF".toByteArray())
        outputStream.write(intToByteArray(36 + dataSize)) // File size - 8
        outputStream.write("WAVE".toByteArray())
        outputStream.write("fmt ".toByteArray())
        outputStream.write(intToByteArray(16)) // Subchunk1Size
        outputStream.write(shortToByteArray(3)) // AudioFormat (3 = IEEE float)
        outputStream.write(shortToByteArray(channels.toShort()))
        outputStream.write(intToByteArray(config.sampleRate))
        outputStream.write(intToByteArray(byteRate))
        outputStream.write(shortToByteArray(blockAlign.toShort()))
        outputStream.write(shortToByteArray(bitsPerSample.toShort()))
        outputStream.write("data".toByteArray())
        outputStream.write(intToByteArray(dataSize))
    }

    private fun updateWavHeader(filePath: String, config: RecordingConfig) {
        try {
            val file = RandomAccessFile(filePath, "rw")
            val fileSize = file.length().toInt()
            val dataSize = fileSize - 44 // Header is 44 bytes

            // Update RIFF chunk size
            file.seek(4)
            file.write(intToByteArray(fileSize - 8))

            // Update data chunk size
            file.seek(40)
            file.write(intToByteArray(dataSize))

            file.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update WAV header", e)
        }
    }

    private fun convertFloatToBytes(floatBuffer: FloatArray, size: Int, byteBuffer: ByteArray): Int {
        val buffer = ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN)
        buffer.clear()
        for (i in 0 until size) {
            buffer.putFloat(floatBuffer[i])
        }
        return size * 4
    }

    private fun intToByteArray(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }

    // ============================================
    // SESSION MANAGEMENT
    // ============================================

    private fun updateSessionDuration() {
        _session.value?.let { session ->
            val duration = System.currentTimeMillis() - session.startTime
            _session.value = session.copy(duration = duration)
        }
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        stopRecording()
        scope.cancel()
    }

    // ============================================
    // UTILITY
    // ============================================

    fun getRecordingsDirectory(): File = recordingsDir

    fun listRecordings(): List<File> {
        return recordingsDir.listFiles()?.filter { it.extension == "wav" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteRecording(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recording", e)
            false
        }
    }
}

