package com.example.dwn.podcast.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.podcast.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// Theme colors
private val PodcastPrimary = Color(0xFF6C63FF)
private val PodcastSecondary = Color(0xFF00D9FF)
private val PodcastAccent = Color(0xFFFF6B6B)
private val PodcastGreen = Color(0xFF00E676)
private val PodcastOrange = Color(0xFFFF9800)
private val PodcastYellow = Color(0xFFFFEB3B)
private val PodcastSurface = Color(0xFF12121A)
private val PodcastSurfaceLight = Color(0xFF1A1A24)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB0B0C0)
private val TextTertiary = Color(0xFF6E6E80)

// ============================================
// RECORDING TAB
// ============================================

@Composable
fun RecordingTab(
    recordingState: RecordingState,
    remoteSession: RemoteSession?,
    recordings: List<File>,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCreateRemoteSession: () -> Unit,
    onEndRemoteSession: () -> Unit,
    onAddGuest: (String) -> Unit,
    onDeleteRecording: (String) -> Unit,
    onSelectRecording: (String) -> Unit
) {
    var showGuestDialog by remember { mutableStateOf(false) }
    var showRecordingsList by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Recording visualization
        RecordingVisualization(
            isRecording = recordingState.isRecording,
            isPaused = recordingState.isPaused,
            peakLevel = recordingState.peakLevel,
            rmsLevel = recordingState.rmsLevel
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Duration display
        Text(
            formatDuration(recordingState.duration),
            color = if (recordingState.isRecording && !recordingState.isPaused)
                PodcastAccent else TextPrimary,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status text
        Text(
            when {
                !recordingState.isRecording -> "Ready to Record"
                recordingState.isPaused -> "Paused"
                else -> "Recording..."
            },
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Level Meters
        LevelMetersDisplay(
            peakLevel = recordingState.peakLevel,
            rmsLevel = recordingState.rmsLevel,
            lufsLevel = recordingState.lufsLevel,
            isClipping = recordingState.isClipping
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Recording Controls
        RecordingControls(
            isRecording = recordingState.isRecording,
            isPaused = recordingState.isPaused,
            onStart = onStartRecording,
            onPause = onPauseRecording,
            onResume = onResumeRecording,
            onStop = onStopRecording
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Remote Session Section
        RemoteSessionSection(
            remoteSession = remoteSession,
            onCreateSession = onCreateRemoteSession,
            onEndSession = onEndRemoteSession,
            onAddGuest = { showGuestDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Track Controls
        TrackControlsSection()

        Spacer(modifier = Modifier.height(24.dp))

        // Recordings List Section
        RecordingsListSection(
            recordings = recordings,
            isExpanded = showRecordingsList,
            onToggleExpanded = { showRecordingsList = !showRecordingsList },
            onSelect = onSelectRecording,
            onDelete = onDeleteRecording
        )

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showGuestDialog) {
        AddGuestDialog(
            onDismiss = { showGuestDialog = false },
            onAdd = { name ->
                onAddGuest(name)
                showGuestDialog = false
            }
        )
    }
}

// ============================================
// RECORDINGS LIST SECTION
// ============================================

@Composable
private fun RecordingsListSection(
    recordings: List<File>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = PodcastSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Recordings",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PodcastSecondary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "${recordings.size}",
                            color = PodcastSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }

            // Recordings list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    if (recordings.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.MicOff,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No recordings yet",
                                    color = TextTertiary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Start recording to see files here",
                                    color = TextTertiary.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        recordings.forEach { file ->
                            RecordingItem(
                                file = file,
                                onSelect = { onSelect(file.absolutePath) },
                                onDelete = { onDelete(file.absolutePath) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingItem(
    file: File,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val fileSizeMB = remember(file) { "%.2f MB".format(file.length() / (1024.0 * 1024.0)) }
    val fileDate = remember(file) { dateFormat.format(Date(file.lastModified())) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        color = PodcastSurfaceLight
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(PodcastPrimary.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AudioFile,
                    contentDescription = null,
                    tint = PodcastPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text(
                        fileSizeMB,
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                    Text(
                        " • ",
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                    Text(
                        fileDate,
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                }
            }

            // Actions
            Row {
                IconButton(
                    onClick = onSelect,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = PodcastSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = PodcastAccent.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = PodcastSurface,
            title = { Text("Delete Recording?", color = TextPrimary) },
            text = {
                Text(
                    "This will permanently delete '${file.name}'. This action cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PodcastAccent)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun RecordingVisualization(
    isRecording: Boolean,
    isPaused: Boolean,
    peakLevel: Float,
    rmsLevel: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "viz")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording && !isPaused) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .graphicsLayer {
                scaleX = if (isRecording && !isPaused) pulseScale else 1f
                scaleY = if (isRecording && !isPaused) pulseScale else 1f
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 10

            // Background ring
            drawCircle(
                color = PodcastSurface,
                radius = radius,
                center = center,
                style = Stroke(width = 8.dp.toPx())
            )

            // Animated level ring
            if (isRecording) {
                val levelAngle = ((rmsLevel + 60) / 60 * 360).coerceIn(0f, 360f)
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(PodcastPrimary, PodcastSecondary, PodcastPrimary),
                        center = center
                    ),
                    startAngle = -90f + rotation,
                    sweepAngle = levelAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Waveform visualization
        if (isRecording && !isPaused) {
            WaveformCircle()
        }

        // Center mic icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = if (isRecording && !isPaused) {
                            listOf(PodcastAccent, PodcastAccent.copy(alpha = 0.7f))
                        } else {
                            listOf(PodcastPrimary, PodcastPrimary.copy(alpha = 0.7f))
                        }
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isRecording && isPaused) Icons.Default.Pause else Icons.Default.Mic,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun WaveformCircle() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = Modifier.size(160.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = size.minDimension / 2 - 30

        val points = (0 until 60).map { i ->
            val angle = (i * 6f) * PI.toFloat() / 180f
            val waveOffset = sin(angle * 4 + phase) * 10 +
                            sin(angle * 8 + phase * 2) * 5
            val radius = baseRadius + waveOffset
            Offset(
                center.x + cos(angle) * radius,
                center.y + sin(angle) * radius
            )
        }

        val path = Path()
        points.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.x, point.y)
            else path.lineTo(point.x, point.y)
        }
        path.close()

        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    PodcastPrimary.copy(alpha = 0.5f),
                    PodcastSecondary.copy(alpha = 0.5f)
                )
            ),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun LevelMetersDisplay(
    peakLevel: Float,
    rmsLevel: Float,
    lufsLevel: Float,
    isClipping: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Input Levels", color = TextPrimary, fontWeight = FontWeight.Medium)
                if (isClipping) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PodcastAccent
                    ) {
                        Text(
                            "CLIPPING!",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Level bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LevelMeterBar(
                    label = "L",
                    level = peakLevel,
                    rmsLevel = rmsLevel
                )
                LevelMeterBar(
                    label = "R",
                    level = peakLevel - 2,
                    rmsLevel = rmsLevel - 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // LUFS and Peak values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MeterValue(
                    label = "Peak",
                    value = "%.1f dB".format(peakLevel),
                    color = if (peakLevel > -6) PodcastAccent else PodcastGreen
                )
                MeterValue(
                    label = "RMS",
                    value = "%.1f dB".format(rmsLevel),
                    color = PodcastSecondary
                )
                MeterValue(
                    label = "LUFS",
                    value = "%.1f".format(lufsLevel),
                    color = PodcastPrimary
                )
            }
        }
    }
}

@Composable
private fun LevelMeterBar(
    label: String,
    level: Float,
    rmsLevel: Float
) {
    val normalizedLevel = ((level + 60) / 60).coerceIn(0f, 1f)
    val normalizedRms = ((rmsLevel + 60) / 60).coerceIn(0f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(30.dp)
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(PodcastSurfaceLight)
        ) {
            // RMS fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(normalizedRms)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PodcastAccent, PodcastOrange, PodcastGreen),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Peak indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = (-normalizedLevel * 100).dp)
                    .background(Color.White)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun MeterValue(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextTertiary, fontSize = 10.sp)
        Text(
            value,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RecordingControls(
    isRecording: Boolean,
    isPaused: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isRecording) {
            // Stop button
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(56.dp)
                    .background(PodcastSurface, CircleShape)
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = PodcastAccent,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Pause/Resume button
            Button(
                onClick = if (isPaused) onResume else onPause,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) PodcastGreen else PodcastOrange
                )
            ) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            // Start recording button
            Button(
                onClick = onStart,
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = PodcastAccent)
            ) {
                Icon(
                    Icons.Default.FiberManualRecord,
                    contentDescription = "Start Recording",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
private fun RemoteSessionSection(
    remoteSession: RemoteSession?,
    onCreateSession: () -> Unit,
    onEndSession: () -> Unit,
    onAddGuest: () -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = PodcastSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Remote Guests",
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (remoteSession != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PodcastGreen.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(PodcastGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                remoteSession.sessionCode,
                                color = PodcastGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (remoteSession == null) {
                Button(
                    onClick = onCreateSession,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PodcastPrimary)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Remote Session")
                }
            } else {
                // Guest list
                if (remoteSession.guests.isEmpty()) {
                    Text(
                        "No guests connected yet. Share the code above to invite guests.",
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                } else {
                    remoteSession.guests.forEach { guest ->
                        GuestItem(guest = guest)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddGuest,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PodcastSecondary
                        )
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Guest")
                    }

                    OutlinedButton(
                        onClick = onEndSession,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PodcastAccent
                        )
                    ) {
                        Icon(Icons.Default.CallEnd, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("End Session")
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestItem(guest: RemoteGuest) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = PodcastSurfaceLight
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PodcastSecondary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    guest.name.first().toString(),
                    color = PodcastSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(guest.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (guest.isConnected) PodcastGreen else PodcastOrange,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (guest.isConnected) "Connected" else "Connecting...",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                    if (guest.latency > 0) {
                        Text(
                            " • ${guest.latency}ms",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Mute button
            IconButton(
                onClick = { /* Toggle mute */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (guest.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (guest.isMuted) PodcastAccent else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TrackControlsSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Audio Tracks",
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            TrackItem(name = "Host Mic", type = TrackType.HOST, color = PodcastPrimary)
            TrackItem(name = "System Audio", type = TrackType.SYSTEM_AUDIO, color = PodcastSecondary)

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Track")
            }
        }
    }
}

@Composable
private fun TrackItem(
    name: String,
    type: TrackType,
    color: Color
) {
    var isMuted by remember { mutableStateOf(false) }
    var isSolo by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(0.8f) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = PodcastSurfaceLight
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                name,
                color = if (isMuted) TextTertiary else TextPrimary,
                modifier = Modifier.weight(1f)
            )

            // Mute button
            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier.size(28.dp)
            ) {
                Text(
                    "M",
                    color = if (isMuted) PodcastAccent else TextTertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Solo button
            IconButton(
                onClick = { isSolo = !isSolo },
                modifier = Modifier.size(28.dp)
            ) {
                Text(
                    "S",
                    color = if (isSolo) PodcastYellow else TextTertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Volume slider
            Slider(
                value = volume,
                onValueChange = { volume = it },
                modifier = Modifier.width(80.dp),
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color
                )
            )
        }
    }
}

@Composable
private fun AddGuestDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PodcastSurface,
        title = { Text("Add Guest", color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Guest Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PodcastSecondary,
                    unfocusedBorderColor = TextTertiary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PodcastSecondary)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / 60000) % 60
    val hours = millis / 3600000
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

