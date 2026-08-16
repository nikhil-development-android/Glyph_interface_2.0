package com.glyphinterface

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

class NotificationService : NotificationListenerService(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var proximity: Sensor? = null
    private var lightSensor: Sensor? = null
    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager

    private var isFlippedAccel = false
    private var isNearProximity = false
    private var isFaceDown = false

    private val powerMonitor = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (it.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        val batteryIntent = context?.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                        if (level >= 0 && scale > 0) {
                            val batteryPct = ((level.toFloat() / scale.toFloat()) * 100).toInt()
                            RootUtils.updateChargingIndicator(batteryPct)
                        }
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        RootUtils.clearAllLedsSmoothly()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        createNotificationChannel()
        registerSensors()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        registerReceiver(powerMonitor, filter)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Glyph Interface Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    fun updateServiceNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopTorchIntent = Intent(this, NotificationService::class.java).apply {
            action = ACTION_STOP_TORCH
        }
        val stopTorchPending = PendingIntent.getService(
            this,
            1,
            stopTorchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopTimerIntent = Intent(this, NotificationService::class.java).apply {
            action = ACTION_STOP_TIMER
        }
        val stopTimerPending = PendingIntent.getService(
            this,
            2,
            stopTimerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusLines = mutableListOf<String>()
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Glyph Interface")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)

        if (isTorchOn) {
            statusLines.add("Torch: ON")
            builder.addAction(0, "TURN OFF TORCH", stopTorchPending)
        }

        if (activeTimerSeconds >= 0) {
            val mins = activeTimerSeconds / 60
            val secs = activeTimerSeconds % 60
            val formatted = String.format("%02d:%02d", mins, secs)
            statusLines.add("Timer: $formatted")
            builder.addAction(0, "STOP TIMER", stopTimerPending)
        }

        if (isFlipModeActive) {
            statusLines.add("Flip Mode: ACTIVE")
        }

        val statusText = if (statusLines.isEmpty()) "Monitoring Glyph Features" else statusLines.joinToString(" | ")
        builder.setContentText(statusText)

        startForeground(NOTIFICATION_ID, builder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_STOP_TIMER -> {
                    activeTimerSeconds = -1
                    RootUtils.clearAllLedsSmoothly()
                    updateServiceNotification()
                }
                ACTION_STOP_TORCH -> {
                    isTorchOn = false
                    RootUtils.setGlyphBrightness(0)
                    updateServiceNotification()
                }
            }
        }
        updateServiceNotification()
        return START_STICKY
    }

    private fun registerSensors() {
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        proximity?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (isFlipModeActive && flipToGlyphEnabled) {
            RootUtils.blinkNotification(this)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Z axis < -8 m/s^2 means screen is facing downwards
                isFlippedAccel = event.values[2] < -8.0f
            }
            Sensor.TYPE_LIGHT -> {
                if (autoBrightnessEnabled) {
                    val lux = event.values[0]
                    val newBrightness = (lux / 1000.0f).coerceIn(0.1f, 1.0f)
                    RootUtils.globalBrightness = newBrightness
                }
            }
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values[0]
                val maxRange = proximity?.maximumRange ?: 5.0f
                isNearProximity = distance < maxRange
            }
        }

        if (flipToGlyphEnabled) {
            val currentlyFaceDown = isFlippedAccel && isNearProximity
            if (currentlyFaceDown && !isFaceDown) {
                isFaceDown = true
                isFlipModeActive = true
                RootUtils.playFlipAnimation(this)
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                updateServiceNotification()
            } else if (!currentlyFaceDown && isFaceDown) {
                isFaceDown = false
                isFlipModeActive = false
                RootUtils.clearAllLedsSmoothly()
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                updateServiceNotification()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        try {
            unregisterReceiver(powerMonitor)
        } catch (e: Exception) {}
    }

    companion object {
        const val CHANNEL_ID = "glyph_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_TORCH = "com.glyphinterface.ACTION_STOP_TORCH"
        const val ACTION_STOP_TIMER = "com.glyphinterface.ACTION_STOP_TIMER"

        var isFlipModeActive = false
        var flipToGlyphEnabled = false
        var autoBrightnessEnabled = false
        var isTorchOn = false
        var activeTimerSeconds = -1
    }
}
