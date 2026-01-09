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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.podcast.*
import java.io.File

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
// AI TOOLS TAB
// ============================================

@Composable
fun AIToolsTab(
    aiState: AIProcessingState,
    recordings: List<File> = emptyList(),
    currentEpisode: PodcastEpisode? = null,
    onTranscribe: (String) -> Unit,
    onRemoveNoise: (String) -> Unit,
    onIsolateVoices: (String) -> Unit,
    onGenerateChapters: () -> Unit,
    onGenerateShowNotes: () -> Unit
) {
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var showFilePicker by remember { mutableStateOf(false) }
    var currentAction by remember { mutableStateOf<String?>(null) }

    // Auto-select episode audio if available
    LaunchedEffect(currentEpisode) {
        currentEpisode?.audioPath?.let { path ->
            selectedFile = File(path)
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Header
        item {
            AIHeaderSection()
        }

        // File Selection Section
        item {
            FileSelectionSection(
                selectedFile = selectedFile,
                recordings = recordings,
                onFileSelected = { selectedFile = it },
                onShowPicker = { showFilePicker = true }
            )
        }

        // Audio AI Tools
        item {
            Text(
                "Audio AI",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            AIToolsGrid(
                tools = listOf(
                    AITool(
                        icon = Icons.Default.NoiseAware,
                        title = "Noise Removal",
                        description = "Remove background noise and hum",
                        color = PodcastSecondary,
                        isProcessing = aiState.isRemovingNoise,
                        isEnabled = selectedFile != null,
                        onClick = { selectedFile?.let { onRemoveNoise(it.absolutePath) } }
                    ),
                    AITool(
                        icon = Icons.Default.RecordVoiceOver,
                        title = "Voice Isolation",
                        description = "Separate speakers into tracks",
                        color = PodcastPrimary,
                        isProcessing = aiState.isIsolatingVoices,
                        isEnabled = selectedFile != null,
                        onClick = { selectedFile?.let { onIsolateVoices(it.absolutePath) } }
                    ),
                    AITool(
                        icon = Icons.Default.SpatialAudio,
                        title = "Echo Removal",
                        description = "Remove room reverb and echo",
                        color = PodcastOrange,
                        isProcessing = false,
                        isEnabled = selectedFile != null,
                        onClick = { }
                    ),
                    AITool(
                        icon = Icons.Default.VolumeUp,
                        title = "Auto Leveling",
                        description = "Balance volume across tracks",
                        color = PodcastGreen,
                        isProcessing = false,
                        isEnabled = selectedFile != null,
                        onClick = { }
                    )
                )
            )
        }

        // Content AI Tools
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Content AI",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            AIToolsGrid(
                tools = listOf(
                    AITool(
                        icon = Icons.Default.TextFields,
                        title = "Transcription",
                        description = "Convert speech to text",
                        color = PodcastPink,
                        isProcessing = aiState.isTranscribing,
                        progress = aiState.transcriptionProgress,
                        isEnabled = selectedFile != null,
                        onClick = { selectedFile?.let { onTranscribe(it.absolutePath) } }
                    ),
                    AITool(
                        icon = Icons.Default.Bookmark,
                        title = "Auto Chapters",
                        description = "Generate chapter markers",
                        color = PodcastOrange,
                        isProcessing = aiState.isGeneratingChapters,
                        isEnabled = selectedFile != null,
                        onClick = onGenerateChapters
                    ),
                    AITool(
                        icon = Icons.Default.Description,
                        title = "Show Notes",
                        description = "Generate episode summary",
                        color = PodcastSecondary,
                        isProcessing = aiState.isGeneratingShowNotes,
                        isEnabled = selectedFile != null,
                        onClick = onGenerateShowNotes
                    ),
                    AITool(
                        icon = Icons.Default.Title,
                        title = "Title Generator",
                        description = "Suggest catchy titles",
                        color = PodcastYellow,
                        isProcessing = false,
                        isEnabled = selectedFile != null,
                        onClick = { }
                    )
                )
            )
        }

        // Video AI Tools
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Video AI",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            AIToolsGrid(
                tools = listOf(
                    AITool(
                        icon = Icons.Default.CenterFocusStrong,
                        title = "Auto Framing",
                        description = "Smart speaker tracking",
                        color = PodcastPrimary,
                        isProcessing = false,
                        onClick = { }
                    ),
                    AITool(
                        icon = Icons.Default.ContentCut,
                        title = "Jump Cut Removal",
                        description = "Remove silences and gaps",
                        color = PodcastAccent,
                        isProcessing = false,
                        onClick = { }
                    ),
                    AITool(
                        icon = Icons.Default.Subtitles,
                        title = "Auto Captions",
                        description = "Generate animated captions",
                        color = PodcastGreen,
                        isProcessing = false,
                        onClick = { }
                    ),
                    AITool(
                        icon = Icons.Default.RemoveRedEye,
                        title = "Eye Contact Fix",
                        description = "Correct gaze direction",
                        color = PodcastSecondary,
                        isProcessing = false,
                        onClick = { }
                    )
                )
            )
        }

        // Experimental Features
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ExperimentalFeaturesSection()
        }

        // Filler Words Detection
        item {
            FillerWordsSection(fillerWords = aiState.detectedFillerWords)
        }

        // Speaker Segments
        item {
            SpeakerSegmentsSection(segments = aiState.speakerSegments)
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun AIHeaderSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PodcastSurface
    ) {
        Box {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                PodcastPrimary.copy(alpha = 0.3f),
                                PodcastSecondary.copy(alpha = 0.2f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI Icon with animation
                val infiniteTransition = rememberInfiniteTransition(label = "ai")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .graphicsLayer { rotationZ = rotation }
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    PodcastPrimary,
                                    PodcastSecondary,
                                    PodcastPink,
                                    PodcastPrimary
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(PodcastSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        "AI Production Tools",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Powered by advanced machine learning",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        AIBadge("Real-time", PodcastGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        AIBadge("Pro Quality", PodcastPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun AIBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private data class AITool(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val color: Color,
    val isProcessing: Boolean,
    val progress: Float = 0f,
    val isEnabled: Boolean = true,
    val onClick: () -> Unit
)

@Composable
private fun AIToolsGrid(tools: List<AITool>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tools.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { tool ->
                    AIToolCard(
                        tool = tool,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty space if odd number of tools
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AIToolCard(
    tool: AITool,
    modifier: Modifier = Modifier
) {
    val isClickable = !tool.isProcessing && tool.isEnabled

    Surface(
        modifier = modifier
            .clickable(enabled = isClickable, onClick = tool.onClick)
            .then(if (!tool.isEnabled) Modifier.graphicsLayer { alpha = 0.5f } else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(tool.color.copy(alpha = if (tool.isEnabled) 0.15f else 0.08f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (tool.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = tool.color,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            tool.icon,
                            null,
                            tint = if (tool.isEnabled) tool.color else tool.color.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (tool.isProcessing && tool.progress > 0) {
                    Text(
                        "${(tool.progress * 100).toInt()}%",
                        color = tool.color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (!tool.isEnabled) {
                    Text(
                        "Select file",
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                tool.title,
                color = if (tool.isEnabled) TextPrimary else TextSecondary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                tool.description,
                color = TextTertiary,
                fontSize = 11.sp
            )

            if (tool.isProcessing && tool.progress > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { tool.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = tool.color,
                    trackColor = tool.color.copy(alpha = 0.2f),
                )
            }
        }
    }
}

@Composable
private fun ExperimentalFeaturesSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Science,
                    null,
                    tint = PodcastPink,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Experimental Features",
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PodcastPink.copy(alpha = 0.2f)
                ) {
                    Text(
                        "BETA",
                        color = PodcastPink,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExperimentalFeatureItem(
                icon = Icons.Default.SmartToy,
                title = "AI Co-Host",
                description = "Generate AI responses and commentary",
                isEnabled = false
            )

            ExperimentalFeatureItem(
                icon = Icons.Default.Translate,
                title = "Real-time Translation",
                description = "Live translate to multiple languages",
                isEnabled = false
            )

            ExperimentalFeatureItem(
                icon = Icons.Default.SurroundSound,
                title = "Spatial Audio",
                description = "Create immersive 3D audio experiences",
                isEnabled = true
            )
        }
    }
}

@Composable
private fun ExperimentalFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean
) {
    var enabled by remember { mutableStateOf(isEnabled) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = if (enabled) PodcastPink else TextTertiary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 13.sp)
            Text(description, color = TextTertiary, fontSize = 11.sp)
        }
        Switch(
            checked = enabled,
            onCheckedChange = { enabled = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = PodcastPink,
                checkedTrackColor = PodcastPink.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun FillerWordsSection(fillerWords: List<FillerWord>) {
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
                Text(
                    "Filler Words Detected",
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                if (fillerWords.isNotEmpty()) {
                    TextButton(onClick = { }) {
                        Text("Remove All", color = PodcastAccent, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (fillerWords.isEmpty()) {
                Text(
                    "No filler words detected. Run transcription to analyze.",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            } else {
                // Filler word summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FillerWordStat(word = "Um", count = 12)
                    FillerWordStat(word = "Uh", count = 8)
                    FillerWordStat(word = "Like", count = 15)
                    FillerWordStat(word = "You know", count = 6)
                }
            }
        }
    }
}

@Composable
private fun FillerWordStat(word: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$count",
            color = PodcastAccent,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(word, color = TextTertiary, fontSize = 11.sp)
    }
}

@Composable
private fun SpeakerSegmentsSection(segments: List<SpeakerSegment>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Speaker Diarization",
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (segments.isEmpty()) {
                Text(
                    "Run voice isolation to identify speakers.",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            } else {
                // Speaker breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpeakerBreakdown(
                        name = "Host",
                        percentage = 65,
                        color = PodcastPrimary,
                        modifier = Modifier.weight(0.65f)
                    )
                    SpeakerBreakdown(
                        name = "Guest",
                        percentage = 35,
                        color = PodcastSecondary,
                        modifier = Modifier.weight(0.35f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeakerBreakdown(
    name: String,
    percentage: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, color = TextPrimary, fontSize = 12.sp)
            Text("$percentage%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
    }
}

// ============================================
// FILE SELECTION SECTION
// ============================================

@Composable
private fun FileSelectionSection(
    selectedFile: File?,
    recordings: List<File>,
    onFileSelected: (File) -> Unit,
    onShowPicker: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = PodcastPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Source Audio",
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            if (selectedFile != null) "Ready for processing"
                            else "Select a recording to process",
                            color = if (selectedFile != null) PodcastGreen else TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }

            // Selected file info
            if (selectedFile != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = PodcastPrimary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PodcastGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selectedFile.name,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                "%.2f MB".format(selectedFile.length() / (1024.0 * 1024.0)),
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = { expanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Change",
                                tint = PodcastSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Recordings dropdown
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Select from recordings:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (recordings.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No recordings available",
                                    color = TextTertiary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "Record audio in the Record tab first",
                                    color = TextTertiary.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        recordings.take(5).forEach { file ->
                            val isSelected = selectedFile?.absolutePath == file.absolutePath
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onFileSelected(file)
                                        expanded = false
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PodcastPrimary.copy(alpha = 0.15f)
                                       else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isSelected) Icons.Default.RadioButtonChecked
                                        else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isSelected) PodcastPrimary else TextTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            file.name,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "%.2f MB".format(file.length() / (1024.0 * 1024.0)),
                                            color = TextTertiary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (recordings.size > 5) {
                            Text(
                                "+${recordings.size - 5} more recordings",
                                color = TextTertiary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 10.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

