package com.example.dwn.podcast.webrtc

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "PodcastWebRTC"

// ============================================
// WEBRTC CONFIGURATION
// ============================================

data class WebRTCConfig(
    val stunServers: List<String> = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302",
        "stun:stun2.l.google.com:19302"
    ),
    val turnServers: List<TurnServer> = emptyList(),
    val enableVideo: Boolean = true,
    val enableAudio: Boolean = true,
    val videoWidth: Int = 640,
    val videoHeight: Int = 480,
    val videoFps: Int = 30,
    val audioBitrate: Int = 64000,
    val videoBitrate: Int = 1000000
)

data class TurnServer(
    val url: String,
    val username: String,
    val credential: String
)

// ============================================
// PARTICIPANT MODEL
// ============================================

data class Participant(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: ParticipantRole = ParticipantRole.GUEST,
    val isLocal: Boolean = false,
    val isAudioEnabled: Boolean = true,
    val isVideoEnabled: Boolean = true,
    val isSpeaking: Boolean = false,
    val audioLevel: Float = 0f,
    val connectionState: ConnectionState = ConnectionState.CONNECTING,
    val networkQuality: NetworkQuality = NetworkQuality.GOOD,
    val joinedAt: Long = System.currentTimeMillis()
)

enum class ParticipantRole {
    HOST, CO_HOST, GUEST, VIEWER
}

enum class ConnectionState {
    CONNECTING, CONNECTED, RECONNECTING, DISCONNECTED, FAILED
}

enum class NetworkQuality {
    EXCELLENT, GOOD, FAIR, POOR
}

// ============================================
// ROOM STATE
// ============================================

data class RoomState(
    val roomId: String = "",
    val roomCode: String = "",
    val isActive: Boolean = false,
    val participants: List<Participant> = emptyList(),
    val isRecording: Boolean = false,
    val recordingStartTime: Long? = null,
    val maxParticipants: Int = 10
)

// ============================================
// SIGNALING MESSAGES
// ============================================

sealed class SignalingMessage {
    data class Offer(val sdp: String, val fromId: String) : SignalingMessage()
    data class Answer(val sdp: String, val fromId: String) : SignalingMessage()
    data class IceCandidate(
        val candidate: String,
        val sdpMid: String,
        val sdpMLineIndex: Int,
        val fromId: String
    ) : SignalingMessage()
    data class ParticipantJoined(val participant: Participant) : SignalingMessage()
    data class ParticipantLeft(val participantId: String) : SignalingMessage()
    data class MuteChanged(val participantId: String, val isAudioMuted: Boolean, val isVideoMuted: Boolean) : SignalingMessage()
}

// ============================================
// WEBRTC ROOM MANAGER
// ============================================

class PodcastWebRTCRoom(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _roomState = MutableStateFlow(RoomState())
    val roomState: StateFlow<RoomState> = _roomState.asStateFlow()

    private val _localParticipant = MutableStateFlow<Participant?>(null)
    val localParticipant: StateFlow<Participant?> = _localParticipant.asStateFlow()

    private val _signalingMessages = MutableStateFlow<SignalingMessage?>(null)
    val signalingMessages: StateFlow<SignalingMessage?> = _signalingMessages.asStateFlow()

    // Peer connections for each participant
    private val peerConnections = ConcurrentHashMap<String, PeerConnectionWrapper>()

    private var config: WebRTCConfig = WebRTCConfig()
    private var signalingClient: SignalingClient? = null

    // ============================================
    // ROOM MANAGEMENT
    // ============================================

    fun createRoom(hostName: String, config: WebRTCConfig = WebRTCConfig()): String {
        this.config = config

        val roomCode = generateRoomCode()
        val roomId = UUID.randomUUID().toString()

        val host = Participant(
            name = hostName,
            role = ParticipantRole.HOST,
            isLocal = true,
            connectionState = ConnectionState.CONNECTED
        )

        _localParticipant.value = host
        _roomState.value = RoomState(
            roomId = roomId,
            roomCode = roomCode,
            isActive = true,
            participants = listOf(host)
        )

        // Initialize signaling
        initializeSignaling(roomId)

        Log.d(TAG, "Room created: $roomCode")
        return roomCode
    }

    fun joinRoom(roomCode: String, participantName: String, role: ParticipantRole = ParticipantRole.GUEST): Boolean {
        // Validate room code
        if (roomCode.length != 6) {
            Log.e(TAG, "Invalid room code")
            return false
        }

        val participant = Participant(
            name = participantName,
            role = role,
            isLocal = true,
            connectionState = ConnectionState.CONNECTING
        )

        _localParticipant.value = participant
        _roomState.value = _roomState.value.copy(
            roomCode = roomCode,
            participants = _roomState.value.participants + participant
        )

        // Connect to signaling server
        scope.launch {
            connectToRoom(roomCode, participant)
        }

        Log.d(TAG, "Joining room: $roomCode as $participantName")
        return true
    }

    fun leaveRoom() {
        val local = _localParticipant.value ?: return

        // Notify others
        _signalingMessages.value = SignalingMessage.ParticipantLeft(local.id)

        // Close all peer connections
        peerConnections.values.forEach { it.close() }
        peerConnections.clear()

        // Reset state
        _localParticipant.value = null
        _roomState.value = RoomState()

        // Disconnect signaling
        signalingClient?.disconnect()
        signalingClient = null

        Log.d(TAG, "Left room")
    }

    // ============================================
    // PARTICIPANT MANAGEMENT
    // ============================================

    fun addCoHost(participantId: String) {
        updateParticipantRole(participantId, ParticipantRole.CO_HOST)
    }

    fun removeCoHost(participantId: String) {
        updateParticipantRole(participantId, ParticipantRole.GUEST)
    }

    fun kickParticipant(participantId: String) {
        // Send kick message
        _signalingMessages.value = SignalingMessage.ParticipantLeft(participantId)

        // Remove from local state
        _roomState.value = _roomState.value.copy(
            participants = _roomState.value.participants.filter { it.id != participantId }
        )

        // Close peer connection
        peerConnections[participantId]?.close()
        peerConnections.remove(participantId)

        Log.d(TAG, "Kicked participant: $participantId")
    }

    private fun updateParticipantRole(participantId: String, role: ParticipantRole) {
        _roomState.value = _roomState.value.copy(
            participants = _roomState.value.participants.map {
                if (it.id == participantId) it.copy(role = role) else it
            }
        )
    }

    // ============================================
    // MEDIA CONTROLS
    // ============================================

    fun setLocalAudioEnabled(enabled: Boolean) {
        _localParticipant.value = _localParticipant.value?.copy(isAudioEnabled = enabled)

        // Notify peers
        _localParticipant.value?.let { local ->
            _signalingMessages.value = SignalingMessage.MuteChanged(
                local.id,
                !enabled,
                !local.isVideoEnabled
            )
        }

        // Update local audio track
        peerConnections.values.forEach { pc ->
            pc.setAudioEnabled(enabled)
        }

        Log.d(TAG, "Local audio enabled: $enabled")
    }

    fun setLocalVideoEnabled(enabled: Boolean) {
        _localParticipant.value = _localParticipant.value?.copy(isVideoEnabled = enabled)

        // Notify peers
        _localParticipant.value?.let { local ->
            _signalingMessages.value = SignalingMessage.MuteChanged(
                local.id,
                !local.isAudioEnabled,
                !enabled
            )
        }

        // Update local video track
        peerConnections.values.forEach { pc ->
            pc.setVideoEnabled(enabled)
        }

        Log.d(TAG, "Local video enabled: $enabled")
    }

    fun muteParticipant(participantId: String, muteAudio: Boolean, muteVideo: Boolean) {
        // Only host/co-host can mute others
        val local = _localParticipant.value ?: return
        if (local.role != ParticipantRole.HOST && local.role != ParticipantRole.CO_HOST) {
            return
        }

        _signalingMessages.value = SignalingMessage.MuteChanged(participantId, muteAudio, muteVideo)
    }

    fun switchCamera() {
        // Toggle between front and back camera
        Log.d(TAG, "Switching camera")
    }

    // ============================================
    // RECORDING
    // ============================================

    fun startRecording(): Boolean {
        if (_roomState.value.isRecording) return false

        _roomState.value = _roomState.value.copy(
            isRecording = true,
            recordingStartTime = System.currentTimeMillis()
        )

        Log.d(TAG, "Recording started")
        return true
    }

    fun stopRecording(): Long {
        val startTime = _roomState.value.recordingStartTime ?: return 0
        val duration = System.currentTimeMillis() - startTime

        _roomState.value = _roomState.value.copy(
            isRecording = false,
            recordingStartTime = null
        )

        Log.d(TAG, "Recording stopped, duration: ${duration}ms")
        return duration
    }

    // ============================================
    // SIGNALING
    // ============================================

    private fun initializeSignaling(roomId: String) {
        signalingClient = SignalingClient(
            roomId = roomId,
            onMessage = { message -> handleSignalingMessage(message) },
            onConnected = { Log.d(TAG, "Signaling connected") },
            onDisconnected = { Log.d(TAG, "Signaling disconnected") }
        )
        signalingClient?.connect()
    }

    private suspend fun connectToRoom(roomCode: String, participant: Participant) {
        // Connect to signaling server and exchange offers/answers
        delay(500) // Simulated connection delay

        // Update connection state
        _localParticipant.value = participant.copy(connectionState = ConnectionState.CONNECTED)
        _roomState.value = _roomState.value.copy(
            isActive = true,
            participants = _roomState.value.participants.map {
                if (it.id == participant.id) it.copy(connectionState = ConnectionState.CONNECTED)
                else it
            }
        )

        // Notify others
        _signalingMessages.value = SignalingMessage.ParticipantJoined(participant)
    }

    private fun handleSignalingMessage(message: SignalingMessage) {
        scope.launch {
            when (message) {
                is SignalingMessage.Offer -> handleOffer(message)
                is SignalingMessage.Answer -> handleAnswer(message)
                is SignalingMessage.IceCandidate -> handleIceCandidate(message)
                is SignalingMessage.ParticipantJoined -> handleParticipantJoined(message.participant)
                is SignalingMessage.ParticipantLeft -> handleParticipantLeft(message.participantId)
                is SignalingMessage.MuteChanged -> handleMuteChanged(message)
            }
        }
    }

    private suspend fun handleOffer(offer: SignalingMessage.Offer) {
        // Create peer connection if needed
        val pc = getOrCreatePeerConnection(offer.fromId)

        // Set remote description and create answer
        // In real implementation, use WebRTC API
        Log.d(TAG, "Received offer from: ${offer.fromId}")
    }

    private suspend fun handleAnswer(answer: SignalingMessage.Answer) {
        val pc = peerConnections[answer.fromId] ?: return
        // Set remote description
        Log.d(TAG, "Received answer from: ${answer.fromId}")
    }

    private fun handleIceCandidate(ice: SignalingMessage.IceCandidate) {
        val pc = peerConnections[ice.fromId] ?: return
        // Add ICE candidate
        Log.d(TAG, "Received ICE candidate from: ${ice.fromId}")
    }

    private fun handleParticipantJoined(participant: Participant) {
        _roomState.value = _roomState.value.copy(
            participants = _roomState.value.participants + participant
        )

        // Create peer connection for new participant
        scope.launch {
            createPeerConnectionForParticipant(participant.id)
        }

        Log.d(TAG, "Participant joined: ${participant.name}")
    }

    private fun handleParticipantLeft(participantId: String) {
        _roomState.value = _roomState.value.copy(
            participants = _roomState.value.participants.filter { it.id != participantId }
        )

        // Close peer connection
        peerConnections[participantId]?.close()
        peerConnections.remove(participantId)

        Log.d(TAG, "Participant left: $participantId")
    }

    private fun handleMuteChanged(message: SignalingMessage.MuteChanged) {
        _roomState.value = _roomState.value.copy(
            participants = _roomState.value.participants.map {
                if (it.id == message.participantId) {
                    it.copy(
                        isAudioEnabled = !message.isAudioMuted,
                        isVideoEnabled = !message.isVideoMuted
                    )
                } else it
            }
        )
    }

    // ============================================
    // PEER CONNECTIONS
    // ============================================

    private fun getOrCreatePeerConnection(participantId: String): PeerConnectionWrapper {
        return peerConnections.getOrPut(participantId) {
            PeerConnectionWrapper(participantId, config)
        }
    }

    private suspend fun createPeerConnectionForParticipant(participantId: String) {
        val pc = getOrCreatePeerConnection(participantId)

        // Create and send offer
        // In real implementation, use WebRTC API
        Log.d(TAG, "Created peer connection for: $participantId")
    }

    // ============================================
    // UTILITIES
    // ============================================

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    fun getParticipantCount(): Int = _roomState.value.participants.size

    fun isHost(): Boolean = _localParticipant.value?.role == ParticipantRole.HOST

    // ============================================
    // CLEANUP
    // ============================================

    fun release() {
        leaveRoom()
        scope.cancel()
    }
}

// ============================================
// PEER CONNECTION WRAPPER
// ============================================

class PeerConnectionWrapper(
    private val participantId: String,
    private val config: WebRTCConfig
) {
    private var isAudioEnabled = true
    private var isVideoEnabled = true

    fun setAudioEnabled(enabled: Boolean) {
        isAudioEnabled = enabled
    }

    fun setVideoEnabled(enabled: Boolean) {
        isVideoEnabled = enabled
    }

    fun close() {
        // Close peer connection
        Log.d(TAG, "Closed peer connection for: $participantId")
    }
}

// ============================================
// SIGNALING CLIENT (WebSocket-based)
// ============================================

class SignalingClient(
    private val roomId: String,
    private val onMessage: (SignalingMessage) -> Unit,
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isConnected = false

    fun connect() {
        scope.launch {
            // In real implementation, connect to WebSocket server
            delay(100)
            isConnected = true
            onConnected()
        }
    }

    fun send(message: SignalingMessage) {
        if (!isConnected) return

        scope.launch {
            // Serialize and send via WebSocket
            val json = when (message) {
                is SignalingMessage.Offer -> JSONObject().apply {
                    put("type", "offer")
                    put("sdp", message.sdp)
                    put("fromId", message.fromId)
                }
                is SignalingMessage.Answer -> JSONObject().apply {
                    put("type", "answer")
                    put("sdp", message.sdp)
                    put("fromId", message.fromId)
                }
                is SignalingMessage.IceCandidate -> JSONObject().apply {
                    put("type", "ice")
                    put("candidate", message.candidate)
                    put("sdpMid", message.sdpMid)
                    put("sdpMLineIndex", message.sdpMLineIndex)
                    put("fromId", message.fromId)
                }
                else -> JSONObject()
            }

            Log.d(TAG, "Sending: $json")
        }
    }

    fun disconnect() {
        isConnected = false
        scope.cancel()
        onDisconnected()
    }
}

