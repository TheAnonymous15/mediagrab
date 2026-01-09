package com.example.dwn.player

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun AudioPlayerControls(
    state: AudioPlayerState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.currentFileId == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // File name
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.currentFileName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onStop, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Progress slider
            Slider(
                value = if (state.duration > 0) state.currentPosition.toFloat() / state.duration.toFloat() else 0f,
                onValueChange = { onSeek(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Time display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(state.currentPosition),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatTime(state.duration),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSeekBackward) {
                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s")
                }

                Spacer(modifier = Modifier.width(16.dp))

                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(onClick = onSeekForward) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s")
                }
            }
        }
    }
}

@Composable
fun MiniAudioPlayer(
    state: AudioPlayerState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.currentFileId == null) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = state.currentFileName,
                            style = MaterialTheme.typography.bodyMedium,
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
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play"
                        )
                    }
                    IconButton(onClick = onStop) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }
                }
            }
        }
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

