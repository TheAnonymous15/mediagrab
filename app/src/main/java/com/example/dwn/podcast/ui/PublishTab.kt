package com.example.dwn.podcast.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.podcast.*

// Theme colors
private val PodcastPrimary = Color(0xFF6C63FF)
private val PodcastSecondary = Color(0xFF00D9FF)
private val PodcastAccent = Color(0xFFFF6B6B)
private val PodcastGreen = Color(0xFF00E676)
private val PodcastOrange = Color(0xFFFF9800)
private val PodcastPink = Color(0xFFFF4081)
private val PodcastYellow = Color(0xFFFFEB3B)
private val PodcastSurface = Color(0xFF12121A)
private val PodcastSurfaceLight = Color(0xFF1A1A24)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB0B0C0)
private val TextTertiary = Color(0xFF6E6E80)

// ============================================
// PUBLISH TAB
// ============================================

@Composable
fun PublishTab(
    currentProject: PodcastProject?,
    onExport: (ExportFormat) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.MP3_320) }
    var showPublishDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Export Section
        item {
            ExportSection(
                selectedFormat = selectedFormat,
                onFormatSelect = { selectedFormat = it },
                onExport = { onExport(selectedFormat) }
            )
        }

        // Distribution Platforms
        item {
            Text(
                "Distribution",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            DistributionPlatformsSection()
        }

        // RSS Feed
        item {
            RSSFeedSection(feedUrl = currentProject?.rssFeedUrl)
        }

        // Monetization
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Monetization",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            MonetizationSection()
        }

        // Analytics Preview
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Analytics Overview",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            AnalyticsPreviewSection(analytics = currentProject?.analytics)
        }

        // Scheduling
        item {
            SchedulingSection(onSchedule = { showPublishDialog = true })
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    if (showPublishDialog) {
        PublishDialog(
            onDismiss = { showPublishDialog = false },
            onPublish = { showPublishDialog = false }
        )
    }
}

@Composable
private fun ExportSection(
    selectedFormat: ExportFormat,
    onFormatSelect: (ExportFormat) -> Unit,
    onExport: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Download,
                    null,
                    tint = PodcastGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Export Episode",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Format selection
            Text("Output Format", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExportFormat.entries) { format ->
                    FormatChip(
                        format = format,
                        isSelected = selectedFormat == format,
                        onClick = { onFormatSelect(format) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quality settings
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = PodcastSurfaceLight
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    QualitySettingRow(
                        label = "Sample Rate",
                        value = "48 kHz"
                    )
                    QualitySettingRow(
                        label = "Bit Depth",
                        value = if (selectedFormat == ExportFormat.WAV || selectedFormat == ExportFormat.FLAC) "32-bit" else "16-bit"
                    )
                    QualitySettingRow(
                        label = "Loudness Target",
                        value = "-16 LUFS"
                    )
                    QualitySettingRow(
                        label = "Estimated Size",
                        value = when (selectedFormat) {
                            ExportFormat.MP3_128 -> "~28 MB"
                            ExportFormat.MP3_320 -> "~70 MB"
                            ExportFormat.AAC_256 -> "~56 MB"
                            ExportFormat.WAV -> "~500 MB"
                            ExportFormat.FLAC -> "~250 MB"
                            ExportFormat.OGG -> "~45 MB"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Export options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportOption(
                    icon = Icons.Default.Subtitles,
                    label = "Include Transcript",
                    isChecked = true
                )
                ExportOption(
                    icon = Icons.Default.Bookmark,
                    label = "Include Chapters",
                    isChecked = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PodcastGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Episode", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FormatChip(
    format: ExportFormat,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) PodcastGreen.copy(alpha = 0.2f) else PodcastSurfaceLight,
        border = if (isSelected) BorderStroke(2.dp, PodcastGreen) else null,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                format.extension.uppercase(),
                color = if (isSelected) PodcastGreen else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                format.label.substringAfter(" "),
                color = TextTertiary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun QualitySettingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ExportOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isChecked: Boolean
) {
    var checked by remember { mutableStateOf(isChecked) }

    Surface(
        modifier = Modifier
            .clickable { checked = !checked },
        shape = RoundedCornerShape(8.dp),
        color = if (checked) PodcastPrimary.copy(alpha = 0.1f) else PodcastSurfaceLight
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { checked = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = PodcastPrimary,
                    uncheckedColor = TextTertiary
                ),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = TextPrimary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DistributionPlatformsSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Connected Platforms",
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            PlatformItem(
                name = "Apple Podcasts",
                status = "Connected",
                isConnected = true,
                color = Color(0xFF8E44AD)
            )
            PlatformItem(
                name = "Spotify",
                status = "Connected",
                isConnected = true,
                color = Color(0xFF1DB954)
            )
            PlatformItem(
                name = "Google Podcasts",
                status = "Connect",
                isConnected = false,
                color = Color(0xFF4285F4)
            )
            PlatformItem(
                name = "Amazon Music",
                status = "Connect",
                isConnected = false,
                color = Color(0xFFFF9900)
            )
            PlatformItem(
                name = "YouTube",
                status = "Connected",
                isConnected = true,
                color = Color(0xFFFF0000)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PodcastPrimary)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Platform")
            }
        }
    }
}

@Composable
private fun PlatformItem(
    name: String,
    status: String,
    isConnected: Boolean,
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
                .size(32.dp)
                .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Podcasts,
                null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(name, color = TextPrimary, modifier = Modifier.weight(1f))

        if (isConnected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(PodcastGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(status, color = PodcastGreen, fontSize = 12.sp)
            }
        } else {
            TextButton(onClick = { }) {
                Text(status, color = PodcastPrimary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RSSFeedSection(feedUrl: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.RssFeed,
                        null,
                        tint = PodcastOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "RSS Feed",
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                TextButton(onClick = { }) {
                    Text("Validate", color = PodcastOrange, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = PodcastSurfaceLight
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        feedUrl ?: "https://yourpodcast.com/feed.xml",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.ContentCopy,
                            null,
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                FeedFeatureChip("Podcast 2.0", true)
                Spacer(modifier = Modifier.width(8.dp))
                FeedFeatureChip("Chapters", true)
                Spacer(modifier = Modifier.width(8.dp))
                FeedFeatureChip("Transcripts", true)
            }
        }
    }
}

@Composable
private fun FeedFeatureChip(label: String, isEnabled: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isEnabled) PodcastGreen.copy(alpha = 0.15f) else PodcastSurfaceLight
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isEnabled) Icons.Default.Check else Icons.Default.Close,
                null,
                tint = if (isEnabled) PodcastGreen else TextTertiary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                label,
                color = if (isEnabled) PodcastGreen else TextTertiary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun MonetizationSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MonetizationOption(
                icon = Icons.Default.Campaign,
                title = "Dynamic Ad Insertion",
                description = "Automatically insert targeted ads",
                isEnabled = true
            )

            Divider(color = PodcastSurfaceLight, modifier = Modifier.padding(vertical = 8.dp))

            MonetizationOption(
                icon = Icons.Default.Star,
                title = "Premium Episodes",
                description = "Offer exclusive content to subscribers",
                isEnabled = false
            )

            Divider(color = PodcastSurfaceLight, modifier = Modifier.padding(vertical = 8.dp))

            MonetizationOption(
                icon = Icons.Default.Favorite,
                title = "Listener Tips",
                description = "Accept donations from your audience",
                isEnabled = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Earnings summary
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = PodcastPrimary.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("This Month", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            "$247.50",
                            color = PodcastGreen,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = { }) {
                        Text("View Details", color = PodcastPrimary)
                        Icon(Icons.Default.ChevronRight, null, tint = PodcastPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonetizationOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean
) {
    var enabled by remember { mutableStateOf(isEnabled) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = if (enabled) PodcastPrimary else TextTertiary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp)
            Text(description, color = TextTertiary, fontSize = 11.sp)
        }
        Switch(
            checked = enabled,
            onCheckedChange = { enabled = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = PodcastPrimary,
                checkedTrackColor = PodcastPrimary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun AnalyticsPreviewSection(analytics: ProjectAnalytics?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AnalyticStat(
                    value = formatNumber(analytics?.totalDownloads ?: 12500),
                    label = "Downloads",
                    trend = "+12%",
                    isPositive = true
                )
                AnalyticStat(
                    value = formatNumber(analytics?.subscriberCount ?: 856),
                    label = "Subscribers",
                    trend = "+5%",
                    isPositive = true
                )
                AnalyticStat(
                    value = "4.8",
                    label = "Rating",
                    trend = "",
                    isPositive = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mini chart placeholder
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(8.dp),
                color = PodcastSurfaceLight
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Simple bar chart
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        listOf(0.4f, 0.6f, 0.5f, 0.8f, 0.7f, 0.9f, 1f).forEach { height ->
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .fillMaxHeight(height * 0.8f)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(PodcastPrimary, PodcastSecondary)
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("View Full Analytics", color = PodcastPrimary)
                Icon(Icons.Default.ChevronRight, null, tint = PodcastPrimary)
            }
        }
    }
}

@Composable
private fun AnalyticStat(
    value: String,
    label: String,
    trend: String,
    isPositive: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(label, color = TextTertiary, fontSize = 11.sp)
        if (trend.isNotEmpty()) {
            Text(
                trend,
                color = if (isPositive) PodcastGreen else PodcastAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SchedulingSection(onSchedule: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PodcastSurface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    null,
                    tint = PodcastSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Schedule Publishing",
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Set a date and time to automatically publish your episode across all connected platforms.",
                color = TextTertiary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSchedule,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PodcastSecondary)
                ) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Schedule")
                }

                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PodcastPrimary)
                ) {
                    Icon(Icons.Default.Publish, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Publish Now")
                }
            }
        }
    }
}

@Composable
private fun PublishDialog(
    onDismiss: () -> Unit,
    onPublish: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PodcastSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Publish, null, tint = PodcastPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publish Episode", color = TextPrimary)
            }
        },
        text = {
            Column {
                Text(
                    "Your episode will be published to all connected platforms. This action cannot be undone.",
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Connected platforms summary
                Row {
                    listOf("Apple Podcasts", "Spotify", "YouTube").forEach { platform ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PodcastGreen.copy(alpha = 0.1f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                platform,
                                color = PodcastGreen,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPublish,
                colors = ButtonDefaults.buttonColors(containerColor = PodcastPrimary)
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

private fun formatNumber(num: Long): String {
    return when {
        num >= 1000000 -> "%.1fM".format(num / 1000000f)
        num >= 1000 -> "%.1fK".format(num / 1000f)
        else -> num.toString()
    }
}

