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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.dwn.radio.*
import kotlin.math.sin

// ============================================
// 🎨 RADIO STUDIO COLORS
// ============================================

private object StudioColors {
    val pink = Color(0xFFE91E63)
    val pinkLight = Color(0xFFFF4081)
    val purple = Color(0xFF9C27B0)
    val blue = Color(0xFF2196F3)
    val cyan = Color(0xFF00BCD4)
    val teal = Color(0xFF009688)
    val green = Color(0xFF4CAF50)
    val orange = Color(0xFFFF5722)
    val amber = Color(0xFFFFC107)
    val red = Color(0xFFE53935)

    val bgDark = Color(0xFF0A0A0F)
    val bgMid = Color(0xFF101018)
    val surface = Color(0xFF161620)
    val surfaceVariant = Color(0xFF1E1E2A)
    val card = Color(0xFF222230)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B8)
    val textTertiary = Color(0xFF707080)

    val glassWhite = Color(0x14FFFFFF)
    val glassBorder = Color(0x20FFFFFF)

    val live = Color(0xFFE53935)
    val onAir = Color(0xFFFF1744)
}

// ============================================
// 📻 RADIO STUDIO SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRadioScreen(
    onBack: () -> Unit
) {
    // State
    var hasStation by remember { mutableStateOf(false) }
    var isLive by remember { mutableStateOf(false) }
    var showCreateStation by remember { mutableStateOf(false) }
    var showGoLive by remember { mutableStateOf(false) }
    var showMixer by remember { mutableStateOf(false) }
    var showSoundBoard by remember { mutableStateOf(false) }
    var showPlaylist by remember { mutableStateOf(false) }
    var showAnalytics by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Station data
    var stationName by remember { mutableStateOf("My Radio Station") }
    var stationTagline by remember { mutableStateOf("Broadcasting the best vibes") }

    // Broadcast data
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastDuration by remember { mutableLongStateOf(0L) }
    var listenerCount by remember { mutableIntStateOf(0) }
    var peakListeners by remember { mutableIntStateOf(0) }

    // Mixer state
    var micVolume by remember { mutableFloatStateOf(0.8f) }
    var micMuted by remember { mutableStateOf(false) }
    var mediaVolume by remember { mutableFloatStateOf(0.6f) }
    var mediaMuted by remember { mutableStateOf(false) }

    // Chat messages
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }

    // Simulate broadcast
    LaunchedEffect(isLive) {
        if (isLive) {
            while (isLive) {
                kotlinx.coroutines.delay(1000)
                broadcastDuration += 1000

                // Simulate listener changes
                if ((0..10).random() > 6) {
                    val change = (-2..4).random()
                    listenerCount = (listenerCount + change).coerceIn(1, 500)
                    if (listenerCount > peakListeners) peakListeners = listenerCount
                }

                // Simulate chat
                if ((0..20).random() > 17) {
                    val names = listOf("MusicLover", "RadioFan", "NightOwl", "ChillVibes", "BeatDropper")
                    val msgs = listOf("🔥🔥🔥", "Great vibes!", "Love this!", "Hello from Brazil!", "👏👏")
                    chatMessages = (chatMessages + ChatMessage(
                        senderId = "user",
                        senderName = names.random(),
                        message = msgs.random()
                    )).takeLast(50)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.bgDark)
    ) {
        // Animated background
        StudioBackground(isLive = isLive)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            StudioTopBar(
                isLive = isLive,
                onBack = onBack,
                onSettings = { }
            )

            if (!hasStation) {
                // Create Station View
                CreateStationView(
                    onCreateStation = { name, tagline ->
                        stationName = name
                        stationTagline = tagline
                        hasStation = true
                    }
                )
            } else if (!isLive) {
                // Station Dashboard
                StationDashboard(
                    stationName = stationName,
                    stationTagline = stationTagline,
                    onGoLive = { showGoLive = true },
                    onEditStation = { showCreateStation = true },
                    onViewArchive = { selectedTab = 1 },
                    onViewAnalytics = { showAnalytics = true }
                )
            } else {
                // Live Broadcast View
                LiveBroadcastView(
                    broadcastTitle = broadcastTitle,
                    duration = broadcastDuration,
                    listenerCount = listenerCount,
                    peakListeners = peakListeners,
                    micVolume = micVolume,
                    micMuted = micMuted,
                    mediaVolume = mediaVolume,
                    mediaMuted = mediaMuted,
                    chatMessages = chatMessages,
                    onMicVolumeChange = { micVolume = it },
                    onMicMuteToggle = { micMuted = !micMuted },
                    onMediaVolumeChange = { mediaVolume = it },
                    onMediaMuteToggle = { mediaMuted = !mediaMuted },
                    onOpenMixer = { showMixer = true },
                    onOpenSoundBoard = { showSoundBoard = true },
                    onOpenPlaylist = { showPlaylist = true },
                    onEndBroadcast = {
                        isLive = false
                        broadcastDuration = 0L
                        listenerCount = 0
                        peakListeners = 0
                        chatMessages = emptyList()
                        showAnalytics = true
                    },
                    onSendMessage = { msg ->
                        chatMessages = (chatMessages + ChatMessage(
                            senderId = "host",
                            senderName = stationName,
                            message = msg,
                            isHost = true
                        )).takeLast(50)
                    }
                )
            }
        }
    }

    // Go Live Dialog
    if (showGoLive) {
        GoLiveDialog(
            onDismiss = { showGoLive = false },
            onGoLive = { title ->
                broadcastTitle = title
                isLive = true
                listenerCount = (5..15).random()
                showGoLive = false
            }
        )
    }

    // Mixer Sheet
    if (showMixer) {
        MixerSheet(
            micVolume = micVolume,
            micMuted = micMuted,
            mediaVolume = mediaVolume,
            mediaMuted = mediaMuted,
            onMicVolumeChange = { micVolume = it },
            onMicMuteToggle = { micMuted = !micMuted },
            onMediaVolumeChange = { mediaVolume = it },
            onMediaMuteToggle = { mediaMuted = !mediaMuted },
            onDismiss = { showMixer = false }
        )
    }

    // Sound Board Sheet
    if (showSoundBoard) {
        SoundBoardSheet(
            onDismiss = { showSoundBoard = false }
        )
    }

    // Analytics Dialog
    if (showAnalytics) {
        AnalyticsDialog(
            duration = broadcastDuration,
            totalListeners = peakListeners + (10..30).random(),
            peakListeners = peakListeners,
            chatMessages = chatMessages.size,
            onDismiss = { showAnalytics = false }
        )
    }
}

// ============================================
// 🌌 ANIMATED BACKGROUND
// ============================================

@Composable
private fun StudioBackground(isLive: Boolean) {
    val transition = rememberInfiniteTransition(label = "bg")

    val pulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isLive) 1000 else 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    StudioColors.bgDark,
                    if (isLive) Color(0xFF1A0A0A) else Color(0xFF0D0815),
                    StudioColors.bgMid,
                    StudioColors.bgDark
                )
            )
        )

        val color = if (isLive) StudioColors.live else StudioColors.purple

        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    color.copy(alpha = 0.1f * pulse),
                    Color.Transparent
                )
            ),
            radius = 400f * pulse,
            center = Offset(size.width * 0.5f, size.height * 0.2f)
        )
    }
}

// ============================================
// 📱 TOP BAR
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioTopBar(
    isLive: Boolean,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLive) {
                    // Live indicator
                    val transition = rememberInfiniteTransition(label = "live")
                    val alpha by transition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StudioColors.live.copy(alpha = alpha)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "ON AIR",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column {
                    Text(
                        "Radio Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        if (isLive) "Broadcasting live" else "Your station",
                        color = StudioColors.textTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        actions = {
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = StudioColors.textPrimary,
            navigationIconContentColor = StudioColors.textSecondary,
            actionIconContentColor = StudioColors.textSecondary
        )
    )
}

// ============================================
// 🆕 CREATE STATION VIEW
// ============================================

@Composable
private fun CreateStationView(
    onCreateStation: (name: String, tagline: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    Brush.linearGradient(listOf(StudioColors.pink, StudioColors.purple)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Radio,
                null,
                tint = Color.White,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Create Your Station",
            color = StudioColors.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Start broadcasting to the world",
            color = StudioColors.textTertiary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Station name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Station Name") },
            placeholder = { Text("My Awesome Radio") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StudioColors.pink,
                unfocusedBorderColor = StudioColors.glassBorder,
                focusedTextColor = StudioColors.textPrimary,
                unfocusedTextColor = StudioColors.textPrimary,
                focusedLabelColor = StudioColors.pink,
                unfocusedLabelColor = StudioColors.textSecondary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tagline
        OutlinedTextField(
            value = tagline,
            onValueChange = { tagline = it },
            label = { Text("Tagline") },
            placeholder = { Text("The best music, 24/7") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StudioColors.pink,
                unfocusedBorderColor = StudioColors.glassBorder,
                focusedTextColor = StudioColors.textPrimary,
                unfocusedTextColor = StudioColors.textPrimary,
                focusedLabelColor = StudioColors.pink,
                unfocusedLabelColor = StudioColors.textSecondary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onCreateStation(name.ifEmpty { "My Radio" }, tagline) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StudioColors.pink
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Station", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ============================================
// 📊 STATION DASHBOARD
// ============================================

@Composable
private fun StationDashboard(
    stationName: String,
    stationTagline: String,
    onGoLive: () -> Unit,
    onEditStation: () -> Unit,
    onViewArchive: () -> Unit,
    onViewAnalytics: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Station Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(StudioColors.purple, StudioColors.pink)
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                stationName,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (stationTagline.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    stationTagline,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                            }
                        }

                        IconButton(onClick = onEditStation) {
                            Icon(Icons.Default.Edit, "Edit", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Go Live Button
                    Button(
                        onClick = onGoLive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            null,
                            tint = StudioColors.pink,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "GO LIVE",
                            color = StudioColors.pink,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Stats
        Text(
            "Station Stats",
            color = StudioColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = "0",
                label = "Total Shows",
                icon = Icons.Default.Mic,
                color = StudioColors.pink,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "0",
                label = "Followers",
                icon = Icons.Default.People,
                color = StudioColors.purple,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "0h",
                label = "Broadcast",
                icon = Icons.Default.Schedule,
                color = StudioColors.blue,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions
        Text(
            "Quick Actions",
            color = StudioColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.History,
                title = "Archive",
                subtitle = "Past shows",
                color = StudioColors.orange,
                onClick = onViewArchive,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Default.Analytics,
                title = "Analytics",
                subtitle = "View stats",
                color = StudioColors.cyan,
                onClick = onViewAnalytics,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.Schedule,
                title = "Schedule",
                subtitle = "Plan shows",
                color = StudioColors.green,
                onClick = { },
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Default.Settings,
                title = "Settings",
                subtitle = "Configure",
                color = StudioColors.purple,
                onClick = { },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = StudioColors.card,
        border = BorderStroke(1.dp, StudioColors.glassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = StudioColors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, color = StudioColors.textTertiary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = StudioColors.card,
        border = BorderStroke(1.dp, StudioColors.glassBorder),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = StudioColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = StudioColors.textTertiary, fontSize = 11.sp)
            }
        }
    }
}

// ============================================
// 🔴 LIVE BROADCAST VIEW
// ============================================

@Composable
private fun LiveBroadcastView(
    broadcastTitle: String,
    duration: Long,
    listenerCount: Int,
    peakListeners: Int,
    micVolume: Float,
    micMuted: Boolean,
    mediaVolume: Float,
    mediaMuted: Boolean,
    chatMessages: List<ChatMessage>,
    onMicVolumeChange: (Float) -> Unit,
    onMicMuteToggle: () -> Unit,
    onMediaVolumeChange: (Float) -> Unit,
    onMediaMuteToggle: () -> Unit,
    onOpenMixer: () -> Unit,
    onOpenSoundBoard: () -> Unit,
    onOpenPlaylist: () -> Unit,
    onEndBroadcast: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var chatInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Live Stats Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = StudioColors.live.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, StudioColors.live.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            broadcastTitle.ifEmpty { "Live Broadcast" },
                            color = StudioColors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            formatDuration(duration),
                            color = StudioColors.live,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.People,
                                null,
                                tint = StudioColors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "$listenerCount",
                                color = StudioColors.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Peak: $peakListeners",
                            color = StudioColors.textTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Audio Level Visualizer
                AudioLevelVisualizer()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mic control
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = if (micMuted) StudioColors.red.copy(alpha = 0.2f) else StudioColors.card,
                onClick = onMicMuteToggle
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (micMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        null,
                        tint = if (micMuted) StudioColors.red else StudioColors.green,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (micMuted) "Muted" else "Mic On",
                        color = StudioColors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Mixer
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = StudioColors.card,
                onClick = onOpenMixer
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Tune,
                        null,
                        tint = StudioColors.purple,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Mixer", color = StudioColors.textSecondary, fontSize = 11.sp)
                }
            }

            // Sound Board
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = StudioColors.card,
                onClick = onOpenSoundBoard
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.GridView,
                        null,
                        tint = StudioColors.orange,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sounds", color = StudioColors.textSecondary, fontSize = 11.sp)
                }
            }

            // Playlist
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = StudioColors.card,
                onClick = onOpenPlaylist
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.QueueMusic,
                        null,
                        tint = StudioColors.cyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Music", color = StudioColors.textSecondary, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chat Section
        Text(
            "Live Chat",
            color = StudioColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = StudioColors.card,
            border = BorderStroke(1.dp, StudioColors.glassBorder)
        ) {
            Column {
                // Messages
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    reverseLayout = true
                ) {
                    items(chatMessages.reversed()) { message ->
                        ChatMessageItem(message)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioColors.surfaceVariant)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Message your listeners...", color = StudioColors.textTertiary) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = StudioColors.textPrimary,
                            unfocusedTextColor = StudioColors.textPrimary
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank()) {
                                onSendMessage(chatInput)
                                chatInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, null, tint = StudioColors.pink)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // End Broadcast Button
        Button(
            onClick = onEndBroadcast,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StudioColors.red),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Stop, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("End Broadcast", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AudioLevelVisualizer() {
    val transition = rememberInfiniteTransition(label = "audio")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(30) { index ->
            val height by transition.animateFloat(
                initialValue = 8f,
                targetValue = 32f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (200..500).random(),
                        delayMillis = index * 20,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(StudioColors.live, StudioColors.orange)
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (message.isHost) StudioColors.pink else StudioColors.purple.copy(alpha = 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                message.senderName.first().toString(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    message.senderName,
                    color = if (message.isHost) StudioColors.pink else StudioColors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (message.isHost) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StudioColors.pink.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "HOST",
                            color = StudioColors.pink,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Text(
                message.message,
                color = StudioColors.textPrimary,
                fontSize = 13.sp
            )
        }
    }
}

// ============================================
// 🎙️ GO LIVE DIALOG
// ============================================

@Composable
private fun GoLiveDialog(
    onDismiss: () -> Unit,
    onGoLive: (title: String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioColors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, null, tint = StudioColors.live)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Go Live", color = StudioColors.textPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "Give your broadcast a title",
                    color = StudioColors.textSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g., Late Night Vibes") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioColors.pink,
                        unfocusedBorderColor = StudioColors.glassBorder,
                        focusedTextColor = StudioColors.textPrimary,
                        unfocusedTextColor = StudioColors.textPrimary
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onGoLive(title) },
                colors = ButtonDefaults.buttonColors(containerColor = StudioColors.live)
            ) {
                Text("GO LIVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = StudioColors.textSecondary)
            }
        }
    )
}

// ============================================
// 🎛️ MIXER SHEET
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MixerSheet(
    micVolume: Float,
    micMuted: Boolean,
    mediaVolume: Float,
    mediaMuted: Boolean,
    onMicVolumeChange: (Float) -> Unit,
    onMicMuteToggle: () -> Unit,
    onMediaVolumeChange: (Float) -> Unit,
    onMediaMuteToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioColors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Audio Mixer",
                color = StudioColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Microphone
            MixerChannel(
                name = "Microphone",
                icon = Icons.Default.Mic,
                color = StudioColors.green,
                volume = micVolume,
                isMuted = micMuted,
                onVolumeChange = onMicVolumeChange,
                onMuteToggle = onMicMuteToggle
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Media
            MixerChannel(
                name = "Media/Music",
                icon = Icons.Default.MusicNote,
                color = StudioColors.cyan,
                volume = mediaVolume,
                isMuted = mediaMuted,
                onVolumeChange = onMediaVolumeChange,
                onMuteToggle = onMediaMuteToggle
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Effects toggles
            Text(
                "Effects",
                color = StudioColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EffectToggle("Noise Gate", true, {}, Modifier.weight(1f))
                EffectToggle("Compressor", true, {}, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EffectToggle("Voice Enhance", false, {}, Modifier.weight(1f))
                EffectToggle("Auto-Duck", true, {}, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MixerChannel(
    name: String,
    icon: ImageVector,
    color: Color,
    volume: Float,
    isMuted: Boolean,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = StudioColors.card
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(name, color = StudioColors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                IconButton(onClick = onMuteToggle) {
                    Icon(
                        if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        null,
                        tint = if (isMuted) StudioColors.red else StudioColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = color,
                        activeTrackColor = color
                    ),
                    enabled = !isMuted
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    "${(volume * 100).toInt()}%",
                    color = if (isMuted) StudioColors.textTertiary else StudioColors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EffectToggle(
    name: String,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) StudioColors.pink.copy(alpha = 0.15f) else StudioColors.card,
        border = BorderStroke(1.dp, if (enabled) StudioColors.pink.copy(alpha = 0.3f) else StudioColors.glassBorder),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (enabled) StudioColors.green else StudioColors.textTertiary, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                name,
                color = if (enabled) StudioColors.textPrimary else StudioColors.textTertiary,
                fontSize = 12.sp
            )
        }
    }
}

// ============================================
// 🎹 SOUND BOARD SHEET
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundBoardSheet(onDismiss: () -> Unit) {
    val sounds = defaultSoundBoardItems

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioColors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Sound Board",
                color = StudioColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(280.dp)
            ) {
                items(sounds) { sound ->
                    SoundBoardButton(sound = sound, onClick = { })
                }
            }
        }
    }
}

@Composable
private fun SoundBoardButton(
    sound: SoundBoardItem,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(sound.color).copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Color(sound.color).copy(alpha = 0.3f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(sound.icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                sound.name,
                color = StudioColors.textSecondary,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================
// 📊 ANALYTICS DIALOG
// ============================================

@Composable
private fun AnalyticsDialog(
    duration: Long,
    totalListeners: Int,
    peakListeners: Int,
    chatMessages: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioColors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Analytics, null, tint = StudioColors.cyan)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Broadcast Summary", color = StudioColors.textPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                AnalyticRow("Duration", formatDuration(duration))
                AnalyticRow("Total Listeners", "$totalListeners")
                AnalyticRow("Peak Listeners", "$peakListeners")
                AnalyticRow("Chat Messages", "$chatMessages")
                AnalyticRow("New Followers", "+${(2..10).random()}")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = StudioColors.pink)
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun AnalyticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = StudioColors.textSecondary, fontSize = 14.sp)
        Text(value, color = StudioColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ============================================
// 🔧 HELPERS
// ============================================

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 60000) % 60
    val hours = ms / 3600000
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

