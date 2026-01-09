package com.example.dwn.podcast.ui

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.podcast.*
import kotlin.math.*

// Theme colors (same as main screen)
private val PodcastPrimary = Color(0xFF6C63FF)
private val PodcastSecondary = Color(0xFF00D9FF)
private val PodcastAccent = Color(0xFFFF6B6B)
private val PodcastGreen = Color(0xFF00E676)
private val PodcastOrange = Color(0xFFFF9800)
private val PodcastPink = Color(0xFFFF4081)
private val PodcastYellow = Color(0xFFFFEB3B)
private val PodcastSurface = Color(0xFF12121A)
private val PodcastSurfaceLight = Color(0xFF1A1A24)
private val PodcastCard = Color(0xFF16161F)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB0B0C0)
private val TextTertiary = Color(0xFF6E6E80)

// ============================================
// DASHBOARD TAB
// ============================================

@Composable
fun DashboardTab(
    projects: List<PodcastProject>,
    currentProject: PodcastProject?,
    onProjectSelect: (String) -> Unit,
    onCreateProject: (String, String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Stats
        item {
            QuickStatsSection()
        }

        // Quick Actions
        item {
            QuickActionsSection()
        }

        // Projects Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your Podcasts",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                FilledTonalButton(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PodcastPrimary
                    )
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Podcast")
                }
            }
        }

        // Projects Grid
        if (projects.isEmpty()) {
            item {
                EmptyProjectsCard(onCreateProject = { showCreateDialog = true })
            }
        } else {
            items(projects) { project ->
                ProjectCard(
                    project = project,
                    isSelected = currentProject?.id == project.id,
                    onClick = { onProjectSelect(project.id) }
                )
            }
        }

        // Recent Activity
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Recent Activity",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(3) { index ->
            ActivityItem(
                title = when (index) {
                    0 -> "Episode exported successfully"
                    1 -> "Transcription completed"
                    else -> "New listener milestone: 1000!"
                },
                time = "${(index + 1) * 2} hours ago",
                icon = when (index) {
                    0 -> Icons.Default.Download
                    1 -> Icons.Default.TextFields
                    else -> Icons.Default.Celebration
                },
                color = when (index) {
                    0 -> PodcastGreen
                    1 -> PodcastSecondary
                    else -> PodcastPrimary
                }
            )
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc ->
                onCreateProject(name, desc)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun QuickStatsSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Total Episodes",
            value = "24",
            icon = Icons.Default.Podcasts,
            color = PodcastPrimary
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Total Listens",
            value = "12.5K",
            icon = Icons.Default.Headphones,
            color = PodcastSecondary
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Subscribers",
            value = "856",
            icon = Icons.Default.People,
            color = PodcastGreen
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                title,
                color = TextTertiary,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuickActionsSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Quick Actions",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(
                    icon = Icons.Default.FiberManualRecord,
                    label = "Record",
                    color = PodcastAccent
                )
                QuickActionButton(
                    icon = Icons.Default.Videocam,
                    label = "Go Live",
                    color = PodcastPink
                )
                QuickActionButton(
                    icon = Icons.Default.Upload,
                    label = "Import",
                    color = PodcastSecondary
                )
                QuickActionButton(
                    icon = Icons.Default.AutoAwesome,
                    label = "AI Tools",
                    color = PodcastPrimary
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun ProjectCard(
    project: PodcastProject,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) PodcastPrimary.copy(alpha = 0.15f) else PodcastSurface,
        border = if (isSelected) BorderStroke(2.dp, PodcastPrimary) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover art placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PodcastPrimary, PodcastSecondary)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Podcasts,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    "${project.episodes.size} episodes",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    StatusChip(
                        text = "${project.episodes.count { it.status == EpisodeStatus.PUBLISHED }} published",
                        color = PodcastGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusChip(
                        text = "${project.episodes.count { it.status == EpisodeStatus.DRAFT }} drafts",
                        color = PodcastOrange
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextTertiary
            )
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text,
            color = color,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyProjectsCard(onCreateProject: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PodcastSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Podcasts,
                contentDescription = null,
                tint = PodcastPrimary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Podcasts Yet",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Create your first podcast to get started",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateProject,
                colors = ButtonDefaults.buttonColors(containerColor = PodcastPrimary)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Podcast")
            }
        }
    }
}

@Composable
private fun ActivityItem(
    title: String,
    time: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PodcastSurface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 13.sp)
                Text(time, color = TextTertiary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PodcastSurface,
        title = {
            Text("Create New Podcast", color = TextPrimary)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Podcast Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PodcastPrimary,
                        unfocusedBorderColor = TextTertiary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PodcastPrimary,
                        unfocusedBorderColor = TextTertiary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, description) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PodcastPrimary)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

