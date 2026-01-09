package com.example.dwn.podcast.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.podcast.*
import com.example.dwn.podcast.livekit.LiveKitRoomState
import com.example.dwn.podcast.livekit.LiveKitParticipant
import com.example.dwn.podcast.livekit.ConnectionQuality as LKConnectionQuality

// Theme colors
private val PodcastPrimary = Color(0xFF6C63FF)
private val PodcastSecondary = Color(0xFF00D9FF)
private val PodcastAccent = Color(0xFFFF6B6B)
private val PodcastGreen = Color(0xFF00E676)
private val PodcastOrange = Color(0xFFFF9800)
private val PodcastPink = Color(0xFFFF4081)
private val PodcastYellow = Color(0xFFFFEB3B)
private val PodcastSurface = Color(0xFF12121A)
private val PodcastSurfaceLight = Color(0xFF1A1A24)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB0B0C0)
private val TextTertiary = Color(0xFF6E6E80)

// ============================================
// LIVE STREAMING TAB
// ============================================

@Composable
fun LiveStreamingTab(
    liveState: LiveStreamState,
    liveKitState: LiveKitRoomState? = null,
    onStartStream: (StreamPlatform, String) -> Unit,
    onStopStream: () -> Unit,
    onCreateRoom: ((String) -> Unit)? = null,
    onJoinRoom: ((String, String) -> Unit)? = null,
    onLeaveRoom: (() -> Unit)? = null,
    onToggleMic: ((Boolean) -> Unit)? = null,
    onToggleCamera: ((Boolean) -> Unit)? = null
) {
    var selectedPlatform by remember { mutableStateOf(StreamPlatform.YOUTUBE) }
    var streamKey by remember { mutableStateOf("") }
    var showSetupSheet by remember { mutableStateOf(false) }
    var showRemoteGuestsSection by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (liveState.isLive) {
                // Live Dashboard
                LiveDashboard(
                    liveState = liveState,
                    onStopStream = onStopStream
                )
            } else {
                // Stream Setup
                StreamSetupCard(
                    selectedPlatform = selectedPlatform,
                    onPlatformSelect = { selectedPlatform = it },
                    streamKey = streamKey,
                    onStreamKeyChange = { streamKey = it },
                    onGoLive = { onStartStream(selectedPlatform, streamKey) }
                )
            }
        }

        if (liveState.isLive) {
            item {
                LiveChatSection()
            }

            item {
                StreamControlsSection()
            }

            item {
                LiveAnalyticsSection(
                    viewerCount = liveState.viewerCount,
                    duration = liveState.duration,
                    health = liveState.health
                )
            }
        } else {
            // Remote Guests Section (LiveKit)
            item {
                RemoteGuestsSection(
                    liveKitState = liveKitState,
                    onCreateRoom = onCreateRoom,
                    onJoinRoom = onJoinRoom,
                    onLeaveRoom = onLeaveRoom,
                    onToggleMic = onToggleMic,
                    onToggleCamera = onToggleCamera
                )
            }

            item {
                StreamDestinationsCard()
            }

            item {
                StreamQualitySettings()
            }

            item {
                ScheduledStreamsCard()
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun LiveDashboard(
    liveState: LiveStreamState,
    onStopStream: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PodcastSurface
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                        .background(PodcastAccent, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "LIVE",
                    color = PodcastAccent,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Streaming to ${liveState.platform?.label ?: "Unknown"}",
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LiveStat(
                    icon = Icons.Default.Timer,
                    value = formatDuration(liveState.duration),
                    label = "Duration"
                )
                LiveStat(
                    icon = Icons.Default.Visibility,
                    value = "${liveState.viewerCount}",
                    label = "Viewers"
                )
                LiveStat(
                    icon = Icons.Default.Speed,
                    value = "${liveState.bitrate / 1000}k",
                    label = "Bitrate"
                )
                LiveStat(
                    icon = Icons.Default.SignalCellularAlt,
                    value = liveState.health.name,
                    label = "Health",
                    valueColor = when (liveState.health) {
                        StreamHealth.EXCELLENT -> PodcastGreen
                        StreamHealth.GOOD -> PodcastSecondary
                        StreamHealth.FAIR -> PodcastOrange
                        StreamHealth.POOR -> PodcastAccent
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // End stream button
            Button(
                onClick = onStopStream,
                colors = ButtonDefaults.buttonColors(containerColor = PodcastAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.StopCircle, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("END STREAM")
            }
        }
    }
}

@Composable
private fun LiveStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    valueColor: Color = TextPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = TextTertiary, fontSize = 10.sp)
    }
}

@Composable
private fun StreamSetupCard(
    selectedPlatform: StreamPlatform,
    onPlatformSelect: (StreamPlatform) -> Unit,
    streamKey: String,
    onStreamKeyChange: (String) -> Unit,
    onGoLive: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Videocam,
                    null,
                    tint = PodcastPink,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Go Live",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Stream to your audience in real-time",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Platform selection
            Text("Select Platform", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StreamPlatform.entries) { platform ->
                    PlatformChip(
                        platform = platform,
                        isSelected = selectedPlatform == platform,
                        onClick = { onPlatformSelect(platform) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stream key input
            OutlinedTextField(
                value = streamKey,
                onValueChange = onStreamKeyChange,
                label = { Text("Stream Key") },
                placeholder = { Text("Enter your stream key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PodcastPink,
                    unfocusedBorderColor = TextTertiary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                trailingIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Visibility, null, tint = TextTertiary)
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Go Live button
            Button(
                onClick = onGoLive,
                enabled = streamKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PodcastPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PlayCircle, null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("GO LIVE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun PlatformChip(
    platform: StreamPlatform,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = when (platform) {
        StreamPlatform.YOUTUBE -> Color(0xFFFF0000)
        StreamPlatform.TWITCH -> Color(0xFF9146FF)
        StreamPlatform.FACEBOOK -> Color(0xFF1877F2)
        else -> PodcastSecondary
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else PodcastSurfaceLight,
        border = if (isSelected) BorderStroke(2.dp, color) else null,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (platform) {
                    StreamPlatform.YOUTUBE -> Icons.Default.PlayCircle
                    StreamPlatform.TWITCH -> Icons.Default.Gamepad
                    StreamPlatform.FACEBOOK -> Icons.Default.Facebook
                    else -> Icons.Default.Stream
                },
                null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                platform.label,
                color = if (isSelected) color else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun LiveChatSection() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Chat", color = TextPrimary, fontWeight = FontWeight.Medium)
                Row {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.PushPin, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Settings, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Chat messages (placeholder)
            LazyColumn(
                modifier = Modifier.weight(1f),
                reverseLayout = true
            ) {
                items(5) { index ->
                    ChatMessage(
                        username = "User${5 - index}",
                        message = "This is a sample chat message #${5 - index}",
                        isHighlighted = index == 0
                    )
                }
            }

            // Chat input
            OutlinedTextField(
                value = "",
                onValueChange = { },
                placeholder = { Text("Send a message...", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PodcastPrimary,
                    unfocusedBorderColor = TextTertiary
                ),
                trailingIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Send, null, tint = PodcastPrimary)
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatMessage(
    username: String,
    message: String,
    isHighlighted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (isHighlighted) Modifier.background(
                    PodcastPrimary.copy(alpha = 0.1f),
                    RoundedCornerShape(4.dp)
                ) else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            username,
            color = PodcastSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            message,
            color = TextPrimary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun StreamControlsSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Stream Controls", color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StreamControlButton(
                    icon = Icons.Default.MicOff,
                    label = "Mute Mic",
                    isActive = false
                )
                StreamControlButton(
                    icon = Icons.Default.VideocamOff,
                    label = "Pause Video",
                    isActive = false
                )
                StreamControlButton(
                    icon = Icons.Default.ScreenShare,
                    label = "Share Screen",
                    isActive = false
                )
                StreamControlButton(
                    icon = Icons.Default.Layers,
                    label = "Overlays",
                    isActive = true
                )
            }
        }
    }
}

@Composable
private fun StreamControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isActive) PodcastPrimary.copy(alpha = 0.2f) else PodcastSurfaceLight,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = if (isActive) PodcastPrimary else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = TextTertiary, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LiveAnalyticsSection(
    viewerCount: Int,
    duration: Long,
    health: StreamHealth
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Live Analytics", color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AnalyticItem(
                    value = "$viewerCount",
                    label = "Current Viewers",
                    change = "+12%",
                    isPositive = true
                )
                AnalyticItem(
                    value = "${viewerCount + 45}",
                    label = "Peak Viewers",
                    change = "",
                    isPositive = true
                )
                AnalyticItem(
                    value = "4.2",
                    label = "Avg. View Time",
                    change = "mins",
                    isPositive = true
                )
            }
        }
    }
}

@Composable
private fun AnalyticItem(
    value: String,
    label: String,
    change: String,
    isPositive: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(label, color = TextTertiary, fontSize = 11.sp)
        if (change.isNotEmpty()) {
            Text(
                change,
                color = if (isPositive) PodcastGreen else PodcastAccent,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun StreamDestinationsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stream Destinations", color = TextPrimary, fontWeight = FontWeight.Medium)
                TextButton(onClick = { }) {
                    Text("Add", color = PodcastPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Multistream to multiple platforms simultaneously",
                color = TextTertiary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Connected platforms
            DestinationItem(platform = "YouTube", isConnected = true, viewers = "1.2K subs")
            DestinationItem(platform = "Twitch", isConnected = false, viewers = "Connect")
        }
    }
}

@Composable
private fun DestinationItem(
    platform: String,
    isConnected: Boolean,
    viewers: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (isConnected) PodcastGreen else TextTertiary, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(platform, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(viewers, color = if (isConnected) PodcastGreen else PodcastPrimary, fontSize = 12.sp)
    }
}

@Composable
private fun StreamQualitySettings() {
    var selectedQuality by remember { mutableStateOf("1080p") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Stream Quality", color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("720p", "1080p", "4K").forEach { quality ->
                    FilterChip(
                        selected = selectedQuality == quality,
                        onClick = { selectedQuality = quality },
                        label = { Text(quality) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PodcastPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Estimated bitrate: 6000 kbps", color = TextTertiary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ScheduledStreamsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Scheduled Streams", color = TextPrimary, fontWeight = FontWeight.Medium)
                TextButton(onClick = { }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Text("Schedule")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "No scheduled streams. Schedule your next live session.",
                color = TextTertiary,
                fontSize = 12.sp
            )
        }
    }
}

// ============================================
// REMOTE GUESTS SECTION (LIVEKIT)
// ============================================

@Composable
private fun RemoteGuestsSection(
    liveKitState: LiveKitRoomState?,
    onCreateRoom: ((String) -> Unit)?,
    onJoinRoom: ((String, String) -> Unit)?,
    onLeaveRoom: (() -> Unit)?,
    onToggleMic: ((Boolean) -> Unit)?,
    onToggleCamera: ((Boolean) -> Unit)?
) {
    var hostName by remember { mutableStateOf("") }
    var roomCode by remember { mutableStateOf("") }
    var guestName by remember { mutableStateOf("") }
    var showJoinDialog by remember { mutableStateOf(false) }

    val isConnected = liveKitState?.isConnected == true
    val isConnecting = liveKitState?.isConnecting == true

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.linearGradient(listOf(PodcastPrimary, PodcastSecondary)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Remote Guests",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        if (isConnected) "Connected • ${liveKitState?.participants?.size ?: 0} participants"
                        else "Invite remote guests via LiveKit",
                        color = if (isConnected) PodcastGreen else TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isConnected) {
                // Room info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = PodcastSurfaceLight
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Room Code", color = TextSecondary, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    liveKitState?.roomCode ?: "",
                                    color = PodcastSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    letterSpacing = 2.sp
                                )
                                IconButton(onClick = { /* Copy to clipboard */ }) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = TextTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Participants list
                        Text("Participants", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        liveKitState?.participants?.forEach { participant ->
                            ParticipantRow(participant = participant)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val localParticipant = liveKitState?.localParticipant

                    // Mic toggle
                    MediaControlButton(
                        icon = if (localParticipant?.isMicrophoneEnabled == true)
                            Icons.Default.Mic else Icons.Default.MicOff,
                        label = "Mic",
                        isEnabled = localParticipant?.isMicrophoneEnabled == true,
                        onClick = { onToggleMic?.invoke(!(localParticipant?.isMicrophoneEnabled ?: false)) }
                    )

                    // Camera toggle
                    MediaControlButton(
                        icon = if (localParticipant?.isCameraEnabled == true)
                            Icons.Default.Videocam else Icons.Default.VideocamOff,
                        label = "Camera",
                        isEnabled = localParticipant?.isCameraEnabled == true,
                        onClick = { onToggleCamera?.invoke(!(localParticipant?.isCameraEnabled ?: false)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Leave button
                Button(
                    onClick = { onLeaveRoom?.invoke() },
                    colors = ButtonDefaults.buttonColors(containerColor = PodcastAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CallEnd, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Leave Room")
                }

            } else {
                // Create or Join room
                OutlinedTextField(
                    value = hostName,
                    onValueChange = { hostName = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PodcastPrimary,
                        unfocusedBorderColor = TextTertiary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Create Room
                    Button(
                        onClick = {
                            if (hostName.isNotBlank()) {
                                onCreateRoom?.invoke(hostName)
                            }
                        },
                        enabled = hostName.isNotBlank() && !isConnecting,
                        colors = ButtonDefaults.buttonColors(containerColor = PodcastPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Room")
                        }
                    }

                    // Join Room
                    OutlinedButton(
                        onClick = { showJoinDialog = true },
                        enabled = !isConnecting,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Login, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Join Room")
                    }
                }
            }
        }
    }

    // Join Room Dialog
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            containerColor = PodcastSurface,
            title = { Text("Join Room", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = guestName,
                        onValueChange = { guestName = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PodcastSecondary,
                            unfocusedBorderColor = TextTertiary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = roomCode,
                        onValueChange = { roomCode = it.uppercase().take(6) },
                        label = { Text("Room Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PodcastSecondary,
                            unfocusedBorderColor = TextTertiary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (guestName.isNotBlank() && roomCode.length == 6) {
                            onJoinRoom?.invoke(roomCode, guestName)
                            showJoinDialog = false
                        }
                    },
                    enabled = guestName.isNotBlank() && roomCode.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = PodcastSecondary)
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ParticipantRow(participant: LiveKitParticipant) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PodcastSurface, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (participant.isLocal) PodcastPrimary else PodcastSecondary,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                participant.name.take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Name and role
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    participant.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (participant.isLocal) {
                    Text(
                        " (You)",
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                participant.role.name,
                color = when (participant.role) {
                    com.example.dwn.podcast.livekit.ParticipantRole.HOST -> PodcastPrimary
                    com.example.dwn.podcast.livekit.ParticipantRole.CO_HOST -> PodcastSecondary
                    else -> TextTertiary
                },
                fontSize = 10.sp
            )
        }

        // Speaking indicator
        if (participant.isSpeaking) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(PodcastGreen, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Mic status
        Icon(
            if (participant.isMicrophoneEnabled) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = null,
            tint = if (participant.isMicrophoneEnabled) TextSecondary else PodcastAccent,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Camera status
        Icon(
            if (participant.isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
            contentDescription = null,
            tint = if (participant.isCameraEnabled) TextSecondary else PodcastAccent,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun MediaControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isEnabled: Boolean,
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
                    if (isEnabled) PodcastSurfaceLight else PodcastAccent.copy(alpha = 0.2f),
                    CircleShape
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isEnabled) Color.White else PodcastAccent,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            color = TextTertiary,
            fontSize = 11.sp
        )
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / 60000) % 60
    val hours = millis / 3600000
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

