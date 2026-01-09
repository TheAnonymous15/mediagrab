package com.example.dwn.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.dwn.MainActivity
import com.example.dwn.R

class MediaNotificationManager(private val context: Context) {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "media_playback_channel"
        const val ACTION_PLAY = "com.example.dwn.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.dwn.ACTION_PAUSE"
        const val ACTION_STOP = "com.example.dwn.ACTION_STOP"
        const val ACTION_PREVIOUS = "com.example.dwn.ACTION_PREVIOUS"
        const val ACTION_NEXT = "com.example.dwn.ACTION_NEXT"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var mediaSession: MediaSessionCompat? = null

    init {
        createNotificationChannel()
        setupMediaSession()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows media playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(context, "DwnMediaSession").apply {
            @Suppress("DEPRECATION")
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }
    }

    fun showNotification(
        title: String,
        isPlaying: Boolean,
        currentPosition: Long,
        duration: Long,
        onPlay: () -> Unit,
        onPause: () -> Unit,
        onStop: () -> Unit,
        onSeekForward: () -> Unit,
        onSeekBackward: () -> Unit
    ) {
        // Update media session
        mediaSession?.let { session ->
            // Set metadata
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "MediaGrab")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .build()
            session.setMetadata(metadata)

            // Set playback state
            val state = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_FAST_FORWARD or
                    PlaybackStateCompat.ACTION_REWIND
                )
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    currentPosition,
                    1f
                )
                .build()
            session.setPlaybackState(state)

            // Set callback for media buttons
            session.setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    onPlay()
                }

                override fun onPause() {
                    onPause()
                }

                override fun onStop() {
                    onStop()
                }

                override fun onFastForward() {
                    onSeekForward()
                }

                override fun onRewind() {
                    onSeekBackward()
                }

                override fun onSeekTo(pos: Long) {
                    // Handle seek if needed
                }
            })
        }

        // Create intents for notification actions
        val playPauseIntent = createActionIntent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY)
        val stopIntent = createActionIntent(ACTION_STOP)
        val prevIntent = createActionIntent(ACTION_PREVIOUS)
        val nextIntent = createActionIntent(ACTION_NEXT)

        // Open app intent
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "Playing" else "Paused")
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            // Add actions
            .addAction(
                R.drawable.ic_replay_10,
                "Rewind",
                prevIntent
            )
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(
                R.drawable.ic_forward_10,
                "Forward",
                nextIntent
            )
            .addAction(
                R.drawable.ic_stop,
                "Stop",
                stopIntent
            )
            // Media style
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2) // Show rewind, play/pause, forward in compact view
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun hideNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun release() {
        hideNotification()
        mediaSession?.release()
        mediaSession = null
    }
}

