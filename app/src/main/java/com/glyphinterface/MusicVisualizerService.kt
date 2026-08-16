package com.glyphinterface

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class MusicVisualizerService : Service() {
    private var musicVisualizer: MusicVisualizer? = null

    override fun onCreate() {
        super.onCreate()
        musicVisualizer = MusicVisualizer(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d(TAG, "Service started, initializing visualizer")

            musicVisualizer?.start { low, midLow, mid, midHigh, high ->
                RootUtils.updateMusicVisualizer(low, midLow, mid, midHigh, high)
            }
        }
        return START_STICKY
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Glyph Music Visualizer")
            .setContentText("Music sync is active")
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
