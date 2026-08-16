package com.glyphinterface

import android.content.Context
import android.util.Log

object RootUtils {
    private const val TAG = "RootUtils"
    private const val BASE_PATH = "/sys/class/leds/aw20036_led"

    var isMainEnabled = true
    var globalBrightness = 1.0f
        set(value) {
            field = value.coerceIn(0.0f, 1.0f)
            GlyphManager.currentBrightnessMultiplier = field
        }

    // Grid mapping 33 LEDs (indices 0..32) into 11 visual columns on Nothing Phone
    val GRID_PATTERN = arrayOf(
        intArrayOf(0, 12, 24),
        intArrayOf(1, 13, 25),
        intArrayOf(2, 14, 26),
        intArrayOf(3, 15, 27),
        intArrayOf(4, 16, 28),
        intArrayOf(5, 17, 29),
        intArrayOf(6, 18, 30),
        intArrayOf(7, 19, 31),
        intArrayOf(8, 20, 32),
        intArrayOf(9, 21),
        intArrayOf(10, 22, 23, 11)
    )

    fun setGlyphOperatingMode(enabled: Boolean) {
        val mode = if (enabled) 1 else 0
        GlyphManager.runCommand("echo $mode > $BASE_PATH/operating_mode")
    }

    fun setGlyphBrightness(brightness: Int) {
        val scaled = (brightness * globalBrightness).toInt().coerceIn(0, 255)
        GlyphManager.runCommand("echo $scaled > $BASE_PATH/all_white_brightness")
    }

    fun setLedBrightness(ledIndex: Int, brightness: Int) {
        if (ledIndex in 0..32) {
            val scaled = (brightness * globalBrightness).toInt().coerceIn(0, 255)
            GlyphManager.runCommand("echo $ledIndex $scaled > $BASE_PATH/single_brightness")
        }
    }

    /**
     * Updates all 33 LEDs based on 5 frequency energy bands (Bass, Mid-Low, Mid, Mid-High, High).
     */
    fun updateMusicVisualizer(low: Int, midLow: Int, mid: Int, midHigh: Int, high: Int) {
        if (!isMainEnabled) return

        val bands = arrayOf(low, midLow, mid, midHigh, high)
        val columnBands = intArrayOf(0, 0, 1, 1, 2, 2, 2, 3, 3, 4, 4)

        val sb = StringBuilder()
        for (col in 0 until minOf(GRID_PATTERN.size, columnBands.size)) {
            val bandIdx = columnBands[col]
            val energy = (bands[bandIdx] * globalBrightness).toInt().coerceIn(0, 255)
            val leds = GRID_PATTERN[col]
            val numLedsToLight = when {
                energy > 160 -> 3
                energy > 75 -> 2
                energy > 15 -> 1
                else -> 0
            }
            for (row in leds.indices) {
                val ledId = leds[row]
                if (ledId in 0..32) {
                    val ledBrightness = if (row < numLedsToLight) energy else 0
                    sb.append("echo $ledId $ledBrightness > $BASE_PATH/single_brightness; ")
                }
            }
        }
        if (sb.isNotEmpty()) {
            GlyphManager.runCommand(sb.toString(), priority = 3)
        }
    }

    fun updateVolumeIndicator(step: Int) {
        if (!isMainEnabled) return
        val maxSteps = 15
        val clampedStep = step.coerceIn(0, maxSteps)
        val ledsToTurnOn = ((clampedStep.toFloat() / maxSteps) * 33).toInt().coerceIn(0, 33)

        val sb = StringBuilder()
        for (i in 0..32) {
            val br = if (i < ledsToTurnOn) (255 * globalBrightness).toInt().coerceIn(0, 255) else 0
            sb.append("echo $i $br > $BASE_PATH/single_brightness; ")
        }
        GlyphManager.runCommand(sb.toString(), priority = 5)
    }

    fun updateTimerIndicator(progress: Float) {
        if (!isMainEnabled) return
        val clamped = progress.coerceIn(0.0f, 1.0f)
        val activeLeds = (clamped * 33).toInt().coerceIn(0, 33)

        val sb = StringBuilder()
        for (i in 0..32) {
            val br = if (i < activeLeds) (255 * globalBrightness).toInt().coerceIn(0, 255) else 0
            sb.append("echo $i $br > $BASE_PATH/single_brightness; ")
        }
        GlyphManager.runCommand(sb.toString(), priority = 5)
    }

    fun updateChargingIndicator(level: Int) {
        if (!isMainEnabled) return
        val clamped = level.coerceIn(0, 100)
        val activeLeds = ((clamped / 100.0f) * 33).toInt().coerceIn(0, 33)

        val sb = StringBuilder()
        for (i in 0..32) {
            val br = if (i < activeLeds) (255 * globalBrightness).toInt().coerceIn(0, 255) else 0
            sb.append("echo $i $br > $BASE_PATH/single_brightness; ")
        }
        GlyphManager.runCommand(sb.toString(), priority = 5)
    }

    fun playCallAnimation(context: Context, pattern: String) {
        if (!isMainEnabled) return
        val fileName = if (pattern.endsWith(".csv")) pattern else "$pattern.csv"
        val fullPath = if (fileName.startsWith("call/")) fileName else "call/$fileName"
        Log.d(TAG, "Playing call pattern animation: $fullPath")
        GlyphManager.playAnimation(context, fullPath, isNotification = false)
    }

    fun stopCallAnimation() {
        GlyphManager.stopAnimation()
        clearAllLedsSmoothly()
    }

    fun playFlipAnimation(context: Context) {
        if (!isMainEnabled) return
        GlyphManager.playAnimation(context, "flip.csv", isNotification = true)
    }

    fun blinkNotification(context: Context) {
        if (!isMainEnabled) return
        GlyphManager.playAnimation(context, "notification/Beak.csv", isNotification = true)
    }

    fun clearAllLeds() {
        GlyphManager.runCommand("echo 0 > $BASE_PATH/all_white_brightness", priority = 10)
    }

    fun clearAllLedsSmoothly() {
        GlyphManager.clearSmoothly()
    }
}
