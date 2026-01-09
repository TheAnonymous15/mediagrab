package com.example.dwn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
// 🎵 AUDIO SOCIAL COLORS
// ============================================

private object AudioSocialColors {
    val pink = Color(0xFFE91E63)
    val purple = Color(0xFF9C27B0)
    val blue = Color(0xFF2196F3)
    val cyan = Color(0xFF00BCD4)
    val teal = Color(0xFF009688)
    val green = Color(0xFF4CAF50)
    val orange = Color(0xFFFF5722)
    val amber = Color(0xFFFFC107)
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

    // Audio specific
    val liveRed = Color(0xFFFF3B30)
    val speakingGreen = Color(0xFF34C759)
    val waveformPink = Color(0xFFFF2D92)
}

// ============================================
// 🏠 AUDIO SOCIAL HUB SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSocialScreen(
    onBack: () -> Unit,
    onNavigateToRoom: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var showJoinRoomDialog by remember { mutableStateOf(false) }

    // Mock data for rooms
    val liveRooms = remember {
        listOf(
            MockRoom("1", "Late Night Beats 🎵", "Chill music vibes", 47, true),
            MockRoom("2", "Podcast Discussion", "Tech talk with friends", 23, true),
            MockRoom("3", "Album Release Party", "New Drake album listen", 156, true),
            MockRoom("4", "Study Session", "Lo-fi beats to study", 89, false)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AudioSocialColors.bgDark)
    ) {
        // Animated background
        AudioSocialBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Headphones,
                            null,
                            tint = AudioSocialColors.pink,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Audio Space",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                "Listen Together",
                                color = AudioSocialColors.textTertiary,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.Notifications, "Notifications")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = AudioSocialColors.textPrimary,
                    navigationIconContentColor = AudioSocialColors.textPrimary,
                    actionIconContentColor = AudioSocialColors.textSecondary
                )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Section
                AudioSocialHero(
                    onCreateRoom = { showCreateRoomDialog = true },
                    onJoinRoom = { showJoinRoomDialog = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Actions
                QuickActionsRow()

                Spacer(modifier = Modifier.height(24.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = AudioSocialColors.pink,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = AudioSocialColors.pink,
                                height = 3.dp
                            )
                        }
                    },
                    divider = {},
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Live Now") },
                        selectedContentColor = AudioSocialColors.pink,
                        unselectedContentColor = AudioSocialColors.textTertiary
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Discover") },
                        selectedContentColor = AudioSocialColors.pink,
                        unselectedContentColor = AudioSocialColors.textTertiary
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Clips") },
                        selectedContentColor = AudioSocialColors.pink,
                        unselectedContentColor = AudioSocialColors.textTertiary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on selected tab
                when (selectedTab) {
                    0 -> LiveRoomsSection(rooms = liveRooms, onRoomClick = onNavigateToRoom)
                    1 -> DiscoverSection()
                    2 -> ClipsSection()
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showCreateRoomDialog = true },
            containerColor = AudioSocialColors.pink,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Icon(Icons.Default.Add, "Create Room", tint = Color.White)
        }
    }

    // Dialogs
    if (showCreateRoomDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateRoomDialog = false },
            onCreate = { title, type ->
                showCreateRoomDialog = false
                // Navigate to new room
            }
        )
    }

    if (showJoinRoomDialog) {
        JoinRoomDialog(
            onDismiss = { showJoinRoomDialog = false },
            onJoin = { code ->
                showJoinRoomDialog = false
                onNavigateToRoom(code)
            }
        )
    }
}

// ============================================
// 🌌 ANIMATED BACKGROUND
// ============================================

@Composable
private fun AudioSocialBackground() {
    val transition = rememberInfiniteTransition(label = "bg")

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    AudioSocialColors.bgDark,
                    AudioSocialColors.bgMid,
                    Color(0xFF0F0818),
                    AudioSocialColors.bgDark
                )
            )
        )

        // Animated orbs
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    AudioSocialColors.pink.copy(alpha = 0.12f * pulse),
                    Color.Transparent
                )
            ),
            radius = 400f,
            center = Offset(
                size.width * 0.8f + kotlin.math.cos(Math.toRadians(phase.toDouble())).toFloat() * 50f,
                size.height * 0.15f
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    AudioSocialColors.purple.copy(alpha = 0.1f * pulse),
                    Color.Transparent
                )
            ),
            radius = 350f,
            center = Offset(
                size.width * 0.2f,
                size.height * 0.6f + kotlin.math.sin(Math.toRadians(phase.toDouble() * 0.5)).toFloat() * 30f
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    AudioSocialColors.cyan.copy(alpha = 0.08f),
                    Color.Transparent
                )
            ),
            radius = 300f,
            center = Offset(size.width * 0.9f, size.height * 0.8f)
        )
    }
}

// ============================================
// 🎯 HERO SECTION
// ============================================

@Composable
private fun AudioSocialHero(
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            AudioSocialColors.pink,
                            AudioSocialColors.purple,
                            AudioSocialColors.blue
                        )
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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LiveIndicator()
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "47 rooms live",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Listen\nTogether",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 38.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Join rooms, share music, and vibe with friends in real-time",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }

                    // Animated waveform icon
                    WaveformIcon()
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onCreateRoom,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = AudioSocialColors.pink
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Room", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onJoinRoom,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Login, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Join Room", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveIndicator() {
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(AudioSocialColors.liveRed.copy(alpha = alpha), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "LIVE",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun WaveformIcon() {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .background(Color.White.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { i ->
                val height = (12 + 16 * sin(phase / 30f + i * 0.8f)).coerceIn(8f, 28f)
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(height.dp)
                        .background(Color.White, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

// ============================================
// ⚡ QUICK ACTIONS
// ============================================

@Composable
private fun QuickActionsRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            QuickActionCard(
                icon = Icons.Default.MusicNote,
                title = "Music Party",
                subtitle = "Listen together",
                color = AudioSocialColors.pink
            )
        }
        item {
            QuickActionCard(
                icon = Icons.Default.Podcasts,
                title = "Podcast",
                subtitle = "Group listen",
                color = AudioSocialColors.purple
            )
        }
        item {
            QuickActionCard(
                icon = Icons.Default.School,
                title = "Study",
                subtitle = "Focus rooms",
                color = AudioSocialColors.blue
            )
        }
        item {
            QuickActionCard(
                icon = Icons.Default.Mic,
                title = "Talk",
                subtitle = "Voice chat",
                color = AudioSocialColors.cyan
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {
    Surface(
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = AudioSocialColors.glassWhite,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        onClick = { }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                title,
                color = AudioSocialColors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                color = AudioSocialColors.textTertiary,
                fontSize = 10.sp
            )
        }
    }
}

// ============================================
// 📡 LIVE ROOMS SECTION
// ============================================

data class MockRoom(
    val id: String,
    val title: String,
    val description: String,
    val listenerCount: Int,
    val isLive: Boolean
)

@Composable
private fun LiveRoomsSection(
    rooms: List<MockRoom>,
    onRoomClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        rooms.forEach { room ->
            LiveRoomCard(room = room, onClick = { onRoomClick(room.id) })
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun LiveRoomCard(
    room: MockRoom,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = AudioSocialColors.card,
        border = BorderStroke(1.dp, AudioSocialColors.glassBorder),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (room.isLive) {
                        Box(
                            modifier = Modifier
                                .background(AudioSocialColors.liveRed.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(AudioSocialColors.liveRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "LIVE",
                                    color = AudioSocialColors.liveRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        room.title,
                        color = AudioSocialColors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Headphones,
                        null,
                        tint = AudioSocialColors.textTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${room.listenerCount}",
                        color = AudioSocialColors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                room.description,
                color = AudioSocialColors.textTertiary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Participant avatars
            Row(
                horizontalArrangement = Arrangement.spacedBy((-8).dp)
            ) {
                repeat(minOf(5, room.listenerCount)) { i ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                listOf(
                                    AudioSocialColors.pink,
                                    AudioSocialColors.purple,
                                    AudioSocialColors.blue,
                                    AudioSocialColors.cyan,
                                    AudioSocialColors.orange
                                )[i % 5],
                                CircleShape
                            )
                            .border(2.dp, AudioSocialColors.card, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            listOf("A", "B", "C", "D", "E")[i % 5],
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (room.listenerCount > 5) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(AudioSocialColors.surfaceVariant, CircleShape)
                            .border(2.dp, AudioSocialColors.card, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "+${room.listenerCount - 5}",
                            color = AudioSocialColors.textSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// 🔍 DISCOVER SECTION
// ============================================

@Composable
private fun DiscoverSection() {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            "Trending Topics",
            color = AudioSocialColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val topics = listOf("Music", "Podcast", "Study", "Gaming", "Tech", "Art", "Sports", "News")

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(topics) { topic ->
                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text(topic) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = AudioSocialColors.surfaceVariant,
                        labelColor = AudioSocialColors.textSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Recommended for You",
            color = AudioSocialColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Empty state
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AudioSocialColors.surface
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.Explore,
                    null,
                    tint = AudioSocialColors.textTertiary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Discover rooms based on your interests",
                    color = AudioSocialColors.textSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ============================================
// 🎵 CLIPS SECTION
// ============================================

@Composable
private fun ClipsSection() {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Your Clips",
                color = AudioSocialColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = { }) {
                Text("Create Clip", color = AudioSocialColors.pink)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Empty state
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AudioSocialColors.surface
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.GraphicEq,
                    null,
                    tint = AudioSocialColors.textTertiary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No clips yet",
                    color = AudioSocialColors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Create audio clips from rooms or your library",
                    color = AudioSocialColors.textTertiary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AudioSocialColors.pink
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Your First Clip")
                }
            }
        }
    }
}

// ============================================
// 📝 DIALOGS
// ============================================

@Composable
private fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (String, RoomType) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(RoomType.OPEN) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AudioSocialColors.surface,
        title = {
            Text("Create Room", color = AudioSocialColors.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Room Name") },
                    placeholder = { Text("What's this room about?") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AudioSocialColors.pink,
                        unfocusedBorderColor = AudioSocialColors.glassBorder,
                        focusedTextColor = AudioSocialColors.textPrimary,
                        unfocusedTextColor = AudioSocialColors.textPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Room Type", color = AudioSocialColors.textSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoomTypeOption(
                        title = "Open Room",
                        description = "Anyone can join and raise hand to speak",
                        icon = Icons.Default.Public,
                        isSelected = selectedType == RoomType.OPEN,
                        onClick = { selectedType = RoomType.OPEN }
                    )
                    RoomTypeOption(
                        title = "Stage Room",
                        description = "You control who speaks",
                        icon = Icons.Default.RecordVoiceOver,
                        isSelected = selectedType == RoomType.STAGE,
                        onClick = { selectedType = RoomType.STAGE }
                    )
                    RoomTypeOption(
                        title = "Private Room",
                        description = "Invite-only with encryption",
                        icon = Icons.Default.Lock,
                        isSelected = selectedType == RoomType.PRIVATE,
                        onClick = { selectedType = RoomType.PRIVATE }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, selectedType) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AudioSocialColors.pink)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AudioSocialColors.textSecondary)
            }
        }
    )
}

@Composable
private fun RoomTypeOption(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AudioSocialColors.pink.copy(alpha = 0.15f) else AudioSocialColors.surfaceVariant,
        border = if (isSelected) BorderStroke(1.dp, AudioSocialColors.pink) else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = if (isSelected) AudioSocialColors.pink else AudioSocialColors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = AudioSocialColors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    description,
                    color = AudioSocialColors.textTertiary,
                    fontSize = 11.sp
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = AudioSocialColors.pink,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun JoinRoomDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AudioSocialColors.surface,
        title = {
            Text("Join Room", color = AudioSocialColors.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Enter the 6-character room code",
                    color = AudioSocialColors.textSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it.uppercase() },
                    label = { Text("Room Code") },
                    placeholder = { Text("ABC123") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AudioSocialColors.pink,
                        unfocusedBorderColor = AudioSocialColors.glassBorder,
                        focusedTextColor = AudioSocialColors.textPrimary,
                        unfocusedTextColor = AudioSocialColors.textPrimary
                    ),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(code) },
                enabled = code.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = AudioSocialColors.pink)
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AudioSocialColors.textSecondary)
            }
        }
    )
}

