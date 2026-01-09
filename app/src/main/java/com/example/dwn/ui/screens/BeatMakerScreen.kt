package com.example.dwn.ui.screens

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

// ============================================
// 🎵 BEAT MAKER THEME COLORS
// ============================================

private object BeatColors {
    // Primary
    val neonPink = Color(0xFFFF1493)
    val neonBlue = Color(0xFF00BFFF)
    val neonPurple = Color(0xFFDA70D6)
    val neonGreen = Color(0xFF00FF7F)
    val neonOrange = Color(0xFFFF6B35)
    val neonCyan = Color(0xFF00FFFF)
    val neonYellow = Color(0xFFFFE135)
    val neonRed = Color(0xFFFF3333)

    // Backgrounds
    val bgDark = Color(0xFF0A0A0F)
    val bgMid = Color(0xFF12121A)
    val surface = Color(0xFF1A1A25)
    val surfaceElevated = Color(0xFF22222E)
    val card = Color(0xFF1E1E28)

    // Track colors
    val drumColor = Color(0xFFFF6B35)
    val bassColor = Color(0xFF00BFFF)
    val synthColor = Color(0xFFDA70D6)
    val padColor = Color(0xFF00FF7F)
    val vocalColor = Color(0xFFFFE135)
    val fxColor = Color(0xFFFF1493)

    // Text
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B0)
    val textMuted = Color(0xFF606060)

    // Glass
    val glassWhite = Color(0x14FFFFFF)
    val glassBorder = Color(0x25FFFFFF)
}

// ============================================
// 🎹 BEAT MAKER DATA CLASSES
// ============================================

data class Track(
    val id: Int,
    val name: String,
    val type: TrackType,
    val color: Color,
    val steps: MutableList<Boolean>,
    var volume: Float = 0.8f,
    var pan: Float = 0f,
    var muted: Boolean = false,
    var solo: Boolean = false
)

enum class TrackType {
    DRUMS, BASS, SYNTH, PAD, VOCAL, FX
}

data class DrumKit(
    val name: String,
    val sounds: List<DrumSound>
)

data class DrumSound(
    val name: String,
    val resourceId: Int = 0
)

data class BeatPattern(
    val id: Int,
    val name: String,
    val bpm: Int,
    val tracks: List<Track>
)

// ============================================
// 🎛️ BEAT MAKER SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeatMakerScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Beat state
    var bpm by remember { mutableIntStateOf(120) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentStep by remember { mutableIntStateOf(0) }
    var swing by remember { mutableFloatStateOf(0f) }
    var masterVolume by remember { mutableFloatStateOf(0.8f) }

    // Track management
    val tracks = remember {
        mutableStateListOf(
            Track(1, "Kick", TrackType.DRUMS, BeatColors.drumColor, MutableList(16) { false }),
            Track(2, "Snare", TrackType.DRUMS, BeatColors.drumColor, MutableList(16) { false }),
            Track(3, "Hi-Hat", TrackType.DRUMS, BeatColors.drumColor, MutableList(16) { false }),
            Track(4, "Clap", TrackType.DRUMS, BeatColors.drumColor, MutableList(16) { false }),
            Track(5, "Bass", TrackType.BASS, BeatColors.bassColor, MutableList(16) { false }),
            Track(6, "Synth 1", TrackType.SYNTH, BeatColors.synthColor, MutableList(16) { false }),
            Track(7, "Pad", TrackType.PAD, BeatColors.padColor, MutableList(16) { false }),
            Track(8, "FX", TrackType.FX, BeatColors.fxColor, MutableList(16) { false })
        )
    }

    // UI state
    var selectedTrackIndex by remember { mutableIntStateOf(0) }
    var showMixer by remember { mutableStateOf(false) }
    var showFX by remember { mutableStateOf(false) }
    var showPatterns by remember { mutableStateOf(false) }
    var showSamples by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(ViewMode.SEQUENCER) }

    // Animation
    val infiniteTransition = rememberInfiniteTransition(label = "beat")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000 / bpm, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Playback loop
    LaunchedEffect(isPlaying, bpm) {
        if (isPlaying) {
            while (isPlaying) {
                delay((60000L / bpm / 4))
                currentStep = (currentStep + 1) % 16
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BeatColors.bgDark)
    ) {
        // Animated background
        BeatMakerBackground(isPlaying = isPlaying, currentStep = currentStep, bpm = bpm)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            BeatMakerTopBar(
                bpm = bpm,
                onBpmChange = { bpm = it },
                isPlaying = isPlaying,
                onPlayPause = {
                    isPlaying = !isPlaying
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onStop = {
                    isPlaying = false
                    currentStep = 0
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onNavigateBack = onNavigateBack,
                masterVolume = masterVolume,
                onMasterVolumeChange = { masterVolume = it }
            )

            // Mode Tabs
            ViewModeTabs(
                selectedMode = viewMode,
                onModeSelect = { viewMode = it }
            )

            // Main Content
            when (viewMode) {
                ViewMode.SEQUENCER -> {
                    SequencerView(
                        tracks = tracks,
                        currentStep = currentStep,
                        isPlaying = isPlaying,
                        selectedTrackIndex = selectedTrackIndex,
                        onTrackSelect = { selectedTrackIndex = it },
                        onStepToggle = { trackIdx, stepIdx ->
                            tracks[trackIdx].steps[stepIdx] = !tracks[trackIdx].steps[stepIdx]
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                ViewMode.PADS -> {
                    DrumPadsView(
                        onPadTap = { padIndex ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            // Trigger sound
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                ViewMode.PIANO -> {
                    PianoRollView(
                        selectedTrack = tracks.getOrNull(selectedTrackIndex),
                        modifier = Modifier.weight(1f)
                    )
                }
                ViewMode.MIXER -> {
                    MixerView(
                        tracks = tracks,
                        masterVolume = masterVolume,
                        onMasterVolumeChange = { masterVolume = it },
                        onTrackVolumeChange = { idx, vol ->
                            tracks[idx] = tracks[idx].copy(volume = vol)
                        },
                        onTrackPanChange = { idx, pan ->
                            tracks[idx] = tracks[idx].copy(pan = pan)
                        },
                        onTrackMuteToggle = { idx ->
                            tracks[idx] = tracks[idx].copy(muted = !tracks[idx].muted)
                        },
                        onTrackSoloToggle = { idx ->
                            tracks[idx] = tracks[idx].copy(solo = !tracks[idx].solo)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                ViewMode.FX -> {
                    FXView(
                        selectedTrack = tracks.getOrNull(selectedTrackIndex),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Bottom Controls
            BeatMakerBottomBar(
                swing = swing,
                onSwingChange = { swing = it },
                onAddTrack = {
                    val newId = tracks.size + 1
                    tracks.add(Track(newId, "Track $newId", TrackType.SYNTH, BeatColors.synthColor, MutableList(16) { false }))
                },
                onClearPattern = {
                    tracks.forEach { track ->
                        for (i in track.steps.indices) {
                            track.steps[i] = false
                        }
                    }
                },
                onExport = { /* Export functionality */ },
                onSave = { /* Save pattern */ }
            )
        }
    }
}

enum class ViewMode {
    SEQUENCER, PADS, PIANO, MIXER, FX
}

// ============================================
// 🌈 ANIMATED BACKGROUND
// ============================================

@Composable
private fun BeatMakerBackground(
    isPlaying: Boolean,
    currentStep: Int,
    bpm: Int
) {
    val transition = rememberInfiniteTransition(label = "bg")

    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val beatPulse by animateFloatAsState(
        targetValue = if (isPlaying && currentStep % 4 == 0) 1.3f else 1f,
        animationSpec = tween(100),
        label = "beatPulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Base gradient
        drawRect(
            brush = Brush.verticalGradient(
                0f to BeatColors.bgDark,
                0.5f to BeatColors.bgMid,
                1f to BeatColors.bgDark
            )
        )

        // Beat reactive orbs
        if (isPlaying) {
            // Center beat orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        BeatColors.neonPink.copy(alpha = 0.3f * beatPulse),
                        BeatColors.neonPink.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.3f),
                    radius = 200f * beatPulse
                ),
                center = Offset(w * 0.5f, h * 0.3f),
                radius = 200f * beatPulse
            )
        }

        // Floating orbs
        val orb1X = w * 0.2f + cos(Math.toRadians(time.toDouble())).toFloat() * 50f
        val orb1Y = h * 0.2f + sin(Math.toRadians(time.toDouble())).toFloat() * 30f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    BeatColors.neonBlue.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = Offset(orb1X, orb1Y),
                radius = 150f
            ),
            center = Offset(orb1X, orb1Y),
            radius = 150f
        )

        val orb2X = w * 0.8f + cos(Math.toRadians(time * 0.7 + 180).toDouble()).toFloat() * 40f
        val orb2Y = h * 0.7f + sin(Math.toRadians(time * 0.5 + 90).toDouble()).toFloat() * 50f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    BeatColors.neonPurple.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(orb2X, orb2Y),
                radius = 180f
            ),
            center = Offset(orb2X, orb2Y),
            radius = 180f
        )

        // Grid lines
        val gridAlpha = 0.03f
        for (i in 0..20) {
            drawLine(
                color = Color.White.copy(alpha = gridAlpha),
                start = Offset(0f, h * i / 20f),
                end = Offset(w, h * i / 20f),
                strokeWidth = 1f
            )
            drawLine(
                color = Color.White.copy(alpha = gridAlpha),
                start = Offset(w * i / 20f, 0f),
                end = Offset(w * i / 20f, h),
                strokeWidth = 1f
            )
        }
    }
}

// ============================================
// 🎛️ TOP BAR
// ============================================

@Composable
private fun BeatMakerTopBar(
    bpm: Int,
    onBpmChange: (Int) -> Unit,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onNavigateBack: () -> Unit,
    masterVolume: Float,
    onMasterVolumeChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(onClick = onNavigateBack) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = BeatColors.textPrimary
            )
        }

        // Title
        Text(
            "BEAT MAKER",
            color = BeatColors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // BPM Control
        BpmControl(
            bpm = bpm,
            onBpmChange = onBpmChange,
            modifier = Modifier.padding(end = 12.dp)
        )

        // Transport Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stop
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        BeatColors.surface,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = BeatColors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Play/Pause
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isPlaying)
                                listOf(BeatColors.neonOrange, BeatColors.neonRed)
                            else
                                listOf(BeatColors.neonGreen, BeatColors.neonCyan)
                        ),
                        CircleShape
                    )
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun BpmControl(
    bpm: Int,
    onBpmChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(BeatColors.surface, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = { if (bpm > 40) onBpmChange(bpm - 1) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "Decrease BPM",
                tint = BeatColors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$bpm",
                color = BeatColors.neonGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "BPM",
                color = BeatColors.textMuted,
                fontSize = 9.sp
            )
        }

        IconButton(
            onClick = { if (bpm < 300) onBpmChange(bpm + 1) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Increase BPM",
                tint = BeatColors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ============================================
// 📑 VIEW MODE TABS
// ============================================

@Composable
private fun ViewModeTabs(
    selectedMode: ViewMode,
    onModeSelect: (ViewMode) -> Unit
) {
    val modes = listOf(
        ViewMode.SEQUENCER to "Sequencer",
        ViewMode.PADS to "Pads",
        ViewMode.PIANO to "Piano",
        ViewMode.MIXER to "Mixer",
        ViewMode.FX to "FX"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(modes) { (mode, label) ->
            val isSelected = selectedMode == mode

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected)
                            Brush.linearGradient(listOf(BeatColors.neonPink, BeatColors.neonPurple))
                        else
                            Brush.linearGradient(listOf(BeatColors.surface, BeatColors.surface))
                    )
                    .clickable { onModeSelect(mode) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    label,
                    color = if (isSelected) Color.White else BeatColors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

// ============================================
// 🎹 SEQUENCER VIEW
// ============================================

@Composable
private fun SequencerView(
    tracks: List<Track>,
    currentStep: Int,
    isPlaying: Boolean,
    selectedTrackIndex: Int,
    onTrackSelect: (Int) -> Unit,
    onStepToggle: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Step indicators
        StepIndicator(currentStep = currentStep, totalSteps = 16)

        // Tracks
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(tracks) { index, track ->
                TrackRow(
                    track = track,
                    trackIndex = index,
                    currentStep = currentStep,
                    isPlaying = isPlaying,
                    isSelected = index == selectedTrackIndex,
                    onSelect = { onTrackSelect(index) },
                    onStepToggle = { stepIdx -> onStepToggle(index, stepIdx) }
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 80.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(totalSteps) { step ->
            val isCurrentStep = step == currentStep
            val isBeat = step % 4 == 0

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .background(
                        when {
                            isCurrentStep -> BeatColors.neonGreen
                            isBeat -> BeatColors.surface
                            else -> BeatColors.bgMid
                        },
                        RoundedCornerShape(2.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${step + 1}",
                    color = if (isCurrentStep) Color.Black else BeatColors.textMuted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    trackIndex: Int,
    currentStep: Int,
    isPlaying: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onStepToggle: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) BeatColors.surface else BeatColors.bgMid
            )
            .clickable { onSelect() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track label
        Box(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
                .background(track.color.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    track.name,
                    color = track.color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (track.muted) {
                        Text("M", color = BeatColors.neonRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    if (track.solo) {
                        Text("S", color = BeatColors.neonYellow, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Steps
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(16) { step ->
                val isActive = track.steps[step]
                val isCurrentStep = step == currentStep && isPlaying
                val isBeatDivision = step % 4 == 0

                val stepColor by animateColorAsState(
                    targetValue = when {
                        isActive && isCurrentStep -> track.color
                        isActive -> track.color.copy(alpha = 0.7f)
                        isCurrentStep -> BeatColors.neonGreen.copy(alpha = 0.3f)
                        isBeatDivision -> BeatColors.surface
                        else -> BeatColors.bgDark
                    },
                    animationSpec = tween(50),
                    label = "stepColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(stepColor)
                        .then(
                            if (isActive && isCurrentStep) {
                                Modifier.shadow(4.dp, RoundedCornerShape(4.dp), spotColor = track.color)
                            } else Modifier
                        )
                        .clickable { onStepToggle(step) }
                )
            }
        }
    }
}

// ============================================
// 🥁 DRUM PADS VIEW
// ============================================

@Composable
private fun DrumPadsView(
    onPadTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val pads = listOf(
        "KICK" to BeatColors.drumColor,
        "SNARE" to BeatColors.drumColor,
        "HI-HAT" to BeatColors.drumColor,
        "CLAP" to BeatColors.drumColor,
        "TOM 1" to BeatColors.neonOrange,
        "TOM 2" to BeatColors.neonOrange,
        "CRASH" to BeatColors.neonYellow,
        "RIDE" to BeatColors.neonYellow,
        "PERC 1" to BeatColors.synthColor,
        "PERC 2" to BeatColors.synthColor,
        "FX 1" to BeatColors.fxColor,
        "FX 2" to BeatColors.fxColor,
        "BASS 1" to BeatColors.bassColor,
        "BASS 2" to BeatColors.bassColor,
        "SYNTH 1" to BeatColors.padColor,
        "SYNTH 2" to BeatColors.padColor
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        itemsIndexed(pads) { index, (name, color) ->
            DrumPad(
                name = name,
                color = color,
                onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPadTap(index)
                }
            )
        }
    }
}

@Composable
private fun DrumPad(
    name: String,
    color: Color,
    onTap: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "scale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 0.3f,
        animationSpec = tween(50),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 16.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = color.copy(alpha = glowAlpha)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = if (isPressed) 0.9f else 0.6f),
                        color.copy(alpha = if (isPressed) 0.7f else 0.3f)
                    )
                )
            )
            .border(
                2.dp,
                color.copy(alpha = glowAlpha),
                RoundedCornerShape(16.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onTap()
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================
// 🎹 PIANO ROLL VIEW
// ============================================

@Composable
private fun PianoRollView(
    selectedTrack: Track?,
    modifier: Modifier = Modifier
) {
    val notes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val octaves = listOf(4, 3, 2)

    Column(modifier = modifier) {
        Text(
            "Piano Roll - ${selectedTrack?.name ?: "No Track"}",
            color = BeatColors.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(12.dp)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // Piano keys
            Column(
                modifier = Modifier
                    .width(50.dp)
                    .fillMaxHeight()
                    .background(BeatColors.bgMid)
            ) {
                octaves.forEach { octave ->
                    notes.reversed().forEach { note ->
                        val isBlack = note.contains("#")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(
                                    if (isBlack) Color(0xFF222222) else Color(0xFFEEEEEE)
                                )
                                .border(0.5.dp, BeatColors.bgDark),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                "$note$octave",
                                color = if (isBlack) Color.White else Color.Black,
                                fontSize = 8.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            // Grid
            LazyRow(modifier = Modifier.fillMaxSize()) {
                items(64) { step ->
                    Column {
                        octaves.forEach { _ ->
                            notes.reversed().forEach { note ->
                                val isBlack = note.contains("#")
                                val isBeat = step % 4 == 0

                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(20.dp)
                                        .background(
                                            when {
                                                isBeat -> BeatColors.surface
                                                isBlack -> BeatColors.bgDark.copy(alpha = 0.8f)
                                                else -> BeatColors.bgDark
                                            }
                                        )
                                        .border(0.5.dp, BeatColors.bgMid.copy(alpha = 0.5f))
                                        .clickable { /* Add note */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// 🎚️ MIXER VIEW
// ============================================

@Composable
private fun MixerView(
    tracks: List<Track>,
    masterVolume: Float,
    onMasterVolumeChange: (Float) -> Unit,
    onTrackVolumeChange: (Int, Float) -> Unit,
    onTrackPanChange: (Int, Float) -> Unit,
    onTrackMuteToggle: (Int) -> Unit,
    onTrackSoloToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(12.dp)) {
        Text(
            "MIXER",
            color = BeatColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Track channels
            tracks.forEachIndexed { index, track ->
                MixerChannel(
                    track = track,
                    onVolumeChange = { onTrackVolumeChange(index, it) },
                    onPanChange = { onTrackPanChange(index, it) },
                    onMuteToggle = { onTrackMuteToggle(index) },
                    onSoloToggle = { onTrackSoloToggle(index) }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Master channel
            MasterChannel(
                volume = masterVolume,
                onVolumeChange = onMasterVolumeChange
            )
        }
    }
}

@Composable
private fun MixerChannel(
    track: Track,
    onVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onSoloToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(BeatColors.surface)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Track name
        Text(
            track.name,
            color = track.color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Pan knob
        MiniKnob(
            value = track.pan,
            onValueChange = onPanChange,
            label = "PAN",
            color = track.color
        )

        Spacer(modifier = Modifier.weight(1f))

        // Volume fader
        VerticalSlider(
            value = track.volume,
            onValueChange = onVolumeChange,
            color = track.color,
            modifier = Modifier
                .height(120.dp)
                .width(32.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Mute/Solo
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (track.muted) BeatColors.neonRed else BeatColors.bgDark)
                    .clickable { onMuteToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text("M", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (track.solo) BeatColors.neonYellow else BeatColors.bgDark)
                    .clickable { onSoloToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text("S", color = if (track.solo) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MasterChannel(
    volume: Float,
    onVolumeChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(BeatColors.neonPink.copy(alpha = 0.2f), BeatColors.surface)
                )
            )
            .border(1.dp, BeatColors.neonPink.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "MASTER",
            color = BeatColors.neonPink,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        // Master fader
        VerticalSlider(
            value = volume,
            onValueChange = onVolumeChange,
            color = BeatColors.neonPink,
            modifier = Modifier
                .height(150.dp)
                .width(36.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "${(volume * 100).toInt()}%",
            color = BeatColors.neonPink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MiniKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(BeatColors.bgDark)
                .border(2.dp, color.copy(alpha = 0.5f), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount.y / 200f
                        onValueChange((value + delta).coerceIn(-1f, 1f))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Knob indicator
            Canvas(modifier = Modifier.size(24.dp)) {
                val angle = -135f + (value + 1f) / 2f * 270f
                val rad = Math.toRadians(angle.toDouble())
                val indicatorLength = size.minDimension / 2 * 0.6f

                drawLine(
                    color = color,
                    start = center,
                    end = Offset(
                        center.x + cos(rad).toFloat() * indicatorLength,
                        center.y + sin(rad).toFloat() * indicatorLength
                    ),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }
        Text(
            label,
            color = BeatColors.textMuted,
            fontSize = 8.sp
        )
    }
}

@Composable
private fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BeatColors.bgDark)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val newValue = 1f - (change.position.y / size.height)
                    onValueChange(newValue.coerceIn(0f, 1f))
                }
            }
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(value)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.5f))
                    ),
                    RoundedCornerShape(8.dp)
                )
        )
    }
}

// ============================================
// 🎛️ FX VIEW
// ============================================

@Composable
private fun FXView(
    selectedTrack: Track?,
    modifier: Modifier = Modifier
) {
    val fxList = listOf(
        "EQ" to Icons.Default.Equalizer,
        "Compressor" to Icons.Default.Compress,
        "Reverb" to Icons.Default.Waves,
        "Delay" to Icons.Default.Timer,
        "Distortion" to Icons.Default.Bolt,
        "Chorus" to Icons.Default.Layers,
        "Filter" to Icons.Default.FilterAlt,
        "Limiter" to Icons.Default.VerticalAlignTop
    )

    var selectedFx by remember { mutableStateOf("EQ") }

    Column(modifier = modifier.padding(12.dp)) {
        Text(
            "FX - ${selectedTrack?.name ?: "Master"}",
            color = BeatColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // FX selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(fxList) { (name, icon) ->
                val isSelected = selectedFx == name

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) BeatColors.neonPurple.copy(alpha = 0.3f)
                            else BeatColors.surface
                        )
                        .border(
                            1.dp,
                            if (isSelected) BeatColors.neonPurple else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedFx = name }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            icon,
                            contentDescription = name,
                            tint = if (isSelected) BeatColors.neonPurple else BeatColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            name,
                            color = if (isSelected) BeatColors.neonPurple else BeatColors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // FX Controls
        when (selectedFx) {
            "EQ" -> EQControls()
            "Compressor" -> CompressorControls()
            "Reverb" -> ReverbControls()
            "Delay" -> DelayControls()
            else -> GenericFXControls(selectedFx)
        }
    }
}

@Composable
private fun EQControls() {
    var low by remember { mutableFloatStateOf(0f) }
    var mid by remember { mutableFloatStateOf(0f) }
    var high by remember { mutableFloatStateOf(0f) }

    Column {
        Text("3-Band EQ", color = BeatColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FXKnob(value = low, onValueChange = { low = it }, label = "LOW", color = BeatColors.neonOrange)
            FXKnob(value = mid, onValueChange = { mid = it }, label = "MID", color = BeatColors.neonGreen)
            FXKnob(value = high, onValueChange = { high = it }, label = "HIGH", color = BeatColors.neonBlue)
        }
    }
}

@Composable
private fun CompressorControls() {
    var threshold by remember { mutableFloatStateOf(-20f) }
    var ratio by remember { mutableFloatStateOf(4f) }
    var attack by remember { mutableFloatStateOf(10f) }
    var release by remember { mutableFloatStateOf(100f) }

    Column {
        Text("Compressor", color = BeatColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FXKnob(value = (threshold + 60) / 60, onValueChange = { threshold = it * 60 - 60 }, label = "THRESH", color = BeatColors.neonRed)
            FXKnob(value = ratio / 20, onValueChange = { ratio = it * 20 }, label = "RATIO", color = BeatColors.neonYellow)
            FXKnob(value = attack / 100, onValueChange = { attack = it * 100 }, label = "ATK", color = BeatColors.neonGreen)
            FXKnob(value = release / 500, onValueChange = { release = it * 500 }, label = "REL", color = BeatColors.neonBlue)
        }
    }
}

@Composable
private fun ReverbControls() {
    var roomSize by remember { mutableFloatStateOf(0.5f) }
    var damping by remember { mutableFloatStateOf(0.5f) }
    var wetDry by remember { mutableFloatStateOf(0.3f) }
    var preDelay by remember { mutableFloatStateOf(0.1f) }

    Column {
        Text("Reverb", color = BeatColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FXKnob(value = roomSize, onValueChange = { roomSize = it }, label = "ROOM", color = BeatColors.neonCyan)
            FXKnob(value = damping, onValueChange = { damping = it }, label = "DAMP", color = BeatColors.neonPurple)
            FXKnob(value = wetDry, onValueChange = { wetDry = it }, label = "MIX", color = BeatColors.neonPink)
            FXKnob(value = preDelay, onValueChange = { preDelay = it }, label = "PRE", color = BeatColors.neonOrange)
        }
    }
}

@Composable
private fun DelayControls() {
    var time by remember { mutableFloatStateOf(0.25f) }
    var feedback by remember { mutableFloatStateOf(0.3f) }
    var mix by remember { mutableFloatStateOf(0.3f) }

    Column {
        Text("Delay", color = BeatColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FXKnob(value = time, onValueChange = { time = it }, label = "TIME", color = BeatColors.neonBlue)
            FXKnob(value = feedback, onValueChange = { feedback = it }, label = "FDBK", color = BeatColors.neonGreen)
            FXKnob(value = mix, onValueChange = { mix = it }, label = "MIX", color = BeatColors.neonPink)
        }
    }
}

@Composable
private fun GenericFXControls(fxName: String) {
    var param1 by remember { mutableFloatStateOf(0.5f) }
    var param2 by remember { mutableFloatStateOf(0.5f) }
    var param3 by remember { mutableFloatStateOf(0.5f) }

    Column {
        Text(fxName, color = BeatColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FXKnob(value = param1, onValueChange = { param1 = it }, label = "AMOUNT", color = BeatColors.neonPink)
            FXKnob(value = param2, onValueChange = { param2 = it }, label = "TONE", color = BeatColors.neonBlue)
            FXKnob(value = param3, onValueChange = { param3 = it }, label = "MIX", color = BeatColors.neonGreen)
        }
    }
}

@Composable
private fun FXKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BeatColors.surface,
                            BeatColors.bgDark
                        )
                    )
                )
                .border(3.dp, color.copy(alpha = 0.4f), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount.y / 300f
                        onValueChange((value + delta).coerceIn(0f, 1f))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(52.dp)) {
                // Background arc
                drawArc(
                    color = color.copy(alpha = 0.2f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )

                // Value arc
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = 270f * value,
                    useCenter = false,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )

                // Indicator dot
                val angle = Math.toRadians((135 + 270 * value).toDouble())
                val radius = size.minDimension / 2 - 8
                drawCircle(
                    color = color,
                    radius = 5f,
                    center = Offset(
                        center.x + cos(angle).toFloat() * radius,
                        center.y + sin(angle).toFloat() * radius
                    )
                )
            }

            // Value display
            Text(
                "${(value * 100).toInt()}",
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            label,
            color = BeatColors.textMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================
// 🎛️ BOTTOM BAR
// ============================================

@Composable
private fun BeatMakerBottomBar(
    swing: Float,
    onSwingChange: (Float) -> Unit,
    onAddTrack: () -> Unit,
    onClearPattern: () -> Unit,
    onExport: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BeatColors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Swing control
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "SWING",
                color = BeatColors.textMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = swing,
                onValueChange = onSwingChange,
                modifier = Modifier.width(80.dp),
                colors = SliderDefaults.colors(
                    thumbColor = BeatColors.neonGreen,
                    activeTrackColor = BeatColors.neonGreen,
                    inactiveTrackColor = BeatColors.bgDark
                )
            )
            Text(
                "${(swing * 100).toInt()}%",
                color = BeatColors.neonGreen,
                fontSize = 10.sp
            )
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BottomBarButton(
                icon = Icons.Default.Add,
                label = "Track",
                onClick = onAddTrack
            )
            BottomBarButton(
                icon = Icons.Default.Delete,
                label = "Clear",
                onClick = onClearPattern
            )
            BottomBarButton(
                icon = Icons.Default.Save,
                label = "Save",
                onClick = onSave,
                color = BeatColors.neonGreen
            )
            BottomBarButton(
                icon = Icons.Default.Upload,
                label = "Export",
                onClick = onExport,
                color = BeatColors.neonPink
            )
        }
    }
}

@Composable
private fun BottomBarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color = BeatColors.textSecondary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            label,
            color = color,
            fontSize = 9.sp
        )
    }
}

