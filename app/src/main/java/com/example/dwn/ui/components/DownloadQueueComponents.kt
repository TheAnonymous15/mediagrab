package com.example.dwn.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.download.QueueStatus
import com.example.dwn.download.QueuedDownload
import com.example.dwn.ui.theme.*

@Composable
fun DownloadQueueCard(
    queue: List<QueuedDownload>,
    onRemove: (String) -> Unit,
    onRetry: (String) -> Unit,
    onClearCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (queue.isEmpty()) return

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
                        imageVector = Icons.Default.Queue,
                        contentDescription = null,
                        tint = PrimaryPink,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Download Queue",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    // Active count badge
                    val activeCount = queue.count { it.status == QueueStatus.DOWNLOADING }
                    if (activeCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryPink.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "$activeCount active",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = PrimaryPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Clear completed button
                val hasCompleted = queue.any { it.status == QueueStatus.COMPLETED }
                if (hasCompleted) {
                    TextButton(
                        onClick = onClearCompleted,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Clear done",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Queue items
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                queue.forEach { item ->
                    QueueItem(
                        item = item,
                        onRemove = { onRemove(item.id) },
                        onRetry = { onRetry(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueItem(
    item: QueuedDownload,
    onRemove: () -> Unit,
    onRetry: () -> Unit
) {
    val backgroundColor = when (item.status) {
        QueueStatus.DOWNLOADING -> PrimaryPink.copy(alpha = 0.1f)
        QueueStatus.PROCESSING -> PrimaryPurple.copy(alpha = 0.1f)
        QueueStatus.SAVING -> PrimaryBlue.copy(alpha = 0.1f)
        QueueStatus.COMPLETED -> SuccessGreen.copy(alpha = 0.1f)
        QueueStatus.FAILED -> ErrorRed.copy(alpha = 0.1f)
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (item.status) {
                                QueueStatus.DOWNLOADING -> PrimaryPink.copy(alpha = 0.2f)
                                QueueStatus.PROCESSING -> PrimaryPurple.copy(alpha = 0.2f)
                                QueueStatus.SAVING -> PrimaryBlue.copy(alpha = 0.2f)
                                QueueStatus.COMPLETED -> SuccessGreen.copy(alpha = 0.2f)
                                QueueStatus.FAILED -> ErrorRed.copy(alpha = 0.2f)
                                QueueStatus.QUEUED -> Color.White.copy(alpha = 0.1f)
                                QueueStatus.CHECKING -> PrimaryBlue.copy(alpha = 0.2f)
                                QueueStatus.CANCELLED -> TextTertiary.copy(alpha = 0.2f)
                                QueueStatus.PAUSED -> AccentOrange.copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (item.status) {
                        QueueStatus.DOWNLOADING -> {
                            CircularProgressIndicator(
                                progress = { item.progress },
                                modifier = Modifier.size(20.dp),
                                color = PrimaryPink,
                                strokeWidth = 2.dp,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }
                        QueueStatus.PROCESSING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PrimaryPurple,
                                strokeWidth = 2.dp
                            )
                        }
                        QueueStatus.SAVING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PrimaryBlue,
                                strokeWidth = 2.dp
                            )
                        }
                        QueueStatus.COMPLETED -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        QueueStatus.FAILED -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        QueueStatus.QUEUED -> {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        QueueStatus.CHECKING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PrimaryBlue,
                                strokeWidth = 2.dp
                            )
                        }
                        QueueStatus.CANCELLED -> {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        QueueStatus.PAUSED -> {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                tint = AccentOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    // Title or URL
                    Text(
                        text = item.title.ifEmpty {
                            item.url.take(40) + if (item.url.length > 40) "..." else ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Status message
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Media type badge
                        Text(
                            text = item.mediaType.icon,
                            fontSize = 12.sp
                        )
                        Text(
                            text = item.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (item.status) {
                                QueueStatus.DOWNLOADING -> PrimaryPink
                                QueueStatus.PROCESSING -> PrimaryPurple
                                QueueStatus.SAVING -> PrimaryBlue
                                QueueStatus.COMPLETED -> SuccessGreen
                                QueueStatus.FAILED -> ErrorRed
                                else -> TextSecondary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Actions
                when (item.status) {
                    QueueStatus.FAILED -> {
                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = PrimaryPink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    QueueStatus.COMPLETED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    else -> {}
                }

                // Remove button (not for completed)
                if (item.status != QueueStatus.COMPLETED) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Remove",
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Progress bar for downloading items
            if (item.status == QueueStatus.DOWNLOADING && item.progress > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedProgressBar(
                    progress = item.progress,
                    modifier = Modifier.fillMaxWidth(),
                    height = 3.dp,
                    showGlow = true
                )
            }
        }
    }
}

@Composable
fun AddToQueueButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = PrimaryPink
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = enabled).copy(
            brush = Brush.horizontalGradient(
                listOf(PrimaryPink.copy(alpha = 0.5f), PrimaryPurple.copy(alpha = 0.5f))
            )
        )
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Add to Queue",
            fontWeight = FontWeight.Medium
        )
    }
}

