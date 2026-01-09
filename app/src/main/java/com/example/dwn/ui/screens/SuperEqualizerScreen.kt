package com.example.dwn.ui.screens

import android.media.audiofx.PresetReverb
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.player.audio.*
import com.example.dwn.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.*

// ============================================
// THEME COLORS
// ============================================
private val EqOrange = Color(0xFFFF9800)
private val EqOrangeLight = Color(0xFFFFB74D)
private val EqOrangeDark = Color(0xFFE65100)
private val EqPurple = Color(0xFF9C27B0)
private val EqBlue = Color(0xFF2196F3)
private val EqGreen = Color(0xFF4CAF50)
private val EqRed = Color(0xFFE53935)
private val EqCyan = Color(0xFF00BCD4)
private val EqPink = Color(0xFFE91E63)

private val EqBackground = Color(0xFF0D0D0D)
private val EqSurface = Color(0xFF1A1A1A)
private val EqSurfaceLight = Color(0xFF252525)
private val EqTrackColor = Color(0xFF333333)

// ============================================
// MAIN SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SuperEqualizerScreen(
    superEqualizer: SuperEqualizer,
    onBack: () -> Unit
) {
    val state by superEqualizer.state.collectAsState()
    val graphicEQState by superEqualizer.graphicEQ.state.collectAsState()
    val presets by superEqualizer.presetManager.presets.collectAsState()
    val currentPreset by superEqualizer.presetManager.currentPreset.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()

    val tabs = listOf("EQ", "FX", "SPATIAL", "PRESETS", "METERS")

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EqBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Header
            SuperEqHeader(
                isEnabled = state.isEnabled,
                onEnabledChange = { superEqualizer.setEnabled(it) },
                onBack = onBack,
                onUndo = { superEqualizer.undo() },
                onRedo = { superEqualizer.redo() },
                canUndo = superEqualizer.undoRedo.canUndo.collectAsState().value,
                canRedo = superEqualizer.undoRedo.canRedo.collectAsState().value
            )

            // Preset Quick Select
            PresetQuickBar(
                currentPreset = currentPreset,
                presets = presets.take(10),
                onPresetSelect = { superEqualizer.applyPreset(it) }
            )

            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = EqOrange,
                edgePadding = 16.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                title,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (pagerState.currentPage == index) EqOrange else TextSecondary
                            )
                        }
                    )
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> EqualizerTab(superEqualizer, graphicEQState)
                    1 -> EffectsTab(superEqualizer)
                    2 -> SpatialTab(superEqualizer)
                    3 -> PresetsTab(superEqualizer, presets)
                    4 -> MetersTab(superEqualizer)
                }
            }

            // Bottom Quick Actions
            BottomQuickActions(
                currentMode = state.specialMode,
                onModeSelect = { superEqualizer.activateSpecialMode(it) },
                onReset = { superEqualizer.resetAll() }
            )
        }
    }
}

// ============================================
// HEADER
// ============================================

@Composable
private fun SuperEqHeader(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EqSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Super Equalizer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    if (isEnabled) "ACTIVE" else "DISABLED",
                    fontSize = 10.sp,
                    color = if (isEnabled) EqGreen else TextSecondary
                )
            }

            // Undo/Redo
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    "Undo",
                    tint = if (canUndo) Color.White else TextTertiary
                )
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    "Redo",
                    tint = if (canRedo) Color.White else TextTertiary
                )
            }

            // Power Toggle
            Switch(
                checked = isEnabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = EqOrange,
                    checkedTrackColor = EqOrange.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = EqTrackColor
                )
            )
        }
    }
}

// ============================================
// PRESET QUICK BAR
// ============================================

@Composable
private fun PresetQuickBar(
    currentPreset: AudioPreset?,
    presets: List<AudioPreset>,
    onPresetSelect: (AudioPreset) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets) { preset ->
            FilterChip(
                selected = preset.id == currentPreset?.id,
                onClick = { onPresetSelect(preset) },
                label = { Text(preset.name, fontSize = 12.sp, maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EqOrange,
                    selectedLabelColor = Color.White,
                    containerColor = EqSurface,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = EqTrackColor,
                    selectedBorderColor = EqOrange,
                    enabled = true,
                    selected = preset.id == currentPreset?.id
                )
            )
        }
    }
}

// ============================================
// EQUALIZER TAB
// ============================================

@Composable
private fun EqualizerTab(
    superEqualizer: SuperEqualizer,
    graphicEQState: GraphicEQState
) {
    var selectedMode by remember { mutableStateOf(graphicEQState.mode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // EQ Enable Row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = EqSurface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = if (graphicEQState.isEnabled) EqOrange else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Graphic Equalizer",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (graphicEQState.isEnabled) "Enabled" else "Disabled",
                            color = if (graphicEQState.isEnabled) EqGreen else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = graphicEQState.isEnabled,
                    onCheckedChange = { superEqualizer.graphicEQ.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EqOrange,
                        checkedTrackColor = EqOrange.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = EqTrackColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // EQ Mode Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mode", color = TextSecondary, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GraphicEQMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = {
                            selectedMode = mode
                            superEqualizer.graphicEQ.setMode(mode)
                        },
                        label = { Text(mode.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EqOrange,
                            selectedLabelColor = Color.White,
                            containerColor = EqSurfaceLight,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Large EQ Graph - Takes most of the space
        GraphicEQBands(
            bands = graphicEQState.bands,
            gainRange = graphicEQState.gainRange,
            isEnabled = graphicEQState.isEnabled,
            onBandChange = { index, gain -> superEqualizer.graphicEQ.setBandGain(index, gain) },
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Controls - Glassmorphic Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flat button
                TextButton(onClick = { superEqualizer.graphicEQ.resetToFlat() }) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("FLAT", fontSize = 12.sp)
                }

                // Preamp Knob in center - compact version
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "PREAMP",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    val preampValue = graphicEQState.preampGain
                    var knobSizePx by remember { mutableFloatStateOf(0f) }
                    var dragStartAngle by remember { mutableFloatStateOf(0f) }
                    var dragStartValue by remember { mutableFloatStateOf(0f) }

                    // Mini preamp knob with touch
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .onSizeChanged { knobSizePx = it.width.toFloat() }
                            .pointerInput(graphicEQState.isEnabled) {
                                if (graphicEQState.isEnabled) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val centerX = knobSizePx / 2
                                            val centerY = knobSizePx / 2
                                            dragStartAngle = atan2(
                                                offset.x - centerX,
                                                centerY - offset.y
                                            ) * (180f / PI.toFloat())
                                            dragStartValue = preampValue
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val centerX = knobSizePx / 2
                                            val centerY = knobSizePx / 2
                                            val currentAngle = atan2(
                                                change.position.x - centerX,
                                                centerY - change.position.y
                                            ) * (180f / PI.toFloat())

                                            var angleDelta = currentAngle - dragStartAngle
                                            if (angleDelta > 180) angleDelta -= 360
                                            if (angleDelta < -180) angleDelta += 360

                                            val valueDelta = (angleDelta / 270f) * 24f
                                            val newValue = (dragStartValue + valueDelta).coerceIn(-12f, 12f)
                                            superEqualizer.graphicEQ.setPreampGain(newValue)

                                            dragStartAngle = currentAngle
                                            dragStartValue = newValue
                                        }
                                    )
                                }
                            }
                            .pointerInput(graphicEQState.isEnabled) {
                                if (graphicEQState.isEnabled) {
                                    detectTapGestures(onDoubleTap = {
                                        superEqualizer.graphicEQ.setPreampGain(0f)
                                    })
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val normalizedValue = (preampValue + 12f) / 24f
                        val rotationAngle = (normalizedValue - 0.5f) * 270f

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val outerRadius = size.minDimension / 2 - 2.dp.toPx()
                            val knobRadius = outerRadius * 0.7f

                            // Outer track
                            drawCircle(
                                color = Color(0xFF1A1A1A),
                                radius = outerRadius,
                                center = center
                            )

                            // Value arc background
                            drawArc(
                                color = EqTrackColor,
                                startAngle = -225f,
                                sweepAngle = 270f,
                                useCenter = false,
                                topLeft = Offset(center.x - outerRadius + 3.dp.toPx(), center.y - outerRadius + 3.dp.toPx()),
                                size = Size((outerRadius - 3.dp.toPx()) * 2, (outerRadius - 3.dp.toPx()) * 2),
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Active arc
                            if (normalizedValue > 0.01f) {
                                drawArc(
                                    color = EqOrange,
                                    startAngle = -225f,
                                    sweepAngle = normalizedValue * 270f,
                                    useCenter = false,
                                    topLeft = Offset(center.x - outerRadius + 3.dp.toPx(), center.y - outerRadius + 3.dp.toPx()),
                                    size = Size((outerRadius - 3.dp.toPx()) * 2, (outerRadius - 3.dp.toPx()) * 2),
                                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // Knob body
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF3A3A3A), Color(0xFF252525), Color(0xFF1A1A1A)),
                                    center = Offset(center.x - 2.dp.toPx(), center.y - 2.dp.toPx())
                                ),
                                radius = knobRadius,
                                center = center
                            )

                            // Pointer
                            val pointerAngle = (rotationAngle - 90f) * (PI.toFloat() / 180f)
                            val pointerEnd = Offset(
                                center.x + cos(pointerAngle) * (knobRadius * 0.65f),
                                center.y + sin(pointerAngle) * (knobRadius * 0.65f)
                            )

                            drawLine(
                                color = EqOrange,
                                start = center,
                                end = pointerEnd,
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawCircle(color = EqOrange, radius = 3.dp.toPx(), center = pointerEnd)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${if (preampValue >= 0) "+" else ""}${preampValue.toInt()}dB",
                        color = EqOrange,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Auto EQ
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Auto EQ",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = superEqualizer.smartProcessor.autoEQEnabled.collectAsState().value,
                        onCheckedChange = { superEqualizer.enableAutoEQ(it) },
                        modifier = Modifier.height(24.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EqBlue,
                            checkedTrackColor = EqBlue.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GraphicEQBands(
    bands: List<GraphicEQBand>,
    gainRange: Float,
    isEnabled: Boolean,
    onBandChange: (Int, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var containerWidth by remember { mutableFloatStateOf(0f) }
    val bandCount = bands.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged {
                containerWidth = it.width.toFloat()
            }
    ) {
        // Large EQ Response Curve - Takes most of the space
        if (bands.isNotEmpty() && containerWidth > 0) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)  // Takes all remaining vertical space
                    .clip(RoundedCornerShape(12.dp))
                    .background(EqSurface)
            ) {
                val bandWidth = size.width / bands.size
                val points = bands.mapIndexed { index, band ->
                    val normalizedGain = (band.gain + gainRange) / (gainRange * 2)
                    val x = bandWidth * index + bandWidth / 2
                    val y = size.height * (1 - normalizedGain)
                    Offset(x, y.coerceIn(16f, size.height - 16f))
                }

                // Draw horizontal grid lines
                val gridLines = listOf(0.25f, 0.5f, 0.75f)
                gridLines.forEach { ratio ->
                    drawLine(
                        color = EqTrackColor.copy(alpha = 0.3f),
                        start = Offset(0f, size.height * ratio),
                        end = Offset(size.width, size.height * ratio),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw center line (0 dB) with emphasis
                drawLine(
                    color = EqTrackColor,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Draw vertical grid lines for each band
                bands.forEachIndexed { index, _ ->
                    val x = bandWidth * index + bandWidth / 2
                    drawLine(
                        color = EqTrackColor.copy(alpha = 0.2f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw filled area under curve
                if (points.size >= 2) {
                    val fillPath = Path()
                    fillPath.moveTo(0f, size.height / 2)
                    fillPath.lineTo(points.first().x, points.first().y)

                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2
                        fillPath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }

                    fillPath.lineTo(size.width, size.height / 2)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                EqOrange.copy(alpha = 0.4f),
                                EqOrange.copy(alpha = 0.1f)
                            )
                        )
                    )

                    // Draw curve line
                    val linePath = Path()
                    linePath.moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2
                        linePath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                    drawPath(
                        path = linePath,
                        color = EqOrange,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Draw dots at each band position
                points.forEachIndexed { index, point ->
                    // Glow
                    drawCircle(
                        color = EqOrange.copy(alpha = 0.3f),
                        radius = 10.dp.toPx(),
                        center = point
                    )
                    // Main dot
                    drawCircle(
                        color = EqOrange,
                        radius = 6.dp.toPx(),
                        center = point
                    )
                    // Inner highlight
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = 2.dp.toPx(),
                        center = Offset(point.x - 1.dp.toPx(), point.y - 1.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // EQ Knobs Row - centered for 5 bands, scrollable for more
        if (bandCount <= 5) {
            // Center the knobs for 5 or fewer bands
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                bands.forEach { band ->
                    EQBandKnob(
                        band = band,
                        gainRange = gainRange,
                        isEnabled = isEnabled,
                        onValueChange = { onBandChange(band.index, it) },
                        modifier = Modifier.width(72.dp)
                    )
                }
            }
        } else {
            // Scrollable row for many bands
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(bands.size) { index ->
                    val band = bands[index]
                    EQBandKnob(
                        band = band,
                        gainRange = gainRange,
                        isEnabled = isEnabled,
                        onValueChange = { onBandChange(band.index, it) },
                        modifier = Modifier.width(84.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun EQBandKnob(
    band: GraphicEQBand,
    gainRange: Float,
    isEnabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Normalize gain from -gainRange..+gainRange to -135..+135 degrees rotation
    val normalizedGain = band.gain / gainRange  // -1 to +1

    // Animate the rotation smoothly
    val animatedRotation by animateFloatAsState(
        targetValue = normalizedGain * 135f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "knobRotation"
    )

    // Glow intensity based on gain amount
    val glowIntensity = abs(normalizedGain)
    val animatedGlow by animateFloatAsState(
        targetValue = glowIntensity,
        animationSpec = tween(200),
        label = "glow"
    )

    var knobSize by remember { mutableFloatStateOf(0f) }
    var dragStartAngle by remember { mutableFloatStateOf(0f) }
    var dragStartGain by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Frequency label at top with better styling
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (band.gain != 0f) EqOrange.copy(alpha = 0.2f) else Color.Transparent
        ) {
            Text(
                text = formatFrequency(band.frequency),
                color = if (band.gain != 0f) EqOrange else Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Rotary Knob - BIGGER
        Box(
            modifier = Modifier
                .size(72.dp)  // Fixed bigger size
                .onSizeChanged { knobSize = it.width.toFloat() }
                .pointerInput(isEnabled) {
                    if (isEnabled) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                val centerX = knobSize / 2
                                val centerY = knobSize / 2
                                dragStartAngle = atan2(
                                    offset.x - centerX,
                                    centerY - offset.y
                                ) * (180f / PI.toFloat())
                                dragStartGain = band.gain
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onDrag = { change, _ ->
                                change.consume()
                                val centerX = knobSize / 2
                                val centerY = knobSize / 2
                                val currentAngle = atan2(
                                    change.position.x - centerX,
                                    centerY - change.position.y
                                ) * (180f / PI.toFloat())

                                var angleDelta = currentAngle - dragStartAngle
                                if (angleDelta > 180) angleDelta -= 360
                                if (angleDelta < -180) angleDelta += 360

                                val gainDelta = (angleDelta / 135f) * gainRange
                                val newGain = (dragStartGain + gainDelta).coerceIn(-gainRange, gainRange)
                                onValueChange(newGain)

                                dragStartAngle = currentAngle
                                dragStartGain = newGain
                            }
                        )
                    }
                }
                .pointerInput(isEnabled) {
                    if (isEnabled) {
                        detectTapGestures(
                            onDoubleTap = { onValueChange(0f) }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Draw the knob with enhanced graphics
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val outerRadius = size.minDimension / 2 - 2.dp.toPx()
                val knobRadius = outerRadius * 0.72f
                val innerKnobRadius = knobRadius * 0.85f

                // Outer glow when active
                if (isEnabled && animatedGlow > 0.1f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                EqOrange.copy(alpha = animatedGlow * 0.4f),
                                EqOrange.copy(alpha = animatedGlow * 0.2f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = outerRadius * 1.2f
                        ),
                        radius = outerRadius * 1.2f,
                        center = center
                    )
                }

                // Outer track ring (darker background)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1A1A1A), Color(0xFF0D0D0D)),
                        center = center
                    ),
                    radius = outerRadius,
                    center = center
                )

                // Value arc - shows current gain level
                val arcStartAngle = -225f  // Start from bottom-left
                val arcSweepAngle = (normalizedGain + 1) * 135f  // Sweep based on value

                // Background arc (full range)
                drawArc(
                    color = EqTrackColor,
                    startAngle = -225f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius + 4.dp.toPx(), center.y - outerRadius + 4.dp.toPx()),
                    size = Size((outerRadius - 4.dp.toPx()) * 2, (outerRadius - 4.dp.toPx()) * 2),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Active arc (current value)
                if (abs(normalizedGain) > 0.01f) {
                    val activeArcStart = -90f  // Center (0 dB position)
                    val activeArcSweep = normalizedGain * 135f

                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = if (normalizedGain > 0) {
                                listOf(EqOrange, EqOrangeDark, EqOrange)
                            } else {
                                listOf(EqCyan, EqBlue, EqCyan)
                            }
                        ),
                        startAngle = activeArcStart,
                        sweepAngle = activeArcSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - outerRadius + 4.dp.toPx(), center.y - outerRadius + 4.dp.toPx()),
                        size = Size((outerRadius - 4.dp.toPx()) * 2, (outerRadius - 4.dp.toPx()) * 2),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Draw tick marks
                for (i in -6..6) {
                    val tickAngle = (i * 22.5f) - 90f
                    val angleRad = tickAngle * (PI.toFloat() / 180f)
                    val isCenter = i == 0
                    val isActive = (i > 0 && normalizedGain * 6 >= i) || (i < 0 && normalizedGain * 6 <= i)

                    val tickOuterRadius = outerRadius - 10.dp.toPx()
                    val tickInnerRadius = tickOuterRadius - (if (isCenter) 8.dp.toPx() else 5.dp.toPx())

                    val tickColor = when {
                        isCenter -> Color.White
                        isActive && normalizedGain > 0 -> EqOrange
                        isActive && normalizedGain < 0 -> EqCyan
                        else -> EqTrackColor.copy(alpha = 0.5f)
                    }

                    drawLine(
                        color = tickColor,
                        start = Offset(
                            center.x + cos(angleRad) * tickInnerRadius,
                            center.y + sin(angleRad) * tickInnerRadius
                        ),
                        end = Offset(
                            center.x + cos(angleRad) * tickOuterRadius,
                            center.y + sin(angleRad) * tickOuterRadius
                        ),
                        strokeWidth = if (isCenter) 2.5.dp.toPx() else 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Main knob body with 3D effect
                // Shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = knobRadius,
                    center = Offset(center.x + 2.dp.toPx(), center.y + 2.dp.toPx())
                )

                // Knob gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isEnabled) {
                            listOf(
                                Color(0xFF3A3A3A),
                                Color(0xFF252525),
                                Color(0xFF1A1A1A)
                            )
                        } else {
                            listOf(Color(0xFF333333), Color(0xFF222222))
                        },
                        center = Offset(center.x - 4.dp.toPx(), center.y - 4.dp.toPx()),
                        radius = knobRadius
                    ),
                    radius = knobRadius,
                    center = center
                )

                // Knob highlight rim
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f)
                        ),
                        start = Offset(center.x - knobRadius, center.y - knobRadius),
                        end = Offset(center.x + knobRadius, center.y + knobRadius)
                    ),
                    radius = knobRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Inner knob detail circle
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2A2A2A),
                            Color(0xFF1F1F1F)
                        ),
                        center = center
                    ),
                    radius = innerKnobRadius,
                    center = center
                )

                // Pointer/indicator
                val pointerAngle = (animatedRotation - 90f) * (PI.toFloat() / 180f)
                val pointerOuterLength = knobRadius * 0.75f
                val pointerInnerLength = knobRadius * 0.25f

                val pointerStart = Offset(
                    center.x + cos(pointerAngle) * pointerInnerLength,
                    center.y + sin(pointerAngle) * pointerInnerLength
                )
                val pointerEnd = Offset(
                    center.x + cos(pointerAngle) * pointerOuterLength,
                    center.y + sin(pointerAngle) * pointerOuterLength
                )

                // Pointer glow
                if (isEnabled) {
                    drawLine(
                        color = EqOrange.copy(alpha = 0.3f),
                        start = pointerStart,
                        end = pointerEnd,
                        strokeWidth = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Main pointer line
                drawLine(
                    color = if (isEnabled) EqOrange else Color.Gray,
                    start = pointerStart,
                    end = pointerEnd,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Pointer tip dot
                drawCircle(
                    color = if (isEnabled) EqOrange else Color.Gray,
                    radius = 4.dp.toPx(),
                    center = pointerEnd
                )

                // Center dot
                drawCircle(
                    color = if (isEnabled && isDragging) EqOrange else Color(0xFF444444),
                    radius = 4.dp.toPx(),
                    center = center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Current dB value with background
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = when {
                band.gain > 0 -> EqOrange.copy(alpha = 0.15f)
                band.gain < 0 -> EqCyan.copy(alpha = 0.15f)
                else -> Color.Transparent
            },
            border = BorderStroke(
                1.dp,
                when {
                    band.gain > 0 -> EqOrange.copy(alpha = 0.3f)
                    band.gain < 0 -> EqCyan.copy(alpha = 0.3f)
                    else -> EqTrackColor
                }
            )
        ) {
            Text(
                text = "${if (band.gain >= 0) "+" else ""}${band.gain.toInt()}dB",
                color = when {
                    band.gain > 0 -> EqOrange
                    band.gain < 0 -> EqCyan
                    else -> TextSecondary
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

// Keep the old slider as an alternative option
@Composable
private fun EQBandSlider(
    band: GraphicEQBand,
    gainRange: Float,
    isEnabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val normalizedGain = (band.gain + gainRange) / (gainRange * 2)
    var sliderHeight by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Column(
        modifier = modifier.padding(horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gain label
        Text(
            text = "+${gainRange.toInt()}",
            color = TextTertiary,
            fontSize = 7.sp
        )

        // Slider Track
        Box(
            modifier = Modifier
                .weight(1f)
                .width(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(EqTrackColor)
                .onSizeChanged { sliderHeight = it.height.toFloat() }
                .pointerInput(isEnabled) {
                    if (isEnabled) {
                        detectVerticalDragGestures { change, _ ->
                            change.consume()
                            val y = change.position.y
                            val newNormalized = 1f - (y / sliderHeight).coerceIn(0f, 1f)
                            val newGain = (newNormalized * gainRange * 2) - gainRange
                            onValueChange(newGain)
                        }
                    }
                }
                .pointerInput(isEnabled) {
                    if (isEnabled) {
                        detectTapGestures { offset ->
                            val newNormalized = 1f - (offset.y / sliderHeight).coerceIn(0f, 1f)
                            val newGain = (newNormalized * gainRange * 2) - gainRange
                            onValueChange(newGain)
                        }
                    }
                }
        ) {
            // Fill from center
            val fillHeight = abs(normalizedGain - 0.5f) * 2
            val isPositive = band.gain >= 0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fillHeight.coerceIn(0f, 1f) * 0.5f)
                    .align(if (isPositive) Alignment.Center else Alignment.Center)
                    .graphicsLayer {
                        translationY = if (isPositive) {
                            -(fillHeight * sliderHeight * 0.25f)
                        } else {
                            (fillHeight * sliderHeight * 0.25f)
                        }
                    }
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isEnabled) {
                                if (isPositive) listOf(EqOrange, EqOrangeDark)
                                else listOf(EqOrangeDark, EqOrange)
                            } else listOf(Color.Gray, Color.DarkGray)
                        ),
                        shape = RoundedCornerShape(3.dp)
                    )
            )

            // Thumb
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = (1 - normalizedGain) * (sliderHeight - with(density) { 14.dp.toPx() })
                    }
                    .background(if (isEnabled) EqOrange else Color.Gray, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }

        // Bottom gain label
        Text(
            text = "-${gainRange.toInt()}",
            color = TextTertiary,
            fontSize = 7.sp
        )

        // Frequency
        Text(
            text = formatFrequency(band.frequency),
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium
        )

        // Current dB
        Text(
            text = "${if (band.gain >= 0) "+" else ""}${band.gain.toInt()}",
            color = EqOrange,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatFrequency(freq: Int): String {
    return if (freq >= 1000) "${freq / 1000}k" else "$freq"
}

// ============================================
// GENERIC FX KNOB COMPONENT
// ============================================

@Composable
fun FXKnob(
    label: String,
    value: Float,
    minValue: Float,
    maxValue: Float,
    unit: String = "",
    accentColor: Color = EqOrange,
    isEnabled: Boolean = true,
    knobDiameter: Dp = 64.dp,
    onValueChange: (Float) -> Unit
) {
    val range = maxValue - minValue
    val normalizedValue = (value - minValue) / range  // 0 to 1

    val animatedRotation by animateFloatAsState(
        targetValue = (normalizedValue - 0.5f) * 270f,  // -135 to +135
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "knobRotation"
    )

    val glowIntensity = if (normalizedValue != 0.5f) abs(normalizedValue - 0.5f) * 2 else 0f
    val animatedGlow by animateFloatAsState(
        targetValue = glowIntensity,
        animationSpec = tween(150),
        label = "glow"
    )

    var knobSizePx by remember { mutableFloatStateOf(0f) }
    var dragStartAngle by remember { mutableFloatStateOf(0f) }
    var dragStartValue by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.width(knobDiameter + 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Label
        Text(
            text = label,
            color = if (isEnabled) Color.White else TextTertiary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Knob
        Box(
            modifier = Modifier
                .size(knobDiameter)
                .onSizeChanged { knobSizePx = it.width.toFloat() }
                .pointerInput(isEnabled) {
                    if (isEnabled) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                val centerX = knobSizePx / 2
                                val centerY = knobSizePx / 2
                                dragStartAngle = atan2(
                                    offset.x - centerX,
                                    centerY - offset.y
                                ) * (180f / PI.toFloat())
                                dragStartValue = value
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onDrag = { change, _ ->
                                change.consume()
                                val centerX = knobSizePx / 2
                                val centerY = knobSizePx / 2
                                val currentAngle = atan2(
                                    change.position.x - centerX,
                                    centerY - change.position.y
                                ) * (180f / PI.toFloat())

                                var angleDelta = currentAngle - dragStartAngle
                                if (angleDelta > 180) angleDelta -= 360
                                if (angleDelta < -180) angleDelta += 360

                                val valueDelta = (angleDelta / 270f) * range
                                val newValue = (dragStartValue + valueDelta).coerceIn(minValue, maxValue)
                                onValueChange(newValue)

                                dragStartAngle = currentAngle
                                dragStartValue = newValue
                            }
                        )
                    }
                }
                .pointerInput(isEnabled) {
                    if (isEnabled) {
                        detectTapGestures(
                            onDoubleTap = {
                                // Reset to center/default
                                val centerValue = (minValue + maxValue) / 2
                                onValueChange(if (minValue == 0f) minValue else centerValue)
                            }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val outerRadius = size.minDimension / 2 - 2.dp.toPx()
                val knobRadius = outerRadius * 0.7f

                // Glow
                if (isEnabled && animatedGlow > 0.1f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = animatedGlow * 0.4f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = outerRadius * 1.2f
                        ),
                        radius = outerRadius * 1.2f,
                        center = center
                    )
                }

                // Outer track
                drawCircle(
                    color = Color(0xFF1A1A1A),
                    radius = outerRadius,
                    center = center
                )

                // Value arc background
                drawArc(
                    color = EqTrackColor,
                    startAngle = -225f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius + 3.dp.toPx(), center.y - outerRadius + 3.dp.toPx()),
                    size = Size((outerRadius - 3.dp.toPx()) * 2, (outerRadius - 3.dp.toPx()) * 2),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Active arc
                if (normalizedValue > 0.01f) {
                    drawArc(
                        color = accentColor,
                        startAngle = -225f,
                        sweepAngle = normalizedValue * 270f,
                        useCenter = false,
                        topLeft = Offset(center.x - outerRadius + 3.dp.toPx(), center.y - outerRadius + 3.dp.toPx()),
                        size = Size((outerRadius - 3.dp.toPx()) * 2, (outerRadius - 3.dp.toPx()) * 2),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Knob body shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = knobRadius,
                    center = Offset(center.x + 1.dp.toPx(), center.y + 1.dp.toPx())
                )

                // Knob body
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isEnabled) {
                            listOf(Color(0xFF3A3A3A), Color(0xFF252525), Color(0xFF1A1A1A))
                        } else {
                            listOf(Color(0xFF2A2A2A), Color(0xFF1A1A1A))
                        },
                        center = Offset(center.x - 2.dp.toPx(), center.y - 2.dp.toPx())
                    ),
                    radius = knobRadius,
                    center = center
                )

                // Knob rim
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.1f), Color.Transparent),
                        start = Offset(center.x - knobRadius, center.y - knobRadius),
                        end = Offset(center.x + knobRadius, center.y + knobRadius)
                    ),
                    radius = knobRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Pointer
                val pointerAngle = (animatedRotation - 90f) * (PI.toFloat() / 180f)
                val pointerStart = Offset(
                    center.x + cos(pointerAngle) * (knobRadius * 0.2f),
                    center.y + sin(pointerAngle) * (knobRadius * 0.2f)
                )
                val pointerEnd = Offset(
                    center.x + cos(pointerAngle) * (knobRadius * 0.7f),
                    center.y + sin(pointerAngle) * (knobRadius * 0.7f)
                )

                // Pointer glow
                if (isEnabled) {
                    drawLine(
                        color = accentColor.copy(alpha = 0.3f),
                        start = pointerStart,
                        end = pointerEnd,
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                drawLine(
                    color = if (isEnabled) accentColor else Color.Gray,
                    start = pointerStart,
                    end = pointerEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = if (isEnabled) accentColor else Color.Gray,
                    radius = 3.dp.toPx(),
                    center = pointerEnd
                )

                // Center dot
                drawCircle(
                    color = if (isDragging) accentColor else Color(0xFF444444),
                    radius = 2.5.dp.toPx(),
                    center = center
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Value display
        Text(
            text = when {
                unit == "dB" -> "${if (value >= 0) "+" else ""}${value.toInt()}$unit"
                unit == ":1" -> "${value.toInt()}$unit"
                unit == "ms" -> "${value.toInt()}$unit"
                unit == "%" -> "${(value * 100).toInt()}$unit"
                unit == "x" -> "${"%.1f".format(value)}$unit"
                unit == "Hz" -> "${value.toInt()}$unit"
                else -> "${"%.1f".format(value)}$unit"
            },
            color = if (isEnabled) accentColor else TextTertiary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ============================================
// SPECTRUM VISUALIZATION
// ============================================

@Composable
private fun SpectrumVisualization(
    spectrumData: SpectrumData,
    modifier: Modifier = Modifier
) {
    val magnitudes = spectrumData.magnitudes
    val barCount = 32

    Canvas(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(EqSurface)) {
        if (magnitudes.isEmpty()) return@Canvas

        val barWidth = size.width / barCount
        val step = magnitudes.size / barCount

        for (i in 0 until barCount) {
            val index = (i * step).coerceIn(0, magnitudes.size - 1)
            val magnitude = magnitudes[index]
            val normalizedMag = ((magnitude + 90) / 90).coerceIn(0f, 1f)
            val barHeight = normalizedMag * size.height

            val gradient = Brush.verticalGradient(
                colors = listOf(EqOrange, EqOrangeDark),
                startY = size.height - barHeight,
                endY = size.height
            )

            drawRect(
                brush = gradient,
                topLeft = Offset(i * barWidth + 2, size.height - barHeight),
                size = Size(barWidth - 4, barHeight)
            )
        }
    }
}

// ============================================
// EFFECTS TAB - WITH KNOBS
// ============================================

@Composable
private fun EffectsTab(superEqualizer: SuperEqualizer) {
    val compressorSettings by superEqualizer.compressor.settings.collectAsState()
    val limiterSettings by superEqualizer.limiter.settings.collectAsState()
    val distortionSettings by superEqualizer.distortion.settings.collectAsState()
    val saturationSettings by superEqualizer.saturation.settings.collectAsState()
    val bassBoostSettings by superEqualizer.bassBoost.settings.collectAsState()
    val loudnessSettings by superEqualizer.loudnessEnhancer.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Bass & Loudness Section
        SectionHeader("BASS & LOUDNESS", Icons.Default.GraphicEq)

        FXKnobCard(
            title = "Bass Boost",
            isEnabled = bassBoostSettings.isEnabled,
            onEnabledChange = { superEqualizer.bassBoost.updateSettings(enabled = it) },
            accentColor = EqOrange
        ) {
            FXKnob(
                label = "Strength",
                value = bassBoostSettings.strength.toFloat(),
                minValue = 0f,
                maxValue = 1000f,
                unit = "",
                accentColor = EqOrange,
                isEnabled = bassBoostSettings.isEnabled,
                onValueChange = { superEqualizer.bassBoost.updateSettings(strength = it.toInt()) }
            )
            FXKnob(
                label = "Loudness",
                value = loudnessSettings.gainMb.toFloat() / 100f,
                minValue = 0f,
                maxValue = 30f,
                unit = "dB",
                accentColor = EqGreen,
                isEnabled = loudnessSettings.isEnabled,
                onValueChange = { superEqualizer.loudnessEnhancer.updateSettings(gainMb = (it * 100).toInt()) }
            )
        }

        // Dynamics Section
        SectionHeader("DYNAMICS", Icons.Default.Compress)

        // Compressor with 4 knobs
        FXKnobCard(
            title = "Compressor",
            isEnabled = compressorSettings.isEnabled,
            onEnabledChange = { superEqualizer.compressor.updateSettings(enabled = it) },
            accentColor = EqBlue
        ) {
            FXKnob(
                label = "Threshold",
                value = compressorSettings.threshold,
                minValue = -60f,
                maxValue = 0f,
                unit = "dB",
                accentColor = EqBlue,
                isEnabled = compressorSettings.isEnabled,
                onValueChange = { superEqualizer.compressor.updateSettings(threshold = it) }
            )
            FXKnob(
                label = "Ratio",
                value = compressorSettings.ratio,
                minValue = 1f,
                maxValue = 20f,
                unit = ":1",
                accentColor = EqBlue,
                isEnabled = compressorSettings.isEnabled,
                onValueChange = { superEqualizer.compressor.updateSettings(ratio = it) }
            )
            FXKnob(
                label = "Attack",
                value = compressorSettings.attack,
                minValue = 0.1f,
                maxValue = 500f,
                unit = "ms",
                accentColor = EqBlue,
                isEnabled = compressorSettings.isEnabled,
                onValueChange = { superEqualizer.compressor.updateSettings(attack = it) }
            )
            FXKnob(
                label = "Release",
                value = compressorSettings.release,
                minValue = 10f,
                maxValue = 5000f,
                unit = "ms",
                accentColor = EqBlue,
                isEnabled = compressorSettings.isEnabled,
                onValueChange = { superEqualizer.compressor.updateSettings(release = it) }
            )
        }

        // Limiter with 2 knobs
        FXKnobCard(
            title = "Limiter",
            isEnabled = limiterSettings.isEnabled,
            onEnabledChange = { superEqualizer.limiter.updateSettings(enabled = it) },
            accentColor = EqRed
        ) {
            FXKnob(
                label = "Ceiling",
                value = limiterSettings.ceiling,
                minValue = -12f,
                maxValue = 0f,
                unit = "dB",
                accentColor = EqRed,
                isEnabled = limiterSettings.isEnabled,
                onValueChange = { superEqualizer.limiter.updateSettings(ceiling = it) }
            )
            FXKnob(
                label = "Release",
                value = limiterSettings.release,
                minValue = 10f,
                maxValue = 1000f,
                unit = "ms",
                accentColor = EqRed,
                isEnabled = limiterSettings.isEnabled,
                onValueChange = { superEqualizer.limiter.updateSettings(release = it) }
            )
        }

        // Harmonic Section
        SectionHeader("HARMONIC", Icons.Default.Waves)

        // Distortion with 3 knobs
        FXKnobCard(
            title = "Distortion",
            isEnabled = distortionSettings.isEnabled,
            onEnabledChange = { superEqualizer.distortion.updateSettings(enabled = it) },
            accentColor = EqOrange
        ) {
            FXKnob(
                label = "Drive",
                value = distortionSettings.drive,
                minValue = 0f,
                maxValue = 1f,
                unit = "%",
                accentColor = EqOrange,
                isEnabled = distortionSettings.isEnabled,
                onValueChange = { superEqualizer.distortion.updateSettings(drive = it) }
            )
            FXKnob(
                label = "Tone",
                value = distortionSettings.tone,
                minValue = 0f,
                maxValue = 1f,
                unit = "%",
                accentColor = EqOrange,
                isEnabled = distortionSettings.isEnabled,
                onValueChange = { superEqualizer.distortion.updateSettings(tone = it) }
            )
            FXKnob(
                label = "Mix",
                value = distortionSettings.mix,
                minValue = 0f,
                maxValue = 1f,
                unit = "%",
                accentColor = EqOrange,
                isEnabled = distortionSettings.isEnabled,
                onValueChange = { superEqualizer.distortion.updateSettings(mix = it) }
            )
        }

        // Saturation with 2 knobs
        FXKnobCard(
            title = "Saturation",
            isEnabled = saturationSettings.isEnabled,
            onEnabledChange = { superEqualizer.saturation.updateSettings(enabled = it) },
            accentColor = EqPurple
        ) {
            FXKnob(
                label = "Amount",
                value = saturationSettings.amount,
                minValue = 0f,
                maxValue = 1f,
                unit = "%",
                accentColor = EqPurple,
                isEnabled = saturationSettings.isEnabled,
                onValueChange = { superEqualizer.saturation.updateSettings(amount = it) }
            )
            FXKnob(
                label = "Mix",
                value = saturationSettings.mix,
                minValue = 0f,
                maxValue = 1f,
                unit = "%",
                accentColor = EqPurple,
                isEnabled = saturationSettings.isEnabled,
                onValueChange = { superEqualizer.saturation.updateSettings(mix = it) }
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// FX Knob Card - Container for knobs with enable toggle
@Composable
private fun FXKnobCard(
    title: String,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    accentColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = EqSurface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header with enable toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isEnabled) accentColor else EqTrackColor,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        title,
                        color = if (isEnabled) Color.White else TextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = EqTrackColor
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            // Knobs row
            AnimatedVisibility(visible = isEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    content = content
                )
            }
        }
    }
}

// ============================================
// SPATIAL TAB - WITH KNOBS
// ============================================

@Composable
private fun SpatialTab(superEqualizer: SuperEqualizer) {
    val reverbSettings by superEqualizer.reverb.settings.collectAsState()
    val virtualizerSettings by superEqualizer.virtualizer.settings.collectAsState()
    val stereoSettings by superEqualizer.stereoWidener.settings.collectAsState()
    val delaySettings by superEqualizer.delay.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Reverb with 3 knobs
        FXKnobCard(
            title = "Reverb",
            isEnabled = reverbSettings.isEnabled,
            onEnabledChange = { superEqualizer.reverb.updateSettings(enabled = it) },
            accentColor = EqCyan
        ) {
            FXKnob(
                label = "Room",
                value = reverbSettings.roomSize,
                minValue = 0f,
                maxValue = 1f,
                unit = "%",
                accentColor = EqCyan,
                isEnabled = reverbSettings.isEnabled,
                onValueChange = { superEqualizer.reverb.updateSettings(roomSize = it) }
            )
            FXKnob(
                label = "Damping",
                value = reverbSettings.damping,
                minValue = 0f,
                maxValue = 1f,
                unit = "%",
                accentColor = EqCyan,
                isEnabled = reverbSettings.isEnabled,
                onValueChange = { superEqualizer.reverb.updateSettings(damping = it) }
            )
            FXKnob(
                label = "Mix",
                value = reverbSettings.wetDryMix,
                minValue = 0f,
                maxValue = 1f,
                unit = "%",
                accentColor = EqCyan,
                isEnabled = reverbSettings.isEnabled,
                onValueChange = { superEqualizer.reverb.updateSettings(wetDryMix = it) }
            )
        }

        // Virtualizer with 2 knobs
        FXKnobCard(
            title = "3D Virtualizer",
            isEnabled = virtualizerSettings.isEnabled,
            onEnabledChange = { superEqualizer.virtualizer.updateSettings(enabled = it) },
            accentColor = EqPurple
        ) {
            FXKnob(
                label = "Strength",
                value = virtualizerSettings.strength.toFloat(),
                minValue = 0f,
                maxValue = 1000f,
                unit = "",
                accentColor = EqPurple,
                isEnabled = virtualizerSettings.isEnabled,
                onValueChange = { superEqualizer.virtualizer.updateSettings(strength = it.toInt()) }
            )
            FXKnob(
                label = "Room",
                value = virtualizerSettings.roomSize,
                minValue = 0f,
                maxValue = 1f,
                unit = "%",
                accentColor = EqPurple,
                isEnabled = virtualizerSettings.isEnabled,
                onValueChange = { superEqualizer.virtualizer.updateSettings(roomSize = it) }
            )
        }

        // Stereo Width with 3 knobs
        FXKnobCard(
            title = "Stereo Width",
            isEnabled = stereoSettings.isEnabled,
            onEnabledChange = { superEqualizer.stereoWidener.updateSettings(enabled = it) },
            accentColor = EqGreen
        ) {
            FXKnob(
                label = "Width",
                value = stereoSettings.width,
                minValue = 0f,
                maxValue = 3f,
                unit = "x",
                accentColor = EqGreen,
                isEnabled = stereoSettings.isEnabled,
                onValueChange = { superEqualizer.stereoWidener.updateSettings(width = it) }
            )
            FXKnob(
                label = "Mid",
                value = stereoSettings.midGain,
                minValue = -12f,
                maxValue = 12f,
                unit = "dB",
                accentColor = EqGreen,
                isEnabled = stereoSettings.isEnabled,
                onValueChange = { superEqualizer.stereoWidener.updateSettings(midGain = it) }
            )
            FXKnob(
                label = "Side",
                value = stereoSettings.sideGain,
                minValue = -12f,
                maxValue = 12f,
                unit = "dB",
                accentColor = EqGreen,
                isEnabled = stereoSettings.isEnabled,
                onValueChange = { superEqualizer.stereoWidener.updateSettings(sideGain = it) }
            )
        }

        // Delay with 3 knobs
        FXKnobCard(
            title = "Delay",
            isEnabled = delaySettings.isEnabled,
            onEnabledChange = { superEqualizer.delay.updateSettings(enabled = it) },
            accentColor = EqPink
        ) {
            FXKnob(
                label = "Time",
                value = delaySettings.timeMs,
                minValue = 1f,
                maxValue = 2000f,
                unit = "ms",
                accentColor = EqPink,
                isEnabled = delaySettings.isEnabled,
                onValueChange = { superEqualizer.delay.updateSettings(timeMs = it) }
            )
            FXKnob(
                label = "Feedback",
                value = delaySettings.feedback,
                minValue = 0f,
                maxValue = 0.95f,
                unit = "%",
                accentColor = EqPink,
                isEnabled = delaySettings.isEnabled,
                onValueChange = { superEqualizer.delay.updateSettings(feedback = it) }
            )
            FXKnob(
                label = "Mix",
                value = delaySettings.wetDryMix,
                minValue = 0f,
                maxValue = 1f,
                unit = "%",
                accentColor = EqPink,
                isEnabled = delaySettings.isEnabled,
                onValueChange = { superEqualizer.delay.updateSettings(wetDryMix = it) }
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ============================================
// PRESETS TAB
// ============================================

@Composable
private fun PresetsTab(
    superEqualizer: SuperEqualizer,
    presets: List<AudioPreset>
) {
    var selectedCategory by remember { mutableStateOf(PresetCategory.GENRE) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Category Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(PresetCategory.entries) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category.icon, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(category.label, fontSize = 12.sp)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EqOrange,
                        containerColor = EqSurface
                    )
                )
            }
        }

        // Presets Grid
        val filteredPresets = superEqualizer.presetManager.getPresetsByCategory(selectedCategory)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredPresets) { preset ->
                PresetCard(
                    preset = preset,
                    isSelected = superEqualizer.presetManager.currentPreset.collectAsState().value?.id == preset.id,
                    onClick = { superEqualizer.applyPreset(preset) },
                    onFavorite = { superEqualizer.presetManager.toggleFavorite(preset.id) },
                    onDelete = if (preset.isCustom) {
                        { superEqualizer.presetManager.deletePreset(preset.id) }
                    } else null
                )
            }
        }

        // Save Custom Preset Button
        Button(
            onClick = { showSaveDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EqOrange)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE CUSTOM PRESET")
        }
    }

    // Save Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = EqSurface,
            title = { Text("Save Preset", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { newPresetName = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EqOrange,
                        unfocusedBorderColor = EqTrackColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPresetName.isNotBlank()) {
                        superEqualizer.presetManager.saveCustomPreset(newPresetName)
                        newPresetName = ""
                        showSaveDialog = false
                    }
                }) {
                    Text("SAVE", color = EqOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PresetCard(
    preset: AudioPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) EqOrange.copy(alpha = 0.2f) else EqSurface,
        border = BorderStroke(
            1.dp,
            if (isSelected) EqOrange else EqTrackColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    preset.name,
                    color = if (isSelected) EqOrange else Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(
                        onClick = onFavorite,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (preset.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            null,
                            tint = if (preset.isFavorite) EqOrange else TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint = EqRed.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                preset.category.label,
                color = TextTertiary,
                fontSize = 10.sp
            )

            preset.specialMode?.let { mode ->
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = EqPurple.copy(alpha = 0.3f)
                ) {
                    Text(
                        mode.label,
                        color = EqPurple,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// METERS TAB
// ============================================

@Composable
private fun MetersTab(
    superEqualizer: SuperEqualizer
) {
    // Auto-enable analyzer when viewing this tab
    LaunchedEffect(Unit) {
        superEqualizer.spectrumAnalyzer.setEnabled(true)
    }

    // Collect REAL data from spectrum analyzer
    val spectrumData by superEqualizer.spectrumAnalyzer.spectrumData.collectAsState()
    val waveformData by superEqualizer.spectrumAnalyzer.waveformData.collectAsState()
    val analyzerSettings by superEqualizer.spectrumAnalyzer.settings.collectAsState()
    val eqState by superEqualizer.state.collectAsState()

    // Calculate real levels from waveform data
    val realLevels = remember(waveformData) {
        calculateRealLevels(waveformData)
    }

    // Use real spectrum magnitudes
    val hasAudio = waveformData.isNotEmpty() && waveformData.any { it.toInt() != 0 && it.toInt() != -128 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Visualizer Enable Control
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = EqSurface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Insights,
                        contentDescription = null,
                        tint = if (analyzerSettings.isEnabled) EqGreen else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Audio Analyzer",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (analyzerSettings.isEnabled) {
                                if (hasAudio) "Capturing audio..." else "Waiting for audio..."
                            } else "Disabled",
                            color = if (analyzerSettings.isEnabled) EqGreen else TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = analyzerSettings.isEnabled,
                    onCheckedChange = { superEqualizer.spectrumAnalyzer.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EqGreen,
                        checkedTrackColor = EqGreen.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = EqTrackColor
                    )
                )
            }
        }

        // Level Meters
        SectionHeader("LEVEL METERS", Icons.Default.Speed)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = EqSurface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Channel
                RealLevelMeterBar(
                    label = "L",
                    peak = realLevels.peakL,
                    rms = realLevels.rmsL,
                    hasSignal = hasAudio,
                    modifier = Modifier.weight(1f)
                )

                // Right Channel
                RealLevelMeterBar(
                    label = "R",
                    peak = realLevels.peakR,
                    rms = realLevels.rmsR,
                    hasSignal = hasAudio,
                    modifier = Modifier.weight(1f)
                )

                // Peak/RMS Display
                Column(
                    modifier = Modifier
                        .weight(2.5f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (hasAudio) {
                        Text("PEAK", color = TextTertiary, fontSize = 10.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .background(EqSurfaceLight, RoundedCornerShape(12.dp))
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                "%.1f dB".format(maxOf(realLevels.peakL, realLevels.peakR)),
                                color = when {
                                    realLevels.peakL > -3f || realLevels.peakR > -3f -> EqRed
                                    realLevels.peakL > -12f || realLevels.peakR > -12f -> EqOrange
                                    else -> EqGreen
                                },
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SmallMeterValue("Peak L", "%.1f".format(realLevels.peakL), EqOrange)
                            SmallMeterValue("Peak R", "%.1f".format(realLevels.peakR), EqOrange)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SmallMeterValue("RMS L", "%.1f".format(realLevels.rmsL), EqCyan)
                            SmallMeterValue("RMS R", "%.1f".format(realLevels.rmsR), EqCyan)
                        }
                    } else {
                        Icon(
                            Icons.Default.VolumeOff,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No Audio Signal",
                            color = TextTertiary,
                            fontSize = 14.sp
                        )
                        Text(
                            "Play some music to see meters",
                            color = TextTertiary.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Real-time Spectrum Display
        SectionHeader("SPECTRUM ANALYZER", Icons.Default.Equalizer)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(16.dp),
            color = EqSurface
        ) {
            if (hasAudio && spectrumData.magnitudes.isNotEmpty()) {
                RealSpectrumBars(
                    magnitudes = spectrumData.magnitudes,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (analyzerSettings.isEnabled) "Waiting for audio..." else "Enable analyzer above",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Waveform Display
        SectionHeader("WAVEFORM", Icons.Default.Timeline)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shape = RoundedCornerShape(16.dp),
            color = EqSurface
        ) {
            if (hasAudio && waveformData.isNotEmpty()) {
                RealWaveformDisplay(
                    waveform = waveformData,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No waveform data",
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // System Info
        SectionHeader("SYSTEM INFO", Icons.Default.Info)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = EqSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow("Analyzer Status",
                    if (analyzerSettings.isEnabled) "Active" else "Disabled",
                    if (analyzerSettings.isEnabled) EqGreen else TextSecondary)
                InfoRow("Audio Signal",
                    if (hasAudio) "Detected" else "None",
                    if (hasAudio) EqGreen else TextSecondary)
                InfoRow("Spectrum Bins", "${spectrumData.magnitudes.size}", EqCyan)
                InfoRow("Peak Frequency",
                    if (hasAudio) "%.0f Hz".format(spectrumData.peakFrequency) else "N/A",
                    EqOrange)
                InfoRow("Audio Device", eqState.audioDevice?.name ?: "Phone Speaker", Color.White)
                InfoRow("Bluetooth Codec", eqState.bluetoothCodec?.label ?: "N/A", EqPurple)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// Calculate real audio levels from waveform data
private data class RealAudioLevels(
    val peakL: Float = -60f,
    val peakR: Float = -60f,
    val rmsL: Float = -60f,
    val rmsR: Float = -60f
)

private fun calculateRealLevels(waveform: ByteArray): RealAudioLevels {
    if (waveform.isEmpty()) return RealAudioLevels()

    var maxL = 0f
    var maxR = 0f
    var sumSquaredL = 0f
    var sumSquaredR = 0f
    var countL = 0
    var countR = 0

    // Waveform data is unsigned 8-bit, center is 128
    for (i in waveform.indices) {
        val sample = (waveform[i].toInt() and 0xFF) - 128
        val normalized = sample / 128f

        if (i % 2 == 0) {
            // Left channel
            val abs = abs(normalized)
            if (abs > maxL) maxL = abs
            sumSquaredL += normalized * normalized
            countL++
        } else {
            // Right channel
            val abs = abs(normalized)
            if (abs > maxR) maxR = abs
            sumSquaredR += normalized * normalized
            countR++
        }
    }

    // Convert to dB
    val peakLdB = if (maxL > 0) 20 * log10(maxL) else -60f
    val peakRdB = if (maxR > 0) 20 * log10(maxR) else -60f
    val rmsL = if (countL > 0) sqrt(sumSquaredL / countL) else 0f
    val rmsR = if (countR > 0) sqrt(sumSquaredR / countR) else 0f
    val rmsLdB = if (rmsL > 0) 20 * log10(rmsL) else -60f
    val rmsRdB = if (rmsR > 0) 20 * log10(rmsR) else -60f

    return RealAudioLevels(
        peakL = peakLdB.coerceIn(-60f, 0f),
        peakR = peakRdB.coerceIn(-60f, 0f),
        rmsL = rmsLdB.coerceIn(-60f, 0f),
        rmsR = rmsRdB.coerceIn(-60f, 0f)
    )
}

@Composable
private fun RealLevelMeterBar(
    label: String,
    peak: Float,
    rms: Float,
    hasSignal: Boolean,
    modifier: Modifier = Modifier
) {
    val peakHeight by animateFloatAsState(
        targetValue = if (hasSignal) ((peak + 60) / 60).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(50),
        label = "peakHeight"
    )
    val rmsHeight by animateFloatAsState(
        targetValue = if (hasSignal) ((rms + 60) / 60).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(100),
        label = "rmsHeight"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(EqTrackColor)
        ) {
            // Scale markers
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // dB scale markers: 0, -6, -12, -24, -36, -48, -60
                listOf("0", "-12", "-24", "-36", "-48", "-60").forEach { db ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                        )
                    }
                }
            }

            // RMS fill (green/yellow/red gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(rmsHeight)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(EqRed, EqOrange, EqGreen),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Peak indicator line
            if (hasSignal && peakHeight > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .graphicsLayer {
                            translationY = -(peakHeight * size.height)
                        }
                        .background(Color.White)
                )
            }

            // Clip indicator
            if (peak > -1f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .align(Alignment.TopCenter)
                        .background(EqRed)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // dB value
        Text(
            if (hasSignal) "%.0f".format(peak) else "--",
            color = when {
                !hasSignal -> TextTertiary
                peak > -3f -> EqRed
                peak > -12f -> EqOrange
                else -> EqGreen
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RealSpectrumBars(
    magnitudes: FloatArray,
    modifier: Modifier = Modifier
) {
    val barCount = 32
    val step = maxOf(1, magnitudes.size / barCount)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0 until barCount) {
            val index = (i * step).coerceIn(0, magnitudes.size - 1)
            val magnitude = magnitudes.getOrElse(index) { -90f }
            // Normalize from -90..0 dB to 0..1
            val normalizedHeight = ((magnitude + 90) / 90).coerceIn(0f, 1f)

            val animatedHeight by animateFloatAsState(
                targetValue = normalizedHeight,
                animationSpec = tween(50),
                label = "bar$i"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedHeight.coerceIn(0.02f, 1f))
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(EqOrange, EqOrangeDark)
                            ),
                            RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun RealWaveformDisplay(
    waveform: ByteArray,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (waveform.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val centerY = height / 2
        val step = maxOf(1, waveform.size / width.toInt())

        val path = Path()
        var started = false

        for (i in 0 until width.toInt()) {
            val sampleIndex = (i * step).coerceIn(0, waveform.size - 1)
            val sample = (waveform[sampleIndex].toInt() and 0xFF) - 128
            val y = centerY - (sample / 128f) * (height / 2)

            if (!started) {
                path.moveTo(i.toFloat(), y)
                started = true
            } else {
                path.lineTo(i.toFloat(), y)
            }
        }

        // Draw center line
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f
        )

        // Draw waveform
        drawPath(
            path = path,
            color = EqCyan,
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun SmallMeterValue(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextTertiary, fontSize = 9.sp)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}


@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ============================================
// BOTTOM QUICK ACTIONS
// ============================================

@Composable
private fun BottomQuickActions(
    currentMode: SpecialMode?,
    onModeSelect: (SpecialMode?) -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EqSurface,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "QUICK MODES",
                    color = TextTertiary,
                    fontSize = 10.sp
                )

                // Reset All Button
                TextButton(
                    onClick = onReset,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = "Reset All",
                        tint = EqRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RESET ALL", color = EqRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SpecialMode.entries.take(6)) { mode ->
                    QuickModeChip(
                        mode = mode,
                        isSelected = currentMode == mode,
                        onClick = {
                            onModeSelect(if (currentMode == mode) null else mode)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickModeChip(
    mode: SpecialMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = when (mode) {
        SpecialMode.PODCAST -> EqBlue
        SpecialMode.GAMING -> EqGreen
        SpecialMode.CALL -> EqCyan
        SpecialMode.KARAOKE -> EqPink
        SpecialMode.WORKOUT -> EqOrange
        SpecialMode.NIGHT_MODE -> EqPurple
        else -> EqOrange
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color else color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else color.copy(alpha = 0.3f))
    ) {
        Text(
            mode.label,
            color = if (isSelected) Color.White else color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

// ============================================
// COMMON COMPONENTS
// ============================================

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = EqOrange, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            color = EqOrange,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun EffectCard(
    title: String,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = EqSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isEnabled) accentColor else EqTrackColor,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        title,
                        color = if (isEnabled) Color.White else TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = EqTrackColor
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            AnimatedVisibility(visible = isEnabled) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun EffectSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.width(70.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = EqOrange,
                activeTrackColor = EqOrange,
                inactiveTrackColor = EqTrackColor
            )
        )

        Text(
            "${value.toInt()}$unit",
            color = EqOrange,
            fontSize = 11.sp,
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.End
        )
    }
}

