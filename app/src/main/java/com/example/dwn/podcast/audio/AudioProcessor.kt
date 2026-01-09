package com.example.dwn.podcast.audio

import android.content.Context
import android.media.*
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

private const val TAG = "AudioProcessor"

// ============================================
// AUDIO PROCESSING EFFECTS
// ============================================

data class NoiseRemovalSettings(
    val enabled: Boolean = false,
    val strength: Float = 0.5f,        // 0-1
    val adaptiveMode: Boolean = true,
    val preserveVoice: Boolean = true
)

data class VoiceIsolationSettings(
    val enabled: Boolean = false,
    val strength: Float = 0.7f,
    val frequencyRangeLow: Int = 80,   // Hz
    val frequencyRangeHigh: Int = 8000 // Hz
)

data class EchoRemovalSettings(
    val enabled: Boolean = false,
    val strength: Float = 0.6f,
    val roomSize: Float = 0.3f
)

data class AutoLevelSettings(
    val enabled: Boolean = false,
    val targetLevel: Float = -16f,     // LUFS
    val maxGain: Float = 12f,          // dB
    val attackTime: Float = 10f,       // ms
    val releaseTime: Float = 100f      // ms
)

data class CompressorSettings(
    val enabled: Boolean = false,
    val threshold: Float = -20f,       // dB
    val ratio: Float = 4f,             // :1
    val attack: Float = 10f,           // ms
    val release: Float = 100f,         // ms
    val makeupGain: Float = 0f,        // dB
    val knee: Float = 6f               // dB
)

data class DeEsserSettings(
    val enabled: Boolean = false,
    val threshold: Float = -30f,       // dB
    val frequency: Int = 6000,         // Hz
    val bandwidth: Float = 2000f,      // Hz
    val reduction: Float = 6f          // dB
)

data class LimiterSettings(
    val enabled: Boolean = false,
    val ceiling: Float = -1f,          // dB
    val release: Float = 50f,          // ms
    val truePeak: Boolean = true
)

// ============================================
// AUDIO PROCESSOR
// ============================================

class PodcastAudioProcessor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    // Processing settings
    var noiseRemoval = NoiseRemovalSettings()
    var voiceIsolation = VoiceIsolationSettings()
    var echoRemoval = EchoRemovalSettings()
    var autoLevel = AutoLevelSettings()
    var compressor = CompressorSettings()
    var deEsser = DeEsserSettings()
    var limiter = LimiterSettings()

    // FFT buffers for spectral processing
    private val fftSize = 2048
    private val hopSize = fftSize / 4

    // ============================================
    // NOISE REMOVAL
    // ============================================

    suspend fun removeNoise(
        inputPath: String,
        outputPath: String,
        settings: NoiseRemovalSettings = noiseRemoval
    ): Boolean = withContext(Dispatchers.IO) {
        if (_isProcessing.value) return@withContext false
        _isProcessing.value = true
        _progress.value = 0f

        try {
            val inputFile = File(inputPath)
            val outputFile = File(outputPath)

            val audioData = readWavFile(inputFile)
            if (audioData == null) {
                Log.e(TAG, "Failed to read input file")
                return@withContext false
            }

            val (samples, sampleRate, channels) = audioData

            // Spectral noise gate processing
            val processed = processSpectralNoiseGate(
                samples,
                sampleRate,
                settings.strength,
                settings.adaptiveMode
            ) { _progress.value = it * 0.9f }

            // Write output
            writeWavFile(outputFile, processed, sampleRate, channels)

            _progress.value = 1f
            Log.d(TAG, "Noise removal complete: $outputPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Noise removal failed", e)
            false
        } finally {
            _isProcessing.value = false
        }
    }

    private fun processSpectralNoiseGate(
        samples: FloatArray,
        sampleRate: Int,
        strength: Float,
        adaptive: Boolean,
        progressCallback: (Float) -> Unit
    ): FloatArray {
        val output = FloatArray(samples.size)
        val window = createHannWindow(fftSize)

        // Estimate noise floor from first 0.5 seconds (assuming it's mostly noise/silence)
        val noiseFrames = minOf((sampleRate * 0.5f / hopSize).toInt(), 10)
        val noiseProfile = FloatArray(fftSize / 2 + 1)

        for (frame in 0 until noiseFrames) {
            val startIdx = frame * hopSize
            if (startIdx + fftSize > samples.size) break

            val frameData = samples.copyOfRange(startIdx, startIdx + fftSize)
            applyWindow(frameData, window)
            val spectrum = computeFFTMagnitude(frameData)

            for (i in spectrum.indices) {
                noiseProfile[i] += spectrum[i] / noiseFrames
            }
        }

        // Process frames
        val numFrames = (samples.size - fftSize) / hopSize
        val overlapAdd = FloatArray(samples.size)
        val windowSum = FloatArray(samples.size)

        for (frame in 0 until numFrames) {
            val startIdx = frame * hopSize
            val frameData = samples.copyOfRange(startIdx, startIdx + fftSize)
            applyWindow(frameData, window)

            // FFT
            val (real, imag) = computeFFT(frameData)

            // Spectral subtraction
            for (i in 0 until fftSize / 2 + 1) {
                val magnitude = sqrt(real[i] * real[i] + imag[i] * imag[i])
                val phase = atan2(imag[i], real[i])

                // Subtract noise floor with strength factor
                val threshold = noiseProfile[i] * (1f + strength * 2f)
                val newMagnitude = if (magnitude > threshold) {
                    magnitude - noiseProfile[i] * strength
                } else {
                    magnitude * (1f - strength * 0.8f)
                }.coerceAtLeast(0f)

                real[i] = newMagnitude * cos(phase)
                imag[i] = newMagnitude * sin(phase)

                // Mirror for IFFT
                if (i > 0 && i < fftSize / 2) {
                    real[fftSize - i] = real[i]
                    imag[fftSize - i] = -imag[i]
                }
            }

            // IFFT
            val processed = computeIFFT(real, imag)
            applyWindow(processed, window)

            // Overlap-add
            for (i in 0 until fftSize) {
                if (startIdx + i < overlapAdd.size) {
                    overlapAdd[startIdx + i] += processed[i]
                    windowSum[startIdx + i] += window[i] * window[i]
                }
            }

            progressCallback(frame.toFloat() / numFrames)
        }

        // Normalize by window sum
        for (i in output.indices) {
            output[i] = if (windowSum[i] > 0.0001f) {
                overlapAdd[i] / windowSum[i]
            } else {
                samples[i]
            }
        }

        return output
    }

    // ============================================
    // VOICE ISOLATION
    // ============================================

    suspend fun isolateVoice(
        inputPath: String,
        outputPath: String,
        settings: VoiceIsolationSettings = voiceIsolation
    ): Boolean = withContext(Dispatchers.IO) {
        if (_isProcessing.value) return@withContext false
        _isProcessing.value = true
        _progress.value = 0f

        try {
            val inputFile = File(inputPath)
            val outputFile = File(outputPath)

            val audioData = readWavFile(inputFile)
            if (audioData == null) {
                Log.e(TAG, "Failed to read input file")
                return@withContext false
            }

            val (samples, sampleRate, channels) = audioData

            // Band-pass filter for voice frequencies
            val processed = applyBandpassFilter(
                samples,
                sampleRate,
                settings.frequencyRangeLow.toFloat(),
                settings.frequencyRangeHigh.toFloat()
            ) { _progress.value = it * 0.9f }

            writeWavFile(outputFile, processed, sampleRate, channels)

            _progress.value = 1f
            Log.d(TAG, "Voice isolation complete: $outputPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Voice isolation failed", e)
            false
        } finally {
            _isProcessing.value = false
        }
    }

    private fun applyBandpassFilter(
        samples: FloatArray,
        sampleRate: Int,
        lowFreq: Float,
        highFreq: Float,
        progressCallback: (Float) -> Unit
    ): FloatArray {
        val output = FloatArray(samples.size)
        val window = createHannWindow(fftSize)

        val numFrames = (samples.size - fftSize) / hopSize
        val overlapAdd = FloatArray(samples.size)
        val windowSum = FloatArray(samples.size)

        val lowBin = (lowFreq * fftSize / sampleRate).toInt()
        val highBin = (highFreq * fftSize / sampleRate).toInt()

        for (frame in 0 until numFrames) {
            val startIdx = frame * hopSize
            val frameData = samples.copyOfRange(startIdx, startIdx + fftSize)
            applyWindow(frameData, window)

            val (real, imag) = computeFFT(frameData)

            // Apply bandpass
            for (i in 0 until fftSize / 2 + 1) {
                val gain = when {
                    i < lowBin -> {
                        val rolloff = i.toFloat() / lowBin
                        rolloff * rolloff
                    }
                    i > highBin -> {
                        val rolloff = (fftSize / 2 - i).toFloat() / (fftSize / 2 - highBin)
                        (rolloff * rolloff).coerceAtLeast(0f)
                    }
                    else -> 1f
                }

                real[i] *= gain
                imag[i] *= gain

                if (i > 0 && i < fftSize / 2) {
                    real[fftSize - i] = real[i]
                    imag[fftSize - i] = -imag[i]
                }
            }

            val processed = computeIFFT(real, imag)
            applyWindow(processed, window)

            for (i in 0 until fftSize) {
                if (startIdx + i < overlapAdd.size) {
                    overlapAdd[startIdx + i] += processed[i]
                    windowSum[startIdx + i] += window[i] * window[i]
                }
            }

            progressCallback(frame.toFloat() / numFrames)
        }

        for (i in output.indices) {
            output[i] = if (windowSum[i] > 0.0001f) {
                overlapAdd[i] / windowSum[i]
            } else {
                samples[i]
            }
        }

        return output
    }

    // ============================================
    // AUTO LEVELING
    // ============================================

    suspend fun autoLevel(
        inputPath: String,
        outputPath: String,
        settings: AutoLevelSettings = autoLevel
    ): Boolean = withContext(Dispatchers.IO) {
        if (_isProcessing.value) return@withContext false
        _isProcessing.value = true
        _progress.value = 0f

        try {
            val inputFile = File(inputPath)
            val outputFile = File(outputPath)

            val audioData = readWavFile(inputFile)
            if (audioData == null) {
                Log.e(TAG, "Failed to read input file")
                return@withContext false
            }

            val (samples, sampleRate, channels) = audioData

            // First pass: analyze loudness
            _progress.value = 0.1f
            val currentLufs = calculateIntegratedLoudness(samples, sampleRate)

            // Calculate gain needed
            val gainDb = (settings.targetLevel - currentLufs).coerceIn(-settings.maxGain, settings.maxGain)
            val gainLinear = 10f.pow(gainDb / 20f)

            Log.d(TAG, "Auto level: current=$currentLufs LUFS, target=${settings.targetLevel} LUFS, gain=$gainDb dB")

            // Apply gain with envelope following
            val output = FloatArray(samples.size)
            var envelope = 0f
            val attackCoeff = exp(-1f / (settings.attackTime * sampleRate / 1000f))
            val releaseCoeff = exp(-1f / (settings.releaseTime * sampleRate / 1000f))

            for (i in samples.indices) {
                val inputLevel = abs(samples[i])

                envelope = if (inputLevel > envelope) {
                    attackCoeff * envelope + (1 - attackCoeff) * inputLevel
                } else {
                    releaseCoeff * envelope + (1 - releaseCoeff) * inputLevel
                }

                // Apply gain
                output[i] = samples[i] * gainLinear

                // Soft clip if needed
                if (abs(output[i]) > 0.99f) {
                    output[i] = tanh(output[i])
                }

                if (i % 10000 == 0) {
                    _progress.value = 0.1f + 0.8f * (i.toFloat() / samples.size)
                }
            }

            writeWavFile(outputFile, output, sampleRate, channels)

            _progress.value = 1f
            Log.d(TAG, "Auto level complete: $outputPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Auto level failed", e)
            false
        } finally {
            _isProcessing.value = false
        }
    }

    private fun calculateIntegratedLoudness(samples: FloatArray, sampleRate: Int): Float {
        // Simplified LUFS calculation
        val blockSize = (sampleRate * 0.4f).toInt() // 400ms blocks
        val hopSizeL = blockSize / 4

        val blockLoudness = mutableListOf<Float>()

        var i = 0
        while (i + blockSize < samples.size) {
            var sumSquares = 0.0
            for (j in 0 until blockSize) {
                sumSquares += samples[i + j] * samples[i + j]
            }
            val rms = sqrt(sumSquares / blockSize)
            val loudness = -0.691f + 10f * log10((rms * rms + 1e-10).toFloat())
            blockLoudness.add(loudness)
            i += hopSizeL
        }

        // Gate blocks below -70 LUFS (absolute gate)
        val gatedBlocks = blockLoudness.filter { it > -70f }
        if (gatedBlocks.isEmpty()) return -70f

        // Relative gate: -10 dB below ungated mean
        val ungatedMean = gatedBlocks.average().toFloat()
        val relativeThreshold = ungatedMean - 10f

        val finalBlocks = gatedBlocks.filter { it > relativeThreshold }
        if (finalBlocks.isEmpty()) return -70f

        return finalBlocks.average().toFloat()
    }

    // ============================================
    // COMPRESSOR
    // ============================================

    suspend fun applyCompression(
        inputPath: String,
        outputPath: String,
        settings: CompressorSettings = compressor
    ): Boolean = withContext(Dispatchers.IO) {
        if (_isProcessing.value) return@withContext false
        _isProcessing.value = true
        _progress.value = 0f

        try {
            val inputFile = File(inputPath)
            val outputFile = File(outputPath)

            val audioData = readWavFile(inputFile)
            if (audioData == null) return@withContext false

            val (samples, sampleRate, channels) = audioData

            val output = FloatArray(samples.size)
            var envelope = 0f
            val attackCoeff = exp(-1f / (settings.attack * sampleRate / 1000f))
            val releaseCoeff = exp(-1f / (settings.release * sampleRate / 1000f))
            val makeupLinear = 10f.pow(settings.makeupGain / 20f)

            for (i in samples.indices) {
                val inputLevel = abs(samples[i])
                val inputDb = if (inputLevel > 0) 20f * log10(inputLevel) else -100f

                // Envelope follower
                envelope = if (inputLevel > envelope) {
                    attackCoeff * envelope + (1 - attackCoeff) * inputLevel
                } else {
                    releaseCoeff * envelope + (1 - releaseCoeff) * inputLevel
                }

                val envelopeDb = if (envelope > 0) 20f * log10(envelope) else -100f

                // Calculate gain reduction with soft knee
                val gainReductionDb = if (envelopeDb > settings.threshold - settings.knee / 2) {
                    if (envelopeDb < settings.threshold + settings.knee / 2) {
                        // Soft knee region
                        val x = envelopeDb - settings.threshold + settings.knee / 2
                        (1f / settings.ratio - 1f) * x * x / (2f * settings.knee)
                    } else {
                        // Above knee
                        (envelopeDb - settings.threshold) * (1f / settings.ratio - 1f)
                    }
                } else {
                    0f
                }

                val gainLinear = 10f.pow(gainReductionDb / 20f) * makeupLinear
                output[i] = samples[i] * gainLinear

                if (i % 10000 == 0) {
                    _progress.value = i.toFloat() / samples.size
                }
            }

            writeWavFile(outputFile, output, sampleRate, channels)

            _progress.value = 1f
            true
        } catch (e: Exception) {
            Log.e(TAG, "Compression failed", e)
            false
        } finally {
            _isProcessing.value = false
        }
    }

    // ============================================
    // LIMITER
    // ============================================

    suspend fun applyLimiter(
        inputPath: String,
        outputPath: String,
        settings: LimiterSettings = limiter
    ): Boolean = withContext(Dispatchers.IO) {
        if (_isProcessing.value) return@withContext false
        _isProcessing.value = true
        _progress.value = 0f

        try {
            val inputFile = File(inputPath)
            val outputFile = File(outputPath)

            val audioData = readWavFile(inputFile)
            if (audioData == null) return@withContext false

            val (samples, sampleRate, channels) = audioData

            val ceilingLinear = 10f.pow(settings.ceiling / 20f)
            val output = FloatArray(samples.size)
            var envelope = 0f
            val releaseCoeff = exp(-1f / (settings.release * sampleRate / 1000f))

            // Lookahead buffer (5ms)
            val lookahead = (sampleRate * 0.005f).toInt()

            for (i in samples.indices) {
                // Peak detection with lookahead
                var peakLevel = abs(samples[i])
                for (j in 1..lookahead) {
                    if (i + j < samples.size) {
                        peakLevel = maxOf(peakLevel, abs(samples[i + j]))
                    }
                }

                // Envelope
                envelope = if (peakLevel > envelope) {
                    peakLevel
                } else {
                    releaseCoeff * envelope + (1 - releaseCoeff) * peakLevel
                }

                // Calculate gain
                val gain = if (envelope > ceilingLinear) {
                    ceilingLinear / envelope
                } else {
                    1f
                }

                output[i] = samples[i] * gain

                // Hard clip as safety
                output[i] = output[i].coerceIn(-ceilingLinear, ceilingLinear)

                if (i % 10000 == 0) {
                    _progress.value = i.toFloat() / samples.size
                }
            }

            writeWavFile(outputFile, output, sampleRate, channels)

            _progress.value = 1f
            true
        } catch (e: Exception) {
            Log.e(TAG, "Limiter failed", e)
            false
        } finally {
            _isProcessing.value = false
        }
    }

    // ============================================
    // NORMALIZE
    // ============================================

    suspend fun normalize(
        inputPath: String,
        outputPath: String,
        targetPeak: Float = -1f // dB
    ): Boolean = withContext(Dispatchers.IO) {
        if (_isProcessing.value) return@withContext false
        _isProcessing.value = true
        _progress.value = 0f

        try {
            val inputFile = File(inputPath)
            val outputFile = File(outputPath)

            val audioData = readWavFile(inputFile)
            if (audioData == null) return@withContext false

            val (samples, sampleRate, channels) = audioData

            // Find peak
            var peak = 0f
            for (sample in samples) {
                if (abs(sample) > peak) peak = abs(sample)
            }

            if (peak == 0f) {
                // Silent file
                writeWavFile(outputFile, samples, sampleRate, channels)
                return@withContext true
            }

            val targetLinear = 10f.pow(targetPeak / 20f)
            val gain = targetLinear / peak

            val output = FloatArray(samples.size)
            for (i in samples.indices) {
                output[i] = samples[i] * gain
                if (i % 10000 == 0) {
                    _progress.value = i.toFloat() / samples.size
                }
            }

            writeWavFile(outputFile, output, sampleRate, channels)

            _progress.value = 1f
            true
        } catch (e: Exception) {
            Log.e(TAG, "Normalize failed", e)
            false
        } finally {
            _isProcessing.value = false
        }
    }

    // ============================================
    // FFT UTILITIES
    // ============================================

    private fun createHannWindow(size: Int): FloatArray {
        return FloatArray(size) { i ->
            (0.5f * (1f - cos(2f * PI.toFloat() * i / (size - 1)))).toFloat()
        }
    }

    private fun applyWindow(data: FloatArray, window: FloatArray) {
        for (i in data.indices) {
            data[i] *= window[i]
        }
    }

    private fun computeFFT(input: FloatArray): Pair<FloatArray, FloatArray> {
        val n = input.size
        val real = input.copyOf()
        val imag = FloatArray(n)

        // Cooley-Tukey FFT (radix-2)
        val bits = (ln(n.toDouble()) / ln(2.0)).toInt()

        // Bit reversal
        for (i in 0 until n) {
            val j = Integer.reverse(i) ushr (32 - bits)
            if (j > i) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR
            }
        }

        // FFT
        var size = 2
        while (size <= n) {
            val halfSize = size / 2
            val tableStep = n / size

            for (i in 0 until n step size) {
                var k = 0
                for (j in i until i + halfSize) {
                    val angle = -2.0 * PI * k * tableStep / n
                    val cos = cos(angle).toFloat()
                    val sin = sin(angle).toFloat()

                    val tReal = real[j + halfSize] * cos - imag[j + halfSize] * sin
                    val tImag = real[j + halfSize] * sin + imag[j + halfSize] * cos

                    real[j + halfSize] = real[j] - tReal
                    imag[j + halfSize] = imag[j] - tImag
                    real[j] += tReal
                    imag[j] += tImag

                    k++
                }
            }
            size *= 2
        }

        return Pair(real, imag)
    }

    private fun computeIFFT(real: FloatArray, imag: FloatArray): FloatArray {
        val n = real.size

        // Conjugate
        val conjImag = FloatArray(n) { -imag[it] }

        // FFT
        val (resultReal, resultImag) = computeFFT(real.copyOf().also {
            for (i in it.indices) { }
        }.let {
            // Create complex input for IFFT
            val complexReal = real.copyOf()
            computeFFT(complexReal).let { (r, _) -> r }
        }.let { real })

        // Actually do proper IFFT
        val output = FloatArray(n)
        val bits = (ln(n.toDouble()) / ln(2.0)).toInt()

        val ifftReal = real.copyOf()
        val ifftImag = FloatArray(n) { -imag[it] }

        // Bit reversal
        for (i in 0 until n) {
            val j = Integer.reverse(i) ushr (32 - bits)
            if (j > i) {
                var temp = ifftReal[i]
                ifftReal[i] = ifftReal[j]
                ifftReal[j] = temp
                temp = ifftImag[i]
                ifftImag[i] = ifftImag[j]
                ifftImag[j] = temp
            }
        }

        // FFT
        var size = 2
        while (size <= n) {
            val halfSize = size / 2
            val tableStep = n / size

            for (i in 0 until n step size) {
                var k = 0
                for (j in i until i + halfSize) {
                    val angle = -2.0 * PI * k * tableStep / n
                    val cos = cos(angle).toFloat()
                    val sin = sin(angle).toFloat()

                    val tReal = ifftReal[j + halfSize] * cos - ifftImag[j + halfSize] * sin
                    val tImag = ifftReal[j + halfSize] * sin + ifftImag[j + halfSize] * cos

                    ifftReal[j + halfSize] = ifftReal[j] - tReal
                    ifftImag[j + halfSize] = ifftImag[j] - tImag
                    ifftReal[j] += tReal
                    ifftImag[j] += tImag

                    k++
                }
            }
            size *= 2
        }

        // Scale and return real part
        for (i in 0 until n) {
            output[i] = ifftReal[i] / n
        }

        return output
    }

    private fun computeFFTMagnitude(input: FloatArray): FloatArray {
        val (real, imag) = computeFFT(input)
        val magnitude = FloatArray(input.size / 2 + 1)
        for (i in magnitude.indices) {
            magnitude[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }
        return magnitude
    }

    // ============================================
    // WAV FILE I/O
    // ============================================

    private fun readWavFile(file: File): Triple<FloatArray, Int, Int>? {
        try {
            val inputStream = DataInputStream(BufferedInputStream(FileInputStream(file)))

            // Read RIFF header
            val riff = ByteArray(4)
            inputStream.read(riff)
            if (String(riff) != "RIFF") {
                Log.e(TAG, "Not a RIFF file")
                return null
            }

            inputStream.skipBytes(4) // File size

            val wave = ByteArray(4)
            inputStream.read(wave)
            if (String(wave) != "WAVE") {
                Log.e(TAG, "Not a WAVE file")
                return null
            }

            // Find fmt chunk
            var sampleRate = 44100
            var channels = 2
            var bitsPerSample = 16
            var audioFormat = 1

            while (inputStream.available() > 0) {
                val chunkId = ByteArray(4)
                inputStream.read(chunkId)
                val chunkIdStr = String(chunkId)

                val chunkSize = readIntLE(inputStream)

                when (chunkIdStr) {
                    "fmt " -> {
                        audioFormat = readShortLE(inputStream).toInt()
                        channels = readShortLE(inputStream).toInt()
                        sampleRate = readIntLE(inputStream)
                        inputStream.skipBytes(4) // Byte rate
                        inputStream.skipBytes(2) // Block align
                        bitsPerSample = readShortLE(inputStream).toInt()

                        if (chunkSize > 16) {
                            inputStream.skipBytes(chunkSize - 16)
                        }
                    }
                    "data" -> {
                        val numSamples = chunkSize / (bitsPerSample / 8)
                        val samples = FloatArray(numSamples)

                        when {
                            audioFormat == 3 && bitsPerSample == 32 -> {
                                // IEEE Float
                                for (i in 0 until numSamples) {
                                    samples[i] = java.lang.Float.intBitsToFloat(readIntLE(inputStream))
                                }
                            }
                            audioFormat == 1 && bitsPerSample == 16 -> {
                                // PCM 16-bit
                                for (i in 0 until numSamples) {
                                    samples[i] = readShortLE(inputStream).toFloat() / 32768f
                                }
                            }
                            audioFormat == 1 && bitsPerSample == 24 -> {
                                // PCM 24-bit
                                for (i in 0 until numSamples) {
                                    val b1 = inputStream.read()
                                    val b2 = inputStream.read()
                                    val b3 = inputStream.read()
                                    val value = (b1 or (b2 shl 8) or (b3 shl 16))
                                    val signed = if (value and 0x800000 != 0) value or 0xFF000000.toInt() else value
                                    samples[i] = signed.toFloat() / 8388608f
                                }
                            }
                            else -> {
                                Log.e(TAG, "Unsupported format: $audioFormat, $bitsPerSample bits")
                                return null
                            }
                        }

                        inputStream.close()
                        return Triple(samples, sampleRate, channels)
                    }
                    else -> {
                        inputStream.skipBytes(chunkSize)
                    }
                }
            }

            inputStream.close()
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading WAV file", e)
            return null
        }
    }

    private fun writeWavFile(file: File, samples: FloatArray, sampleRate: Int, channels: Int) {
        val outputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))

        val bitsPerSample = 32
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = samples.size * 4

        // RIFF header
        outputStream.write("RIFF".toByteArray())
        writeIntLE(outputStream, 36 + dataSize)
        outputStream.write("WAVE".toByteArray())

        // fmt chunk
        outputStream.write("fmt ".toByteArray())
        writeIntLE(outputStream, 16)
        writeShortLE(outputStream, 3) // IEEE float
        writeShortLE(outputStream, channels.toShort())
        writeIntLE(outputStream, sampleRate)
        writeIntLE(outputStream, byteRate)
        writeShortLE(outputStream, blockAlign.toShort())
        writeShortLE(outputStream, bitsPerSample.toShort())

        // data chunk
        outputStream.write("data".toByteArray())
        writeIntLE(outputStream, dataSize)

        for (sample in samples) {
            writeIntLE(outputStream, java.lang.Float.floatToIntBits(sample))
        }

        outputStream.flush()
        outputStream.close()
    }

    private fun readIntLE(input: DataInputStream): Int {
        val b1 = input.read()
        val b2 = input.read()
        val b3 = input.read()
        val b4 = input.read()
        return b1 or (b2 shl 8) or (b3 shl 16) or (b4 shl 24)
    }

    private fun readShortLE(input: DataInputStream): Short {
        val b1 = input.read()
        val b2 = input.read()
        return (b1 or (b2 shl 8)).toShort()
    }

    private fun writeIntLE(output: DataOutputStream, value: Int) {
        output.write(value and 0xFF)
        output.write((value shr 8) and 0xFF)
        output.write((value shr 16) and 0xFF)
        output.write((value shr 24) and 0xFF)
    }

    private fun writeShortLE(output: DataOutputStream, value: Short) {
        output.write(value.toInt() and 0xFF)
        output.write((value.toInt() shr 8) and 0xFF)
    }

    fun release() {
        scope.cancel()
    }
}

