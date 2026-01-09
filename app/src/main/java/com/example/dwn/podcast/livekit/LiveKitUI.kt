package com.example.dwn.podcast.livekit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.livekit.android.room.Room
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.launch

// ============================================
// COLORS
// ============================================

private val LiveKitPrimary = Color(0xFFFF6B00)
private val LiveKitBackground = Color(0xFF0D0D0D)
private val LiveKitSurface = Color(0xFF1A1A1A)
private val LiveKitSurfaceVariant = Color(0xFF2A2A2A)

// ============================================
// MAIN LIVEKIT ROOM UI
// ============================================

@Composable
fun LiveKitRoomScreen(
    roomManager: LiveKitRoomManager,
    onLeaveRoom: () -> Unit
) {
    val state by roomManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LiveKitBackground)
    ) {
        // Top Bar
        LiveKitTopBar(
            roomCode = state.roomCode,
            participantCount = state.participants.size,
            isRecording = state.isRecording,
            isStreaming = state.isStreaming,
            onLeave = {
                roomManager.leaveRoom()
                onLeaveRoom()
            }
        )

        // Participants Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.participants.isEmpty()) {
                // Waiting for participants
                WaitingForParticipants(roomCode = state.roomCode)
            } else {
                // Show participants
                ParticipantsGrid(
                    participants = state.participants,
                    room = roomManager.getRoom()
                )
            }
        }

        // Control Bar
        LiveKitControlBar(
            localParticipant = state.localParticipant,
            onToggleMic = {
                scope.launch {
                    val current = state.localParticipant?.isMicrophoneEnabled ?: false
                    roomManager.setMicrophoneEnabled(!current)
                }
            },
            onToggleCamera = {
                scope.launch {
                    val current = state.localParticipant?.isCameraEnabled ?: false
                    roomManager.setCameraEnabled(!current)
                }
            },
            onSwitchCamera = {
                scope.launch { roomManager.switchCamera() }
            },
            onToggleScreenShare = {
                scope.launch {
                    // Toggle screen share
                }
            },
            onLeave = {
                roomManager.leaveRoom()
                onLeaveRoom()
            }
        )
    }
}

// ============================================
// TOP BAR
// ============================================

@Composable
private fun LiveKitTopBar(
    roomCode: String,
    participantCount: Int,
    isRecording: Boolean,
    isStreaming: Boolean,
    onLeave: () -> Unit
) {
    Surface(
        color = LiveKitSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Room Code
            Column {
                Text(
                    "Room Code",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                Text(
                    roomCode,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Participant count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(LiveKitSurfaceVariant, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.People,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "$participantCount",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Recording indicator
            if (isRecording) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Red, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REC", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Live indicator
            if (isStreaming) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(LiveKitPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("LIVE", color = LiveKitPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Leave button
            IconButton(onClick = onLeave) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = "Leave",
                    tint = Color.Red
                )
            }
        }
    }
}

// ============================================
// PARTICIPANTS GRID
// ============================================

@Composable
private fun ParticipantsGrid(
    participants: List<LiveKitParticipant>,
    room: Room?
) {
    val columns = when {
        participants.size <= 1 -> 1
        participants.size <= 4 -> 2
        else -> 3
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(participants) { participant ->
            ParticipantTile(
                participant = participant,
                room = room,
                modifier = Modifier
                    .aspectRatio(if (participants.size <= 2) 16f / 9f else 4f / 3f)
            )
        }
    }
}

// ============================================
// PARTICIPANT TILE
// ============================================

@Composable
private fun ParticipantTile(
    participant: LiveKitParticipant,
    room: Room?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LiveKitSurface)
            .then(
                if (participant.isSpeaking) {
                    Modifier.border(2.dp, LiveKitPrimary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
    ) {
        // Video or Avatar
        if (participant.isCameraEnabled) {
            // Show camera is enabled placeholder
            // In production, use LiveKit's VideoRenderer component
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LiveKitSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            // Avatar
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(LiveKitPrimary, LiveKitPrimary.copy(alpha = 0.7f))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        participant.name.take(2).uppercase(),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Overlay - Name and indicators
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Name
                Text(
                    participant.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                // Role badge
                if (participant.role == ParticipantRole.HOST) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "HOST",
                        color = LiveKitPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(LiveKitPrimary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                } else if (participant.role == ParticipantRole.CO_HOST) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "CO-HOST",
                        color = Color.Cyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.Cyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Connection quality
                ConnectionQualityIndicator(participant.connectionQuality)

                Spacer(modifier = Modifier.width(8.dp))

                // Mic indicator
                Icon(
                    if (participant.isMicrophoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = null,
                    tint = if (participant.isMicrophoneEnabled) Color.White else Color.Red,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Speaking indicator
        if (participant.isSpeaking) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(12.dp)
                    .background(LiveKitPrimary, CircleShape)
            )
        }
    }
}

// ============================================
// CONNECTION QUALITY INDICATOR
// ============================================

@Composable
private fun ConnectionQualityIndicator(quality: ConnectionQuality) {
    val bars = when (quality) {
        ConnectionQuality.EXCELLENT -> 4
        ConnectionQuality.GOOD -> 3
        ConnectionQuality.FAIR -> 2
        ConnectionQuality.POOR -> 1
        ConnectionQuality.LOST -> 0
    }

    val color = when (quality) {
        ConnectionQuality.EXCELLENT, ConnectionQuality.GOOD -> Color.Green
        ConnectionQuality.FAIR -> Color.Yellow
        ConnectionQuality.POOR, ConnectionQuality.LOST -> Color.Red
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(((index + 1) * 3).dp)
                    .background(
                        if (index < bars) color else Color.Gray.copy(alpha = 0.3f),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

// ============================================
// WAITING SCREEN
// ============================================

@Composable
private fun WaitingForParticipants(roomCode: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Groups,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Waiting for participants...",
            color = Color.Gray,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Share this code to invite guests:",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            roomCode,
            color = LiveKitPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
    }
}

// ============================================
// CONTROL BAR
// ============================================

@Composable
private fun LiveKitControlBar(
    localParticipant: LiveKitParticipant?,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onLeave: () -> Unit
) {
    Surface(
        color = LiveKitSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Microphone
            ControlButton(
                icon = if (localParticipant?.isMicrophoneEnabled == true)
                    Icons.Default.Mic else Icons.Default.MicOff,
                label = "Mic",
                isEnabled = localParticipant?.isMicrophoneEnabled == true,
                onClick = onToggleMic
            )

            // Camera
            ControlButton(
                icon = if (localParticipant?.isCameraEnabled == true)
                    Icons.Default.Videocam else Icons.Default.VideocamOff,
                label = "Camera",
                isEnabled = localParticipant?.isCameraEnabled == true,
                onClick = onToggleCamera
            )

            // Switch Camera
            ControlButton(
                icon = Icons.Default.FlipCameraAndroid,
                label = "Flip",
                isEnabled = true,
                onClick = onSwitchCamera
            )

            // Screen Share
            ControlButton(
                icon = Icons.Default.ScreenShare,
                label = "Share",
                isEnabled = false,
                onClick = onToggleScreenShare
            )

            // Leave
            ControlButton(
                icon = Icons.Default.CallEnd,
                label = "Leave",
                isEnabled = true,
                isDestructive = true,
                onClick = onLeave
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isEnabled: Boolean,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .background(
                    when {
                        isDestructive -> Color.Red.copy(alpha = 0.2f)
                        isEnabled -> LiveKitSurfaceVariant
                        else -> Color.Red.copy(alpha = 0.2f)
                    },
                    CircleShape
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = when {
                    isDestructive -> Color.Red
                    isEnabled -> Color.White
                    else -> Color.Red
                },
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            label,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}

