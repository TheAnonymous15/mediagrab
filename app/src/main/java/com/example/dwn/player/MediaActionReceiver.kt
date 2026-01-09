package com.example.dwn.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MediaActionReceiver : BroadcastReceiver() {

    companion object {
        var onPlayCallback: (() -> Unit)? = null
        var onPauseCallback: (() -> Unit)? = null
        var onStopCallback: (() -> Unit)? = null
        var onPreviousCallback: (() -> Unit)? = null
        var onNextCallback: (() -> Unit)? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            MediaNotificationManager.ACTION_PLAY -> onPlayCallback?.invoke()
            MediaNotificationManager.ACTION_PAUSE -> onPauseCallback?.invoke()
            MediaNotificationManager.ACTION_STOP -> onStopCallback?.invoke()
            MediaNotificationManager.ACTION_PREVIOUS -> onPreviousCallback?.invoke()
            MediaNotificationManager.ACTION_NEXT -> onNextCallback?.invoke()
        }
    }
}

