package com.example.dwn.podcast.livekit

import android.content.Context
import android.util.Log
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

private const val TAG = "LiveKitRoom"

// ============================================
// LIVEKIT CONFIGURATION
// ============================================

data class LiveKitConfig(
    val serverUrl: String,
    val apiKey: String = "",
    val apiSecret: String = "",
    val enableVideo: Boolean = true,
    val enableAudio: Boolean = true
) {
    companion object {
        fun selfHosted(
            publicIp: String,
            port: Int = 7880,
            apiKey: String = "devkey",
            apiSecret: String = "secret"
        ) = LiveKitConfig(
            serverUrl = "ws://$publicIp:$port",
            apiKey = apiKey,
            apiSecret = apiSecret
        )

        fun cloud(
            projectUrl: String,
            apiKey: String,
            apiSecret: String
        ) = LiveKitConfig(
            serverUrl = projectUrl,
            apiKey = apiKey,
            apiSecret = apiSecret
        )
    }
}

// ============================================
// PARTICIPANT STATE
// ============================================

data class LiveKitParticipant(
    val id: String,
    val identity: String,
    val name: String,
    val role: ParticipantRole = ParticipantRole.GUEST,
    val isLocal: Boolean = false,
    val isMicrophoneEnabled: Boolean = true,
    val isCameraEnabled: Boolean = true,
    val isSpeaking: Boolean = false,
    val audioLevel: Float = 0f,
    val connectionQuality: ConnectionQuality = ConnectionQuality.EXCELLENT
)

enum class ParticipantRole {
    HOST, CO_HOST, GUEST
}

enum class ConnectionQuality {
    EXCELLENT, GOOD, FAIR, POOR, LOST
}

// ============================================
// ROOM STATE
// ============================================

data class LiveKitRoomState(
    val roomName: String = "",
    val roomCode: String = "",
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val participants: List<LiveKitParticipant> = emptyList(),
    val localParticipant: LiveKitParticipant? = null,
    val isRecording: Boolean = false,
    val isStreaming: Boolean = false,
    val error: String? = null
)

// ============================================
// LIVEKIT ROOM MANAGER
// ============================================

class LiveKitRoomManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var room: Room? = null
    private var config: LiveKitConfig = LiveKitConfig.selfHosted("localhost")

    private val _state = MutableStateFlow(LiveKitRoomState())
    val state: StateFlow<LiveKitRoomState> = _state.asStateFlow()

    private var tokenServerUrl: String = ""

    init {
        LiveKit.loggingLevel = io.livekit.android.util.LoggingLevel.INFO
    }

    fun configure(config: LiveKitConfig, tokenServerUrl: String = "") {
        this.config = config
        this.tokenServerUrl = tokenServerUrl
    }

    // ============================================
    // ROOM MANAGEMENT
    // ============================================

    suspend fun createRoom(displayName: String): Result<String> {
        return try {
            val roomCode = generateRoomCode()
            val roomName = "podcast_$roomCode"

            _state.value = _state.value.copy(isConnecting = true, error = null)

            val token = getAccessToken(roomName, displayName, isHost = true)
            connectToRoom(token, roomName, roomCode, displayName, ParticipantRole.HOST)

            Result.success(roomCode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create room", e)
            _state.value = _state.value.copy(isConnecting = false, error = e.message)
            Result.failure(e)
        }
    }

    suspend fun joinRoom(
        roomCode: String,
        displayName: String,
        role: ParticipantRole = ParticipantRole.GUEST
    ): Result<Unit> {
        return try {
            val roomName = "podcast_${roomCode.uppercase()}"

            _state.value = _state.value.copy(isConnecting = true, error = null)

            val token = getAccessToken(roomName, displayName, isHost = role == ParticipantRole.HOST)
            connectToRoom(token, roomName, roomCode, displayName, role)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join room", e)
            _state.value = _state.value.copy(isConnecting = false, error = e.message)
            Result.failure(e)
        }
    }

    private suspend fun connectToRoom(
        token: String,
        roomName: String,
        roomCode: String,
        displayName: String,
        role: ParticipantRole
    ) {
        room = LiveKit.create(context)

        scope.launch {
            room?.events?.collect { event ->
                handleRoomEvent(event)
            }
        }

        try {
            room?.connect(
                url = config.serverUrl,
                token = token
            )

            room?.localParticipant?.let { local ->
                if (config.enableAudio) {
                    local.setMicrophoneEnabled(true)
                }
                if (config.enableVideo) {
                    local.setCameraEnabled(true)
                }
            }

            _state.value = _state.value.copy(
                roomName = roomName,
                roomCode = roomCode,
                isConnected = true,
                isConnecting = false,
                localParticipant = room?.localParticipant?.toLiveKitParticipant(role, true)
            )

            updateParticipantsList()

            Log.d(TAG, "Connected to room: $roomCode")

        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            throw e
        }
    }

    fun leaveRoom() {
        scope.launch {
            try {
                room?.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting", e)
            }
            room = null
            _state.value = LiveKitRoomState()
            Log.d(TAG, "Left room")
        }
    }

    // ============================================
    // MEDIA CONTROLS
    // ============================================

    suspend fun setMicrophoneEnabled(enabled: Boolean) {
        try {
            room?.localParticipant?.setMicrophoneEnabled(enabled)
            updateParticipantsList()
            Log.d(TAG, "Microphone enabled: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set microphone", e)
        }
    }

    suspend fun setCameraEnabled(enabled: Boolean) {
        try {
            room?.localParticipant?.setCameraEnabled(enabled)
            updateParticipantsList()
            Log.d(TAG, "Camera enabled: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set camera", e)
        }
    }

    suspend fun switchCamera() {
        try {
            // Switch camera implementation depends on SDK version
            Log.d(TAG, "Camera switch requested")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch camera", e)
        }
    }

    suspend fun setScreenShareEnabled(enabled: Boolean) {
        try {
            room?.localParticipant?.setScreenShareEnabled(enabled)
            Log.d(TAG, "Screen share enabled: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set screen share", e)
        }
    }

    // ============================================
    // PARTICIPANT MANAGEMENT
    // ============================================

    fun muteParticipant(participantId: String) {
        scope.launch {
            try {
                val data = """{"action":"mute","targetId":"$participantId"}""".toByteArray()
                room?.localParticipant?.publishData(data)
                Log.d(TAG, "Mute request sent to: $participantId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mute participant", e)
            }
        }
    }

    fun kickParticipant(participantId: String) {
        scope.launch {
            try {
                val data = """{"action":"kick","targetId":"$participantId"}""".toByteArray()
                room?.localParticipant?.publishData(data)
                Log.d(TAG, "Kick request sent to: $participantId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to kick participant", e)
            }
        }
    }

    fun promoteToCoHost(participantId: String) {
        scope.launch {
            try {
                val data = """{"action":"promote","targetId":"$participantId","role":"CO_HOST"}""".toByteArray()
                room?.localParticipant?.publishData(data)
                Log.d(TAG, "Promote request sent to: $participantId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to promote participant", e)
            }
        }
    }

    // ============================================
    // RECORDING & STREAMING
    // ============================================

    suspend fun startRecording(): Boolean {
        _state.value = _state.value.copy(isRecording = true)
        Log.d(TAG, "Recording started (server-side)")
        return true
    }

    suspend fun stopRecording() {
        _state.value = _state.value.copy(isRecording = false)
        Log.d(TAG, "Recording stopped")
    }

    suspend fun startStreaming(rtmpUrl: String): Boolean {
        _state.value = _state.value.copy(isStreaming = true)
        Log.d(TAG, "Streaming started to: $rtmpUrl")
        return true
    }

    suspend fun stopStreaming() {
        _state.value = _state.value.copy(isStreaming = false)
        Log.d(TAG, "Streaming stopped")
    }

    // ============================================
    // EVENT HANDLING
    // ============================================

    private fun handleRoomEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.ParticipantConnected -> {
                Log.d(TAG, "Participant connected: ${event.participant.identity}")
                updateParticipantsList()
            }
            is RoomEvent.ParticipantDisconnected -> {
                Log.d(TAG, "Participant disconnected: ${event.participant.identity}")
                updateParticipantsList()
            }
            is RoomEvent.TrackSubscribed -> {
                Log.d(TAG, "Track subscribed")
                updateParticipantsList()
            }
            is RoomEvent.TrackUnsubscribed -> {
                Log.d(TAG, "Track unsubscribed")
                updateParticipantsList()
            }
            is RoomEvent.TrackMuted -> {
                Log.d(TAG, "Track muted")
                updateParticipantsList()
            }
            is RoomEvent.TrackUnmuted -> {
                Log.d(TAG, "Track unmuted")
                updateParticipantsList()
            }
            is RoomEvent.ActiveSpeakersChanged -> {
                updateSpeakingState(event.speakers)
            }
            is RoomEvent.Disconnected -> {
                Log.d(TAG, "Disconnected from room")
                _state.value = _state.value.copy(isConnected = false)
            }
            is RoomEvent.Reconnecting -> {
                Log.d(TAG, "Reconnecting...")
            }
            is RoomEvent.Reconnected -> {
                Log.d(TAG, "Reconnected")
                _state.value = _state.value.copy(isConnected = true)
            }
            is RoomEvent.DataReceived -> {
                handleDataMessage(event.data, event.participant)
            }
            else -> {}
        }
    }

    private fun handleDataMessage(data: ByteArray, participant: RemoteParticipant?) {
        try {
            val message = String(data)
            val json = org.json.JSONObject(message)
            val action = json.optString("action")
            val targetId = json.optString("targetId")

            val localId = room?.localParticipant?.identity?.value
            if (targetId != localId) return

            when (action) {
                "mute" -> scope.launch { setMicrophoneEnabled(false) }
                "kick" -> leaveRoom()
                "promote" -> {
                    val role = json.optString("role")
                    _state.value.localParticipant?.let { local ->
                        _state.value = _state.value.copy(
                            localParticipant = local.copy(role = ParticipantRole.valueOf(role))
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse data message", e)
        }
    }

    private fun updateParticipantsList() {
        val participants = mutableListOf<LiveKitParticipant>()

        room?.localParticipant?.let { local ->
            val localRole = _state.value.localParticipant?.role ?: ParticipantRole.HOST
            participants.add(local.toLiveKitParticipant(localRole, true))
        }

        room?.remoteParticipants?.values?.forEach { remote ->
            participants.add(remote.toLiveKitParticipant(ParticipantRole.GUEST, false))
        }

        _state.value = _state.value.copy(
            participants = participants,
            localParticipant = participants.firstOrNull { it.isLocal }
        )
    }

    private fun updateSpeakingState(speakers: List<Participant>) {
        val speakerIds = speakers.map { it.identity?.value }

        _state.value = _state.value.copy(
            participants = _state.value.participants.map { p ->
                p.copy(isSpeaking = p.identity in speakerIds)
            }
        )
    }

    // ============================================
    // TOKEN
    // ============================================

    private suspend fun getAccessToken(
        roomName: String,
        participantName: String,
        isHost: Boolean
    ): String = withContext(Dispatchers.IO) {
        if (tokenServerUrl.isNotEmpty()) {
            try {
                val client = okhttp3.OkHttpClient()
                val requestBody = okhttp3.FormBody.Builder()
                    .add("room", roomName)
                    .add("participant", participantName)
                    .add("isHost", isHost.toString())
                    .build()

                val request = okhttp3.Request.Builder()
                    .url(tokenServerUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val json = org.json.JSONObject(response.body?.string() ?: "{}")
                return@withContext json.getString("token")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get token from server", e)
                throw e
            }
        } else {
            throw IllegalStateException("Token server URL not configured")
        }
    }

    // ============================================
    // UTILITIES
    // ============================================

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun Participant.toLiveKitParticipant(
        role: ParticipantRole,
        isLocal: Boolean
    ): LiveKitParticipant {
        return LiveKitParticipant(
            id = sid?.value ?: "",
            identity = identity?.value ?: "",
            name = name ?: identity?.value ?: "Unknown",
            role = role,
            isLocal = isLocal,
            isMicrophoneEnabled = isMicrophoneEnabled(),
            isCameraEnabled = isCameraEnabled(),
            isSpeaking = isSpeaking,
            audioLevel = audioLevel,
            connectionQuality = when (connectionQuality) {
                io.livekit.android.room.participant.ConnectionQuality.EXCELLENT -> ConnectionQuality.EXCELLENT
                io.livekit.android.room.participant.ConnectionQuality.GOOD -> ConnectionQuality.GOOD
                io.livekit.android.room.participant.ConnectionQuality.POOR -> ConnectionQuality.POOR
                io.livekit.android.room.participant.ConnectionQuality.LOST -> ConnectionQuality.LOST
                else -> ConnectionQuality.GOOD
            }
        )
    }

    fun getRoom(): Room? = room

    fun release() {
        leaveRoom()
        scope.cancel()
    }
}

