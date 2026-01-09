package com.example.dwn.ui

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.dwn.data.DownloadItem
import com.example.dwn.data.DownloadStatus
import com.example.dwn.player.AudioPlayerState
import com.example.dwn.player.VideoPlayerActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadHistoryScreen(
    downloads: List<DownloadItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onResumeClick: (DownloadItem) -> Unit,
    onPauseClick: (DownloadItem) -> Unit,
    onDeleteClick: (DownloadItem) -> Unit,
    onPlayClick: (DownloadItem) -> Unit,
    onPlayAsAudioClick: (DownloadItem) -> Unit,
    onBackClick: () -> Unit,
    activeDownloadId: String?,
    audioPlayerState: AudioPlayerState,
    onAudioPlayPause: () -> Unit,
    onAudioStop: () -> Unit,
    onAudioSeekForward: () -> Unit,
    onAudioSeekBackward: () -> Unit,
    onAudioSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mini Audio Player at top
            AnimatedVisibility(
                visible = audioPlayerState.currentFileId != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                MiniAudioPlayerBar(
                    state = audioPlayerState,
                    onPlayPause = onAudioPlayPause,
                    onStop = onAudioStop,
                    onSeekForward = onAudioSeekForward,
                    onSeekBackward = onAudioSeekBackward,
                    onSeek = onAudioSeek
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search downloads...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "No downloads yet" else "No results found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(downloads, key = { it.id }) { download ->
                        DownloadItemCard(
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
                            onPlayAsAudioClick = { onPlayAsAudioClick(download) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniAudioPlayerBar(
    state: AudioPlayerState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeek: (Float) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 8.dp
    ) {
        Column {
            // Progress bar
            LinearProgressIndicator(
                progress = { if (state.duration > 0) state.currentPosition.toFloat() / state.duration.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.currentFileName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${formatTime(state.currentPosition)} / ${formatTime(state.duration)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onSeekBackward, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Replay10, contentDescription = "Rewind", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onPlayPause, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play"
                        )
                    }
                    IconButton(onClick = onSeekForward, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Forward10, contentDescription = "Forward", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onStop, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadItemCard(
    download: DownloadItem,
    isActive: Boolean,
    isPlaying: Boolean,
    audioProgress: Float,
    onResumeClick: () -> Unit,
    onPauseClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlayClick: () -> Unit,
    onPlayAsAudioClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPlayMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                download.status == DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                download.status == DownloadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                download.status == DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Media type icon
                Icon(
                    imageVector = if (download.mediaType == "MP3") Icons.Default.MusicNote else Icons.Default.Movie,
                    contentDescription = download.mediaType,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Title and info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title.ifEmpty { download.fileName.ifEmpty { "Downloading..." } },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(status = download.status)
                        Text(
                            text = download.mediaType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        download.completedAt?.let {
                            Text(
                                text = formatDate(it),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action buttons
                Row {
                    // Play button for completed downloads
                    if (download.status == DownloadStatus.COMPLETED) {
                        if (download.mediaType == "MP4") {
                            // MP4 has dropdown menu for video/audio options
                            Box {
                                IconButton(onClick = { showPlayMenu = true }) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play options",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DropdownMenu(
                                    expanded = showPlayMenu,
                                    onDismissRequest = { showPlayMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("▶️ Play Video") },
                                        onClick = {
                                            showPlayMenu = false
                                            onPlayClick()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Videocam, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🎵 Play as Audio") },
                                        onClick = {
                                            showPlayMenu = false
                                            onPlayAsAudioClick()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.MusicNote, contentDescription = null)
                                        }
                                    )
                                }
                            }
                        } else {
                            // MP3 - simple play button
                            IconButton(onClick = onPlayClick) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    when (download.status) {
                        DownloadStatus.DOWNLOADING -> {
                            IconButton(onClick = onPauseClick) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause")
                            }
                        }
                        DownloadStatus.PAUSED, DownloadStatus.FAILED -> {
                            IconButton(onClick = onResumeClick) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                            }
                        }
                        else -> {}
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }

            // Progress bar for active downloads
            if (download.status == DownloadStatus.DOWNLOADING || download.status == DownloadStatus.PAUSED) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { download.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${(download.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Audio playback progress for playing items
            if (isPlaying && audioProgress > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { audioProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Error message
            if (download.status == DownloadStatus.FAILED && !download.errorMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = download.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Download") },
            text = { Text("Are you sure you want to delete this download?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatusChip(status: DownloadStatus) {
    val (text, color) = when (status) {
        DownloadStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.outline
        DownloadStatus.DOWNLOADING -> "Downloading" to MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> "Paused" to MaterialTheme.colorScheme.secondary
        DownloadStatus.COMPLETED -> "Completed" to MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.outline
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun openDownloadedFile(context: Context, download: DownloadItem) {
    try {
        val baseDir = if (download.mediaType == "MP3") {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        }

        val file = File(baseDir, download.fileName)

        if (!file.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                uri,
                if (download.mediaType == "MP3") "audio/*" else "video/*"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

