package com.example.dwn.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.dwn.ui.theme.DwnTheme
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.abs

private const val TAG = "VideoPlayerActivity"

class VideoPlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private var fileName: String = ""
    private var audioOnlyMode: Boolean = false
    private var videoUri: android.net.Uri? = null

    companion object {
        private const val EXTRA_FILE_NAME = "file_name"
        private const val EXTRA_AUDIO_ONLY = "audio_only"
        private const val EXTRA_VIDEO_URI = "video_uri"

        // Result extras for audio mode continuation
        const val RESULT_CONTINUE_AS_AUDIO = "continue_as_audio"
        const val RESULT_FILE_NAME = "result_file_name"
        const val RESULT_POSITION = "result_position"
        const val RESULT_VIDEO_URI = "result_video_uri"

        fun createIntent(context: Context, fileName: String, audioOnly: Boolean = false): Intent {
            return Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_FILE_NAME, fileName)
                putExtra(EXTRA_AUDIO_ONLY, audioOnly)
            }
        }

        fun createIntentFromUri(context: Context, uri: android.net.Uri, fileName: String): Intent {
            return Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_FILE_NAME, fileName)
                putExtra(EXTRA_VIDEO_URI, uri.toString())
                putExtra(EXTRA_AUDIO_ONLY, false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: ""
        audioOnlyMode = intent.getBooleanExtra(EXTRA_AUDIO_ONLY, false)
        val uriString = intent.getStringExtra(EXTRA_VIDEO_URI)
        videoUri = uriString?.let { android.net.Uri.parse(it) }

        if (fileName.isEmpty() && videoUri == null) {
            Toast.makeText(this, "No video file specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            DwnTheme {
                VideoPlayerScreen(
                    fileName = fileName,
                    videoUri = videoUri,
                    audioOnlyMode = audioOnlyMode,
                    onBackClick = { finish() },
                    onEnterPiP = { enterPipMode() },
                    onToggleFullscreen = { toggleFullscreen() },
                    onToggleAudioMode = { audioOnlyMode = !audioOnlyMode },
                    onSwitchToAudioPlayer = { currentPos ->
                        // Set result to indicate we want to continue as audio
                        val resultIntent = android.content.Intent().apply {
                            putExtra(RESULT_CONTINUE_AS_AUDIO, true)
                            putExtra(RESULT_FILE_NAME, fileName)
                            putExtra(RESULT_POSITION, currentPos)
                            // Include URI for device media files
                            videoUri?.let { putExtra(RESULT_VIDEO_URI, it.toString()) }
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    activity = this
                )
            }
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    private fun toggleFullscreen() {
        requestedOrientation = if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (player?.isPlaying == true) {
                enterPipMode()
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    fileName: String,
    videoUri: android.net.Uri? = null,
    audioOnlyMode: Boolean,
    onBackClick: () -> Unit,
    onEnterPiP: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleAudioMode: () -> Unit,
    onSwitchToAudioPlayer: (Long) -> Unit = {},
    activity: ComponentActivity
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var volume by remember { mutableFloatStateOf(0.5f) }
    var brightness by remember { mutableFloatStateOf(0.5f) }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var showSeekIndicator by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableIntStateOf(0) } // -1 = rewind, 1 = forward
    var seekAmount by remember { mutableIntStateOf(0) }
    var isAudioOnly by remember { mutableStateOf(audioOnlyMode) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    LaunchedEffect(Unit) {
        val layoutParams = activity.window.attributes
        brightness = if (layoutParams.screenBrightness < 0) 0.5f else layoutParams.screenBrightness
        volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
    }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            // Determine media source - either from URI or file path
            val mediaItem = if (videoUri != null) {
                MediaItem.fromUri(videoUri)
            } else {
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val file = File(moviesDir, fileName)
                if (file.exists()) {
                    MediaItem.fromUri(file.absolutePath)
                } else {
                    null
                }
            }

            if (mediaItem != null) {
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        duration = this@apply.duration
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            })
        }
    }

    LaunchedEffect(player) {
        while (true) {
            currentPosition = player.currentPosition
            delay(200)
        }
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Hide seek indicator after delay
    LaunchedEffect(showSeekIndicator) {
        if (showSeekIndicator) {
            delay(800)
            showSeekIndicator = false
            seekAmount = 0
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { offset ->
                        val isLeftSide = offset.x < size.width / 2
                        if (isLeftSide) {
                            player.seekTo(player.currentPosition - 10000)
                            seekDirection = -1
                            seekAmount = 10
                        } else {
                            player.seekTo(player.currentPosition + 10000)
                            seekDirection = 1
                            seekAmount = 10
                        }
                        showSeekIndicator = true
                    }
                )
            }
            .pointerInput(Unit) {
                var totalDragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        showSeekIndicator = true
                    },
                    onDragEnd = {
                        // Apply the seek
                        val seekMs = (totalDragX / size.width * duration * 0.5f).toLong()
                        if (abs(seekMs) > 1000) {
                            player.seekTo((player.currentPosition + seekMs).coerceIn(0, duration))
                        }
                        showSeekIndicator = false
                        seekAmount = 0
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                        val seekSeconds = (totalDragX / size.width * duration / 1000 * 0.5f).toInt()
                        seekDirection = if (seekSeconds >= 0) 1 else -1
                        seekAmount = abs(seekSeconds)
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val isLeftSide = offset.x < size.width / 2
                        if (isLeftSide) {
                            showBrightnessIndicator = true
                        } else {
                            showVolumeIndicator = true
                        }
                    },
                    onDragEnd = {
                        showVolumeIndicator = false
                        showBrightnessIndicator = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        val isLeftSide = change.position.x < size.width / 2
                        val delta = -dragAmount / size.height

                        if (isLeftSide) {
                            brightness = (brightness + delta).coerceIn(0f, 1f)
                            val layoutParams = activity.window.attributes
                            layoutParams.screenBrightness = brightness
                            activity.window.attributes = layoutParams
                        } else {
                            volume = (volume + delta).coerceIn(0f, 1f)
                            val newVolume = (volume * maxVolume).toInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                        }
                    }
                )
            }
    ) {
        // Video Player (hidden in audio-only mode)
        if (!isAudioOnly) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Audio-only mode UI
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Audio Mode",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = fileName,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Seek Indicator (center)
        AnimatedVisibility(
            visible = showSeekIndicator && seekAmount > 0,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (seekDirection < 0) Icons.Default.FastRewind else Icons.Default.FastForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "${seekAmount}s",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Volume Indicator
        AnimatedVisibility(
            visible = showVolumeIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = when {
                        volume == 0f -> Icons.AutoMirrored.Filled.VolumeOff
                        volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = "Volume",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(volume * 100).toInt()}%",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Brightness Indicator
        AnimatedVisibility(
            visible = showBrightnessIndicator,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = if (brightness < 0.5f) Icons.Default.BrightnessLow else Icons.Default.BrightnessHigh,
                    contentDescription = "Brightness",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(brightness * 100).toInt()}%",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = fileName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        maxLines = 1
                    )

                    Row {
                        // Audio-only toggle - switches to main audio player
                        IconButton(onClick = {
                            if (!isAudioOnly) {
                                // Switch to audio-only mode: exit video player and continue in audio player
                                onSwitchToAudioPlayer(player.currentPosition)
                            } else {
                                // Switch back to video mode
                                isAudioOnly = false
                                onToggleAudioMode()
                            }
                        }) {
                            Icon(
                                imageVector = if (isAudioOnly) Icons.Default.Videocam else Icons.Default.MusicNote,
                                contentDescription = if (isAudioOnly) "Switch to Video" else "Continue as Audio",
                                tint = if (isAudioOnly) Color(0xFF4CAF50) else Color.White
                            )
                        }
                        IconButton(onClick = onEnterPiP) {
                            Icon(
                                Icons.Default.PictureInPicture,
                                contentDescription = "Picture in Picture",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = onToggleFullscreen) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { player.seekTo(player.currentPosition - 10000) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = {
                            if (isPlaying) player.pause() else player.play()
                        },
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    IconButton(
                        onClick = { player.seekTo(player.currentPosition + 10000) },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Bottom controls - Styled progress bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Time display above progress
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(currentPosition),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatDuration(duration),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }

                    // Styled thin progress bar
                    Column {
                        val progress = if (duration > 0) {
                            currentPosition.toFloat() / duration.toFloat()
                        } else 0f

                        // Custom thin track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        ) {
                            // Active progress with gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFFE91E63),
                                                Color(0xFFFF5722)
                                            )
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Invisible slider for touch interaction
                        Slider(
                            value = progress,
                            onValueChange = { player.seekTo((it * duration).toLong()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            )
                        )
                    }

                    // Swipe hint
                    Text(
                        text = "↔ Swipe to seek • ↕ Left: Brightness, Right: Volume",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

