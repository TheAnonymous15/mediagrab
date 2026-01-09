package com.example.dwn.podcast

import android.content.Context
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.example.dwn.podcast.audio.PodcastAudioRecorder
import com.example.dwn.podcast.audio.PodcastAudioProcessor
import com.example.dwn.podcast.audio.RecordingConfig
import com.example.dwn.podcast.audio.RecordingMetrics
import com.example.dwn.podcast.audio.MediaCodecAudioEncoder
import com.example.dwn.podcast.audio.AudioEncoderCodec
import com.example.dwn.podcast.ai.PodcastAIProcessor
import com.example.dwn.podcast.ai.Transcript
import com.example.dwn.podcast.ai.DetectedChapter
import com.example.dwn.podcast.ai.FillerWordAnalysis
import com.example.dwn.podcast.ai.ShowNotes
import com.example.dwn.podcast.ai.PodcastSpeechRecognizer
import com.example.dwn.podcast.ai.TranscriptionConfig
import com.example.dwn.podcast.ai.TranscriptionProvider
import com.example.dwn.podcast.ai.TranscriptionResult
import com.example.dwn.podcast.export.PodcastExporter
import com.example.dwn.podcast.export.AudioExportConfig
import com.example.dwn.podcast.export.ExportMetadata
import com.example.dwn.podcast.export.ExportResult
import com.example.dwn.podcast.export.AudioCodec
import com.example.dwn.podcast.export.ChapterMark
import com.example.dwn.podcast.export.PodcastRSSInfo
import com.example.dwn.podcast.export.EpisodeRSSInfo
import com.example.dwn.podcast.streaming.PodcastRTMPStreamer
import com.example.dwn.podcast.streaming.StreamConfig
import com.example.dwn.podcast.streaming.StreamStats
import com.example.dwn.podcast.streaming.MultiPlatformStreamer
import com.example.dwn.podcast.webrtc.PodcastWebRTCRoom
import com.example.dwn.podcast.webrtc.Participant
import com.example.dwn.podcast.webrtc.ParticipantRole
import com.example.dwn.podcast.webrtc.RoomState
import com.example.dwn.podcast.webrtc.WebRTCConfig
import com.example.dwn.podcast.video.PodcastVideoRecorder
import com.example.dwn.podcast.video.VideoConfig
import com.example.dwn.podcast.video.VideoRecordingState
import com.example.dwn.podcast.livekit.LiveKitRoomManager
import com.example.dwn.podcast.livekit.LiveKitConfig
import com.example.dwn.podcast.livekit.LiveKitRoomState
import com.example.dwn.podcast.livekit.LiveKitParticipant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "PodcastManager"

// ============================================
// CORE DATA MODELS
// ============================================

data class PodcastProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val coverArtPath: String? = null,
    val episodes: List<PodcastEpisode> = emptyList(),
    val settings: ProjectSettings = ProjectSettings(),
    val collaborators: List<Collaborator> = emptyList(),
    val rssFeedUrl: String? = null,
    val analytics: ProjectAnalytics = ProjectAnalytics()
)

data class PodcastEpisode(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val number: Int = 1,
    val season: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val publishedAt: Long? = null,
    val duration: Long = 0,
    val audioPath: String? = null,
    val videoPath: String? = null,
    val thumbnailPath: String? = null,
    val transcriptPath: String? = null,
    val chapters: List<Chapter> = emptyList(),
    val markers: List<Marker> = emptyList(),
    val tracks: List<AudioTrack> = emptyList(),
    val status: EpisodeStatus = EpisodeStatus.DRAFT,
    val analytics: EpisodeAnalytics = EpisodeAnalytics()
)

data class Chapter(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startTime: Long,
    val endTime: Long? = null,
    val imageUrl: String? = null,
    val url: String? = null
)

data class Marker(
    val id: String = UUID.randomUUID().toString(),
    val type: MarkerType,
    val timestamp: Long,
    val label: String = "",
    val color: Long = 0xFFFF9800
)

enum class MarkerType {
    CHAPTER, AD_SLOT, HIGHLIGHT, CUT, SPONSOR, NOTE
}

data class AudioTrack(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: TrackType,
    val filePath: String? = null,
    val volume: Float = 1f,
    val pan: Float = 0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val effects: List<TrackEffect> = emptyList(),
    val waveformData: FloatArray = floatArrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioTrack
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}

enum class TrackType {
    HOST, GUEST, SYSTEM_AUDIO, MUSIC_BED, SOUND_EFFECT, VOICE_OVER
}

data class TrackEffect(
    val id: String = UUID.randomUUID().toString(),
    val type: EffectType,
    val enabled: Boolean = true,
    val parameters: Map<String, Float> = emptyMap()
)

enum class EffectType {
    NOISE_REMOVAL, VOICE_ISOLATION, ECHO_REMOVAL, AUTO_LEVEL,
    EQ, COMPRESSOR, DE_ESSER, LIMITER, REVERB, DELAY
}

enum class EpisodeStatus {
    DRAFT, RECORDING, EDITING, REVIEW, SCHEDULED, PUBLISHED
}

data class ProjectSettings(
    val sampleRate: Int = 48000,
    val bitDepth: Int = 32,
    val channels: Int = 2,
    val autoBackup: Boolean = true,
    val cloudSync: Boolean = false,
    val loudnessTarget: Float = -16f, // LUFS
    val defaultFormat: ExportFormat = ExportFormat.MP3_320
)

enum class ExportFormat(val label: String, val extension: String) {
    MP3_128("MP3 128kbps", "mp3"),
    MP3_320("MP3 320kbps", "mp3"),
    AAC_256("AAC 256kbps", "m4a"),
    WAV("WAV Lossless", "wav"),
    FLAC("FLAC Lossless", "flac"),
    OGG("OGG Vorbis", "ogg")
}

data class Collaborator(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val role: CollaboratorRole,
    val avatarUrl: String? = null
)

enum class CollaboratorRole(val label: String, val permissions: Set<Permission>) {
    HOST("Host", Permission.entries.toSet()),
    PRODUCER("Producer", setOf(Permission.EDIT, Permission.RECORD, Permission.PUBLISH, Permission.MANAGE_GUESTS)),
    EDITOR("Editor", setOf(Permission.EDIT, Permission.EXPORT)),
    GUEST("Guest", setOf(Permission.RECORD))
}

enum class Permission {
    RECORD, EDIT, PUBLISH, EXPORT, MANAGE_GUESTS, MANAGE_SETTINGS, DELETE
}

data class ProjectAnalytics(
    val totalDownloads: Long = 0,
    val totalListens: Long = 0,
    val averageListenDuration: Long = 0,
    val subscriberCount: Long = 0,
    val topCountries: List<CountryStat> = emptyList(),
    val platformStats: List<PlatformStat> = emptyList()
)

data class EpisodeAnalytics(
    val downloads: Long = 0,
    val listens: Long = 0,
    val completionRate: Float = 0f,
    val averageListenTime: Long = 0,
    val retentionData: List<Float> = emptyList(),
    val chapterEngagement: Map<String, Float> = emptyMap()
)

data class CountryStat(val country: String, val listens: Long)
data class PlatformStat(val platform: String, val listens: Long, val percentage: Float)

// ============================================
// RECORDING STATE
// ============================================

data class RecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val duration: Long = 0,
    val peakLevel: Float = 0f,
    val rmsLevel: Float = 0f,
    val lufsLevel: Float = -60f,
    val isClipping: Boolean = false,
    val activeTracks: List<String> = emptyList(),
    val waveformBuffer: FloatArray = floatArrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RecordingState
        return isRecording == other.isRecording && duration == other.duration
    }
    override fun hashCode(): Int = isRecording.hashCode() + duration.hashCode()
}

data class LiveStreamState(
    val isLive: Boolean = false,
    val platform: StreamPlatform? = null,
    val viewerCount: Int = 0,
    val duration: Long = 0,
    val bitrate: Int = 0,
    val health: StreamHealth = StreamHealth.GOOD
)

enum class StreamPlatform(val label: String) {
    YOUTUBE("YouTube"),
    TWITCH("Twitch"),
    FACEBOOK("Facebook"),
    CUSTOM_RTMP("Custom RTMP"),
    CUSTOM_SRT("Custom SRT")
}

enum class StreamHealth { EXCELLENT, GOOD, FAIR, POOR }

// ============================================
// AI PROCESSING STATE
// ============================================

data class AIProcessingState(
    val isTranscribing: Boolean = false,
    val transcriptionProgress: Float = 0f,
    val isRemovingNoise: Boolean = false,
    val isIsolatingVoices: Boolean = false,
    val isGeneratingChapters: Boolean = false,
    val isGeneratingShowNotes: Boolean = false,
    val detectedFillerWords: List<FillerWord> = emptyList(),
    val speakerSegments: List<SpeakerSegment> = emptyList()
)

data class FillerWord(
    val word: String,
    val startTime: Long,
    val endTime: Long,
    val confidence: Float
)

data class SpeakerSegment(
    val speakerId: String,
    val speakerName: String,
    val startTime: Long,
    val endTime: Long,
    val transcript: String = ""
)

data class TranscriptSegment(
    val text: String,
    val startTime: Long,
    val endTime: Long,
    val speakerId: String? = null,
    val confidence: Float = 1f
)

// ============================================
// REMOTE GUEST STATE
// ============================================

data class RemoteSession(
    val id: String = UUID.randomUUID().toString(),
    val hostId: String,
    val guests: List<RemoteGuest> = emptyList(),
    val isActive: Boolean = false,
    val startTime: Long = 0,
    val sessionCode: String = generateSessionCode()
)

data class RemoteGuest(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val avatarUrl: String? = null,
    val isConnected: Boolean = false,
    val isMuted: Boolean = false,
    val audioLevel: Float = 0f,
    val latency: Int = 0,
    val isRecordingLocally: Boolean = false
)

private fun generateSessionCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars.random() }.joinToString("")
}

// ============================================
// ENHANCED PODCAST MANAGER WITH FULL FUNCTIONALITY
// ============================================

class PodcastManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Core components with REAL functionality
    val audioRecorder = PodcastAudioRecorder(context)
    val audioProcessor = PodcastAudioProcessor(context)
    val aiProcessor = PodcastAIProcessor(context)
    val exporter = PodcastExporter(context)

    // NEW: Advanced components for full functionality
    val audioEncoder = MediaCodecAudioEncoder()
    val speechRecognizer = PodcastSpeechRecognizer(context)
    val rtmpStreamer = PodcastRTMPStreamer(context)
    val multiPlatformStreamer = MultiPlatformStreamer(context)
    val webrtcRoom = PodcastWebRTCRoom(context)
    val videoRecorder = PodcastVideoRecorder(context)

    // LiveKit Room Manager (Future-proof solution for remote guests)
    val liveKitRoom = LiveKitRoomManager(context)

    // Projects
    private val _projects = MutableStateFlow<List<PodcastProject>>(emptyList())
    val projects: StateFlow<List<PodcastProject>> = _projects.asStateFlow()

    private val _currentProject = MutableStateFlow<PodcastProject?>(null)
    val currentProject: StateFlow<PodcastProject?> = _currentProject.asStateFlow()

    private val _currentEpisode = MutableStateFlow<PodcastEpisode?>(null)
    val currentEpisode: StateFlow<PodcastEpisode?> = _currentEpisode.asStateFlow()

    // Recording state - derived from audioRecorder
    val isRecording: StateFlow<Boolean> = audioRecorder.isRecording
    val isPaused: StateFlow<Boolean> = audioRecorder.isPaused
    val recordingMetrics: StateFlow<RecordingMetrics> = audioRecorder.metrics
    val waveformData: StateFlow<FloatArray> = audioRecorder.waveformData

    // Video recording state
    val videoState: StateFlow<VideoRecordingState> = videoRecorder.state

    // WebRTC room state (legacy)
    val webrtcRoomState: StateFlow<RoomState> = webrtcRoom.roomState
    val localParticipant: StateFlow<Participant?> = webrtcRoom.localParticipant

    // LiveKit room state (recommended)
    val liveKitRoomState: StateFlow<LiveKitRoomState> = liveKitRoom.state

    // RTMP streaming stats
    val streamingStats: StateFlow<StreamStats> = rtmpStreamer.stats

    // Combined recording state for UI
    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    // Live Streaming
    private val _liveStreamState = MutableStateFlow(LiveStreamState())
    val liveStreamState: StateFlow<LiveStreamState> = _liveStreamState.asStateFlow()

    // AI Processing state
    private val _aiState = MutableStateFlow(AIProcessingState())
    val aiState: StateFlow<AIProcessingState> = _aiState.asStateFlow()

    // Remote Session
    private val _remoteSession = MutableStateFlow<RemoteSession?>(null)
    val remoteSession: StateFlow<RemoteSession?> = _remoteSession.asStateFlow()

    // Transcription results
    private val _currentTranscript = MutableStateFlow<Transcript?>(null)
    val currentTranscript: StateFlow<Transcript?> = _currentTranscript.asStateFlow()

    // NEW: Real transcription result from speech recognizer
    private val _transcriptionResult = MutableStateFlow<TranscriptionResult?>(null)
    val transcriptionResult: StateFlow<TranscriptionResult?> = _transcriptionResult.asStateFlow()

    private val _detectedChapters = MutableStateFlow<List<DetectedChapter>>(emptyList())
    val detectedChapters: StateFlow<List<DetectedChapter>> = _detectedChapters.asStateFlow()

    private val _fillerWordAnalysis = MutableStateFlow<FillerWordAnalysis?>(null)
    val fillerWordAnalysis: StateFlow<FillerWordAnalysis?> = _fillerWordAnalysis.asStateFlow()

    private val _showNotes = MutableStateFlow<ShowNotes?>(null)
    val showNotes: StateFlow<ShowNotes?> = _showNotes.asStateFlow()

    // Export state
    val isExporting: StateFlow<Boolean> = exporter.isExporting
    val exportProgress: StateFlow<Float> = exporter.progress
    val exportPhase: StateFlow<String> = exporter.currentPhase

    // Processing state
    val isProcessingAudio: StateFlow<Boolean> = audioProcessor.isProcessing
    val audioProcessingProgress: StateFlow<Float> = audioProcessor.progress

    val isAIProcessing: StateFlow<Boolean> = aiProcessor.isProcessing
    val aiProgress: StateFlow<Float> = aiProcessor.progress
    val aiTask: StateFlow<String> = aiProcessor.currentTask

    // Recordings list
    private val _recordings = MutableStateFlow<List<File>>(emptyList())
    val recordings: StateFlow<List<File>> = _recordings.asStateFlow()

    private val podcastDir: File by lazy {
        File(context.getExternalFilesDir(null), "Podcasts").also { it.mkdirs() }
    }

    init {
        loadProjects()
        refreshRecordings()
        observeRecordingState()
    }

    private fun observeRecordingState() {
        scope.launch {
            combine(
                audioRecorder.isRecording,
                audioRecorder.isPaused,
                audioRecorder.metrics,
                audioRecorder.session
            ) { isRecording, isPaused, metrics, session ->
                RecordingState(
                    isRecording = isRecording,
                    isPaused = isPaused,
                    duration = session?.duration ?: 0,
                    peakLevel = metrics.peakLevel,
                    rmsLevel = metrics.rmsLevel,
                    lufsLevel = metrics.lufsIntegrated,
                    isClipping = metrics.isClipping,
                    activeTracks = if (isRecording) listOf("main") else emptyList(),
                    waveformBuffer = floatArrayOf()
                )
            }.collect { state ->
                _recordingState.value = state
            }
        }
    }

    // ============================================
    // PROJECT MANAGEMENT
    // ============================================

    fun createProject(name: String, description: String = ""): PodcastProject {
        val project = PodcastProject(
            name = name,
            description = description
        )
        _projects.value = _projects.value + project
        saveProjects()
        return project
    }

    fun selectProject(projectId: String) {
        _currentProject.value = _projects.value.find { it.id == projectId }
    }

    fun updateProject(project: PodcastProject) {
        _projects.value = _projects.value.map {
            if (it.id == project.id) project.copy(modifiedAt = System.currentTimeMillis())
            else it
        }
        if (_currentProject.value?.id == project.id) {
            _currentProject.value = project
        }
        saveProjects()
    }

    fun deleteProject(projectId: String) {
        _projects.value = _projects.value.filter { it.id != projectId }
        if (_currentProject.value?.id == projectId) {
            _currentProject.value = null
        }
        saveProjects()
    }

    // ============================================
    // EPISODE MANAGEMENT
    // ============================================

    fun createEpisode(projectId: String, title: String): PodcastEpisode? {
        val project = _projects.value.find { it.id == projectId } ?: return null
        val episodeNumber = project.episodes.size + 1

        val episode = PodcastEpisode(
            title = title,
            number = episodeNumber
        )

        val updatedProject = project.copy(
            episodes = project.episodes + episode,
            modifiedAt = System.currentTimeMillis()
        )
        updateProject(updatedProject)
        return episode
    }

    fun selectEpisode(episodeId: String) {
        _currentEpisode.value = _currentProject.value?.episodes?.find { it.id == episodeId }
    }

    fun updateEpisode(episode: PodcastEpisode) {
        val project = _currentProject.value ?: return
        val updatedEpisodes = project.episodes.map { if (it.id == episode.id) episode else it }
        updateProject(project.copy(episodes = updatedEpisodes))
        if (_currentEpisode.value?.id == episode.id) {
            _currentEpisode.value = episode
        }
    }

    /**
     * Create a new episode from a recording file
     */
    fun createEpisodeFromRecording(
        projectId: String,
        title: String,
        recordingPath: String
    ): PodcastEpisode? {
        val project = _projects.value.find { it.id == projectId } ?: return null
        val recordingFile = File(recordingPath)

        if (!recordingFile.exists()) {
            Log.e(TAG, "Recording file not found: $recordingPath")
            return null
        }

        // Estimate duration from file size (rough approximation)
        // For 48kHz stereo float: bytes / (48000 * 2 * 4) = seconds
        val estimatedDuration = (recordingFile.length() / (48000 * 2 * 4)) * 1000

        val episode = PodcastEpisode(
            title = title,
            number = project.episodes.size + 1,
            audioPath = recordingPath,
            duration = estimatedDuration,
            status = EpisodeStatus.DRAFT
        )

        val updatedProject = project.copy(
            episodes = project.episodes + episode,
            modifiedAt = System.currentTimeMillis()
        )

        updateProject(updatedProject)
        _currentEpisode.value = episode

        Log.d(TAG, "Created episode '${episode.title}' from recording: $recordingPath")
        return episode
    }

    // ============================================
    // RECORDING - REAL FUNCTIONALITY
    // ============================================

    fun hasRecordingPermission(): Boolean = audioRecorder.hasRecordingPermission()

    fun startRecording(config: RecordingConfig = RecordingConfig()): Boolean {
        Log.d(TAG, "Starting recording...")
        return audioRecorder.startRecording(config)
    }

    fun pauseRecording() {
        Log.d(TAG, "Pausing recording...")
        audioRecorder.pauseRecording()
    }

    fun resumeRecording() {
        Log.d(TAG, "Resuming recording...")
        audioRecorder.resumeRecording()
    }

    fun stopRecording(): String? {
        Log.d(TAG, "Stopping recording...")
        val session = audioRecorder.stopRecording()
        refreshRecordings()

        session?.let { s ->
            // Update current episode with recording
            _currentEpisode.value?.let { episode ->
                updateEpisode(episode.copy(
                    audioPath = s.outputPath,
                    duration = s.duration
                ))
            }
        }

        return session?.outputPath
    }

    fun refreshRecordings() {
        _recordings.value = audioRecorder.listRecordings()
    }

    fun deleteRecording(path: String): Boolean {
        val result = audioRecorder.deleteRecording(path)
        if (result) refreshRecordings()
        return result
    }

    // ============================================
    // AUDIO PROCESSING - REAL FUNCTIONALITY
    // ============================================

    suspend fun removeNoise(inputPath: String, outputPath: String? = null): Boolean {
        val output = outputPath ?: inputPath.replace(".wav", "_denoised.wav")
        _aiState.value = _aiState.value.copy(isRemovingNoise = true)

        val result = audioProcessor.removeNoise(inputPath, output)

        _aiState.value = _aiState.value.copy(isRemovingNoise = false)
        refreshRecordings()
        return result
    }

    suspend fun isolateVoices(inputPath: String, outputPath: String? = null): Boolean {
        val output = outputPath ?: inputPath.replace(".wav", "_isolated.wav")
        _aiState.value = _aiState.value.copy(isIsolatingVoices = true)

        val result = audioProcessor.isolateVoice(inputPath, output)

        _aiState.value = _aiState.value.copy(isIsolatingVoices = false)
        refreshRecordings()
        return result
    }

    suspend fun autoLevel(inputPath: String, outputPath: String? = null, targetLufs: Float = -16f): Boolean {
        val output = outputPath ?: inputPath.replace(".wav", "_leveled.wav")

        audioProcessor.autoLevel = audioProcessor.autoLevel.copy(
            enabled = true,
            targetLevel = targetLufs
        )

        return audioProcessor.autoLevel(inputPath, output)
    }

    suspend fun applyCompression(inputPath: String, outputPath: String? = null): Boolean {
        val output = outputPath ?: inputPath.replace(".wav", "_compressed.wav")
        return audioProcessor.applyCompression(inputPath, output)
    }

    suspend fun applyLimiter(inputPath: String, outputPath: String? = null): Boolean {
        val output = outputPath ?: inputPath.replace(".wav", "_limited.wav")
        return audioProcessor.applyLimiter(inputPath, output)
    }

    suspend fun normalize(inputPath: String, outputPath: String? = null, targetPeak: Float = -1f): Boolean {
        val output = outputPath ?: inputPath.replace(".wav", "_normalized.wav")
        return audioProcessor.normalize(inputPath, output, targetPeak)
    }

    // ============================================
    // AI PROCESSING - REAL FUNCTIONALITY
    // ============================================

    suspend fun transcribeAudio(audioPath: String): Transcript? {
        _aiState.value = _aiState.value.copy(
            isTranscribing = true,
            transcriptionProgress = 0f
        )

        scope.launch {
            aiProcessor.progress.collect { progress ->
                _aiState.value = _aiState.value.copy(transcriptionProgress = progress)
            }
        }

        val transcript = aiProcessor.transcribeAudio(audioPath)

        _currentTranscript.value = transcript
        _aiState.value = _aiState.value.copy(
            isTranscribing = false,
            transcriptionProgress = 1f,
            speakerSegments = transcript?.segments?.mapNotNull { seg ->
                seg.speakerId?.let { id ->
                    SpeakerSegment(
                        speakerId = id,
                        speakerName = transcript.speakers.find { it.id == id }?.name ?: id,
                        startTime = seg.startTime,
                        endTime = seg.endTime,
                        transcript = seg.text
                    )
                }
            } ?: emptyList()
        )

        // Update episode with transcript
        transcript?.let { t ->
            _currentEpisode.value?.let { episode ->
                val transcriptFile = File(podcastDir, "transcripts/${episode.id}.txt")
                transcriptFile.parentFile?.mkdirs()
                transcriptFile.writeText(t.fullText)
                updateEpisode(episode.copy(transcriptPath = transcriptFile.absolutePath))
            }
        }

        return transcript
    }

    suspend fun generateChapters(audioPath: String? = null): List<DetectedChapter> {
        _aiState.value = _aiState.value.copy(isGeneratingChapters = true)

        val transcript = _currentTranscript.value ?: run {
            // Need to transcribe first
            val path = audioPath ?: _currentEpisode.value?.audioPath ?: return emptyList()
            transcribeAudio(path)
            _currentTranscript.value
        } ?: return emptyList()

        val chapters = aiProcessor.detectChapters(transcript)
        _detectedChapters.value = chapters

        // Update episode with chapters
        _currentEpisode.value?.let { episode ->
            val episodeChapters = chapters.map { ch ->
                Chapter(
                    title = ch.title,
                    startTime = ch.startTime,
                    endTime = ch.endTime
                )
            }
            updateEpisode(episode.copy(chapters = episodeChapters))
        }

        _aiState.value = _aiState.value.copy(isGeneratingChapters = false)
        return chapters
    }

    suspend fun detectFillerWords(): FillerWordAnalysis? {
        val transcript = _currentTranscript.value ?: return null

        val analysis = aiProcessor.detectFillerWords(transcript)
        _fillerWordAnalysis.value = analysis

        _aiState.value = _aiState.value.copy(
            detectedFillerWords = analysis.instances.map { fw ->
                FillerWord(
                    word = fw.word,
                    startTime = fw.startTime,
                    endTime = fw.endTime,
                    confidence = fw.confidence
                )
            }
        )

        return analysis
    }

    suspend fun generateShowNotes(): ShowNotes? {
        _aiState.value = _aiState.value.copy(isGeneratingShowNotes = true)

        val transcript = _currentTranscript.value ?: run {
            _aiState.value = _aiState.value.copy(isGeneratingShowNotes = false)
            return null
        }

        val chapters = _detectedChapters.value.ifEmpty {
            generateChapters()
        }

        val showNotes = aiProcessor.generateShowNotes(transcript, chapters)
        _showNotes.value = showNotes

        // Update episode description
        _currentEpisode.value?.let { episode ->
            updateEpisode(episode.copy(description = showNotes.summary))
        }

        _aiState.value = _aiState.value.copy(isGeneratingShowNotes = false)
        return showNotes
    }

    suspend fun suggestTitles(): List<String> {
        val transcript = _currentTranscript.value ?: return emptyList()
        return aiProcessor.suggestTitles(transcript)
    }

    // ============================================
    // EXPORT - REAL FUNCTIONALITY
    // ============================================

    suspend fun exportEpisode(
        episodeId: String,
        codec: AudioCodec = AudioCodec.AAC,
        normalizeToLufs: Float? = -16f
    ): ExportResult {
        val episode = _currentProject.value?.episodes?.find { it.id == episodeId }
            ?: return ExportResult.Error("Episode not found")

        val audioPath = episode.audioPath
            ?: return ExportResult.Error("No audio file for episode")

        val config = AudioExportConfig(
            codec = codec,
            normalizeToLufs = normalizeToLufs,
            includeChapters = episode.chapters.isNotEmpty()
        )

        val metadata = ExportMetadata(
            title = episode.title,
            artist = _currentProject.value?.name ?: "",
            album = _currentProject.value?.name ?: "",
            trackNumber = episode.number
        )

        val outputName = "${episode.title.replace(" ", "_")}_${episode.number}"

        return exporter.exportAudio(audioPath, outputName, config, metadata)
    }

    suspend fun exportMultipleFormats(
        episodeId: String,
        formats: List<AudioCodec>
    ): List<ExportResult> {
        val episode = _currentProject.value?.episodes?.find { it.id == episodeId }
            ?: return listOf(ExportResult.Error("Episode not found"))

        val audioPath = episode.audioPath
            ?: return listOf(ExportResult.Error("No audio file for episode"))

        val metadata = ExportMetadata(
            title = episode.title,
            artist = _currentProject.value?.name ?: "",
            album = _currentProject.value?.name ?: ""
        )

        val baseName = "${episode.title.replace(" ", "_")}_${episode.number}"

        return exporter.exportMultipleFormats(audioPath, baseName, formats, metadata)
    }

    fun generateRSSFeed(): String? {
        val project = _currentProject.value ?: return null

        val podcastInfo = PodcastRSSInfo(
            title = project.name,
            description = project.description,
            websiteUrl = project.rssFeedUrl ?: "https://example.com",
            author = "Podcast Author",
            ownerName = "Podcast Owner",
            ownerEmail = "owner@example.com",
            artworkUrl = project.coverArtPath ?: "",
            category = "Technology"
        )

        val episodes = project.episodes.filter { it.status == EpisodeStatus.PUBLISHED }.map { ep ->
            EpisodeRSSInfo(
                title = ep.title,
                description = ep.description,
                audioUrl = ep.audioPath ?: "",
                fileSize = ep.audioPath?.let { File(it).length() } ?: 0,
                duration = ep.duration,
                pubDate = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
                    .format(Date(ep.publishedAt ?: System.currentTimeMillis())),
                episodeNumber = ep.number,
                seasonNumber = ep.season,
                chapters = ep.chapters.map { ch ->
                    ChapterMark(ch.title, ch.startTime, ch.endTime)
                }
            )
        }

        return exporter.generateRSSFeed(podcastInfo, episodes)
    }

    fun generateChaptersJson(episodeId: String): String? {
        val episode = _currentProject.value?.episodes?.find { it.id == episodeId } ?: return null
        val chapters = episode.chapters.map { ch ->
            ChapterMark(ch.title, ch.startTime, ch.endTime)
        }
        return exporter.generateChaptersJson(chapters)
    }

    // ============================================
    // LIVE STREAMING (PLACEHOLDER - would need RTMP library)
    // ============================================

    fun startLiveStream(platform: StreamPlatform, streamKey: String) {
        _liveStreamState.value = LiveStreamState(
            isLive = true,
            platform = platform,
            duration = 0,
            health = StreamHealth.GOOD
        )

        // Start duration timer
        scope.launch {
            val startTime = System.currentTimeMillis()
            while (_liveStreamState.value.isLive) {
                delay(1000)
                _liveStreamState.value = _liveStreamState.value.copy(
                    duration = System.currentTimeMillis() - startTime
                )
            }
        }

        Log.d(TAG, "Live stream started on ${platform.label}")
    }

    fun stopLiveStream() {
        _liveStreamState.value = LiveStreamState()
        Log.d(TAG, "Live stream stopped")
    }

    // ============================================
    // REMOTE SESSION (PLACEHOLDER - would need WebRTC)
    // ============================================

    fun createRemoteSession(): RemoteSession {
        val session = RemoteSession(
            hostId = "local_host",
            isActive = true,
            startTime = System.currentTimeMillis()
        )
        _remoteSession.value = session
        Log.d(TAG, "Remote session created: ${session.sessionCode}")
        return session
    }

    fun joinRemoteSession(sessionCode: String) {
        Log.d(TAG, "Joining session: $sessionCode")
    }

    fun endRemoteSession() {
        _remoteSession.value = null
        Log.d(TAG, "Remote session ended")
    }

    fun addGuest(name: String): RemoteGuest {
        val guest = RemoteGuest(name = name, isConnected = true)
        val session = _remoteSession.value ?: return guest
        _remoteSession.value = session.copy(guests = session.guests + guest)
        return guest
    }

    fun muteGuest(guestId: String, muted: Boolean) {
        val session = _remoteSession.value ?: return
        _remoteSession.value = session.copy(
            guests = session.guests.map {
                if (it.id == guestId) it.copy(isMuted = muted) else it
            }
        )
    }

    fun removeGuest(guestId: String) {
        val session = _remoteSession.value ?: return
        _remoteSession.value = session.copy(
            guests = session.guests.filter { it.id != guestId }
        )
    }

    // ============================================
    // PERSISTENCE
    // ============================================

    private fun loadProjects() {
        // Load from SharedPreferences or database
        val prefs = context.getSharedPreferences("podcast_projects", Context.MODE_PRIVATE)
        val projectsJson = prefs.getString("projects", null)

        if (projectsJson == null) {
            // Create sample project
            _projects.value = listOf(
                PodcastProject(
                    name = "My First Podcast",
                    description = "A podcast about interesting topics",
                    episodes = listOf(
                        PodcastEpisode(
                            title = "Welcome Episode",
                            number = 1,
                            duration = 1800000,
                            status = EpisodeStatus.DRAFT
                        )
                    )
                )
            )
        } else {
            // Parse JSON - simplified for now
            _projects.value = listOf(
                PodcastProject(
                    name = "My Podcast",
                    description = "Loaded from storage"
                )
            )
        }
    }

    private fun saveProjects() {
        val prefs = context.getSharedPreferences("podcast_projects", Context.MODE_PRIVATE)
        // Save projects - simplified
        prefs.edit().putString("projects_count", _projects.value.size.toString()).apply()
        Log.d(TAG, "Projects saved: ${_projects.value.size}")
    }

    // ============================================
    // RTMP LIVE STREAMING - REAL FUNCTIONALITY
    // ============================================

    fun setupCameraForStreaming(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useFrontCamera: Boolean = true
    ) {
        rtmpStreamer.setupCamera(lifecycleOwner, previewView, useFrontCamera)
    }

    fun startRTMPStream(
        rtmpUrl: String,
        streamKey: String,
        videoWidth: Int = 1280,
        videoHeight: Int = 720,
        videoBitrate: Int = 2500000,
        enableVideo: Boolean = true,
        enableAudio: Boolean = true
    ): Boolean {
        val config = StreamConfig(
            rtmpUrl = rtmpUrl,
            streamKey = streamKey,
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            videoBitrate = videoBitrate,
            enableVideo = enableVideo,
            enableAudio = enableAudio
        )

        val result = rtmpStreamer.startStreaming(config)

        if (result) {
            _liveStreamState.value = LiveStreamState(
                isLive = true,
                platform = StreamPlatform.CUSTOM_RTMP,
                health = StreamHealth.GOOD
            )

            // Update stream duration
            scope.launch {
                val startTime = System.currentTimeMillis()
                while (rtmpStreamer.isStreaming.value) {
                    delay(1000)
                    _liveStreamState.value = _liveStreamState.value.copy(
                        duration = System.currentTimeMillis() - startTime,
                        bitrate = videoBitrate,
                        health = when (streamingStats.value.health) {
                            com.example.dwn.podcast.streaming.StreamHealth.EXCELLENT -> StreamHealth.EXCELLENT
                            com.example.dwn.podcast.streaming.StreamHealth.GOOD -> StreamHealth.GOOD
                            com.example.dwn.podcast.streaming.StreamHealth.FAIR -> StreamHealth.FAIR
                            com.example.dwn.podcast.streaming.StreamHealth.POOR -> StreamHealth.POOR
                            else -> StreamHealth.GOOD
                        }
                    )
                }
            }
        }

        return result
    }

    fun stopRTMPStream() {
        rtmpStreamer.stopStreaming()
        _liveStreamState.value = LiveStreamState()
    }

    // ============================================
    // WEBRTC REMOTE GUESTS - REAL FUNCTIONALITY
    // ============================================

    fun createWebRTCRoom(hostName: String): String {
        val roomCode = webrtcRoom.createRoom(hostName, WebRTCConfig(
            enableVideo = true,
            enableAudio = true
        ))

        // Update remote session state
        _remoteSession.value = RemoteSession(
            hostId = webrtcRoom.localParticipant.value?.id ?: "",
            isActive = true,
            sessionCode = roomCode,
            startTime = System.currentTimeMillis()
        )

        return roomCode
    }

    fun joinWebRTCRoom(roomCode: String, participantName: String): Boolean {
        val result = webrtcRoom.joinRoom(roomCode, participantName, ParticipantRole.GUEST)

        if (result) {
            _remoteSession.value = RemoteSession(
                hostId = "",
                isActive = true,
                sessionCode = roomCode,
                startTime = System.currentTimeMillis()
            )
        }

        return result
    }

    fun leaveWebRTCRoom() {
        webrtcRoom.leaveRoom()
        _remoteSession.value = null
    }

    fun setLocalAudioEnabled(enabled: Boolean) {
        webrtcRoom.setLocalAudioEnabled(enabled)
    }

    fun setLocalVideoEnabled(enabled: Boolean) {
        webrtcRoom.setLocalVideoEnabled(enabled)
    }

    fun promoteToCoHost(participantId: String) {
        webrtcRoom.addCoHost(participantId)
    }

    fun demoteFromCoHost(participantId: String) {
        webrtcRoom.removeCoHost(participantId)
    }

    fun kickParticipant(participantId: String) {
        webrtcRoom.kickParticipant(participantId)
    }

    fun startWebRTCRecording(): Boolean {
        return webrtcRoom.startRecording()
    }

    fun stopWebRTCRecording(): Long {
        return webrtcRoom.stopRecording()
    }

    // ============================================
    // VIDEO RECORDING - REAL FUNCTIONALITY
    // ============================================

    fun initializeVideoRecorder(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        config: VideoConfig = VideoConfig()
    ) {
        videoRecorder.initializeCamera(lifecycleOwner, previewView, config)
    }

    fun startVideoRecording(): Boolean {
        return videoRecorder.startRecording()
    }

    fun pauseVideoRecording() {
        videoRecorder.pauseRecording()
    }

    fun resumeVideoRecording() {
        videoRecorder.resumeRecording()
    }

    fun stopVideoRecording(): String? {
        val path = videoRecorder.stopRecording()

        // Update current episode with video
        path?.let { videoPath ->
            _currentEpisode.value?.let { episode ->
                updateEpisode(episode.copy(videoPath = videoPath))
            }
        }

        return path
    }

    fun switchVideoCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        videoRecorder.switchCamera(lifecycleOwner, previewView)
    }

    // ============================================
    // REAL AI TRANSCRIPTION
    // ============================================

    suspend fun transcribeWithWhisper(
        audioPath: String,
        apiKey: String,
        language: String = "en"
    ): TranscriptionResult? {
        _aiState.value = _aiState.value.copy(isTranscribing = true)

        val config = TranscriptionConfig(
            provider = TranscriptionProvider.WHISPER_API,
            apiKey = apiKey,
            language = language,
            enableWordTimestamps = true,
            enableSpeakerDiarization = true
        )

        val result = speechRecognizer.transcribeAudio(audioPath, config)
        _transcriptionResult.value = result

        _aiState.value = _aiState.value.copy(
            isTranscribing = false,
            transcriptionProgress = 1f
        )

        return result
    }

    fun startRealtimeTranscription(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit
    ) {
        speechRecognizer.startRealtimeTranscription(
            language = "en-US",
            onPartialResult = onPartialResult,
            onFinalResult = onFinalResult,
            onError = { error ->
                Log.e(TAG, "Transcription error: $error")
            }
        )
    }

    fun stopRealtimeTranscription() {
        speechRecognizer.stopRealtimeTranscription()
    }

    // ============================================
    // MEDIA CODEC ENCODING - AAC/OPUS/FLAC
    // ============================================

    suspend fun encodeToAAC(inputWavPath: String, outputPath: String): Boolean {
        return audioEncoder.encodeToAAC(inputWavPath, outputPath)
    }

    suspend fun encodeToOpus(inputWavPath: String, outputPath: String): Boolean {
        return audioEncoder.encodeToOpus(inputWavPath, outputPath)
    }

    suspend fun encodeToFLAC(inputWavPath: String, outputPath: String): Boolean {
        return audioEncoder.encodeToFLAC(inputWavPath, outputPath)
    }

    suspend fun encodeToMultipleFormats(
        inputWavPath: String,
        outputDir: String,
        baseName: String
    ): Map<AudioEncoderCodec, Boolean> {
        return audioEncoder.let { enc ->
            val batchEncoder = com.example.dwn.podcast.audio.BatchAudioEncoder()
            batchEncoder.encodeToMultipleFormats(
                inputWavPath,
                outputDir,
                baseName,
                listOf(AudioEncoderCodec.AAC_LC, AudioEncoderCodec.OPUS, AudioEncoderCodec.FLAC)
            )
        }
    }

    // ============================================
    // LIVEKIT - REMOTE GUESTS (RECOMMENDED)
    // ============================================

    /**
     * Configure LiveKit connection
     * Call this before creating/joining rooms
     *
     * @param publicIp Your server's public IP
     * @param liveKitPort LiveKit server port (default 7880)
     * @param tokenServerPort Token server port (default 8081)
     * @param apiKey LiveKit API key
     * @param apiSecret LiveKit API secret
     */
    fun configureLiveKit(
        publicIp: String,
        liveKitPort: Int = 7880,
        tokenServerPort: Int = 8081,
        apiKey: String = "devkey",
        apiSecret: String = "secret"
    ) {
        val config = LiveKitConfig.selfHosted(
            publicIp = publicIp,
            port = liveKitPort,
            apiKey = apiKey,
            apiSecret = apiSecret
        )
        liveKitRoom.configure(config, "http://$publicIp:$tokenServerPort/token")
        Log.d(TAG, "LiveKit configured: ws://$publicIp:$liveKitPort")
    }

    /**
     * Create a LiveKit room and join as host
     * @return Room code (6 characters) or null on failure
     */
    suspend fun createLiveKitRoom(displayName: String): String? {
        return liveKitRoom.createRoom(displayName).getOrNull()
    }

    /**
     * Join an existing LiveKit room as guest
     */
    suspend fun joinLiveKitRoom(roomCode: String, displayName: String): Boolean {
        return liveKitRoom.joinRoom(roomCode, displayName).isSuccess
    }

    /**
     * Leave the current LiveKit room
     */
    fun leaveLiveKitRoom() {
        liveKitRoom.leaveRoom()
    }

    /**
     * Toggle local microphone
     */
    suspend fun setLiveKitMicEnabled(enabled: Boolean) {
        liveKitRoom.setMicrophoneEnabled(enabled)
    }

    /**
     * Toggle local camera
     */
    suspend fun setLiveKitCameraEnabled(enabled: Boolean) {
        liveKitRoom.setCameraEnabled(enabled)
    }

    /**
     * Switch between front and back camera
     */
    suspend fun switchLiveKitCamera() {
        liveKitRoom.switchCamera()
    }

    /**
     * Enable screen sharing
     */
    suspend fun setLiveKitScreenShareEnabled(enabled: Boolean) {
        liveKitRoom.setScreenShareEnabled(enabled)
    }

    /**
     * Mute a remote participant (host only)
     */
    fun muteLiveKitParticipant(participantId: String) {
        liveKitRoom.muteParticipant(participantId)
    }

    /**
     * Kick a participant from the room (host only)
     */
    fun kickLiveKitParticipant(participantId: String) {
        liveKitRoom.kickParticipant(participantId)
    }

    /**
     * Promote a participant to co-host
     */
    fun promoteLiveKitParticipant(participantId: String) {
        liveKitRoom.promoteToCoHost(participantId)
    }

    /**
     * Start server-side recording (requires Egress service)
     */
    suspend fun startLiveKitRecording(): Boolean {
        return liveKitRoom.startRecording()
    }

    /**
     * Stop server-side recording
     */
    suspend fun stopLiveKitRecording() {
        liveKitRoom.stopRecording()
    }

    /**
     * Start streaming to RTMP (YouTube, Twitch, etc.)
     */
    suspend fun startLiveKitStreaming(rtmpUrl: String): Boolean {
        return liveKitRoom.startStreaming(rtmpUrl)
    }

    /**
     * Stop streaming
     */
    suspend fun stopLiveKitStreaming() {
        liveKitRoom.stopStreaming()
    }

    fun release() {
        audioRecorder.release()
        audioProcessor.release()
        aiProcessor.release()
        exporter.release()

        // Release new components
        audioEncoder.release()
        speechRecognizer.release()
        rtmpStreamer.release()
        multiPlatformStreamer.release()
        webrtcRoom.release()
        videoRecorder.release()

        // Release LiveKit
        liveKitRoom.release()

        endRemoteSession()
        scope.cancel()
    }
}
