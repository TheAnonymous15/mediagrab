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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ============================================
// 🤖 AI TOOLS SCREEN
// ============================================

private object AIToolsColors {
    val pink = Color(0xFFE91E63)
    val purple = Color(0xFF9C27B0)
    val blue = Color(0xFF2196F3)
    val cyan = Color(0xFF00BCD4)
    val teal = Color(0xFF009688)
    val green = Color(0xFF4CAF50)
    val orange = Color(0xFFFF5722)
    val amber = Color(0xFFFFC107)

    val bgDark = Color(0xFF0D0D0D)
    val bgMid = Color(0xFF1A0A1A)
    val surface = Color(0xFF1A1A1A)
    val surfaceVariant = Color(0xFF252525)
    val card = Color(0xFF1E1E1E)

    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0B0B0)
    val textTertiary = Color(0xFF707070)

    val glassWhite = Color(0x14FFFFFF)
    val glassBorder = Color(0x20FFFFFF)

    // AI specific
    val aiGradient1 = Color(0xFF667EEA)
    val aiGradient2 = Color(0xFF764BA2)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIToolsScreen(
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("all") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AIToolsColors.bgDark)
    ) {
        // Animated background
        AIBackgroundAnimation()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AI Tools", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Brush.horizontalGradient(
                                listOf(AIToolsColors.aiGradient1, AIToolsColors.aiGradient2)
                            ).let { AIToolsColors.purple.copy(alpha = 0.3f) }
                        ) {
                            Text(
                                "BETA",
                                color = AIToolsColors.purple,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = AIToolsColors.textPrimary,
                    navigationIconContentColor = AIToolsColors.textPrimary
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // Hero Section
                AIHeroSection()

                Spacer(modifier = Modifier.height(24.dp))

                // Category Tabs
                CategoryTabs(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // AI Tools Grid
                AIToolsGrid(category = selectedCategory)

                Spacer(modifier = Modifier.height(24.dp))

                // Quick AI Actions
                QuickAIActions()

                Spacer(modifier = Modifier.height(24.dp))

                // AI Processing History
                AIHistorySection()

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun AIBackgroundAnimation() {
    val transition = rememberInfiniteTransition(label = "ai_bg")

    val pulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Base gradient
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    AIToolsColors.bgDark,
                    AIToolsColors.bgMid,
                    AIToolsColors.bgDark
                )
            )
        )

        // AI-themed orbs
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    AIToolsColors.aiGradient1.copy(alpha = 0.15f * pulse),
                    Color.Transparent
                )
            ),
            radius = 400f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.15f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    AIToolsColors.aiGradient2.copy(alpha = 0.12f * pulse),
                    Color.Transparent
                )
            ),
            radius = 350f,
            center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.7f)
        )
    }
}

@Composable
private fun AIHeroSection() {
    val transition = rememberInfiniteTransition(label = "hero")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(AIToolsColors.aiGradient1, AIToolsColors.aiGradient2)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI-Powered",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "Transform Your\nMedia with AI",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 32.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Enhance, transcribe, and create with cutting-edge AI",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }

                    // AI Icon
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        "all" to "All",
        "audio" to "Audio",
        "video" to "Video",
        "image" to "Image",
        "text" to "Text"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { (id, label) ->
            val isSelected = selectedCategory == id

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(id) },
                label = {
                    Text(
                        label,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AIToolsColors.purple,
                    selectedLabelColor = Color.White,
                    containerColor = AIToolsColors.surfaceVariant,
                    labelColor = AIToolsColors.textSecondary
                )
            )
        }
    }
}

@Composable
private fun AIToolsGrid(category: String) {
    val allTools = listOf(
        AITool("transcribe", "Transcribe", "Speech to text", Icons.Default.Subtitles, AIToolsColors.pink, "audio"),
        AITool("enhance_audio", "Enhance Audio", "Noise removal & clarity", Icons.Default.GraphicEq, AIToolsColors.purple, "audio"),
        AITool("voice_clone", "Voice Clone", "Clone any voice", Icons.Default.RecordVoiceOver, AIToolsColors.blue, "audio"),
        AITool("separate_vocals", "Vocal Separator", "Extract vocals & music", Icons.Default.MusicNote, AIToolsColors.cyan, "audio"),
        AITool("upscale", "Upscale Video", "4K enhancement", Icons.Default.Hd, AIToolsColors.green, "video"),
        AITool("remove_bg", "Remove BG", "Background removal", Icons.Default.ContentCut, AIToolsColors.orange, "video"),
        AITool("auto_caption", "Auto Caption", "Generate subtitles", Icons.Default.ClosedCaption, AIToolsColors.amber, "video"),
        AITool("scene_detect", "Scene Detection", "Smart chapter marks", Icons.Default.ViewTimeline, AIToolsColors.teal, "video"),
        AITool("style_transfer", "Style Transfer", "Artistic filters", Icons.Default.Palette, AIToolsColors.pink, "image"),
        AITool("face_enhance", "Face Enhance", "Portrait improvement", Icons.Default.Face, AIToolsColors.purple, "image"),
        AITool("summarize", "Summarize", "AI text summary", Icons.Default.Summarize, AIToolsColors.blue, "text"),
        AITool("translate", "Translate", "Multi-language", Icons.Default.Translate, AIToolsColors.green, "text")
    )

    val filteredTools = if (category == "all") allTools else allTools.filter { it.category == category }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        filteredTools.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { tool ->
                    AIToolCard(
                        tool = tool,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class AITool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val category: String
)

@Composable
private fun AIToolCard(
    tool: AITool,
    modifier: Modifier = Modifier
) {
    var isProcessing by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = AIToolsColors.glassWhite,
        border = BorderStroke(1.dp, AIToolsColors.glassBorder),
        onClick = { isProcessing = !isProcessing }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            tool.color.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = tool.color,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            tool.icon,
                            null,
                            tint = tool.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // AI badge
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(AIToolsColors.aiGradient1, AIToolsColors.aiGradient2)
                            ),
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
                color = AIToolsColors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                tool.description,
                color = AIToolsColors.textTertiary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun QuickAIActions() {
    Column {
        Text(
            "Quick Actions",
            color = AIToolsColors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = AIToolsColors.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                QuickAction(
                    icon = Icons.Default.Mic,
                    title = "Record & Transcribe",
                    subtitle = "Start recording and get instant transcription",
                    color = AIToolsColors.pink
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = AIToolsColors.glassBorder
                )

                QuickAction(
                    icon = Icons.Default.Upload,
                    title = "Upload & Enhance",
                    subtitle = "Improve audio/video quality automatically",
                    color = AIToolsColors.purple
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = AIToolsColors.glassBorder
                )

                QuickAction(
                    icon = Icons.Default.AutoAwesome,
                    title = "Smart Edit",
                    subtitle = "Let AI edit your content intelligently",
                    color = AIToolsColors.blue
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
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
            Text(title, color = AIToolsColors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = AIToolsColors.textTertiary, fontSize = 12.sp)
        }

        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = AIToolsColors.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AIHistorySection() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recent AI Tasks",
                color = AIToolsColors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = { }) {
                Text("View All", color = AIToolsColors.purple, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Empty state or history items
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AIToolsColors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.History,
                    null,
                    tint = AIToolsColors.textTertiary,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "No AI tasks yet",
                    color = AIToolsColors.textSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    "Your AI processing history will appear here",
                    color = AIToolsColors.textTertiary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

