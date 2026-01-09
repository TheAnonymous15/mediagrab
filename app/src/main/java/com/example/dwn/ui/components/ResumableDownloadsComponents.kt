package com.example.dwn.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.data.DownloadItem
import com.example.dwn.data.DownloadStatus
import com.example.dwn.ui.theme.*

@Composable
fun ResumableDownloadsCard(
    downloads: List<DownloadItem>,
    onResume: (DownloadItem) -> Unit,
    onCancel: (DownloadItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (downloads.isEmpty()) return

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Resumable Downloads",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    // Count badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WarningOrange.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${downloads.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = WarningOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Download items
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                downloads.forEach { item ->
                    ResumableDownloadItem(
                        item = item,
                        onResume = { onResume(item) },
                        onCancel = { onCancel(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumableDownloadItem(
    item: DownloadItem,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val statusColor = when (item.status) {
        DownloadStatus.PAUSED -> WarningOrange
        DownloadStatus.FAILED -> ErrorRed
        DownloadStatus.DOWNLOADING -> PrimaryBlue
        else -> TextSecondary
    }

    val backgroundColor = when (item.status) {
        DownloadStatus.PAUSED -> WarningOrange.copy(alpha = 0.1f)
        DownloadStatus.FAILED -> ErrorRed.copy(alpha = 0.1f)
        else -> Color.White.copy(alpha = 0.05f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.status) {
                            DownloadStatus.PAUSED -> Icons.Default.Pause
                            DownloadStatus.FAILED -> Icons.Default.ErrorOutline
                            else -> Icons.Default.Download
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.ifEmpty { item.fileName.ifEmpty { "Unknown" } },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Media type
                        Text(
                            text = if (item.mediaType == "MP3") "🎵" else "🎬",
                            fontSize = 12.sp
                        )

                        // Status
                        Text(
                            text = when (item.status) {
                                DownloadStatus.PAUSED -> "Paused"
                                DownloadStatus.FAILED -> "Failed"
                                else -> item.status.name
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )

                        // Progress if available
                        if (item.progress > 0f) {
                            Text(
                                text = "•",
                                color = TextTertiary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${(item.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Resume button
                    FilledIconButton(
                        onClick = onResume,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = SuccessGreen.copy(alpha = 0.2f),
                            contentColor = SuccessGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Cancel button
                    FilledIconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = ErrorRed.copy(alpha = 0.2f),
                            contentColor = ErrorRed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Progress bar
            if (item.progress > 0f) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = statusColor,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }
}

