package com.example.dwn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ============================================
// 🎨 THEME COLORS
// ============================================

private object EditorColors {
    val pink = Color(0xFFE91E63)
    val purple = Color(0xFF9C27B0)
    val blue = Color(0xFF2196F3)
    val cyan = Color(0xFF00BCD4)
    val teal = Color(0xFF009688)
    val orange = Color(0xFFFF5722)
    val amber = Color(0xFFFFC107)

    val bgDark = Color(0xFF0D0D0D)
    val bgMid = Color(0xFF1A1A1A)
    val surface = Color(0xFF1E1E1E)
    val surfaceVariant = Color(0xFF252525)
    val card = Color(0xFF2A2A2A)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B0)
    val textTertiary = Color(0xFF707070)

    val success = Color(0xFF4CAF50)
    val error = Color(0xFFE53935)

    val glassWhite = Color(0x14FFFFFF)
    val glassBorder = Color(0x20FFFFFF)
}

// ============================================
// 📹 SCREEN RECORDER SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRecorderScreen(
    onBack: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableLongStateOf(0L) }
    var showSettings by remember { mutableStateOf(false) }

    // Recording settings
    var recordAudio by remember { mutableStateOf(true) }
    var recordMic by remember { mutableStateOf(false) }
    var showTouches by remember { mutableStateOf(true) }
    var selectedQuality by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableStateOf("30") }

    // Timer effect
    LaunchedEffect(isRecording, isPaused) {
        while (isRecording && !isPaused) {
            delay(1000)
            recordingTime += 1000
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorColors.bgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        "Screen Recorder",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = EditorColors.textPrimary,
                    navigationIconContentColor = EditorColors.textPrimary,
                    actionIconContentColor = EditorColors.textPrimary
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Recording Preview/Status
                RecordingPreviewCard(
                    isRecording = isRecording,
                    isPaused = isPaused,
                    recordingTime = recordingTime
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Recording Controls
                RecordingControls(
                    isRecording = isRecording,
                    isPaused = isPaused,
                    onStartStop = {
                        if (isRecording) {
                            isRecording = false
                            isPaused = false
                            // Save recording
                        } else {
                            isRecording = true
                            recordingTime = 0
                        }
                    },
                    onPauseResume = { isPaused = !isPaused }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Quick Settings
                QuickSettingsSection(
                    recordAudio = recordAudio,
                    onRecordAudioChange = { recordAudio = it },
                    recordMic = recordMic,
                    onRecordMicChange = { recordMic = it },
                    showTouches = showTouches,
                    onShowTouchesChange = { showTouches = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Quality Settings
                QualitySettingsSection(
                    selectedQuality = selectedQuality,
                    onQualityChange = { selectedQuality = it },
                    selectedFps = selectedFps,
                    onFpsChange = { selectedFps = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Recent Recordings
                RecentRecordingsSection()

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun RecordingPreviewCard(
    isRecording: Boolean,
    isPaused: Boolean,
    recordingTime: Long
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording && !isPaused) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(24.dp),
        color = EditorColors.surface
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Recording indicator
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(pulse)
                        .background(
                            if (isRecording) {
                                if (isPaused) EditorColors.amber else EditorColors.error
                            } else EditorColors.surfaceVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isRecording) {
                            if (isPaused) Icons.Default.Pause else Icons.Default.FiberManualRecord
                        } else Icons.Default.Screenshot,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timer
                Text(
                    formatTime(recordingTime),
                    color = EditorColors.textPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    when {
                        isRecording && !isPaused -> "Recording..."
                        isRecording && isPaused -> "Paused"
                        else -> "Ready to record"
                    },
                    color = EditorColors.textSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun RecordingControls(
    isRecording: Boolean,
    isPaused: Boolean,
    onStartStop: () -> Unit,
    onPauseResume: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pause/Resume button (only when recording)
        AnimatedVisibility(visible = isRecording) {
            FloatingActionButton(
                onClick = onPauseResume,
                containerColor = EditorColors.amber,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    tint = Color.White
                )
            }
        }

        // Main record/stop button
        FloatingActionButton(
            onClick = onStartStop,
            containerColor = if (isRecording) EditorColors.error else EditorColors.pink,
            modifier = Modifier.size(72.dp)
        ) {
            Icon(
                if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                contentDescription = if (isRecording) "Stop" else "Start",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // Screenshot button
        FloatingActionButton(
            onClick = { /* Take screenshot */ },
            containerColor = EditorColors.surfaceVariant,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                Icons.Default.Screenshot,
                contentDescription = "Screenshot",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun QuickSettingsSection(
    recordAudio: Boolean,
    onRecordAudioChange: (Boolean) -> Unit,
    recordMic: Boolean,
    onRecordMicChange: (Boolean) -> Unit,
    showTouches: Boolean,
    onShowTouchesChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = EditorColors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Quick Settings",
                color = EditorColors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingToggle(
                icon = Icons.Default.VolumeUp,
                title = "System Audio",
                subtitle = "Record device audio",
                checked = recordAudio,
                onCheckedChange = onRecordAudioChange
            )

            SettingToggle(
                icon = Icons.Default.Mic,
                title = "Microphone",
                subtitle = "Record from mic",
                checked = recordMic,
                onCheckedChange = onRecordMicChange
            )

            SettingToggle(
                icon = Icons.Default.TouchApp,
                title = "Show Touches",
                subtitle = "Display tap indicators",
                checked = showTouches,
                onCheckedChange = onShowTouchesChange
            )
        }
    }
}

@Composable
private fun SettingToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = EditorColors.textSecondary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = EditorColors.textPrimary, fontSize = 14.sp)
            Text(subtitle, color = EditorColors.textTertiary, fontSize = 12.sp)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EditorColors.pink,
                checkedTrackColor = EditorColors.pink.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun QualitySettingsSection(
    selectedQuality: String,
    onQualityChange: (String) -> Unit,
    selectedFps: String,
    onFpsChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = EditorColors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Quality",
                color = EditorColors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Resolution
            Text("Resolution", color = EditorColors.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("720p", "1080p", "1440p").forEach { quality ->
                    FilterChip(
                        selected = selectedQuality == quality,
                        onClick = { onQualityChange(quality) },
                        label = { Text(quality) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EditorColors.pink,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frame rate
            Text("Frame Rate", color = EditorColors.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("24", "30", "60").forEach { fps ->
                    FilterChip(
                        selected = selectedFps == fps,
                        onClick = { onFpsChange(fps) },
                        label = { Text("${fps}fps") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EditorColors.pink,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentRecordingsSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = EditorColors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Recordings",
                    color = EditorColors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                TextButton(onClick = { }) {
                    Text("View All", color = EditorColors.pink)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        tint = EditorColors.textTertiary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No recordings yet",
                        color = EditorColors.textTertiary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / 60000) % 60
    val hours = millis / 3600000
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

