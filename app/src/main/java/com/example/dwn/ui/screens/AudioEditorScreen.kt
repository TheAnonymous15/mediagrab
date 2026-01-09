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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

// ============================================
// 🎵 AUDIO EDITOR SCREEN
// ============================================

private object AudioEditorColors {
    val pink = Color(0xFFE91E63)
    val purple = Color(0xFF9C27B0)
    val blue = Color(0xFF2196F3)
    val cyan = Color(0xFF00BCD4)
    val orange = Color(0xFFFF5722)
    val green = Color(0xFF4CAF50)

    val bgDark = Color(0xFF0D0D0D)
    val surface = Color(0xFF1A1A1A)
    val surfaceVariant = Color(0xFF252525)
    val card = Color(0xFF1E1E1E)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B0)
    val textTertiary = Color(0xFF707070)

    val waveform = Color(0xFFE91E63)
    val waveformBg = Color(0xFF3D1F2F)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEditorScreen(
    onBack: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0.3f) }
    var selectedTool by remember { mutableStateOf("trim") }
    var showEffects by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AudioEditorColors.bgDark)
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
                        Text("Audio Editor", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Untitled.mp3", color = AudioEditorColors.textSecondary, fontSize = 12.sp)
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
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Save, "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = AudioEditorColors.textPrimary,
                    navigationIconContentColor = AudioEditorColors.textPrimary,
                    actionIconContentColor = AudioEditorColors.textPrimary
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Waveform Display
                WaveformDisplay(
                    position = currentPosition,
                    onPositionChange = { currentPosition = it },
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Time indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("00:${(currentPosition * 60).toInt().toString().padStart(2, '0')}",
                         color = AudioEditorColors.textPrimary, fontSize = 14.sp)
                    Text("03:24", color = AudioEditorColors.textSecondary, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Playback Controls
                PlaybackControls(
                    isPlaying = isPlaying,
                    onPlayPause = { isPlaying = !isPlaying },
                    onSeekBack = { currentPosition = (currentPosition - 0.05f).coerceAtLeast(0f) },
                    onSeekForward = { currentPosition = (currentPosition + 0.05f).coerceAtMost(1f) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Tool Tabs
                ToolTabs(
                    selectedTool = selectedTool,
                    onToolSelected = { selectedTool = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tool Content
                when (selectedTool) {
                    "trim" -> TrimToolContent()
                    "effects" -> EffectsToolContent()
                    "enhance" -> EnhanceToolContent()
                    "mix" -> MixToolContent()
                }

                Spacer(modifier = Modifier.weight(1f))

                // Export Button
                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AudioEditorColors.pink
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Download, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Audio", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun WaveformDisplay(
    position: Float,
    onPositionChange: (Float) -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
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
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = AudioEditorColors.card
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Waveform visualization
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val centerY = height / 2

                // Draw waveform bars
                val barCount = 80
                val barWidth = width / barCount

                for (i in 0 until barCount) {
                    val x = i * barWidth
                    val normalizedPos = i.toFloat() / barCount

                    // Generate pseudo-random waveform
                    val amplitude = if (isPlaying) {
                        (0.3f + 0.7f * sin(phase / 30f + i * 0.3f).coerceIn(-1f, 1f).let { (it + 1) / 2 })
                    } else {
                        (0.2f + 0.6f * sin(i * 0.5f + normalizedPos * 10).let { (it + 1) / 2 })
                    }

                    val barHeight = height * 0.8f * amplitude

                    // Color based on position
                    val color = if (normalizedPos <= position) {
                        AudioEditorColors.waveform
                    } else {
                        AudioEditorColors.waveformBg
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x + 1, centerY - barHeight / 2),
                        size = androidx.compose.ui.geometry.Size(barWidth - 2, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                    )
                }

                // Position indicator line
                val indicatorX = width * position
                drawLine(
                    color = Color.White,
                    start = Offset(indicatorX, 0f),
                    end = Offset(indicatorX, height),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Selection handles
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left handle
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(AudioEditorColors.cyan, RoundedCornerShape(2.dp))
                )

                // Right handle
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(AudioEditorColors.cyan, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSeekBack) {
            Icon(
                Icons.Default.Replay10,
                "Back 10s",
                tint = AudioEditorColors.textPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        FloatingActionButton(
            onClick = onPlayPause,
            containerColor = AudioEditorColors.pink,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                "Play/Pause",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(onClick = onSeekForward) {
            Icon(
                Icons.Default.Forward10,
                "Forward 10s",
                tint = AudioEditorColors.textPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun ToolTabs(
    selectedTool: String,
    onToolSelected: (String) -> Unit
) {
    val tools = listOf(
        "trim" to Icons.Default.ContentCut,
        "effects" to Icons.Default.AutoAwesome,
        "enhance" to Icons.Default.Tune,
        "mix" to Icons.Default.Layers
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tools.forEach { (tool, icon) ->
            val isSelected = selectedTool == tool

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) AudioEditorColors.pink else AudioEditorColors.surfaceVariant,
                onClick = { onToolSelected(tool) }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        icon,
                        tool,
                        tint = if (isSelected) Color.White else AudioEditorColors.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        tool.replaceFirstChar { it.uppercase() },
                        color = if (isSelected) Color.White else AudioEditorColors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TrimToolContent() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AudioEditorColors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Trim Audio", color = AudioEditorColors.textPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = "00:00",
                    onValueChange = { },
                    label = { Text("Start") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AudioEditorColors.pink,
                        unfocusedBorderColor = AudioEditorColors.textTertiary
                    )
                )
                OutlinedTextField(
                    value = "03:24",
                    onValueChange = { },
                    label = { Text("End") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AudioEditorColors.pink,
                        unfocusedBorderColor = AudioEditorColors.textTertiary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Split at Position")
                }
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delete Selection")
                }
            }
        }
    }
}

@Composable
private fun EffectsToolContent() {
    data class EffectItem(val name: String, val icon: ImageVector, val color: Color)

    val effects = listOf(
        EffectItem("Fade In", Icons.Default.TrendingUp, AudioEditorColors.pink),
        EffectItem("Fade Out", Icons.Default.TrendingDown, AudioEditorColors.purple),
        EffectItem("Reverb", Icons.Default.Waves, AudioEditorColors.blue),
        EffectItem("Echo", Icons.Default.GraphicEq, AudioEditorColors.cyan),
        EffectItem("Pitch", Icons.Default.Speed, AudioEditorColors.orange),
        EffectItem("Tempo", Icons.Default.Timer, AudioEditorColors.green)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AudioEditorColors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Audio Effects", color = AudioEditorColors.textPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(180.dp)
            ) {
                items(effects) { effect ->
                    EffectButton(name = effect.name, icon = effect.icon, color = effect.color)
                }
            }
        }
    }
}

@Composable
private fun EffectButton(
    name: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        color = AudioEditorColors.surfaceVariant,
        onClick = { }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, name, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(name, color = AudioEditorColors.textSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun EnhanceToolContent() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AudioEditorColors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Enhance Audio", color = AudioEditorColors.textPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))

            EnhanceSlider("Volume", 0.7f, AudioEditorColors.pink)
            EnhanceSlider("Bass", 0.5f, AudioEditorColors.purple)
            EnhanceSlider("Treble", 0.5f, AudioEditorColors.blue)
            EnhanceSlider("Noise Reduction", 0.3f, AudioEditorColors.green)
        }
    }
}

@Composable
private fun EnhanceSlider(
    name: String,
    initialValue: Float,
    color: Color
) {
    var value by remember { mutableFloatStateOf(initialValue) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, color = AudioEditorColors.textSecondary, fontSize = 13.sp)
            Text("${(value * 100).toInt()}%", color = color, fontSize = 13.sp)
        }
        Slider(
            value = value,
            onValueChange = { value = it },
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
    }
}

@Composable
private fun MixToolContent() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AudioEditorColors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Mix Tracks", color = AudioEditorColors.textPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            // Track list
            repeat(2) { index ->
                TrackItem(
                    name = if (index == 0) "Main Audio" else "Background Music",
                    color = if (index == 0) AudioEditorColors.pink else AudioEditorColors.blue
                )
                if (index < 1) Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Audio Track")
            }
        }
    }
}

@Composable
private fun TrackItem(name: String, color: Color) {
    var volume by remember { mutableFloatStateOf(0.8f) }
    var muted by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = AudioEditorColors.surfaceVariant
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(name, color = AudioEditorColors.textPrimary, fontSize = 13.sp)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { muted = !muted }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    "Mute",
                    tint = if (muted) AudioEditorColors.textTertiary else color,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

