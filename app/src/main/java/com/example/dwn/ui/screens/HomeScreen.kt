package com.example.dwn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.MediaType
import com.example.dwn.data.DownloadItem
import com.example.dwn.data.SettingsManager
import com.example.dwn.data.DownloadQuality
import com.example.dwn.download.QueuedDownload
import com.example.dwn.podcast.ui.PodcastScreen
import com.example.dwn.ui.components.*
import com.example.dwn.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    url: String,
    onUrlChange: (String) -> Unit,
    status: String,
    progress: Float,
    isDownloading: Boolean,
    isReady: Boolean,
    selectedMediaType: MediaType,
    onMediaTypeChange: (MediaType) -> Unit,
    downloadCount: Int,
    onViewDownloadsClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCheckPlaylist: () -> Unit,
    onAddToQueue: () -> Unit = {},
    onPauseClick: () -> Unit,
    onCancelClick: () -> Unit,
    currentDownloadId: String?,
    // Queue related
    downloadQueue: List<QueuedDownload> = emptyList(),
    onRemoveFromQueue: (String) -> Unit = {},
    onRetryQueued: (String) -> Unit = {},
    onClearCompletedQueue: () -> Unit = {},
    // Resumable downloads
    resumableDownloads: List<DownloadItem> = emptyList(),
    onResumeDownload: (DownloadItem) -> Unit = {},
    onCancelDownload: (DownloadItem) -> Unit = {},
    onResumeAllDownloads: () -> Unit = {},
    onCancelAllDownloads: () -> Unit = {},
    // Network state
    isConnected: Boolean = true
) {

    // Menu state
    var showMenu by remember { mutableStateOf(false) }

    // Exit confirmation dialog state
    var showExitDialog by remember { mutableStateOf(false) }

    // Additional dialog states
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showPodcastScreen by remember { mutableStateOf(false) }
    var showBeatMakerScreen by remember { mutableStateOf(false) }

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

    // Podcast Screen (Full screen overlay)
    if (showPodcastScreen) {
        PodcastScreen(
            onBack = { showPodcastScreen = false }
        )
        return
    }

    // Beat Maker Screen (Full screen overlay)
    if (showBeatMakerScreen) {
        BeatMakerScreen(
            onNavigateBack = { showBeatMakerScreen = false }
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Animated background
        GradientBackground {
            AnimatedGradientOrbs()
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Network Status Banner (when offline)
            AnimatedVisibility(
                visible = !isConnected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                NetworkStatusBanner(
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Resumable Downloads Banner (when there are paused/failed downloads)
            AnimatedVisibility(
                visible = resumableDownloads.isNotEmpty() && isConnected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ResumableDownloadsBanner(
                    count = resumableDownloads.size,
                    onResumeAll = onResumeAllDownloads,
                    onCancelAll = onCancelAllDownloads,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Top Bar with title only (menu moved to floating)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MultiMedia",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Downloader",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(48.dp)) // Space for floating button
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Hero Section
            HeroSection(isReady = isReady)

            Spacer(modifier = Modifier.height(32.dp))


            // Format Selection - Enhanced Glassmorphic
            EnhancedGlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Select Format",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MediaType.entries.forEach { mediaType ->
                        StyledChip(
                            text = mediaType.label,
                            icon = mediaType.icon,
                            selected = selectedMediaType == mediaType,
                            onClick = {
                                if (!isDownloading) onMediaTypeChange(mediaType)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // URL Input Card - Enhanced Glassmorphic
            EnhancedGlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                StyledTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = "Enter your link",
                    placeholder = "Paste video or playlist link...",
                    enabled = !isDownloading && isReady,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = if (url.isNotEmpty()) PrimaryPink else TextTertiary
                        )
                    },
                    trailingIcon = {
                        if (url.isNotEmpty() && !isDownloading) {
                            IconButton(onClick = { onUrlChange("") }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))


            // Action Buttons
            if (isDownloading) {
                DownloadingControls(
                    progress = progress,
                    onPauseClick = onPauseClick,
                    onCancelClick = onCancelClick
                )
            } else {
                // Download and Add to Queue buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GradientButton(
                        text = "Download ${selectedMediaType.name}",
                        onClick = onCheckPlaylist,
                        enabled = url.isNotBlank() && isReady,
                        icon = Icons.Default.Download,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Add to Queue button
                    AddToQueueButton(
                        onClick = onAddToQueue,
                        enabled = url.isNotBlank() && isReady,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Download Queue
            AnimatedVisibility(
                visible = downloadQueue.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                DownloadQueueCard(
                    queue = downloadQueue,
                    onRemove = onRemoveFromQueue,
                    onRetry = onRetryQueued,
                    onClearCompleted = onClearCompletedQueue,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Resumable Downloads
            AnimatedVisibility(
                visible = resumableDownloads.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ResumableDownloadsCard(
                    downloads = resumableDownloads,
                    onResume = onResumeDownload,
                    onCancel = onCancelDownload,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Status Card
            StatusCard(status = status, isDownloading = isDownloading, progress = progress)

            Spacer(modifier = Modifier.height(24.dp))

            // Save Location Info
            SaveLocationInfo(selectedMediaType = selectedMediaType)

            Spacer(modifier = Modifier.height(16.dp))

            // View History Link
            TextButton(
                onClick = onViewDownloadsClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = PrimaryPink
                )
            ) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "View Download History",
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Floating Menu Button
        FloatingMenuButton(
            showMenu = showMenu,
            onMenuToggle = { showMenu = it },
            downloadCount = downloadCount,
            onViewDownloadsClick = {
                showMenu = false
                onViewDownloadsClick()
            },
            onPodcastClick = {
                showMenu = false
                showPodcastScreen = true
            },
            onBeatMakerClick = {
                showMenu = false
                showBeatMakerScreen = true
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
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 24.dp, end = 24.dp)
        )
    }
}

@Composable
private fun FloatingMenuButton(
    showMenu: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    downloadCount: Int,
    onViewDownloadsClick: () -> Unit,
    onPodcastClick: () -> Unit,
    onBeatMakerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onHelpClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Floating button with glassmorphic effect
        Surface(
            onClick = { onMenuToggle(!showMenu) },
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.1f),
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    )
                )
            ),
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (showMenu) Icons.Default.Close else Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                // Badge
                if (downloadCount > 0 && !showMenu) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(20.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PrimaryPink, PrimaryPurple)
                                ),
                                CircleShape
                            ),
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
        }

        // Dropdown Menu - opens upward since button is at bottom
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { onMenuToggle(false) },
            modifier = Modifier
                .background(
                    color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp)
                )
                .width(220.dp),
            offset = DpOffset(0.dp, 8.dp)
        ) {
            // Downloads
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("MediaGrab Studio", color = Color.White)
                        if (downloadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryPink.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "$downloadCount",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = PrimaryPink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                onClick = onViewDownloadsClick,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = null,
                        tint = PrimaryPink
                    )
                }
            )

            // Podcast Studio
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Podcast Studio", color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF6C63FF).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "PRO",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color(0xFF6C63FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                onClick = onPodcastClick,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = Color(0xFF6C63FF)
                    )
                }
            )

            // Beat Maker
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Beat Maker", color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF00F0FF).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "NEW",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color(0xFF00F0FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                onClick = onBeatMakerClick,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Piano,
                        contentDescription = null,
                        tint = Color(0xFF00F0FF)
                    )
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Settings
            DropdownMenuItem(
                text = { Text("Settings", color = Color.White) },
                onClick = onSettingsClick,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            )

            // About
            DropdownMenuItem(
                text = { Text("About", color = Color.White) },
                onClick = onAboutClick,
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            )

            // Help
            DropdownMenuItem(
                text = { Text("Help & Support", color = Color.White) },
                onClick = onHelpClick,
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Exit
            DropdownMenuItem(
                text = { Text("Exit App", color = ErrorRed) },
                onClick = onExitClick,
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Outlined.ExitToApp,
                        contentDescription = null,
                        tint = ErrorRed
                    )
                }
            )
        }
    }
}

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
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ErrorRed.copy(alpha = 0.3f),
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
                        tint = ErrorRed,
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
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Any active downloads will be paused.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
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
                    Text(
                        "Cancel",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Exit",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun EnhancedGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.2f),
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.1f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun HeroSection(isReady: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon Container
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Outer glow ring
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .blur(30.dp)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                PrimaryPink.copy(alpha = 0.4f),
                                PrimaryPurple.copy(alpha = 0.4f),
                                PrimaryBlue.copy(alpha = 0.4f),
                                PrimaryPink.copy(alpha = 0.4f)
                            )
                        ),
                        CircleShape
                    )
            )

            // Inner icon container
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isReady) {
                        Text(
                            text = "🎵",
                            fontSize = 48.sp
                        )
                    } else {
                        CircularProgressIndicator(
                            color = PrimaryPink,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Download Media from Link",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "High Quality Media - Multi Source",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}


@Composable
private fun DownloadingControls(
    progress: Float,
    onPauseClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Progress Bar
        AnimatedProgressBar(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            height = 8.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = PrimaryPink,
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(
                        listOf(PrimaryPink.copy(alpha = 0.5f), PrimaryPurple.copy(alpha = 0.5f))
                    )
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Pause, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pause")
            }

            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(
                        listOf(ErrorRed.copy(alpha = 0.5f), ErrorRed.copy(alpha = 0.5f))
                    )
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ErrorRed
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun StatusCard(
    status: String,
    isDownloading: Boolean,
    progress: Float
) {
    val (backgroundColor, iconColor, icon) = when {
        status.startsWith("✅") -> Triple(
            SuccessGreen.copy(alpha = 0.15f),
            SuccessGreen,
            Icons.Default.CheckCircle
        )
        status.startsWith("❌") -> Triple(
            ErrorRed.copy(alpha = 0.15f),
            ErrorRed,
            Icons.Default.Error
        )
        status.startsWith("⚠️") -> Triple(
            WarningOrange.copy(alpha = 0.15f),
            WarningOrange,
            Icons.Default.Warning
        )
        isDownloading -> Triple(
            PrimaryPink.copy(alpha = 0.15f),
            PrimaryPink,
            Icons.Default.Download
        )
        else -> Triple(
            Color.White.copy(alpha = 0.05f),
            TextSecondary,
            Icons.Default.Info
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = status.replace(Regex("^[✅❌⚠️⬇️🔍🔄📁⏸️⏳] ?"), ""),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SaveLocationInfo(selectedMediaType: MediaType) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Outlined.Folder,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = when (selectedMediaType) {
                MediaType.MP3 -> "Saves to Music folder"
                MediaType.MP4 -> "Saves to Movies folder"
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )
    }
}

@Composable
private fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val settings by settingsManager.settings.collectAsState()

    var showQualityDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var cacheCleared by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf(settingsManager.getCacheSize(context)) }

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
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DownloadQuality.entries.forEach { quality ->
                        Surface(
                            onClick = {
                                settingsManager.updateDownloadQuality(quality)
                                showQualityDialog = false
                                android.widget.Toast.makeText(
                                    context,
                                    "Quality set to ${quality.label}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            color = if (settings.downloadQuality == quality) PrimaryPink.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
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
                                    Text(quality.description, color = TextSecondary, fontSize = 12.sp)
                                }
                                if (settings.downloadQuality == quality) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = PrimaryPink
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Clear cache confirmation dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = Color(0xFF1A1A2E),
            modifier = Modifier.clip(RoundedCornerShape(24.dp)),
            icon = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(48.dp)
                    )
                }
            },
            title = {
                Text(
                    "Clear Cache?",
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
                        "Current cache size: $cacheSize",
                        color = PrimaryPink,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This will delete temporary files and free up storage space. Your downloads will not be affected.",
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { showClearCacheDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            val success = settingsManager.clearCache(context)
                            if (success) {
                                cacheCleared = true
                                cacheSize = settingsManager.getCacheSize(context)
                                android.widget.Toast.makeText(
                                    context,
                                    "Cache cleared successfully!",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            showClearCacheDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                    ) {
                        Text("Clear Cache")
                    }
                }
            },
            dismissButton = {}
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
                                PrimaryPink.copy(alpha = 0.3f),
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
                    tint = PrimaryPink,
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
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Download quality setting
                SettingsItem(
                    icon = Icons.Default.HighQuality,
                    title = "Download Quality",
                    subtitle = settings.downloadQuality.label,
                    onClick = { showQualityDialog = true }
                )

                // Storage location (info only)
                SettingsItem(
                    icon = Icons.Outlined.Folder,
                    title = "Storage Location",
                    subtitle = "Music & Movies folders",
                    onClick = {
                        android.widget.Toast.makeText(
                            context,
                            "MP3 → Music folder, MP4 → Movies folder",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                // Notifications toggle
                SettingsToggleItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    subtitle = if (settings.notificationsEnabled) "Show download notifications" else "Notifications disabled",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = {
                        settingsManager.updateNotificationsEnabled(it)
                    }
                )

                // Auto-resume downloads toggle
                SettingsToggleItem(
                    icon = Icons.Default.Refresh,
                    title = "Auto-Resume Downloads",
                    subtitle = if (settings.autoResumeDownloads) "Resume interrupted downloads" else "Manual resume only",
                    checked = settings.autoResumeDownloads,
                    onCheckedChange = {
                        settingsManager.updateAutoResumeDownloads(it)
                    }
                )

                // WiFi-only downloads toggle
                SettingsToggleItem(
                    icon = Icons.Default.Wifi,
                    title = "WiFi Only Downloads",
                    subtitle = if (settings.wifiOnlyDownloads)
                        "Downloads only on WiFi • ${settingsManager.getNetworkType()}"
                    else
                        "Download on any network",
                    checked = settings.wifiOnlyDownloads,
                    onCheckedChange = {
                        settingsManager.updateWifiOnlyDownloads(it)
                        if (it) {
                            android.widget.Toast.makeText(
                                context,
                                "Downloads will only start when connected to WiFi",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

                // Dark Mode toggle
                SettingsToggleItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = if (settings.darkMode) "Dark theme enabled" else "Light theme enabled",
                    checked = settings.darkMode,
                    onCheckedChange = {
                        settingsManager.updateDarkMode(it)
                        android.widget.Toast.makeText(
                            context,
                            if (it) "Dark mode enabled" else "Light mode enabled (restart app to apply)",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                // Clear cache
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = "Clear Cache",
                    subtitle = if (cacheCleared) "✓ Cleared • $cacheSize" else "Cache size: $cacheSize",
                    onClick = { showClearCacheDialog = true }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PrimaryPink, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryPink,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryPink,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = TextTertiary
                )
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryPink,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .heightIn(max = 600.dp),
        containerColor = Color(0xFF1A1A2E),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // App Icon with gradient glow
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    PrimaryPink.copy(alpha = 0.5f),
                                    PrimaryPurple.copy(alpha = 0.3f),
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
                                    colors = listOf(PrimaryPink, PrimaryPurple)
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
                    color = PrimaryPink.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "v1.0.0",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = PrimaryPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Description
                Text(
                    text = "A powerful media downloader that lets you save videos and audio from 1000+ websites in high quality.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Features Section
                AboutSection(
                    title = "✨ Features",
                    items = listOf(
                        "🎵 Download as MP3 (Best Quality)",
                        "🎬 Download as MP4 (Up to 4K)",
                        "📋 Full Playlist Support",
                        "🎧 Built-in Audio Player",
                        "📺 Built-in Video Player",
                        "⏸️ Pause & Resume Downloads",
                        "📶 WiFi-Only Mode",
                        "🔔 Background Notifications",
                        "📱 Picture-in-Picture Mode"
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Supported Sources Section
                AboutSection(
                    title = "🌐 Supported Sources (1000+)",
                    items = listOf(
                        "📘 Facebook & Instagram",
                        "🐦 Twitter / X",
                        "📱 TikTok",
                        "🎵 SoundCloud",
                        "📺 Vimeo",
                        "🎬 Dailymotion",
                        "🔴 Reddit",
                        "📡 Twitch",
                        "🎮 Bilibili",
                        "🎵 Bandcamp",
                        "🎧 Mixcloud",
                        "📺 NBC, CBS, ABC, CNN",
                        "🎬 Crunchyroll",
                        "📺 Discovery+",
                        "🎵 Deezer (Preview)",
                        "📻 BBC iPlayer",
                        "📺 ITV",
                        "🎬 Arte.tv",
                        "📺 Channel 4",
                        "🇰🇷 Naver TV",
                        "🇯🇵 Niconico",
                        "🇨🇳 Youku, iQIYI",
                        "📱 VK",
                        "📺 Rumble",
                        "🎵 Audiomack",
                        "📻 Spotify (Podcasts)",
                        "And 1000+ more..."
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Technical Info
                AboutSection(
                    title = "⚙️ Powered By",
                    items = listOf(
                        "yt-dlp - Media Extraction",
                        "FFmpeg - Audio/Video Processing",
                        "Jetpack Compose - Modern UI",
                        "Material Design 3"
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Footer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Made with ❤️ for media lovers",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "© 2026 MediaGrab",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPink
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Got it!", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun AboutSection(
    title: String,
    items: List<String>
) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpDialog(
    onDismiss: () -> Unit
) {
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
                                PrimaryBlue.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = PrimaryBlue,
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
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HelpItem(
                    title = "How to download?",
                    description = "Paste a media link URL and tap Download. Choose MP3 for audio or MP4 for video."
                )

                HelpItem(
                    title = "Playlist downloads",
                    description = "The app detects playlists automatically. You can download all or just one video."
                )

                HelpItem(
                    title = "Where are files saved?",
                    description = "MP3 files go to Music folder, MP4 files go to Movies folder on your device."
                )

                HelpItem(
                    title = "Play All & Shuffle",
                    description = "Long-press any file in Media Library to play all or shuffle your collection."
                )

                HelpItem(
                    title = "Audio from video",
                    description = "In the video player, tap the music note icon to switch to audio-only mode."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!", color = PrimaryBlue, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun HelpItem(
    title: String,
    description: String
) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun NetworkStatusBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = WarningOrange.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = WarningOrange,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "No connection",
                color = WarningOrange,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ResumableDownloadsBanner(
    count: Int,
    onResumeAll: () -> Unit,
    onCancelAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PrimaryPink.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Info icon
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = null,
                tint = PrimaryPink,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Text
            Text(
                text = "$count paused",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )

            // Resume All button
            IconButton(
                onClick = onResumeAll,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        SuccessGreen.copy(alpha = 0.2f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume All",
                    tint = SuccessGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Cancel All button
            IconButton(
                onClick = onCancelAll,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        ErrorRed.copy(alpha = 0.2f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel All",
                    tint = ErrorRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectionRestoredBanner(
    pausedCount: Int,
    onResumeAll: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SuccessGreen.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Success icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        SuccessGreen.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Connection Restored! 🎉",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                if (pausedCount > 0) {
                    Text(
                        text = "$pausedCount download(s) ready to resume",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (pausedCount > 0) {
                Button(
                    onClick = onResumeAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Resume",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
