package com.example.dwn

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.example.dwn.data.DownloadDatabase
import com.example.dwn.data.DownloadStatus
import com.example.dwn.data.MediaPlayStats
import com.example.dwn.download.DownloadManager
import com.example.dwn.player.AudioPlayerManager
import com.example.dwn.player.VideoPlayerActivity
import com.example.dwn.service.NetworkMonitor
import com.example.dwn.ui.FloatingAudioPlayer
import com.example.dwn.ui.screens.DownloadsScreen
import com.example.dwn.ui.screens.EqualizerScreen
import com.example.dwn.ui.screens.SuperEqualizerScreen
import com.example.dwn.ui.screens.HomeScreen
import com.example.dwn.ui.screens.MainHubScreen
import com.example.dwn.ui.screens.PlaylistsScreen
import com.example.dwn.data.DeviceMediaItem
import com.example.dwn.data.UnifiedMediaScanner
import com.example.dwn.ui.screens.SplashScreen
import com.example.dwn.ui.screens.ScreenRecorderScreen
import com.example.dwn.ui.screens.AudioEditorScreen
import com.example.dwn.ui.screens.VideoEditorScreen
import com.example.dwn.ui.screens.AIToolsScreen
import com.example.dwn.ui.screens.AudioSocialScreen
import com.example.dwn.ui.screens.AudioRoomScreen
import com.example.dwn.ui.screens.RemixStudioScreen
import com.example.dwn.ui.screens.ContextModeScreen
import com.example.dwn.ui.screens.MediaVaultScreen
import com.example.dwn.ui.screens.MyRadioScreen
import com.example.dwn.ui.screens.DJStudioScreen
import com.example.dwn.ui.screens.BeatMakerScreen
import com.example.dwn.ui.screens.FMRadioScreen
import com.example.dwn.arena.ArtistsArenaScreen
import com.example.dwn.arena.ArtistProfileScreen
import com.example.dwn.arena.TrackUploadScreen
import com.example.dwn.arena.TrackDetailScreen
import com.example.dwn.arena.ArtistDashboardScreen
import com.example.dwn.arena.RemixChallengeScreen
import com.example.dwn.arena.ChallengesListScreen
import com.example.dwn.arena.ArenaSearchScreen
import com.example.dwn.arena.ArtistOnboardingScreen
import com.example.dwn.arena.ArtistSettingsScreen
import com.example.dwn.ui.components.PermissionRequestDialog
import com.example.dwn.ui.theme.DwnTheme
import com.example.dwn.util.PermissionManager
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "DwnApp"

enum class MediaType(val label: String, val icon: String, val extension: String) {
    MP3("MP3 Audio", "🎵", "mp3"),
    MP4("MP4 Video", "🎬", "mp4")
}

enum class Screen {
    SPLASH, HUB, HOME, DOWNLOADS, EQUALIZER, PLAYLISTS, PODCAST,
    SCREEN_RECORDER, AUDIO_EDITOR, VIDEO_EDITOR, AI_TOOLS,
    AUDIO_SOCIAL, AUDIO_ROOM, REMIX_STUDIO, CONTEXT_MODE, MEDIA_VAULT, RADIO, DJ_STUDIO, BEAT_MAKER,
    ARTISTS_ARENA, ARTIST_PROFILE, TRACK_UPLOAD, TRACK_DETAIL, ARTIST_DASHBOARD,
    REMIX_CHALLENGE, CHALLENGES_LIST, ARENA_SEARCH, ARTIST_ONBOARDING, ARTIST_SETTINGS,
    FM_RADIO
}

class MainActivity : ComponentActivity() {

    private var isYoutubeDLInitialized by mutableStateOf(false)
    private var initStatus by mutableStateOf("Initializing yt-dlp...")

    private lateinit var database: DownloadDatabase
    private lateinit var downloadManager: DownloadManager
    private lateinit var audioPlayerManager: AudioPlayerManager
    private lateinit var networkMonitor: NetworkMonitor

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.filter { it.value }.map { it.key }
        val denied = permissions.entries.filter { !it.value }.map { it.key }

        if (granted.isNotEmpty()) {
            Log.d(TAG, "Permissions granted: $granted")
            // Clear media scanner cache so it re-scans with new permissions
            UnifiedMediaScanner.getInstance(this).clearCache()
            Log.d(TAG, "Cleared media scanner cache after permission grant")
        }

        if (denied.isNotEmpty()) {
            Log.d(TAG, "Permissions denied: $denied")
            // Update UI to show which permissions are missing
            permissionsDenied = denied
        }

        // Check if all essential permissions are now granted
        if (PermissionManager.hasEssentialPermissions(this)) {
            showPermissionDialog = false
        }
    }

    // Track permission state
    private var showPermissionDialog by mutableStateOf(false)
    private var permissionsDenied by mutableStateOf<List<String>>(emptyList())

    // Launcher for video player that handles audio mode continuation
    private val videoPlayerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val continueAsAudio = result.data?.getBooleanExtra(VideoPlayerActivity.RESULT_CONTINUE_AS_AUDIO, false) ?: false
            if (continueAsAudio) {
                val fileName = result.data?.getStringExtra(VideoPlayerActivity.RESULT_FILE_NAME) ?: return@registerForActivityResult
                val position = result.data?.getLongExtra(VideoPlayerActivity.RESULT_POSITION, 0L) ?: 0L
                val videoUriString = result.data?.getStringExtra(VideoPlayerActivity.RESULT_VIDEO_URI)

                if (videoUriString != null) {
                    // Device media file - play from URI
                    val uri = android.net.Uri.parse(videoUriString)
                    audioPlayerManager.playFromUri(
                        context = this,
                        fileId = "device_video_$fileName",
                        fileName = fileName,
                        uri = uri,
                        startPosition = position
                    )
                } else {
                    // Downloaded file - play from file path
                    audioPlayerManager.play(
                        fileId = "video_$fileName",
                        fileName = fileName,
                        isVideo = true,
                        startPosition = position
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(TAG, "onCreate: Starting app")

        // Initialize database and download manager
        database = DownloadDatabase.getDatabase(this)
        downloadManager = DownloadManager.getInstance(this, database.downloadDao())
        audioPlayerManager = AudioPlayerManager(this)

        // Initialize network monitor
        networkMonitor = NetworkMonitor.getInstance(this)
        setupNetworkCallbacks()

        // Check if launched from "Resume Downloads" notification
        val shouldResumeDownloads = intent?.getBooleanExtra(NetworkMonitor.EXTRA_RESUME_DOWNLOADS, false) ?: false

        checkAndRequestPermissions()
        initializeYoutubeDL()

        setContent {
            // Get dark mode setting
            val settingsManager = remember { com.example.dwn.data.SettingsManager.getInstance(this) }
            val settings by settingsManager.settings.collectAsState()

            // Network state
            val isConnected by networkMonitor.isConnected.collectAsState()
            val connectionType by networkMonitor.connectionType.collectAsState()

            DwnTheme(darkTheme = settings.darkMode) {
                var currentScreen by rememberSaveable { mutableStateOf(Screen.SPLASH) }
                var url by remember { mutableStateOf("") }
                var status by remember { mutableStateOf(initStatus) }
                var progress by remember { mutableFloatStateOf(0f) }
                var isDownloading by remember { mutableStateOf(false) }
                var selectedMediaType by remember { mutableStateOf(MediaType.MP3) }
                var currentDownloadId by remember { mutableStateOf<String?>(null) }


                // Show network status banner
                var showNetworkBanner by remember { mutableStateOf(!isConnected) }


                // Permission dialog
                val missingPermissionInfo = remember(showPermissionDialog) {
                    if (showPermissionDialog) {
                        PermissionManager.getMissingPermissionInfo(this@MainActivity)
                    } else {
                        emptyList()
                    }
                }

                // Show permission dialog when needed
                if (showPermissionDialog && missingPermissionInfo.isNotEmpty()) {
                    PermissionRequestDialog(
                        missingPermissions = missingPermissionInfo,
                        onRequestPermissions = {
                            requestAllPermissions()
                        },
                        onDismiss = {
                            showPermissionDialog = false
                        },
                        onOpenSettings = {
                            openAppSettings()
                        }
                    )
                }

                // Download queue
                val downloadQueue by downloadManager.downloadQueue.collectAsState()

                // Handle resume downloads from notification
                LaunchedEffect(shouldResumeDownloads, isYoutubeDLInitialized) {
                    if (shouldResumeDownloads && isYoutubeDLInitialized && isConnected) {
                        // Resume all paused/failed downloads
                        val pausedDownloads = database.downloadDao().getPausedDownloads()
                        if (pausedDownloads.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Resuming ${pausedDownloads.size} download(s)...",
                                Toast.LENGTH_SHORT
                            ).show()
                            pausedDownloads.forEach { download ->
                                downloadManager.addToQueue(
                                    download.url,
                                    MediaType.valueOf(download.mediaType)
                                )
                            }
                        }
                        // Clear the notification
                        networkMonitor.dismissNotification()
                    }
                }

                // Update network status banner
                LaunchedEffect(isConnected) {
                    showNetworkBanner = !isConnected
                    if (!isConnected) {
                        status = "⚠️ No internet connection"
                    } else if (isYoutubeDLInitialized) {
                        status = "✅ Ready! Enter your download link"
                    }
                }

                // Back press handling
                var backPressedTime by remember { mutableLongStateOf(0L) }
                val context = LocalContext.current

                // Download history states
                var searchQuery by remember { mutableStateOf("") }
                val downloads by database.downloadDao().getAllDownloads().collectAsState(initial = emptyList())
                val filteredDownloads = remember(downloads, searchQuery) {
                    if (searchQuery.isEmpty()) downloads
                    else downloads.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                        it.fileName.contains(searchQuery, ignoreCase = true)
                    }
                }

                // Resumable downloads (paused or failed)
                val resumableDownloads = remember(downloads) {
                    downloads.filter {
                        it.status == DownloadStatus.PAUSED || it.status == DownloadStatus.FAILED
                    }
                }

                // Handle back button press
                BackHandler(enabled = currentScreen != Screen.SPLASH) {
                    when (currentScreen) {
                        Screen.DOWNLOADS, Screen.EQUALIZER, Screen.PLAYLISTS, Screen.HOME, Screen.PODCAST,
                        Screen.SCREEN_RECORDER, Screen.AUDIO_EDITOR, Screen.VIDEO_EDITOR, Screen.AI_TOOLS,
                        Screen.AUDIO_SOCIAL, Screen.REMIX_STUDIO, Screen.CONTEXT_MODE, Screen.MEDIA_VAULT, Screen.RADIO, Screen.DJ_STUDIO, Screen.BEAT_MAKER,
                        Screen.ARTISTS_ARENA -> {
                            // Go back to hub from any sub-screen
                            currentScreen = Screen.HUB
                        }
                        Screen.AUDIO_ROOM -> {
                            currentScreen = Screen.AUDIO_SOCIAL
                        }
                        Screen.ARTIST_PROFILE, Screen.TRACK_UPLOAD -> {
                            currentScreen = Screen.ARTISTS_ARENA
                        }
                        Screen.HUB -> {
                            // Double press to exit from hub
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - backPressedTime < 2000) {
                                // Exit app
                                (context as? ComponentActivity)?.finish()
                            } else {
                                backPressedTime = currentTime
                                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                            }
                        }
                        else -> {}
                    }
                }

                // Update status when init completes
                LaunchedEffect(isYoutubeDLInitialized, initStatus) {
                    if (isYoutubeDLInitialized) {
                        status = "✅ Ready! Enter your download link"
                    } else {
                        status = initStatus
                    }
                }

                // Audio player state - available on all screens
                val audioPlayerState by audioPlayerManager.playerState.collectAsState()

                // Show splash screen
                if (currentScreen == Screen.SPLASH) {
                    SplashScreen(
                        onSplashComplete = {
                            currentScreen = Screen.HUB
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        floatingActionButton = {
                            // Show floating player on HUB or HOME screen when audio is playing
                            if ((currentScreen == Screen.HUB || currentScreen == Screen.HOME) && audioPlayerState.currentFileId != null) {
                                FloatingAudioPlayer(
                                    state = audioPlayerState,
                                    onPlayPause = {
                                        if (audioPlayerState.isPlaying) {
                                            audioPlayerManager.pause()
                                        } else {
                                            audioPlayerManager.resume()
                                        }
                                    },
                                    onStop = { audioPlayerManager.stop() },
                                    onSeekForward = { audioPlayerManager.seekForward() },
                                    onSeekBackward = { audioPlayerManager.seekBackward() },
                                    onEqualizerClick = { currentScreen = Screen.EQUALIZER }
                                )
                            }
                        },
                        floatingActionButtonPosition = FabPosition.End
                    ) { _ ->
                    when (currentScreen) {
                        Screen.SPLASH -> {
                            // Splash is handled outside Scaffold
                        }
                        Screen.HUB -> {
                            MainHubScreen(
                                onNavigateToDownloader = { currentScreen = Screen.HOME },
                                onNavigateToPodcast = { currentScreen = Screen.PODCAST },
                                onNavigateToEqualizer = { currentScreen = Screen.EQUALIZER },
                                onNavigateToPlaylists = { currentScreen = Screen.PLAYLISTS },
                                onNavigateToSettings = { /* TODO: Settings screen */ },
                                onNavigateToDownloads = { currentScreen = Screen.DOWNLOADS },
                                onNavigateToScreenRecorder = { currentScreen = Screen.SCREEN_RECORDER },
                                onNavigateToAudioEditor = { currentScreen = Screen.AUDIO_EDITOR },
                                onNavigateToVideoEditor = { currentScreen = Screen.VIDEO_EDITOR },
                                onNavigateToAITools = { currentScreen = Screen.AI_TOOLS },
                                onNavigateToAudioSocial = { currentScreen = Screen.AUDIO_SOCIAL },
                                onNavigateToRemixStudio = { currentScreen = Screen.REMIX_STUDIO },
                                onNavigateToContextMode = { currentScreen = Screen.CONTEXT_MODE },
                                onNavigateToMediaVault = { currentScreen = Screen.MEDIA_VAULT },
                                onNavigateToRadio = { currentScreen = Screen.RADIO },
                                onNavigateToFMRadio = { currentScreen = Screen.FM_RADIO },
                                onNavigateToDJStudio = { currentScreen = Screen.DJ_STUDIO },
                                onNavigateToBeatMaker = { currentScreen = Screen.BEAT_MAKER },
                                onNavigateToArtistsArena = { currentScreen = Screen.ARTISTS_ARENA },
                                totalDownloads = downloads.size,
                                activeDownloads = downloadQueue.size,
                                totalMediaFiles = downloads.filter {
                                    it.status == DownloadStatus.COMPLETED
                                }.size,
                                // Player state and controls
                                isAudioPlaying = audioPlayerState.isPlaying,
                                currentAudioTrack = audioPlayerState.currentFileName,
                                currentAudioArtist = if (audioPlayerState.currentFileId != null) "Now Playing" else "",
                                isVideoPlaying = false, // Video uses separate activity
                                currentVideoTrack = "",
                                onAudioPlayPause = {
                                    if (audioPlayerState.isPlaying) {
                                        audioPlayerManager.pause()
                                    } else {
                                        audioPlayerManager.resume()
                                    }
                                },
                                onVideoPlayPause = { },
                                onOpenAudioPlayer = { currentScreen = Screen.DOWNLOADS },
                                onOpenVideoPlayer = { currentScreen = Screen.DOWNLOADS }
                            )
                        }
                        Screen.PODCAST -> {
                            com.example.dwn.podcast.ui.PodcastScreen(
                                onBack = { currentScreen = Screen.HUB }
                            )
                        }
                        Screen.HOME -> {
                            HomeScreen(
                                modifier = Modifier,
                                url = url,
                                onUrlChange = { url = it },
                                status = status,
                                progress = progress,
                                isDownloading = isDownloading,
                                isReady = isYoutubeDLInitialized,
                                selectedMediaType = selectedMediaType,
                                onMediaTypeChange = { selectedMediaType = it },
                                downloadCount = downloads.size,
                                onViewDownloadsClick = { currentScreen = Screen.DOWNLOADS },
                                onCheckPlaylist = {
                                    // Direct download - no playlist checking
                                    if (url.isNotBlank() && isYoutubeDLInitialized) {
                                        isDownloading = true
                                        status = "🚀 Starting download..."
                                        progress = 0f

                                        lifecycleScope.launch {
                                            val downloadId = downloadManager.smartDownload(
                                                url = url.trim(),
                                                mediaType = selectedMediaType,
                                                onProgress = { prog, msg ->
                                                    progress = prog
                                                    status = msg
                                                },
                                                onComplete = { success, message, item ->
                                                    isDownloading = false
                                                    status = message
                                                    currentDownloadId = null
                                                    if (success) {
                                                        url = ""
                                                    }
                                                }
                                            )
                                            currentDownloadId = downloadId
                                        }
                                    } else if (!isYoutubeDLInitialized) {
                                        status = "⏳ Still initializing... Please wait"
                                    } else {
                                        status = "Please enter a valid URL"
                                    }
                                },
                                onDownloadClick = {
                                    // Direct download (single video)
                                    if (url.isNotBlank() && isYoutubeDLInitialized) {
                                        isDownloading = true
                                        status = "Starting ${selectedMediaType.label} download..."
                                        progress = 0f

                                        lifecycleScope.launch {
                                            val downloadId = downloadManager.startDownload(
                                                url = url.trim(),
                                                mediaType = selectedMediaType,
                                                onProgress = { prog, msg ->
                                                    progress = prog
                                                    status = msg
                                                },
                                                onComplete = { success, message, item ->
                                                    isDownloading = false
                                                    status = message
                                                    currentDownloadId = null
                                                    if (success) {
                                                        url = ""
                                                    }
                                                }
                                            )
                                            currentDownloadId = downloadId
                                        }
                                    }
                                },
                                onPauseClick = {
                                    currentDownloadId?.let { id ->
                                        downloadManager.pauseDownload(id)
                                        isDownloading = false
                                        status = "⏸️ Download paused"
                                    }
                                },
                                onCancelClick = {
                                    currentDownloadId?.let { id ->
                                        downloadManager.cancelDownload(id)
                                        isDownloading = false
                                        status = "❌ Download cancelled"
                                        currentDownloadId = null
                                    }
                                },
                                currentDownloadId = currentDownloadId,
                                // Download queue
                                downloadQueue = downloadQueue,
                                onAddToQueue = {
                                    if (url.isNotBlank() && isYoutubeDLInitialized) {
                                        // Check WiFi-only setting
                                        val (canDownload, errorMsg) = settingsManager.canDownload()
                                        if (canDownload) {
                                            downloadManager.addToQueue(url.trim(), selectedMediaType)
                                            url = ""
                                            status = "✅ Added to download queue"
                                        } else {
                                            // Still add to queue but show warning
                                            downloadManager.addToQueue(url.trim(), selectedMediaType)
                                            url = ""
                                            status = "⏸️ Queued - Waiting for WiFi"
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                onRemoveFromQueue = { id ->
                                    downloadManager.removeFromQueue(id)
                                },
                                onRetryQueued = { id ->
                                    downloadManager.retryQueuedDownload(id)
                                },
                                onClearCompletedQueue = {
                                    downloadManager.clearCompletedFromQueue()
                                },
                                // Resumable downloads
                                resumableDownloads = resumableDownloads,
                                onResumeDownload = { download ->
                                    lifecycleScope.launch {
                                        isDownloading = true
                                        status = "▶️ Resuming download..."
                                        progress = download.progress

                                        downloadManager.resumeDownload(
                                            downloadId = download.id,
                                            onProgress = { prog, msg ->
                                                progress = prog
                                                status = msg
                                            },
                                            onComplete = { success, message, _ ->
                                                isDownloading = false
                                                status = message
                                                currentDownloadId = null
                                            }
                                        )
                                        currentDownloadId = download.id
                                    }
                                },
                                onCancelDownload = { download ->
                                    lifecycleScope.launch {
                                        downloadManager.deleteDownload(download.id)
                                        status = "❌ Download cancelled"
                                    }
                                },
                                onResumeAllDownloads = {
                                    lifecycleScope.launch {
                                        if (resumableDownloads.isNotEmpty()) {
                                            status = "▶️ Resuming ${resumableDownloads.size} download(s)..."
                                            resumableDownloads.forEach { download ->
                                                downloadManager.addToQueue(
                                                    download.url,
                                                    MediaType.valueOf(download.mediaType)
                                                )
                                            }
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Resuming ${resumableDownloads.size} download(s)",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                onCancelAllDownloads = {
                                    lifecycleScope.launch {
                                        if (resumableDownloads.isNotEmpty()) {
                                            val count = resumableDownloads.size
                                            resumableDownloads.forEach { download ->
                                                downloadManager.deleteDownload(download.id)
                                            }
                                            status = "❌ Cancelled $count download(s)"
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Cancelled $count download(s)",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                // Network state
                                isConnected = isConnected
                            )
                        }
                        Screen.DOWNLOADS -> {
                            val context = LocalContext.current

                            DownloadsScreen(
                                downloads = filteredDownloads,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                onResumeClick = { download ->
                                    lifecycleScope.launch {
                                        isDownloading = true
                                        currentScreen = Screen.HOME
                                        downloadManager.resumeDownload(
                                            downloadId = download.id,
                                            onProgress = { prog, msg ->
                                                progress = prog
                                                status = msg
                                            },
                                            onComplete = { success, message, _ ->
                                                isDownloading = false
                                                status = message
                                            }
                                        )
                                    }
                                },
                                onPauseClick = { download ->
                                    downloadManager.pauseDownload(download.id)
                                },
                                onDeleteClick = { download ->
                                    lifecycleScope.launch {
                                        downloadManager.deleteDownload(download.id)
                                    }
                                },
                                onPlayClick = { download ->
                                    if (download.mediaType == "MP3") {
                                        audioPlayerManager.play(download.id, download.fileName)
                                    } else {
                                        // Stop any playing audio first
                                        if (audioPlayerState.isPlaying) {
                                            audioPlayerManager.pause()
                                        }
                                        // Open video player with launcher to handle audio mode return
                                        videoPlayerLauncher.launch(
                                            VideoPlayerActivity.createIntent(context, download.fileName)
                                        )
                                    }
                                },
                                onPlayAsAudioClick = { download ->
                                    // Play MP4 as audio using the audio player (no video)
                                    audioPlayerManager.play(download.id, download.fileName, isVideo = true)
                                },
                                onPlayAllClick = { downloadList: List<com.example.dwn.data.DownloadItem>, startIndex: Int, shuffle: Boolean ->
                                    // Convert downloads to queue items
                                    val queueItems = downloadList.map { item ->
                                        com.example.dwn.player.QueueItem(
                                            id = item.id,
                                            fileName = item.fileName,
                                            isVideo = item.mediaType == "MP4"
                                        )
                                    }
                                    audioPlayerManager.playQueue(queueItems, startIndex, shuffle)
                                },
                                onBackClick = {
                                    currentScreen = Screen.HOME
                                },
                                activeDownloadId = currentDownloadId,
                                audioPlayerState = audioPlayerState,
                                onAudioPlayPause = {
                                    if (audioPlayerState.isPlaying) {
                                        audioPlayerManager.pause()
                                    } else {
                                        audioPlayerManager.resume()
                                    }
                                },
                                onAudioStop = { audioPlayerManager.stop() },
                                onAudioSeekForward = { audioPlayerManager.seekForward() },
                                onAudioSeekBackward = { audioPlayerManager.seekBackward() },
                                onAudioSeek = { fraction ->
                                    val position = (fraction * audioPlayerState.duration).toLong()
                                    audioPlayerManager.seekTo(position)
                                },
                                onToggleRepeat = { audioPlayerManager.toggleRepeatMode() },
                                onToggleShuffle = { audioPlayerManager.toggleShuffle() },
                                onPlayNext = { audioPlayerManager.playNext() },
                                onPlayPrevious = { audioPlayerManager.playPrevious() },
                                onEqualizerClick = { currentScreen = Screen.EQUALIZER },
                                onPlaylistsClick = { currentScreen = Screen.PLAYLISTS },
                                onPlayDeviceMedia = { mediaItem, isAudio ->
                                    // Track play count for device media
                                    lifecycleScope.launch {
                                        database.mediaPlayStatsDao().recordPlay(
                                            id = "device_${if (isAudio) "audio" else "video"}_${mediaItem.id}",
                                            mediaUri = mediaItem.uri.toString(),
                                            mediaType = if (isAudio) "AUDIO" else "VIDEO",
                                            mediaSource = "DEVICE",
                                            title = mediaItem.name,
                                            artist = mediaItem.artist,
                                            album = mediaItem.album,
                                            duration = mediaItem.duration
                                        )
                                    }

                                    if (isAudio) {
                                        // Play audio file from device
                                        audioPlayerManager.playFromUri(
                                            context = context,
                                            fileId = mediaItem.id.toString(),
                                            fileName = mediaItem.name,
                                            uri = mediaItem.uri
                                        )
                                    } else {
                                        // Stop any playing audio first
                                        if (audioPlayerState.isPlaying) {
                                            audioPlayerManager.pause()
                                        }
                                        // Play video file from device using launcher to support audio mode
                                        videoPlayerLauncher.launch(
                                            VideoPlayerActivity.createIntentFromUri(context, mediaItem.uri, mediaItem.name)
                                        )
                                    }
                                },
                                onPlayDeviceMediaAsAudio = { mediaItem ->
                                    // Track play count for device media (played as audio)
                                    lifecycleScope.launch {
                                        database.mediaPlayStatsDao().recordPlay(
                                            id = "device_video_as_audio_${mediaItem.id}",
                                            mediaUri = mediaItem.uri.toString(),
                                            mediaType = "AUDIO", // Treated as audio
                                            mediaSource = "DEVICE",
                                            title = mediaItem.name,
                                            artist = mediaItem.artist,
                                            album = mediaItem.album,
                                            duration = mediaItem.duration
                                        )
                                    }

                                    // Play video file as audio only (no video player)
                                    audioPlayerManager.playFromUri(
                                        context = context,
                                        fileId = "audio_${mediaItem.id}",
                                        fileName = mediaItem.name,
                                        uri = mediaItem.uri
                                    )
                                },
                                onPlayAllDeviceMedia = { mediaList, startIndex, shuffle ->
                                    // Track play for the starting item
                                    if (mediaList.isNotEmpty() && startIndex in mediaList.indices) {
                                        val mediaItem = mediaList[startIndex]
                                        lifecycleScope.launch {
                                            database.mediaPlayStatsDao().recordPlay(
                                                id = "device_audio_${mediaItem.id}",
                                                mediaUri = mediaItem.uri.toString(),
                                                mediaType = "AUDIO",
                                                mediaSource = "DEVICE",
                                                title = mediaItem.name,
                                                artist = mediaItem.artist,
                                                album = mediaItem.album,
                                                duration = mediaItem.duration
                                            )
                                        }
                                    }

                                    // Convert device media to queue items and play
                                    audioPlayerManager.playQueueFromUris(
                                        context = context,
                                        items = mediaList.map { Triple(it.id.toString(), it.name, it.uri) },
                                        startIndex = startIndex,
                                        shuffle = shuffle
                                    )
                                },
                                onRequestMediaPermission = {
                                    // Request media permissions specifically
                                    requestSpecificPermissions(
                                        PermissionManager.getMediaPermissions()
                                    )
                                }
                            )
                        }
                        Screen.EQUALIZER -> {
                            SuperEqualizerScreen(
                                superEqualizer = audioPlayerManager.superEqualizer,
                                onBack = { currentScreen = Screen.HOME }
                            )
                        }
                        Screen.PLAYLISTS -> {
                            // Collect completed downloads and favourites
                            val completedDownloads by database.downloadDao().getCompletedDownloads().collectAsState(initial = emptyList())
                            val favouriteDownloads by database.downloadDao().getMostPlayedDownloads(50).collectAsState(initial = emptyList())
                            val unifiedFavourites by database.mediaPlayStatsDao().getMostPlayed(50).collectAsState(initial = emptyList())

                            PlaylistsScreen(
                                audioPlayerState = audioPlayerState,
                                onBack = { currentScreen = Screen.HUB },
                                onPlayItem = { index ->
                                    audioPlayerManager.playQueueItem(index)
                                },
                                onRemoveItem = { index ->
                                    audioPlayerManager.removeFromQueue(index)
                                },
                                onClearQueue = {
                                    audioPlayerManager.clearQueue()
                                },
                                onToggleShuffle = {
                                    audioPlayerManager.toggleShuffle()
                                },
                                onToggleRepeat = {
                                    audioPlayerManager.toggleRepeatMode()
                                },
                                onPlayPause = {
                                    if (audioPlayerState.isPlaying) {
                                        audioPlayerManager.pause()
                                    } else {
                                        audioPlayerManager.resume()
                                    }
                                },
                                downloadedItems = completedDownloads,
                                favouriteItems = favouriteDownloads,
                                unifiedFavourites = unifiedFavourites,
                                onPlayDownloadedItem = { downloadItem ->
                                    // Track play count using unified MediaPlayStats system
                                    lifecycleScope.launch {
                                        // Update legacy playCount in downloads table
                                        database.downloadDao().incrementPlayCount(downloadItem.id)

                                        // Also record in unified MediaPlayStats table
                                        database.mediaPlayStatsDao().recordPlay(
                                            id = "download_${downloadItem.id}",
                                            mediaUri = downloadItem.filePath,
                                            mediaType = if (downloadItem.mediaType == "MP3") "AUDIO" else "VIDEO",
                                            mediaSource = "DOWNLOAD",
                                            title = downloadItem.title.ifEmpty { downloadItem.fileName },
                                            duration = 0L // Duration not stored in DownloadItem
                                        )
                                    }

                                    // Play the downloaded file
                                    if (downloadItem.mediaType == "MP4") {
                                        // Play video using factory method
                                        val intent = VideoPlayerActivity.createIntent(
                                            context = context,
                                            fileName = downloadItem.fileName
                                        )
                                        videoPlayerLauncher.launch(intent)
                                    } else {
                                        // Play audio
                                        audioPlayerManager.play(
                                            fileId = downloadItem.id,
                                            fileName = downloadItem.fileName,
                                            isVideo = false
                                        )
                                    }
                                },
                                onPlayDeviceMedia = { deviceMedia ->
                                    // Determine if audio based on mimeType
                                    val isAudio = deviceMedia.mimeType.startsWith("audio")

                                    // Track play count for device media
                                    lifecycleScope.launch {
                                        database.mediaPlayStatsDao().recordPlay(
                                            id = "device_${if (isAudio) "audio" else "video"}_${deviceMedia.id}",
                                            mediaUri = deviceMedia.uri.toString(),
                                            mediaType = if (isAudio) "AUDIO" else "VIDEO",
                                            mediaSource = "DEVICE",
                                            title = deviceMedia.name,
                                            artist = deviceMedia.artist,
                                            album = deviceMedia.album,
                                            duration = deviceMedia.duration
                                        )
                                    }

                                    if (isAudio) {
                                        audioPlayerManager.playFromUri(
                                            context = context,
                                            fileId = "device_audio_${deviceMedia.id}",
                                            fileName = deviceMedia.name,
                                            uri = deviceMedia.uri
                                        )
                                    } else {
                                        // Play video from device using factory method
                                        val intent = VideoPlayerActivity.createIntentFromUri(
                                            context = context,
                                            uri = deviceMedia.uri,
                                            fileName = deviceMedia.name
                                        )
                                        videoPlayerLauncher.launch(intent)
                                    }
                                },
                                onPlayFavouriteItem = { mediaPlayStats ->
                                    // Track play count (increment again since user is playing)
                                    lifecycleScope.launch {
                                        database.mediaPlayStatsDao().incrementPlayCount(mediaPlayStats.id)
                                    }

                                    // Play based on source
                                    when {
                                        mediaPlayStats.mediaSource == "DOWNLOAD" -> {
                                            // Find the download item and play it
                                            val downloadId = mediaPlayStats.id.removePrefix("download_")
                                            lifecycleScope.launch {
                                                val downloadItem = database.downloadDao().getDownloadById(downloadId)
                                                if (downloadItem != null) {
                                                    if (downloadItem.mediaType == "MP4") {
                                                        val intent = VideoPlayerActivity.createIntent(
                                                            context = context,
                                                            fileName = downloadItem.fileName
                                                        )
                                                        videoPlayerLauncher.launch(intent)
                                                    } else {
                                                        audioPlayerManager.play(
                                                            fileId = downloadItem.id,
                                                            fileName = downloadItem.fileName,
                                                            isVideo = false
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        mediaPlayStats.mediaSource == "DEVICE" -> {
                                            // Play from URI
                                            val uri = android.net.Uri.parse(mediaPlayStats.mediaUri)
                                            if (mediaPlayStats.mediaType == "VIDEO") {
                                                val intent = VideoPlayerActivity.createIntentFromUri(
                                                    context = context,
                                                    uri = uri,
                                                    fileName = mediaPlayStats.title
                                                )
                                                videoPlayerLauncher.launch(intent)
                                            } else {
                                                audioPlayerManager.playFromUri(
                                                    context = context,
                                                    fileId = mediaPlayStats.id,
                                                    fileName = mediaPlayStats.title,
                                                    uri = uri
                                                )
                                            }
                                        }
                                        else -> {
                                            // Try to play from URI as fallback
                                            val uri = android.net.Uri.parse(mediaPlayStats.mediaUri)
                                            audioPlayerManager.playFromUri(
                                                context = context,
                                                fileId = mediaPlayStats.id,
                                                fileName = mediaPlayStats.title,
                                                uri = uri
                                            )
                                        }
                                    }
                                },
                                onRequestMediaPermission = {
                                    requestSpecificPermissions(
                                        PermissionManager.getMediaPermissions()
                                    )
                                }
                            )
                        }
                        Screen.SCREEN_RECORDER -> {
                            ScreenRecorderScreen(
                                onBack = { currentScreen = Screen.HUB }
                            )
                        }
                        Screen.AUDIO_EDITOR -> {
                            AudioEditorScreen(
                                onBack = { currentScreen = Screen.HUB }
                            )
                        }
                        Screen.VIDEO_EDITOR -> {
                            VideoEditorScreen(
                                onBack = { currentScreen = Screen.HUB }
                            )
                        }
                        Screen.AI_TOOLS -> {
                            AIToolsScreen(
                                onBack = { currentScreen = Screen.HUB }
                            )
                        }
                        Screen.AUDIO_SOCIAL -> {
                            AudioSocialScreen(
                                onBack = { currentScreen = Screen.HUB },
                                onNavigateToRoom = { roomId ->
                                    currentScreen = Screen.AUDIO_ROOM
                                }
                            )
                        }
                        Screen.AUDIO_ROOM -> {
                            AudioRoomScreen(
                                roomId = "mock_room_id",
                                onBack = { currentScreen = Screen.AUDIO_SOCIAL }
                            )
                        }
                        Screen.REMIX_STUDIO -> {
                            RemixStudioScreen(
                                onBack = { currentScreen = Screen.HUB }
                            )
                        }
                        Screen.CONTEXT_MODE -> {
                            ContextModeScreen(
                                onBack = { currentScreen = Screen.HUB }
                            )
                        }
                        Screen.MEDIA_VAULT -> {
                            MediaVaultScreen(
                                onBack = { currentScreen = Screen.HUB }
                            )
                        }
                        Screen.RADIO -> {
                            MyRadioScreen(
                                onBack = { currentScreen = Screen.HUB }
                            )
                        }
                        Screen.DJ_STUDIO -> {
                            DJStudioScreen(
                                onBack = { currentScreen = Screen.HUB }
                            )
                        }
                        Screen.BEAT_MAKER -> {
                            BeatMakerScreen(
                                onNavigateBack = { currentScreen = Screen.HUB }
                            )
                        }
                        Screen.ARTISTS_ARENA -> {
                            var selectedArtistId by remember { mutableStateOf<String?>(null) }
                            var selectedTrackId by remember { mutableStateOf<String?>(null) }
                            ArtistsArenaScreen(
                                onBack = { currentScreen = Screen.HUB },
                                onNavigateToUpload = { currentScreen = Screen.TRACK_UPLOAD },
                                onNavigateToArtistProfile = { artistId ->
                                    selectedArtistId = artistId
                                    currentScreen = Screen.ARTIST_PROFILE
                                },
                                onNavigateToTrackDetail = { trackId ->
                                    selectedTrackId = trackId
                                    currentScreen = Screen.TRACK_DETAIL
                                },
                                onNavigateToRemixStudio = { currentScreen = Screen.REMIX_STUDIO },
                                onPlayTrack = { track ->
                                    // Play the track using audio player
                                    audioPlayerManager.play(
                                        fileId = track.id,
                                        fileName = track.title,
                                        isVideo = false
                                    )
                                }
                            )
                        }
                        Screen.ARTIST_PROFILE -> {
                            var artistIdToShow by remember { mutableStateOf("artist_1") }
                            ArtistProfileScreen(
                                artistId = artistIdToShow,
                                onBack = { currentScreen = Screen.ARTISTS_ARENA },
                                onTrackClick = { trackId ->
                                    currentScreen = Screen.TRACK_DETAIL
                                },
                                onPlayTrack = { track ->
                                    audioPlayerManager.play(
                                        fileId = track.id,
                                        fileName = track.title,
                                        isVideo = false
                                    )
                                },
                                onRemixTrack = { currentScreen = Screen.REMIX_STUDIO }
                            )
                        }
                        Screen.TRACK_UPLOAD -> {
                            TrackUploadScreen(
                                onBack = { currentScreen = Screen.ARTISTS_ARENA },
                                onUploadComplete = { track ->
                                    currentScreen = Screen.ARTISTS_ARENA
                                }
                            )
                        }
                        Screen.TRACK_DETAIL -> {
                            TrackDetailScreen(
                                trackId = "track_1", // TODO: Pass actual track ID
                                onBack = { currentScreen = Screen.ARTISTS_ARENA },
                                onArtistClick = { artistId ->
                                    currentScreen = Screen.ARTIST_PROFILE
                                },
                                onPlayTrack = { track ->
                                    audioPlayerManager.play(
                                        fileId = track.id,
                                        fileName = track.title,
                                        isVideo = false
                                    )
                                },
                                onRemixTrack = { currentScreen = Screen.REMIX_STUDIO },
                                onShareTrack = { /* TODO: Share track */ }
                            )
                        }
                        Screen.ARTIST_DASHBOARD -> {
                            ArtistDashboardScreen(
                                onBack = { currentScreen = Screen.ARTISTS_ARENA },
                                onUploadTrack = { currentScreen = Screen.TRACK_UPLOAD },
                                onViewTrack = { trackId ->
                                    currentScreen = Screen.TRACK_DETAIL
                                },
                                onViewAnalytics = { /* Analytics detail */ },
                                onManageProfile = { currentScreen = Screen.ARTIST_SETTINGS }
                            )
                        }
                        Screen.REMIX_CHALLENGE -> {
                            RemixChallengeScreen(
                                challengeId = "challenge_1", // TODO: Pass actual challenge ID
                                onBack = { currentScreen = Screen.CHALLENGES_LIST },
                                onPlayOriginal = { track ->
                                    audioPlayerManager.play(
                                        fileId = track.id,
                                        fileName = track.title,
                                        isVideo = false
                                    )
                                },
                                onSubmitRemix = { currentScreen = Screen.REMIX_STUDIO },
                                onViewSubmission = { trackId ->
                                    currentScreen = Screen.TRACK_DETAIL
                                },
                                onArtistClick = { artistId ->
                                    currentScreen = Screen.ARTIST_PROFILE
                                }
                            )
                        }
                        Screen.CHALLENGES_LIST -> {
                            ChallengesListScreen(
                                onBack = { currentScreen = Screen.ARTISTS_ARENA },
                                onChallengeClick = { challengeId ->
                                    currentScreen = Screen.REMIX_CHALLENGE
                                },
                                onCreateChallenge = { /* Create new challenge */ }
                            )
                        }
                        Screen.ARENA_SEARCH -> {
                            ArenaSearchScreen(
                                onBack = { currentScreen = Screen.ARTISTS_ARENA },
                                onTrackClick = { trackId ->
                                    currentScreen = Screen.TRACK_DETAIL
                                },
                                onArtistClick = { artistId ->
                                    currentScreen = Screen.ARTIST_PROFILE
                                },
                                onPlayTrack = { track ->
                                    audioPlayerManager.play(
                                        fileId = track.id,
                                        fileName = track.title,
                                        isVideo = false
                                    )
                                }
                            )
                        }
                        Screen.ARTIST_ONBOARDING -> {
                            ArtistOnboardingScreen(
                                onBack = { currentScreen = Screen.ARTISTS_ARENA },
                                onComplete = { currentScreen = Screen.ARTIST_DASHBOARD }
                            )
                        }
                        Screen.ARTIST_SETTINGS -> {
                            ArtistSettingsScreen(
                                onBack = { currentScreen = Screen.ARTIST_DASHBOARD },
                                onEditProfile = { /* Edit profile */ },
                                onManageRights = { /* Manage rights */ },
                                onPrivacy = { /* Privacy settings */ },
                                onNotifications = { /* Notification settings */ },
                                onLogout = { currentScreen = Screen.ARTISTS_ARENA }
                            )
                        }
                        Screen.FM_RADIO -> {
                            FMRadioScreen(
                                onBack = { currentScreen = Screen.HUB }
                            )
                        }
                    }
                }
                } // End of else block for splash screen
            }
        }
    }

    private fun checkAndRequestPermissions() {
        // On Android 11+, check if MANAGE_EXTERNAL_STORAGE is needed
        if (PermissionManager.needsManageStoragePermission() && !PermissionManager.hasManageStoragePermission()) {
            Log.d(TAG, "MANAGE_EXTERNAL_STORAGE not granted - requesting...")
            PermissionManager.openManageStorageSettings(this)
            return
        } else if (PermissionManager.needsManageStoragePermission()) {
            Log.d(TAG, "MANAGE_EXTERNAL_STORAGE already granted")
        }

        // Get all missing permissions
        val missingPermissions = PermissionManager.getMissingPermissions(
            this,
            PermissionManager.getAllPermissions()
        )

        if (missingPermissions.isNotEmpty()) {
            Log.d(TAG, "Missing permissions: $missingPermissions")
            // Show permission dialog
            showPermissionDialog = true
        } else {
            Log.d(TAG, "All permissions already granted")
            showPermissionDialog = false
        }
    }

    private fun requestManageStoragePermission() {
        PermissionManager.openManageStorageSettings(this)
    }

    private fun requestAllPermissions() {
        val missingPermissions = PermissionManager.getMissingPermissions(
            this,
            PermissionManager.getAllPermissions()
        )

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun requestSpecificPermissions(permissions: List<String>) {
        Log.d(TAG, "requestSpecificPermissions called with: $permissions")
        val missing = PermissionManager.getMissingPermissions(this, permissions)
        Log.d(TAG, "Missing permissions to request: $missing")
        if (missing.isNotEmpty()) {
            Log.d(TAG, "Launching permission request for: ${missing.toTypedArray().contentToString()}")
            requestPermissionLauncher.launch(missing.toTypedArray())
        } else {
            Log.d(TAG, "All permissions already granted")
        }
    }

    private fun openAppSettings() {
        PermissionManager.openAppSettings(this)
    }

    private fun initializeYoutubeDL() {
        lifecycleScope.launch(Dispatchers.IO) {
            var retryCount = 0
            val maxRetries = 3

            while (retryCount < maxRetries) {
                try {
                    Log.d(TAG, "Initializing YoutubeDL... (attempt ${retryCount + 1})")
                    withContext(Dispatchers.Main) {
                        initStatus = if (retryCount > 0) {
                            "Retrying initialization (${retryCount + 1}/$maxRetries)..."
                        } else {
                            "Initializing yt-dlp..."
                        }
                    }

                    val appDir = applicationContext.filesDir
                    val freeSpace = appDir.freeSpace / (1024 * 1024)
                    Log.d(TAG, "Available storage: ${freeSpace}MB")

                    if (freeSpace < 100) {
                        throw Exception("Not enough storage space. Need at least 100MB, have ${freeSpace}MB")
                    }

                    YoutubeDL.getInstance().init(this@MainActivity)
                    Log.d(TAG, "YoutubeDL initialized successfully")

                    withContext(Dispatchers.Main) {
                        initStatus = "Initializing FFmpeg..."
                    }

                    try {
                        com.yausername.ffmpeg.FFmpeg.getInstance().init(this@MainActivity)
                        Log.d(TAG, "FFmpeg initialized successfully")
                    } catch (e: Exception) {
                        Log.w(TAG, "FFmpeg init failed (non-fatal): ${e.message}")
                    }

                    withContext(Dispatchers.Main) {
                        initStatus = "Checking for updates..."
                    }

                    try {
                        YoutubeDL.getInstance().updateYoutubeDL(this@MainActivity)
                        Log.d(TAG, "YoutubeDL updated")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to update yt-dlp (using bundled version): ${e.message}")
                    }

                    withContext(Dispatchers.Main) {
                        isYoutubeDLInitialized = true
                        Log.d(TAG, "All initialization complete")
                    }
                    return@launch

                } catch (e: Exception) {
                    retryCount++
                    Log.e(TAG, "Initialization attempt $retryCount failed", e)

                    if (retryCount >= maxRetries) {
                        withContext(Dispatchers.Main) {
                            initStatus = "❌ Init failed: ${e.message}"
                            Toast.makeText(
                                this@MainActivity,
                                "Failed to initialize after $maxRetries attempts: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Log.d(TAG, "Waiting 2 seconds before retry...")
                        kotlinx.coroutines.delay(2000)
                    }
                }
            }
        }
    }

    private fun setupNetworkCallbacks() {
        // NetworkMonitor now handles auto-resume internally
        // These callbacks are just for UI updates if needed
        networkMonitor.onConnectionRestored = {
            Log.d(TAG, "Connection restored - NetworkMonitor will auto-resume downloads")
        }

        networkMonitor.onConnectionLost = {
            Log.d(TAG, "Connection lost - downloads paused")
        }

        networkMonitor.onWifiConnected = {
            Log.d(TAG, "WiFi connected")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayerManager.release()
        // Don't unregister network monitor - it's a singleton and should persist
    }
}
