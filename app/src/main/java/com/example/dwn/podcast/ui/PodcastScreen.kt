package com.example.dwn.podcast.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.podcast.*
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ============================================
// THEME COLORS - FUTURISTIC DARK
// ============================================

private val PodcastPrimary = Color(0xFF6C63FF)      // Purple accent
private val PodcastSecondary = Color(0xFF00D9FF)    // Cyan accent
private val PodcastAccent = Color(0xFFFF6B6B)       // Red accent
private val PodcastGreen = Color(0xFF00E676)        // Green for recording
private val PodcastOrange = Color(0xFFFF9800)       // Orange
private val PodcastPink = Color(0xFFFF4081)         // Pink
private val PodcastYellow = Color(0xFFFFEB3B)       // Yellow

private val PodcastBackground = Color(0xFF0A0A0F)
private val PodcastSurface = Color(0xFF12121A)
private val PodcastSurfaceLight = Color(0xFF1A1A24)
private val PodcastCard = Color(0xFF16161F)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB0B0C0)
private val TextTertiary = Color(0xFF6E6E80)

// ============================================
// MAIN PODCAST SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val podcastManager = remember { PodcastManager(context) }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Record", "Edit", "Live", "AI Tools", "Publish")

    val projects by podcastManager.projects.collectAsState()
    val currentProject by podcastManager.currentProject.collectAsState()
    val currentEpisode by podcastManager.currentEpisode.collectAsState()
    val recordingState by podcastManager.recordingState.collectAsState()
    val liveState by podcastManager.liveStreamState.collectAsState()
    val aiState by podcastManager.aiState.collectAsState()
    val remoteSession by podcastManager.remoteSession.collectAsState()
    val recordings by podcastManager.recordings.collectAsState()

    // LiveKit state
    val liveKitState by podcastManager.liveKitRoomState.collectAsState()

    // Permission handling
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Selected recording for processing
    var selectedRecording by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PodcastBackground)
    ) {
        // Animated background gradient
        AnimatedGradientBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PodcastTopBar(
                    title = "Podcast Studio",
                    onBack = onBack,
                    isRecording = recordingState.isRecording,
                    isLive = liveState.isLive
                )
            },
            bottomBar = {
                PodcastBottomNavigation(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Recording indicator bar
                AnimatedVisibility(
                    visible = recordingState.isRecording || liveState.isLive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    RecordingIndicatorBar(
                        isRecording = recordingState.isRecording,
                        isLive = liveState.isLive,
                        duration = if (recordingState.isRecording) recordingState.duration else liveState.duration,
                        viewerCount = liveState.viewerCount
                    )
                }

                // Tab content
                when (selectedTab) {
                    0 -> DashboardTab(
                        projects = projects,
                        currentProject = currentProject,
                        onProjectSelect = { podcastManager.selectProject(it) },
                        onCreateProject = { name, desc -> podcastManager.createProject(name, desc) }
                    )
                    1 -> RecordingTab(
                        recordingState = recordingState,
                        remoteSession = remoteSession,
                        recordings = recordings,
                        onStartRecording = { podcastManager.startRecording() },
                        onPauseRecording = { podcastManager.pauseRecording() },
                        onResumeRecording = { podcastManager.resumeRecording() },
                        onStopRecording = { podcastManager.stopRecording() },
                        onCreateRemoteSession = { podcastManager.createRemoteSession() },
                        onEndRemoteSession = { podcastManager.endRemoteSession() },
                        onAddGuest = { podcastManager.addGuest(it) },
                        onDeleteRecording = { podcastManager.deleteRecording(it) },
                        onSelectRecording = { path ->
                            // Create episode from recording
                            currentProject?.let { project ->
                                scope.launch {
                                    val episode = podcastManager.createEpisodeFromRecording(
                                        projectId = project.id,
                                        title = "New Episode",
                                        recordingPath = path
                                    )
                                    episode?.let { podcastManager.selectEpisode(it.id) }
                                }
                            }
                        }
                    )
                    2 -> EditingTab(
                        currentEpisode = currentEpisode,
                        onUpdateEpisode = { podcastManager.updateEpisode(it) }
                    )
                    3 -> LiveStreamingTab(
                        liveState = liveState,
                        liveKitState = liveKitState,
                        onStartStream = { platform, key -> podcastManager.startLiveStream(platform, key) },
                        onStopStream = { podcastManager.stopLiveStream() },
                        onCreateRoom = { name ->
                            scope.launch {
                                podcastManager.createLiveKitRoom(name)
                            }
                        },
                        onJoinRoom = { code, name ->
                            scope.launch {
                                podcastManager.joinLiveKitRoom(code, name)
                            }
                        },
                        onLeaveRoom = { podcastManager.leaveLiveKitRoom() },
                        onToggleMic = { enabled ->
                            scope.launch {
                                podcastManager.setLiveKitMicEnabled(enabled)
                            }
                        },
                        onToggleCamera = { enabled ->
                            scope.launch {
                                podcastManager.setLiveKitCameraEnabled(enabled)
                            }
                        }
                    )
                    4 -> AIToolsTab(
                        aiState = aiState,
                        recordings = recordings,
                        currentEpisode = currentEpisode,
                        onTranscribe = { path ->
                            scope.launch { podcastManager.transcribeAudio(path) }
                        },
                        onRemoveNoise = { path ->
                            scope.launch { podcastManager.removeNoise(path) }
                        },
                        onIsolateVoices = { path ->
                            scope.launch { podcastManager.isolateVoices(path) }
                        },
                        onGenerateChapters = {
                            scope.launch { podcastManager.generateChapters() }
                        },
                        onGenerateShowNotes = {
                            scope.launch { podcastManager.generateShowNotes() }
                        }
                    )
                    5 -> PublishTab(
                        currentProject = currentProject,
                        onExport = { format ->
                            scope.launch {
                                currentEpisode?.id?.let { id ->
                                    podcastManager.exportEpisode(id, com.example.dwn.podcast.export.AudioCodec.AAC)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            podcastManager.release()
        }
    }
}

// ============================================
// ANIMATED BACKGROUND
// ============================================

@Composable
private fun AnimatedGradientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 3

        // Animated gradient orbs
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    PodcastPrimary.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(
                    centerX + cos(offset * PI.toFloat() / 180) * 100,
                    centerY + sin(offset * PI.toFloat() / 180) * 100
                ),
                radius = 400f
            ),
            radius = 400f,
            center = Offset(
                centerX + cos(offset * PI.toFloat() / 180) * 100,
                centerY + sin(offset * PI.toFloat() / 180) * 100
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    PodcastSecondary.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                center = Offset(
                    centerX - cos((offset + 180) * PI.toFloat() / 180) * 150,
                    centerY - sin((offset + 180) * PI.toFloat() / 180) * 150
                ),
                radius = 350f
            ),
            radius = 350f,
            center = Offset(
                centerX - cos((offset + 180) * PI.toFloat() / 180) * 150,
                centerY - sin((offset + 180) * PI.toFloat() / 180) * 150
            )
        )
    }
}

// ============================================
// TOP BAR
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodcastTopBar(
    title: String,
    onBack: () -> Unit,
    isRecording: Boolean,
    isLive: Boolean
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Animated mic icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(PodcastPrimary, PodcastSecondary)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        title,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Pro Studio",
                        color = PodcastPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
        },
        actions = {
            // Live indicator
            if (isLive) {
                LiveBadge()
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Recording indicator
            if (isRecording) {
                RecordingBadge()
                Spacer(modifier = Modifier.width(8.dp))
            }

            IconButton(onClick = { }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun LiveBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "live")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = PodcastAccent.copy(alpha = alpha)
    ) {
        Text(
            "LIVE",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun RecordingBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "rec")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(PodcastSurface, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(PodcastAccent.copy(alpha = alpha), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "REC",
            color = PodcastAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ============================================
// BOTTOM NAVIGATION
// ============================================

@Composable
private fun PodcastBottomNavigation(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val icons = listOf(
        Icons.Default.Dashboard,
        Icons.Default.FiberManualRecord,
        Icons.Default.Edit,
        Icons.Default.Videocam,
        Icons.Default.AutoAwesome,
        Icons.Default.Publish
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PodcastSurface.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isSelected) {
                                    Brush.linearGradient(
                                        colors = listOf(PodcastPrimary, PodcastSecondary)
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(Color.Transparent, Color.Transparent)
                                    )
                                },
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icons[index],
                            contentDescription = title,
                            tint = if (isSelected) Color.White else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        title,
                        color = if (isSelected) TextPrimary else TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ============================================
// RECORDING INDICATOR BAR
// ============================================

@Composable
private fun RecordingIndicatorBar(
    isRecording: Boolean,
    isLive: Boolean,
    duration: Long,
    viewerCount: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_bar")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isLive) PodcastAccent.copy(alpha = pulse * 0.3f)
               else PodcastGreen.copy(alpha = pulse * 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (isLive) PodcastAccent else PodcastGreen,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isLive) "LIVE" else "RECORDING",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Text(
                formatDuration(duration),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            if (isLive && viewerCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "$viewerCount",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / 60000) % 60
    val hours = millis / 3600000
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

