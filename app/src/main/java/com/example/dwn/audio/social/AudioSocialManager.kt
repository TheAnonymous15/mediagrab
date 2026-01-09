package com.example.dwn.audio.social

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * AudioSocialManager - Core manager for audio-first social features
 * Handles rooms, co-listening, clips, and real-time audio
 */
class AudioSocialManager(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ============================================
    // STATE
    // ============================================

    private val _currentRoom = MutableStateFlow<AudioRoom?>(null)
    val currentRoom: StateFlow<AudioRoom?> = _currentRoom.asStateFlow()

    private val _coListeningSession = MutableStateFlow<CoListeningSession?>(null)
    val coListeningSession: StateFlow<CoListeningSession?> = _coListeningSession.asStateFlow()

    private val _localParticipant = MutableStateFlow<AudioRoomParticipant?>(null)
    val localParticipant: StateFlow<AudioRoomParticipant?> = _localParticipant.asStateFlow()

    private val _participants = MutableStateFlow<List<AudioRoomParticipant>>(emptyList())
    val participants: StateFlow<List<AudioRoomParticipant>> = _participants.asStateFlow()

    private val _isMicEnabled = MutableStateFlow(false)
    val isMicEnabled: StateFlow<Boolean> = _isMicEnabled.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _speakingParticipants = MutableStateFlow<Set<String>>(emptySet())
    val speakingParticipants: StateFlow<Set<String>> = _speakingParticipants.asStateFlow()

    private val _dspSettings = MutableStateFlow(AudioDSPSettings())
    val dspSettings: StateFlow<AudioDSPSettings> = _dspSettings.asStateFlow()

    private val _audioClips = MutableStateFlow<List<AudioClip>>(emptyList())
    val audioClips: StateFlow<List<AudioClip>> = _audioClips.asStateFlow()

    private val _notifications = MutableStateFlow<List<AudioNotification>>(emptyList())
    val notifications: StateFlow<List<AudioNotification>> = _notifications.asStateFlow()

    // Room storage (in-memory for now, would be server-synced)
    private val rooms = ConcurrentHashMap<String, AudioRoom>()

    // Audio processing
    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var autoGainControl: AutomaticGainControl? = null
    private var recordingJob: Job? = null

    // ============================================
    // ROOM MANAGEMENT
    // ============================================

    /**
     * Create a new audio room
     */
    fun createRoom(
        title: String,
        description: String = "",
        type: RoomType = RoomType.OPEN,
        userId: String,
        userName: String,
        settings: RoomSettings = RoomSettings()
    ): AudioRoom {
        val room = AudioRoom(
            title = title,
            description = description,
            type = type,
            hostId = userId,
            settings = settings,
            participants = listOf(
                AudioRoomParticipant(
                    userId = userId,
                    displayName = userName,
                    role = ParticipantRole.HOST,
                    presenceState = PresenceState.LISTENING,
                    isMuted = false
                )
            )
        )

        rooms[room.id] = room
        _currentRoom.value = room
        _localParticipant.value = room.participants.first()
        _participants.value = room.participants
        _isConnected.value = true

        return room
    }

    /**
     * Join an existing room by code
     */
    fun joinRoom(
        roomCode: String,
        userId: String,
        userName: String
    ): Result<AudioRoom> {
        val room = rooms.values.find { it.code == roomCode }
            ?: return Result.failure(Exception("Room not found"))

        if (room.participants.size >= room.maxParticipants) {
            return Result.failure(Exception("Room is full"))
        }

        val participant = AudioRoomParticipant(
            userId = userId,
            displayName = userName,
            role = ParticipantRole.LISTENER,
            presenceState = PresenceState.LISTENING,
            isMuted = room.settings.autoMuteOnJoin
        )

        val updatedRoom = room.copy(
            participants = room.participants + participant
        )

        rooms[room.id] = updatedRoom
        _currentRoom.value = updatedRoom
        _localParticipant.value = participant
        _participants.value = updatedRoom.participants
        _isConnected.value = true

        // Sync with co-listening session if active
        if (_coListeningSession.value != null) {
            // New joiner auto-syncs
        }

        return Result.success(updatedRoom)
    }

    /**
     * Leave current room
     */
    fun leaveRoom() {
        val room = _currentRoom.value ?: return
        val local = _localParticipant.value ?: return

        val updatedRoom = room.copy(
            participants = room.participants.filter { it.userId != local.userId }
        )

        if (updatedRoom.participants.isEmpty()) {
            rooms.remove(room.id)
        } else {
            rooms[room.id] = updatedRoom
        }

        stopMicrophone()
        _currentRoom.value = null
        _localParticipant.value = null
        _participants.value = emptyList()
        _coListeningSession.value = null
        _isConnected.value = false
    }

    /**
     * Start the room (host only)
     */
    fun startRoom(): Boolean {
        val room = _currentRoom.value ?: return false
        val local = _localParticipant.value ?: return false

        if (local.role != ParticipantRole.HOST) return false

        val updatedRoom = room.copy(
            status = RoomStatus.LIVE,
            startedAt = System.currentTimeMillis()
        )

        rooms[room.id] = updatedRoom
        _currentRoom.value = updatedRoom

        return true
    }

    /**
     * End the room (host only)
     */
    fun endRoom(): Boolean {
        val room = _currentRoom.value ?: return false
        val local = _localParticipant.value ?: return false

        if (local.role != ParticipantRole.HOST) return false

        val updatedRoom = room.copy(
            status = RoomStatus.ENDED,
            endedAt = System.currentTimeMillis()
        )

        rooms[room.id] = updatedRoom
        leaveRoom()

        return true
    }

    // ============================================
    // PARTICIPANT MANAGEMENT
    // ============================================

    /**
     * Raise hand to request to speak
     */
    fun raiseHand() {
        val local = _localParticipant.value ?: return
        val room = _currentRoom.value ?: return

        if (!room.settings.allowRaiseHand) return

        val updated = local.copy(isHandRaised = true)
        updateLocalParticipant(updated)
    }

    /**
     * Lower hand
     */
    fun lowerHand() {
        val local = _localParticipant.value ?: return
        val updated = local.copy(isHandRaised = false)
        updateLocalParticipant(updated)
    }

    /**
     * Promote participant to speaker (host/co-host only)
     */
    fun promoteToSpeaker(participantId: String): Boolean {
        val room = _currentRoom.value ?: return false
        val local = _localParticipant.value ?: return false

        if (local.role != ParticipantRole.HOST && local.role != ParticipantRole.CO_HOST) {
            return false
        }

        val participant = room.participants.find { it.id == participantId } ?: return false
        val updatedParticipant = participant.copy(
            role = ParticipantRole.SPEAKER,
            isHandRaised = false,
            isMuted = false
        )

        updateParticipant(updatedParticipant)

        // Send notification
        addNotification(
            AudioNotification(
                type = AudioNotificationType.INVITED_TO_SPEAK,
                title = "You're now a speaker!",
                message = "You've been invited to speak in ${room.title}",
                roomId = room.id
            )
        )

        return true
    }

    /**
     * Mute a participant (host/co-host only)
     */
    fun muteParticipant(participantId: String): Boolean {
        val room = _currentRoom.value ?: return false
        val local = _localParticipant.value ?: return false

        if (local.role != ParticipantRole.HOST && local.role != ParticipantRole.CO_HOST) {
            return false
        }

        val participant = room.participants.find { it.id == participantId } ?: return false
        val updatedParticipant = participant.copy(isMuted = true)
        updateParticipant(updatedParticipant)

        return true
    }

    /**
     * Remove participant from room (host only)
     */
    fun removeParticipant(participantId: String): Boolean {
        val room = _currentRoom.value ?: return false
        val local = _localParticipant.value ?: return false

        if (local.role != ParticipantRole.HOST) return false

        val updatedRoom = room.copy(
            participants = room.participants.filter { it.id != participantId }
        )

        rooms[room.id] = updatedRoom
        _currentRoom.value = updatedRoom
        _participants.value = updatedRoom.participants

        return true
    }

    private fun updateLocalParticipant(participant: AudioRoomParticipant) {
        _localParticipant.value = participant
        updateParticipant(participant)
    }

    private fun updateParticipant(participant: AudioRoomParticipant) {
        val room = _currentRoom.value ?: return

        val updatedParticipants = room.participants.map {
            if (it.id == participant.id) participant else it
        }

        val updatedRoom = room.copy(participants = updatedParticipants)
        rooms[room.id] = updatedRoom
        _currentRoom.value = updatedRoom
        _participants.value = updatedParticipants
    }

    // ============================================
    // MICROPHONE CONTROL
    // ============================================

    /**
     * Toggle microphone
     */
    fun toggleMicrophone() {
        if (_isMicEnabled.value) {
            stopMicrophone()
        } else {
            startMicrophone()
        }
    }

    /**
     * Start microphone capture
     */
    @Suppress("MissingPermission")
    fun startMicrophone() {
        val room = _currentRoom.value ?: return
        val local = _localParticipant.value ?: return

        // Check if user can speak
        if (local.role == ParticipantRole.LISTENER && room.type == RoomType.STAGE) {
            return
        }

        try {
            val sampleRate = when (room.settings.audioQuality) {
                AudioQuality.NARROWBAND -> 8000
                AudioQuality.WIDEBAND -> 16000
                AudioQuality.FULLBAND -> 48000
            }

            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )

            // Apply audio processing
            if (room.settings.enableNoiseSuppression && NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(audioRecord!!.audioSessionId)
                noiseSuppressor?.enabled = true
            }

            if (room.settings.enableEchoCancellation && AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(audioRecord!!.audioSessionId)
                echoCanceler?.enabled = true
            }

            if (room.settings.enableAutoLeveling && AutomaticGainControl.isAvailable()) {
                autoGainControl = AutomaticGainControl.create(audioRecord!!.audioSessionId)
                autoGainControl?.enabled = true
            }

            audioRecord?.startRecording()
            _isMicEnabled.value = true

            // Update presence
            updateLocalParticipant(local.copy(
                presenceState = PresenceState.SPEAKING,
                isMuted = false
            ))

            // Start audio level monitoring
            startAudioLevelMonitoring(bufferSize)

        } catch (e: Exception) {
            e.printStackTrace()
            stopMicrophone()
        }
    }

    /**
     * Stop microphone capture
     */
    fun stopMicrophone() {
        recordingJob?.cancel()
        recordingJob = null

        noiseSuppressor?.release()
        noiseSuppressor = null

        echoCanceler?.release()
        echoCanceler = null

        autoGainControl?.release()
        autoGainControl = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        _isMicEnabled.value = false

        _localParticipant.value?.let { local ->
            updateLocalParticipant(local.copy(
                presenceState = PresenceState.MUTED,
                isMuted = true
            ))
        }
    }

    private fun startAudioLevelMonitoring(bufferSize: Int) {
        recordingJob = scope.launch {
            val buffer = ShortArray(bufferSize)

            while (isActive && audioRecord != null) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0

                if (read > 0) {
                    // Calculate audio level
                    var sum = 0L
                    for (i in 0 until read) {
                        sum += buffer[i] * buffer[i]
                    }
                    val rms = Math.sqrt(sum.toDouble() / read).toFloat()
                    val level = (rms / 32768f).coerceIn(0f, 1f)

                    // Update speaking level
                    _localParticipant.value?.let { local ->
                        if (level > 0.01f) {  // Speaking threshold
                            _speakingParticipants.value = _speakingParticipants.value + local.id
                        } else {
                            _speakingParticipants.value = _speakingParticipants.value - local.id
                        }
                    }
                }

                delay(50)  // 20Hz update rate
            }
        }
    }

    // ============================================
    // CO-LISTENING
    // ============================================

    /**
     * Start a co-listening session
     */
    fun startCoListening(
        mediaUri: String,
        mediaTitle: String,
        mediaArtist: String? = null,
        mediaDuration: Long
    ): CoListeningSession? {
        val room = _currentRoom.value ?: return null
        val local = _localParticipant.value ?: return null

        // Only host can start co-listening
        if (local.role != ParticipantRole.HOST && local.role != ParticipantRole.CO_HOST) {
            return null
        }

        val session = CoListeningSession(
            roomId = room.id,
            mediaUri = mediaUri,
            mediaTitle = mediaTitle,
            mediaArtist = mediaArtist,
            mediaDuration = mediaDuration,
            isPlaying = false,
            hostControlled = true
        )

        _coListeningSession.value = session
        return session
    }

    /**
     * Control co-listening playback
     */
    fun playCoListening() {
        _coListeningSession.value?.let { session ->
            _coListeningSession.value = session.copy(isPlaying = true)
        }
    }

    fun pauseCoListening() {
        _coListeningSession.value?.let { session ->
            _coListeningSession.value = session.copy(isPlaying = false)
        }
    }

    fun seekCoListening(position: Long) {
        _coListeningSession.value?.let { session ->
            _coListeningSession.value = session.copy(currentPosition = position)
        }
    }

    fun syncCoListeningPosition(position: Long) {
        _coListeningSession.value?.let { session ->
            _coListeningSession.value = session.copy(currentPosition = position)
        }
    }

    fun stopCoListening() {
        _coListeningSession.value = null
    }

    // ============================================
    // AUDIO CLIPS
    // ============================================

    /**
     * Create an audio clip
     */
    fun createClip(
        sourceUri: String,
        title: String,
        description: String,
        startTime: Long,
        endTime: Long,
        userId: String,
        userName: String
    ): AudioClip {
        val clip = AudioClip(
            creatorId = userId,
            creatorName = userName,
            sourceMediaUri = sourceUri,
            clipUri = sourceUri,  // Would be processed clip URI
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            duration = endTime - startTime
        )

        _audioClips.value = _audioClips.value + clip
        return clip
    }

    // ============================================
    // DSP SETTINGS
    // ============================================

    fun updateDSPSettings(settings: AudioDSPSettings) {
        _dspSettings.value = settings
    }

    // ============================================
    // NOTIFICATIONS
    // ============================================

    private fun addNotification(notification: AudioNotification) {
        _notifications.value = listOf(notification) + _notifications.value
    }

    fun markNotificationRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    // ============================================
    // DISCOVERY
    // ============================================

    /**
     * Get all available rooms
     */
    fun getAvailableRooms(): List<AudioRoom> {
        return rooms.values.filter {
            it.type == RoomType.OPEN && it.status == RoomStatus.LIVE
        }
    }

    /**
     * Search rooms by topic
     */
    fun searchRooms(query: String): List<AudioRoom> {
        return rooms.values.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        leaveRoom()
        scope.cancel()
    }
}

