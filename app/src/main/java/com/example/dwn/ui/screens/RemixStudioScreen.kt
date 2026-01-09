package com.example.dwn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.remix.*
import kotlin.math.sin
import kotlin.math.roundToInt

// ============================================
// 🎨 REMIX STUDIO COLORS
// ============================================

private object RemixColors {
    val pink = Color(0xFFE91E63)
    val pinkLight = Color(0xFFFF4081)
    val purple = Color(0xFF9C27B0)
    val blue = Color(0xFF2196F3)
    val cyan = Color(0xFF00BCD4)
    val green = Color(0xFF4CAF50)
    val orange = Color(0xFFFF5722)
    val amber = Color(0xFFFFC107)
    val red = Color(0xFFE53935)

    val bgDark = Color(0xFF0A0A0F)
    val bgMid = Color(0xFF101018)
    val surface = Color(0xFF161620)
    val surfaceVariant = Color(0xFF1E1E2A)
    val card = Color(0xFF222230)
    val timeline = Color(0xFF1A1A24)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B8)
    val textTertiary = Color(0xFF707080)

    val glassWhite = Color(0x14FFFFFF)
    val glassBorder = Color(0x20FFFFFF)

    val waveform = Color(0xFFE91E63)
    val waveformBg = Color(0xFF3D1F2F)
    val selection = Color(0x40E91E63)
    val highlight = Color(0xFFFFC107)
    val playhead = Color(0xFFFFFFFF)
}

// ============================================
// 🎚️ REMIX STUDIO SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemixStudioScreen(
    onBack: () -> Unit
) {
    // Project state
    var project by remember {
        mutableStateOf(
            RemixProject(
                name = "My Remix",
                sourceUri = "content://media/audio/123",
                sourceType = MediaSourceType.AUDIO_FILE,
                sourceDuration = 180000L,  // 3 minutes
                hasVideo = false
            )
        )
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var selectionStart by remember { mutableStateOf<Long?>(null) }
    var selectionEnd by remember { mutableStateOf<Long?>(null) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showProcessingSheet by remember { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf("clip") }

    // Mock waveform data
    val waveformSamples = remember {
        List(300) { i ->
            (0.2f + 0.6f * kotlin.math.abs(sin(i * 0.1f + i * 0.02f))).coerceIn(0.1f, 1f)
        }
    }

    // Mock detected highlights
    val highlights = remember {
        listOf(
            DetectedHighlight(
                startTime = 15000L,
                endTime = 25000L,
                type = HighlightType.SPEECH_EMPHASIS,
                confidence = 0.92f,
                description = "Key moment"
            ),
            DetectedHighlight(
                startTime = 65000L,
                endTime = 80000L,
                type = HighlightType.MUSIC_DROP,
                confidence = 0.88f,
                description = "Beat drop"
            ),
            DetectedHighlight(
                startTime = 120000L,
                endTime = 135000L,
                type = HighlightType.EMOTION_PEAK,
                confidence = 0.85f,
                description = "Emotional peak"
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RemixColors.bgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            RemixTopBar(
                projectName = project.name,
                onBack = onBack,
                onExport = { showExportSheet = true }
            )

            // Main Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Source Info Card
                SourceInfoCard(project = project)

                Spacer(modifier = Modifier.height(16.dp))

                // Waveform Display with Selection
                WaveformEditor(
                    samples = waveformSamples,
                    duration = project.sourceDuration,
                    currentPosition = currentPosition,
                    selectionStart = selectionStart,
                    selectionEnd = selectionEnd,
                    highlights = highlights,
                    isPlaying = isPlaying,
                    onPositionChange = { currentPosition = it },
                    onSelectionChange = { start, end ->
                        selectionStart = start
                        selectionEnd = end
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Playback Controls
                PlaybackControls(
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = project.sourceDuration,
                    onPlayPause = { isPlaying = !isPlaying },
                    onSeek = { currentPosition = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Tool Tabs
                ToolSelector(
                    selectedTool = selectedTool,
                    onToolSelect = { selectedTool = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tool Content
                when (selectedTool) {
                    "clip" -> ClipToolContent(
                        selectionStart = selectionStart,
                        selectionEnd = selectionEnd,
                        duration = project.sourceDuration,
                        onCreateClip = { /* Create clip */ }
                    )
                    "highlights" -> HighlightsToolContent(
                        highlights = highlights,
                        onSelectHighlight = { highlight ->
                            selectionStart = highlight.startTime
                            selectionEnd = highlight.endTime
                            currentPosition = highlight.startTime
                        }
                    )
                    "audio" -> AudioToolContent(
                        onOpenProcessing = { showProcessingSheet = true }
                    )
                    "video" -> VideoToolContent()
                    "captions" -> CaptionsToolContent()
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // Bottom Action Bar
            BottomActionBar(
                hasSelection = selectionStart != null && selectionEnd != null,
                onQuickClip = { /* Quick clip */ },
                onAddToProject = { /* Add clip to project */ }
            )
        }
    }

    // Export Sheet
    if (showExportSheet) {
        ExportBottomSheet(
            project = project,
            onDismiss = { showExportSheet = false },
            onExport = { config ->
                showExportSheet = false
                // Start export
            }
        )
    }

    // Processing Sheet
    if (showProcessingSheet) {
        AudioProcessingSheet(
            onDismiss = { showProcessingSheet = false }
        )
    }
}

// ============================================
// 📱 TOP BAR
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemixTopBar(
    projectName: String,
    onBack: () -> Unit,
    onExport: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Remix Studio",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    projectName,
                    color = RemixColors.textTertiary,
                    fontSize = 12.sp
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
        },
        actions = {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Undo, "Undo")
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.Redo, "Redo")
            }
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RemixColors.pink
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = RemixColors.textPrimary,
            navigationIconContentColor = RemixColors.textSecondary,
            actionIconContentColor = RemixColors.textSecondary
        )
    )
}

// ============================================
// 📁 SOURCE INFO CARD
// ============================================

@Composable
private fun SourceInfoCard(project: RemixProject) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = RemixColors.card,
        border = BorderStroke(1.dp, RemixColors.glassBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (project.hasVideo) RemixColors.purple.copy(alpha = 0.2f)
                        else RemixColors.pink.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (project.hasVideo) Icons.Default.Videocam else Icons.Default.MusicNote,
                    null,
                    tint = if (project.hasVideo) RemixColors.purple else RemixColors.pink,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.name,
                    color = RemixColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${formatDuration(project.sourceDuration)} • ${project.sourceType.name.replace("_", " ")}",
                    color = RemixColors.textTertiary,
                    fontSize = 12.sp
                )
            }

            // Action chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionChip(
                    icon = Icons.Default.FolderOpen,
                    onClick = { }
                )
                ActionChip(
                    icon = Icons.Default.MoreVert,
                    onClick = { }
                )
            }
        }
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = RemixColors.surfaceVariant,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = RemixColors.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

// ============================================
// 🌊 WAVEFORM EDITOR
// ============================================

@Composable
private fun WaveformEditor(
    samples: List<Float>,
    duration: Long,
    currentPosition: Long,
    selectionStart: Long?,
    selectionEnd: Long?,
    highlights: List<DetectedHighlight>,
    isPlaying: Boolean,
    onPositionChange: (Long) -> Unit,
    onSelectionChange: (Long?, Long?) -> Unit
) {
    val density = LocalDensity.current

    val transition = rememberInfiniteTransition(label = "wave")
    val playheadGlow by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = RemixColors.timeline
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newPos = (offset.x / size.width * duration).toLong()
                        onPositionChange(newPos.coerceIn(0, duration))
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            ) {
                val width = size.width
                val height = size.height
                val centerY = height / 2

                // Draw highlight regions
                highlights.forEach { highlight ->
                    val startX = (highlight.startTime.toFloat() / duration) * width
                    val endX = (highlight.endTime.toFloat() / duration) * width

                    drawRect(
                        color = RemixColors.highlight.copy(alpha = 0.15f),
                        topLeft = Offset(startX, 0f),
                        size = androidx.compose.ui.geometry.Size(endX - startX, height)
                    )

                    // Highlight indicator at top
                    drawRoundRect(
                        color = RemixColors.highlight,
                        topLeft = Offset(startX, 0f),
                        size = androidx.compose.ui.geometry.Size(endX - startX, 4.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                    )
                }

                // Draw selection region
                if (selectionStart != null && selectionEnd != null) {
                    val startX = (selectionStart.toFloat() / duration) * width
                    val endX = (selectionEnd.toFloat() / duration) * width

                    drawRect(
                        color = RemixColors.selection,
                        topLeft = Offset(startX, 0f),
                        size = androidx.compose.ui.geometry.Size(endX - startX, height)
                    )

                    // Selection handles
                    drawLine(
                        color = RemixColors.pink,
                        start = Offset(startX, 0f),
                        end = Offset(startX, height),
                        strokeWidth = 3.dp.toPx()
                    )
                    drawLine(
                        color = RemixColors.pink,
                        start = Offset(endX, 0f),
                        end = Offset(endX, height),
                        strokeWidth = 3.dp.toPx()
                    )
                }

                // Draw waveform
                val barWidth = width / samples.size
                samples.forEachIndexed { index, amplitude ->
                    val x = index * barWidth
                    val barHeight = height * amplitude * 0.8f
                    val samplePos = (index.toFloat() / samples.size) * duration

                    val isInSelection = selectionStart != null && selectionEnd != null &&
                            samplePos >= selectionStart && samplePos <= selectionEnd

                    val isPlayed = samplePos <= currentPosition

                    val color = when {
                        isInSelection -> RemixColors.pink
                        isPlayed -> RemixColors.waveform.copy(alpha = 0.9f)
                        else -> RemixColors.waveformBg
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, centerY - barHeight / 2),
                        size = androidx.compose.ui.geometry.Size(barWidth - 1.dp.toPx(), barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                    )
                }

                // Draw playhead
                val playheadX = (currentPosition.toFloat() / duration) * width

                // Playhead glow
                if (isPlaying) {
                    drawLine(
                        color = RemixColors.playhead.copy(alpha = playheadGlow * 0.3f),
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, height),
                        strokeWidth = 8.dp.toPx()
                    )
                }

                // Playhead line
                drawLine(
                    color = RemixColors.playhead,
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, height),
                    strokeWidth = 2.dp.toPx()
                )

                // Playhead handle
                drawCircle(
                    color = RemixColors.playhead,
                    radius = 6.dp.toPx(),
                    center = Offset(playheadX, 0f)
                )
            }

            // Time labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0:00", color = RemixColors.textTertiary, fontSize = 10.sp)
                Text(formatDuration(duration / 2), color = RemixColors.textTertiary, fontSize = 10.sp)
                Text(formatDuration(duration), color = RemixColors.textTertiary, fontSize = 10.sp)
            }
        }
    }
}

// ============================================
// ▶️ PLAYBACK CONTROLS
// ============================================

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Time display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatDuration(currentPosition),
                color = RemixColors.pink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "-${formatDuration(duration - currentPosition)}",
                color = RemixColors.textTertiary,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onSeek(0) }) {
                Icon(
                    Icons.Default.SkipPrevious,
                    "Start",
                    tint = RemixColors.textSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = { onSeek((currentPosition - 5000).coerceAtLeast(0)) }) {
                Icon(
                    Icons.Default.Replay5,
                    "Back 5s",
                    tint = RemixColors.textSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Play/Pause button
            FloatingActionButton(
                onClick = onPlayPause,
                containerColor = RemixColors.pink,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(onClick = { onSeek((currentPosition + 5000).coerceAtMost(duration)) }) {
                Icon(
                    Icons.Default.Forward5,
                    "Forward 5s",
                    tint = RemixColors.textSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = { onSeek(duration) }) {
                Icon(
                    Icons.Default.SkipNext,
                    "End",
                    tint = RemixColors.textSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ============================================
// 🛠️ TOOL SELECTOR
// ============================================

@Composable
private fun ToolSelector(
    selectedTool: String,
    onToolSelect: (String) -> Unit
) {
    val tools = listOf(
        Triple("clip", Icons.Default.ContentCut, "Clip"),
        Triple("highlights", Icons.Default.AutoAwesome, "AI Highlights"),
        Triple("audio", Icons.Default.GraphicEq, "Audio"),
        Triple("video", Icons.Default.Videocam, "Video"),
        Triple("captions", Icons.Default.ClosedCaption, "Captions")
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tools) { (id, icon, label) ->
            val isSelected = selectedTool == id

            Surface(
                onClick = { onToolSelect(id) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) RemixColors.pink else RemixColors.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        label,
                        tint = if (isSelected) Color.White else RemixColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        label,
                        color = if (isSelected) Color.White else RemixColors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ============================================
// ✂️ CLIP TOOL CONTENT
// ============================================

@Composable
private fun ClipToolContent(
    selectionStart: Long?,
    selectionEnd: Long?,
    duration: Long,
    onCreateClip: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = RemixColors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Smart Clipping",
                color = RemixColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectionStart != null && selectionEnd != null) {
                // Selection info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RemixColors.pink.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Selection", color = RemixColors.textTertiary, fontSize = 11.sp)
                        Text(
                            "${formatDuration(selectionStart)} - ${formatDuration(selectionEnd)}",
                            color = RemixColors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Duration", color = RemixColors.textTertiary, fontSize = 11.sp)
                        Text(
                            formatDuration(selectionEnd - selectionStart),
                            color = RemixColors.pink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickOptionChip(
                        icon = Icons.Default.AutoFixHigh,
                        label = "Auto Trim",
                        isActive = true,
                        modifier = Modifier.weight(1f)
                    )
                    QuickOptionChip(
                        icon = Icons.Default.Waves,
                        label = "Fade",
                        isActive = true,
                        modifier = Modifier.weight(1f)
                    )
                    QuickOptionChip(
                        icon = Icons.Default.VolumeUp,
                        label = "Normalize",
                        isActive = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onCreateClip,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RemixColors.pink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Clip", fontWeight = FontWeight.SemiBold)
                }
            } else {
                // No selection
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.TouchApp,
                            null,
                            tint = RemixColors.textTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap and drag on the waveform to select a region",
                            color = RemixColors.textTertiary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickOptionChip(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) RemixColors.green.copy(alpha = 0.15f) else RemixColors.surfaceVariant,
        border = if (isActive) BorderStroke(1.dp, RemixColors.green.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = if (isActive) RemixColors.green else RemixColors.textTertiary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                label,
                color = if (isActive) RemixColors.green else RemixColors.textTertiary,
                fontSize = 11.sp
            )
        }
    }
}

// ============================================
// ✨ HIGHLIGHTS TOOL CONTENT
// ============================================

@Composable
private fun HighlightsToolContent(
    highlights: List<DetectedHighlight>,
    onSelectHighlight: (DetectedHighlight) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = RemixColors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "AI-Detected Highlights",
                    color = RemixColors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = RemixColors.highlight.copy(alpha = 0.2f)
                ) {
                    Text(
                        "${highlights.size} found",
                        color = RemixColors.highlight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            highlights.forEachIndexed { index, highlight ->
                HighlightItem(
                    highlight = highlight,
                    onClick = { onSelectHighlight(highlight) }
                )
                if (index < highlights.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, RemixColors.pink.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Re-analyze", color = RemixColors.pink)
            }
        }
    }
}

@Composable
private fun HighlightItem(
    highlight: DetectedHighlight,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = RemixColors.surfaceVariant,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(RemixColors.highlight.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (highlight.type) {
                        HighlightType.SPEECH_EMPHASIS -> "🎤"
                        HighlightType.MUSIC_DROP -> "🎵"
                        HighlightType.LAUGHTER -> "😂"
                        HighlightType.APPLAUSE -> "👏"
                        HighlightType.EMOTION_PEAK -> "❤️"
                        else -> "✨"
                    },
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    highlight.description.ifEmpty { highlight.type.name.replace("_", " ") },
                    color = RemixColors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${formatDuration(highlight.startTime)} - ${formatDuration(highlight.endTime)}",
                    color = RemixColors.textTertiary,
                    fontSize = 12.sp
                )
            }

            // Confidence
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${(highlight.confidence * 100).roundToInt()}%",
                    color = RemixColors.highlight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "confidence",
                    color = RemixColors.textTertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ============================================
// 🎛️ AUDIO TOOL CONTENT
// ============================================

@Composable
private fun AudioToolContent(
    onOpenProcessing: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = RemixColors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Audio Processing",
                color = RemixColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick presets
            Text("Quick Presets", color = RemixColors.textTertiary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioPresetChip("Voice", Icons.Default.RecordVoiceOver, RemixColors.pink, Modifier.weight(1f))
                AudioPresetChip("Podcast", Icons.Default.Podcasts, RemixColors.purple, Modifier.weight(1f))
                AudioPresetChip("Music", Icons.Default.MusicNote, RemixColors.blue, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onOpenProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RemixColors.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Tune, null, tint = RemixColors.textPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Advanced Processing", color = RemixColors.textPrimary)
            }
        }
    }
}

@Composable
private fun AudioPresetChip(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        onClick = { }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ============================================
// 🎬 VIDEO TOOL CONTENT
// ============================================

@Composable
private fun VideoToolContent() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = RemixColors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Video Options",
                color = RemixColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Aspect ratio options
            Text("Output Format", color = RemixColors.textTertiary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AspectRatioChip("16:9", true, Modifier.weight(1f))
                AspectRatioChip("9:16", false, Modifier.weight(1f))
                AspectRatioChip("1:1", false, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Video options
            VideoOptionRow(
                icon = Icons.Default.GraphicEq,
                title = "Waveform Overlay",
                subtitle = "Show audio visualization",
                enabled = false
            )

            VideoOptionRow(
                icon = Icons.Default.Image,
                title = "Background Image",
                subtitle = "Add custom background",
                enabled = false
            )
        }
    }
}

@Composable
private fun AspectRatioChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) RemixColors.pink else RemixColors.surfaceVariant,
        onClick = { }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = if (isSelected) Color.White else RemixColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun VideoOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean
) {
    var isEnabled by remember { mutableStateOf(enabled) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = RemixColors.textSecondary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = RemixColors.textPrimary, fontSize = 14.sp)
            Text(subtitle, color = RemixColors.textTertiary, fontSize = 11.sp)
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = { isEnabled = it },
            colors = SwitchDefaults.colors(checkedTrackColor = RemixColors.pink)
        )
    }
}

// ============================================
// 💬 CAPTIONS TOOL CONTENT
// ============================================

@Composable
private fun CaptionsToolContent() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = RemixColors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Captions & Subtitles",
                color = RemixColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Auto-generate button
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RemixColors.pink),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Auto-Generate Captions", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Caption style
            Text("Caption Style", color = RemixColors.textTertiary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CaptionStyle.entries.toList()) { style ->
                    CaptionStyleChip(style = style, isSelected = style == CaptionStyle.MODERN)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Export options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, RemixColors.glassBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Export SRT", color = RemixColors.textSecondary, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, RemixColors.glassBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Export VTT", color = RemixColors.textSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CaptionStyleChip(
    style: CaptionStyle,
    isSelected: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) RemixColors.pink else RemixColors.surfaceVariant,
        onClick = { }
    ) {
        Text(
            style.label,
            color = if (isSelected) Color.White else RemixColors.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

// ============================================
// 📤 BOTTOM ACTION BAR
// ============================================

@Composable
private fun BottomActionBar(
    hasSelection: Boolean,
    onQuickClip: () -> Unit,
    onAddToProject: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = RemixColors.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onQuickClip,
                modifier = Modifier.weight(1f),
                enabled = hasSelection,
                border = BorderStroke(1.dp, if (hasSelection) RemixColors.pink else RemixColors.glassBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.ContentCut,
                    null,
                    tint = if (hasSelection) RemixColors.pink else RemixColors.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Quick Clip",
                    color = if (hasSelection) RemixColors.pink else RemixColors.textTertiary
                )
            }

            Button(
                onClick = onAddToProject,
                modifier = Modifier.weight(1f),
                enabled = hasSelection,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RemixColors.pink,
                    disabledContainerColor = RemixColors.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Project")
            }
        }
    }
}

// ============================================
// 📤 EXPORT BOTTOM SHEET
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportBottomSheet(
    project: RemixProject,
    onDismiss: () -> Unit,
    onExport: (ExportConfig) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(defaultExportPresets[0]) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RemixColors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(RemixColors.glassBorder, RoundedCornerShape(2.dp))
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
                "Export",
                color = RemixColors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Choose an export preset",
                color = RemixColors.textTertiary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Presets
            defaultExportPresets.forEach { preset ->
                ExportPresetItem(
                    preset = preset,
                    isSelected = selectedPreset.id == preset.id,
                    onClick = { selectedPreset = preset }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onExport(selectedPreset.config) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RemixColors.pink),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export as ${selectedPreset.name}", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ExportPresetItem(
    preset: ExportPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) RemixColors.pink.copy(alpha = 0.15f) else RemixColors.surfaceVariant,
        border = if (isSelected) BorderStroke(2.dp, RemixColors.pink) else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(preset.icon, fontSize = 24.sp)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    preset.name,
                    color = RemixColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${preset.config.format.name.replace("_", " ")} • ${preset.config.quality.label}",
                    color = RemixColors.textTertiary,
                    fontSize = 12.sp
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = RemixColors.pink,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ============================================
// 🎛️ AUDIO PROCESSING SHEET
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioProcessingSheet(
    onDismiss: () -> Unit
) {
    var eqPreset by remember { mutableStateOf(EQPreset.FLAT) }
    var compressorPreset by remember { mutableStateOf(CompressorPreset.OFF) }
    var noiseReduction by remember { mutableFloatStateOf(0f) }
    var loudnessTarget by remember { mutableFloatStateOf(-14f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RemixColors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(RemixColors.glassBorder, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Audio Processing",
                color = RemixColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // EQ Preset
            Text("Equalizer", color = RemixColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EQPreset.entries.toList()) { preset ->
                    FilterChip(
                        selected = eqPreset == preset,
                        onClick = { eqPreset = preset },
                        label = { Text(preset.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RemixColors.pink,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Compressor
            Text("Compressor", color = RemixColors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CompressorPreset.entries.toList()) { preset ->
                    FilterChip(
                        selected = compressorPreset == preset,
                        onClick = { compressorPreset = preset },
                        label = { Text(preset.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RemixColors.purple,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Noise Reduction
            ProcessingSlider(
                label = "Noise Reduction",
                value = noiseReduction,
                onValueChange = { noiseReduction = it },
                color = RemixColors.blue
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Loudness Target
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Loudness Target", color = RemixColors.textSecondary, fontSize = 13.sp)
                    Text("${loudnessTarget.roundToInt()} LUFS", color = RemixColors.cyan, fontSize = 13.sp)
                }
                Slider(
                    value = loudnessTarget,
                    onValueChange = { loudnessTarget = it },
                    valueRange = -24f..-6f,
                    colors = SliderDefaults.colors(
                        thumbColor = RemixColors.cyan,
                        activeTrackColor = RemixColors.cyan
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RemixColors.pink),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Apply Processing", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ProcessingSlider(
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
            Text(label, color = RemixColors.textSecondary, fontSize = 13.sp)
            Text("${(value * 100).roundToInt()}%", color = color, fontSize = 13.sp)
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

// ============================================
// 🔧 UTILITY
// ============================================

private fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 60000) % 60
    val hours = ms / 3600000

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

