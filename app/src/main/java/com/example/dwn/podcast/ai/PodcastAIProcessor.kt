package com.example.dwn.podcast.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.*
import kotlin.math.*

private const val TAG = "PodcastAI"

// ============================================
// TRANSCRIPTION MODELS
// ============================================

data class TranscriptWord(
    val word: String,
    val startTime: Long,    // milliseconds
    val endTime: Long,
    val confidence: Float,
    val speakerId: String? = null
)

data class TranscriptSegment(
    val text: String,
    val startTime: Long,
    val endTime: Long,
    val speakerId: String? = null,
    val confidence: Float = 1f,
    val words: List<TranscriptWord> = emptyList()
)

data class Transcript(
    val id: String = UUID.randomUUID().toString(),
    val segments: List<TranscriptSegment>,
    val fullText: String,
    val duration: Long,
    val language: String = "en",
    val speakers: List<Speaker> = emptyList()
)

data class Speaker(
    val id: String,
    val name: String,
    val speakingTime: Long,
    val wordCount: Int
)

// ============================================
// CHAPTER DETECTION
// ============================================

data class DetectedChapter(
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val summary: String = "",
    val keywords: List<String> = emptyList(),
    val confidence: Float = 1f
)

// ============================================
// FILLER WORD DETECTION
// ============================================

data class FillerWordInstance(
    val word: String,
    val startTime: Long,
    val endTime: Long,
    val confidence: Float
)

data class FillerWordAnalysis(
    val instances: List<FillerWordInstance>,
    val wordCounts: Map<String, Int>,
    val totalCount: Int,
    val fillerPerMinute: Float
)

// ============================================
// SHOW NOTES GENERATION
// ============================================

data class ShowNotes(
    val title: String,
    val summary: String,
    val keyPoints: List<String>,
    val topics: List<String>,
    val guests: List<String>,
    val links: List<String>,
    val timestamps: List<Pair<Long, String>>
)

// ============================================
// AI PROCESSOR
// ============================================

class PodcastAIProcessor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentTask = MutableStateFlow("")
    val currentTask: StateFlow<String> = _currentTask.asStateFlow()

    // Common filler words
    private val fillerWords = setOf(
        "um", "uh", "er", "ah", "like", "you know", "actually", "basically",
        "literally", "right", "so", "well", "i mean", "sort of", "kind of"
    )

    // ============================================
    // TRANSCRIPTION
    // ============================================

    suspend fun transcribeAudio(
        audioPath: String,
        language: String = "en"
    ): Transcript? = withContext(Dispatchers.IO) {
        _isProcessing.value = true
        _currentTask.value = "Transcribing audio..."
        _progress.value = 0f

        try {
            val audioFile = File(audioPath)
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file not found: $audioPath")
                return@withContext null
            }

            // Get audio duration
            val duration = getAudioDuration(audioPath)

            // Simulate transcription with realistic results
            // In production, this would use Google Speech-to-Text, Whisper, or similar
            val segments = generateSimulatedTranscript(duration) { progress ->
                _progress.value = progress
            }

            val speakers = identifySpeakers(segments)

            val transcript = Transcript(
                segments = segments,
                fullText = segments.joinToString(" ") { it.text },
                duration = duration,
                language = language,
                speakers = speakers
            )

            _progress.value = 1f
            Log.d(TAG, "Transcription complete: ${segments.size} segments")
            transcript
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            null
        } finally {
            _isProcessing.value = false
            _currentTask.value = ""
        }
    }

    private suspend fun generateSimulatedTranscript(
        duration: Long,
        progressCallback: (Float) -> Unit
    ): List<TranscriptSegment> {
        val segments = mutableListOf<TranscriptSegment>()

        // Sample sentences for simulation
        val sampleSentences = listOf(
            "Welcome to today's episode of the podcast.",
            "We have a really exciting topic to discuss today.",
            "Let me introduce our guest for today's show.",
            "Thanks for having me, it's great to be here.",
            "So let's dive right into the main topic.",
            "That's a really interesting point you raised.",
            "I think there's a lot to unpack there.",
            "Let me share my perspective on this.",
            "Our listeners might be wondering about that.",
            "Can you elaborate a bit more on that?",
            "Absolutely, let me explain further.",
            "This is something I'm really passionate about.",
            "I've been researching this topic for years.",
            "The data really supports this conclusion.",
            "Let's take a quick break and we'll be right back.",
            "Welcome back everyone to the show.",
            "Before we wrap up, any final thoughts?",
            "Thank you so much for joining us today.",
            "Don't forget to subscribe and leave a review.",
            "We'll see you in the next episode."
        )

        var currentTime = 0L
        val avgSegmentDuration = 5000L // 5 seconds average
        val segmentCount = (duration / avgSegmentDuration).toInt().coerceAtLeast(5)

        for (i in 0 until segmentCount) {
            delay(50) // Simulate processing time

            val sentence = sampleSentences[i % sampleSentences.size]
            val segmentDuration = (avgSegmentDuration * (0.7 + Random().nextFloat() * 0.6)).toLong()
            val speakerId = if (i % 3 == 0) "speaker_2" else "speaker_1"

            // Generate words with timing
            val words = sentence.split(" ").mapIndexed { index, word ->
                val wordDuration = segmentDuration / sentence.split(" ").size
                TranscriptWord(
                    word = word,
                    startTime = currentTime + index * wordDuration,
                    endTime = currentTime + (index + 1) * wordDuration,
                    confidence = 0.85f + Random().nextFloat() * 0.15f,
                    speakerId = speakerId
                )
            }

            segments.add(
                TranscriptSegment(
                    text = sentence,
                    startTime = currentTime,
                    endTime = currentTime + segmentDuration,
                    speakerId = speakerId,
                    confidence = 0.9f + Random().nextFloat() * 0.1f,
                    words = words
                )
            )

            currentTime += segmentDuration
            progressCallback(i.toFloat() / segmentCount)

            if (currentTime >= duration) break
        }

        return segments
    }

    private fun identifySpeakers(segments: List<TranscriptSegment>): List<Speaker> {
        val speakerMap = mutableMapOf<String, MutableList<TranscriptSegment>>()

        segments.forEach { segment ->
            segment.speakerId?.let { id ->
                speakerMap.getOrPut(id) { mutableListOf() }.add(segment)
            }
        }

        return speakerMap.map { (id, segs) ->
            Speaker(
                id = id,
                name = if (id == "speaker_1") "Host" else "Guest ${id.substringAfter("_")}",
                speakingTime = segs.sumOf { it.endTime - it.startTime },
                wordCount = segs.sumOf { it.text.split(" ").size }
            )
        }
    }

    // ============================================
    // SPEAKER DIARIZATION
    // ============================================

    suspend fun diarizeSpeakers(
        audioPath: String,
        expectedSpeakers: Int = 2
    ): List<TranscriptSegment> = withContext(Dispatchers.IO) {
        _isProcessing.value = true
        _currentTask.value = "Identifying speakers..."
        _progress.value = 0f

        try {
            val duration = getAudioDuration(audioPath)
            val segments = mutableListOf<TranscriptSegment>()

            // Simulate diarization
            var currentTime = 0L
            val segmentDuration = 10000L // 10 second segments
            var currentSpeaker = 1

            while (currentTime < duration) {
                delay(100)

                // Randomly switch speakers occasionally
                if (Random().nextFloat() < 0.3) {
                    currentSpeaker = if (currentSpeaker == 1) 2 else 1
                }

                val endTime = minOf(currentTime + segmentDuration, duration)

                segments.add(
                    TranscriptSegment(
                        text = "",
                        startTime = currentTime,
                        endTime = endTime,
                        speakerId = "speaker_$currentSpeaker"
                    )
                )

                currentTime = endTime
                _progress.value = currentTime.toFloat() / duration
            }

            _progress.value = 1f
            Log.d(TAG, "Diarization complete: ${segments.size} segments")
            segments
        } catch (e: Exception) {
            Log.e(TAG, "Diarization failed", e)
            emptyList()
        } finally {
            _isProcessing.value = false
            _currentTask.value = ""
        }
    }

    // ============================================
    // CHAPTER DETECTION
    // ============================================

    suspend fun detectChapters(
        transcript: Transcript
    ): List<DetectedChapter> = withContext(Dispatchers.Default) {
        _isProcessing.value = true
        _currentTask.value = "Detecting chapters..."
        _progress.value = 0f

        try {
            val chapters = mutableListOf<DetectedChapter>()

            // Analyze transcript for topic changes
            val segments = transcript.segments
            if (segments.isEmpty()) return@withContext emptyList()

            // Topic keywords for chapter detection
            val topicIndicators = listOf(
                "let's talk about", "moving on to", "next topic", "another thing",
                "let me introduce", "speaking of", "on the subject of",
                "let's discuss", "turning to", "regarding", "about"
            )

            val introIndicators = listOf("welcome", "hello", "hi everyone", "today we")
            val outroIndicators = listOf("thank you", "thanks for", "see you", "goodbye", "wrap up")

            var lastChapterStart = 0L
            var chapterCount = 0

            // Always add intro chapter
            chapters.add(
                DetectedChapter(
                    title = "Introduction",
                    startTime = 0,
                    endTime = minOf(60000, transcript.duration),
                    summary = "Episode introduction and welcome",
                    keywords = listOf("introduction", "welcome"),
                    confidence = 0.95f
                )
            )
            lastChapterStart = 60000
            chapterCount++

            // Scan for topic changes
            for ((index, segment) in segments.withIndex()) {
                delay(20)
                _progress.value = index.toFloat() / segments.size * 0.8f

                val textLower = segment.text.lowercase()

                // Check for topic indicators
                val hasTopicChange = topicIndicators.any { textLower.contains(it) }

                // Check for significant time gap
                val timeSinceLastChapter = segment.startTime - lastChapterStart
                val shouldCreateChapter = hasTopicChange && timeSinceLastChapter > 120000 // 2 min minimum

                if (shouldCreateChapter && chapterCount < 10) {
                    // Extract topic from text
                    val title = extractChapterTitle(segment.text, chapterCount)

                    // Update previous chapter end time
                    if (chapters.isNotEmpty()) {
                        val lastChapter = chapters.last()
                        chapters[chapters.lastIndex] = lastChapter.copy(endTime = segment.startTime)
                    }

                    chapters.add(
                        DetectedChapter(
                            title = title,
                            startTime = segment.startTime,
                            endTime = transcript.duration,
                            summary = "Discussion about $title",
                            keywords = extractKeywords(segment.text),
                            confidence = 0.8f
                        )
                    )

                    lastChapterStart = segment.startTime
                    chapterCount++
                }
            }

            // Check if we need an outro chapter
            val lastSegment = segments.lastOrNull()
            if (lastSegment != null) {
                val textLower = lastSegment.text.lowercase()
                val isOutro = outroIndicators.any { textLower.contains(it) }

                if (isOutro && chapters.last().title != "Outro") {
                    val outroStart = maxOf(lastSegment.startTime - 60000, chapters.last().startTime + 60000)

                    // Update last chapter end time
                    if (chapters.isNotEmpty()) {
                        val lastChapter = chapters.last()
                        chapters[chapters.lastIndex] = lastChapter.copy(endTime = outroStart)
                    }

                    chapters.add(
                        DetectedChapter(
                            title = "Outro",
                            startTime = outroStart,
                            endTime = transcript.duration,
                            summary = "Closing remarks and farewell",
                            keywords = listOf("outro", "conclusion", "farewell"),
                            confidence = 0.9f
                        )
                    )
                }
            }

            _progress.value = 1f
            Log.d(TAG, "Chapter detection complete: ${chapters.size} chapters")
            chapters
        } catch (e: Exception) {
            Log.e(TAG, "Chapter detection failed", e)
            emptyList()
        } finally {
            _isProcessing.value = false
            _currentTask.value = ""
        }
    }

    private fun extractChapterTitle(text: String, index: Int): String {
        // Try to extract meaningful title from text
        val words = text.split(" ").take(6)
        return if (words.size >= 3) {
            words.joinToString(" ").take(50)
        } else {
            "Chapter ${index + 1}"
        }
    }

    private fun extractKeywords(text: String): List<String> {
        val stopWords = setOf("the", "a", "an", "is", "are", "was", "were", "be", "been",
            "being", "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "can", "to", "of", "in", "for", "on", "with", "at",
            "by", "from", "as", "into", "through", "during", "before", "after", "above",
            "below", "between", "under", "again", "further", "then", "once", "here", "there",
            "when", "where", "why", "how", "all", "each", "few", "more", "most", "other",
            "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too",
            "very", "just", "and", "but", "if", "or", "because", "until", "while", "this",
            "that", "these", "those", "i", "you", "he", "she", "it", "we", "they", "what",
            "which", "who", "whom", "this", "that", "am", "let", "me", "my", "your", "our")

        return text.lowercase()
            .replace(Regex("[^a-z\\s]"), "")
            .split(" ")
            .filter { it.length > 3 && it !in stopWords }
            .groupBy { it }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }

    // ============================================
    // FILLER WORD DETECTION
    // ============================================

    suspend fun detectFillerWords(
        transcript: Transcript
    ): FillerWordAnalysis = withContext(Dispatchers.Default) {
        _isProcessing.value = true
        _currentTask.value = "Detecting filler words..."
        _progress.value = 0f

        try {
            val instances = mutableListOf<FillerWordInstance>()
            val wordCounts = mutableMapOf<String, Int>()

            val allWords = transcript.segments.flatMap { it.words }

            for ((index, word) in allWords.withIndex()) {
                val wordLower = word.word.lowercase().trim()

                if (wordLower in fillerWords) {
                    instances.add(
                        FillerWordInstance(
                            word = wordLower,
                            startTime = word.startTime,
                            endTime = word.endTime,
                            confidence = word.confidence
                        )
                    )
                    wordCounts[wordLower] = (wordCounts[wordLower] ?: 0) + 1
                }

                // Check for multi-word fillers
                if (index < allWords.size - 1) {
                    val twoWords = "$wordLower ${allWords[index + 1].word.lowercase()}"
                    if (twoWords in fillerWords) {
                        instances.add(
                            FillerWordInstance(
                                word = twoWords,
                                startTime = word.startTime,
                                endTime = allWords[index + 1].endTime,
                                confidence = (word.confidence + allWords[index + 1].confidence) / 2
                            )
                        )
                        wordCounts[twoWords] = (wordCounts[twoWords] ?: 0) + 1
                    }
                }

                _progress.value = index.toFloat() / allWords.size
            }

            val totalCount = instances.size
            val durationMinutes = transcript.duration / 60000f
            val fillerPerMinute = if (durationMinutes > 0) totalCount / durationMinutes else 0f

            _progress.value = 1f
            Log.d(TAG, "Filler word detection complete: $totalCount instances")

            FillerWordAnalysis(
                instances = instances,
                wordCounts = wordCounts,
                totalCount = totalCount,
                fillerPerMinute = fillerPerMinute
            )
        } catch (e: Exception) {
            Log.e(TAG, "Filler word detection failed", e)
            FillerWordAnalysis(emptyList(), emptyMap(), 0, 0f)
        } finally {
            _isProcessing.value = false
            _currentTask.value = ""
        }
    }

    // ============================================
    // SHOW NOTES GENERATION
    // ============================================

    suspend fun generateShowNotes(
        transcript: Transcript,
        chapters: List<DetectedChapter>
    ): ShowNotes = withContext(Dispatchers.Default) {
        _isProcessing.value = true
        _currentTask.value = "Generating show notes..."
        _progress.value = 0f

        try {
            delay(500)
            _progress.value = 0.2f

            // Extract title from first segment or generate one
            val title = generateEpisodeTitle(transcript)

            _progress.value = 0.4f

            // Generate summary
            val summary = generateSummary(transcript)

            _progress.value = 0.6f

            // Extract key points
            val keyPoints = extractKeyPoints(transcript, chapters)

            _progress.value = 0.8f

            // Extract topics
            val topics = chapters.map { it.title }.filter { it != "Introduction" && it != "Outro" }

            // Get speaker names
            val guests = transcript.speakers
                .filter { !it.name.equals("Host", ignoreCase = true) }
                .map { it.name }

            // Generate timestamps
            val timestamps = chapters.map { it.startTime to it.title }

            _progress.value = 1f
            Log.d(TAG, "Show notes generation complete")

            ShowNotes(
                title = title,
                summary = summary,
                keyPoints = keyPoints,
                topics = topics,
                guests = guests,
                links = emptyList(), // Would need URL detection
                timestamps = timestamps
            )
        } catch (e: Exception) {
            Log.e(TAG, "Show notes generation failed", e)
            ShowNotes("", "", emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        } finally {
            _isProcessing.value = false
            _currentTask.value = ""
        }
    }

    private fun generateEpisodeTitle(transcript: Transcript): String {
        // Extract key topics and generate a title
        val allText = transcript.fullText.lowercase()
        val keywords = extractKeywords(allText)

        return if (keywords.isNotEmpty()) {
            "Episode: ${keywords.take(3).joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }}"
        } else {
            "Podcast Episode"
        }
    }

    private fun generateSummary(transcript: Transcript): String {
        // Generate a brief summary
        val sentences = transcript.fullText.split(Regex("[.!?]"))
            .filter { it.length > 20 }
            .take(3)

        return if (sentences.isNotEmpty()) {
            sentences.joinToString(". ") { it.trim() } + "."
        } else {
            "This episode covers various topics discussed by the hosts and guests."
        }
    }

    private fun extractKeyPoints(transcript: Transcript, chapters: List<DetectedChapter>): List<String> {
        val keyPoints = mutableListOf<String>()

        // Add chapter summaries as key points
        chapters.forEach { chapter ->
            if (chapter.summary.isNotEmpty() && chapter.title != "Introduction" && chapter.title != "Outro") {
                keyPoints.add("• ${chapter.title}: ${chapter.summary}")
            }
        }

        // Add speaker insights
        transcript.speakers.forEach { speaker ->
            keyPoints.add("• ${speaker.name} spoke for ${speaker.speakingTime / 60000} minutes")
        }

        return keyPoints.take(10)
    }

    // ============================================
    // TITLE SUGGESTIONS
    // ============================================

    suspend fun suggestTitles(
        transcript: Transcript
    ): List<String> = withContext(Dispatchers.Default) {
        _isProcessing.value = true
        _currentTask.value = "Generating title suggestions..."

        try {
            val keywords = extractKeywords(transcript.fullText)
            val suggestions = mutableListOf<String>()

            // Generate various title styles
            if (keywords.isNotEmpty()) {
                // Question style
                suggestions.add("What You Need to Know About ${keywords.first().replaceFirstChar { it.uppercase() }}")

                // Listicle style
                suggestions.add("${keywords.size} Key Insights on ${keywords.first().replaceFirstChar { it.uppercase() }}")

                // Direct style
                suggestions.add("Deep Dive: ${keywords.take(2).joinToString(" & ") { it.replaceFirstChar { c -> c.uppercase() } }}")

                // Conversational style
                val guest = transcript.speakers.find { it.name != "Host" }
                if (guest != null) {
                    suggestions.add("${guest.name} on ${keywords.first().replaceFirstChar { it.uppercase() }}")
                }

                // Hook style
                suggestions.add("The Truth About ${keywords.first().replaceFirstChar { it.uppercase() }}")
            }

            // Add generic suggestions
            suggestions.add("Episode ${Random().nextInt(100) + 1}: Insights and Discussions")

            Log.d(TAG, "Generated ${suggestions.size} title suggestions")
            suggestions
        } catch (e: Exception) {
            Log.e(TAG, "Title suggestion failed", e)
            listOf("Podcast Episode")
        } finally {
            _isProcessing.value = false
            _currentTask.value = ""
        }
    }

    // ============================================
    // UTILITIES
    // ============================================

    private fun getAudioDuration(path: String): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(path)
            val duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            duration?.toLongOrNull() ?: 300000 // Default 5 minutes
        } catch (e: Exception) {
            Log.e(TAG, "Error getting audio duration", e)
            300000 // Default 5 minutes
        }
    }

    fun release() {
        scope.cancel()
    }
}

