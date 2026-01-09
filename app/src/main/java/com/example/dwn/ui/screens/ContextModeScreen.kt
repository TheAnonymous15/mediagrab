package com.example.dwn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.context.*
import kotlin.math.roundToInt

// ============================================
// 🎨 CONTEXT MODE COLORS
// ============================================

private object ContextColors {
    val pink = Color(0xFFE91E63)
    val purple = Color(0xFF9C27B0)
    val blue = Color(0xFF2196F3)
    val cyan = Color(0xFF00BCD4)
    val teal = Color(0xFF009688)
    val green = Color(0xFF4CAF50)
    val orange = Color(0xFFFF5722)
    val amber = Color(0xFFFFC107)
    val indigo = Color(0xFF3F51B5)

    val bgDark = Color(0xFF0A0A0F)
    val bgMid = Color(0xFF101018)
    val surface = Color(0xFF161620)
    val surfaceVariant = Color(0xFF1E1E2A)
    val card = Color(0xFF222230)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B8)
    val textTertiary = Color(0xFF707080)

    val glassWhite = Color(0x14FFFFFF)
    val glassBorder = Color(0x20FFFFFF)

    // Mode colors
    val walkColor = Color(0xFF4CAF50)
    val driveColor = Color(0xFF2196F3)
    val focusColor = Color(0xFF9C27B0)
    val nightColor = Color(0xFF3F51B5)
    val castColor = Color(0xFFFF5722)
    val workoutColor = Color(0xFFE91E63)
    val commuteColor = Color(0xFF00BCD4)
    val autoColor = Color(0xFFFFC107)
}

// ============================================
// 🎯 CONTEXT MEDIA MODE SCREEN
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextModeScreen(
    onBack: () -> Unit
) {
    // Mock state - would come from ContextMediaManager
    var activeMode by remember { mutableStateOf(ContextMode.AUTO) }
    var isLocked by remember { mutableStateOf(false) }
    var showModeDetails by remember { mutableStateOf<ContextMode?>(null) }

    // Mock signals
    val signals = remember {
        ContextSignals(
            audioOutput = AudioOutputType.BLUETOOTH_EARBUDS,
            bluetoothDeviceType = BluetoothDeviceType.EARBUDS,
            isHeadphonesConnected = true,
            motionState = MotionState.STATIONARY,
            timeOfDay = TimeOfDay.EVENING,
            hourOfDay = 19,
            isScreenOn = true
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ContextColors.bgDark)
    ) {
        // Animated background
        ContextBackground(activeMode = activeMode)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoMode,
                            null,
                            tint = getModeColor(activeMode),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Context Mode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "Adaptive Audio Intelligence",
                                color = ContextColors.textTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Lock toggle
                    IconButton(onClick = { isLocked = !isLocked }) {
                        Icon(
                            if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            "Lock Mode",
                            tint = if (isLocked) ContextColors.amber else ContextColors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = ContextColors.textPrimary,
                    navigationIconContentColor = ContextColors.textSecondary
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Current Mode Card
                CurrentModeCard(
                    mode = activeMode,
                    isLocked = isLocked,
                    signals = signals,
                    onUnlock = { isLocked = false }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Context Signals
                ContextSignalsCard(signals = signals)

                Spacer(modifier = Modifier.height(24.dp))

                // Mode Selector
                ModeSelector(
                    activeMode = activeMode,
                    onModeSelect = { mode ->
                        activeMode = mode
                        showModeDetails = mode
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Settings
                QuickSettingsCard(
                    activeMode = activeMode
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Audio Adaptation Preview
                AudioAdaptationCard(
                    mode = activeMode
                )

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Mode details sheet
    if (showModeDetails != null) {
        ModeDetailsSheet(
            mode = showModeDetails!!,
            onDismiss = { showModeDetails = null }
        )
    }
}

// ============================================
// 🌌 ANIMATED BACKGROUND
// ============================================

@Composable
private fun ContextBackground(activeMode: ContextMode) {
    val transition = rememberInfiniteTransition(label = "bg")

    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val modeColor = getModeColor(activeMode)

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    ContextColors.bgDark,
                    ContextColors.bgMid,
                    ContextColors.bgDark
                )
            )
        )

        // Mode-colored ambient orb
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    modeColor.copy(alpha = 0.12f * pulse),
                    Color.Transparent
                )
            ),
            radius = 400f,
            center = Offset(size.width * 0.7f, size.height * 0.2f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    modeColor.copy(alpha = 0.08f * pulse),
                    Color.Transparent
                )
            ),
            radius = 300f,
            center = Offset(size.width * 0.2f, size.height * 0.7f)
        )
    }
}

// ============================================
// 🎯 CURRENT MODE CARD
// ============================================

@Composable
private fun CurrentModeCard(
    mode: ContextMode,
    isLocked: Boolean,
    signals: ContextSignals,
    onUnlock: () -> Unit
) {
    val modeColor = getModeColor(mode)

    val transition = rememberInfiniteTransition(label = "mode")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            modeColor.copy(alpha = 0.9f),
                            modeColor.copy(alpha = 0.7f)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (mode == ContextMode.AUTO) {
                                // Animated auto indicator
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            Color.White.copy(alpha = glowAlpha),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "AUTO",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            } else {
                                Text(
                                    "ACTIVE MODE",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                mode.icon,
                                fontSize = 32.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                mode.displayName,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            mode.description,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                    }

                    // Lock indicator
                    if (isLocked) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            onClick = onUnlock
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                "Locked",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Current context info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ContextChip(
                        icon = getAudioOutputIcon(signals.audioOutput),
                        label = getAudioOutputLabel(signals.audioOutput)
                    )
                    ContextChip(
                        icon = Icons.Default.DirectionsWalk,
                        label = signals.motionState.name.lowercase().replaceFirstChar { it.uppercase() }
                    )
                    ContextChip(
                        icon = getTimeIcon(signals.timeOfDay),
                        label = signals.timeOfDay.name.lowercase().replaceFirstChar { it.uppercase() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextChip(
    icon: ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = Color.White, fontSize = 11.sp)
        }
    }
}

// ============================================
// 📡 CONTEXT SIGNALS CARD
// ============================================

@Composable
private fun ContextSignalsCard(signals: ContextSignals) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = ContextColors.card,
        border = BorderStroke(1.dp, ContextColors.glassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Context Signals",
                    color = ContextColors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ContextColors.green.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(ContextColors.green, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Live",
                            color = ContextColors.green,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Signal rows
            SignalRow(
                icon = getAudioOutputIcon(signals.audioOutput),
                label = "Audio Output",
                value = getAudioOutputLabel(signals.audioOutput),
                color = ContextColors.pink
            )

            SignalRow(
                icon = Icons.Default.DirectionsWalk,
                label = "Motion",
                value = signals.motionState.name.lowercase().replaceFirstChar { it.uppercase() },
                color = ContextColors.green
            )

            SignalRow(
                icon = getTimeIcon(signals.timeOfDay),
                label = "Time",
                value = "${signals.hourOfDay}:00 • ${signals.timeOfDay.name.lowercase().replaceFirstChar { it.uppercase() }}",
                color = ContextColors.blue
            )

            SignalRow(
                icon = if (signals.isScreenOn) Icons.Default.PhoneAndroid else Icons.Default.PhonelinkOff,
                label = "Screen",
                value = if (signals.isScreenOn) "On" else "Off",
                color = ContextColors.amber
            )

            SignalRow(
                icon = Icons.Default.Cast,
                label = "Casting",
                value = if (signals.isCasting) "Active" else "None",
                color = ContextColors.orange,
                showDivider = false
            )
        }
    }
}

@Composable
private fun SignalRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(label, color = ContextColors.textSecondary, fontSize = 14.sp)
            }
            Text(value, color = ContextColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        if (showDivider) {
            HorizontalDivider(color = ContextColors.glassBorder)
        }
    }
}

// ============================================
// 🎚️ MODE SELECTOR
// ============================================

@Composable
private fun ModeSelector(
    activeMode: ContextMode,
    onModeSelect: (ContextMode) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Select Mode",
            color = ContextColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // First row - AUTO + WALK + DRIVE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModeCard(
                mode = ContextMode.AUTO,
                isActive = activeMode == ContextMode.AUTO,
                onClick = { onModeSelect(ContextMode.AUTO) },
                modifier = Modifier.weight(1f)
            )
            ModeCard(
                mode = ContextMode.WALK,
                isActive = activeMode == ContextMode.WALK,
                onClick = { onModeSelect(ContextMode.WALK) },
                modifier = Modifier.weight(1f)
            )
            ModeCard(
                mode = ContextMode.DRIVE,
                isActive = activeMode == ContextMode.DRIVE,
                onClick = { onModeSelect(ContextMode.DRIVE) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Second row - FOCUS + NIGHT + CAST
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModeCard(
                mode = ContextMode.FOCUS,
                isActive = activeMode == ContextMode.FOCUS,
                onClick = { onModeSelect(ContextMode.FOCUS) },
                modifier = Modifier.weight(1f)
            )
            ModeCard(
                mode = ContextMode.NIGHT,
                isActive = activeMode == ContextMode.NIGHT,
                onClick = { onModeSelect(ContextMode.NIGHT) },
                modifier = Modifier.weight(1f)
            )
            ModeCard(
                mode = ContextMode.CAST,
                isActive = activeMode == ContextMode.CAST,
                onClick = { onModeSelect(ContextMode.CAST) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Third row - WORKOUT + COMMUTE + CUSTOM
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModeCard(
                mode = ContextMode.WORKOUT,
                isActive = activeMode == ContextMode.WORKOUT,
                onClick = { onModeSelect(ContextMode.WORKOUT) },
                modifier = Modifier.weight(1f)
            )
            ModeCard(
                mode = ContextMode.COMMUTE,
                isActive = activeMode == ContextMode.COMMUTE,
                onClick = { onModeSelect(ContextMode.COMMUTE) },
                modifier = Modifier.weight(1f)
            )
            ModeCard(
                mode = ContextMode.CUSTOM,
                isActive = activeMode == ContextMode.CUSTOM,
                onClick = { onModeSelect(ContextMode.CUSTOM) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeCard(
    mode: ContextMode,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modeColor = getModeColor(mode)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) modeColor.copy(alpha = 0.2f) else ContextColors.surfaceVariant,
        border = if (isActive) BorderStroke(2.dp, modeColor) else BorderStroke(1.dp, ContextColors.glassBorder),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(mode.icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                mode.displayName,
                color = if (isActive) modeColor else ContextColors.textPrimary,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

// ============================================
// ⚡ QUICK SETTINGS
// ============================================

@Composable
private fun QuickSettingsCard(activeMode: ContextMode) {
    var autoModeEnabled by remember { mutableStateOf(true) }
    var privacyMode by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = ContextColors.card,
        border = BorderStroke(1.dp, ContextColors.glassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Quick Settings",
                color = ContextColors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            QuickSettingRow(
                icon = Icons.Default.AutoMode,
                title = "Auto Mode",
                subtitle = "Automatically switch based on context",
                checked = autoModeEnabled,
                onCheckedChange = { autoModeEnabled = it },
                color = ContextColors.amber
            )

            QuickSettingRow(
                icon = Icons.Default.Shield,
                title = "Privacy Mode",
                subtitle = "Disable motion & location sensors",
                checked = privacyMode,
                onCheckedChange = { privacyMode = it },
                color = ContextColors.green
            )
        }
    }
}

@Composable
private fun QuickSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = ContextColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = ContextColors.textTertiary, fontSize = 11.sp)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = color,
                checkedTrackColor = color.copy(alpha = 0.5f)
            )
        )
    }
}

// ============================================
// 🎵 AUDIO ADAPTATION PREVIEW
// ============================================

@Composable
private fun AudioAdaptationCard(mode: ContextMode) {
    val config = defaultModeConfigurations[mode]
    val audioProfile = config?.audioProfile ?: ModeAudioProfile()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = ContextColors.card,
        border = BorderStroke(1.dp, ContextColors.glassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Audio Adaptation",
                    color = ContextColors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                TextButton(onClick = { }) {
                    Text("Customize", color = ContextColors.pink, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // EQ Preview
            AdaptationItem(
                icon = Icons.Default.Equalizer,
                title = "Equalizer",
                value = audioProfile.eqPreset.displayName,
                isEnabled = audioProfile.eqEnabled,
                color = ContextColors.pink
            )

            // Compression
            AdaptationItem(
                icon = Icons.Default.Compress,
                title = "Compression",
                value = audioProfile.compressionStrength.displayName,
                isEnabled = audioProfile.compressionEnabled,
                color = ContextColors.purple
            )

            // Speech Enhancement
            AdaptationItem(
                icon = Icons.Default.RecordVoiceOver,
                title = "Speech Enhancement",
                value = if (audioProfile.speechEnhancement) "${(audioProfile.speechEnhancementLevel * 100).roundToInt()}%" else "Off",
                isEnabled = audioProfile.speechEnhancement,
                color = ContextColors.blue
            )

            // Volume Leveling
            AdaptationItem(
                icon = Icons.Default.VolumeUp,
                title = "Auto Volume",
                value = if (audioProfile.autoVolumeLeveling) "On" else "Off",
                isEnabled = audioProfile.autoVolumeLeveling,
                color = ContextColors.green
            )

            // Spatial Audio
            AdaptationItem(
                icon = Icons.Default.SurroundSound,
                title = "Spatial Audio",
                value = if (audioProfile.spatialAudioEnabled) "On" else "Off",
                isEnabled = audioProfile.spatialAudioEnabled,
                color = ContextColors.cyan,
                showDivider = false
            )
        }
    }
}

@Composable
private fun AdaptationItem(
    icon: ImageVector,
    title: String,
    value: String,
    isEnabled: Boolean,
    color: Color,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    null,
                    tint = if (isEnabled) color else ContextColors.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    title,
                    color = if (isEnabled) ContextColors.textPrimary else ContextColors.textTertiary,
                    fontSize = 14.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isEnabled) color.copy(alpha = 0.15f) else ContextColors.surfaceVariant
            ) {
                Text(
                    value,
                    color = if (isEnabled) color else ContextColors.textTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(color = ContextColors.glassBorder.copy(alpha = 0.5f))
        }
    }
}

// ============================================
// 📋 MODE DETAILS SHEET
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeDetailsSheet(
    mode: ContextMode,
    onDismiss: () -> Unit
) {
    val config = defaultModeConfigurations[mode]
    val audioProfile = config?.audioProfile ?: ModeAudioProfile()
    val uiProfile = config?.uiProfile ?: ModeUIProfile()
    val modeColor = getModeColor(mode)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ContextColors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(ContextColors.glassBorder, RoundedCornerShape(2.dp))
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
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mode.icon, fontSize = 36.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        mode.displayName,
                        color = ContextColors.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        mode.description,
                        color = ContextColors.textTertiary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Audio Settings Preview
            Text(
                "Audio Settings",
                color = ContextColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = ContextColors.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    DetailRow("EQ Preset", audioProfile.eqPreset.displayName)
                    DetailRow("Bass", "${audioProfile.bassAdjustment.roundToInt()} dB")
                    DetailRow("Treble", "${audioProfile.trebleAdjustment.roundToInt()} dB")
                    DetailRow("Compression", audioProfile.compressionStrength.displayName)
                    DetailRow("Speech Enhance", if (audioProfile.speechEnhancement) "On" else "Off")
                    DetailRow("Spatial Audio", if (audioProfile.spatialAudioEnabled) "On" else "Off", false)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // UI Settings Preview
            Text(
                "UI Settings",
                color = ContextColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = ContextColors.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    DetailRow("UI Complexity", uiProfile.uiComplexity.name.lowercase().replaceFirstChar { it.uppercase() })
                    DetailRow("Control Size", uiProfile.controlSize.name.lowercase().replaceFirstChar { it.uppercase() })
                    DetailRow("Gestures", if (uiProfile.gesturesEnabled) "Enabled" else "Disabled")
                    DetailRow("Notifications", if (uiProfile.showNotifications) "On" else "Silent", false)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = modeColor),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Apply ${mode.displayName} Mode", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = ContextColors.textTertiary, fontSize = 13.sp)
            Text(value, color = ContextColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        if (showDivider) {
            HorizontalDivider(color = ContextColors.glassBorder.copy(alpha = 0.3f))
        }
    }
}

// ============================================
// 🔧 HELPER FUNCTIONS
// ============================================

private fun getModeColor(mode: ContextMode): Color {
    return when (mode) {
        ContextMode.AUTO -> ContextColors.autoColor
        ContextMode.WALK -> ContextColors.walkColor
        ContextMode.DRIVE -> ContextColors.driveColor
        ContextMode.FOCUS -> ContextColors.focusColor
        ContextMode.NIGHT -> ContextColors.nightColor
        ContextMode.CAST -> ContextColors.castColor
        ContextMode.WORKOUT -> ContextColors.workoutColor
        ContextMode.COMMUTE -> ContextColors.commuteColor
        ContextMode.CUSTOM -> ContextColors.pink
    }
}

private fun getAudioOutputIcon(output: AudioOutputType): ImageVector {
    return when (output) {
        AudioOutputType.SPEAKER -> Icons.Default.Speaker
        AudioOutputType.WIRED_HEADPHONES -> Icons.Default.Headphones
        AudioOutputType.BLUETOOTH_EARBUDS -> Icons.Default.BluetoothAudio
        AudioOutputType.BLUETOOTH_HEADPHONES -> Icons.Default.Headphones
        AudioOutputType.BLUETOOTH_SPEAKER -> Icons.Default.SpeakerGroup
        AudioOutputType.BLUETOOTH_CAR -> Icons.Default.DirectionsCar
        AudioOutputType.CAST_DEVICE -> Icons.Default.Cast
        AudioOutputType.USB_AUDIO -> Icons.Default.Usb
        AudioOutputType.HDMI -> Icons.Default.Tv
    }
}

private fun getAudioOutputLabel(output: AudioOutputType): String {
    return when (output) {
        AudioOutputType.SPEAKER -> "Speaker"
        AudioOutputType.WIRED_HEADPHONES -> "Wired"
        AudioOutputType.BLUETOOTH_EARBUDS -> "BT Earbuds"
        AudioOutputType.BLUETOOTH_HEADPHONES -> "BT Headphones"
        AudioOutputType.BLUETOOTH_SPEAKER -> "BT Speaker"
        AudioOutputType.BLUETOOTH_CAR -> "Car Audio"
        AudioOutputType.CAST_DEVICE -> "Casting"
        AudioOutputType.USB_AUDIO -> "USB Audio"
        AudioOutputType.HDMI -> "HDMI"
    }
}

private fun getTimeIcon(time: TimeOfDay): ImageVector {
    return when (time) {
        TimeOfDay.MORNING -> Icons.Default.WbSunny
        TimeOfDay.DAY -> Icons.Default.LightMode
        TimeOfDay.EVENING -> Icons.Default.WbTwilight
        TimeOfDay.NIGHT -> Icons.Default.NightsStay
    }
}

