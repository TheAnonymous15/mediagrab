package com.example.dwn.ui.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.ui.theme.*

// Gradient Background
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF1A0A1A),
                        DarkBackground
                    )
                )
            ),
        content = content
    )
}

// Animated Gradient Orbs for background decoration
@Composable
fun AnimatedGradientOrbs(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale2"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Pink orb - top right
        Box(
            modifier = Modifier
                .offset(x = 200.dp, y = (-50).dp)
                .size(300.dp)
                .scale(scale1)
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

        // Purple orb - bottom left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-100).dp, y = 100.dp)
                .size(350.dp)
                .scale(scale2)
                .blur(120.dp)
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

        // Blue orb - center right
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 50.dp)
                .size(200.dp)
                .scale(scale1)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryBlue.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
    }
}

// Glassmorphism Card
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        border = null
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

// Gradient Button
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(
                            colors = listOf(PrimaryPink, PrimaryPurple)
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Gray.copy(alpha = 0.3f),
                                Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    },
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// Styled Text Field
@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = {
            Text(
                placeholder,
                color = TextTertiary
            )
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryPink,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
            cursorColor = PrimaryPink,
            focusedLabelColor = PrimaryPink,
            unfocusedLabelColor = TextSecondary
        )
    )
}

// Chip Selector
@Composable
fun StyledChip(
    text: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) {
        Brush.horizontalGradient(listOf(PrimaryPink, PrimaryPurple))
    } else {
        Brush.horizontalGradient(listOf(
            Color.White.copy(alpha = 0.1f),
            Color.White.copy(alpha = 0.1f)
        ))
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// Status Badge
@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Animated Progress Bar
@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    trackColor: Color = Color.White.copy(alpha = 0.1f),
    showGlow: Boolean = true
) {
    Box(modifier = modifier) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(trackColor)
        )

        // Progress with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(PrimaryPink, AccentOrange)
                    )
                )
        )

        // Glow effect
        if (showGlow && progress > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(height * 2)
                    .offset(y = (-height / 2))
                    .blur(8.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                PrimaryPink.copy(alpha = 0.5f),
                                AccentOrange.copy(alpha = 0.5f)
                            )
                        ),
                        RoundedCornerShape(height)
                    )
            )
        }
    }
}

// Pulsing Animation for loading states
@Composable
fun PulsingDot(
    color: Color = PrimaryPink,
    size: Dp = 12.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .background(
                color.copy(alpha = alpha),
                CircleShape
            )
    )
}

// Icon Button with Ripple
@Composable
fun StyledIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = Color.White,
    backgroundColor: Color = Color.White.copy(alpha = 0.1f),
    size: Dp = 48.dp
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = backgroundColor
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

