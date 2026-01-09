package com.example.dwn.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.player.AudioPlayerState
import com.example.dwn.player.formatTime
import com.example.dwn.ui.components.AnimatedProgressBar
import com.example.dwn.ui.components.PulsingDot
import com.example.dwn.ui.theme.*

@Composable
fun FloatingAudioPlayer(
    state: AudioPlayerState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onEqualizerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    if (state.currentFileId == null) return

    // Pulsing animation for FAB
    val infiniteTransition = rememberInfiniteTransition(label = "fab")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(modifier = modifier) {
        // Scrim to close when clicking outside (only when expanded)
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { expanded = false }
            )
        }

        // Expanded Player Card
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Surface(
                modifier = Modifier
                    .padding(bottom = 80.dp, end = 8.dp)
                    .width(300.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1A1A2E),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Now Playing",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        IconButton(
                            onClick = { expanded = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Album Art / Visualizer
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Glow effect
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .blur(25.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            PrimaryPink.copy(alpha = 0.4f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )

                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = PrimaryPink.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (state.isPlaying) {
                                    // Animated bars
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.Bottom,
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        repeat(4) { index ->
                                            AnimatedBar(index = index)
                                        }
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = PrimaryPink,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Song title
                    Text(
                        text = state.currentFileName,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress bar
                    AnimatedProgressBar(
                        progress = if (state.duration > 0) {
                            state.currentPosition.toFloat() / state.duration.toFloat()
                        } else 0f,
                        modifier = Modifier.fillMaxWidth(),
                        height = 4.dp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(state.currentPosition),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = formatTime(state.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Control buttons
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Rewind
                        IconButton(
                            onClick = onSeekBackward,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Play/Pause
                        Surface(
                            onClick = onPlayPause,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(PrimaryPink, PrimaryPurple)
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Forward
                        IconButton(
                            onClick = onSeekForward,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Stop
                        IconButton(
                            onClick = {
                                onStop()
                                expanded = false
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = ErrorRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Equalizer button
                    Surface(
                        onClick = {
                            onEqualizerClick()
                            expanded = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryPurple.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Equalizer,
                                contentDescription = "Equalizer",
                                tint = PrimaryPink,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Equalizer",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .scale(if (state.isPlaying && !expanded) pulseScale else 1f)
        ) {
            // Glow behind FAB
            if (state.isPlaying && !expanded) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .blur(16.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    PrimaryPink.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                        .align(Alignment.Center)
                )
            }

            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = Color.Transparent,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(PrimaryPink, PrimaryPurple)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (expanded)
                            Icons.Default.Close
                        else if (state.isPlaying)
                            Icons.Default.MusicNote
                        else
                            Icons.Default.Pause,
                        contentDescription = "Audio Player",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedBar(index: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "bar$index")
    val height by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 300 + (index * 100),
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "height$index"
    )

    Box(
        modifier = Modifier
            .width(6.dp)
            .height(height.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(PrimaryPink, AccentOrange)
                ),
                RoundedCornerShape(3.dp)
            )
    )
}

