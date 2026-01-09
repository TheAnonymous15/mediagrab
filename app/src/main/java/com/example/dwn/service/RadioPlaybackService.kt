package com.example.dwn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.dwn.MainActivity
import com.example.dwn.R
import kotlinx.coroutines.*

/**
 * Background service for Online Radio playback with notification controls
 */
class RadioPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "radio_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.dwn.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.dwn.ACTION_PAUSE"
        const val ACTION_STOP = "com.example.dwn.ACTION_STOP"
        const val ACTION_NEXT = "com.example.dwn.ACTION_NEXT"
        const val ACTION_PREV = "com.example.dwn.ACTION_PREV"

        const val EXTRA_STATION_URL = "station_url"
        const val EXTRA_STATION_NAME = "station_name"
        const val EXTRA_STATION_COUNTRY = "station_country"
    }

    private val binder = RadioBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private var currentStationUrl: String = ""
    private var currentStationName: String = "Online Radio"
    private var currentStationCountry: String = ""
    private var isPlaying = false
    private var isPrepared = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    // Callback for UI updates
    var playbackCallback: PlaybackCallback? = null

    interface PlaybackCallback {
        fun onPlaybackStateChanged(playing: Boolean)
        fun onStationChanged(name: String, country: String)
        fun onError(message: String)
    }

    // Binder for activity connection
    inner class RadioBinder : Binder() {
        fun getService(): RadioPlaybackService = this@RadioPlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initMediaSession()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        registerBecomingNoisyReceiver()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val url = intent.getStringExtra(EXTRA_STATION_URL) ?: currentStationUrl
                val name = intent.getStringExtra(EXTRA_STATION_NAME) ?: currentStationName
                val country = intent.getStringExtra(EXTRA_STATION_COUNTRY) ?: currentStationCountry

                if (url.isNotBlank()) {
                    playStation(url, name, country)
                } else if (isPrepared) {
                    resume()
                }
            }
            ACTION_PAUSE -> pause()
            ACTION_STOP -> {
                stop()
                stopSelf()
            }
            ACTION_NEXT -> playbackCallback?.let { /* Trigger next from UI */ }
            ACTION_PREV -> playbackCallback?.let { /* Trigger prev from UI */ }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Radio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Online Radio playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "RadioPlaybackService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    resume()
                }

                override fun onPause() {
                    pause()
                }

                override fun onStop() {
                    stop()
                    stopSelf()
                }

                override fun onSkipToNext() {
                    // Handle next station
                }

                override fun onSkipToPrevious() {
                    // Handle previous station
                }
            })

            isActive = true
        }
    }

    fun playStation(url: String, name: String, country: String) {
        serviceScope.launch {
            try {
                currentStationUrl = url
                currentStationName = name
                currentStationCountry = country

                // Request audio focus
                if (!requestAudioFocus()) {
                    playbackCallback?.onError("Could not get audio focus")
                    return@launch
                }

                // Release existing player
                mediaPlayer?.release()

                // Create new player
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )

                    setOnPreparedListener {
                        this@RadioPlaybackService.isPrepared = true
                        start()
                        this@RadioPlaybackService.isPlaying = true
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                        updateNotification()
                        playbackCallback?.onPlaybackStateChanged(true)
                        playbackCallback?.onStationChanged(name, country)
                    }

                    setOnErrorListener { _, what, extra ->
                        playbackCallback?.onError("Playback error: $what, $extra")
                        this@RadioPlaybackService.isPlaying = false
                        this@RadioPlaybackService.isPrepared = false
                        updatePlaybackState(PlaybackStateCompat.STATE_ERROR)
                        true
                    }

                    setOnCompletionListener {
                        this@RadioPlaybackService.isPlaying = false
                        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                        playbackCallback?.onPlaybackStateChanged(false)
                    }

                    setDataSource(url)
                    prepareAsync()
                }

                // Start foreground with notification
                startForeground(NOTIFICATION_ID, createNotification())

            } catch (e: Exception) {
                playbackCallback?.onError("Failed to play: ${e.message}")
            }
        }
    }

    fun resume() {
        if (isPrepared && !isPlaying) {
            mediaPlayer?.start()
            isPlaying = true
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            updateNotification()
            playbackCallback?.onPlaybackStateChanged(true)
        }
    }

    fun pause() {
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            updateNotification()
            playbackCallback?.onPlaybackStateChanged(false)
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        isPrepared = false
        abandonAudioFocus()
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
        playbackCallback?.onPlaybackStateChanged(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun isPlaying(): Boolean = isPlaying

    fun getCurrentStation(): Pair<String, String> = Pair(currentStationName, currentStationCountry)

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }
                .build()

            audioManager?.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                { focusChange -> handleAudioFocusChange(focusChange) },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1f, 1f)
                if (!isPlaying && isPrepared) {
                    resume()
                }
            }
        }
    }

    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, 0, 1f)
            .build()

        mediaSession?.setPlaybackState(playbackState)

        // Update metadata
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentStationName)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentStationCountry)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Online Radio")
            .build()

        mediaSession?.setMetadata(metadata)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action intents
        val playIntent = PendingIntent.getService(
            this, 0,
            Intent(this, RadioPlaybackService::class.java).apply { action = ACTION_PLAY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, RadioPlaybackService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, RadioPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseAction = if (isPlaying) pauseIntent else playIntent
        val playPauseText = if (isPlaying) "Pause" else "Play"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentStationName)
            .setContentText(if (currentStationCountry.isNotBlank()) "📻 $currentStationCountry" else "Online Radio")
            .setSubText(if (isPlaying) "Playing" else "Paused")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", playIntent) // Placeholder
            .addAction(playPauseIcon, playPauseText, playPauseAction)
            .addAction(android.R.drawable.ic_media_next, "Next", playIntent) // Placeholder
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    // Handle headphone disconnect
    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    private fun registerBecomingNoisyReceiver() {
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(becomingNoisyReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaPlayer?.release()
        mediaSession?.release()
        abandonAudioFocus()
        try {
            unregisterReceiver(becomingNoisyReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }
}

