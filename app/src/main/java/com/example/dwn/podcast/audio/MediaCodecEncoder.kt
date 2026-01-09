package com.example.dwn.podcast.audio

import android.media.*
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "AudioEncoder"

// ============================================
// ENCODER CONFIGURATION
// ============================================

data class EncoderConfig(
    val codec: AudioEncoderCodec = AudioEncoderCodec.AAC_LC,
    val sampleRate: Int = 48000,
    val channels: Int = 2,
    val bitRate: Int = 256000
)

enum class AudioEncoderCodec(val mimeType: String, val extension: String) {
    AAC_LC(MediaFormat.MIMETYPE_AUDIO_AAC, "m4a"),
    AAC_HE(MediaFormat.MIMETYPE_AUDIO_AAC, "m4a"),
    OPUS(MediaFormat.MIMETYPE_AUDIO_OPUS, "opus"),
    FLAC(MediaFormat.MIMETYPE_AUDIO_FLAC, "flac")
}

// ============================================
// AUDIO ENCODER
// ============================================

class MediaCodecAudioEncoder {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _isEncoding = MutableStateFlow(false)
    val isEncoding: StateFlow<Boolean> = _isEncoding.asStateFlow()

    private var encoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null

    // ============================================
    // ENCODE WAV TO AAC
    // ============================================

    suspend fun encodeToAAC(
        inputWavPath: String,
        outputPath: String,
        config: EncoderConfig = EncoderConfig()
    ): Boolean = withContext(Dispatchers.IO) {
        _isEncoding.value = true
        _progress.value = 0f

        try {
            // Read WAV file
            val wavData = readWavFile(inputWavPath) ?: run {
                Log.e(TAG, "Failed to read WAV file")
                return@withContext false
            }

            val (pcmData, sampleRate, channels) = wavData

            // Setup encoder
            val format = MediaFormat.createAudioFormat(
                config.codec.mimeType,
                config.sampleRate,
                config.channels
            ).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, config.bitRate)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            encoder = MediaCodec.createEncoderByType(config.codec.mimeType)
            encoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder?.start()

            // Setup muxer
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var audioTrack = -1
            var muxerStarted = false

            // Encode
            val bufferInfo = MediaCodec.BufferInfo()
            val timeoutUs = 10000L
            var inputDone = false
            var outputDone = false
            var inputIndex = 0
            var presentationTimeUs = 0L

            val bytesPerSample = 4 // Float32
            val samplesPerFrame = 1024
            val bytesPerFrame = samplesPerFrame * channels * bytesPerSample
            val totalFrames = pcmData.size / samplesPerFrame
            var frameIndex = 0

            while (!outputDone) {
                // Feed input
                if (!inputDone) {
                    val inputBufferId = encoder?.dequeueInputBuffer(timeoutUs) ?: -1
                    if (inputBufferId >= 0) {
                        val inputBuffer = encoder?.getInputBuffer(inputBufferId)
                        inputBuffer?.clear()

                        val startSample = frameIndex * samplesPerFrame
                        val endSample = minOf(startSample + samplesPerFrame, pcmData.size)
                        val samplesToEncode = endSample - startSample

                        if (samplesToEncode > 0) {
                            // Convert float to 16-bit PCM for encoder
                            val pcm16 = ShortArray(samplesToEncode)
                            for (i in 0 until samplesToEncode) {
                                pcm16[i] = (pcmData[startSample + i] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                            }

                            val bytes = ByteArray(samplesToEncode * 2)
                            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm16)

                            inputBuffer?.put(bytes)

                            presentationTimeUs = (frameIndex * samplesPerFrame * 1000000L) / sampleRate

                            encoder?.queueInputBuffer(
                                inputBufferId,
                                0,
                                bytes.size,
                                presentationTimeUs,
                                0
                            )

                            frameIndex++
                            _progress.value = frameIndex.toFloat() / totalFrames
                        } else {
                            encoder?.queueInputBuffer(
                                inputBufferId,
                                0,
                                0,
                                presentationTimeUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        }
                    }
                }

                // Get output
                val outputBufferId = encoder?.dequeueOutputBuffer(bufferInfo, timeoutUs) ?: -1

                when {
                    outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = encoder?.outputFormat
                        newFormat?.let {
                            audioTrack = muxer?.addTrack(it) ?: -1
                            muxer?.start()
                            muxerStarted = true
                        }
                    }
                    outputBufferId >= 0 -> {
                        val outputBuffer = encoder?.getOutputBuffer(outputBufferId)

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size > 0 && muxerStarted && audioTrack >= 0) {
                            outputBuffer?.position(bufferInfo.offset)
                            outputBuffer?.limit(bufferInfo.offset + bufferInfo.size)
                            muxer?.writeSampleData(audioTrack, outputBuffer!!, bufferInfo)
                        }

                        encoder?.releaseOutputBuffer(outputBufferId, false)

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            // Cleanup
            encoder?.stop()
            encoder?.release()
            encoder = null

            muxer?.stop()
            muxer?.release()
            muxer = null

            _progress.value = 1f
            Log.d(TAG, "Encoding complete: $outputPath")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Encoding failed", e)
            cleanup()
            false
        } finally {
            _isEncoding.value = false
        }
    }

    // ============================================
    // ENCODE WAV TO OPUS
    // ============================================

    suspend fun encodeToOpus(
        inputWavPath: String,
        outputPath: String,
        bitRate: Int = 128000
    ): Boolean = withContext(Dispatchers.IO) {
        // Opus encoding requires API 29+ for MediaCodec
        if (android.os.Build.VERSION.SDK_INT < 29) {
            Log.e(TAG, "OPUS encoding requires API 29+")
            return@withContext false
        }

        val config = EncoderConfig(
            codec = AudioEncoderCodec.OPUS,
            bitRate = bitRate
        )

        // Similar implementation as AAC
        encodeToAAC(inputWavPath, outputPath, config)
    }

    // ============================================
    // ENCODE WAV TO FLAC
    // ============================================

    suspend fun encodeToFLAC(
        inputWavPath: String,
        outputPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        _isEncoding.value = true
        _progress.value = 0f

        try {
            val wavData = readWavFile(inputWavPath) ?: return@withContext false
            val (pcmData, sampleRate, channels) = wavData

            // FLAC encoding via MediaCodec
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_FLAC,
                sampleRate,
                channels
            ).apply {
                setInteger(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL, 5)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_FLAC)
            encoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder?.start()

            // Output stream for FLAC
            val outputStream = BufferedOutputStream(FileOutputStream(outputPath))

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var frameIndex = 0
            val samplesPerFrame = 4096
            val totalFrames = pcmData.size / samplesPerFrame

            while (!outputDone) {
                if (!inputDone) {
                    val inputBufferId = encoder?.dequeueInputBuffer(10000) ?: -1
                    if (inputBufferId >= 0) {
                        val inputBuffer = encoder?.getInputBuffer(inputBufferId)
                        inputBuffer?.clear()

                        val startSample = frameIndex * samplesPerFrame
                        val endSample = minOf(startSample + samplesPerFrame, pcmData.size)
                        val samplesToEncode = endSample - startSample

                        if (samplesToEncode > 0) {
                            val pcm16 = ShortArray(samplesToEncode)
                            for (i in 0 until samplesToEncode) {
                                pcm16[i] = (pcmData[startSample + i] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                            }

                            val bytes = ByteArray(samplesToEncode * 2)
                            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm16)
                            inputBuffer?.put(bytes)

                            val pts = (frameIndex * samplesPerFrame * 1000000L) / sampleRate
                            encoder?.queueInputBuffer(inputBufferId, 0, bytes.size, pts, 0)

                            frameIndex++
                            _progress.value = frameIndex.toFloat() / totalFrames
                        } else {
                            encoder?.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        }
                    }
                }

                val outputBufferId = encoder?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
                if (outputBufferId >= 0) {
                    val outputBuffer = encoder?.getOutputBuffer(outputBufferId)

                    if (bufferInfo.size > 0) {
                        val data = ByteArray(bufferInfo.size)
                        outputBuffer?.get(data)
                        outputStream.write(data)
                    }

                    encoder?.releaseOutputBuffer(outputBufferId, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            cleanup()

            _progress.value = 1f
            true

        } catch (e: Exception) {
            Log.e(TAG, "FLAC encoding failed", e)
            cleanup()
            false
        } finally {
            _isEncoding.value = false
        }
    }

    // ============================================
    // WAV FILE READING
    // ============================================

    private fun readWavFile(path: String): Triple<FloatArray, Int, Int>? {
        try {
            val input = DataInputStream(BufferedInputStream(FileInputStream(path)))

            // Read RIFF header
            val riff = ByteArray(4)
            input.read(riff)
            if (String(riff) != "RIFF") {
                Log.e(TAG, "Not a RIFF file")
                return null
            }

            input.skipBytes(4) // File size

            val wave = ByteArray(4)
            input.read(wave)
            if (String(wave) != "WAVE") {
                Log.e(TAG, "Not a WAVE file")
                return null
            }

            var sampleRate = 44100
            var channels = 2
            var bitsPerSample = 16
            var audioFormat = 1

            while (input.available() > 0) {
                val chunkId = ByteArray(4)
                if (input.read(chunkId) < 4) break
                val chunkIdStr = String(chunkId)

                val chunkSize = readIntLE(input)

                when (chunkIdStr) {
                    "fmt " -> {
                        audioFormat = readShortLE(input).toInt()
                        channels = readShortLE(input).toInt()
                        sampleRate = readIntLE(input)
                        input.skipBytes(4) // Byte rate
                        input.skipBytes(2) // Block align
                        bitsPerSample = readShortLE(input).toInt()

                        if (chunkSize > 16) {
                            input.skipBytes(chunkSize - 16)
                        }
                    }
                    "data" -> {
                        val numSamples = chunkSize / (bitsPerSample / 8)
                        val samples = FloatArray(numSamples)

                        when {
                            audioFormat == 3 && bitsPerSample == 32 -> {
                                // IEEE Float
                                for (i in 0 until numSamples) {
                                    samples[i] = java.lang.Float.intBitsToFloat(readIntLE(input))
                                }
                            }
                            audioFormat == 1 && bitsPerSample == 16 -> {
                                // PCM 16-bit
                                for (i in 0 until numSamples) {
                                    samples[i] = readShortLE(input).toFloat() / 32768f
                                }
                            }
                            audioFormat == 1 && bitsPerSample == 24 -> {
                                // PCM 24-bit
                                for (i in 0 until numSamples) {
                                    val b1 = input.read()
                                    val b2 = input.read()
                                    val b3 = input.read()
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

                        input.close()
                        return Triple(samples, sampleRate, channels)
                    }
                    else -> {
                        input.skipBytes(chunkSize)
                    }
                }
            }

            input.close()
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading WAV file", e)
            return null
        }
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

    // ============================================
    // CLEANUP
    // ============================================

    private fun cleanup() {
        try {
            encoder?.stop()
            encoder?.release()
            encoder = null

            muxer?.stop()
            muxer?.release()
            muxer = null
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error", e)
        }
    }

    fun release() {
        cleanup()
        scope.cancel()
    }
}

// ============================================
// BATCH ENCODER
// ============================================

class BatchAudioEncoder {
    private val encoder = MediaCodecAudioEncoder()

    val progress: StateFlow<Float> = encoder.progress
    val isEncoding: StateFlow<Boolean> = encoder.isEncoding

    suspend fun encodeToMultipleFormats(
        inputWavPath: String,
        outputDir: String,
        baseName: String,
        formats: List<AudioEncoderCodec>
    ): Map<AudioEncoderCodec, Boolean> {
        val results = mutableMapOf<AudioEncoderCodec, Boolean>()

        for (codec in formats) {
            val outputPath = "$outputDir/$baseName.${codec.extension}"

            val success = when (codec) {
                AudioEncoderCodec.AAC_LC, AudioEncoderCodec.AAC_HE -> {
                    encoder.encodeToAAC(inputWavPath, outputPath, EncoderConfig(codec = codec))
                }
                AudioEncoderCodec.OPUS -> {
                    encoder.encodeToOpus(inputWavPath, outputPath)
                }
                AudioEncoderCodec.FLAC -> {
                    encoder.encodeToFLAC(inputWavPath, outputPath)
                }
            }

            results[codec] = success
        }

        return results
    }

    fun release() {
        encoder.release()
    }
}

