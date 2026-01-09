package com.example.dwn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var showSubtext by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Rotation for outer ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Scale animation for logo
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    // Pulse animation
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(300)
        showText = true
        delay(200)
        showSubtext = true
        delay(1000) // Total splash duration ~1.5s
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D0D),
                        Color(0xFF1A0A1A),
                        Color(0xFF0D0D0D)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated background orbs
        Box(
            modifier = Modifier
                .offset(x = (-50).dp, y = (-100).dp)
                .size(300.dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryPink.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .offset(x = 100.dp, y = 150.dp)
                .size(250.dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryPurple.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(logoScale)
            ) {
                // Outer rotating ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .rotate(rotation)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    PrimaryPink,
                                    PrimaryPurple,
                                    PrimaryBlue,
                                    PrimaryPink
                                )
                            ),
                            CircleShape
                        )
                )

                // Inner circle background
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(Color(0xFF0D0D0D), CircleShape)
                )

                // Glow effect
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulse)
                        .blur(20.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    PrimaryPink.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                // Icon container
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulse),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Music icon representation
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🎵",
                                fontSize = 48.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // App name with animation
            AnimatedVisibility(
                visible = showText,
                enter = fadeIn(animationSpec = tween(500)) +
                        slideInVertically(initialOffsetY = { 20 })
            ) {
                Text(
                    text = "MediaGrab",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 8.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            AnimatedVisibility(
                visible = showSubtext,
                enter = fadeIn(animationSpec = tween(500)) +
                        slideInVertically(initialOffsetY = { 10 })
            ) {
                Text(
                    text = "Experience the power of audio",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Loading indicator
            AnimatedVisibility(
                visible = showSubtext,
                enter = fadeIn(animationSpec = tween(300))
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { index ->
                        LoadingDot(index = index)
                    }
                }
            }
        }

        // Bottom branding
        AnimatedVisibility(
            visible = showSubtext,
            enter = fadeIn(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "Download • Convert • Enjoy",
                fontSize = 12.sp,
                color = TextTertiary,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun LoadingDot(index: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot$index")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = index * 150,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale$index"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                delayMillis = index * 150,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha$index"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .scale(scale)
            .background(
                PrimaryPink.copy(alpha = alpha),
                CircleShape
            )
    )
}

