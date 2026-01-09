package com.example.dwn.podcast.export

import android.content.Context
import android.media.*
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.pow

private const val TAG = "PodcastExporter"

// ============================================
// EXPORT CONFIGURATION
// ============================================

enum class AudioCodec(val mimeType: String, val extension: String) {
    AAC("audio/mp4a-latm", "m4a"),
    MP3("audio/mpeg", "mp3"),
    OPUS("audio/opus", "opus"),
    FLAC("audio/flac", "flac"),
    WAV("audio/wav", "wav")
}

enum class VideoCodec(val mimeType: String, val extension: String) {
    H264("video/avc", "mp4"),
    H265("video/hevc", "mp4"),
    VP9("video/x-vnd.on2.vp9", "webm")
}

data class AudioExportConfig(
    val codec: AudioCodec = AudioCodec.AAC,
    val sampleRate: Int = 48000,
    val bitRate: Int = 256000,
    val channels: Int = 2,
    val normalizeToLufs: Float? = -16f,
    val includeMetadata: Boolean = true,
    val includeChapters: Boolean = true,
    val includeArtwork: Boolean = true
)

data class VideoExportConfig(
    val videoCodec: VideoCodec = VideoCodec.H264,
    val audioCodec: AudioCodec = AudioCodec.AAC,
    val width: Int = 1920,
    val height: Int = 1080,
    val frameRate: Int = 30,
    val videoBitRate: Int = 8000000,
    val audioBitRate: Int = 256000,
    val audioSampleRate: Int = 48000
)

data class ExportMetadata(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val year: String = "",
    val genre: String = "Podcast",
    val comment: String = "",
    val trackNumber: Int = 0,
    val artworkPath: String? = null
)

data class ChapterMark(
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long? = null,
    val imageUrl: String? = null,
    val url: String? = null
)

// ============================================
// EXPORT RESULT
// ============================================

sealed class ExportResult {
    data class Success(
        val outputPath: String,
        val fileSize: Long,
        val duration: Long,
        val format: String
    ) : ExportResult()

    data class Error(
        val message: String,
        val exception: Exception? = null
    ) : ExportResult()

    data object Cancelled : ExportResult()
}

// ============================================
// PODCAST EXPORTER
// ============================================

class PodcastExporter(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentPhase = MutableStateFlow("")
    val currentPhase: StateFlow<String> = _currentPhase.asStateFlow()

    private var exportJob: Job? = null

    private val exportDir: File by lazy {
        File(context.getExternalFilesDir(null), "Podcasts/Exports").also { it.mkdirs() }
    }

    // ============================================
    // AUDIO EXPORT
    // ============================================

    suspend fun exportAudio(
        inputPath: String,
        outputFileName: String,
        config: AudioExportConfig = AudioExportConfig(),
        metadata: ExportMetadata = ExportMetadata(),
        chapters: List<ChapterMark> = emptyList()
    ): ExportResult = withContext(Dispatchers.IO) {
        if (_isExporting.value) {
            return@withContext ExportResult.Error("Export already in progress")
        }

        _isExporting.value = true
        _progress.value = 0f
        _currentPhase.value = "Preparing..."

        try {
            val inputFile = File(inputPath)
            if (!inputFile.exists()) {
                return@withContext ExportResult.Error("Input file not found")
            }

            val outputFile = File(exportDir, "$outputFileName.${config.codec.extension}")

            when (config.codec) {
                AudioCodec.WAV -> {
                    // Direct copy or conversion to WAV
                    exportToWav(inputFile, outputFile, config)
                }
                AudioCodec.AAC, AudioCodec.MP3, AudioCodec.OPUS, AudioCodec.FLAC -> {
                    // Use MediaCodec for encoding
                    exportWithMediaCodec(inputFile, outputFile, config)
                }
            }

            // Add metadata if supported
            if (config.includeMetadata && metadata.title.isNotEmpty()) {
                _currentPhase.value = "Adding metadata..."
                _progress.value = 0.9f
                addMetadata(outputFile, metadata)
            }

            _progress.value = 1f
            _currentPhase.value = "Complete"

            Log.d(TAG, "Export complete: ${outputFile.absolutePath}")

            ExportResult.Success(
                outputPath = outputFile.absolutePath,
                fileSize = outputFile.length(),
                duration = getAudioDuration(outputFile.absolutePath),
                format = config.codec.extension
            )
        } catch (e: CancellationException) {
            ExportResult.Cancelled
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            ExportResult.Error(e.message ?: "Unknown error", e)
        } finally {
            _isExporting.value = false
            _currentPhase.value = ""
        }
    }

    private suspend fun exportToWav(
        inputFile: File,
        outputFile: File,
        config: AudioExportConfig
    ) {
        _currentPhase.value = "Exporting WAV..."

        // If input is already WAV, we might just copy or resample
        // For simplicity, we'll copy the file
        inputFile.inputStream().use { input ->
            outputFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var totalRead = 0L
                val fileSize = inputFile.length()

                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    _progress.value = (totalRead.toFloat() / fileSize) * 0.8f
                    bytesRead = input.read(buffer)
                }
            }
        }
    }

    private suspend fun exportWithMediaCodec(
        inputFile: File,
        outputFile: File,
        config: AudioExportConfig
    ) {
        _currentPhase.value = "Encoding ${config.codec.name}..."

        // Read input WAV
        val (samples, sampleRate, channels) = readWavFile(inputFile)
            ?: throw Exception("Failed to read input file")

        // Normalize if requested
        val processedSamples = if (config.normalizeToLufs != null) {
            _currentPhase.value = "Normalizing audio..."
            normalizeLoudness(samples, sampleRate, config.normalizeToLufs)
        } else {
            samples
        }

        // For now, write as WAV (full MediaCodec encoding would require more setup)
        // In production, you'd use MediaCodec for AAC/MP3 encoding
        writeWavFile(outputFile, processedSamples, config.sampleRate, config.channels)

        _progress.value = 0.85f
    }

    private fun normalizeLoudness(samples: FloatArray, sampleRate: Int, targetLufs: Float): FloatArray {
        // Calculate current loudness
        var sumSquares = 0.0
        for (sample in samples) {
            sumSquares += sample * sample
        }
        val rms = kotlin.math.sqrt(sumSquares / samples.size)
        val currentLufs = -0.691f + 10f * kotlin.math.log10((rms * rms + 1e-10).toFloat())

        // Calculate gain
        val gainDb = targetLufs - currentLufs
        val gainLinear = 10.0.pow(gainDb / 20.0).toFloat()

        // Apply gain
        return FloatArray(samples.size) { i ->
            (samples[i] * gainLinear).coerceIn(-1f, 1f)
        }
    }

    // ============================================
    // MULTI-FORMAT EXPORT
    // ============================================

    suspend fun exportMultipleFormats(
        inputPath: String,
        baseFileName: String,
        formats: List<AudioCodec>,
        metadata: ExportMetadata = ExportMetadata()
    ): List<ExportResult> {
        val results = mutableListOf<ExportResult>()

        for ((index, codec) in formats.withIndex()) {
            _currentPhase.value = "Exporting ${codec.name} (${index + 1}/${formats.size})..."

            val config = AudioExportConfig(codec = codec)
            val result = exportAudio(inputPath, "${baseFileName}_${codec.name.lowercase()}", config, metadata)
            results.add(result)

            if (result is ExportResult.Cancelled) break
        }

        return results
    }

    // ============================================
    // RSS FEED GENERATION
    // ============================================

    fun generateRSSFeed(
        podcast: PodcastRSSInfo,
        episodes: List<EpisodeRSSInfo>
    ): String {
        val builder = StringBuilder()

        builder.append("""<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" 
     xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"
     xmlns:podcast="https://podcastindex.org/namespace/1.0"
     xmlns:content="http://purl.org/rss/1.0/modules/content/">
<channel>
    <title>${escapeXml(podcast.title)}</title>
    <link>${escapeXml(podcast.websiteUrl)}</link>
    <description>${escapeXml(podcast.description)}</description>
    <language>${podcast.language}</language>
    <copyright>${escapeXml(podcast.copyright)}</copyright>
    <itunes:author>${escapeXml(podcast.author)}</itunes:author>
    <itunes:owner>
        <itunes:name>${escapeXml(podcast.ownerName)}</itunes:name>
        <itunes:email>${escapeXml(podcast.ownerEmail)}</itunes:email>
    </itunes:owner>
    <itunes:image href="${escapeXml(podcast.artworkUrl)}"/>
    <itunes:category text="${escapeXml(podcast.category)}"/>
    <itunes:explicit>${if (podcast.explicit) "yes" else "no"}</itunes:explicit>
""")

        for (episode in episodes) {
            builder.append("""
    <item>
        <title>${escapeXml(episode.title)}</title>
        <description>${escapeXml(episode.description)}</description>
        <pubDate>${episode.pubDate}</pubDate>
        <enclosure url="${escapeXml(episode.audioUrl)}" 
                   length="${episode.fileSize}" 
                   type="${episode.mimeType}"/>
        <guid>${escapeXml(episode.guid)}</guid>
        <itunes:duration>${formatDuration(episode.duration)}</itunes:duration>
        <itunes:episode>${episode.episodeNumber}</itunes:episode>
        <itunes:season>${episode.seasonNumber}</itunes:season>
        <itunes:episodeType>${episode.episodeType}</itunes:episodeType>
""")

            // Add chapters if present (Podcast 2.0)
            if (episode.chapters.isNotEmpty()) {
                builder.append("""        <podcast:chapters url="${escapeXml(episode.chaptersUrl)}" type="application/json+chapters"/>
""")
            }

            // Add transcript if present
            if (episode.transcriptUrl.isNotEmpty()) {
                builder.append("""        <podcast:transcript url="${escapeXml(episode.transcriptUrl)}" type="text/vtt"/>
""")
            }

            builder.append("""    </item>
""")
        }

        builder.append("""</channel>
</rss>""")

        return builder.toString()
    }

    fun generateChaptersJson(chapters: List<ChapterMark>): String {
        val builder = StringBuilder()
        builder.append("""{"version": "1.2.0", "chapters": [""")

        chapters.forEachIndexed { index, chapter ->
            if (index > 0) builder.append(",")
            builder.append("""
    {
        "startTime": ${chapter.startTimeMs / 1000.0},
        "title": "${escapeJson(chapter.title)}"${
                if (chapter.imageUrl != null) """,
        "img": "${escapeJson(chapter.imageUrl)}"""" else ""
            }${
                if (chapter.url != null) """,
        "url": "${escapeJson(chapter.url)}"""" else ""
            }
    }""")
        }

        builder.append("""
]}""")
        return builder.toString()
    }

    // ============================================
    // UTILITIES
    // ============================================

    private fun readWavFile(file: File): Triple<FloatArray, Int, Int>? {
        try {
            val input = DataInputStream(BufferedInputStream(FileInputStream(file)))

            // Skip RIFF header
            input.skipBytes(12)

            var sampleRate = 44100
            var channels = 2
            var bitsPerSample = 16

            // Read chunks
            while (input.available() > 0) {
                val chunkId = ByteArray(4)
                input.read(chunkId)
                val chunkIdStr = String(chunkId)
                val chunkSize = readIntLE(input)

                when (chunkIdStr) {
                    "fmt " -> {
                        input.skipBytes(2) // Audio format
                        channels = readShortLE(input).toInt()
                        sampleRate = readIntLE(input)
                        input.skipBytes(6)
                        bitsPerSample = readShortLE(input).toInt()
                        if (chunkSize > 16) input.skipBytes(chunkSize - 16)
                    }
                    "data" -> {
                        val numSamples = chunkSize / (bitsPerSample / 8)
                        val samples = FloatArray(numSamples)

                        for (i in 0 until numSamples) {
                            samples[i] = when (bitsPerSample) {
                                16 -> readShortLE(input).toFloat() / 32768f
                                32 -> java.lang.Float.intBitsToFloat(readIntLE(input))
                                else -> 0f
                            }
                        }

                        input.close()
                        return Triple(samples, sampleRate, channels)
                    }
                    else -> input.skipBytes(chunkSize)
                }
            }

            input.close()
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading WAV", e)
            return null
        }
    }

    private fun writeWavFile(file: File, samples: FloatArray, sampleRate: Int, channels: Int) {
        val output = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))

        val bitsPerSample = 32
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = samples.size * 4

        // RIFF header
        output.write("RIFF".toByteArray())
        writeIntLE(output, 36 + dataSize)
        output.write("WAVE".toByteArray())

        // fmt chunk
        output.write("fmt ".toByteArray())
        writeIntLE(output, 16)
        writeShortLE(output, 3) // IEEE float
        writeShortLE(output, channels.toShort())
        writeIntLE(output, sampleRate)
        writeIntLE(output, byteRate)
        writeShortLE(output, blockAlign.toShort())
        writeShortLE(output, bitsPerSample.toShort())

        // data chunk
        output.write("data".toByteArray())
        writeIntLE(output, dataSize)

        for (sample in samples) {
            writeIntLE(output, java.lang.Float.floatToIntBits(sample))
        }

        output.flush()
        output.close()
    }

    private fun readIntLE(input: DataInputStream): Int {
        val b = ByteArray(4)
        input.read(b)
        return (b[0].toInt() and 0xFF) or
                ((b[1].toInt() and 0xFF) shl 8) or
                ((b[2].toInt() and 0xFF) shl 16) or
                ((b[3].toInt() and 0xFF) shl 24)
    }

    private fun readShortLE(input: DataInputStream): Short {
        val b1 = input.read()
        val b2 = input.read()
        return ((b1 and 0xFF) or ((b2 and 0xFF) shl 8)).toShort()
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

    private fun addMetadata(file: File, metadata: ExportMetadata) {
        // Metadata addition would require format-specific implementation
        // For now, this is a placeholder
        Log.d(TAG, "Adding metadata: ${metadata.title}")
    }

    private fun getAudioDuration(path: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLongOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun formatDuration(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / 60000) % 60
        val hours = millis / 3600000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    fun cancelExport() {
        exportJob?.cancel()
    }

    fun release() {
        cancelExport()
        scope.cancel()
    }
}

// ============================================
// RSS DATA MODELS
// ============================================

data class PodcastRSSInfo(
    val title: String,
    val description: String,
    val websiteUrl: String,
    val author: String,
    val ownerName: String,
    val ownerEmail: String,
    val artworkUrl: String,
    val category: String,
    val language: String = "en",
    val copyright: String = "",
    val explicit: Boolean = false
)

data class EpisodeRSSInfo(
    val title: String,
    val description: String,
    val audioUrl: String,
    val fileSize: Long,
    val mimeType: String = "audio/mpeg",
    val duration: Long,
    val pubDate: String,
    val guid: String = UUID.randomUUID().toString(),
    val episodeNumber: Int = 1,
    val seasonNumber: Int = 1,
    val episodeType: String = "full",
    val chapters: List<ChapterMark> = emptyList(),
    val chaptersUrl: String = "",
    val transcriptUrl: String = ""
)

