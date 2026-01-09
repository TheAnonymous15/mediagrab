package com.example.dwn.podcast.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.podcast.*

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
// EDITING TAB
// ============================================

@Composable
fun EditingTab(
    currentEpisode: PodcastEpisode?,
    onUpdateEpisode: (PodcastEpisode) -> Unit
) {
    var showChapterDialog by remember { mutableStateOf(false) }
    var showMarkerDialog by remember { mutableStateOf(false) }
    var playheadPosition by remember { mutableFloatStateOf(0.3f) }
    var zoom by remember { mutableFloatStateOf(1f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Toolbar
        EditingToolbar(
            zoom = zoom,
            onZoomChange = { zoom = it },
            onUndo = { },
            onRedo = { }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Waveform Timeline
        TimelineEditor(
            duration = currentEpisode?.duration ?: 1800000,
            playheadPosition = playheadPosition,
            onPlayheadChange = { playheadPosition = it },
            chapters = currentEpisode?.chapters ?: emptyList(),
            markers = currentEpisode?.markers ?: emptyList(),
            zoom = zoom
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tracks Panel
        TracksPanel(
            tracks = currentEpisode?.tracks ?: listOf(
                AudioTrack(name = "Host", type = TrackType.HOST),
                AudioTrack(name = "Guest", type = TrackType.GUEST),
                AudioTrack(name = "Music Bed", type = TrackType.MUSIC_BED)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Chapters & Markers
        ChaptersMarkersPanel(
            chapters = currentEpisode?.chapters ?: emptyList(),
            markers = currentEpisode?.markers ?: emptyList(),
            onAddChapter = { showChapterDialog = true },
            onAddMarker = { showMarkerDialog = true },
            currentTime = (playheadPosition * (currentEpisode?.duration ?: 0)).toLong()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Effects Panel
        EffectsPanel()
    }

    if (showChapterDialog) {
        AddChapterDialog(
            currentTime = (playheadPosition * (currentEpisode?.duration ?: 0)).toLong(),
            onDismiss = { showChapterDialog = false },
            onAdd = { title, time ->
                currentEpisode?.let { episode ->
                    val newChapter = Chapter(title = title, startTime = time)
                    onUpdateEpisode(episode.copy(chapters = episode.chapters + newChapter))
                }
                showChapterDialog = false
            }
        )
    }

    if (showMarkerDialog) {
        AddMarkerDialog(
            currentTime = (playheadPosition * (currentEpisode?.duration ?: 0)).toLong(),
            onDismiss = { showMarkerDialog = false },
            onAdd = { type, label, time ->
                currentEpisode?.let { episode ->
                    val newMarker = Marker(type = type, timestamp = time, label = label)
                    onUpdateEpisode(episode.copy(markers = episode.markers + newMarker))
                }
                showMarkerDialog = false
            }
        )
    }
}

@Composable
private fun EditingToolbar(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PodcastSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playback controls
            Row {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.SkipPrevious, null, tint = TextSecondary)
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.PlayArrow, null, tint = PodcastPrimary)
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.SkipNext, null, tint = TextSecondary)
                }
            }

            // Edit tools
            Row {
                ToolButton(icon = Icons.Default.ContentCut, label = "Cut")
                ToolButton(icon = Icons.Default.ContentCopy, label = "Copy")
                ToolButton(icon = Icons.Default.ContentPaste, label = "Paste")
                ToolButton(icon = Icons.Default.Delete, label = "Delete")
            }

            // Undo/Redo
            Row {
                IconButton(onClick = onUndo) {
                    Icon(Icons.Default.Undo, null, tint = TextSecondary)
                }
                IconButton(onClick = onRedo) {
                    Icon(Icons.Default.Redo, null, tint = TextSecondary)
                }
            }

            // Zoom
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onZoomChange((zoom - 0.25f).coerceAtLeast(0.5f)) }) {
                    Icon(Icons.Default.ZoomOut, null, tint = TextSecondary)
                }
                Text(
                    "${(zoom * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                IconButton(onClick = { onZoomChange((zoom + 0.25f).coerceAtMost(4f)) }) {
                    Icon(Icons.Default.ZoomIn, null, tint = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    IconButton(onClick = { }) {
        Icon(icon, label, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun TimelineEditor(
    duration: Long,
    playheadPosition: Float,
    onPlayheadChange: (Float) -> Unit,
    chapters: List<Chapter>,
    markers: List<Marker>,
    zoom: Float
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        color = PodcastSurface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Time ruler
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            ) {
                val tickCount = (size.width / (50 * zoom)).toInt()
                val tickSpacing = size.width / tickCount

                for (i in 0..tickCount) {
                    val x = i * tickSpacing
                    drawLine(
                        color = TextTertiary,
                        start = Offset(x, size.height - 8),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                }
            }

            // Waveform
            WaveformDisplay(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(top = 20.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val newPos = (change.position.x / size.width).coerceIn(0f, 1f)
                            onPlayheadChange(newPos)
                        }
                    }
            )

            // Chapter markers
            chapters.forEach { chapter ->
                val position = chapter.startTime.toFloat() / duration
                ChapterMarker(
                    position = position,
                    title = chapter.title,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            // Playhead
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .offset(x = (playheadPosition * 300).dp) // Simplified
                    .background(PodcastAccent)
            )

            // Playhead handle
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .offset(x = (playheadPosition * 300 - 6).dp)
                    .background(PodcastAccent, CircleShape)
                    .align(Alignment.TopStart)
            )
        }
    }
}

@Composable
private fun WaveformDisplay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Canvas(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(PodcastSurfaceLight)) {
        val barCount = 100
        val barWidth = size.width / barCount

        for (i in 0 until barCount) {
            val height = (0.2f + kotlin.math.sin((i + offset * 10) * 0.3f).toFloat().coerceIn(0f, 1f) * 0.8f) * size.height

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(PodcastPrimary, PodcastSecondary)
                ),
                topLeft = Offset(i * barWidth + 1, (size.height - height) / 2),
                size = androidx.compose.ui.geometry.Size(barWidth - 2, height)
            )
        }
    }
}

@Composable
private fun ChapterMarker(
    position: Float,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.offset(x = (position * 300).dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(PodcastOrange, CircleShape)
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(100.dp)
                .background(PodcastOrange.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun TracksPanel(tracks: List<AudioTrack>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tracks", color = TextPrimary, fontWeight = FontWeight.Medium)
                TextButton(onClick = { }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Text("Add Track")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            tracks.forEachIndexed { index, track ->
                TrackRow(
                    track = track,
                    color = when (index % 4) {
                        0 -> PodcastPrimary
                        1 -> PodcastSecondary
                        2 -> PodcastGreen
                        else -> PodcastOrange
                    }
                )
            }
        }
    }
}

@Composable
private fun TrackRow(track: AudioTrack, color: Color) {
    var isMuted by remember { mutableStateOf(track.isMuted) }
    var isSolo by remember { mutableStateOf(track.isSolo) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(PodcastSurfaceLight, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            track.name,
            color = if (isMuted) TextTertiary else TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier.width(80.dp)
        )

        // Mini waveform
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PodcastSurface)
        ) {
            val bars = 40
            for (i in 0 until bars) {
                val height = (kotlin.random.Random.nextFloat() * 0.8f + 0.2f) * size.height
                drawRect(
                    color = color.copy(alpha = if (isMuted) 0.2f else 0.6f),
                    topLeft = Offset(i * (size.width / bars), (size.height - height) / 2),
                    size = androidx.compose.ui.geometry.Size(size.width / bars - 1, height)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Mute/Solo
        IconButton(
            onClick = { isMuted = !isMuted },
            modifier = Modifier.size(24.dp)
        ) {
            Text(
                "M",
                color = if (isMuted) PodcastAccent else TextTertiary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }

        IconButton(
            onClick = { isSolo = !isSolo },
            modifier = Modifier.size(24.dp)
        ) {
            Text(
                "S",
                color = if (isSolo) PodcastYellow else TextTertiary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }

        // FX button
        IconButton(
            onClick = { },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Tune,
                null,
                tint = TextTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ChaptersMarkersPanel(
    chapters: List<Chapter>,
    markers: List<Marker>,
    onAddChapter: () -> Unit,
    onAddMarker: () -> Unit,
    currentTime: Long
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.width(200.dp),
                    containerColor = Color.Transparent,
                    contentColor = PodcastPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Chapters", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Markers", fontSize = 12.sp) }
                    )
                }

                IconButton(onClick = if (selectedTab == 0) onAddChapter else onAddMarker) {
                    Icon(Icons.Default.Add, null, tint = PodcastPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> {
                    if (chapters.isEmpty()) {
                        Text(
                            "No chapters yet. Add chapters to help listeners navigate.",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    } else {
                        chapters.forEach { chapter ->
                            ChapterItem(chapter = chapter)
                        }
                    }
                }
                1 -> {
                    if (markers.isEmpty()) {
                        Text(
                            "No markers yet. Add markers for ad slots, highlights, etc.",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    } else {
                        markers.forEach { marker ->
                            MarkerItem(marker = marker)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterItem(chapter: Chapter) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Bookmark,
            null,
            tint = PodcastOrange,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            formatTime(chapter.startTime),
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            chapter.title,
            color = TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { }, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Edit, null, tint = TextTertiary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun MarkerItem(marker: Marker) {
    val color = when (marker.type) {
        MarkerType.AD_SLOT -> PodcastGreen
        MarkerType.HIGHLIGHT -> PodcastYellow
        MarkerType.CUT -> PodcastAccent
        MarkerType.SPONSOR -> PodcastPink
        else -> PodcastSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            formatTime(marker.timestamp),
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            marker.type.name.replace("_", " "),
            color = color,
            fontSize = 11.sp
        )
        if (marker.label.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                marker.label,
                color = TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EffectsPanel() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Quick Effects", color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf(
                        Triple(Icons.Default.VolumeUp, "Normalize", PodcastPrimary),
                        Triple(Icons.Default.NoiseAware, "Noise Rem.", PodcastSecondary),
                        Triple(Icons.Default.Compress, "Compress", PodcastOrange),
                        Triple(Icons.Default.Equalizer, "EQ", PodcastGreen),
                        Triple(Icons.Default.GraphicEq, "De-ess", PodcastPink)
                    )
                ) { (icon, label, color) ->
                    EffectChip(icon = icon, label = label, color = color)
                }
            }
        }
    }
}

@Composable
private fun EffectChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.clickable { }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = color, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AddChapterDialog(
    currentTime: Long,
    onDismiss: () -> Unit,
    onAdd: (String, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PodcastSurface,
        title = { Text("Add Chapter", color = TextPrimary) },
        text = {
            Column {
                Text(
                    "Time: ${formatTime(currentTime)}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Chapter Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PodcastOrange,
                        unfocusedBorderColor = TextTertiary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(title, currentTime) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PodcastOrange)
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

@Composable
private fun AddMarkerDialog(
    currentTime: Long,
    onDismiss: () -> Unit,
    onAdd: (MarkerType, String, Long) -> Unit
) {
    var selectedType by remember { mutableStateOf(MarkerType.NOTE) }
    var label by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PodcastSurface,
        title = { Text("Add Marker", color = TextPrimary) },
        text = {
            Column {
                Text(
                    "Time: ${formatTime(currentTime)}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Type", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(MarkerType.entries) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name.replace("_", " "), fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PodcastPrimary,
                        unfocusedBorderColor = TextTertiary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(selectedType, label, currentTime) },
                colors = ButtonDefaults.buttonColors(containerColor = PodcastPrimary)
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

private fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / 60000) % 60
    val hours = millis / 3600000
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

