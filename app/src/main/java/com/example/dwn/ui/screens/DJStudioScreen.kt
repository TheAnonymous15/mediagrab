package com.example.dwn.ui.screens
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.dj.*
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random
// DJ Colors matching Virtual DJ style
private object DJColors {
    val deckA = Color(0xFF00BFFF)  // Cyan/Blue for Deck A
    val deckB = Color(0xFFFF4444)  // Red for Deck B
    val waveA = Color(0xFF00D4FF)  // Light cyan waveform
    val waveB = Color(0xFFFF6666)  // Light red waveform
    val bgDark = Color(0xFF1A1A1A)
    val bgMid = Color(0xFF2D2D2D)
    val surface = Color(0xFF363636)
    val surfaceLight = Color(0xFF404040)
    val textWhite = Color(0xFFFFFFFF)
    val textGray = Color(0xFF808080)
    val textDim = Color(0xFF606060)
    val green = Color(0xFF00FF00)
    val yellow = Color(0xFFFFFF00)
    val orange = Color(0xFFFF8800)
    val red = Color(0xFFFF0000)
    val purple = Color(0xFF8844FF)
    val vinylLabel = Color(0xFFB8C800)  // Yellow-green vinyl label
    val vinylBlack = Color(0xFF1A1A1A)
    val chrome = Color(0xFF666666)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DJStudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val haptic = LocalHapticFeedback.current
    // Force landscape
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    // DJ Engine
    val djEngine = remember { DJEnginePro(context) }
    val deckAState by djEngine.deckAState.collectAsState()
    val deckBState by djEngine.deckBState.collectAsState()
    val mixerState by djEngine.mixerState.collectAsState()
    val fxState by djEngine.fxState.collectAsState()
    val deckAWaveform by djEngine.deckAWaveform.collectAsState()
    val deckBWaveform by djEngine.deckBWaveform.collectAsState()
    var showLibrary by remember { mutableStateOf(false) }
    var selectedDeckForLoad by remember { mutableIntStateOf(1) }
    // Jog rotation animation
    var deckAJog by remember { mutableFloatStateOf(0f) }
    var deckBJog by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(deckAState.isPlaying, deckAState.isScratching) {
        while (deckAState.isPlaying && !deckAState.isScratching) {
            deckAJog = (deckAJog + 2f) % 360f
            delay(16)
        }
    }
    LaunchedEffect(deckBState.isPlaying, deckBState.isScratching) {
        while (deckBState.isPlaying && !deckBState.isScratching) {
            deckBJog = (deckBJog + 2f) % 360f
            delay(16)
        }
    }
    DisposableEffect(Unit) { onDispose { djEngine.release() } }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DJColors.bgDark)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Waveforms
            WaveformRow(
                deckAState = deckAState,
                deckBState = deckBState,
                deckAWaveform = deckAWaveform,
                deckBWaveform = deckBWaveform,
                modifier = Modifier.weight(0.18f)
            )
            // Track Info Row
            TrackInfoRow(
                deckAState = deckAState,
                deckBState = deckBState,
                modifier = Modifier.fillMaxWidth()
            )
            // Main Controls Area
            Row(
                modifier = Modifier
                    .weight(0.82f)
                    .fillMaxWidth()
            ) {
                // Left Deck A
                DeckSection(
                    deckLabel = "A",
                    deckColor = DJColors.deckA,
                    deckState = deckAState,
                    jogRotation = deckAJog,
                    isLeftDeck = true,
                    onPlayPause = { djEngine.togglePlayDeckA() },
                    onCue = { djEngine.cueDeckA() },
                    onSync = { djEngine.syncDeckAToB() },
                    onStartScratch = { djEngine.startScratchA() },
                    onScratch = { djEngine.scratchDeckA(it) },
                    onEndScratch = { djEngine.endScratchA() },
                    onJogSpin = { deckAJog = (deckAJog + it) % 360f },
                    onVolumeChange = { djEngine.setDeckAVolume(it) },
                    onLoadTrack = { selectedDeckForLoad = 1; showLibrary = true },
                    modifier = Modifier.weight(1f)
                )
                // Center Mixer
                CenterMixer(
                    mixerState = mixerState,
                    deckAState = deckAState,
                    deckBState = deckBState,
                    onCrossfaderChange = { djEngine.setCrossfader(it) },
                    onMasterVolumeChange = { djEngine.setMasterVolume(it) },
                    onBack = onBack,
                    modifier = Modifier.width(220.dp)
                )
                // Right Deck B
                DeckSection(
                    deckLabel = "B",
                    deckColor = DJColors.deckB,
                    deckState = deckBState,
                    jogRotation = deckBJog,
                    isLeftDeck = false,
                    onPlayPause = { djEngine.togglePlayDeckB() },
                    onCue = { djEngine.cueDeckB() },
                    onSync = { djEngine.syncDeckBToA() },
                    onStartScratch = { djEngine.startScratchB() },
                    onScratch = { djEngine.scratchDeckB(it) },
                    onEndScratch = { djEngine.endScratchB() },
                    onJogSpin = { deckBJog = (deckBJog + it) % 360f },
                    onVolumeChange = { djEngine.setDeckBVolume(it) },
                    onLoadTrack = { selectedDeckForLoad = 2; showLibrary = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    // Library Sheet
    if (showLibrary) {
        TrackLibrarySheet(
            djEngine = djEngine,
            selectedDeck = selectedDeckForLoad,
            onDismiss = { showLibrary = false },
            onLoadTrack = { track ->
                if (selectedDeckForLoad == 1) djEngine.loadToDeckA(track)
                else djEngine.loadToDeckB(track)
                showLibrary = false
            }
        )
    }
}
// ============================================
// WAVEFORM ROW
// ============================================
@Composable
private fun WaveformRow(
    deckAState: DeckStatePro,
    deckBState: DeckStatePro,
    deckAWaveform: List<Float>,
    deckBWaveform: List<Float>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Deck A Waveform
        WaveformDisplay(
            waveform = deckAWaveform,
            position = deckAState.position,
            deckColor = DJColors.waveA,
            isPlaying = deckAState.isPlaying,
            modifier = Modifier.weight(1f)
        )
        // Center playhead indicator
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.White)
        )
        // Deck B Waveform
        WaveformDisplay(
            waveform = deckBWaveform,
            position = deckBState.position,
            deckColor = DJColors.waveB,
            isPlaying = deckBState.isPlaying,
            modifier = Modifier.weight(1f)
        )
    }
}
@Composable
private fun WaveformDisplay(
    waveform: List<Float>,
    position: Float,
    deckColor: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(4.dp))) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        // Background
        drawRect(Color(0xFF1A1A1A))
        if (waveform.isNotEmpty()) {
            val barWidth = width / waveform.size
            val playheadX = position * width
            waveform.forEachIndexed { index, amplitude ->
                val x = index * barWidth
                val barHeight = amplitude * height * 0.8f
                val isPast = x < playheadX
                val alpha = if (isPast) 1f else 0.5f
                // Top half
                drawRect(
                    color = deckColor.copy(alpha = alpha),
                    topLeft = Offset(x, centerY - barHeight / 2),
                    size = Size(maxOf(barWidth - 0.5f, 1f), barHeight / 2)
                )
                // Bottom half (mirrored)
                drawRect(
                    color = deckColor.copy(alpha = alpha * 0.7f),
                    topLeft = Offset(x, centerY),
                    size = Size(maxOf(barWidth - 0.5f, 1f), barHeight / 2)
                )
            }
            // Playhead line
            drawLine(
                color = Color.White,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, height),
                strokeWidth = 2f
            )
        } else {
            // Empty state - center line
            drawLine(
                color = deckColor.copy(alpha = 0.3f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1f
            )
        }
    }
}
// ============================================
// TRACK INFO ROW
// ============================================
@Composable
private fun TrackInfoRow(
    deckAState: DeckStatePro,
    deckBState: DeckStatePro,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(DJColors.bgMid)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Deck A Info
        DeckInfoPanel(
            deckLabel = "A",
            track = deckAState.track,
            bpm = deckAState.bpm,
            remain = deckAState.duration - deckAState.currentPosition,
            deckColor = DJColors.deckA,
            modifier = Modifier.weight(1f)
        )
        // Center tabs
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabButton("AUDIO", true)
            TabButton("VIDEO", false)
        }
        // Deck B Info
        DeckInfoPanel(
            deckLabel = "B",
            track = deckBState.track,
            bpm = deckBState.bpm,
            remain = deckBState.duration - deckBState.currentPosition,
            deckColor = DJColors.deckB,
            isRightAligned = true,
            modifier = Modifier.weight(1f)
        )
    }
}
@Composable
private fun TabButton(label: String, isActive: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isActive) DJColors.surfaceLight else Color.Transparent,
        border = if (!isActive) BorderStroke(1.dp, DJColors.textDim) else null
    ) {
        Text(
            label,
            color = if (isActive) Color.White else DJColors.textGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
@Composable
private fun DeckInfoPanel(
    deckLabel: String,
    track: DeviceTrackPro?,
    bpm: Float,
    remain: Long,
    deckColor: Color,
    isRightAligned: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = if (isRightAligned) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isRightAligned) {
            Text(deckLabel, color = deckColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
        }
        Column(
            horizontalAlignment = if (isRightAligned) Alignment.End else Alignment.Start
        ) {
            Text(
                track?.let { "${it.artist} - ${it.title}" } ?: "No Track Loaded",
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("BPM", color = DJColors.textGray, fontSize = 8.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    String.format("%.2f", bpm),
                    color = deckColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("REMAIN", color = DJColors.textGray, fontSize = 8.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    formatTime(remain),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
        if (isRightAligned) {
            Spacer(Modifier.width(8.dp))
            Text(deckLabel, color = deckColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
// ============================================
// DECK SECTION
// ============================================
@Composable
private fun DeckSection(
    deckLabel: String,
    deckColor: Color,
    deckState: DeckStatePro,
    jogRotation: Float,
    isLeftDeck: Boolean,
    onPlayPause: () -> Unit,
    onCue: () -> Unit,
    onSync: () -> Unit,
    onStartScratch: () -> Unit,
    onScratch: (Float) -> Unit,
    onEndScratch: () -> Unit,
    onJogSpin: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onLoadTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxHeight().padding(4.dp)) {
        if (isLeftDeck) {
            // Left deck: Pads | Jog | Volume
            PadsAndFXColumn(deckColor = deckColor, isLeftDeck = true, modifier = Modifier.width(100.dp))
            JogWheelSection(
                rotation = jogRotation,
                isPlaying = deckState.isPlaying,
                deckColor = deckColor,
                deckLabel = deckLabel,
                onStartScratch = onStartScratch,
                onScratch = { delta -> onScratch(delta); onJogSpin(delta * 2f) },
                onEndScratch = onEndScratch,
                onLoadTrack = onLoadTrack,
                modifier = Modifier.weight(1f)
            )
            VolumeAndTransport(
                volume = deckState.volume,
                isPlaying = deckState.isPlaying,
                deckColor = deckColor,
                onVolumeChange = onVolumeChange,
                onPlayPause = onPlayPause,
                onCue = onCue,
                onSync = onSync,
                modifier = Modifier.width(80.dp)
            )
        } else {
            // Right deck: Volume | Jog | Pads
            VolumeAndTransport(
                volume = deckState.volume,
                isPlaying = deckState.isPlaying,
                deckColor = deckColor,
                onVolumeChange = onVolumeChange,
                onPlayPause = onPlayPause,
                onCue = onCue,
                onSync = onSync,
                modifier = Modifier.width(80.dp)
            )
            JogWheelSection(
                rotation = jogRotation,
                isPlaying = deckState.isPlaying,
                deckColor = deckColor,
                deckLabel = deckLabel,
                onStartScratch = onStartScratch,
                onScratch = { delta -> onScratch(delta); onJogSpin(delta * 2f) },
                onEndScratch = onEndScratch,
                onLoadTrack = onLoadTrack,
                modifier = Modifier.weight(1f)
            )
            PadsAndFXColumn(deckColor = deckColor, isLeftDeck = false, modifier = Modifier.width(100.dp))
        }
    }
}
// ============================================
// PADS AND FX COLUMN
// ============================================
@Composable
private fun PadsAndFXColumn(
    deckColor: Color,
    isLeftDeck: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedPadMode by remember { mutableStateOf("BEATGRID") }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // PADS Section
        Column {
            Text("PADS", color = DJColors.textGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            // Pad mode buttons
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                PadModeBtn("BEATGRID", selectedPadMode == "BEATGRID", deckColor) { selectedPadMode = "BEATGRID" }
                PadModeBtn("ROLLS", selectedPadMode == "ROLLS", deckColor) { selectedPadMode = "ROLLS" }
            }
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                PadModeBtn("SCRATCH", selectedPadMode == "SCRATCH", deckColor) { selectedPadMode = "SCRATCH" }
                PadModeBtn("SAMPLER", selectedPadMode == "SAMPLER", deckColor) { selectedPadMode = "SAMPLER" }
            }
            Spacer(Modifier.height(6.dp))
            // Pattern pads
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                PatternPad("Pattern 1", deckColor, Modifier.weight(1f))
                PatternPad("Pattern 2", deckColor, Modifier.weight(1f))
            }
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                PatternPad("Pattern 3", deckColor, Modifier.weight(1f))
                PatternPad("Pattern 4", deckColor, Modifier.weight(1f))
            }
        }
        // FX Section
        Column {
            Text("FX", color = DJColors.textGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FXKnob("FILTER", deckColor)
                FXKnob("FLANGER", deckColor)
                FXKnob("CUT", deckColor)
            }
        }
        // LOOP Section
        Column {
            Text("LOOP", color = DJColors.textGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoopButton("<", deckColor)
                LoopButton("4", deckColor, isMain = true)
                LoopButton(">", deckColor)
            }
        }
    }
}
@Composable
private fun PadModeBtn(label: String, isActive: Boolean, deckColor: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(3.dp),
        color = if (isActive) deckColor else DJColors.surface,
        modifier = Modifier.height(20.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(label, color = if (isActive) Color.Black else DJColors.textGray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
private fun PatternPad(label: String, deckColor: Color, modifier: Modifier = Modifier) {
    Surface(
        onClick = { },
        shape = RoundedCornerShape(4.dp),
        color = DJColors.surface,
        border = BorderStroke(1.dp, deckColor.copy(alpha = 0.3f)),
        modifier = modifier.height(28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = DJColors.textGray, fontSize = 7.sp)
        }
    }
}
@Composable
private fun FXKnob(label: String, deckColor: Color) {
    var value by remember { mutableFloatStateOf(0.5f) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        value = (value - dragAmount.y / 100f).coerceIn(0f, 1f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2 - 2.dp.toPx()
                // Outer ring
                drawCircle(color = DJColors.chrome, radius = radius, center = center)
                drawCircle(color = DJColors.bgDark, radius = radius * 0.85f, center = center)
                // Position indicator
                val angle = -135f + value * 270f
                val rad = angle * (PI / 180f).toFloat()
                drawLine(
                    color = deckColor,
                    start = Offset(center.x + cos(rad) * radius * 0.3f, center.y + sin(rad) * radius * 0.3f),
                    end = Offset(center.x + cos(rad) * radius * 0.7f, center.y + sin(rad) * radius * 0.7f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }
        Text(label, color = DJColors.textGray, fontSize = 6.sp)
    }
}
@Composable
private fun LoopButton(label: String, deckColor: Color, isMain: Boolean = false) {
    Surface(
        onClick = { },
        shape = RoundedCornerShape(4.dp),
        color = if (isMain) DJColors.surface else Color.Transparent,
        border = BorderStroke(1.dp, DJColors.textDim),
        modifier = Modifier.size(if (isMain) 32.dp else 24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontSize = if (isMain) 12.sp else 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
// ============================================
// JOG WHEEL SECTION
// ============================================
@Composable
private fun JogWheelSection(
    rotation: Float,
    isPlaying: Boolean,
    deckColor: Color,
    deckLabel: String,
    onStartScratch: () -> Unit,
    onScratch: (Float) -> Unit,
    onEndScratch: () -> Unit,
    onLoadTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lastAngle by remember { mutableFloatStateOf(0f) }
    Column(
        modifier = modifier.fillMaxHeight().padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Match button
        Surface(
            onClick = { },
            shape = RoundedCornerShape(4.dp),
            color = DJColors.surface
        ) {
            Text("Match", color = DJColors.textGray, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Spacer(Modifier.height(8.dp))
        // Jog Wheel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onStartScratch() },
                        onDragEnd = { onEndScratch() },
                        onDragCancel = { onEndScratch() }
                    ) { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val angle = atan2(
                            change.position.y - center.y,
                            change.position.x - center.x
                        ) * (180f / PI.toFloat())
                        val delta = angle - lastAngle
                        if (abs(delta) < 180) {
                            onScratch(delta)
                        }
                        lastAngle = angle
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                val size = size.minDimension
                val center = Offset(this.size.width / 2, this.size.height / 2)
                val radius = size / 2
                // Outer chrome ring with dots pattern
                drawCircle(
                    color = DJColors.chrome,
                    radius = radius,
                    center = center
                )
                // Dots pattern on outer ring
                for (i in 0 until 72) {
                    val angle = (i * 5f) * (PI / 180f).toFloat()
                    val dotRadius = radius * 0.92f
                    drawCircle(
                        color = Color(0xFF444444),
                        radius = 2f,
                        center = Offset(
                            center.x + cos(angle) * dotRadius,
                            center.y + sin(angle) * dotRadius
                        )
                    )
                }
                // Inner black area
                drawCircle(
                    color = DJColors.vinylBlack,
                    radius = radius * 0.85f,
                    center = center
                )
                // Vinyl grooves
                rotate(rotation, center) {
                    for (i in 1..12) {
                        val grooveRadius = radius * (0.35f + i * 0.04f)
                        drawCircle(
                            color = Color(0xFF252525),
                            radius = grooveRadius,
                            center = center,
                            style = Stroke(width = 0.5f)
                        )
                    }
                    // Position marker
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = Offset(center.x, center.y - radius * 0.25f),
                        end = Offset(center.x, center.y - radius * 0.65f),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
                // Center label (yellow-green)
                drawCircle(
                    color = DJColors.vinylLabel,
                    radius = radius * 0.22f,
                    center = center
                )
                // Center spindle
                drawCircle(
                    color = Color(0xFF333333),
                    radius = radius * 0.08f,
                    center = center
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // Load button
        Surface(
            onClick = onLoadTrack,
            shape = RoundedCornerShape(4.dp),
            color = deckColor.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, deckColor)
        ) {
            Text(
                "LOAD",
                color = deckColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}
// ============================================
// VOLUME AND TRANSPORT
// ============================================
@Composable
private fun VolumeAndTransport(
    volume: Float,
    isPlaying: Boolean,
    deckColor: Color,
    onVolumeChange: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onCue: () -> Unit,
    onSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight().padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Volume slider
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VerticalVolumeSlider(
                value = volume,
                deckColor = deckColor,
                onChange = onVolumeChange,
                modifier = Modifier.weight(1f).width(40.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        // Transport buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // CUE button
            TransportButton(
                label = "CUE",
                color = DJColors.orange,
                onClick = onCue,
                modifier = Modifier.fillMaxWidth()
            )
            // Play/Pause button
            TransportButton(
                label = if (isPlaying) "II" else "▶",
                color = if (isPlaying) DJColors.green else deckColor,
                isActive = isPlaying,
                onClick = onPlayPause,
                modifier = Modifier.fillMaxWidth()
            )
            // SYNC button
            TransportButton(
                label = "SYNC",
                color = DJColors.purple,
                onClick = onSync,
                modifier = Modifier.fillMaxWidth()
            )
            // Mix Assist
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Mix Assist", color = DJColors.textGray, fontSize = 7.sp)
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(DJColors.surface, CircleShape)
                        .border(1.dp, DJColors.textDim, CircleShape)
                )
            }
        }
    }
}
@Composable
private fun VerticalVolumeSlider(
    value: Float,
    deckColor: Color,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var componentHeight by remember { mutableFloatStateOf(1f) }
    val haptic = LocalHapticFeedback.current
    val animatedValue by animateFloatAsState(targetValue = value, animationSpec = tween(50), label = "vol")
    val draggableState = rememberDraggableState { delta ->
        if (componentHeight > 0) {
            val newValue = (value - delta * 1.2f / componentHeight).coerceIn(0f, 1f)
            onChange(newValue)
        }
    }
    Box(
        modifier = modifier
            .onSizeChanged { componentHeight = it.height.toFloat() }
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val trackWidth = 8.dp.toPx()
            val trackHeight = size.height - 20.dp.toPx()
            val cX = size.width / 2
            val topY = 10.dp.toPx()
            // Track background
            drawRoundRect(
                color = Color(0xFF1A1A1A),
                topLeft = Offset(cX - trackWidth / 2, topY),
                size = Size(trackWidth, trackHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            // Level fill
            val fillHeight = trackHeight * animatedValue
            if (fillHeight > 0) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(DJColors.red, DJColors.yellow, DJColors.green, deckColor)
                    ),
                    topLeft = Offset(cX - trackWidth / 2, topY + trackHeight - fillHeight),
                    size = Size(trackWidth, fillHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }
            // Fader knob
            val knobY = topY + trackHeight * (1 - animatedValue)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF888888), Color(0xFF444444))),
                topLeft = Offset(cX - 12.dp.toPx(), knobY - 10.dp.toPx()),
                size = Size(24.dp.toPx(), 20.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            // Knob grip lines
            for (i in -1..1) {
                drawLine(
                    color = Color(0xFF222222),
                    start = Offset(cX - 6.dp.toPx(), knobY + i * 4.dp.toPx()),
                    end = Offset(cX + 6.dp.toPx(), knobY + i * 4.dp.toPx()),
                    strokeWidth = 1.5f
                )
            }
        }
    }
}
@Composable
private fun TransportButton(
    label: String,
    color: Color,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isActive) color else DJColors.surface,
        border = BorderStroke(1.dp, color),
        modifier = modifier.height(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (isActive) Color.Black else color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
// ============================================
// CENTER MIXER
// ============================================
@Composable
private fun CenterMixer(
    mixerState: MixerState,
    deckAState: DeckStatePro,
    deckBState: DeckStatePro,
    onCrossfaderChange: (Float) -> Unit,
    onMasterVolumeChange: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var vuLevelA by remember { mutableFloatStateOf(0f) }
    var vuLevelB by remember { mutableFloatStateOf(0f) }
    // Simulate VU meters
    LaunchedEffect(deckAState.isPlaying, deckAState.volume, mixerState.crossfader) {
        while (true) {
            if (deckAState.isPlaying) {
                val crossfadeA = if (mixerState.crossfader <= 0.5f) 1f else 1f - ((mixerState.crossfader - 0.5f) * 2f)
                vuLevelA = (deckAState.volume * crossfadeA * (0.6f + Random.nextFloat() * 0.4f)).coerceIn(0f, 1f)
            } else {
                vuLevelA = (vuLevelA * 0.85f).coerceAtMost(0.01f)
            }
            delay(50)
        }
    }
    LaunchedEffect(deckBState.isPlaying, deckBState.volume, mixerState.crossfader) {
        while (true) {
            if (deckBState.isPlaying) {
                val crossfadeB = if (mixerState.crossfader >= 0.5f) 1f else mixerState.crossfader * 2f
                vuLevelB = (deckBState.volume * crossfadeB * (0.6f + Random.nextFloat() * 0.4f)).coerceIn(0f, 1f)
            } else {
                vuLevelB = (vuLevelB * 0.85f).coerceAtMost(0.01f)
            }
            delay(50)
        }
    }
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = DJColors.bgMid
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            IconButton(onClick = onBack, modifier = Modifier.size(24.dp).align(Alignment.Start)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = DJColors.textGray, modifier = Modifier.size(16.dp))
            }
            // Vertical knobs for VOCAL, INSTRU, BEAT
            Row(
                modifier = Modifier.weight(0.4f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MixerKnobColumn("VOCAL", DJColors.deckA, DJColors.deckB)
                MixerKnobColumn("INSTRU", DJColors.deckA, DJColors.deckB)
                MixerKnobColumn("BEAT", DJColors.deckA, DJColors.deckB)
            }
            Spacer(Modifier.height(8.dp))
            // MIX FX label
            Text("MIX FX", color = DJColors.textGray, fontSize = 8.sp)
            // Headphone buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                HeadphoneButton(false)
                HeadphoneButton(true)
            }
            Spacer(Modifier.height(8.dp))
            // VU Meters + Master Volume
            Row(
                modifier = Modifier.weight(0.3f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VUMeter(vuLevelA, DJColors.deckA, Modifier.width(12.dp).fillMaxHeight())
                Spacer(Modifier.width(8.dp))
                MasterVolumeSlider(
                    value = mixerState.masterVolume,
                    onChange = onMasterVolumeChange,
                    modifier = Modifier.width(30.dp).fillMaxHeight()
                )
                Spacer(Modifier.width(8.dp))
                VUMeter(vuLevelB, DJColors.deckB, Modifier.width(12.dp).fillMaxHeight())
            }
            Spacer(Modifier.height(12.dp))
            // Crossfader
            CrossfaderSlider(
                value = mixerState.crossfader,
                onChange = onCrossfaderChange,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )
            // A/B Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("A", color = DJColors.deckA, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("B", color = DJColors.deckB, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
@Composable
private fun MixerKnobColumn(label: String, colorA: Color, colorB: Color) {
    var valueA by remember { mutableFloatStateOf(0.5f) }
    var valueB by remember { mutableFloatStateOf(0.5f) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MixerKnob(value = valueA, color = colorA, onChange = { valueA = it })
        Text(label, color = DJColors.textGray, fontSize = 7.sp)
        MixerKnob(value = valueB, color = colorB, onChange = { valueB = it })
    }
}
@Composable
private fun MixerKnob(value: Float, color: Color, onChange: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    onChange((value - dragAmount.y / 100f).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 2.dp.toPx()
            drawCircle(color = DJColors.chrome, radius = radius, center = center)
            drawCircle(color = DJColors.bgDark, radius = radius * 0.8f, center = center)
            val angle = -135f + value * 270f
            val rad = angle * (PI / 180f).toFloat()
            drawLine(
                color = color,
                start = Offset(center.x + cos(rad) * radius * 0.25f, center.y + sin(rad) * radius * 0.25f),
                end = Offset(center.x + cos(rad) * radius * 0.65f, center.y + sin(rad) * radius * 0.65f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
    }
}
@Composable
private fun HeadphoneButton(isActive: Boolean) {
    Surface(
        onClick = { },
        shape = RoundedCornerShape(4.dp),
        color = if (isActive) DJColors.orange else DJColors.surface
    ) {
        Icon(
            Icons.Default.Headphones,
            contentDescription = null,
            tint = if (isActive) Color.Black else DJColors.textGray,
            modifier = Modifier.padding(6.dp).size(16.dp)
        )
    }
}
@Composable
private fun VUMeter(level: Float, color: Color, modifier: Modifier = Modifier) {
    val animLevel by animateFloatAsState(level, tween(50), label = "vu")
    Canvas(modifier) {
        val segs = 16
        val segH = (size.height - 8.dp.toPx()) / segs
        val gap = 2.dp.toPx()
        val active = (animLevel * segs).toInt()
        for (i in 0 until segs) {
            val segColor = when {
                i >= 14 -> DJColors.red
                i >= 12 -> DJColors.yellow
                else -> color
            }
            drawRoundRect(
                color = if ((segs - 1 - i) < active) segColor else segColor.copy(alpha = 0.15f),
                topLeft = Offset(0f, 4.dp.toPx() + i * segH + gap / 2),
                size = Size(size.width, segH - gap),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
        }
    }
}
@Composable
private fun MasterVolumeSlider(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var componentHeight by remember { mutableFloatStateOf(1f) }
    val haptic = LocalHapticFeedback.current
    val animatedValue by animateFloatAsState(targetValue = value, animationSpec = tween(50), label = "master")
    val draggableState = rememberDraggableState { delta ->
        if (componentHeight > 0) {
            onChange((value - delta * 1.2f / componentHeight).coerceIn(0f, 1f))
        }
    }
    Box(
        modifier = modifier
            .onSizeChanged { componentHeight = it.height.toFloat() }
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val trackWidth = 6.dp.toPx()
            val trackHeight = size.height - 16.dp.toPx()
            val cX = size.width / 2
            val topY = 8.dp.toPx()
            drawRoundRect(
                color = Color(0xFF1A1A1A),
                topLeft = Offset(cX - trackWidth / 2, topY),
                size = Size(trackWidth, trackHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
            val knobY = topY + trackHeight * (1 - animatedValue)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF888888), Color(0xFF444444))),
                topLeft = Offset(cX - 10.dp.toPx(), knobY - 8.dp.toPx()),
                size = Size(20.dp.toPx(), 16.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
            )
        }
    }
}
@Composable
private fun CrossfaderSlider(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var componentWidth by remember { mutableFloatStateOf(1f) }
    val haptic = LocalHapticFeedback.current
    var lastValue by remember { mutableFloatStateOf(value) }
    val animatedValue by animateFloatAsState(targetValue = value, animationSpec = tween(30), label = "xfade")
    val draggableState = rememberDraggableState { delta ->
        if (componentWidth > 0) {
            val newValue = (value + delta * 1.3f / componentWidth).coerceIn(0f, 1f)
            if ((lastValue < 0.5f && newValue >= 0.5f) || (lastValue > 0.5f && newValue <= 0.5f)) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            lastValue = newValue
            onChange(newValue)
        }
    }
    Box(
        modifier = modifier
            .onSizeChanged { componentWidth = it.width.toFloat() }
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            )
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val pad = 16.dp.toPx()
                    val trackWidth = size.width - pad * 2
                    val tappedValue = ((offset.x - pad) / trackWidth).coerceIn(0f, 1f)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onChange(tappedValue)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cY = size.height / 2
            val pad = 16.dp.toPx()
            val trackWidth = size.width - pad * 2
            val knobWidth = 44.dp.toPx()
            val knobHeight = 28.dp.toPx()
            // Track
            drawRoundRect(
                color = Color(0xFF1A1A1A),
                topLeft = Offset(pad, cY - 8.dp.toPx()),
                size = Size(trackWidth, 16.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
            )
            // Center line
            drawLine(
                color = DJColors.textDim,
                start = Offset(size.width / 2, cY - 20.dp.toPx()),
                end = Offset(size.width / 2, cY + 20.dp.toPx()),
                strokeWidth = 1f
            )
            // Knob position
            val availableTrack = trackWidth - knobWidth
            val knobX = pad + animatedValue * availableTrack
            // Knob
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF888888), Color(0xFF555555), Color(0xFF444444))),
                topLeft = Offset(knobX, cY - knobHeight / 2),
                size = Size(knobWidth, knobHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
            )
            // Grip lines
            for (i in 0..3) {
                val lineX = knobX + knobWidth / 2 - 9.dp.toPx() + i * 6.dp.toPx()
                drawLine(
                    color = Color(0xFF222222),
                    start = Offset(lineX, cY - 8.dp.toPx()),
                    end = Offset(lineX, cY + 8.dp.toPx()),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
// ============================================
// LIBRARY SHEET
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackLibrarySheet(
    djEngine: DJEnginePro,
    selectedDeck: Int,
    onDismiss: () -> Unit,
    onLoadTrack: (DeviceTrackPro) -> Unit
) {
    var tracks by remember { mutableStateOf<List<DeviceTrackPro>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        tracks = djEngine.getDeviceMusic()
        loading = false
    }
    val filtered = remember(tracks, search) {
        if (search.isBlank()) tracks
        else tracks.filter { it.title.contains(search, true) || it.artist.contains(search, true) }
    }
    val deckColor = if (selectedDeck == 1) DJColors.deckA else DJColors.deckB
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DJColors.bgMid) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("LOAD TO DECK ${if (selectedDeck == 1) "A" else "B"}", color = deckColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${filtered.size} tracks", color = DJColors.textGray, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search...", color = DJColors.textGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = deckColor,
                    unfocusedBorderColor = DJColors.textDim,
                    focusedTextColor = Color.White
                ),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = DJColors.textGray) },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            if (loading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = deckColor)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.height(350.dp)) {
                    items(filtered) { track ->
                        Surface(
                            onClick = { onLoadTrack(track) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = DJColors.surface
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(36.dp).background(deckColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MusicNote, null, tint = deckColor, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(track.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(track.artist, color = DJColors.textGray, fontSize = 10.sp, maxLines = 1)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${track.bpm.toInt()} BPM", color = deckColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text(formatTime(track.duration), color = DJColors.textGray, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
private fun formatTime(ms: Long): String {
    val s = (ms / 1000) % 60
    val m = ms / 60000
    return "$m:${s.toString().padStart(2, '0')}"
}
