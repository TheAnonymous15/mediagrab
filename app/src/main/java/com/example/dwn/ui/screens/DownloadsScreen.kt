package com.example.dwn.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dwn.data.DeviceMediaItem
import com.example.dwn.data.UnifiedMediaScanner
import com.example.dwn.data.DownloadItem
import com.example.dwn.data.DownloadStatus
import com.example.dwn.data.formatDuration
import com.example.dwn.data.formatFileSize
import com.example.dwn.player.AudioPlayerState
import com.example.dwn.player.formatTime
import com.example.dwn.ui.components.*
import com.example.dwn.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DownloadsScreen(
    downloads: List<DownloadItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onResumeClick: (DownloadItem) -> Unit,
    onPauseClick: (DownloadItem) -> Unit,
    onDeleteClick: (DownloadItem) -> Unit,
    onPlayClick: (DownloadItem) -> Unit,
    onPlayAsAudioClick: (DownloadItem) -> Unit,
    onPlayAllClick: (List<DownloadItem>, Int, Boolean) -> Unit = { _, _, _ -> }, // list, startIndex, shuffle
    onBackClick: () -> Unit,
    activeDownloadId: String?,
    audioPlayerState: AudioPlayerState,
    onAudioPlayPause: () -> Unit,
    onAudioStop: () -> Unit,
    onAudioSeekForward: () -> Unit,
    onAudioSeekBackward: () -> Unit,
    onAudioSeek: (Float) -> Unit,
    onToggleRepeat: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onPlayNext: () -> Unit = {},
    onPlayPrevious: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onPlaylistsClick: () -> Unit = {},
    onPlayDeviceMedia: (DeviceMediaItem, Boolean) -> Unit = { _, _ -> },
    onPlayDeviceMediaAsAudio: (DeviceMediaItem) -> Unit = {},
    onPlayAllDeviceMedia: (List<DeviceMediaItem>, Int, Boolean) -> Unit = { _, _, _ -> },
    onRequestMediaPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    // Use unified media scanner singleton
    val mediaScanner = remember { UnifiedMediaScanner.getInstance(context) }
    val scanState by mediaScanner.scanState.collectAsState()

    var deviceMediaTab by remember { mutableIntStateOf(0) } // 0 = Audio, 1 = Video

    // Rescan function that can be triggered after permission grant
    val rescanMedia: () -> Unit = {
        scope.launch {
            if (mediaScanner.hasMediaPermission()) {
                mediaScanner.refreshMedia()
            } else {
                // Request permission from MainActivity
                onRequestMediaPermission()
            }
        }
    }

    // Scan device media when on Device Media tab
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1) {
            val hasPermission = mediaScanner.hasMediaPermission()
            android.util.Log.d("DownloadsScreen", "Device Media tab - hasPermission: $hasPermission")

            if (hasPermission && scanState.audioFiles.isEmpty() && scanState.videoFiles.isEmpty()) {
                android.util.Log.d("DownloadsScreen", "Starting media scan...")
                mediaScanner.scanAllMedia()
            }
        }
    }

    // Separate downloads by type - only include completed for playback
    val audioDownloads = downloads.filter { it.mediaType == "MP3" && it.status == DownloadStatus.COMPLETED }
    val videoDownloads = downloads.filter { it.mediaType == "MP4" && it.status == DownloadStatus.COMPLETED }

    Box(modifier = modifier.fillMaxSize()) {
        // Background
        GradientBackground {
            AnimatedGradientOrbs()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            TopBar(
                onBackClick = onBackClick,
                downloadCount = downloads.size,
                audioCount = audioDownloads.size,
                videoCount = videoDownloads.size
            )

            // Mini Player with swipe to seek
            AnimatedVisibility(
                visible = audioPlayerState.currentFileId != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                SwipeableMiniPlayer(
                    state = audioPlayerState,
                    onPlayPause = onAudioPlayPause,
                    onStop = onAudioStop,
                    onSeekForward = onAudioSeekForward,
                    onSeekBackward = onAudioSeekBackward,
                    onSeek = onAudioSeek,
                    onToggleRepeat = onToggleRepeat,
                    onToggleShuffle = onToggleShuffle,
                    onPlayNext = onPlayNext,
                    onPlayPrevious = onPlayPrevious,
                    onEqualizerClick = onEqualizerClick,
                    onPlaylistsClick = onPlaylistsClick
                )
            }

            // Tab Row
            GlassmorphicTabRow(
                selectedTabIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    scope.launch { pagerState.animateScrollToPage(index) }
                }
            )

            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        // My Downloads Tab
                        MyDownloadsTab(
                            audioDownloads = audioDownloads.filter {
                                searchQuery.isEmpty() ||
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.fileName.contains(searchQuery, ignoreCase = true)
                            },
                            videoDownloads = videoDownloads.filter {
                                searchQuery.isEmpty() ||
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.fileName.contains(searchQuery, ignoreCase = true)
                            },
                            activeDownloadId = activeDownloadId,
                            audioPlayerState = audioPlayerState,
                            onResumeClick = onResumeClick,
                            onPauseClick = onPauseClick,
                            onDeleteClick = onDeleteClick,
                            onPlayClick = onPlayClick,
                            onPlayAsAudioClick = onPlayAsAudioClick,
                            onPlayAllClick = onPlayAllClick
                        )
                    }
                    1 -> {
                        // Device Media Tab
                        DeviceMediaTab(
                            audioFiles = scanState.audioFiles.filter {
                                searchQuery.isEmpty() ||
                                it.name.contains(searchQuery, ignoreCase = true)
                            },
                            videoFiles = scanState.videoFiles.filter {
                                searchQuery.isEmpty() ||
                                it.name.contains(searchQuery, ignoreCase = true)
                            },
                            isScanning = scanState.isScanning,
                            selectedTab = deviceMediaTab,
                            onTabChange = { deviceMediaTab = it },
                            onPlayMedia = onPlayDeviceMedia,
                            onPlayAsAudio = onPlayDeviceMediaAsAudio,
                            onPlayAllMedia = onPlayAllDeviceMedia,
                            audioPlayerState = audioPlayerState,
                            hasPermission = scanState.hasPermission,
                            onRequestPermission = rescanMedia
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onBackClick: () -> Unit,
    downloadCount: Int,
    audioCount: Int,
    videoCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "Media Library",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "🎵 $audioCount audio • 🎬 $videoCount video",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun GlassmorphicTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("My Downloads" to Icons.Default.Download, "Device Media" to Icons.Default.PhoneAndroid).forEachIndexed { index, (title, icon) ->
                val isSelected = selectedTabIndex == index
                Surface(
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        Color.White.copy(alpha = 0.15f)
                    } else {
                        Color.Transparent
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) PrimaryPink else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeableMiniPlayer(
    state: AudioPlayerState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleRepeat: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onPlayNext: () -> Unit = {},
    onPlayPrevious: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onPlaylistsClick: () -> Unit = {}
) {
    var seekDelta by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var showExtendedControls by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isSeeking = true },
                    onDragEnd = {
                        if (seekDelta != 0f) {
                            // Calculate seek amount based on drag distance
                            val seekAmount = (seekDelta / size.width) * 60 // Max 60 seconds
                            val currentPos = state.currentPosition
                            val newPos = (currentPos + (seekAmount * 1000).toLong())
                                .coerceIn(0L, state.duration)
                            onSeek(newPos.toFloat() / state.duration.coerceAtLeast(1))
                        }
                        seekDelta = 0f
                        isSeeking = false
                    },
                    onDragCancel = {
                        seekDelta = 0f
                        isSeeking = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        seekDelta += dragAmount
                    }
                )
            }
            .clickable { showExtendedControls = !showExtendedControls },
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Seek indicator
            AnimatedVisibility(visible = isSeeking && seekDelta != 0f) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryPink.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (seekDelta > 0) "⏩ +${(seekDelta / 10).toInt()}s" else "⏪ ${(seekDelta / 10).toInt()}s",
                        modifier = Modifier.padding(8.dp),
                        color = PrimaryPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Progress
            AnimatedProgressBar(
                progress = if (state.duration > 0) {
                    state.currentPosition.toFloat() / state.duration.toFloat()
                } else 0f,
                modifier = Modifier.fillMaxWidth(),
                height = 4.dp,
                showGlow = state.isPlaying
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art placeholder with animation
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryPink.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (state.isPlaying) {
                            PulsingDot(color = PrimaryPink, size = 16.dp)
                        } else {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = PrimaryPink,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Song info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.currentFileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatTime(state.currentPosition)} / ${formatTime(state.duration)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        // Show queue position if in queue
                        if (state.queue.isNotEmpty()) {
                            Text(
                                text = " • ${state.currentQueueIndex + 1}/${state.queue.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryPink
                            )
                        }
                        if (isSeeking) {
                            Text(
                                text = " • Swipe to seek",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryPink
                            )
                        }
                    }
                }

                // Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Repeat button
                    IconButton(
                        onClick = onToggleRepeat,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = when (state.repeatMode) {
                                com.example.dwn.player.RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (state.repeatMode != com.example.dwn.player.RepeatMode.OFF) PrimaryPink else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onSeekBackward,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Replay10,
                            contentDescription = "Rewind",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Surface(
                        onClick = onPlayPause,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = PrimaryPink
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onSeekForward,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Forward10,
                            contentDescription = "Forward",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Stop",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Extended controls (shown when clicked)
            AnimatedVisibility(visible = showExtendedControls) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconButton(
                                onClick = onToggleShuffle,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (state.isShuffleEnabled) PrimaryPink else TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = if (state.isShuffleEnabled) "On" else "Off",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isShuffleEnabled) PrimaryPink else TextTertiary
                            )
                        }

                        // Previous button
                        IconButton(
                            onClick = onPlayPrevious,
                            modifier = Modifier.size(44.dp),
                            enabled = state.queue.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = if (state.queue.isNotEmpty()) Color.White else TextTertiary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Play/Pause (large)
                        Surface(
                            onClick = onPlayPause,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = PrimaryPink
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Next button
                        IconButton(
                            onClick = onPlayNext,
                            modifier = Modifier.size(44.dp),
                            enabled = state.queue.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = if (state.queue.isNotEmpty()) Color.White else TextTertiary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Repeat button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconButton(
                                onClick = onToggleRepeat,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = when (state.repeatMode) {
                                        com.example.dwn.player.RepeatMode.ONE -> Icons.Default.RepeatOne
                                        else -> Icons.Default.Repeat
                                    },
                                    contentDescription = "Repeat",
                                    tint = if (state.repeatMode != com.example.dwn.player.RepeatMode.OFF) PrimaryPink else TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = when (state.repeatMode) {
                                    com.example.dwn.player.RepeatMode.OFF -> "Off"
                                    com.example.dwn.player.RepeatMode.ONE -> "One"
                                    com.example.dwn.player.RepeatMode.ALL -> "All"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.repeatMode != com.example.dwn.player.RepeatMode.OFF) PrimaryPink else TextTertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Equalizer and Playlist buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Equalizer button
                        Surface(
                            onClick = onEqualizerClick,
                            modifier = Modifier
                                .weight(1f)
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

                        // Playlist/Queue button
                        Surface(
                            onClick = onPlaylistsClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryBlue.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.QueueMusic,
                                    contentDescription = "Queue",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Queue (${state.queue.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Hint
            Text(
                text = if (showExtendedControls) "↔ Swipe to seek • Tap to collapse" else "↔ Swipe to seek • Tap for more",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        "Search media...",
                        color = TextTertiary
                    )
                },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = PrimaryPink,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MyDownloadsTab(
    audioDownloads: List<DownloadItem>,
    videoDownloads: List<DownloadItem>,
    activeDownloadId: String?,
    audioPlayerState: AudioPlayerState,
    onResumeClick: (DownloadItem) -> Unit,
    onPauseClick: (DownloadItem) -> Unit,
    onDeleteClick: (DownloadItem) -> Unit,
    onPlayClick: (DownloadItem) -> Unit,
    onPlayAsAudioClick: (DownloadItem) -> Unit,
    onPlayAllClick: (List<DownloadItem>, Int, Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Audio, 1 = Video
    var isGridView by remember { mutableStateOf(false) }

    if (audioDownloads.isEmpty() && videoDownloads.isEmpty()) {
        EmptyState(title = "No Downloads Yet", subtitle = "Your downloaded files will appear here")
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sub-tabs for Audio/Video
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SubTab(
                    title = "Audio",
                    icon = "🎵",
                    count = audioDownloads.size,
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                SubTab(
                    title = "Video",
                    icon = "🎬",
                    count = videoDownloads.size,
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            // View toggle for video tab
            if (selectedTab == 1 && videoDownloads.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // List view button
                    IconButton(
                        onClick = { isGridView = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "List view",
                            tint = if (!isGridView) PrimaryPink else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Grid view button
                    IconButton(
                        onClick = { isGridView = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Grid view",
                            tint = if (isGridView) PrimaryPink else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> {
                    // Audio downloads
                    if (audioDownloads.isEmpty()) {
                        EmptyState(title = "No Audio Downloads", subtitle = "Downloaded MP3 files will appear here")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Play All / Shuffle All buttons
                            item {
                                PlayAllShuffleRow(
                                    itemCount = audioDownloads.size,
                                    onPlayAll = { onPlayAllClick(audioDownloads, 0, false) },
                                    onShuffleAll = { onPlayAllClick(audioDownloads, 0, true) }
                                )
                            }
                            itemsIndexed(audioDownloads, key = { _, item -> item.id }) { index, download ->
                                DownloadCard(
                                    download = download,
                                    isActive = download.id == activeDownloadId,
                                    isPlaying = audioPlayerState.currentFileId == download.id && audioPlayerState.isPlaying,
                                    audioProgress = if (audioPlayerState.currentFileId == download.id) {
                                        audioPlayerState.currentPosition.toFloat() / audioPlayerState.duration.coerceAtLeast(1).toFloat()
                                    } else 0f,
                                    onResumeClick = { onResumeClick(download) },
                                    onPauseClick = { onPauseClick(download) },
                                    onDeleteClick = { onDeleteClick(download) },
                                    onPlayClick = { onPlayClick(download) },
                                    onPlayAsAudioClick = { onPlayAsAudioClick(download) },
                                    onPlayAllClick = { onPlayAllClick(audioDownloads, index, false) },
                                    onShuffleAllClick = { onPlayAllClick(audioDownloads, index, true) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(100.dp)) }
                        }
                    }
                }
                1 -> {
                    // Video downloads
                    if (videoDownloads.isEmpty()) {
                        EmptyState(title = "No Video Downloads", subtitle = "Downloaded MP4 files will appear here")
                    } else {
                        if (isGridView) {
                            // Grid view for videos
                            VideoGridView(
                                videos = videoDownloads,
                                activeDownloadId = activeDownloadId,
                                audioPlayerState = audioPlayerState,
                                onResumeClick = onResumeClick,
                                onPauseClick = onPauseClick,
                                onDeleteClick = onDeleteClick,
                                onPlayClick = onPlayClick,
                                onPlayAsAudioClick = onPlayAsAudioClick
                            )
                        } else {
                            // List view for videos
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Play All / Shuffle All buttons
                                item {
                                    PlayAllShuffleRow(
                                        itemCount = videoDownloads.size,
                                        onPlayAll = { onPlayAllClick(videoDownloads, 0, false) },
                                        onShuffleAll = { onPlayAllClick(videoDownloads, 0, true) }
                                    )
                                }
                                itemsIndexed(videoDownloads, key = { _, item -> item.id }) { index, download ->
                                    DownloadCard(
                                        download = download,
                                        isActive = download.id == activeDownloadId,
                                        isPlaying = audioPlayerState.currentFileId == download.id && audioPlayerState.isPlaying,
                                        audioProgress = if (audioPlayerState.currentFileId == download.id) {
                                            audioPlayerState.currentPosition.toFloat() / audioPlayerState.duration.coerceAtLeast(1).toFloat()
                                        } else 0f,
                                        onResumeClick = { onResumeClick(download) },
                                        onPauseClick = { onPauseClick(download) },
                                        onDeleteClick = { onDeleteClick(download) },
                                        onPlayClick = { onPlayClick(download) },
                                        onPlayAsAudioClick = { onPlayAsAudioClick(download) },
                                        onPlayAllClick = { onPlayAllClick(videoDownloads, index, false) },
                                        onShuffleAllClick = { onPlayAllClick(videoDownloads, index, true) }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(100.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoGridView(
    videos: List<DownloadItem>,
    activeDownloadId: String?,
    audioPlayerState: AudioPlayerState,
    onResumeClick: (DownloadItem) -> Unit,
    onPauseClick: (DownloadItem) -> Unit,
    onDeleteClick: (DownloadItem) -> Unit,
    onPlayClick: (DownloadItem) -> Unit,
    onPlayAsAudioClick: (DownloadItem) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Create rows of 2 items
        val chunkedVideos = videos.chunked(2)
        items(chunkedVideos.size) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                chunkedVideos[rowIndex].forEach { download ->
                    VideoGridCard(
                        download = download,
                        isActive = download.id == activeDownloadId,
                        onPlayClick = { onPlayClick(download) },
                        onPlayAsAudioClick = { onPlayAsAudioClick(download) },
                        onDeleteClick = { onDeleteClick(download) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty space if odd number of items
                if (chunkedVideos[rowIndex].size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
private fun VideoGridCard(
    download: DownloadItem,
    isActive: Boolean,
    onPlayClick: () -> Unit,
    onPlayAsAudioClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    // Try to load thumbnail
    val thumbnailBitmap = remember(download.fileName) {
        try {
            val moviesDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_MOVIES
            )
            val videoFile = java.io.File(moviesDir, download.fileName)
            if (videoFile.exists()) {
                android.media.ThumbnailUtils.createVideoThumbnail(
                    videoFile.absolutePath,
                    android.provider.MediaStore.Images.Thumbnails.MINI_KIND
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    Surface(
        modifier = modifier
            .aspectRatio(16f / 12f)
            .clip(RoundedCornerShape(16.dp)),
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Thumbnail or placeholder
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder with gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    PrimaryBlue.copy(alpha = 0.3f),
                                    PrimaryPurple.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = download.title.ifEmpty { download.fileName },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (download.status == DownloadStatus.COMPLETED)
                                SuccessGreen.copy(alpha = 0.3f)
                            else
                                WarningOrange.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = if (download.status == DownloadStatus.COMPLETED) "MP4" else download.status.name,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Play button overlay
            Surface(
                onClick = onPlayClick,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // More options button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Video") },
                        onClick = {
                            showMenu = false
                            onPlayClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Play as Audio") },
                        onClick = {
                            showMenu = false
                            onPlayAsAudioClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.MusicNote, null)
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = ErrorRed) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, null, tint = ErrorRed)
                        }
                    )
                }
            }

            // Active indicator
            if (isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, PrimaryPink, RoundedCornerShape(16.dp))
                )
            }
        }
    }
}

@Composable
private fun DeviceMediaTab(
    audioFiles: List<DeviceMediaItem>,
    videoFiles: List<DeviceMediaItem>,
    isScanning: Boolean,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    onPlayMedia: (DeviceMediaItem, Boolean) -> Unit,
    onPlayAsAudio: (DeviceMediaItem) -> Unit = {},
    onPlayAllMedia: (List<DeviceMediaItem>, Int, Boolean) -> Unit = { _, _, _ -> },
    audioPlayerState: AudioPlayerState,
    hasPermission: Boolean = true,
    onRequestPermission: () -> Unit = {}
) {
    var isGridView by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tabs for Audio/Video
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SubTab(
                title = "Audio",
                icon = "🎵",
                count = audioFiles.size,
                isSelected = selectedTab == 0,
                onClick = { onTabChange(0) },
                modifier = Modifier.weight(1f)
            )
            SubTab(
                title = "Video",
                icon = "🎬",
                count = videoFiles.size,
                isSelected = selectedTab == 1,
                onClick = { onTabChange(1) },
                modifier = Modifier.weight(1f)
            )
        }

        // Show permission request UI if needed
        if (!hasPermission) {
            PermissionRequiredContent(
                onRequestPermission = onRequestPermission
            )
            return@Column
        }

        // View toggle for video tab
        if (selectedTab == 1 && videoFiles.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { isGridView = false },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = "List view",
                        tint = if (!isGridView) PrimaryPink else TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { isGridView = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Grid view",
                        tint = if (isGridView) PrimaryPink else TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (isScanning) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryPink)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Scanning device media...",
                        color = TextSecondary
                    )
                }
            }
        } else {
            when (selectedTab) {
                0 -> {
                    // Audio files
                    if (audioFiles.isEmpty()) {
                        EmptyState(title = "No Audio Found", subtitle = "No audio files found on device")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Play All / Shuffle All buttons
                            item {
                                PlayAllShuffleRow(
                                    itemCount = audioFiles.size,
                                    onPlayAll = { onPlayAllMedia(audioFiles, 0, false) },
                                    onShuffleAll = { onPlayAllMedia(audioFiles, 0, true) }
                                )
                            }
                            items(audioFiles, key = { it.id }) { item ->
                                DeviceMediaCard(
                                    item = item,
                                    isAudio = true,
                                    isPlaying = audioPlayerState.currentFileId == item.id.toString() && audioPlayerState.isPlaying,
                                    onPlay = { onPlayMedia(item, true) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(100.dp)) }
                        }
                    }
                }
                1 -> {
                    // Video files
                    if (videoFiles.isEmpty()) {
                        EmptyState(title = "No Video Found", subtitle = "No video files found on device")
                    } else {
                        if (isGridView) {
                            // Grid view for device videos
                            DeviceVideoGridView(
                                videos = videoFiles,
                                onPlayMedia = { onPlayMedia(it, false) },
                                onPlayAsAudio = { onPlayAsAudio(it) }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Play All / Shuffle All buttons
                                item {
                                    PlayAllShuffleRow(
                                        itemCount = videoFiles.size,
                                        onPlayAll = { onPlayAllMedia(videoFiles, 0, false) },
                                        onShuffleAll = { onPlayAllMedia(videoFiles, 0, true) }
                                    )
                                }
                                items(videoFiles, key = { it.id }) { item ->
                                    DeviceMediaCard(
                                        item = item,
                                        isAudio = false,
                                        isPlaying = audioPlayerState.currentFileId == item.id.toString() && audioPlayerState.isPlaying,
                                        onPlay = { onPlayMedia(item, false) },
                                        onPlayAsAudio = { onPlayAsAudio(item) }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(100.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceVideoGridView(
    videos: List<DeviceMediaItem>,
    onPlayMedia: (DeviceMediaItem) -> Unit,
    onPlayAsAudio: (DeviceMediaItem) -> Unit = {}
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val chunkedVideos = videos.chunked(2)
        items(chunkedVideos.size) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                chunkedVideos[rowIndex].forEach { item ->
                    DeviceVideoGridCard(
                        item = item,
                        onPlay = { onPlayMedia(item) },
                        onPlayAsAudio = { onPlayAsAudio(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (chunkedVideos[rowIndex].size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
private fun DeviceVideoGridCard(
    item: DeviceMediaItem,
    onPlay: () -> Unit,
    onPlayAsAudio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Try to load thumbnail
    val thumbnailBitmap = remember(item.path) {
        try {
            if (item.path.isNotEmpty()) {
                android.media.ThumbnailUtils.createVideoThumbnail(
                    item.path,
                    android.provider.MediaStore.Images.Thumbnails.MINI_KIND
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    Surface(
        onClick = onPlay,
        modifier = modifier
            .aspectRatio(16f / 12f)
            .clip(RoundedCornerShape(16.dp)),
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Thumbnail or placeholder
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    PrimaryBlue.copy(alpha = 0.3f),
                                    PrimaryPurple.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(item.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "•",
                            color = TextTertiary,
                            fontSize = 8.sp
                        )
                        Text(
                            text = formatFileSize(item.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Play button overlay
            Surface(
                onClick = onPlay,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Audio mode button at top right
            Surface(
                onClick = onPlayAsAudio,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Play as Audio",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayAllShuffleRow(
    itemCount: Int,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item count
            Text(
                text = "$itemCount items",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play All button
                Surface(
                    onClick = onPlayAll,
                    shape = RoundedCornerShape(20.dp),
                    color = PrimaryPink.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = PrimaryPink,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Play All",
                            color = PrimaryPink,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }

                // Shuffle All button
                Surface(
                    onClick = onShuffleAll,
                    shape = RoundedCornerShape(20.dp),
                    color = PrimaryPurple.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Shuffle",
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubTab(
    title: String,
    icon: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            PrimaryPink.copy(alpha = 0.15f)
        } else {
            Color.White.copy(alpha = 0.05f)
        },
        border = if (isSelected) {
            BorderStroke(1.dp, PrimaryPink.copy(alpha = 0.3f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = if (isSelected) PrimaryPink else Color.White,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) PrimaryPink.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "$count",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = if (isSelected) PrimaryPink else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.2f)
        ) {
            Text(
                text = "$count",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceMediaCard(
    item: DeviceMediaItem,
    isAudio: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onPlayAsAudio: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        shape = RoundedCornerShape(16.dp),
        color = if (isPlaying) PrimaryPink.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isAudio) PrimaryPink.copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isPlaying) {
                        PulsingDot(
                            color = if (isAudio) PrimaryPink else PrimaryBlue,
                            size = 16.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isAudio) Icons.Default.MusicNote else Icons.Default.Movie,
                            contentDescription = null,
                            tint = if (isAudio) PrimaryPink else PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info - Clickable to play
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPlay() }
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.artist != null && item.artist != "<unknown>") {
                        Text(
                            text = item.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1
                        )
                        Text(text = "•", color = TextTertiary, fontSize = 10.sp)
                    }
                    Text(
                        text = formatDuration(item.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    Text(text = "•", color = TextTertiary, fontSize = 10.sp)
                    Text(
                        text = formatFileSize(item.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            // Play button
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = if (isAudio) PrimaryPink else PrimaryBlue
                )
            }

            // Audio mode button for videos
            if (!isAudio) {
                IconButton(
                    onClick = onPlayAsAudio,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Play as Audio",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val needsManageStorage = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R &&
            !android.os.Environment.isExternalStorageManager()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = PrimaryPink.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = PrimaryPink,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (needsManageStorage) "All Files Access Required" else "Permission Required",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (needsManageStorage) {
                    "MediaGrab needs 'All files access' permission to scan and play your device media files"
                } else {
                    "MediaGrab needs permission to access your device media files"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (needsManageStorage) {
                        // Open MANAGE_EXTERNAL_STORAGE settings directly
                        try {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                            ).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                            )
                            context.startActivity(intent)
                        }
                    } else {
                        onRequestPermission()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPink
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (needsManageStorage) "Open Settings" else "Grant Permission")
            }

            if (needsManageStorage) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tap to open Settings → Enable 'All files access' → Return to app",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Refresh button to re-check permission status
            TextButton(
                onClick = onRequestPermission
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Refresh after granting",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// DownloadCard moved here for completeness
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadCard(
    download: DownloadItem,
    isActive: Boolean,
    isPlaying: Boolean,
    audioProgress: Float,
    onResumeClick: () -> Unit,
    onPauseClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlayClick: () -> Unit,
    onPlayAsAudioClick: () -> Unit,
    onPlayAllClick: () -> Unit = {},
    onShuffleAllClick: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPlayMenu by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }

    val cardColor = when {
        isPlaying -> PrimaryPink.copy(alpha = 0.15f)
        download.status == DownloadStatus.COMPLETED -> Color.White.copy(alpha = 0.08f)
        download.status == DownloadStatus.DOWNLOADING -> PrimaryBlue.copy(alpha = 0.1f)
        download.status == DownloadStatus.FAILED -> ErrorRed.copy(alpha = 0.1f)
        else -> Color.White.copy(alpha = 0.05f)
    }

    // Long press context menu
    if (showContextMenu) {
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            containerColor = Color(0xFF1A1A2E),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = download.title.ifEmpty { download.fileName },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Play
                    Surface(
                        onClick = {
                            showContextMenu = false
                            onPlayClick()
                        },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = PrimaryPink
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Play", color = Color.White)
                        }
                    }

                    // Play All (from this item)
                    Surface(
                        onClick = {
                            showContextMenu = false
                            onPlayAllClick()
                        },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Play All", color = Color.White)
                        }
                    }

                    // Shuffle All
                    Surface(
                        onClick = {
                            showContextMenu = false
                            onShuffleAllClick()
                        },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = null,
                                tint = PrimaryPurple
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Shuffle All", color = Color.White)
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // Delete
                    Surface(
                        onClick = {
                            showContextMenu = false
                            showDeleteDialog = true
                        },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = ErrorRed
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Delete", color = ErrorRed)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showContextMenu = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    // Click on card plays the file
                    if (download.status == DownloadStatus.COMPLETED) {
                        onPlayClick()
                    }
                },
                onLongClick = {
                    // Long press shows context menu
                    if (download.status == DownloadStatus.COMPLETED) {
                        showContextMenu = true
                    }
                }
            ),
        shape = RoundedCornerShape(20.dp),
        color = cardColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Media Type Icon
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (download.mediaType == "MP3")
                        PrimaryPink.copy(alpha = 0.2f)
                    else
                        PrimaryBlue.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isPlaying) {
                            PulsingDot(
                                color = PrimaryPink,
                                size = 16.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (download.mediaType == "MP3")
                                    Icons.Default.MusicNote
                                else
                                    Icons.Default.Movie,
                                contentDescription = null,
                                tint = if (download.mediaType == "MP3") PrimaryPink else PrimaryBlue,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title and Info - Clickable to play
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = download.status == DownloadStatus.COMPLETED) {
                            onPlayClick()
                        }
                ) {
                    Text(
                        text = download.title.ifEmpty {
                            download.fileName.ifEmpty { "Downloading..." }
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DownloadStatusBadge(status = download.status)

                        Text(
                            text = "•",
                            color = TextTertiary,
                            fontSize = 10.sp
                        )

                        Text(
                            text = download.mediaType,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Play button for completed downloads
                    if (download.status == DownloadStatus.COMPLETED) {
                        // Direct play for both MP3 and MP4
                        IconButton(onClick = onPlayClick) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = if (download.mediaType == "MP3") PrimaryPink else PrimaryBlue
                            )
                        }

                        // For MP4, add a small audio mode button
                        if (download.mediaType == "MP4") {
                            IconButton(
                                onClick = onPlayAsAudioClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Play as Audio",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Resume/Pause for active downloads
                    when (download.status) {
                        DownloadStatus.DOWNLOADING -> {
                            IconButton(onClick = onPauseClick) {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = Color.White
                                )
                            }
                        }
                        DownloadStatus.PAUSED, DownloadStatus.FAILED -> {
                            IconButton(onClick = onResumeClick) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Resume",
                                    tint = SuccessGreen
                                )
                            }
                        }
                        else -> {}
                    }

                    // Delete
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Progress for active downloads
            if (download.status == DownloadStatus.DOWNLOADING || download.status == DownloadStatus.PAUSED) {
                Spacer(modifier = Modifier.height(12.dp))
                AnimatedProgressBar(
                    progress = download.progress,
                    modifier = Modifier.fillMaxWidth(),
                    height = 4.dp,
                    showGlow = download.status == DownloadStatus.DOWNLOADING
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(download.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryPink,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // Audio playback progress
            if (isPlaying && audioProgress > 0f) {
                Spacer(modifier = Modifier.height(12.dp))
                AnimatedProgressBar(
                    progress = audioProgress,
                    modifier = Modifier.fillMaxWidth(),
                    height = 3.dp
                )
            }
        }
    }

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "Delete Download",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = { Text("Are you sure you want to delete this download?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ErrorRed
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun DownloadStatusBadge(status: DownloadStatus) {
    val (text, color) = when (status) {
        DownloadStatus.PENDING -> "Pending" to TextTertiary
        DownloadStatus.DOWNLOADING -> "Downloading" to PrimaryBlue
        DownloadStatus.PAUSED -> "Paused" to WarningOrange
        DownloadStatus.COMPLETED -> "Completed" to SuccessGreen
        DownloadStatus.FAILED -> "Failed" to ErrorRed
        DownloadStatus.CANCELLED -> "Cancelled" to TextTertiary
    }

    StatusBadge(text = text, color = color)
}

