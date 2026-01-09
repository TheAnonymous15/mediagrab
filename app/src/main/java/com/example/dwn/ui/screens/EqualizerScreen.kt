package com.example.dwn.ui.screens

import android.media.audiofx.PresetReverb
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.player.EqualizerManager
import com.example.dwn.player.EqualizerState
import com.example.dwn.player.EqualizerBand
import com.example.dwn.ui.theme.*

// Orange/Amber color scheme
private val EqOrange = Color(0xFFFF9800)
private val EqOrangeDark = Color(0xFFE65100)
private val EqBackground = Color(0xFF121212)
private val EqSurface = Color(0xFF1E1E1E)
private val EqTrackColor = Color(0xFF3D3D3D)
private val EqCurveColor = Color(0xFF2196F3).copy(alpha = 0.5f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    equalizerManager: EqualizerManager,
    currentDevice: String,
    isBluetoothConnected: Boolean,
    onBack: () -> Unit
) {
    val state by equalizerManager.state.collectAsState()
    var preampValue by remember { mutableFloatStateOf(0.5f) }
    var snapBands by remember { mutableStateOf(true) }
    var showPresetDropdown by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = EQ, 1 = Effects

    // Get preset names from customPresets
    val presetNames = remember { equalizerManager.customPresets.map { it.first } }
    var selectedPreset by remember { mutableStateOf("Flat") }

    var showSaveDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    // Handle system back button
    BackHandler {
        onBack()
    }

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
            // Compact Header
            EqHeader(
                isEnabled = state.isEnabled,
                onEnabledChange = { equalizerManager.setEnabled(it) },
                onBack = onBack
            )

            // Presets Row (horizontal scrollable chips)
            PresetChipsRow(
                presets = presetNames,
                selectedPreset = selectedPreset,
                onPresetSelected = { preset ->
                    selectedPreset = preset
                    equalizerManager.applyCustomPreset(preset)
                }
            )

            // Tab Row for EQ / Effects
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = EqOrange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "EQUALIZER",
                            color = if (selectedTab == 0) EqOrange else TextSecondary,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "EFFECTS",
                            color = if (selectedTab == 1) EqOrange else TextSecondary,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Content based on tab
            when (selectedTab) {
                0 -> {
                    // Preamp Slider
                    PreampSlider(
                        value = preampValue,
                        onValueChange = { preampValue = it },
                        enabled = state.isEnabled
                    )

                    // Equalizer Bands
                    EqualizerBandsSection(
                        state = state,
                        equalizerManager = equalizerManager,
                        snapBands = snapBands,
                        modifier = Modifier.weight(1f)
                    )

                    // Snap Bands Toggle
                    SnapBandsToggle(
                        checked = snapBands,
                        onCheckedChange = { snapBands = it }
                    )
                }
                1 -> {
                    // Effects Tab
                    EffectsTab(
                        state = state,
                        equalizerManager = equalizerManager,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Bottom Action Buttons
            BottomActionButtons(
                onReset = {
                    equalizerManager.resetToDefault()
                    preampValue = 0.5f
                    selectedPreset = "Flat"
                },
                onSave = {
                    showSaveDialog = true
                }
            )
        }
    }

    // Save Preset Dialog
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
                        focusedLabelColor = EqOrange,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = EqOrange,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPresetName.isNotBlank()) {
                        selectedPreset = newPresetName
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
private fun EqHeader(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        Text(
            text = "Equalizer",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "Enable",
            color = if (isEnabled) EqOrange else TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 8.dp)
        )

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

@Composable
private fun PresetChipsRow(
    presets: List<String>,
    selectedPreset: String,
    onPresetSelected: (String) -> Unit
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
                selected = preset == selectedPreset,
                onClick = { onPresetSelected(preset) },
                label = {
                    Text(
                        preset,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                },
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
                    selected = preset == selectedPreset
                )
            )
        }
    }
}

@Composable
private fun PreampSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Preamp",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.width(60.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = EqOrange,
                activeTrackColor = EqOrange,
                inactiveTrackColor = EqTrackColor,
                disabledThumbColor = Color.Gray,
                disabledActiveTrackColor = Color.Gray,
                disabledInactiveTrackColor = EqTrackColor
            )
        )

        Text(
            text = "${((value - 0.5f) * 24).toInt()}dB",
            color = EqOrange,
            fontSize = 12.sp,
            modifier = Modifier.width(45.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun EqualizerBandsSection(
    state: EqualizerState,
    equalizerManager: EqualizerManager,
    snapBands: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var sectionWidth by remember { mutableFloatStateOf(0f) }
    var sectionHeight by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .onSizeChanged {
                sectionWidth = it.width.toFloat()
                sectionHeight = it.height.toFloat()
            }
    ) {
        // Draw the curve connecting band thumbs
        if (state.bands.isNotEmpty() && sectionWidth > 0 && sectionHeight > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bandWidth = size.width / state.bands.size
                val headerOffset = with(density) { 20.dp.toPx() }
                val footerOffset = with(density) { 50.dp.toPx() }
                val usableHeight = size.height - headerOffset - footerOffset

                val points = state.bands.mapIndexed { index, band ->
                    val normalizedLevel = ((band.currentLevel.toFloat() - state.minBandLevel) /
                            (state.maxBandLevel - state.minBandLevel)).coerceIn(0f, 1f)
                    val x = bandWidth * index + bandWidth / 2
                    val y = headerOffset + usableHeight * (1 - normalizedLevel)
                    Offset(x, y)
                }

                if (points.size >= 2) {
                    val path = Path()
                    path.moveTo(points.first().x, points.first().y)

                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2
                        path.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }

                    drawPath(
                        path = path,
                        color = EqCurveColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Band sliders
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            state.bands.forEach { band ->
                EqBandSlider(
                    band = band,
                    minLevel = state.minBandLevel,
                    maxLevel = state.maxBandLevel,
                    isEnabled = state.isEnabled,
                    snapBands = snapBands,
                    onValueChange = { equalizerManager.setBandLevel(band.index, it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EqBandSlider(
    band: EqualizerBand,
    minLevel: Int,
    maxLevel: Int,
    isEnabled: Boolean,
    snapBands: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val normalizedLevel = ((band.currentLevel.toFloat() - minLevel) / (maxLevel - minLevel)).coerceIn(0f, 1f)
    var sliderHeight by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Column(
        modifier = modifier.padding(horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // +dB label at top
        Text(
            text = "+${maxLevel / 100}",
            color = TextTertiary,
            fontSize = 8.sp,
            textAlign = TextAlign.Center
        )

        // Vertical slider track - SHORTER height
        Box(
            modifier = Modifier
                .weight(1f)
                .width(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(EqTrackColor)
                .onSizeChanged { sliderHeight = it.height.toFloat() }
                .pointerInput(isEnabled, snapBands) {
                    if (isEnabled) {
                        detectVerticalDragGestures(
                            onDragStart = { },
                            onDragEnd = { },
                            onVerticalDrag = { change, _ ->
                                change.consume()
                                val y = change.position.y
                                val newNormalized = 1f - (y / sliderHeight).coerceIn(0f, 1f)
                                var newLevel = (minLevel + (newNormalized * (maxLevel - minLevel))).toInt()

                                // Snap to nearest 100 (1dB) if snapBands is enabled
                                if (snapBands) {
                                    newLevel = ((newLevel + 50) / 100) * 100
                                }
                                onValueChange(newLevel.coerceIn(minLevel, maxLevel))
                            }
                        )
                    }
                }
                .pointerInput(isEnabled, snapBands) {
                    if (isEnabled) {
                        detectTapGestures { offset ->
                            val newNormalized = 1f - (offset.y / sliderHeight).coerceIn(0f, 1f)
                            var newLevel = (minLevel + (newNormalized * (maxLevel - minLevel))).toInt()
                            if (snapBands) {
                                newLevel = ((newLevel + 50) / 100) * 100
                            }
                            onValueChange(newLevel.coerceIn(minLevel, maxLevel))
                        }
                    }
                }
        ) {
            // Filled portion (from bottom to current level)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(normalizedLevel)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isEnabled) listOf(EqOrange, EqOrangeDark) else listOf(Color.Gray, Color.DarkGray)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            // Thumb
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = (1 - normalizedLevel) * (sliderHeight - with(density) { 16.dp.toPx() })
                    }
                    .background(
                        if (isEnabled) EqOrange else Color.Gray,
                        CircleShape
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            )
        }

        // -dB label
        Text(
            text = "${minLevel / 100}",
            color = TextTertiary,
            fontSize = 8.sp,
            textAlign = TextAlign.Center
        )

        // Frequency label
        Text(
            text = if (band.centerFreq >= 1000) "${band.centerFreq / 1000}k" else "${band.centerFreq}",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        // Current dB value
        Text(
            text = "${if (band.currentLevel >= 0) "+" else ""}${band.currentLevel / 100}",
            color = EqOrange,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EffectsTab(
    state: EqualizerState,
    equalizerManager: EqualizerManager,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bass Boost
        EffectCard(
            title = "Bass Boost",
            icon = Icons.Default.GraphicEq,
            value = state.bassBoostStrength / 1000f,
            onValueChange = { equalizerManager.setBassBoost((it * 1000).toInt()) },
            enabled = state.isEnabled,
            valueLabel = "${state.bassBoostStrength / 10}%"
        )

        // Virtualizer (3D Sound)
        EffectCard(
            title = "Virtualizer",
            subtitle = "3D Surround Effect",
            icon = Icons.Default.SurroundSound,
            value = state.virtualizerStrength / 1000f,
            onValueChange = { equalizerManager.setVirtualizer((it * 1000).toInt()) },
            enabled = state.isEnabled,
            valueLabel = "${state.virtualizerStrength / 10}%"
        )

        // Reverb Presets
        ReverbSection(
            currentPreset = state.reverbPreset,
            reverbPresets = equalizerManager.reverbPresets,
            onPresetSelected = { equalizerManager.setReverbPreset(it) },
            enabled = state.isEnabled
        )

        // Loudness Enhancer (simulated with info)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = EqSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = EqOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Audio Output",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Connected to: Speaker",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EffectCard(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
    valueLabel: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = EqSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (enabled && value > 0) EqOrange else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    text = valueLabel,
                    color = EqOrange,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = EqOrange,
                    activeTrackColor = EqOrange,
                    inactiveTrackColor = EqTrackColor,
                    disabledThumbColor = Color.Gray,
                    disabledActiveTrackColor = Color.Gray,
                    disabledInactiveTrackColor = EqTrackColor
                )
            )
        }
    }
}

@Composable
private fun ReverbSection(
    currentPreset: Short,
    reverbPresets: List<Pair<String, Short>>,
    onPresetSelected: (Short) -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = EqSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Waves,
                    contentDescription = null,
                    tint = if (enabled && currentPreset != PresetReverb.PRESET_NONE) EqOrange else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Reverb",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reverb preset chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reverbPresets) { (name, preset) ->
                    FilterChip(
                        selected = preset == currentPreset,
                        onClick = { if (enabled) onPresetSelected(preset) },
                        label = { Text(name, fontSize = 11.sp) },
                        enabled = enabled,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EqOrange,
                            selectedLabelColor = Color.White,
                            containerColor = EqTrackColor,
                            labelColor = TextSecondary,
                            disabledContainerColor = EqTrackColor.copy(alpha = 0.5f),
                            disabledLabelColor = TextTertiary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SnapBandsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Snap to dB",
            color = if (checked) EqOrange else TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 8.dp)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EqOrange,
                checkedTrackColor = EqOrange.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = EqTrackColor
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun BottomActionButtons(
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(
            onClick = onReset,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextSecondary
            ),
            border = BorderStroke(1.dp, EqTrackColor)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("RESET")
        }

        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(
                containerColor = EqOrange
            )
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE")
        }
    }
}

