package com.example.dwn.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.dwn.data.DeviceMediaItem
import com.example.dwn.data.DownloadItem
import com.example.dwn.data.MediaPlayStats
import com.example.dwn.data.UnifiedMediaScanner
import com.example.dwn.data.formatDuration
import com.example.dwn.data.formatFileSize
import com.example.dwn.player.AudioPlayerState
import com.example.dwn.player.QueueItem
import com.example.dwn.player.RepeatMode
import com.example.dwn.player.formatTime
import com.example.dwn.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Alias to avoid conflict with player RepeatMode
private typealias AnimRepeatMode = androidx.compose.animation.core.RepeatMode


// Playlist Tab enum
enum class PlaylistTab(val title: String, val icon: ImageVector) {
    MY_PLAYLIST("My Playlist", Icons.AutoMirrored.Filled.QueueMusic),
    MY_DOWNLOADS("My Downloads", Icons.Default.Download),
    DEVICE_MEDIA("Device Media", Icons.Default.PhoneAndroid),
    MY_FAVOURITES("My Favourites", Icons.Default.Favorite)
}

// Media sub-tab for filtering
enum class MediaSubTab(val title: String) {
    ALL("All"),
    AUDIO("Audio"),
    VIDEO("Video")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistsScreen(
    audioPlayerState: AudioPlayerState,
    onBack: () -> Unit,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onPlayPause: () -> Unit,
    downloadedItems: List<DownloadItem> = emptyList(),
    favouriteItems: List<DownloadItem> = emptyList(),
    unifiedFavourites: List<MediaPlayStats> = emptyList(), // Unified favourites from all sources
    onPlayDownloadedItem: (DownloadItem) -> Unit = {},
    onPlayDeviceMedia: (DeviceMediaItem) -> Unit = {},
    onPlayFavouriteItem: (MediaPlayStats) -> Unit = {}, // Play from unified favourites
    onRequestMediaPermission: () -> Unit = {} // Permission request callback
) {
    // Handle system back button
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { PlaylistTab.entries.size })
    var selectedTab by remember { mutableStateOf(PlaylistTab.MY_PLAYLIST) }

    // Use unified media scanner
    val mediaScanner = remember { UnifiedMediaScanner.getInstance(context) }
    val scanState by mediaScanner.scanState.collectAsState()

    // Track if we need to trigger a scan
    var needsScan by remember { mutableStateOf(false) }

    // Load device media when Device Media tab is selected
    LaunchedEffect(pagerState.currentPage) {
        selectedTab = PlaylistTab.entries[pagerState.currentPage]
        if (selectedTab == PlaylistTab.DEVICE_MEDIA) {
            Log.d("PlaylistsScreen", "=== Device Media Tab Selected ===")
            val hasPermission = mediaScanner.hasMediaPermission()
            Log.d("PlaylistsScreen", "hasPermission: $hasPermission")
            Log.d("PlaylistsScreen", "Current audio files: ${scanState.audioFiles.size}")
            Log.d("PlaylistsScreen", "Current video files: ${scanState.videoFiles.size}")
            Log.d("PlaylistsScreen", "isScanning: ${scanState.isScanning}")

            if (hasPermission) {
                // Always scan if we have no files yet
                if (scanState.audioFiles.isEmpty() && scanState.videoFiles.isEmpty() && !scanState.isScanning) {
                    Log.d("PlaylistsScreen", "Triggering initial scan...")
                    mediaScanner.scanAllMedia(forceRefresh = true)
                }
            } else {
                Log.d("PlaylistsScreen", "No permission - showing permission request UI")
            }
        }
    }

    // Also trigger scan when permission state changes
    LaunchedEffect(scanState.hasPermission) {
        if (scanState.hasPermission && selectedTab == PlaylistTab.DEVICE_MEDIA) {
            if (scanState.audioFiles.isEmpty() && scanState.videoFiles.isEmpty() && !scanState.isScanning) {
                Log.d("PlaylistsScreen", "Permission granted - triggering scan...")
                mediaScanner.scanAllMedia(forceRefresh = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Animated background
        PlaylistBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            PlaylistTopBar(
                title = when (selectedTab) {
                    PlaylistTab.MY_PLAYLIST -> "Now Playing Queue"
                    PlaylistTab.MY_DOWNLOADS -> "My Downloads"
                    PlaylistTab.DEVICE_MEDIA -> "Device Media"
                    PlaylistTab.MY_FAVOURITES -> "My Favourites"
                },
                subtitle = when (selectedTab) {
                    PlaylistTab.MY_PLAYLIST -> "${audioPlayerState.queue.size} items in queue"
                    PlaylistTab.MY_DOWNLOADS -> "${downloadedItems.size} files"
                    PlaylistTab.DEVICE_MEDIA -> "${scanState.audioFiles.size + scanState.videoFiles.size} files"
                    PlaylistTab.MY_FAVOURITES -> "${favouriteItems.size} most played"
                },
                onBack = onBack,
                showClear = selectedTab == PlaylistTab.MY_PLAYLIST && audioPlayerState.queue.isNotEmpty(),
                onClear = onClearQueue
            )

            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty() && pagerState.currentPage < tabPositions.size) {
                        val currentTabPosition = tabPositions[pagerState.currentPage]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentSize(align = Alignment.BottomStart)
                                .offset(x = currentTabPosition.left)
                                .width(currentTabPosition.width)
                                .height(3.dp)
                                .background(PrimaryPink, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                },
                divider = {}
            ) {
                PlaylistTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 13.sp,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        },
                        selectedContentColor = PrimaryPink,
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal Pager for tab content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (PlaylistTab.entries[page]) {
                    PlaylistTab.MY_PLAYLIST -> {
                        MyPlaylistContent(
                            audioPlayerState = audioPlayerState,
                            onPlayItem = onPlayItem,
                            onRemoveItem = onRemoveItem,
                            onPlayPause = onPlayPause,
                            onToggleShuffle = onToggleShuffle,
                            onToggleRepeat = onToggleRepeat
                        )
                    }
                    PlaylistTab.MY_DOWNLOADS -> {
                        MyDownloadsContent(
                            downloads = downloadedItems,
                            onPlayItem = onPlayDownloadedItem
                        )
                    }
                    PlaylistTab.DEVICE_MEDIA -> {
                        DeviceMediaContent(
                            audioFiles = scanState.audioFiles,
                            videoFiles = scanState.videoFiles,
                            isLoading = scanState.isScanning,
                            hasPermission = scanState.hasPermission,
                            onPlayItem = onPlayDeviceMedia,
                            onRefresh = {
                                scope.launch {
                                    Log.d("PlaylistsScreen", "=== Refresh button clicked ===")
                                    val hasPermission = mediaScanner.hasMediaPermission()
                                    Log.d("PlaylistsScreen", "hasPermission: $hasPermission")
                                    if (hasPermission) {
                                        Log.d("PlaylistsScreen", "Starting refresh scan...")
                                        mediaScanner.refreshMedia()
                                    } else {
                                        Log.d("PlaylistsScreen", "No permission - requesting...")
                                        onRequestMediaPermission()
                                    }
                                }
                            },
                            onRequestPermission = onRequestMediaPermission
                        )
                    }
                    PlaylistTab.MY_FAVOURITES -> {
                        UnifiedFavouritesContent(
                            legacyFavourites = favouriteItems,
                            unifiedFavourites = unifiedFavourites,
                            onPlayLegacyItem = onPlayDownloadedItem,
                            onPlayUnifiedItem = onPlayFavouriteItem
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// MY PLAYLIST CONTENT (Queue)
// ============================================

@Composable
private fun MyPlaylistContent(
    audioPlayerState: AudioPlayerState,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Now Playing Card
        if (audioPlayerState.currentFileId != null) {
            NowPlayingCard(
                state = audioPlayerState,
                onPlayPause = onPlayPause
            )
        }

        // Playback Controls
        PlaybackModeControls(
            isShuffleEnabled = audioPlayerState.isShuffleEnabled,
            repeatMode = audioPlayerState.repeatMode,
            onToggleShuffle = onToggleShuffle,
            onToggleRepeat = onToggleRepeat
        )

        // Queue List
        if (audioPlayerState.queue.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                title = "Queue is Empty",
                subtitle = "Add songs to start playing"
            )
        } else {
            QueueList(
                queue = audioPlayerState.queue,
                currentIndex = audioPlayerState.currentQueueIndex,
                onPlayItem = onPlayItem,
                onRemoveItem = onRemoveItem
            )
        }
    }
}

// ============================================
// MY DOWNLOADS CONTENT
// ============================================

@Composable
private fun MyDownloadsContent(
    downloads: List<DownloadItem>,
    onPlayItem: (DownloadItem) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(MediaSubTab.ALL) }

    val filteredDownloads = remember(downloads, selectedSubTab) {
        when (selectedSubTab) {
            MediaSubTab.ALL -> downloads
            MediaSubTab.AUDIO -> downloads.filter { it.mediaType == "MP3" }
            MediaSubTab.VIDEO -> downloads.filter { it.mediaType == "MP4" }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tabs for Audio/Video filter
        MediaSubTabRow(
            selectedTab = selectedSubTab,
            onTabSelected = { selectedSubTab = it },
            audioCount = downloads.count { it.mediaType == "MP3" },
            videoCount = downloads.count { it.mediaType == "MP4" }
        )

        if (filteredDownloads.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Download,
                title = "No ${if (selectedSubTab == MediaSubTab.ALL) "Downloads" else selectedSubTab.title}",
                subtitle = "Your downloaded media will appear here"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredDownloads) { item ->
                    DownloadItemCard(
                        item = item,
                        onClick = { onPlayItem(item) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun DownloadItemCard(
    item: DownloadItem,
    onClick: () -> Unit
) {
    val isAudio = item.mediaType == "MP3"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Media type icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isAudio) PrimaryPink.copy(alpha = 0.2f) else PrimaryPurple.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isAudio) Icons.Default.MusicNote else Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = if (isAudio) PrimaryPink else PrimaryPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifEmpty { item.fileName },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.mediaType,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAudio) PrimaryPink else PrimaryPurple
                    )
                    Text(
                        text = " • ",
                        color = TextTertiary
                    )
                    Text(
                        text = formatFileSize(item.fileSize),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    if (item.playCount > 0) {
                        Text(
                            text = " • ${item.playCount} plays",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================
// DEVICE MEDIA CONTENT
// ============================================

@Composable
private fun DeviceMediaContent(
    audioFiles: List<DeviceMediaItem>,
    videoFiles: List<DeviceMediaItem>,
    isLoading: Boolean,
    hasPermission: Boolean = true,
    onPlayItem: (DeviceMediaItem) -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit = {}
) {
    var selectedSubTab by remember { mutableStateOf(MediaSubTab.ALL) }

    // Show permission request if no permission
    if (!hasPermission) {
        PermissionRequiredState(onRequestPermission = onRequestPermission)
        return
    }

    val filteredMedia = remember(audioFiles, videoFiles, selectedSubTab) {
        when (selectedSubTab) {
            MediaSubTab.ALL -> audioFiles + videoFiles
            MediaSubTab.AUDIO -> audioFiles
            MediaSubTab.VIDEO -> videoFiles
        }.sortedByDescending { it.dateAdded }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tabs for Audio/Video filter
        MediaSubTabRow(
            selectedTab = selectedSubTab,
            onTabSelected = { selectedSubTab = it },
            audioCount = audioFiles.size,
            videoCount = videoFiles.size
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = PrimaryPink,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Scanning device media...",
                        color = TextSecondary
                    )
                }
            }
        } else if (filteredMedia.isEmpty()) {
            EmptyState(
                icon = Icons.Default.PhoneAndroid,
                title = "No ${if (selectedSubTab == MediaSubTab.ALL) "Media Files" else selectedSubTab.title}",
                subtitle = "Media files on your device will appear here",
                actionLabel = "Refresh",
                onAction = onRefresh
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMedia) { item ->
                    DeviceMediaItemCard(
                        item = item,
                        onClick = { onPlayItem(item) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun DeviceMediaItemCard(
    item: DeviceMediaItem,
    onClick: () -> Unit
) {
    // Determine if audio based on mimeType
    val isAudio = item.mimeType.startsWith("audio")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Media type icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isAudio) PrimaryPink.copy(alpha = 0.2f) else PrimaryPurple.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isAudio) Icons.Default.MusicNote else Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = if (isAudio) PrimaryPink else PrimaryPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.artist.isNullOrEmpty()) {
                    Text(
                        text = item.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(item.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    Text(
                        text = " • ",
                        color = TextTertiary
                    )
                    Text(
                        text = formatFileSize(item.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================
// MY FAVOURITES CONTENT
// ============================================

@Composable
private fun MyFavouritesContent(
    favourites: List<DownloadItem>,
    onPlayItem: (DownloadItem) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(MediaSubTab.ALL) }

    val filteredFavourites = remember(favourites, selectedSubTab) {
        when (selectedSubTab) {
            MediaSubTab.ALL -> favourites
            MediaSubTab.AUDIO -> favourites.filter { it.mediaType == "MP3" }
            MediaSubTab.VIDEO -> favourites.filter { it.mediaType == "MP4" }
        }
    }

    val totalCount = favourites.size
    val isCompetitive = totalCount >= 50

    Column(modifier = Modifier.fillMaxSize()) {
        // Header info
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = PrimaryPink.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, PrimaryPink.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = PrimaryPink,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Most Played Media",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isCompetitive) {
                            "Top 50 tracks competing for badges"
                        } else {
                            "👑 ${totalCount}/50 tracks • All played media earns recognition!"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCompetitive) TextSecondary else Color(0xFF64FFDA)
                    )
                }
            }
        }

        // Sub-tabs for Audio/Video filter
        MediaSubTabRow(
            selectedTab = selectedSubTab,
            onTabSelected = { selectedSubTab = it },
            audioCount = favourites.count { it.mediaType == "MP3" },
            videoCount = favourites.count { it.mediaType == "MP4" }
        )

        if (filteredFavourites.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Favorite,
                title = "No Favourites Yet",
                subtitle = "Play some media to see your most played tracks here"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val itemsToShow = filteredFavourites.take(50)
                items(itemsToShow) { item ->
                    FavouriteItemCard(
                        item = item,
                        rank = itemsToShow.indexOf(item) + 1,
                        totalItems = itemsToShow.size,
                        onClick = { onPlayItem(item) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun FavouriteItemCard(
    item: DownloadItem,
    rank: Int,
    totalItems: Int,
    onClick: () -> Unit
) {
    val isAudio = item.mediaType == "MP3"

    // Scoring algorithm: Until we have 50 competing items, any played file earns recognition
    // Top 3 get gold/silver/bronze, ranks 4-10 get "Rising Star" status
    // When total < 50, we're more generous with badges
    val isCompetitive = totalItems >= 50
    val rankColor = when {
        rank == 1 -> Color(0xFFFFD700) // Gold - #1 always gets gold
        rank == 2 -> Color(0xFFC0C0C0) // Silver
        rank == 3 -> Color(0xFFCD7F32) // Bronze
        !isCompetitive && rank <= 10 -> Color(0xFF64FFDA) // Teal - Rising star (when < 50 items)
        !isCompetitive && rank <= 25 -> PrimaryPink.copy(alpha = 0.8f) // Pink - Notable (when < 50 items)
        else -> TextSecondary
    }

    val badgeText = when {
        rank == 1 -> "👑 #1"
        rank == 2 -> "🥈 #2"
        rank == 3 -> "🥉 #3"
        !isCompetitive && rank <= 10 -> "⭐ #$rank"
        !isCompetitive && rank <= 25 -> "✨ #$rank"
        else -> "#$rank"
    }

    val showSpecialBorder = rank <= 3 || (!isCompetitive && rank <= 10)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = if (showSpecialBorder) BorderStroke(1.dp, rankColor.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            Surface(
                modifier = Modifier.size(if (rank <= 3) 36.dp else 32.dp),
                shape = CircleShape,
                color = if (showSpecialBorder) rankColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = rankColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (rank <= 3) 10.sp else 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Media type icon
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isAudio) PrimaryPink.copy(alpha = 0.2f) else PrimaryPurple.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isAudio) Icons.Default.MusicNote else Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = if (isAudio) PrimaryPink else PrimaryPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifEmpty { item.fileName },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = PrimaryPink,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.playCount} plays",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryPink,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " • ${item.mediaType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================
// UNIFIED FAVOURITES CONTENT (All Sources)
// ============================================

/**
 * Unified favourites display showing all played media from downloads, device, and streams.
 * Combines data from legacy DownloadItem and new MediaPlayStats for comprehensive tracking.
 */
@Composable
private fun UnifiedFavouritesContent(
    legacyFavourites: List<DownloadItem>,
    unifiedFavourites: List<MediaPlayStats>,
    onPlayLegacyItem: (DownloadItem) -> Unit,
    onPlayUnifiedItem: (MediaPlayStats) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(MediaSubTab.ALL) }
    var selectedSource by remember { mutableStateOf("ALL") } // ALL, DOWNLOAD, DEVICE
    var displayLimit by remember { mutableStateOf(50) } // Default limit
    var showLimitDialog by remember { mutableStateOf(false) }
    var limitInputText by remember { mutableStateOf("50") }

    // Merge legacy and unified favourites, prioritizing unified stats
    val allFavourites = remember(legacyFavourites, unifiedFavourites) {
        // Create a combined list with play stats
        val unified = unifiedFavourites.map { stat ->
            UnifiedFavouriteItem(
                id = stat.id,
                title = stat.title,
                artist = stat.artist,
                mediaType = stat.mediaType,
                mediaSource = stat.mediaSource,
                playCount = stat.playCount,
                lastPlayedAt = stat.lastPlayedAt,
                mediaUri = stat.mediaUri,
                isLegacy = false,
                legacyItem = null,
                unifiedItem = stat
            )
        }

        // Add any legacy items not in unified (fallback)
        val legacyNotInUnified = legacyFavourites.filter { legacy ->
            unified.none { it.id == "download_${legacy.id}" }
        }.map { legacy ->
            UnifiedFavouriteItem(
                id = "legacy_${legacy.id}",
                title = legacy.title.ifEmpty { legacy.fileName },
                artist = null,
                mediaType = if (legacy.mediaType == "MP3") "AUDIO" else "VIDEO",
                mediaSource = "DOWNLOAD",
                playCount = legacy.playCount,
                lastPlayedAt = legacy.lastPlayedAt,
                mediaUri = legacy.filePath,
                isLegacy = true,
                legacyItem = legacy,
                unifiedItem = null
            )
        }

        (unified + legacyNotInUnified).sortedByDescending { it.playCount }
    }

    // Filter by media type and source
    val filteredFavourites = remember(allFavourites, selectedSubTab, selectedSource) {
        allFavourites
            .filter { item ->
                when (selectedSubTab) {
                    MediaSubTab.ALL -> true
                    MediaSubTab.AUDIO -> item.mediaType == "AUDIO"
                    MediaSubTab.VIDEO -> item.mediaType == "VIDEO"
                }
            }
            .filter { item ->
                when (selectedSource) {
                    "ALL" -> true
                    "DOWNLOAD" -> item.mediaSource == "DOWNLOAD"
                    "DEVICE" -> item.mediaSource == "DEVICE"
                    else -> true
                }
            }
    }

    val totalCount = allFavourites.size
    val isCompetitive = totalCount >= displayLimit

    // Limit dialog
    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            containerColor = DarkBackground,
            titleContentColor = Color.White,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = "Set Display Limit",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter the number of top tracks to display (1-500):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = limitInputText,
                        onValueChange = { newValue ->
                            // Only allow numeric input
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                limitInputText = newValue
                            }
                        },
                        label = { Text("Top N tracks") },
                        placeholder = { Text("e.g., 20, 50, 100") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPink,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = PrimaryPink,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = PrimaryPink,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Quick preset buttons
                    Text(
                        text = "Quick presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 20, 50, 100).forEach { preset ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { limitInputText = preset.toString() },
                                shape = RoundedCornerShape(8.dp),
                                color = if (limitInputText == preset.toString())
                                    PrimaryPink.copy(alpha = 0.3f)
                                else
                                    Color.White.copy(alpha = 0.1f),
                                border = BorderStroke(
                                    1.dp,
                                    if (limitInputText == preset.toString())
                                        PrimaryPink
                                    else
                                        Color.White.copy(alpha = 0.2f)
                                )
                            ) {
                                Text(
                                    text = preset.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (limitInputText == preset.toString())
                                        PrimaryPink
                                    else
                                        TextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newLimit = limitInputText.toIntOrNull()?.coerceIn(1, 500) ?: 50
                        displayLimit = newLimit
                        limitInputText = newLimit.toString()
                        showLimitDialog = false
                    }
                ) {
                    Text("Apply", color = PrimaryPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header info with stats - subtle styling
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Most Played Media",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isCompetitive) {
                                "Top $displayLimit tracks competing for badges"
                            } else {
                                "👑 ${totalCount}/$displayLimit tracks • All played media earns recognition!"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCompetitive) TextSecondary else Color(0xFF64FFDA)
                        )
                    }

                    // Limit settings button
                    Surface(
                        modifier = Modifier
                            .clickable {
                                limitInputText = displayLimit.toString()
                                showLimitDialog = true
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Set limit",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Top $displayLimit",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Source counts
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val downloadCount = allFavourites.count { it.mediaSource == "DOWNLOAD" }
                    val deviceCount = allFavourites.count { it.mediaSource == "DEVICE" }

                    SourceChip(
                        label = "All",
                        count = totalCount,
                        isSelected = selectedSource == "ALL",
                        onClick = { selectedSource = "ALL" }
                    )
                    SourceChip(
                        label = "Downloads",
                        count = downloadCount,
                        isSelected = selectedSource == "DOWNLOAD",
                        onClick = { selectedSource = "DOWNLOAD" }
                    )
                    SourceChip(
                        label = "Device",
                        count = deviceCount,
                        isSelected = selectedSource == "DEVICE",
                        onClick = { selectedSource = "DEVICE" }
                    )
                }
            }
        }

        // Sub-tabs for Audio/Video filter
        MediaSubTabRow(
            selectedTab = selectedSubTab,
            onTabSelected = { selectedSubTab = it },
            audioCount = allFavourites.count { it.mediaType == "AUDIO" },
            videoCount = allFavourites.count { it.mediaType == "VIDEO" }
        )

        if (filteredFavourites.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Favorite,
                title = "No Favourites Yet",
                subtitle = "Play some media to see your most played tracks here.\nBoth downloads and device media are tracked!"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val itemsToShow = filteredFavourites.take(displayLimit)
                items(itemsToShow) { item ->
                    UnifiedFavouriteItemCard(
                        item = item,
                        rank = itemsToShow.indexOf(item) + 1,
                        totalItems = itemsToShow.size,
                        displayLimit = displayLimit,
                        onClick = {
                            if (item.isLegacy && item.legacyItem != null) {
                                onPlayLegacyItem(item.legacyItem)
                            } else if (item.unifiedItem != null) {
                                onPlayUnifiedItem(item.unifiedItem)
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

/**
 * Data class to unify legacy DownloadItem and new MediaPlayStats
 */
private data class UnifiedFavouriteItem(
    val id: String,
    val title: String,
    val artist: String?,
    val mediaType: String, // "AUDIO" or "VIDEO"
    val mediaSource: String, // "DOWNLOAD", "DEVICE", "STREAM"
    val playCount: Int,
    val lastPlayedAt: Long?,
    val mediaUri: String,
    val isLegacy: Boolean,
    val legacyItem: DownloadItem?,
    val unifiedItem: MediaPlayStats?
)

@Composable
private fun SourceChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White.copy(alpha = 0.7f) else TextTertiary
            )
        }
    }
}

@Composable
private fun OverallStatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun UnifiedFavouriteItemCard(
    item: UnifiedFavouriteItem,
    rank: Int,
    totalItems: Int,
    displayLimit: Int = 50,
    onClick: () -> Unit
) {
    val isAudio = item.mediaType == "AUDIO"
    var showDetails by remember { mutableStateOf(false) }

    // Scoring algorithm - adapts to user's chosen limit
    // isCompetitive means we have reached the user's limit
    val isCompetitive = totalItems >= displayLimit

    // Calculate dynamic badge thresholds based on display limit
    val risingStarThreshold = (displayLimit * 0.2).toInt().coerceAtLeast(4) // Top 20%
    val notableThreshold = (displayLimit * 0.5).toInt().coerceAtLeast(10) // Top 50%

    // More subtle rank colors
    val rankColor = when {
        rank == 1 -> Color(0xFFFFD700).copy(alpha = 0.85f) // Gold - slightly toned
        rank == 2 -> Color(0xFFC0C0C0).copy(alpha = 0.85f) // Silver - slightly toned
        rank == 3 -> Color(0xFFCD7F32).copy(alpha = 0.85f) // Bronze - slightly toned
        !isCompetitive && rank <= risingStarThreshold -> Color(0xFF64FFDA).copy(alpha = 0.6f) // Teal - more subtle
        !isCompetitive && rank <= notableThreshold -> Color.White.copy(alpha = 0.5f) // White subtle
        else -> TextSecondary.copy(alpha = 0.7f)
    }

    // Badge text with emoji for top ranks
    val badgeText = when {
        rank == 1 -> "👑 #1"
        rank == 2 -> "🥈 #2"
        rank == 3 -> "🥉 #3"
        !isCompetitive && rank <= risingStarThreshold -> "⭐ #$rank"
        !isCompetitive && rank <= notableThreshold -> "✨ #$rank"
        else -> "#$rank"
    }

    // Rank title based on position
    val rankTitle = when {
        rank == 1 -> "Champion"
        rank == 2 -> "Runner-up"
        rank == 3 -> "Third Place"
        rank <= 5 -> "Top 5"
        rank <= 10 -> "Top 10"
        rank <= risingStarThreshold -> "Rising Star"
        rank <= notableThreshold -> "Notable"
        else -> "Played"
    }

    val showSpecialBorder = rank <= 3

    // Source indicator color - more subtle
    val sourceColor = when (item.mediaSource) {
        "DOWNLOAD" -> Color(0xFF4CAF50).copy(alpha = 0.7f) // Green subtle
        "DEVICE" -> Color(0xFF2196F3).copy(alpha = 0.7f) // Blue subtle
        else -> TextSecondary
    }

    // Format last played time
    val lastPlayedText = item.lastPlayedAt?.let { timestamp ->
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            else -> "${diff / 604_800_000}w ago"
        }
    } ?: "Never"

    // Calculate play frequency (plays per day since first play)
    val playFrequency = item.unifiedItem?.let { stats ->
        val daysSinceCreation = ((System.currentTimeMillis() - stats.createdAt) / 86_400_000.0).coerceAtLeast(1.0)
        stats.playCount / daysSinceCreation
    } ?: 0.0

    val frequencyLabel = when {
        playFrequency >= 5.0 -> "🔥 Heavy rotation"
        playFrequency >= 2.0 -> "💫 Frequent play"
        playFrequency >= 1.0 -> "🎵 Regular"
        playFrequency >= 0.5 -> "📻 Occasional"
        else -> "💤 Rare play"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = if (showSpecialBorder) BorderStroke(1.dp, rankColor.copy(alpha = 0.2f)) else null
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank badge - subtle
                Surface(
                    modifier = Modifier.size(if (rank <= 3) 38.dp else 32.dp),
                    shape = CircleShape,
                    color = if (showSpecialBorder) rankColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = rankColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (rank <= 3) 9.sp else 8.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Media type icon with source indicator - subtle styling
                Box {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAudio) PrimaryPink.copy(alpha = 0.12f) else PrimaryPurple.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isAudio) Icons.Default.MusicNote else Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = if (isAudio) PrimaryPink.copy(alpha = 0.7f) else PrimaryPurple.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    // Source indicator dot
                    Surface(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = sourceColor,
                        border = BorderStroke(1.5.dp, DarkBackground)
                    ) {}
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!item.artist.isNullOrEmpty()) {
                        Text(
                            text = item.artist,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Play count chip
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PrimaryPink.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = PrimaryPink.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${item.playCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryPink.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        // Source chip
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = sourceColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = item.mediaSource.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = sourceColor.copy(alpha = 0.9f),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        // Last played
                        Text(
                            text = "• $lastPlayedText",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            fontSize = 9.sp
                        )
                    }
                }

                // Expand/Play button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = { showDetails = !showDetails },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Details",
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Surface(
                        onClick = onClick,
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = if (isAudio) PrimaryPink.copy(alpha = 0.15f) else PrimaryPurple.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = if (isAudio) PrimaryPink else PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Expanded details section
            AnimatedVisibility(
                visible = showDetails,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.03f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stats row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailStatItem(
                                icon = Icons.Default.Star,
                                label = "Rank",
                                value = rankTitle,
                                color = rankColor
                            )
                            DetailStatItem(
                                icon = Icons.Default.Repeat,
                                label = "Total Plays",
                                value = "${item.playCount}",
                                color = PrimaryPink
                            )
                            DetailStatItem(
                                icon = Icons.Default.TrendingUp,
                                label = "Frequency",
                                value = String.format("%.1f/day", playFrequency),
                                color = Color(0xFF64FFDA)
                            )
                        }

                        // Stats row 2 - additional details from MediaPlayStats
                        item.unifiedItem?.let { stats ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DetailStatItem(
                                    icon = Icons.Default.CheckCircle,
                                    label = "Completed",
                                    value = "${stats.completedPlays}",
                                    color = Color(0xFF4CAF50)
                                )
                                DetailStatItem(
                                    icon = Icons.Default.Timer,
                                    label = "Total Time",
                                    value = formatTotalDuration(stats.totalPlayDuration),
                                    color = Color(0xFF2196F3)
                                )
                                DetailStatItem(
                                    icon = Icons.Default.Speed,
                                    label = "Duration",
                                    value = if (stats.duration > 0) formatDuration(stats.duration) else "N/A",
                                    color = TextSecondary
                                )
                            }
                        }

                        // Frequency label
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                playFrequency >= 5.0 -> Color(0xFFFF5722).copy(alpha = 0.15f)
                                playFrequency >= 2.0 -> PrimaryPink.copy(alpha = 0.15f)
                                playFrequency >= 1.0 -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                else -> Color.White.copy(alpha = 0.05f)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = frequencyLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatTotalDuration(totalMs: Long): String {
    if (totalMs <= 0) return "0m"
    val totalMinutes = totalMs / 60_000
    return when {
        totalMinutes >= 60 -> "${totalMinutes / 60}h ${totalMinutes % 60}m"
        else -> "${totalMinutes}m"
    }
}

// ============================================
// SHARED COMPONENTS
// ============================================

@Composable
private fun MediaSubTabRow(
    selectedTab: MediaSubTab,
    onTabSelected: (MediaSubTab) -> Unit,
    audioCount: Int,
    videoCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MediaSubTab.entries.forEach { tab ->
            val count = when (tab) {
                MediaSubTab.ALL -> audioCount + videoCount
                MediaSubTab.AUDIO -> audioCount
                MediaSubTab.VIDEO -> videoCount
            }
            val isSelected = selectedTab == tab

            Surface(
                onClick = { onTabSelected(tab) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) PrimaryPink.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                border = if (isSelected) BorderStroke(1.dp, PrimaryPink.copy(alpha = 0.5f)) else null
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) PrimaryPink else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) PrimaryPink.copy(alpha = 0.7f) else TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
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
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                TextButton(onClick = onAction) {
                    Text(
                        text = actionLabel,
                        color = PrimaryPink
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequiredState(
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val needsManageStorage = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R &&
            !android.os.Environment.isExternalStorageManager()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = PrimaryPink.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
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
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (needsManageStorage) {
                    "MediaGrab needs 'All files access' permission to scan and play your device media files"
                } else {
                    "MediaGrab needs permission to access your media files"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPink),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (needsManageStorage) "Open Settings" else "Grant Permission")
            }

            if (needsManageStorage) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tap to open Settings → Enable 'All files access' → Return to app",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Refresh button to re-check permission status
            TextButton(onClick = onRequestPermission) {
                Icon(
                    Icons.Default.Refresh,
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
@Composable
private fun PlaylistBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(x = (-80).dp, y = (-80).dp)
                .blur(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryPink.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryPurple.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    showClear: Boolean,
    onClear: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        actions = {
            if (showClear) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.ClearAll,
                        contentDescription = "Clear Queue",
                        tint = TextSecondary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun NowPlayingCard(
    state: AudioPlayerState,
    onPlayPause: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        color = PrimaryPink.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, PrimaryPink.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art placeholder
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryPink.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (state.isPlaying) {
                            // Animated equalizer bars
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.height(24.dp)
                            ) {
                                repeat(4) { index ->
                                    AnimatedEqualizerBar(index)
                                }
                            }
                        } else {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = PrimaryPink,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryPink,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.currentFileName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatTime(state.currentPosition)} / ${formatTime(state.duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Play/Pause button
                Surface(
                    onClick = onPlayPause,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = PrimaryPink
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Progress bar
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = {
                    if (state.duration > 0) {
                        state.currentPosition.toFloat() / state.duration.toFloat()
                    } else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = PrimaryPink,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun AnimatedEqualizerBar(index: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_bar_$index")

    val height by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 300 + (index * 100),
                easing = FastOutSlowInEasing
            ),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "height_$index"
    )

    Box(
        modifier = Modifier
            .width(4.dp)
            .height(height.dp)
            .background(
                PrimaryPink,
                RoundedCornerShape(2.dp)
            )
    )
}

@Composable
private fun PlaybackModeControls(
    isShuffleEnabled: Boolean,
    repeatMode: com.example.dwn.player.RepeatMode,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle control
            PlaybackModeButton(
                icon = Icons.Default.Shuffle,
                label = "Shuffle",
                isActive = isShuffleEnabled,
                activeLabel = "On",
                inactiveLabel = "Off",
                onClick = onToggleShuffle
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            // Repeat control
            PlaybackModeButton(
                icon = when (repeatMode) {
                    com.example.dwn.player.RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                label = "Repeat",
                isActive = repeatMode != com.example.dwn.player.RepeatMode.OFF,
                activeLabel = when (repeatMode) {
                    com.example.dwn.player.RepeatMode.ONE -> "One"
                    com.example.dwn.player.RepeatMode.ALL -> "All"
                    else -> "Off"
                },
                inactiveLabel = "Off",
                onClick = onToggleRepeat
            )
        }
    }
}

@Composable
private fun PlaybackModeButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeLabel: String,
    inactiveLabel: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = if (isActive) PrimaryPink.copy(alpha = 0.2f) else Color.Transparent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) PrimaryPink else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) PrimaryPink else TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (isActive) activeLabel else inactiveLabel,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontSize = 10.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueList(
    queue: List<QueueItem>,
    currentIndex: Int,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "UP NEXT",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(queue.size) { index ->
            val item = queue[index]
            val isCurrentlyPlaying = index == currentIndex

            QueueItemCard(
                item = item,
                index = index,
                isCurrentlyPlaying = isCurrentlyPlaying,
                onPlay = { onPlayItem(index) },
                onRemove = { onRemoveItem(index) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueItemCard(
    item: QueueItem,
    index: Int,
    isCurrentlyPlaying: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onRemove
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentlyPlaying) {
            PrimaryPink.copy(alpha = 0.15f)
        } else {
            Color.White.copy(alpha = 0.05f)
        },
        border = if (isCurrentlyPlaying) {
            BorderStroke(1.dp, PrimaryPink.copy(alpha = 0.3f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index or playing indicator
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentlyPlaying) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = PrimaryPink,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Media type icon
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (item.isVideo) {
                    PrimaryPurple.copy(alpha = 0.2f)
                } else {
                    PrimaryPink.copy(alpha = 0.2f)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (item.isVideo) Icons.Default.VideoLibrary else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (item.isVideo) PrimaryPurple else PrimaryPink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentlyPlaying) PrimaryPink else Color.White,
                    fontWeight = if (isCurrentlyPlaying) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isCurrentlyPlaying) {
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryPink.copy(alpha = 0.7f)
                    )
                }
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ============================================
// UTILITY FUNCTIONS
// ============================================

// Note: formatFileSize and formatDuration are imported from com.example.dwn.data package
// loadDeviceMedia is now handled by UnifiedMediaScanner
