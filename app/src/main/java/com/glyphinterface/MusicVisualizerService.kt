package com.glyphinterface

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class MusicVisualizerService : Service() {
    private var musicVisualizer: MusicVisualizer? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())

    private val progressRunnable = object : Runnable {
        override fun run() {
            activeController?.let { controller ->
                val state = controller.playbackState
                val metadata = controller.metadata
                if (state != null && metadata != null) {
                    val duration = metadata.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION)
                    val position = state.position
                    if (duration > 0 && position >= 0) {
                        val progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        RootUtils.updateMusicProgress(progress)
                    }
                }
            }
            if (isRunning) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        musicVisualizer = MusicVisualizer(this)
        createNotificationChannel()
        setupMediaSessionListener()
    }

    private fun setupMediaSessionListener() {
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val component = ComponentName(this, NotificationService::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(component)
            if (!controllers.isNullOrEmpty()) {
                activeController = controllers[0]
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaSessionManager query notice: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d(TAG, "Service started, initializing 3-band visualizer")

            musicVisualizer?.start { bass, vocal, instrument ->
                RootUtils.updateMusicVisualizer3Part(bass, vocal, instrument)
            }

            handler.post(progressRunnable)
        }
        return START_STICKY
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Glyph Music Visualizer")
            .setContentText("Live Bass, Vocal & Instrument sync active")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Glyph Visualizer",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        handler.removeCallbacks(progressRunnable)
        musicVisualizer?.stop()
        RootUtils.clearAllLeds()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "MusicVisualizerService"
        const val CHANNEL_ID = "glyph_visualizer_channel"
        const val NOTIFICATION_ID = 1002
        var isRunning = false
    }
}
