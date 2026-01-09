package com.example.dwn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.*

// ============================================
// 🎨 PREMIUM THEME COLORS
// ============================================

private object AppColors {
    // Primary gradient - Pink/Purple/Blue
    val pink = Color(0xFFE91E63)
    val pinkLight = Color(0xFFFF4081)
    val purple = Color(0xFF9C27B0)
    val purpleLight = Color(0xFFBA68C8)
    val blue = Color(0xFF2196F3)
    val blueLight = Color(0xFF64B5F6)
    val orange = Color(0xFFFF5722)
    val cyan = Color(0xFF00BCD4)
    val teal = Color(0xFF009688)
    val amber = Color(0xFFFFC107)

    // Backgrounds
    val bgDark = Color(0xFF0D0D0D)
    val bgMid = Color(0xFF1A0A1A)
    val surface = Color(0xFF1A1A1A)
    val surfaceVariant = Color(0xFF252525)
    val card = Color(0xFF1E1E1E)
    val cardElevated = Color(0xFF2A2A2A)

    // Text
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B0)
    val textTertiary = Color(0xFF707070)
    val textMuted = Color(0xFF505050)
    val textFaint = Color(0xFF404040)

    // Status
    val success = Color(0xFF4CAF50)
    val successLight = Color(0xFF81C784)
    val error = Color(0xFFE53935)
    val warning = Color(0xFFFF9800)

    // Glass effects
    val glassWhite = Color(0x14FFFFFF)
    val glassBorder = Color(0x20FFFFFF)
    val glassHighlight = Color(0x30FFFFFF)

    // Neon glow
    val neonPink = Color(0xFFFF1493)
    val neonBlue = Color(0xFF00BFFF)
    val neonPurple = Color(0xFFDA70D6)
}

// ============================================
// 🏠 MAIN HUB SCREEN - PREMIUM EDITION
// ============================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainHubScreen(
    onNavigateToDownloader: () -> Unit,
    onNavigateToPodcast: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToScreenRecorder: () -> Unit = {},
    onNavigateToAudioEditor: () -> Unit = {},
    onNavigateToVideoEditor: () -> Unit = {},
    onNavigateToAITools: () -> Unit = {},
    onNavigateToAudioSocial: () -> Unit = {},
    onNavigateToRemixStudio: () -> Unit = {},
    onNavigateToContextMode: () -> Unit = {},
    onNavigateToMediaVault: () -> Unit = {},
    onNavigateToRadio: () -> Unit = {},
    onNavigateToFMRadio: () -> Unit = {},
    onNavigateToDJStudio: () -> Unit = {},
    onNavigateToBeatMaker: () -> Unit = {},
    onNavigateToArtistsArena: () -> Unit = {},
    totalDownloads: Int = 0,
    activeDownloads: Int = 0,
    totalMediaFiles: Int = 0,
    recentlyPlayed: List<String> = emptyList(),
    // Player state and controls
    isAudioPlaying: Boolean = false,
    currentAudioTrack: String = "",
    currentAudioArtist: String = "",
    isVideoPlaying: Boolean = false,
    currentVideoTrack: String = "",
    onAudioPlayPause: () -> Unit = {},
    onVideoPlayPause: () -> Unit = {},
    onOpenAudioPlayer: () -> Unit = {},
    onOpenVideoPlayer: () -> Unit = {}
) {
    var showContent by remember { mutableStateOf(false) }
    var selectedNav by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Menu state
    var showMenu by remember { mutableStateOf(false) }

    // Exit confirmation dialog state
    var showExitDialog by remember { mutableStateOf(false) }

    // Additional dialog states
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Current time for greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    // Exit Confirmation Dialog
    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = {
                showExitDialog = false
                android.os.Process.killProcess(android.os.Process.myPid())
            },
            onDismiss = { showExitDialog = false }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        HelpDialog(
            onDismiss = { showHelpDialog = false }
        )
    }

    LaunchedEffect(Unit) {
        delay(50)
        showContent = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgDark)
    ) {
        // Premium animated background
        PremiumAnimatedBackground()

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Premium Header with greeting
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400)) + slideInVertically { -30 }
            ) {
                PremiumHeader(
                    greeting = greeting
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hero Card - Now Playing Style
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500, 100)) + scaleIn(tween(500, 100), initialScale = 0.92f)
            ) {
                HeroCard(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToDownloader()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Artists Arena Feature Banner
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500, 150)) + slideInVertically(tween(400, 150)) { 30 }
            ) {
                ArtistsArenaBanner(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToArtistsArena()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Live Activity Ring Stats
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400, 200)) + slideInHorizontally { -50 }
            ) {
                ActivityRingsSection(
                    downloads = totalDownloads,
                    active = activeDownloads,
                    library = totalMediaFiles
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Premium Tools Grid
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(500, 300))
            ) {
                PremiumToolsSection(
                    onDownloader = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToDownloader()
                    },
                    onPodcast = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToPodcast()
                    },
                    onEqualizer = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToEqualizer()
                    },
                    onPlaylists = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToPlaylists()
                    },
                    onLibrary = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToDownloads()
                    },
                    onAudioSocial = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToAudioSocial()
                    },
                    onMediaVault = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToMediaVault()
                    },
                    onRadio = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToRadio()
                    },
                    onFMRadio = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToFMRadio()
                    },
                    onDJStudio = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToDJStudio()
                    },
                    onBeatMaker = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToBeatMaker()
                    },
                    onScreenRecorder = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToScreenRecorder()
                    },
                    onAudioEditor = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToAudioEditor()
                    },
                    onVideoEditor = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToVideoEditor()
                    },
                    onAITools = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToAITools()
                    },
                    onRemixStudio = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToRemixStudio()
                    },
                    onContextMode = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToContextMode()
                    },
                    // Player state
                    isAudioPlaying = isAudioPlaying,
                    currentAudioTrack = currentAudioTrack,
                    currentAudioArtist = currentAudioArtist,
                    isVideoPlaying = isVideoPlaying,
                    currentVideoTrack = currentVideoTrack,
                    onAudioPlayPause = onAudioPlayPause,
                    onVideoPlayPause = onVideoPlayPause,
                    onOpenAudioPlayer = onOpenAudioPlayer,
                    onOpenVideoPlayer = onOpenVideoPlayer
                )
            }


            Spacer(modifier = Modifier.height(100.dp))
        }

        // Premium Bottom Navigation
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(600, 400)) + slideInVertically { 80 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PremiumBottomNav(
                selected = selectedNav,
                onSelect = { idx ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedNav = idx
                    when (idx) {
                        0 -> onNavigateToDownloader()
                        1 -> onNavigateToAITools()
                        2 -> onNavigateToPodcast()
                        3 -> onNavigateToEqualizer()
                    }
                },
                showMenu = showMenu,
                onMenuToggle = { showMenu = !showMenu }
            )
        }

        // Glassmorphic Popup Menu - Outside nav bar
        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn(tween(300)) + slideInVertically { 100 } + scaleIn(tween(300), initialScale = 0.8f),
            exit = fadeOut(tween(200)) + slideOutVertically { 100 } + scaleOut(tween(200), targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp)
                .navigationBarsPadding()
        ) {
            BottomNavPopupMenu(
                onYouClick = {
                    showMenu = false
                    onNavigateToSettings()
                },
                onSettingsClick = {
                    showMenu = false
                    showSettingsDialog = true
                },
                onAboutClick = {
                    showMenu = false
                    showAboutDialog = true
                },
                onHelpClick = {
                    showMenu = false
                    showHelpDialog = true
                },
                onExitClick = {
                    showMenu = false
                    showExitDialog = true
                }
            )
        }
    }
}

// ============================================
// 🌈 PREMIUM ANIMATED BACKGROUND
// ============================================

@Composable
private fun PremiumAnimatedBackground() {
    val transition = rememberInfiniteTransition(label = "bg")

    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Base gradient
        drawRect(
            brush = Brush.verticalGradient(
                0f to AppColors.bgDark,
                0.3f to AppColors.bgMid,
                0.7f to Color(0xFF0F0515),
                1f to AppColors.bgDark
            )
        )

        // Animated orb 1 - Pink (top area)
        val orb1X = w * 0.8f + cos(Math.toRadians(time.toDouble())).toFloat() * w * 0.15f
        val orb1Y = h * 0.1f + sin(Math.toRadians(time * 0.7).toDouble()).toFloat() * h * 0.05f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AppColors.pink.copy(alpha = 0.3f * pulse),
                    AppColors.pink.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                center = Offset(orb1X, orb1Y),
                radius = 350f * pulse
            ),
            center = Offset(orb1X, orb1Y),
            radius = 350f * pulse
        )

        // Animated orb 2 - Purple (bottom left)
        val orb2X = w * 0.1f + cos(Math.toRadians(time * 0.5 + 120).toDouble()).toFloat() * w * 0.1f
        val orb2Y = h * 0.75f + sin(Math.toRadians(time * 0.3 + 60).toDouble()).toFloat() * h * 0.08f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AppColors.purple.copy(alpha = 0.25f),
                    AppColors.purple.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(orb2X, orb2Y),
                radius = 400f
            ),
            center = Offset(orb2X, orb2Y),
            radius = 400f
        )

        // Animated orb 3 - Blue (center right)
        val orb3X = w * 0.9f + cos(Math.toRadians(time * 0.4 + 240).toDouble()).toFloat() * w * 0.08f
        val orb3Y = h * 0.45f + sin(Math.toRadians(time * 0.6 + 180).toDouble()).toFloat() * h * 0.1f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AppColors.blue.copy(alpha = 0.2f),
                    AppColors.blue.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(orb3X, orb3Y),
                radius = 280f
            ),
            center = Offset(orb3X, orb3Y),
            radius = 280f
        )

        // Floating particles
        for (i in 0..30) {
            val px = (w * ((i * 0.033f + time / 500f) % 1f))
            val py = h * ((i * 0.05f + time / 800f) % 1f)
            val pAlpha = (0.1f + 0.2f * sin(time / 40f + i.toFloat())).coerceIn(0f, 0.3f)
            val pSize = 1.5f + 2f * sin(time / 25f + i * 0.7f)

            drawCircle(
                color = Color.White.copy(alpha = pAlpha),
                radius = pSize,
                center = Offset(px, py)
            )
        }

        // Subtle scan lines
        for (y in 0..(h / 3).toInt()) {
            if (y % 3 == 0) {
                drawLine(
                    color = Color.White.copy(alpha = 0.015f),
                    start = Offset(0f, y * 3f),
                    end = Offset(w, y * 3f),
                    strokeWidth = 1f
                )
            }
        }
    }
}

// ============================================
// 📱 PREMIUM HEADER
// ============================================

@Composable
private fun PremiumHeader(
    greeting: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                greeting,
                color = AppColors.textTertiary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "MediaGrab",
                    color = AppColors.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Pro badge
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(listOf(AppColors.pink, AppColors.purple)),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "PRO",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit = {},
    badge: Boolean = false
) {
    Box {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(AppColors.glassWhite, CircleShape)
                .border(1.dp, AppColors.glassBorder, CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = AppColors.textSecondary, modifier = Modifier.size(20.dp))
        }
        if (badge) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .background(AppColors.error, CircleShape)
                    .border(1.5.dp, AppColors.bgDark, CircleShape)
            )
        }
    }
}

// ============================================
// 🎯 MEDIAGRAB ULTRA PREMIUM BANNER
// ============================================

@Composable
private fun HeroCard(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "hero")

    // Aurora wave animation
    val auroraPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora"
    )

    // Breathing glow effect
    val glowPulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Ring rotation
    val ringRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )

    // Waveform bars - 7 bars for richer visualization
    val waves = listOf(
        transition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), "w1"),
        transition.animateFloat(0.6f, 0.9f, infiniteRepeatable(tween(500), RepeatMode.Reverse), "w2"),
        transition.animateFloat(0.4f, 0.85f, infiniteRepeatable(tween(600), RepeatMode.Reverse), "w3"),
        transition.animateFloat(0.7f, 0.4f, infiniteRepeatable(tween(550), RepeatMode.Reverse), "w4"),
        transition.animateFloat(0.5f, 0.95f, infiniteRepeatable(tween(650), RepeatMode.Reverse), "w5"),
        transition.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(580), RepeatMode.Reverse), "w6"),
        transition.animateFloat(0.55f, 0.7f, infiniteRepeatable(tween(520), RepeatMode.Reverse), "w7")
    )

    // Floating particle positions
    val particle1Y by transition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
        label = "p1"
    )
    val particle2X by transition.animateFloat(
        initialValue = 0.7f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(3500), RepeatMode.Reverse),
        label = "p2"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.96f else 1f,
        spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .scale(scale)
    ) {
        // Multi-layer glow effects
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .blur(40.dp)
                .offset(y = 12.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            AppColors.cyan.copy(alpha = glowPulse * 0.25f),
                            AppColors.purple.copy(alpha = glowPulse * 0.35f),
                            AppColors.pink.copy(alpha = glowPulse * 0.3f),
                            AppColors.blue.copy(alpha = glowPulse * 0.25f)
                        )
                    ),
                    RoundedCornerShape(32.dp)
                )
        )

        // Main banner card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0A0A12))
                .border(
                    1.5.dp,
                    Brush.sweepGradient(
                        listOf(
                            AppColors.purple.copy(alpha = 0.6f),
                            AppColors.cyan.copy(alpha = 0.4f),
                            AppColors.pink.copy(alpha = 0.5f),
                            AppColors.blue.copy(alpha = 0.3f),
                            AppColors.purple.copy(alpha = 0.6f)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
        ) {
            // Aurora background effect
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val phaseRad = Math.toRadians(auroraPhase.toDouble())

                // Aurora waves
                for (i in 0..2) {
                    val yOffset = h * (0.3f + i * 0.2f)
                    val amplitude = h * 0.15f
                    val waveY = yOffset + (sin(phaseRad + i * 0.5) * amplitude).toFloat()

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                when (i) {
                                    0 -> AppColors.purple.copy(alpha = 0.12f)
                                    1 -> AppColors.cyan.copy(alpha = 0.1f)
                                    else -> AppColors.pink.copy(alpha = 0.08f)
                                },
                                Color.Transparent
                            ),
                            radius = w * 0.4f
                        ),
                        radius = w * 0.35f,
                        center = Offset(w * (0.3f + i * 0.25f), waveY)
                    )
                }

                // Floating particles
                drawCircle(
                    color = AppColors.cyan.copy(alpha = 0.4f),
                    radius = 4f,
                    center = Offset(w * 0.15f, h * particle1Y)
                )
                drawCircle(
                    color = AppColors.pink.copy(alpha = 0.5f),
                    radius = 3f,
                    center = Offset(w * particle2X, h * 0.3f)
                )
                drawCircle(
                    color = AppColors.purple.copy(alpha = 0.3f),
                    radius = 5f,
                    center = Offset(w * 0.5f, h * (1f - particle1Y))
                )

                // Subtle grid lines
                val gridAlpha = 0.03f
                for (x in 0..10) {
                    drawLine(
                        color = Color.White.copy(alpha = gridAlpha),
                        start = Offset(w * x / 10f, 0f),
                        end = Offset(w * x / 10f, h),
                        strokeWidth = 0.5f
                    )
                }
                for (y in 0..5) {
                    drawLine(
                        color = Color.White.copy(alpha = gridAlpha),
                        start = Offset(0f, h * y / 5f),
                        end = Offset(w, h * y / 5f),
                        strokeWidth = 0.5f
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Animated premium badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    AppColors.amber.copy(alpha = 0.6f),
                                    AppColors.orange.copy(alpha = 0.4f)
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            AppColors.amber.copy(alpha = 0.15f),
                                            AppColors.orange.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Diamond,
                                contentDescription = null,
                                tint = AppColors.amber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "ULTRA PRO",
                                color = AppColors.amber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Brand name with shimmer effect
                    Text(
                        "MediaGrab",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Power tagline
                    Text(
                        "Experience the power of audio",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tagline with icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeatureChip(Icons.Outlined.MusicNote, "Audio", AppColors.cyan)
                        FeatureChip(Icons.Outlined.Videocam, "Video", AppColors.pink)
                        FeatureChip(Icons.Outlined.AutoAwesome, "AI", AppColors.purple)
                    }
                }

                // Premium waveform visualizer with ring
                Box(
                    modifier = Modifier.size(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Rotating ring
                    Canvas(
                        modifier = Modifier
                            .size(90.dp)
                            .graphicsLayer { rotationZ = ringRotation }
                    ) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    AppColors.cyan,
                                    AppColors.purple,
                                    AppColors.pink,
                                    Color.Transparent
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = 3f, cap = StrokeCap.Round)
                        )
                    }

                    // Inner circle with waveform
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF1A1A2E),
                                        Color(0xFF0D0D15)
                                    )
                                ),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Waveform bars
                        Row(
                            modifier = Modifier
                                .width(50.dp)
                                .height(35.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            waves.forEachIndexed { index, state ->
                                val barColor = listOf(
                                    AppColors.cyan,
                                    AppColors.blue,
                                    AppColors.purple,
                                    AppColors.pink,
                                    AppColors.purple,
                                    AppColors.blue,
                                    AppColors.cyan
                                )[index]

                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight(state.value)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    barColor,
                                                    barColor.copy(alpha = 0.3f)
                                                )
                                            ),
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Animated bottom gradient line
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                AppColors.cyan.copy(alpha = glowPulse * 0.8f),
                                AppColors.purple.copy(alpha = glowPulse),
                                AppColors.pink.copy(alpha = glowPulse * 0.9f),
                                AppColors.blue.copy(alpha = glowPulse * 0.7f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Top shine effect
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.6f)
                    .height(1.dp)
                    .offset(y = 1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

// ============================================
// 🎤 ARTISTS ARENA - FEATURE INTRODUCTION BANNER
// ============================================

@Composable
private fun ArtistsArenaBanner(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "artists_arena")

    // Pulsing glow
    val glowIntensity by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Sound wave animation
    val waveOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    // Floating notes animation
    val noteFloat by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "note"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.97f else 1f,
        spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "scale"
    )

    // Artist colors - warm golden/amber tones
    val artistGold = Color(0xFFFFD700)
    val artistAmber = Color(0xFFFF8C00)
    val artistRose = Color(0xFFFF6B9D)
    val artistViolet = Color(0xFF8B5CF6)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .scale(scale)
    ) {
        // Subtle glow background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .blur(25.dp)
                .offset(y = 8.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            artistGold.copy(alpha = glowIntensity * 0.15f),
                            artistAmber.copy(alpha = glowIntensity * 0.2f),
                            artistRose.copy(alpha = glowIntensity * 0.15f)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
        )

        // Main banner card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(85.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF1A1510),
                            Color(0xFF0F0D0A),
                            Color(0xFF12100E)
                        )
                    )
                )
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            artistGold.copy(alpha = 0.4f),
                            artistAmber.copy(alpha = 0.3f),
                            artistRose.copy(alpha = 0.2f),
                            artistViolet.copy(alpha = 0.15f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
        ) {
            // Animated sound waves background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw subtle sound wave lines
                for (i in 0..5) {
                    val xPos = w * (0.05f + i * 0.15f) + (waveOffset * w * 0.1f) - w * 0.05f
                    val alpha = (0.08f + sin(waveOffset * 2 * PI + i).toFloat() * 0.04f)

                    drawLine(
                        color = artistGold.copy(alpha = alpha),
                        start = Offset(xPos, h * 0.2f),
                        end = Offset(xPos, h * 0.8f),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }

                // Floating musical notes effect
                val noteY1 = h * (0.3f + noteFloat * 0.4f)
                val noteY2 = h * (0.6f - noteFloat * 0.3f)

                drawCircle(
                    color = artistGold.copy(alpha = 0.15f),
                    radius = 3f,
                    center = Offset(w * 0.85f, noteY1)
                )
                drawCircle(
                    color = artistRose.copy(alpha = 0.12f),
                    radius = 2.5f,
                    center = Offset(w * 0.9f, noteY2)
                )
            }

            // Content row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side - Icon and text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Artist icon with animated ring
                    Box(
                        modifier = Modifier.size(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow ring
                        Canvas(modifier = Modifier.size(52.dp)) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        artistGold.copy(alpha = glowIntensity * 0.6f),
                                        artistAmber.copy(alpha = glowIntensity * 0.4f),
                                        artistRose.copy(alpha = glowIntensity * 0.3f),
                                        Color.Transparent,
                                        artistGold.copy(alpha = glowIntensity * 0.5f)
                                    )
                                ),
                                style = Stroke(width = 2f)
                            )
                        }

                        // Inner circle with icon
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            artistGold.copy(alpha = 0.2f),
                                            artistAmber.copy(alpha = 0.1f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    artistGold.copy(alpha = 0.3f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = artistGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Text content
                    Column {
                        // Badge row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Introducing",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )

                            // Coming soon badge
                            Box(
                                modifier = Modifier
                                    .background(
                                        artistRose.copy(alpha = 0.2f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Artist's Arena",
                                    color = artistRose,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Main title
                        Text(
                            "Artists Arena",
                            color = artistGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            "Upload · Produce · Master · Share",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Feature highlights
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ArtistFeatureTag("Hi-Fi Audio", artistGold)
                            ArtistFeatureTag("Pro Tools", artistAmber)
                        }
                    }
                }

                // Right side - Arrow indicator
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            artistGold.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .border(
                            1.dp,
                            artistGold.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Explore",
                        tint = artistGold.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Top highlight line
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.5f)
                    .height(1.dp)
                    .offset(y = 1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                artistGold.copy(alpha = 0.4f),
                                artistAmber.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun ArtistFeatureTag(text: String, color: Color) {
    Row(
        modifier = Modifier
            .background(
                color.copy(alpha = 0.08f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(color.copy(alpha = 0.7f), CircleShape)
        )
        Text(
            text,
            color = color.copy(alpha = 0.9f),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FeatureChip(
    icon: ImageVector,
    label: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .background(
                color.copy(alpha = 0.12f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ============================================
// 📊 ACTIVITY SECTION - ELEGANT MINIMAL DESIGN
// ============================================

@Composable
private fun ActivityRingsSection(downloads: Int, active: Int, library: Int) {
    // Elegant glassmorphic container matching other tool cards
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        AppColors.glassWhite,
                        AppColors.bgDark.copy(alpha = 0.4f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        AppColors.glassBorder.copy(alpha = 0.6f),
                        AppColors.glassBorder.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElegantActivityItem(
                value = downloads,
                label = "Downloads",
                color = AppColors.pink,
                icon = Icons.Filled.Download
            )

            // Subtle divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(44.dp)
                    .background(AppColors.glassBorder.copy(alpha = 0.2f))
            )

            ElegantActivityItem(
                value = active,
                label = "Active",
                color = AppColors.success,
                icon = Icons.Filled.Speed,
                isLive = active > 0
            )

            // Subtle divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(44.dp)
                    .background(AppColors.glassBorder.copy(alpha = 0.2f))
            )

            ElegantActivityItem(
                value = library,
                label = "Library",
                color = AppColors.blue,
                icon = Icons.Filled.Folder
            )
        }
    }
}

@Composable
private fun ElegantActivityItem(
    value: Int,
    label: String,
    color: Color,
    icon: ImageVector,
    isLive: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "activity")
    val livePulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isLive) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 6.dp)
    ) {
        // Minimal icon with subtle background
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    color.copy(alpha = 0.08f),
                    RoundedCornerShape(9.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = color.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(15.dp)
                    .scale(livePulse)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Value with optional live indicator
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$value",
                color = AppColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (isLive) {
                Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .scale(livePulse)
                        .background(AppColors.success, CircleShape)
                )
            }
        }

        // Label
        Text(
            label,
            color = AppColors.textTertiary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

// Keeping the old ActivityRingCard for backward compatibility but marking unused
@Suppress("unused")
@Composable
private fun ActivityRingCard(
    value: Int,
    label: String,
    color: Color,
    maxValue: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isLive: Boolean = false
) {
    val progress = (value.toFloat() / maxValue).coerceIn(0f, 1f)

    val transition = rememberInfiniteTransition(label = "ring")
    val animatedProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = progress,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val livePulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isLive) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .background(AppColors.glassWhite, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.glassBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Ring
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background ring
                    drawArc(
                        color = color.copy(alpha = 0.2f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Progress ring
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Icon(
                    icon,
                    null,
                    tint = color,
                    modifier = Modifier
                        .size(22.dp)
                        .scale(livePulse)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$value",
                    color = AppColors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isLive) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .scale(livePulse)
                            .background(AppColors.success, CircleShape)
                    )
                }
            }

            Text(
                label,
                color = AppColors.textTertiary,
                fontSize = 11.sp
            )
        }
    }
}

// ============================================
// 🛠 UNIFIED TOOLS SECTION - THREE CATEGORIES
// ============================================

@Composable
private fun PremiumToolsSection(
    onDownloader: () -> Unit,
    onPodcast: () -> Unit,
    onEqualizer: () -> Unit,
    onPlaylists: () -> Unit,
    onLibrary: () -> Unit,
    onAudioSocial: () -> Unit = {},
    onMediaVault: () -> Unit = {},
    onRadio: () -> Unit = {},
    onFMRadio: () -> Unit = {},
    onDJStudio: () -> Unit = {},
    onBeatMaker: () -> Unit = {},
    onScreenRecorder: () -> Unit = {},
    onAudioEditor: () -> Unit = {},
    onVideoEditor: () -> Unit = {},
    onAITools: () -> Unit = {},
    onRemixStudio: () -> Unit = {},
    onContextMode: () -> Unit = {},
    // Player state
    isAudioPlaying: Boolean = false,
    currentAudioTrack: String = "",
    currentAudioArtist: String = "",
    isVideoPlaying: Boolean = false,
    currentVideoTrack: String = "",
    onAudioPlayPause: () -> Unit = {},
    onVideoPlayPause: () -> Unit = {},
    onOpenAudioPlayer: () -> Unit = {},
    onOpenVideoPlayer: () -> Unit = {}
) {
    var selectedCategory by remember { mutableIntStateOf(0) }
    val categories = listOf("Audio", "Video", "Studio", "Rooms", "Activity")

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // Category Tabs
        CategoryTabRow(
            categories = categories,
            selectedIndex = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tools based on selected category
        AnimatedContent(
            targetState = selectedCategory,
            transitionSpec = {
                fadeIn(tween(300)) + slideInHorizontally { if (targetState > initialState) it / 4 else -it / 4 } togetherWith
                fadeOut(tween(200)) + slideOutHorizontally { if (targetState > initialState) -it / 4 else it / 4 }
            },
            label = "category_content"
        ) { category ->
            when (category) {
                0 -> AudioToolsGrid(
                    onPlaylists = onPlaylists,
                    onAudioSocial = onAudioSocial,
                    onAudioEditor = onAudioEditor,
                    onContextMode = onContextMode,
                    onFMRadio = onFMRadio,
                    isPlaying = isAudioPlaying,
                    trackName = currentAudioTrack,
                    artistName = currentAudioArtist,
                    onPlayPause = onAudioPlayPause,
                    onOpenPlayer = onOpenAudioPlayer
                )
                1 -> VideoToolsGrid(
                    onScreenRecorder = onScreenRecorder,
                    onVideoEditor = onVideoEditor,
                    onLibrary = onLibrary,
                    onMediaVault = onMediaVault,
                    isPlaying = isVideoPlaying,
                    trackName = currentVideoTrack,
                    onPlayPause = onVideoPlayPause,
                    onOpenPlayer = onOpenVideoPlayer
                )
                2 -> StudioToolsGrid(
                    onPodcast = onPodcast,
                    onDJStudio = onDJStudio,
                    onBeatMaker = onBeatMaker,
                    onRemixStudio = onRemixStudio,
                    onRadio = onRadio,
                    onMediaVault = onMediaVault
                )
                3 -> MyRoomsSection(
                    onAudioSocial = onAudioSocial,
                    onPodcast = onPodcast
                )
                4 -> MyActivitySection()
            }
        }
    }
}

@Composable
private fun CategoryTabRow(
    categories: List<String>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit
) {
    val transition = rememberInfiniteTransition(label = "tab_glow")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.glassWhite, RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.glassBorder, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        categories.forEachIndexed { index, category ->
            val isSelected = index == selectedIndex
            val color = when (index) {
                0 -> AppColors.pink       // Audio
                1 -> AppColors.purple     // Video
                2 -> AppColors.blue       // Studio
                3 -> AppColors.cyan       // Rooms
                else -> AppColors.amber   // Activity
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(
                                Brush.horizontalGradient(
                                    listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.15f))
                                )
                            )
                        } else Modifier
                    )
                    .clickable { onCategorySelected(index) }
                    .padding(vertical = 10.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    category,
                    color = if (isSelected) color else AppColors.textTertiary,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

// ============================================
// 🎵 AUDIO TOOLS GRID - ELEGANT DESIGN
// ============================================

@Composable
private fun AudioToolsGrid(
    onPlaylists: () -> Unit,
    onAudioSocial: () -> Unit,
    onAudioEditor: () -> Unit,
    onContextMode: () -> Unit,
    onFMRadio: () -> Unit = {},
    isPlaying: Boolean = false,
    trackName: String = "",
    artistName: String = "",
    onPlayPause: () -> Unit = {},
    onOpenPlayer: () -> Unit = {}
) {
    // Stunning glassmorphic container card
    ElegantToolsContainer(title = "Audio Tools", accentColor = AppColors.pink) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 🎵 Now Playing / Player Card
            NowPlayingCard(
                isPlaying = isPlaying,
                trackName = if (trackName.isNotEmpty()) trackName else "No track playing",
                artistName = if (artistName.isNotEmpty()) artistName else "Tap to browse music",
                onPlayPause = onPlayPause,
                onOpenPlayer = onOpenPlayer,
                accentColor = AppColors.pink
            )

            // 🌟 PRO Feature - Audio Space (Compact hero)
            CompactHeroCard(
                icon = Icons.Outlined.Headphones,
                title = "Audio Space",
                subtitle = "Social Audio Rooms",
                gradient = listOf(AppColors.cyan, AppColors.teal),
                onClick = onAudioSocial,
                badge = "⭐ PRO"
            )

            // Elegant tool buttons grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElegantToolButton(
                    icon = Icons.AutoMirrored.Outlined.QueueMusic,
                    label = "Playlists",
                    color = AppColors.pink,
                    onClick = onPlaylists,
                    modifier = Modifier.weight(1f)
                )
                ElegantToolButton(
                    icon = Icons.Outlined.Audiotrack,
                    label = "Audio Editor",
                    color = AppColors.success,
                    onClick = onAudioEditor,
                    modifier = Modifier.weight(1f)
                )
                ElegantToolButton(
                    icon = Icons.Outlined.Radio,
                    label = "FM Radio",
                    color = AppColors.orange,
                    onClick = onFMRadio,
                    modifier = Modifier.weight(1f)
                )
            }

            // Second row with more tools
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElegantToolButton(
                    icon = Icons.Outlined.AutoMode,
                    label = "Context Mode",
                    color = AppColors.amber,
                    onClick = onContextMode,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================
// 🎬 VIDEO TOOLS GRID - ELEGANT DESIGN
// ============================================

@Composable
private fun VideoToolsGrid(
    onScreenRecorder: () -> Unit,
    onVideoEditor: () -> Unit,
    onLibrary: () -> Unit,
    onMediaVault: () -> Unit,
    isPlaying: Boolean = false,
    trackName: String = "",
    onPlayPause: () -> Unit = {},
    onOpenPlayer: () -> Unit = {}
) {
    ElegantToolsContainer(title = "Video Tools", accentColor = AppColors.purple) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 🎬 Video Player Card
            NowPlayingCard(
                isPlaying = isPlaying,
                trackName = if (trackName.isNotEmpty()) trackName else "No video playing",
                artistName = "Tap to browse videos",
                onPlayPause = onPlayPause,
                onOpenPlayer = onOpenPlayer,
                accentColor = AppColors.purple,
                isVideo = true
            )

            // Featured compact cards row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactFeatureCard(
                    icon = Icons.Outlined.ScreenshotMonitor,
                    title = "Screen Rec",
                    subtitle = "HD Capture",
                    color = AppColors.cyan,
                    onClick = onScreenRecorder,
                    modifier = Modifier.weight(1f)
                )
                CompactFeatureCard(
                    icon = Icons.Outlined.VideoSettings,
                    title = "Editor",
                    subtitle = "Cut & Trim",
                    color = AppColors.orange,
                    onClick = onVideoEditor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Elegant tool buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElegantToolButton(
                    icon = Icons.Outlined.Folder,
                    label = "Library",
                    color = AppColors.success,
                    onClick = onLibrary,
                    modifier = Modifier.weight(1f)
                )
                ElegantToolButton(
                    icon = Icons.Outlined.Inventory2,
                    label = "Vault",
                    color = AppColors.pink,
                    onClick = onMediaVault,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================
// 🎛️ STUDIO TOOLS GRID - ELEGANT DESIGN
// ============================================

@Composable
private fun StudioToolsGrid(
    onPodcast: () -> Unit,
    onDJStudio: () -> Unit,
    onBeatMaker: () -> Unit,
    onRemixStudio: () -> Unit,
    onRadio: () -> Unit,
    onMediaVault: () -> Unit
) {
    ElegantToolsContainer(title = "Studio Tools", accentColor = AppColors.blue) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 🐐 GOAT Feature - Podcast Studio (Compact hero)
            CompactHeroCard(
                icon = Icons.Outlined.Mic,
                title = "Podcast Studio",
                subtitle = "Pro Recording & Production",
                gradient = listOf(AppColors.purple, AppColors.pink),
                onClick = onPodcast,
                badge = "🐐 GOAT"
            )

            // Featured compact cards row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactFeatureCard(
                    icon = Icons.Outlined.Album,
                    title = "DJ Studio",
                    subtitle = "Mix & Scratch",
                    color = AppColors.blue,
                    onClick = onDJStudio,
                    modifier = Modifier.weight(1f)
                )
                CompactFeatureCard(
                    icon = Icons.Outlined.MusicNote,
                    title = "Beat Maker",
                    subtitle = "Create Beats",
                    color = AppColors.neonPink,
                    onClick = onBeatMaker,
                    modifier = Modifier.weight(1f)
                )
            }

            // Elegant tool buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElegantToolButton(
                    icon = Icons.Outlined.MusicVideo,
                    label = "Remix",
                    color = AppColors.pink,
                    onClick = onRemixStudio,
                    modifier = Modifier.weight(1f)
                )
                ElegantToolButton(
                    icon = Icons.Outlined.Radio,
                    label = "Radio",
                    color = AppColors.orange,
                    onClick = onRadio,
                    modifier = Modifier.weight(1f)
                )
                ElegantToolButton(
                    icon = Icons.Outlined.Inventory2,
                    label = "Vault",
                    color = AppColors.success,
                    onClick = onMediaVault,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ============================================
// 🏠 MY ROOMS SECTION - Audio Space & Podcast Rooms
// ============================================

@Composable
private fun MyRoomsSection(
    onAudioSocial: () -> Unit,
    onPodcast: () -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Audio Space", "Podcast")

    ElegantToolsContainer(title = "My Rooms", accentColor = AppColors.cyan) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Elegant sub-tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.bgDark.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .border(1.dp, AppColors.glassBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                subTabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedSubTab
                    val color = if (index == 0) AppColors.cyan else AppColors.purple

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        color.copy(alpha = 0.15f)
                                    )
                                } else Modifier
                            )
                            .clickable { selectedSubTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                if (index == 0) Icons.Outlined.Headphones else Icons.Outlined.Mic,
                                null,
                                tint = if (isSelected) color else AppColors.textTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                tab,
                                color = if (isSelected) color else AppColors.textTertiary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                            if (index == 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Brush.linearGradient(listOf(AppColors.amber, AppColors.orange)),
                                            RoundedCornerShape(3.dp)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        "PRO",
                                        color = Color.White,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Content based on selected sub-tab
            AnimatedContent(
                targetState = selectedSubTab,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                label = "rooms_content"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> AudioSpaceRoomsContent(onJoinRoom = onAudioSocial)
                    1 -> PodcastRoomsContent(onJoinRoom = onPodcast)
                }
            }
        }
    }
}

@Composable
private fun AudioSpaceRoomsContent(onJoinRoom: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Create Room Card
        CreateRoomCard(
            title = "Create Audio Space",
            subtitle = "Start a live audio room",
            icon = Icons.Outlined.AddCircleOutline,
            gradient = listOf(AppColors.cyan, AppColors.teal),
            onClick = onJoinRoom,
            isPro = true
        )

        // Section Header - Your Rooms
        SectionHeader(title = "Your Rooms", count = 2)

        // User's rooms
        RoomCard(
            title = "Music Lovers Hangout",
            host = "You",
            listeners = 12,
            isLive = true,
            gradient = listOf(AppColors.cyan, AppColors.teal),
            onClick = onJoinRoom
        )

        RoomCard(
            title = "Late Night Vibes",
            host = "You",
            listeners = 0,
            isLive = false,
            gradient = listOf(AppColors.blue, AppColors.blueLight),
            onClick = onJoinRoom
        )

        // Section Header - Available Rooms
        SectionHeader(title = "Available Rooms", count = 5)

        // Available rooms
        RoomCard(
            title = "Hip Hop Heads 🎤",
            host = "@musicpro",
            listeners = 45,
            isLive = true,
            gradient = listOf(AppColors.pink, AppColors.pinkLight),
            onClick = onJoinRoom
        )

        RoomCard(
            title = "Chill Lo-Fi Session",
            host = "@lofibeats",
            listeners = 128,
            isLive = true,
            gradient = listOf(AppColors.purple, AppColors.purpleLight),
            onClick = onJoinRoom
        )

        RoomCard(
            title = "Jazz & Soul Corner",
            host = "@jazzmaster",
            listeners = 34,
            isLive = true,
            gradient = listOf(AppColors.amber, AppColors.orange),
            onClick = onJoinRoom
        )
    }
}

@Composable
private fun PodcastRoomsContent(onJoinRoom: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Create Room Card
        CreateRoomCard(
            title = "Start Podcast Session",
            subtitle = "Record with guests",
            icon = Icons.Outlined.Mic,
            gradient = listOf(AppColors.purple, AppColors.pink),
            onClick = onJoinRoom,
            isPro = false
        )

        // Section Header - Your Sessions
        SectionHeader(title = "Your Sessions", count = 1)

        // User's podcast sessions
        RoomCard(
            title = "Tech Talk Weekly",
            host = "You",
            listeners = 3,
            isLive = false,
            isPodcast = true,
            gradient = listOf(AppColors.purple, AppColors.pink),
            onClick = onJoinRoom
        )

        // Section Header - Join Sessions
        SectionHeader(title = "Join Sessions", count = 3)

        // Available podcast sessions
        RoomCard(
            title = "Morning Coffee Chat ☕",
            host = "@podcastqueen",
            listeners = 2,
            isLive = true,
            isPodcast = true,
            gradient = listOf(AppColors.orange, AppColors.amber),
            onClick = onJoinRoom
        )

        RoomCard(
            title = "Indie Music Discovery",
            host = "@indiehost",
            listeners = 4,
            isLive = true,
            isPodcast = true,
            gradient = listOf(AppColors.teal, AppColors.cyan),
            onClick = onJoinRoom
        )

        RoomCard(
            title = "Creative Minds Podcast",
            host = "@creativepod",
            listeners = 5,
            isLive = false,
            isPodcast = true,
            gradient = listOf(AppColors.pink, AppColors.pinkLight),
            onClick = onJoinRoom
        )
    }
}

@Composable
private fun CreateRoomCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    isPro: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = 0.7f),
        label = "scale"
    )

    val transition = rememberInfiniteTransition(label = "create_glow")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
    ) {
        // Glow effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .blur(20.dp)
                .background(
                    Brush.horizontalGradient(
                        gradient.map { it.copy(alpha = glowAlpha * 0.3f) }
                    ),
                    RoundedCornerShape(20.dp)
                )
        )

        // Main card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(gradient),
                    RoundedCornerShape(20.dp)
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.3f),
                    RoundedCornerShape(20.dp)
                )
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPro) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.25f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "⭐ PRO",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }

            Icon(
                Icons.Default.Add,
                null,
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .padding(6.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = AppColors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .background(AppColors.glassWhite, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "$count",
                color = AppColors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RoomCard(
    title: String,
    host: String,
    listeners: Int,
    isLive: Boolean,
    gradient: List<Color>,
    onClick: () -> Unit,
    isPodcast: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        spring(dampingRatio = 0.7f),
        label = "scale"
    )

    val transition = rememberInfiniteTransition(label = "live_pulse")
    val livePulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(AppColors.glassWhite, RoundedCornerShape(16.dp))
            .border(1.dp, gradient[0].copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with gradient
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.linearGradient(gradient),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPodcast) Icons.Outlined.Mic else Icons.Outlined.Headphones,
                null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = AppColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    host,
                    color = AppColors.textTertiary,
                    fontSize = 12.sp
                )
                if (listeners > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Person,
                            null,
                            tint = AppColors.textTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            "$listeners",
                            color = AppColors.textTertiary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Live indicator or Join button
        if (isLive) {
            Box(
                modifier = Modifier
                    .background(
                        AppColors.error.copy(alpha = livePulse * 0.2f),
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        AppColors.error.copy(alpha = livePulse),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AppColors.error, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "LIVE",
                        color = AppColors.error,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(gradient),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "Join",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ============================================
// 🤖 AI TOOLS GRID - FULL FEATURED
// ============================================

private data class AIToolItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val category: String
)

@Composable
private fun AIToolsGrid(
    onAITools: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("all") }
    val haptic = LocalHapticFeedback.current

    val allTools = remember {
        listOf(
            // Audio AI Tools
            AIToolItem("transcribe", "Transcribe", "Speech to Text", Icons.Outlined.Subtitles, listOf(AppColors.pink, AppColors.pinkLight), "audio"),
            AIToolItem("enhance_audio", "Enhance Audio", "Noise Removal", Icons.Outlined.GraphicEq, listOf(AppColors.purple, AppColors.purpleLight), "audio"),
            AIToolItem("voice_clone", "Voice Clone", "Clone Voices", Icons.Outlined.RecordVoiceOver, listOf(AppColors.blue, AppColors.blueLight), "audio"),
            AIToolItem("vocal_separator", "Vocal Separator", "Extract Vocals", Icons.Outlined.MusicNote, listOf(AppColors.cyan, AppColors.teal), "audio"),

            // Video AI Tools
            AIToolItem("upscale", "Upscale Video", "4K Enhancement", Icons.Outlined.Hd, listOf(AppColors.success, AppColors.successLight), "video"),
            AIToolItem("remove_bg", "Remove BG", "Background Removal", Icons.Outlined.ContentCut, listOf(AppColors.orange, AppColors.warning), "video"),
            AIToolItem("auto_caption", "Auto Caption", "Generate Subtitles", Icons.Outlined.ClosedCaption, listOf(AppColors.amber, AppColors.orange), "video"),
            AIToolItem("scene_detect", "Scene Detect", "Smart Chapters", Icons.Outlined.ViewTimeline, listOf(AppColors.teal, AppColors.cyan), "video"),

            // Image AI Tools
            AIToolItem("style_transfer", "Style Transfer", "Artistic Filters", Icons.Outlined.Palette, listOf(AppColors.pink, AppColors.pinkLight), "image"),
            AIToolItem("face_enhance", "Face Enhance", "Portrait Boost", Icons.Outlined.Face, listOf(AppColors.purple, AppColors.purpleLight), "image"),
            AIToolItem("image_upscale", "Image Upscale", "HD Enhancement", Icons.Outlined.PhotoSizeSelectLarge, listOf(AppColors.blue, AppColors.blueLight), "image"),

            // Text AI Tools
            AIToolItem("summarize", "Summarize", "AI Summary", Icons.Outlined.Summarize, listOf(AppColors.blue, AppColors.blueLight), "text"),
            AIToolItem("translate", "Translate", "Multi-language", Icons.Outlined.Translate, listOf(AppColors.success, AppColors.successLight), "text"),
            AIToolItem("generate_text", "Generate Text", "AI Writing", Icons.Outlined.AutoAwesome, listOf(AppColors.amber, AppColors.orange), "text")
        )
    }

    val categories = listOf("all" to "All", "audio" to "Audio", "video" to "Video", "image" to "Image", "text" to "Text")
    val filteredTools = if (selectedCategory == "all") allTools else allTools.filter { it.category == selectedCategory }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { (id, label) ->
                val isSelected = selectedCategory == id

                Surface(
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedCategory = id
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) AppColors.purple else AppColors.glassWhite,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) AppColors.purple else AppColors.glassBorder
                    )
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else AppColors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // AI Tools Grid with proper cards
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            filteredTools.chunked(2).forEach { rowTools ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowTools.forEach { tool ->
                        AIToolCard(
                            tool = tool,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAITools()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Add spacer if odd number of items
                    if (rowTools.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Quick AI Actions Section
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Quick Actions",
            color = AppColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = AppColors.glassWhite,
            border = BorderStroke(1.dp, AppColors.glassBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                QuickAIActionRow(
                    icon = Icons.Outlined.Mic,
                    title = "Record & Transcribe",
                    subtitle = "Start recording and get instant transcription",
                    color = AppColors.pink,
                    onClick = onAITools
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = AppColors.glassBorder
                )

                QuickAIActionRow(
                    icon = Icons.Outlined.Upload,
                    title = "Upload & Enhance",
                    subtitle = "Improve audio/video quality automatically",
                    color = AppColors.purple,
                    onClick = onAITools
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = AppColors.glassBorder
                )

                QuickAIActionRow(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Smart Edit",
                    subtitle = "Let AI edit your content intelligently",
                    color = AppColors.blue,
                    onClick = onAITools
                )
            }
        }
    }
}

@Composable
private fun AIToolCard(
    tool: AIToolItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isProcessing by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    isProcessing = true
                    onClick()
                }
            ),
        shape = RoundedCornerShape(18.dp),
        color = AppColors.glassWhite,
        border = BorderStroke(1.dp, AppColors.glassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Brush.linearGradient(tool.gradient.map { it.copy(alpha = 0.2f) }),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = tool.gradient.first(),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            tool.icon,
                            contentDescription = null,
                            tint = tool.gradient.first(),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // AI badge
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(listOf(AppColors.purple, AppColors.pink)),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "AI",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                tool.name,
                color = AppColors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                tool.description,
                color = AppColors.textTertiary,
                fontSize = 11.sp
            )
        }
    }

    // Reset processing state
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            delay(500)
            isProcessing = false
        }
    }
}

@Composable
private fun QuickAIActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = AppColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = AppColors.textTertiary, fontSize = 12.sp)
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = AppColors.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================
// ✨ ELEGANT TOOLS CONTAINER - Glassmorphic Card
// ============================================

@Composable
private fun ElegantToolsContainer(
    title: String,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "container_glow")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Subtle outer glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .blur(20.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main glassmorphic container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            AppColors.glassWhite,
                            AppColors.bgDark.copy(alpha = 0.4f)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            AppColors.glassBorder.copy(alpha = 0.6f),
                            AppColors.glassBorder.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(16.dp),
            content = content
        )
    }
}

// ============================================
// 🌟 COMPACT HERO CARD - For PRO/GOAT Features
// ============================================

@Composable
private fun CompactHeroCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    onClick: () -> Unit,
    badge: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.97f else 1f,
        spring(dampingRatio = 0.7f),
        label = "scale"
    )

    val transition = rememberInfiniteTransition(label = "hero")
    val glowPulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        gradient[0].copy(alpha = 0.15f),
                        gradient.getOrElse(1) { gradient[0] }.copy(alpha = 0.08f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    gradient.map { it.copy(alpha = 0.4f) }
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with subtle glow
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .blur(10.dp)
                    .background(
                        gradient[0].copy(alpha = glowPulse * 0.5f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        Brush.linearGradient(gradient),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = AppColors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (badge.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(gradient),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            badge,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                subtitle,
                color = AppColors.textTertiary,
                fontSize = 11.sp
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = gradient[0].copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================
// 🎯 COMPACT FEATURE CARD - Secondary Features
// ============================================

@Composable
private fun CompactFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        spring(dampingRatio = 0.7f),
        label = "scale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .background(
                color.copy(alpha = 0.08f),
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.2f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            title,
            color = AppColors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Text(
            subtitle,
            color = AppColors.textTertiary,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================
// 🔘 ELEGANT TOOL BUTTON - Minimal & Professional
// ============================================

@Composable
private fun ElegantToolButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.92f else 1f,
        spring(dampingRatio = 0.7f),
        label = "scale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .background(
                AppColors.bgDark.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            color = AppColors.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ============================================
// 🎵 NOW PLAYING CARD - For Audio/Video Tabs
// ============================================

@Composable
private fun NowPlayingCard(
    isPlaying: Boolean,
    trackName: String,
    artistName: String,
    onPlayPause: () -> Unit,
    onOpenPlayer: () -> Unit,
    accentColor: Color,
    isVideo: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.98f else 1f,
        spring(dampingRatio = 0.7f),
        label = "scale"
    )

    val transition = rememberInfiniteTransition(label = "playing")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.08f),
                        accentColor.copy(alpha = 0.03f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                accentColor.copy(alpha = if (isPlaying) pulseAlpha else 0.2f),
                RoundedCornerShape(16.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onOpenPlayer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art / Video thumbnail placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    accentColor.copy(alpha = 0.15f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isVideo) Icons.Outlined.VideoLibrary else Icons.Outlined.MusicNote,
                null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Track info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isVideo) "Video Player" else "Audio Player",
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                trackName,
                color = AppColors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                artistName,
                color = AppColors.textTertiary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        // Play/Pause button
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accentColor.copy(alpha = 0.15f), CircleShape)
                .clickable { onPlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Open player button
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = accentColor.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================
// 📊 MY ACTIVITY SECTION - Unified Activity Center
// ============================================

@Composable
private fun MyActivitySection() {
    ElegantToolsContainer(title = "Activity Center", accentColor = AppColors.amber) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Activity Stats Summary
            ActivityStatsSummary()

            // Recent Activity List
            Text(
                "Recent Activity",
                color = AppColors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Podcast Activity
            ActivityNotificationCard(
                icon = Icons.Outlined.Mic,
                title = "Podcast Studio",
                subtitle = "3 new listeners on 'Tech Talk Weekly'",
                timestamp = "2 min ago",
                color = AppColors.purple,
                badge = "🎙️",
                isLive = true
            )

            // Audio Space Activity
            ActivityNotificationCard(
                icon = Icons.Outlined.Headphones,
                title = "Audio Space",
                subtitle = "15 people joined 'Music Lovers Hangout'",
                timestamp = "10 min ago",
                color = AppColors.cyan,
                badge = "🎧"
            )

            // Community Activity
            ActivityNotificationCard(
                icon = Icons.Outlined.Forum,
                title = "Community",
                subtitle = "@musicpro commented on your playlist",
                timestamp = "25 min ago",
                color = AppColors.pink,
                badge = "💬"
            )

            // Download Activity
            ActivityNotificationCard(
                icon = Icons.Outlined.CloudDownload,
                title = "Downloads",
                subtitle = "3 files completed successfully",
                timestamp = "1 hour ago",
                color = AppColors.success,
                badge = "✅"
            )

            // Remix/DJ Activity
            ActivityNotificationCard(
                icon = Icons.Outlined.Album,
                title = "DJ Studio",
                subtitle = "Your mix 'Summer Vibes' got 42 plays",
                timestamp = "2 hours ago",
                color = AppColors.blue,
                badge = "🎚️"
            )

            // Beat Maker Activity
            ActivityNotificationCard(
                icon = Icons.Outlined.MusicNote,
                title = "Beat Maker",
                subtitle = "New beat template available",
                timestamp = "5 hours ago",
                color = AppColors.neonPink,
                badge = "🎹"
            )

            // View all button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(AppColors.glassWhite, RoundedCornerShape(12.dp))
                    .clickable { }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "View All Activity",
                    color = AppColors.amber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ActivityStatsSummary() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.bgDark.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActivityStatItem(
            value = "12",
            label = "Live Rooms",
            color = AppColors.cyan
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(36.dp)
                .background(AppColors.glassBorder.copy(alpha = 0.3f))
        )

        ActivityStatItem(
            value = "48",
            label = "Listeners",
            color = AppColors.purple
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(36.dp)
                .background(AppColors.glassBorder.copy(alpha = 0.3f))
        )

        ActivityStatItem(
            value = "5",
            label = "New Messages",
            color = AppColors.pink
        )
    }
}

@Composable
private fun ActivityStatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = AppColors.textTertiary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ActivityNotificationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    timestamp: String,
    color: Color,
    badge: String = "",
    isLive: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "live")
    val livePulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color.copy(alpha = 0.05f),
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                color.copy(alpha = if (isLive) livePulse * 0.3f else 0.1f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with badge
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
            }

            // Live indicator
            if (isLive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(10.dp)
                        .background(AppColors.success, CircleShape)
                        .border(2.dp, AppColors.bgDark, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = AppColors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (badge.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(badge, fontSize = 10.sp)
                }
                if (isLive) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(AppColors.success.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "LIVE",
                            color = AppColors.success,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                subtitle,
                color = AppColors.textSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        // Timestamp
        Text(
            timestamp,
            color = AppColors.textTertiary,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun PremiumToolCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    featured: Boolean = false,
    isGoat: Boolean = false,
    isPro: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        spring(dampingRatio = 0.65f),
        label = "scale"
    )

    val transition = rememberInfiniteTransition(label = "tool")
    val iconGlow by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Special GOAT animation
    val goatPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "goat_pulse"
    )

    val goatGlow by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "goat_glow"
    )

    if (isGoat) {
        // 🐐 GOAT Feature Card - Special Hero Design
        Box(
            modifier = modifier
                .scale(scale)
                .height(100.dp)
        ) {
            // Animated border glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
                    .background(
                        Brush.sweepGradient(
                            0f to gradient[0].copy(alpha = goatGlow * 0.6f),
                            0.33f to gradient.getOrElse(1) { gradient[0] }.copy(alpha = goatGlow * 0.4f),
                            0.66f to gradient.getOrElse(2) { gradient[0] }.copy(alpha = goatGlow * 0.6f),
                            1f to gradient[0].copy(alpha = goatGlow * 0.4f)
                        ),
                        RoundedCornerShape(22.dp)
                    )
            )

            // Main card
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                AppColors.card.copy(alpha = 0.95f),
                                AppColors.bgMid.copy(alpha = 0.98f)
                            )
                        ),
                        RoundedCornerShape(22.dp)
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = gradient.map { it.copy(alpha = 0.6f) }
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with enhanced glow
                Box(contentAlignment = Alignment.Center) {
                    // Outer glow ring
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .blur(18.dp)
                            .background(
                                gradient[0].copy(alpha = goatGlow * 0.5f),
                                CircleShape
                            )
                    )

                    // Icon container
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Brush.linearGradient(gradient),
                                RoundedCornerShape(16.dp)
                            )
                            .border(
                                2.dp,
                                Color.White.copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text content
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            color = AppColors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // PRO or GOAT badge
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        if (isPro) listOf(AppColors.amber, AppColors.orange) else gradient
                                    ),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (isPro) "⭐ PRO" else "🐐 GOAT",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            subtitle,
                            color = AppColors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                // Arrow indicator
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    tint = gradient[0],
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    } else {
        // Standard card
        Column(
            modifier = modifier
                .scale(scale)
                .background(AppColors.glassWhite, RoundedCornerShape(18.dp))
                .border(1.dp, gradient[0].copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(if (featured) 16.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon with glow
            Box(contentAlignment = Alignment.Center) {
                // Glow effect
                if (featured) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .blur(12.dp)
                            .background(
                                gradient[0].copy(alpha = iconGlow * 0.4f),
                                CircleShape
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(if (featured) 44.dp else 38.dp)
                        .background(
                            Brush.linearGradient(gradient),
                            RoundedCornerShape(if (featured) 14.dp else 11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(if (featured) 24.dp else 20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (featured) 12.dp else 10.dp))

            Text(
                title,
                color = AppColors.textPrimary,
                fontSize = if (featured) 14.sp else 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (subtitle != null && featured) {
                Text(
                    subtitle,
                    color = AppColors.textTertiary,
                    fontSize = 11.sp
                )
            }
        }
    }
}


// ============================================
// 🎹 PREMIUM BOTTOM NAV
// ============================================

@Composable
private fun PremiumBottomNav(
    selected: Int,
    onSelect: (Int) -> Unit,
    showMenu: Boolean,
    onMenuToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        // Blur background effect
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = AppColors.card.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, AppColors.glassBorder),
            shadowElevation = 20.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(Icons.Outlined.CloudDownload, Icons.Filled.CloudDownload, "Media", selected == 0) { onSelect(0) }
                NavBarItem(Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome, "AI", selected == 1) { onSelect(1) }
                CenterRecordButton(selected == 2) { onSelect(2) }
                NavBarItem(Icons.Outlined.Equalizer, Icons.Filled.Equalizer, "Audio", selected == 3) { onSelect(3) }
                NavBarMenuButton(
                    showMenu = showMenu,
                    onClick = onMenuToggle
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    outlined: ImageVector,
    filled: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        if (isSelected) AppColors.pink else AppColors.textTertiary,
        tween(250),
        label = "color"
    )
    val scale by animateFloatAsState(
        if (isSelected) 1.1f else 1f,
        spring(dampingRatio = 0.6f),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (isSelected) filled else outlined,
            label,
            tint = color,
            modifier = Modifier
                .size(22.dp)
                .scale(scale)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            label,
            color = color,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun CenterRecordButton(isSelected: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "record")

    val glow by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val ringScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(64.dp)
    ) {
        // Outer pulse ring
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(ringScale)
                .border(
                    2.dp,
                    AppColors.pink.copy(alpha = glow * 0.4f),
                    CircleShape
                )
        )

        // Glow
        Box(
            modifier = Modifier
                .size(52.dp)
                .blur(14.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            AppColors.pink.copy(alpha = glow * 0.5f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    Brush.linearGradient(
                        listOf(AppColors.pink, AppColors.purple)
                    ),
                    CircleShape
                )
                .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                "Record",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================
// 🍔 NAV BAR MENU BUTTON (More - trigger only)
// ============================================

@Composable
private fun NavBarMenuButton(
    showMenu: Boolean,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        if (showMenu) AppColors.pink else AppColors.textTertiary,
        tween(250),
        label = "color"
    )
    val scale by animateFloatAsState(
        if (showMenu) 1.1f else 1f,
        spring(dampingRatio = 0.6f),
        label = "scale"
    )
    val rotation by animateFloatAsState(
        if (showMenu) 180f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (showMenu) Icons.Filled.ExpandLess else Icons.Outlined.MoreHoriz,
            "More",
            tint = color,
            modifier = Modifier
                .size(22.dp)
                .scale(scale)
                .rotate(rotation)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            "More",
            color = color,
            fontSize = 10.sp,
            fontWeight = if (showMenu) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ============================================
// 🍔 BOTTOM NAV POPUP MENU (Outside nav bar)
// ============================================

@Composable
private fun BottomNavPopupMenu(
    onYouClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onHelpClick: () -> Unit,
    onExitClick: () -> Unit
) {
    Box(
        modifier = Modifier.width(220.dp)
    ) {
        // Menu glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .blur(30.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppColors.purple.copy(alpha = 0.3f),
                            AppColors.pink.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
        )

        // Menu container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A1A2E).copy(alpha = 0.95f),
                                Color(0xFF0D0D1E).copy(alpha = 0.98f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    // You / Profile
                    GlassmorphicMenuItem(
                        icon = Icons.Outlined.Person,
                        text = "You",
                        iconColor = AppColors.pink,
                        onClick = onYouClick
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Settings
                    GlassmorphicMenuItem(
                        icon = Icons.Outlined.Settings,
                        text = "Settings",
                        iconColor = AppColors.blue,
                        onClick = onSettingsClick
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // About
                    GlassmorphicMenuItem(
                        icon = Icons.Outlined.Info,
                        text = "About",
                        iconColor = AppColors.cyan,
                        onClick = onAboutClick
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Help
                    GlassmorphicMenuItem(
                        icon = Icons.Outlined.Help,
                        text = "Help & Support",
                        iconColor = AppColors.purple,
                        onClick = onHelpClick
                    )

                    // Divider with gradient
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Exit with special styling
                    GlassmorphicMenuItem(
                        icon = Icons.Outlined.ExitToApp,
                        text = "Exit App",
                        iconColor = AppColors.error,
                        onClick = onExitClick,
                        isDestructive = true
                    )
                }
            }
        }
    }
}

// ============================================
// 🍔 FLOATING MENU BUTTON
// ============================================

@Composable
private fun FloatingMenuButton(
    showMenu: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    downloadCount: Int,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onHelpClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animations
    val transition = rememberInfiniteTransition(label = "menu_glow")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val menuRotation by animateFloatAsState(
        targetValue = if (showMenu) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )

    Box(modifier = modifier) {
        // Glow effect behind button
        if (!showMenu && downloadCount > 0) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .blur(20.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AppColors.pink.copy(alpha = glowAlpha * 0.6f),
                                AppColors.purple.copy(alpha = glowAlpha * 0.4f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
        }

        // Main glassmorphic button
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    RoundedCornerShape(18.dp)
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .clip(RoundedCornerShape(18.dp))
                .clickable { onMenuToggle(!showMenu) },
            contentAlignment = Alignment.Center
        ) {
            // Inner shadow effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.05f)
                            ),
                            center = androidx.compose.ui.geometry.Offset.Infinite
                        )
                    )
            )

            // Icon with rotation
            Box(
                modifier = Modifier.rotate(menuRotation),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (showMenu) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Animated badge
            AnimatedVisibility(
                visible = downloadCount > 0 && !showMenu,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AppColors.pink, AppColors.purple)
                            ),
                            CircleShape
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (downloadCount > 99) "99+" else "$downloadCount",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Stunning Glassmorphic Dropdown Menu
        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.8f)
        ) {
            Box(
                modifier = Modifier
                    .offset(y = 64.dp)
                    .width(240.dp)
            ) {
                // Menu glow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .blur(30.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    AppColors.purple.copy(alpha = 0.3f),
                                    AppColors.pink.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            RoundedCornerShape(24.dp)
                        )
                )

                // Menu container
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1A1A2E).copy(alpha = 0.95f),
                                        Color(0xFF0D0D1E).copy(alpha = 0.98f)
                                    )
                                ),
                                RoundedCornerShape(24.dp)
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.3f),
                                        Color.White.copy(alpha = 0.1f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            // Settings
                            GlassmorphicMenuItem(
                                icon = Icons.Outlined.Settings,
                                text = "Settings",
                                iconColor = AppColors.blue,
                                onClick = onSettingsClick
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // About
                            GlassmorphicMenuItem(
                                icon = Icons.Outlined.Info,
                                text = "About",
                                iconColor = AppColors.cyan,
                                onClick = onAboutClick
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Help
                            GlassmorphicMenuItem(
                                icon = Icons.Outlined.Help,
                                text = "Help & Support",
                                iconColor = AppColors.purple,
                                onClick = onHelpClick
                            )

                            // Divider with gradient
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.2f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            // Exit with special styling
                            GlassmorphicMenuItem(
                                icon = Icons.Outlined.ExitToApp,
                                text = "Exit App",
                                iconColor = AppColors.error,
                                onClick = onExitClick,
                                isDestructive = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassmorphicMenuItem(
    icon: ImageVector,
    text: String,
    iconColor: Color,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isPressed) Color.White.copy(alpha = 0.08f) else Color.Transparent
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick()
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon with glow
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        iconColor.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        iconColor.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text
            Text(
                text = text,
                color = if (isDestructive) AppColors.error else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))

            // Arrow indicator
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ============================================
// 🚪 EXIT CONFIRMATION DIALOG
// ============================================

@Composable
private fun ExitConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.clip(RoundedCornerShape(24.dp)),
        containerColor = Color(0xFF1A1A2E),
        icon = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AppColors.error.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = AppColors.error,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Exit App?",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Are you sure you want to exit?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Any active downloads will be paused.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textTertiary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Cancel", color = Color.White, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        Icons.Outlined.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exit", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {}
    )
}

// ============================================
// ⚙️ SETTINGS DIALOG
// ============================================

@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { com.example.dwn.data.SettingsManager.getInstance(context) }
    val settings by settingsManager.settings.collectAsState()

    var showQualityDialog by remember { mutableStateOf(false) }

    // Quality selection dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            containerColor = Color(0xFF1A1A2E),
            title = {
                Text(
                    "Download Quality",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.example.dwn.data.DownloadQuality.entries.forEach { quality ->
                        Surface(
                            onClick = {
                                settingsManager.updateDownloadQuality(quality)
                                showQualityDialog = false
                            },
                            color = if (settings.downloadQuality == quality)
                                AppColors.pink.copy(alpha = 0.2f)
                            else
                                Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(quality.label, color = Color.White, fontWeight = FontWeight.Medium)
                                    Text(quality.description, color = AppColors.textSecondary, fontSize = 12.sp)
                                }
                                if (settings.downloadQuality == quality) {
                                    Icon(Icons.Default.CheckCircle, null, tint = AppColors.pink)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Cancel", color = AppColors.textSecondary)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.clip(RoundedCornerShape(24.dp)),
        containerColor = Color(0xFF1A1A2E),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AppColors.pink.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = AppColors.pink,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsItem(
                    icon = Icons.Default.HighQuality,
                    title = "Download Quality",
                    subtitle = settings.downloadQuality.label,
                    onClick = { showQualityDialog = true }
                )

                SettingsToggleItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    subtitle = if (settings.notificationsEnabled) "Enabled" else "Disabled",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { settingsManager.updateNotificationsEnabled(it) }
                )

                SettingsToggleItem(
                    icon = Icons.Default.Wifi,
                    title = "WiFi Only",
                    subtitle = if (settings.wifiOnlyDownloads) "WiFi only" else "Any network",
                    checked = settings.wifiOnlyDownloads,
                    onCheckedChange = { settingsManager.updateWifiOnlyDownloads(it) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AppColors.pink, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = AppColors.pink, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(subtitle, color = AppColors.textSecondary, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppColors.textTertiary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = AppColors.pink, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(subtitle, color = AppColors.textSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppColors.pink,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = AppColors.textTertiary
                )
            )
        }
    }
}

// ============================================
// ℹ️ ABOUT DIALOG
// ============================================

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.clip(RoundedCornerShape(24.dp)),
        containerColor = Color(0xFF1A1A2E),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AppColors.pink.copy(alpha = 0.5f),
                                    AppColors.purple.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(AppColors.pink, AppColors.purple)
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "MediaGrab",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AppColors.pink.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "v1.0.0",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = AppColors.pink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "A powerful multimedia app with downloader, podcast studio, beat maker, DJ studio, and advanced audio tools.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Made with ❤️ for media lovers",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.pink),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Got it!", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {}
    )
}

// ============================================
// ❓ HELP DIALOG
// ============================================

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.clip(RoundedCornerShape(24.dp)),
        containerColor = Color(0xFF1A1A2E),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AppColors.blue.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Help,
                    contentDescription = null,
                    tint = AppColors.blue,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "Help & Support",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HelpItem(
                    title = "How to use?",
                    description = "Navigate through different sections using the bottom navigation or the menu button."
                )
                HelpItem(
                    title = "Download media",
                    description = "Go to MediaGrab Studio to download audio and video from 1000+ websites."
                )
                HelpItem(
                    title = "Create content",
                    description = "Use Podcast Studio, Beat Maker, or DJ Studio to create your own content."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!", color = AppColors.blue, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun HelpItem(title: String, description: String) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, color = AppColors.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}


