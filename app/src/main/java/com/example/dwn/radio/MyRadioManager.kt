package com.example.dwn.radio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

/**
 * MyRadioManager - Manages your personal radio station broadcasting
 */
class MyRadioManager(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ============================================
    // STATE
    // ============================================

    private val _state = MutableStateFlow(RadioStudioState())
    val state: StateFlow<RadioStudioState> = _state.asStateFlow()

    private val _isLive = MutableStateFlow(false)
    val isLive: StateFlow<Boolean> = _isLive.asStateFlow()

    private val _currentSession = MutableStateFlow<BroadcastSession?>(null)
    val currentSession: StateFlow<BroadcastSession?> = _currentSession.asStateFlow()

    private val _mixer = MutableStateFlow(BroadcastMixer())
    val mixer: StateFlow<BroadcastMixer> = _mixer.asStateFlow()

    private val _listenerCount = MutableStateFlow(0)
    val listenerCount: StateFlow<Int> = _listenerCount.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _broadcastDuration = MutableStateFlow(0L)
    val broadcastDuration: StateFlow<Long> = _broadcastDuration.asStateFlow()

    // Timer job
    private var durationJob: Job? = null
    private var listenerSimulatorJob: Job? = null

    // ============================================
    // STATION MANAGEMENT
    // ============================================

    /**
     * Create a new radio station
     */
    fun createStation(
        name: String,
        tagline: String = "",
        description: String = "",
        genres: List<RadioGenre> = emptyList(),
        accentColor: Long = 0xFFE91E63
    ): MyRadioStation {
        val station = MyRadioStation(
            name = name,
            tagline = tagline,
            description = description,
            genres = genres,
            accentColor = accentColor
        )

        _state.value = _state.value.copy(
            myStation = station,
            hasStation = true
        )

        return station
    }

    /**
     * Update station settings
     */
    fun updateStation(station: MyRadioStation) {
        _state.value = _state.value.copy(myStation = station)
    }

    /**
     * Get current station
     */
    fun getStation(): MyRadioStation? = _state.value.myStation

    // ============================================
    // BROADCAST CONTROL
    // ============================================

    /**
     * Start a live broadcast
     */
    fun startBroadcast(
        title: String,
        description: String = "",
        audioSource: AudioSource = AudioSource.MICROPHONE
    ): BroadcastSession {
        val station = _state.value.myStation ?: throw IllegalStateException("No station created")

        val session = BroadcastSession(
            stationId = station.id,
            title = title,
            description = description,
            status = BroadcastStatus.LIVE,
            startedAt = System.currentTimeMillis(),
            audioSource = audioSource
        )

        _currentSession.value = session
        _isLive.value = true
        _broadcastDuration.value = 0L

        // Start duration timer
        startDurationTimer()

        // Simulate listeners (in real app, this would be from server)
        startListenerSimulation()

        // Update state
        _state.value = _state.value.copy(
            currentSession = session,
            isLive = true,
            myStation = station.copy(isLive = true)
        )

        return session
    }

    /**
     * Pause broadcast
     */
    fun pauseBroadcast() {
        val session = _currentSession.value ?: return
        _currentSession.value = session.copy(status = BroadcastStatus.PAUSED)
        _state.value = _state.value.copy(
            currentSession = _currentSession.value
        )
    }

    /**
     * Resume broadcast
     */
    fun resumeBroadcast() {
        val session = _currentSession.value ?: return
        _currentSession.value = session.copy(status = BroadcastStatus.LIVE)
        _state.value = _state.value.copy(
            currentSession = _currentSession.value
        )
    }

    /**
     * End broadcast
     */
    fun endBroadcast(): BroadcastAnalytics {
        val session = _currentSession.value ?: throw IllegalStateException("No active broadcast")
        val station = _state.value.myStation

        // Stop timers
        durationJob?.cancel()
        listenerSimulatorJob?.cancel()

        // Create analytics
        val analytics = BroadcastAnalytics(
            sessionId = session.id,
            totalListeners = session.peakListeners + (10..50).random(),
            peakListeners = session.peakListeners,
            averageListeners = (session.peakListeners * 0.6f),
            totalDuration = _broadcastDuration.value,
            totalChatMessages = _chatMessages.value.size,
            totalLikes = session.totalLikes,
            newFollowers = (2..15).random()
        )

        // Create archived show
        val archivedShow = ArchivedShow(
            stationId = session.stationId,
            sessionId = session.id,
            title = session.title,
            description = session.description,
            recordingUri = "content://recordings/${session.id}",
            duration = _broadcastDuration.value,
            broadcastedAt = session.startedAt ?: System.currentTimeMillis(),
            listenerCount = analytics.peakListeners
        )

        val currentArchives = _state.value.archivedShows.toMutableList()
        currentArchives.add(0, archivedShow)

        // Update state
        _currentSession.value = session.copy(
            status = BroadcastStatus.ENDED,
            endedAt = System.currentTimeMillis()
        )
        _isLive.value = false

        _state.value = _state.value.copy(
            currentSession = null,
            isLive = false,
            myStation = station?.copy(
                isLive = false,
                totalBroadcastTime = (station.totalBroadcastTime) + _broadcastDuration.value,
                totalShows = station.totalShows + 1,
                lastLiveAt = System.currentTimeMillis()
            ),
            archivedShows = currentArchives,
            liveAnalytics = analytics
        )

        _broadcastDuration.value = 0L
        _chatMessages.value = emptyList()
        _listenerCount.value = 0

        return analytics
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        durationJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (_isLive.value) {
                    _broadcastDuration.value += 1000
                    _state.value = _state.value.copy(
                        broadcastDuration = _broadcastDuration.value
                    )
                }
            }
        }
    }

    private fun startListenerSimulation() {
        listenerSimulatorJob?.cancel()
        listenerSimulatorJob = scope.launch {
            // Simulate listeners joining over time
            var listeners = (5..20).random()
            _listenerCount.value = listeners

            while (isActive && _isLive.value) {
                delay((3000..8000).random().toLong())

                // Random listener changes
                val change = (-3..5).random()
                listeners = (listeners + change).coerceIn(1, 500)
                _listenerCount.value = listeners

                // Update peak
                val session = _currentSession.value
                if (session != null && listeners > session.peakListeners) {
                    _currentSession.value = session.copy(
                        peakListeners = listeners,
                        currentListeners = listeners
                    )
                } else if (session != null) {
                    _currentSession.value = session.copy(currentListeners = listeners)
                }

                // Simulate chat messages occasionally
                if ((0..10).random() > 7) {
                    simulateChatMessage()
                }
            }
        }
    }

    private fun simulateChatMessage() {
        val names = listOf("MusicLover", "RadioFan", "NightOwl", "ChillVibes", "BeatDropper", "AudioPhile", "SoundWave")
        val messages = listOf(
            "Great vibes! 🔥",
            "Love this track!",
            "Hello from NYC!",
            "This is fire 🎵",
            "Can you play some jazz?",
            "First time listener, this is amazing!",
            "Been following for months, best station ever!",
            "The sound quality is incredible",
            "What's the name of this song?",
            "👏👏👏"
        )

        val message = ChatMessage(
            senderId = UUID.randomUUID().toString(),
            senderName = names.random(),
            message = messages.random()
        )

        _chatMessages.value = (_chatMessages.value + message).takeLast(100)
    }

    // ============================================
    // MIXER CONTROLS
    // ============================================

    /**
     * Set microphone volume
     */
    fun setMicVolume(volume: Float) {
        _mixer.value = _mixer.value.copy(micVolume = volume.coerceIn(0f, 1f))
        _state.value = _state.value.copy(mixer = _mixer.value)
    }

    /**
     * Toggle microphone mute
     */
    fun toggleMicMute() {
        _mixer.value = _mixer.value.copy(micMuted = !_mixer.value.micMuted)
        _state.value = _state.value.copy(mixer = _mixer.value)
    }

    /**
     * Set media volume
     */
    fun setMediaVolume(volume: Float) {
        _mixer.value = _mixer.value.copy(mediaVolume = volume.coerceIn(0f, 1f))
        _state.value = _state.value.copy(mixer = _mixer.value)
    }

    /**
     * Toggle media mute
     */
    fun toggleMediaMute() {
        _mixer.value = _mixer.value.copy(mediaMuted = !_mixer.value.mediaMuted)
        _state.value = _state.value.copy(mixer = _mixer.value)
    }

    /**
     * Toggle noise gate
     */
    fun toggleNoiseGate() {
        _mixer.value = _mixer.value.copy(noiseGateEnabled = !_mixer.value.noiseGateEnabled)
        _state.value = _state.value.copy(mixer = _mixer.value)
    }

    /**
     * Toggle compressor
     */
    fun toggleCompressor() {
        _mixer.value = _mixer.value.copy(compressorEnabled = !_mixer.value.compressorEnabled)
        _state.value = _state.value.copy(mixer = _mixer.value)
    }

    /**
     * Toggle auto-ducking
     */
    fun toggleAutoDucking() {
        _mixer.value = _mixer.value.copy(autoDucking = !_mixer.value.autoDucking)
        _state.value = _state.value.copy(mixer = _mixer.value)
    }

    /**
     * Toggle voice enhancement
     */
    fun toggleVoiceEnhancement() {
        _mixer.value = _mixer.value.copy(voiceEnhancement = !_mixer.value.voiceEnhancement)
        _state.value = _state.value.copy(mixer = _mixer.value)
    }

    // ============================================
    // INTERACTIVE FEATURES
    // ============================================

    /**
     * Send chat message as host
     */
    fun sendHostMessage(message: String) {
        val station = _state.value.myStation ?: return

        val chatMessage = ChatMessage(
            senderId = "host",
            senderName = station.name,
            message = message,
            isHost = true
        )

        _chatMessages.value = (_chatMessages.value + chatMessage).takeLast(100)
    }

    /**
     * Create a poll
     */
    fun createPoll(question: String, options: List<String>, durationSeconds: Int = 60): BroadcastPoll {
        val poll = BroadcastPoll(
            question = question,
            options = options.map { PollOption(text = it) },
            durationSeconds = durationSeconds
        )

        _currentSession.value = _currentSession.value?.copy(activePoll = poll)
        _state.value = _state.value.copy(currentSession = _currentSession.value)

        // Auto-end poll after duration
        scope.launch {
            delay(durationSeconds * 1000L)
            endPoll()
        }

        return poll
    }

    /**
     * End current poll
     */
    fun endPoll() {
        _currentSession.value = _currentSession.value?.copy(
            activePoll = _currentSession.value?.activePoll?.copy(isActive = false)
        )
        _state.value = _state.value.copy(currentSession = _currentSession.value)
    }

    /**
     * Toggle Q&A
     */
    fun toggleQnA() {
        val session = _currentSession.value ?: return
        _currentSession.value = session.copy(qnaEnabled = !session.qnaEnabled)
        _state.value = _state.value.copy(currentSession = _currentSession.value)
    }

    /**
     * Mark question as answered
     */
    fun markQuestionAnswered(questionId: String) {
        val session = _currentSession.value ?: return
        val updatedQuestions = session.questions.map { q ->
            if (q.id == questionId) q.copy(isAnswered = true) else q
        }
        _currentSession.value = session.copy(questions = updatedQuestions)
        _state.value = _state.value.copy(currentSession = _currentSession.value)
    }

    // ============================================
    // PLAYLIST MANAGEMENT
    // ============================================

    /**
     * Add track to playlist
     */
    fun addToPlaylist(item: PlaylistItem) {
        val playlist = _state.value.playlist.toMutableList()
        playlist.add(item)
        _state.value = _state.value.copy(playlist = playlist)
    }

    /**
     * Remove track from playlist
     */
    fun removeFromPlaylist(itemId: String) {
        val playlist = _state.value.playlist.filter { it.id != itemId }
        _state.value = _state.value.copy(playlist = playlist)
    }

    /**
     * Reorder playlist
     */
    fun reorderPlaylist(fromIndex: Int, toIndex: Int) {
        val playlist = _state.value.playlist.toMutableList()
        val item = playlist.removeAt(fromIndex)
        playlist.add(toIndex, item)
        _state.value = _state.value.copy(playlist = playlist)
    }

    /**
     * Play track from playlist
     */
    fun playTrack(index: Int) {
        val playlist = _state.value.playlist.mapIndexed { i, item ->
            item.copy(isPlaying = i == index)
        }
        _state.value = _state.value.copy(
            playlist = playlist,
            currentTrackIndex = index
        )
    }

    // ============================================
    // SEGMENTS
    // ============================================

    /**
     * Add segment to broadcast
     */
    fun addSegment(segment: BroadcastSegment) {
        val session = _currentSession.value ?: return
        val segments = session.segments + segment.copy(startTime = System.currentTimeMillis())
        _currentSession.value = session.copy(segments = segments)
        _state.value = _state.value.copy(currentSession = _currentSession.value)
    }

    /**
     * End current segment
     */
    fun endCurrentSegment() {
        val session = _currentSession.value ?: return
        if (session.segments.isEmpty()) return

        val segments = session.segments.toMutableList()
        val lastSegment = segments.last()
        segments[segments.lastIndex] = lastSegment.copy(
            endTime = System.currentTimeMillis(),
            duration = System.currentTimeMillis() - (lastSegment.startTime ?: System.currentTimeMillis())
        )

        _currentSession.value = session.copy(segments = segments)
        _state.value = _state.value.copy(currentSession = _currentSession.value)
    }

    // ============================================
    // SOUND BOARD
    // ============================================

    /**
     * Play sound from soundboard
     */
    fun playSoundEffect(soundId: String) {
        // In real implementation, would play the sound
        // For now, just log
    }

    /**
     * Add custom sound to soundboard
     */
    fun addCustomSound(name: String, uri: String, icon: String = "🎵") {
        val soundBoard = _state.value.soundBoard
        val sounds = soundBoard.sounds + SoundBoardItem(
            name = name,
            icon = icon,
            soundUri = uri,
            isBuiltIn = false
        )
        _state.value = _state.value.copy(
            soundBoard = soundBoard.copy(sounds = sounds)
        )
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        if (_isLive.value) {
            endBroadcast()
        }
        durationJob?.cancel()
        listenerSimulatorJob?.cancel()
        scope.cancel()
    }
}

