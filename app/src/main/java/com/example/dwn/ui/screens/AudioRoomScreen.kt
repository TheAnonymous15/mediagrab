package com.example.dwn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.audio.social.*
import kotlin.math.sin

// ============================================
// 🎵 AUDIO ROOM COLORS
// ============================================

private object RoomColors {
    val pink = Color(0xFFE91E63)
    val purple = Color(0xFF9C27B0)
    val blue = Color(0xFF2196F3)
    val cyan = Color(0xFF00BCD4)
    val green = Color(0xFF4CAF50)
    val orange = Color(0xFFFF5722)
    val red = Color(0xFFE53935)

    val bgDark = Color(0xFF0A0A0F)
    val bgMid = Color(0xFF12121A)
    val surface = Color(0xFF1A1A24)
    val surfaceVariant = Color(0xFF222230)
    val card = Color(0xFF1E1E2A)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B8)
    val textTertiary = Color(0xFF707080)

    val glassWhite = Color(0x12FFFFFF)
    val glassBorder = Color(0x20FFFFFF)

    val liveRed = Color(0xFFFF3B30)
    val speakingGreen = Color(0xFF34C759)
    val mutedGray = Color(0xFF8E8E93)
}

// ============================================
// 🎧 AUDIO ROOM SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioRoomScreen(
    roomId: String,
    onBack: () -> Unit
) {
    // Mock room data
    val room = remember {
        AudioRoom(
            id = roomId,
            code = "ABC123",
            title = "Late Night Beats 🎵",
            description = "Chill music vibes with friends",
            type = RoomType.OPEN,
            status = RoomStatus.LIVE,
            hostId = "user1",
            participants = listOf(
                AudioRoomParticipant(
                    id = "1",
                    userId = "user1",
                    displayName = "DJ Mike",
                    role = ParticipantRole.HOST,
                    presenceState = PresenceState.SPEAKING,
                    isMuted = false
                ),
                AudioRoomParticipant(
                    id = "2",
                    userId = "user2",
                    displayName = "Sarah",
                    role = ParticipantRole.SPEAKER,
                    presenceState = PresenceState.LISTENING,
                    isMuted = true
                ),
                AudioRoomParticipant(
                    id = "3",
                    userId = "user3",
                    displayName = "Alex",
                    role = ParticipantRole.LISTENER,
                    presenceState = PresenceState.LISTENING,
                    isMuted = true,
                    isHandRaised = true
                ),
                AudioRoomParticipant(
                    id = "4",
                    userId = "user4",
                    displayName = "Jordan",
                    role = ParticipantRole.LISTENER,
                    presenceState = PresenceState.LISTENING,
                    isMuted = true
                ),
                AudioRoomParticipant(
                    id = "5",
                    userId = "user5",
                    displayName = "Emma",
                    role = ParticipantRole.LISTENER,
                    presenceState = PresenceState.LISTENING,
                    isMuted = true
                )
            )
        )
    }

    var isMuted by remember { mutableStateOf(true) }
    var isHandRaised by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showCoListening by remember { mutableStateOf(false) }
    var showDSPPanel by remember { mutableStateOf(false) }

    // Mock co-listening session
    var coListeningActive by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0.3f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RoomColors.bgDark)
    ) {
        // Animated background
        RoomBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            RoomTopBar(
                room = room,
                onBack = onBack,
                onSettings = { showSettings = true }
            )

            // Main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Room Info
                RoomInfoCard(room = room)

                Spacer(modifier = Modifier.height(20.dp))

                // Co-Listening Section (if active)
                if (coListeningActive) {
                    CoListeningCard(
                        isPlaying = isPlaying,
                        position = currentPosition,
                        onPlayPause = { isPlaying = !isPlaying },
                        onSeek = { currentPosition = it }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Speakers Section
                SpeakersSection(
                    speakers = room.participants.filter {
                        it.role == ParticipantRole.HOST || it.role == ParticipantRole.CO_HOST || it.role == ParticipantRole.SPEAKER
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Listeners Section
                ListenersSection(
                    listeners = room.participants.filter { it.role == ParticipantRole.LISTENER }
                )

                Spacer(modifier = Modifier.height(100.dp))
            }

            // Bottom Control Bar
            RoomControlBar(
                isMuted = isMuted,
                isHandRaised = isHandRaised,
                onMuteToggle = { isMuted = !isMuted },
                onHandToggle = { isHandRaised = !isHandRaised },
                onCoListen = { coListeningActive = !coListeningActive },
                onDSP = { showDSPPanel = true },
                onLeave = onBack
            )
        }
    }

    // DSP Panel
    if (showDSPPanel) {
        DSPBottomSheet(onDismiss = { showDSPPanel = false })
    }
}

// ============================================
// 🌌 ROOM BACKGROUND
// ============================================

@Composable
private fun RoomBackground() {
    val transition = rememberInfiniteTransition(label = "room_bg")

    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    RoomColors.bgDark,
                    Color(0xFF0F0818),
                    RoomColors.bgMid,
                    RoomColors.bgDark
                )
            )
        )

        // Ambient orbs
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    RoomColors.pink.copy(alpha = 0.1f * pulse),
                    Color.Transparent
                )
            ),
            radius = 300f,
            center = Offset(size.width * 0.2f, size.height * 0.3f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    RoomColors.purple.copy(alpha = 0.08f * pulse),
                    Color.Transparent
                )
            ),
            radius = 350f,
            center = Offset(size.width * 0.8f, size.height * 0.6f)
        )
    }
}

// ============================================
// 📱 TOP BAR
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomTopBar(
    room: AudioRoom,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Live indicator
                Box(
                    modifier = Modifier
                        .background(RoomColors.liveRed.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulsingDot(color = RoomColors.liveRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "LIVE",
                            color = RoomColors.liveRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        room.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${room.participants.size} listening",
                        color = RoomColors.textTertiary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.KeyboardArrowDown, "Minimize")
            }
        },
        actions = {
            IconButton(onClick = { /* Share */ }) {
                Icon(Icons.Default.Share, "Share")
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.MoreVert, "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = RoomColors.textPrimary,
            navigationIconContentColor = RoomColors.textSecondary,
            actionIconContentColor = RoomColors.textSecondary
        )
    )
}

@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "dot")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color.copy(alpha = alpha), CircleShape)
    )
}

// ============================================
// 📋 ROOM INFO CARD
// ============================================

@Composable
private fun RoomInfoCard(room: AudioRoom) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = RoomColors.glassWhite,
        border = BorderStroke(1.dp, RoomColors.glassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    room.description,
                    color = RoomColors.textSecondary,
                    fontSize = 14.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RoomColors.surfaceVariant
                ) {
                    Text(
                        "Code: ${room.code}",
                        color = RoomColors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// 🎵 CO-LISTENING CARD
// ============================================

@Composable
private fun CoListeningCard(
    isPlaying: Boolean,
    position: Float,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(RoomColors.pink.copy(alpha = 0.8f), RoomColors.purple.copy(alpha = 0.8f))
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MusicNote,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "CO-LISTENING",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album art placeholder
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Album,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Blinding Lights",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "The Weeknd",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }

                    // Play/Pause
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Waveform / Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    repeat(40) { i ->
                        val h = if (isPlaying) {
                            (6 + 10 * sin(phase / 25f + i * 0.4f)).coerceIn(4f, 16f)
                        } else {
                            (4 + 8 * sin(i * 0.5f)).coerceIn(4f, 12f)
                        }
                        val isPlayed = i.toFloat() / 40f <= position

                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(h.dp)
                                .background(
                                    if (isPlayed) Color.White else Color.White.copy(alpha = 0.3f),
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "1:12",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.People,
                            null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "47 listening",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        "3:24",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ============================================
// 🎤 SPEAKERS SECTION
// ============================================

@Composable
private fun SpeakersSection(speakers: List<AudioRoomParticipant>) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            "Speakers",
            color = RoomColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(((speakers.size / 3 + 1) * 110).dp)
        ) {
            items(speakers) { speaker ->
                ParticipantCard(
                    participant = speaker,
                    isSpeaker = true
                )
            }
        }
    }
}

// ============================================
// 👥 LISTENERS SECTION
// ============================================

@Composable
private fun ListenersSection(listeners: List<AudioRoomParticipant>) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Listeners (${listeners.size})",
                color = RoomColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Raised hands indicator
            val raisedHands = listeners.count { it.isHandRaised }
            if (raisedHands > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RoomColors.orange.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✋", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "$raisedHands raised",
                            color = RoomColors.orange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(((listeners.size / 4 + 1) * 90).dp)
        ) {
            items(listeners) { listener ->
                ParticipantCard(
                    participant = listener,
                    isSpeaker = false
                )
            }
        }
    }
}

// ============================================
// 👤 PARTICIPANT CARD
// ============================================

@Composable
private fun ParticipantCard(
    participant: AudioRoomParticipant,
    isSpeaker: Boolean
) {
    val isSpeaking = participant.presenceState == PresenceState.SPEAKING

    val transition = rememberInfiniteTransition(label = "speaking")
    val ringAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isSpeaking) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring"
    )

    val ringScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSpeaking) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { }
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Speaking ring
            if (isSpeaking) {
                Box(
                    modifier = Modifier
                        .size(if (isSpeaker) 68.dp else 52.dp)
                        .scale(ringScale)
                        .border(
                            3.dp,
                            RoomColors.speakingGreen.copy(alpha = ringAlpha),
                            CircleShape
                        )
                )
            }

            // Avatar
            Box(
                modifier = Modifier
                    .size(if (isSpeaker) 60.dp else 44.dp)
                    .background(
                        when (participant.role) {
                            ParticipantRole.HOST -> RoomColors.pink
                            ParticipantRole.CO_HOST -> RoomColors.purple
                            ParticipantRole.SPEAKER -> RoomColors.blue
                            ParticipantRole.LISTENER -> RoomColors.surfaceVariant
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    participant.displayName.first().toString(),
                    color = Color.White,
                    fontSize = if (isSpeaker) 22.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Role badge
            if (participant.role == ParticipantRole.HOST) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(20.dp)
                        .background(RoomColors.orange, CircleShape)
                        .border(2.dp, RoomColors.bgDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👑", fontSize = 10.sp)
                }
            }

            // Muted indicator
            if (participant.isMuted && isSpeaker) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(20.dp)
                        .background(RoomColors.mutedGray, CircleShape)
                        .border(2.dp, RoomColors.bgDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MicOff,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Hand raised
            if (participant.isHandRaised) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(20.dp)
                        .background(RoomColors.orange, CircleShape)
                        .border(2.dp, RoomColors.bgDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✋", fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            participant.displayName,
            color = RoomColors.textPrimary,
            fontSize = if (isSpeaker) 12.sp else 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(if (isSpeaker) 70.dp else 50.dp)
        )
    }
}

// ============================================
// 🎛️ CONTROL BAR
// ============================================

@Composable
private fun RoomControlBar(
    isMuted: Boolean,
    isHandRaised: Boolean,
    onMuteToggle: () -> Unit,
    onHandToggle: () -> Unit,
    onCoListen: () -> Unit,
    onDSP: () -> Unit,
    onLeave: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = RoomColors.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute button
            ControlButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMuted) "Unmute" else "Mute",
                color = if (isMuted) RoomColors.mutedGray else RoomColors.speakingGreen,
                onClick = onMuteToggle
            )

            // Hand raise
            ControlButton(
                icon = Icons.Default.PanTool,
                label = if (isHandRaised) "Lower" else "Raise",
                color = if (isHandRaised) RoomColors.orange else RoomColors.textTertiary,
                onClick = onHandToggle
            )

            // Co-Listen
            ControlButton(
                icon = Icons.Default.QueueMusic,
                label = "Co-Listen",
                color = RoomColors.pink,
                onClick = onCoListen
            )

            // DSP/FX
            ControlButton(
                icon = Icons.Default.Tune,
                label = "FX",
                color = RoomColors.purple,
                onClick = onDSP
            )

            // Leave
            ControlButton(
                icon = Icons.Default.ExitToApp,
                label = "Leave",
                color = RoomColors.red,
                onClick = onLeave,
                isDestructive = true
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isDestructive) color.copy(alpha = 0.15f) else color.copy(alpha = 0.15f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================
// 🎚️ DSP BOTTOM SHEET
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DSPBottomSheet(onDismiss: () -> Unit) {
    var eqEnabled by remember { mutableStateOf(false) }
    var spatialEnabled by remember { mutableStateOf(false) }
    var bassBoost by remember { mutableFloatStateOf(0.5f) }
    var reverb by remember { mutableFloatStateOf(0.3f) }
    var spatialWidth by remember { mutableFloatStateOf(0.5f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RoomColors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(RoomColors.glassBorder, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Audio FX & Spatial",
                color = RoomColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // EQ Toggle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = RoomColors.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Equalizer,
                            null,
                            tint = RoomColors.pink,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Equalizer", color = RoomColors.textPrimary, fontWeight = FontWeight.Medium)
                            Text("Customize frequency response", color = RoomColors.textTertiary, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = eqEnabled,
                        onCheckedChange = { eqEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = RoomColors.pink)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Spatial Audio Toggle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = RoomColors.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SurroundSound,
                            null,
                            tint = RoomColors.purple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Spatial Audio", color = RoomColors.textPrimary, fontWeight = FontWeight.Medium)
                            Text("Immersive 3D sound", color = RoomColors.textTertiary, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = spatialEnabled,
                        onCheckedChange = { spatialEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = RoomColors.purple)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sliders
            Text("Quick Controls", color = RoomColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))

            DSPSlider(
                label = "Bass Boost",
                value = bassBoost,
                onValueChange = { bassBoost = it },
                color = RoomColors.pink
            )

            DSPSlider(
                label = "Reverb",
                value = reverb,
                onValueChange = { reverb = it },
                color = RoomColors.blue
            )

            DSPSlider(
                label = "Spatial Width",
                value = spatialWidth,
                onValueChange = { spatialWidth = it },
                color = RoomColors.purple
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoomColors.pink),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Apply", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DSPSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = RoomColors.textSecondary, fontSize = 13.sp)
            Text("${(value * 100).toInt()}%", color = color, fontSize = 13.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            )
        )
    }
}

