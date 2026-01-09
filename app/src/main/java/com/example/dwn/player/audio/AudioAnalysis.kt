package com.example.dwn.player.audio

import android.content.Context
import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

private const val TAG = "AudioAnalysis"

/**
 * AI / SMART PROCESSING - Feature Set 6
 * ANALYSIS & VISUALIZATION - Feature Set 7
 *
 * - Auto-EQ (track analysis)
 * - Genre detection
 * - Adaptive loudness normalization (LUFS)
 * - Real-time FFT spectrum analyzer
 * - Spectrogram
 * - Phase correlation meter
 * - Stereo field scope
 * - LUFS / RMS / Peak meters
 */

// ============================================
// SPECTRUM ANALYZER
// ============================================

data class SpectrumData(
    val frequencies: FloatArray = FloatArray(0),
    val magnitudes: FloatArray = FloatArray(0),
    val peakFrequency: Float = 0f,
    val peakMagnitude: Float = 0f
)

data class SpectrumAnalyzerSettings(
    val isEnabled: Boolean = false,
    val fftSize: Int = 1024,
    val windowType: WindowType = WindowType.HANNING,
    val smoothing: Float = 0.7f,
    val minFreq: Float = 20f,
    val maxFreq: Float = 20000f,
    val minDb: Float = -90f,
    val maxDb: Float = 0f
)

enum class WindowType(val label: String) {
    RECTANGULAR("Rectangular"),
    HANNING("Hanning"),
    HAMMING("Hamming"),
    BLACKMAN("Blackman"),
    KAISER("Kaiser")
}

class SpectrumAnalyzer(private val context: Context) {

    private var visualizer: Visualizer? = null

    private val _settings = MutableStateFlow(SpectrumAnalyzerSettings())
    val settings: StateFlow<SpectrumAnalyzerSettings> = _settings.asStateFlow()

    private val _spectrumData = MutableStateFlow(SpectrumData())
    val spectrumData: StateFlow<SpectrumData> = _spectrumData.asStateFlow()

    private val _waveformData = MutableStateFlow(ByteArray(0))
    val waveformData: StateFlow<ByteArray> = _waveformData.asStateFlow()

    private var smoothedMagnitudes = FloatArray(0)

    fun initialize(audioSessionId: Int) {
        try {
            release()

            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1] // Max size

                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        waveform?.let { _waveformData.value = it }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        fft?.let { processFFT(it, samplingRate) }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
            }

            Log.d(TAG, "Spectrum analyzer initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize spectrum analyzer", e)
        }
    }

    fun setEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(isEnabled = enabled)
        visualizer?.enabled = enabled
    }

    private fun processFFT(fftBytes: ByteArray, samplingRate: Int) {
        val settings = _settings.value
        val n = fftBytes.size / 2

        val magnitudes = FloatArray(n)
        val frequencies = FloatArray(n)

        var peakMagnitude = Float.MIN_VALUE
        var peakFrequency = 0f

        for (i in 0 until n) {
            val real = fftBytes[2 * i].toFloat()
            val imag = fftBytes[2 * i + 1].toFloat()

            val magnitude = sqrt(real * real + imag * imag)
            val db = 20 * log10(magnitude.coerceAtLeast(1f))

            magnitudes[i] = db.coerceIn(settings.minDb, settings.maxDb)
            frequencies[i] = i * samplingRate.toFloat() / (n * 2)

            if (magnitude > peakMagnitude) {
                peakMagnitude = magnitude
                peakFrequency = frequencies[i]
            }
        }

        // Apply smoothing
        if (smoothedMagnitudes.size != magnitudes.size) {
            smoothedMagnitudes = magnitudes.copyOf()
        } else {
            for (i in magnitudes.indices) {
                smoothedMagnitudes[i] = smoothedMagnitudes[i] * settings.smoothing +
                    magnitudes[i] * (1 - settings.smoothing)
            }
        }

        _spectrumData.value = SpectrumData(
            frequencies = frequencies,
            magnitudes = smoothedMagnitudes.copyOf(),
            peakFrequency = peakFrequency,
            peakMagnitude = 20 * log10(peakMagnitude.coerceAtLeast(1f))
        )
    }

    fun updateSettings(
        fftSize: Int? = null,
        windowType: WindowType? = null,
        smoothing: Float? = null,
        minFreq: Float? = null,
        maxFreq: Float? = null,
        minDb: Float? = null,
        maxDb: Float? = null
    ) {
        _settings.value = _settings.value.copy(
            fftSize = fftSize ?: _settings.value.fftSize,
            windowType = windowType ?: _settings.value.windowType,
            smoothing = smoothing?.coerceIn(0f, 0.99f) ?: _settings.value.smoothing,
            minFreq = minFreq?.coerceIn(20f, 1000f) ?: _settings.value.minFreq,
            maxFreq = maxFreq?.coerceIn(5000f, 22000f) ?: _settings.value.maxFreq,
            minDb = minDb?.coerceIn(-120f, -30f) ?: _settings.value.minDb,
            maxDb = maxDb?.coerceIn(-12f, 6f) ?: _settings.value.maxDb
        )
    }

    fun release() {
        visualizer?.enabled = false
        visualizer?.release()
        visualizer = null
    }
}

// ============================================
// LEVEL METERS (LUFS, RMS, Peak)
// ============================================

data class LevelMeterData(
    val peakL: Float = -Float.MAX_VALUE,
    val peakR: Float = -Float.MAX_VALUE,
    val rmsL: Float = -Float.MAX_VALUE,
    val rmsR: Float = -Float.MAX_VALUE,
    val lufs: Float = -Float.MAX_VALUE,
    val truePeakL: Float = -Float.MAX_VALUE,
    val truePeakR: Float = -Float.MAX_VALUE,
    val isClipping: Boolean = false,
    val dynamicRange: Float = 0f
)

class LevelMeter {

    private val _data = MutableStateFlow(LevelMeterData())
    val data: StateFlow<LevelMeterData> = _data.asStateFlow()

    private var lufsBuffer = mutableListOf<Float>()
    private val lufsWindowSize = 400 // ~400ms at 44.1kHz

    private var peakHoldL = -Float.MAX_VALUE
    private var peakHoldR = -Float.MAX_VALUE
    private var peakHoldTime = 0
    private val peakHoldDuration = 2000 // ms

    fun process(left: FloatArray, right: FloatArray, sampleRate: Int) {
        // Peak levels
        val peakL = left.maxOfOrNull { abs(it) } ?: 0f
        val peakR = right.maxOfOrNull { abs(it) } ?: 0f

        // RMS levels
        val rmsL = sqrt(left.map { it * it }.average().toFloat())
        val rmsR = sqrt(right.map { it * it }.average().toFloat())

        // Convert to dB
        val peakDbL = 20 * log10(peakL.coerceAtLeast(1e-10f))
        val peakDbR = 20 * log10(peakR.coerceAtLeast(1e-10f))
        val rmsDbL = 20 * log10(rmsL.coerceAtLeast(1e-10f))
        val rmsDbR = 20 * log10(rmsR.coerceAtLeast(1e-10f))

        // LUFS calculation (simplified)
        val monoRms = sqrt((left.zip(right.toList()) { l, r ->
            val mono = (l + r) / 2
            mono * mono
        }).average().toFloat())

        lufsBuffer.add(monoRms)
        if (lufsBuffer.size > lufsWindowSize) {
            lufsBuffer.removeAt(0)
        }

        val integratedLoudness = if (lufsBuffer.isNotEmpty()) {
            val avgPower = lufsBuffer.average().toFloat()
            -0.691f + 10 * log10(avgPower.coerceAtLeast(1e-10f))
        } else {
            -Float.MAX_VALUE
        }

        // Peak hold
        if (peakDbL > peakHoldL || peakHoldTime <= 0) {
            peakHoldL = peakDbL
            peakHoldTime = peakHoldDuration
        }
        if (peakDbR > peakHoldR || peakHoldTime <= 0) {
            peakHoldR = peakDbR
        }
        peakHoldTime -= (left.size * 1000 / sampleRate)

        // Clipping detection
        val isClipping = peakL >= 1f || peakR >= 1f

        // Dynamic range
        val dynamicRange = maxOf(peakDbL, peakDbR) - minOf(rmsDbL, rmsDbR)

        _data.value = LevelMeterData(
            peakL = peakDbL,
            peakR = peakDbR,
            rmsL = rmsDbL,
            rmsR = rmsDbR,
            lufs = integratedLoudness,
            truePeakL = peakHoldL,
            truePeakR = peakHoldR,
            isClipping = isClipping,
            dynamicRange = dynamicRange
        )
    }

    fun reset() {
        lufsBuffer.clear()
        peakHoldL = -Float.MAX_VALUE
        peakHoldR = -Float.MAX_VALUE
        _data.value = LevelMeterData()
    }
}

// ============================================
// STEREO FIELD ANALYZER
// ============================================

data class StereoFieldData(
    val correlation: Float = 0f,       // -1 to 1 (1 = mono, -1 = out of phase)
    val balance: Float = 0f,           // -1 (left) to 1 (right)
    val width: Float = 0f,             // 0 to 1
    val midLevel: Float = 0f,          // dB
    val sideLevel: Float = 0f          // dB
)

class StereoFieldAnalyzer {

    private val _data = MutableStateFlow(StereoFieldData())
    val data: StateFlow<StereoFieldData> = _data.asStateFlow()

    fun process(left: FloatArray, right: FloatArray) {
        if (left.isEmpty() || right.isEmpty()) return

        // Phase correlation
        var sumLR = 0f
        var sumL2 = 0f
        var sumR2 = 0f

        for (i in left.indices) {
            sumLR += left[i] * right[i]
            sumL2 += left[i] * left[i]
            sumR2 += right[i] * right[i]
        }

        val correlation = if (sumL2 > 0 && sumR2 > 0) {
            sumLR / sqrt(sumL2 * sumR2)
        } else 0f

        // Balance
        val avgL = left.map { abs(it) }.average().toFloat()
        val avgR = right.map { abs(it) }.average().toFloat()
        val balance = if (avgL + avgR > 0) {
            (avgR - avgL) / (avgL + avgR)
        } else 0f

        // Mid/Side levels
        val midSamples = left.zip(right.toList()) { l, r -> (l + r) / 2 }
        val sideSamples = left.zip(right.toList()) { l, r -> (l - r) / 2 }

        val midRms = sqrt(midSamples.map { it * it }.average().toFloat())
        val sideRms = sqrt(sideSamples.map { it * it }.average().toFloat())

        val midLevel = 20 * log10(midRms.coerceAtLeast(1e-10f))
        val sideLevel = 20 * log10(sideRms.coerceAtLeast(1e-10f))

        // Width (ratio of side to mid)
        val width = if (midRms > 0) (sideRms / midRms).coerceIn(0f, 2f) else 0f

        _data.value = StereoFieldData(
            correlation = correlation,
            balance = balance,
            width = width,
            midLevel = midLevel,
            sideLevel = sideLevel
        )
    }
}

// ============================================
// AUTO-EQ / SMART PROCESSING
// ============================================

data class AudioAnalysis(
    val genre: String = "Unknown",
    val genreConfidence: Float = 0f,
    val suggestedPreset: String = "Flat",
    val dominantFrequencies: List<Float> = emptyList(),
    val estimatedBpm: Float = 0f,
    val isVocal: Boolean = false,
    val isBassHeavy: Boolean = false,
    val brightness: Float = 0.5f,       // 0 = dark, 1 = bright
    val warmth: Float = 0.5f            // 0 = cold, 1 = warm
)

class SmartAudioProcessor {

    private val _analysis = MutableStateFlow(AudioAnalysis())
    val analysis: StateFlow<AudioAnalysis> = _analysis.asStateFlow()

    private val _autoEQEnabled = MutableStateFlow(false)
    val autoEQEnabled: StateFlow<Boolean> = _autoEQEnabled.asStateFlow()

    // Genre detection based on spectral analysis
    fun analyzeAudio(spectrumData: SpectrumData) {
        val magnitudes = spectrumData.magnitudes
        if (magnitudes.isEmpty()) return

        val frequencies = spectrumData.frequencies

        // Analyze frequency bands
        val bassEnergy = getEnergyInRange(frequencies, magnitudes, 20f, 200f)
        val lowMidEnergy = getEnergyInRange(frequencies, magnitudes, 200f, 800f)
        val midEnergy = getEnergyInRange(frequencies, magnitudes, 800f, 2500f)
        val highMidEnergy = getEnergyInRange(frequencies, magnitudes, 2500f, 6000f)
        val highEnergy = getEnergyInRange(frequencies, magnitudes, 6000f, 16000f)

        val totalEnergy = bassEnergy + lowMidEnergy + midEnergy + highMidEnergy + highEnergy

        // Normalize
        val bassRatio = if (totalEnergy > 0) bassEnergy / totalEnergy else 0f
        val midRatio = if (totalEnergy > 0) midEnergy / totalEnergy else 0f
        val highRatio = if (totalEnergy > 0) highEnergy / totalEnergy else 0f

        // Simple genre detection
        val (genre, confidence, preset) = when {
            bassRatio > 0.4f && midRatio < 0.2f -> Triple("Electronic/EDM", 0.7f, "Dance")
            bassRatio > 0.35f && midRatio > 0.25f -> Triple("Hip Hop", 0.65f, "Hip Hop")
            midRatio > 0.35f && highRatio > 0.2f -> Triple("Rock", 0.6f, "Rock")
            midRatio > 0.4f && bassRatio < 0.2f -> Triple("Vocal/Speech", 0.7f, "Vocal Boost")
            highRatio > 0.3f -> Triple("Classical/Jazz", 0.55f, "Classical")
            bassRatio > 0.3f -> Triple("Pop", 0.5f, "Pop")
            else -> Triple("Unknown", 0.3f, "Flat")
        }

        val brightness = highRatio / (bassRatio + 0.01f)
        val warmth = bassRatio / (highRatio + 0.01f)

        _analysis.value = AudioAnalysis(
            genre = genre,
            genreConfidence = confidence,
            suggestedPreset = preset,
            dominantFrequencies = findDominantFrequencies(frequencies, magnitudes),
            isBassHeavy = bassRatio > 0.35f,
            isVocal = midRatio > 0.4f,
            brightness = (brightness / 2).coerceIn(0f, 1f),
            warmth = (warmth / 2).coerceIn(0f, 1f)
        )
    }

    private fun getEnergyInRange(
        frequencies: FloatArray,
        magnitudes: FloatArray,
        minFreq: Float,
        maxFreq: Float
    ): Float {
        var energy = 0f
        for (i in frequencies.indices) {
            if (frequencies[i] >= minFreq && frequencies[i] <= maxFreq) {
                // Convert from dB back to linear for energy calculation
                val linear = 10f.pow(magnitudes[i] / 20f)
                energy += linear * linear
            }
        }
        return energy
    }

    private fun findDominantFrequencies(
        frequencies: FloatArray,
        magnitudes: FloatArray,
        count: Int = 5
    ): List<Float> {
        return frequencies.zip(magnitudes.toList())
            .sortedByDescending { it.second }
            .take(count)
            .map { it.first }
    }

    fun setAutoEQEnabled(enabled: Boolean) {
        _autoEQEnabled.value = enabled
    }

    // Get suggested EQ curve based on analysis
    fun getSuggestedEQCurve(): Map<Float, Float> {
        val analysis = _analysis.value

        return when {
            analysis.isBassHeavy -> mapOf(
                60f to -2f, 250f to 0f, 1000f to 2f, 4000f to 3f, 12000f to 2f
            )
            analysis.isVocal -> mapOf(
                60f to -3f, 250f to -1f, 1000f to 3f, 4000f to 2f, 12000f to 0f
            )
            analysis.brightness > 0.7f -> mapOf(
                60f to 2f, 250f to 1f, 1000f to 0f, 4000f to -2f, 12000f to -3f
            )
            analysis.warmth > 0.7f -> mapOf(
                60f to -2f, 250f to 0f, 1000f to 1f, 4000f to 2f, 12000f to 3f
            )
            else -> mapOf(
                60f to 0f, 250f to 0f, 1000f to 0f, 4000f to 0f, 12000f to 0f
            )
        }
    }
}

// ============================================
// LOUDNESS NORMALIZATION
// ============================================

data class LoudnessNormSettings(
    val isEnabled: Boolean = false,
    val targetLufs: Float = -14f,      // Streaming standard
    val maxTruePeak: Float = -1f,      // dBTP
    val mode: LoudnessMode = LoudnessMode.STREAMING
)

enum class LoudnessMode(val label: String, val targetLufs: Float) {
    STREAMING("Streaming", -14f),
    BROADCAST("Broadcast", -23f),
    PODCAST("Podcast", -16f),
    MUSIC_PRODUCTION("Music", -9f),
    CUSTOM("Custom", -14f)
}

class LoudnessNormalizer {

    private val _settings = MutableStateFlow(LoudnessNormSettings())
    val settings: StateFlow<LoudnessNormSettings> = _settings.asStateFlow()

    private var currentLufs = -23f
    private var gainAdjustment = 0f

    fun updateSettings(
        enabled: Boolean? = null,
        targetLufs: Float? = null,
        maxTruePeak: Float? = null,
        mode: LoudnessMode? = null
    ) {
        val newMode = mode ?: _settings.value.mode
        _settings.value = _settings.value.copy(
            isEnabled = enabled ?: _settings.value.isEnabled,
            targetLufs = targetLufs ?: newMode.targetLufs,
            maxTruePeak = maxTruePeak?.coerceIn(-6f, 0f) ?: _settings.value.maxTruePeak,
            mode = newMode
        )
    }

    fun calculateGainAdjustment(measuredLufs: Float): Float {
        if (!_settings.value.isEnabled) return 0f

        currentLufs = measuredLufs
        gainAdjustment = _settings.value.targetLufs - measuredLufs

        // Limit gain to prevent clipping
        return gainAdjustment.coerceIn(-12f, 12f)
    }
}

