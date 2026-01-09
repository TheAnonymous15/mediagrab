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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================
// 🎬 VIDEO EDITOR SCREEN
// ============================================

private object VideoEditorColors {
    val pink = Color(0xFFE91E63)
    val purple = Color(0xFF9C27B0)
    val blue = Color(0xFF2196F3)
    val cyan = Color(0xFF00BCD4)
    val orange = Color(0xFFFF5722)
    val green = Color(0xFF4CAF50)
    val yellow = Color(0xFFFFC107)

    val bgDark = Color(0xFF0A0A0A)
    val surface = Color(0xFF151515)
    val surfaceVariant = Color(0xFF1E1E1E)
    val card = Color(0xFF252525)
    val timeline = Color(0xFF1A1A1A)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B0)
    val textTertiary = Color(0xFF707070)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    onBack: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0.25f) }
    var selectedTool by remember { mutableStateOf("trim") }
    var showExportDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VideoEditorColors.bgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Column {
                        Text("Video Editor", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Project 1", color = VideoEditorColors.textSecondary, fontSize = 12.sp)
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
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VideoEditorColors.pink
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = VideoEditorColors.textPrimary,
                    navigationIconContentColor = VideoEditorColors.textPrimary,
                    actionIconContentColor = VideoEditorColors.textPrimary
                )
            )

            // Video Preview
            VideoPreview(
                isPlaying = isPlaying,
                onPlayPause = { isPlaying = !isPlaying },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Timeline
            VideoTimeline(
                position = currentPosition,
                onPositionChange = { currentPosition = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )

            // Tool Selector
            VideoToolSelector(
                selectedTool = selectedTool,
                onToolSelected = { selectedTool = it }
            )

            // Tool Panel
            VideoToolPanel(
                selectedTool = selectedTool,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }

    if (showExportDialog) {
        ExportDialog(onDismiss = { showExportDialog = false })
    }
}

@Composable
private fun VideoPreview(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(VideoEditorColors.surface)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Video frame placeholder
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(16f / 9f),
            shape = RoundedCornerShape(12.dp),
            color = VideoEditorColors.card
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Placeholder content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = VideoEditorColors.textTertiary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Import a video to start",
                        color = VideoEditorColors.textTertiary,
                        fontSize = 14.sp
                    )
                }

                // Play button overlay
                FloatingActionButton(
                    onClick = onPlayPause,
                    containerColor = VideoEditorColors.pink.copy(alpha = 0.9f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Time display
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            shape = RoundedCornerShape(6.dp),
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Text(
                "00:15 / 01:30",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun VideoTimeline(
    position: Float,
    onPositionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = VideoEditorColors.timeline
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Time ruler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("0:00", "0:30", "1:00", "1:30").forEach { time ->
                    Text(time, color = VideoEditorColors.textTertiary, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Video track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(VideoEditorColors.surfaceVariant, RoundedCornerShape(6.dp))
            ) {
                // Video thumbnails placeholder
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(10) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            VideoEditorColors.pink.copy(alpha = 0.3f),
                                            VideoEditorColors.purple.copy(alpha = 0.3f)
                                        )
                                    )
                                )
                        )
                    }
                }

                // Playhead
                Box(
                    modifier = Modifier
                        .offset(x = (position * 300).dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Audio track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(VideoEditorColors.surfaceVariant, RoundedCornerShape(4.dp))
            ) {
                // Audio waveform placeholder
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    repeat(60) { i ->
                        val height = (4 + (i % 5) * 3).dp
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(height)
                                .background(VideoEditorColors.green.copy(alpha = 0.6f), RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoToolSelector(
    selectedTool: String,
    onToolSelected: (String) -> Unit
) {
    val tools = listOf(
        Triple("trim", Icons.Default.ContentCut, "Trim"),
        Triple("filters", Icons.Default.FilterVintage, "Filters"),
        Triple("text", Icons.Default.TextFields, "Text"),
        Triple("audio", Icons.Default.MusicNote, "Audio"),
        Triple("speed", Icons.Default.Speed, "Speed"),
        Triple("transitions", Icons.Default.SwapHoriz, "Effects")
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(VideoEditorColors.surface)
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(tools) { (id, icon, label) ->
            val isSelected = selectedTool == id

            Surface(
                onClick = { onToolSelected(id) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) VideoEditorColors.pink else Color.Transparent
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        icon,
                        label,
                        tint = if (isSelected) Color.White else VideoEditorColors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        label,
                        color = if (isSelected) Color.White else VideoEditorColors.textSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoToolPanel(
    selectedTool: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = VideoEditorColors.surface
    ) {
        when (selectedTool) {
            "trim" -> TrimPanel()
            "filters" -> FiltersPanel()
            "text" -> TextPanel()
            "audio" -> AudioPanel()
            "speed" -> SpeedPanel()
            "transitions" -> TransitionsPanel()
        }
    }
}

@Composable
private fun TrimPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Trim & Split", color = VideoEditorColors.textPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TrimAction(Icons.Default.ContentCut, "Split", VideoEditorColors.pink)
            TrimAction(Icons.Default.Delete, "Delete", VideoEditorColors.orange)
            TrimAction(Icons.Default.ContentCopy, "Duplicate", VideoEditorColors.blue)
            TrimAction(Icons.Default.Crop, "Crop", VideoEditorColors.purple)
        }
    }
}

@Composable
private fun RowScope.TrimAction(icon: ImageVector, label: String, color: Color) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        color = VideoEditorColors.surfaceVariant,
        onClick = { }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, label, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = VideoEditorColors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FiltersPanel() {
    val filters = listOf(
        "Original" to null,
        "Vivid" to VideoEditorColors.pink,
        "Warm" to VideoEditorColors.orange,
        "Cool" to VideoEditorColors.blue,
        "B&W" to Color.Gray,
        "Vintage" to VideoEditorColors.yellow,
        "Cinema" to VideoEditorColors.purple,
        "Drama" to Color.DarkGray
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Filters", color = VideoEditorColors.textPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filters) { (name, color) ->
                FilterPreview(name = name, color = color)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Adjustment sliders
        AdjustmentSlider("Brightness", 0.5f)
        AdjustmentSlider("Contrast", 0.5f)
        AdjustmentSlider("Saturation", 0.5f)
    }
}

@Composable
private fun FilterPreview(name: String, color: Color?) {
    var selected by remember { mutableStateOf(name == "Original") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(60.dp),
            shape = RoundedCornerShape(12.dp),
            color = color ?: VideoEditorColors.card,
            border = if (selected) BorderStroke(2.dp, VideoEditorColors.pink) else null,
            onClick = { selected = true }
        ) {
            // Filter preview
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, color = VideoEditorColors.textSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun AdjustmentSlider(name: String, initialValue: Float) {
    var value by remember { mutableFloatStateOf(initialValue) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            color = VideoEditorColors.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.width(80.dp)
        )
        Slider(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = VideoEditorColors.pink,
                activeTrackColor = VideoEditorColors.pink
            )
        )
        Text(
            "${(value * 100).toInt()}",
            color = VideoEditorColors.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TextPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Add Text", color = VideoEditorColors.textPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = "",
            onValueChange = { },
            placeholder = { Text("Enter text...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VideoEditorColors.pink,
                unfocusedBorderColor = VideoEditorColors.textTertiary,
                focusedTextColor = VideoEditorColors.textPrimary,
                unfocusedTextColor = VideoEditorColors.textPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextStyleChip("Title", true)
            TextStyleChip("Subtitle", false)
            TextStyleChip("Caption", false)
            TextStyleChip("Lower Third", false)
        }
    }
}

@Composable
private fun TextStyleChip(name: String, selected: Boolean) {
    FilterChip(
        selected = selected,
        onClick = { },
        label = { Text(name, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VideoEditorColors.pink,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
private fun AudioPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Audio", color = VideoEditorColors.textPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AudioAction(Icons.Default.MusicNote, "Add Music", VideoEditorColors.pink, Modifier.weight(1f))
            AudioAction(Icons.Default.Mic, "Voiceover", VideoEditorColors.purple, Modifier.weight(1f))
            AudioAction(Icons.Default.VolumeUp, "Sound FX", VideoEditorColors.blue, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        AdjustmentSlider("Video Volume", 0.8f)
        AdjustmentSlider("Music Volume", 0.5f)
    }
}

@Composable
private fun AudioAction(icon: ImageVector, label: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = VideoEditorColors.surfaceVariant,
        onClick = { }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, label, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = VideoEditorColors.textSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SpeedPanel() {
    var speed by remember { mutableFloatStateOf(1f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Speed", color = VideoEditorColors.textPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "${speed}x",
            color = VideoEditorColors.pink,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Slider(
            value = speed,
            onValueChange = { speed = it },
            valueRange = 0.25f..3f,
            steps = 10,
            colors = SliderDefaults.colors(
                thumbColor = VideoEditorColors.pink,
                activeTrackColor = VideoEditorColors.pink
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0.25x", color = VideoEditorColors.textTertiary, fontSize = 12.sp)
            Text("3x", color = VideoEditorColors.textTertiary, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.5f, 1f, 1.5f, 2f).forEach { preset ->
                FilterChip(
                    selected = speed == preset,
                    onClick = { speed = preset },
                    label = { Text("${preset}x") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VideoEditorColors.pink,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun TransitionsPanel() {
    val transitions = listOf(
        "None" to Icons.Default.Block,
        "Fade" to Icons.Default.Gradient,
        "Slide" to Icons.Default.SwapHoriz,
        "Zoom" to Icons.Default.ZoomIn,
        "Wipe" to Icons.Default.Splitscreen,
        "Dissolve" to Icons.Default.BlurOn
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Transitions", color = VideoEditorColors.textPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transitions) { (name, icon) ->
                TransitionItem(name = name, icon = icon)
            }
        }
    }
}

@Composable
private fun TransitionItem(name: String, icon: ImageVector) {
    var selected by remember { mutableStateOf(name == "None") }

    Surface(
        modifier = Modifier.aspectRatio(1.2f),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) VideoEditorColors.pink.copy(alpha = 0.2f) else VideoEditorColors.surfaceVariant,
        border = if (selected) BorderStroke(2.dp, VideoEditorColors.pink) else null,
        onClick = { selected = true }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                name,
                tint = if (selected) VideoEditorColors.pink else VideoEditorColors.textSecondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                name,
                color = if (selected) VideoEditorColors.pink else VideoEditorColors.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ExportDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VideoEditorColors.surface,
        title = {
            Text("Export Video", color = VideoEditorColors.textPrimary)
        },
        text = {
            Column {
                Text("Quality", color = VideoEditorColors.textSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("720p", "1080p", "4K").forEach { quality ->
                        FilterChip(
                            selected = quality == "1080p",
                            onClick = { },
                            label = { Text(quality) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VideoEditorColors.pink,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Format", color = VideoEditorColors.textSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MP4", "MOV", "WebM").forEach { format ->
                        FilterChip(
                            selected = format == "MP4",
                            onClick = { },
                            label = { Text(format) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VideoEditorColors.pink,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VideoEditorColors.pink)
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VideoEditorColors.textSecondary)
            }
        }
    )
}

