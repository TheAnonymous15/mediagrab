package com.example.dwn.audio.social

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// ============================================
// 🎵 AUDIO ROOM DATA MODELS
// ============================================

enum class RoomType {
    OPEN,       // Drop-in listening, raise-hand to speak
    STAGE,      // Host + speakers, audience silent
    PRIVATE     // Invite-only, optional E2E encryption
}

enum class ParticipantRole {
    HOST,
    CO_HOST,
    SPEAKER,
    LISTENER
}

enum class PresenceState {
    LISTENING,
    SPEAKING,
    MUTED,
    RECORDING,
    AWAY
}

enum class RoomStatus {
    WAITING,    // Room created but not started
    LIVE,       // Room is active
    ENDED,      // Room has ended
    PAUSED      // Room is temporarily paused
}

data class AudioRoomParticipant(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val role: ParticipantRole = ParticipantRole.LISTENER,
    val presenceState: PresenceState = PresenceState.LISTENING,
    val isMuted: Boolean = true,
    val isHandRaised: Boolean = false,
    val speakingLevel: Float = 0f,  // 0-1 audio level
    val joinedAt: Long = System.currentTimeMillis(),
    val totalSpeakingTime: Long = 0L
)

data class AudioRoom(
    val id: String = UUID.randomUUID().toString(),
    val code: String = generateRoomCode(),
    val title: String,
    val description: String = "",
    val type: RoomType = RoomType.OPEN,
    val status: RoomStatus = RoomStatus.WAITING,
    val hostId: String,
    val participants: List<AudioRoomParticipant> = emptyList(),
    val maxParticipants: Int = 100,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val isRecording: Boolean = false,
    val isEncrypted: Boolean = false,
    val settings: RoomSettings = RoomSettings()
)

data class RoomSettings(
    val allowRaiseHand: Boolean = true,
    val autoMuteOnJoin: Boolean = true,
    val speakingTimeLimit: Int? = null,  // seconds, null = unlimited
    val enableNoiseSuppression: Boolean = true,
    val enableEchoCancellation: Boolean = true,
    val enableAutoLeveling: Boolean = true,
    val audioQuality: AudioQuality = AudioQuality.WIDEBAND
)

enum class AudioQuality {
    NARROWBAND,     // 8kHz - voice calls
    WIDEBAND,       // 16kHz - good quality voice
    FULLBAND        // 48kHz - studio quality
}

// ============================================
// 🎧 CO-LISTENING SESSION
// ============================================

data class CoListeningSession(
    val id: String = UUID.randomUUID().toString(),
    val roomId: String,
    val mediaUri: String,
    val mediaTitle: String,
    val mediaArtist: String? = null,
    val mediaDuration: Long = 0L,  // milliseconds
    val currentPosition: Long = 0L,
    val isPlaying: Boolean = false,
    val hostControlled: Boolean = true,
    val syncTolerance: Long = 500L  // ms tolerance for sync
)

// ============================================
// 🎤 AUDIO CLIP
// ============================================

data class AudioClip(
    val id: String = UUID.randomUUID().toString(),
    val creatorId: String,
    val creatorName: String,
    val sourceRoomId: String? = null,
    val sourceMediaUri: String? = null,
    val clipUri: String,
    val title: String,
    val description: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val duration: Long = 0L,
    val waveformData: List<Float> = emptyList(),
    val transcript: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isPublic: Boolean = true
)

// ============================================
// 💬 TIMESTAMPED COMMENT
// ============================================

data class TimestampedComment(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val userName: String,
    val content: String,
    val timestamp: Long,  // Position in audio/media
    val isVoiceNote: Boolean = false,
    val voiceNoteUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// ============================================
// 🎛️ AUDIO DSP SETTINGS
// ============================================

data class AudioDSPSettings(
    // Per-speaker EQ
    val eqEnabled: Boolean = false,
    val eqBands: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),

    // Dynamics
    val compressorEnabled: Boolean = false,
    val compressorThreshold: Float = -24f,
    val compressorRatio: Float = 4f,
    val compressorAttack: Float = 10f,
    val compressorRelease: Float = 100f,

    val limiterEnabled: Boolean = true,
    val limiterThreshold: Float = -1f,

    val noiseGateEnabled: Boolean = false,
    val noiseGateThreshold: Float = -40f,

    val deEsserEnabled: Boolean = false,
    val deEsserFrequency: Float = 6000f,
    val deEsserThreshold: Float = -20f,

    // FX
    val reverbEnabled: Boolean = false,
    val reverbMix: Float = 0.2f,
    val reverbDecay: Float = 1.5f,

    val warmthEnabled: Boolean = false,
    val warmthAmount: Float = 0.3f,

    // Spatial
    val spatialEnabled: Boolean = false,
    val spatialWidth: Float = 0.5f,
    val spatialDepth: Float = 0.3f
)

// ============================================
// 📊 ROOM ANALYTICS
// ============================================

data class RoomAnalytics(
    val roomId: String,
    val peakListeners: Int = 0,
    val totalUniqueListeners: Int = 0,
    val averageListenDuration: Long = 0L,
    val totalSpeakingTime: Long = 0L,
    val engagementScore: Float = 0f,
    val retentionRate: Float = 0f
)

// ============================================
// 🔔 NOTIFICATIONS
// ============================================

enum class AudioNotificationType {
    ROOM_STARTING_SOON,
    CREATOR_LIVE,
    CLIP_MENTION,
    HAND_RAISED,
    INVITED_TO_SPEAK,
    NEW_VOICE_REPLY
}

data class AudioNotification(
    val id: String = UUID.randomUUID().toString(),
    val type: AudioNotificationType,
    val title: String,
    val message: String,
    val roomId: String? = null,
    val clipId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

// ============================================
// 🔧 UTILITY
// ============================================

private fun generateRoomCode(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..6).map { chars.random() }.joinToString("")
}

