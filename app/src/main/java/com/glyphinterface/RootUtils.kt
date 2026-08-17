package com.glyphinterface

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log

object RootUtils {
    private const val TAG = "RootUtils"
    private const val BASE_PATH = GlyphManager.BASE_PATH

    var isMainEnabled = true
    var globalBrightness = 1.0f
        set(value) {
            field = value.coerceIn(0.0f, 1.0f)
            GlyphManager.currentBrightnessMultiplier = field
        }

    /**
     * 8-Step LED Ladder (LEDs 0 to 31):
     * Step 0: [0, 12, 24]
     * Step 1: [1, 13, 25]
     * Step 2: [2, 14, 26]
     * Step 3: [3, 15, 27]
     * Step 4: [4, 16, 28]
     * Step 5: [5, 17, 29]
     * Step 6: [6, 18, 30]
     * Step 7: [7, 19, 31]
     */
    val STEP_COLUMNS = arrayOf(
        intArrayOf(0, 12, 24),
        intArrayOf(1, 13, 25),
        intArrayOf(2, 14, 26),
        intArrayOf(3, 15, 27),
        intArrayOf(4, 16, 28),
        intArrayOf(5, 17, 29),
        intArrayOf(6, 18, 30),
        intArrayOf(7, 19, 31)
    )

    // Special Individual LEDs
    const val LED_MINI_2 = 20      // Mini LED no-2 (Vocal / Timer tick)
    const val LED_MEDIUM_3 = 33    // Medium LED no-3 (Instrument / Notification)

    private val mainHandler = Handler(Looper.getMainLooper())
    private var volumeTimeoutRunnable: Runnable? = null

    // Music Visualizer Rate Limiter State (throttled to give LEDs time to blink)
    private var lastVisualizerTime = 0L
    private var lastVocalState = false
    private var lastInstrumentState = false
    private var vocalHoldUntil = 0L
    private var instrumentHoldUntil = 0L

    /**
     * Main on/off switch:
     * on: echo 1 > operating_mode
     * off: echo 0 > operating_mode
     */
    fun setGlyphOperatingMode(enabled: Boolean) {
        val mode = if (enabled) 1 else 0
        GlyphManager.runCommand("echo $mode > $BASE_PATH/operating_mode")
    }

    /**
     * Torch & Master Brightness Control:
     * echo 1 > operating_mode
     * echo 255 > all_brightness
     * echo 125 > all_brightness
     * echo 0 > all_brightness
     */
    fun setGlyphBrightness(brightness: Int) {
        val scaled = (brightness * globalBrightness).toInt().coerceIn(0, 255)
        val mode = if (scaled > 0) 1 else 0
        val sb = StringBuilder()
        sb.append("echo $mode > $BASE_PATH/operating_mode; ")
        sb.append("echo $scaled > $BASE_PATH/all_brightness; ")
        sb.append("echo $scaled > $BASE_PATH/all_white_brightness; ")
        for (i in 0..31) {
            sb.append("echo \"$i $scaled\" > $BASE_PATH/single_brightness; ")
        }
        sb.append("echo \"$LED_MINI_2 $scaled\" > $BASE_PATH/single_brightness; ")
        sb.append("echo \"$LED_MEDIUM_3 $scaled\" > $BASE_PATH/single_brightness; ")
        GlyphManager.runCommand(sb.toString(), priority = 10)
    }

    /**
     * Single LED brightness control:
     * echo "X 255" > single_brightness
     */
    fun setLedBrightness(ledIndex: Int, brightness: Int) {
        val scaled = (brightness * globalBrightness).toInt().coerceIn(0, 255)
        GlyphManager.runCommand("echo \"$ledIndex $scaled\" > $BASE_PATH/single_brightness")
    }

    /**
     * Volume Indicator Logic (0 to 31 LEDs):
     * Displays current volume level across 0-31 LEDs and automatically turns off after 2.5s.
     */
    fun updateVolumeIndicator(currentStreamVolume: Int, maxVolume: Int = 15) {
        if (!isMainEnabled) return

        volumeTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }

        val normalized = (currentStreamVolume.toFloat() / maxVolume.coerceAtLeast(1)).coerceIn(0f, 1f)
        val activeLeds = (normalized * 32f).toInt().coerceIn(0, 32)
        val maxBrightness = (255 * globalBrightness).toInt().coerceIn(0, 255)

        val sb = StringBuilder()
        sb.append("echo 1 > $BASE_PATH/operating_mode; ")
        for (i in 0..31) {
            val br = if (i < activeLeds) maxBrightness else 0
            sb.append("echo \"$i $br\" > $BASE_PATH/single_brightness; ")
        }
        GlyphManager.runCommand(sb.toString(), priority = 5)

        // Turn off volume LEDs after 2.5 seconds
        volumeTimeoutRunnable = Runnable {
            if (isMainEnabled && !NotificationService.isTorchOn) {
                clearStepLeds()
            }
        }
        mainHandler.postDelayed(volumeTimeoutRunnable!!, 2500)
    }

    /**
     * Timer Logic:
     * Shows remaining time on LEDs 0 to 31 (31 down to 0 turn off progressively).
     * LED 20 blinks only on each second tick!
     */
    fun updateTimerProgress(progress: Float, isTicking: Boolean = false) {
        if (!isMainEnabled) return

        val clamped = progress.coerceIn(0.0f, 1.0f)
        val activeLeds = (clamped * 32f).toInt().coerceIn(0, 32)
        val maxBrightness = (255 * globalBrightness).toInt().coerceIn(0, 255)

        val sb = StringBuilder()
        sb.append("echo 1 > $BASE_PATH/operating_mode; ")
        for (i in 0..31) {
            val br = if (i < activeLeds) maxBrightness else 0
            sb.append("echo \"$i $br\" > $BASE_PATH/single_brightness; ")
        }
        GlyphManager.runCommand(sb.toString(), priority = 5)

        if (isTicking) {
            blinkTimerTickLed()
        }
    }

    /**
     * Blinks LED 20 (Mini LED) for 180ms during timer second tick
     */
    fun blinkTimerTickLed() {
        if (!isMainEnabled) return
        Thread {
            val br = (255 * globalBrightness).toInt().coerceIn(0, 255)
            GlyphManager.runCommandSync("echo \"$LED_MINI_2 $br\" > $BASE_PATH/single_brightness")
            SystemClock.sleep(180)
            GlyphManager.runCommandSync("echo \"$LED_MINI_2 0\" > $BASE_PATH/single_brightness")
        }.apply {
            isDaemon = true
            name = "TimerTickBlink"
            start()
        }
    }

    /**
     * Blinks all_brightness 3 times when timer completes
     */
    fun blinkTimerFinished() {
        if (!isMainEnabled) return
        Thread {
            val br = (255 * globalBrightness).toInt().coerceIn(0, 255)
            for (i in 0 until 3) {
                GlyphManager.runCommandSync("echo $br > $BASE_PATH/all_brightness; echo $br > $BASE_PATH/all_white_brightness")
                SystemClock.sleep(220)
                GlyphManager.runCommandSync("echo 0 > $BASE_PATH/all_brightness; echo 0 > $BASE_PATH/all_white_brightness")
                SystemClock.sleep(180)
            }
        }.apply {
            isDaemon = true
            name = "TimerFinishedBlink"
            start()
        }
    }

    /**
     * Notification Blinking (2 times):
     * echo 255 > all_brightness
     * echo 0 > all_brightness
     * echo 255 > all_brightness
     * echo 0 > all_brightness
     */
    fun blinkNotification(context: Context? = null) {
        if (!isMainEnabled) return
        Thread {
            val br = (255 * globalBrightness).toInt().coerceIn(0, 255)
            for (i in 0 until 2) {
                GlyphManager.runCommandSync("echo $br > $BASE_PATH/all_brightness; echo $br > $BASE_PATH/all_white_brightness")
                SystemClock.sleep(180)
                GlyphManager.runCommandSync("echo 0 > $BASE_PATH/all_brightness; echo 0 > $BASE_PATH/all_white_brightness")
                SystemClock.sleep(140)
            }
        }.apply {
            isDaemon = true
            name = "NotificationBlink"
            start()
        }
    }

    /**
     * Flip to Glyph notification:
     * When face down and notification arrives, LED 33 blinks for 2 seconds.
     */
    fun blinkFlipNotification() {
        if (!isMainEnabled) return
        Thread {
            val br = (255 * globalBrightness).toInt().coerceIn(0, 255)
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 2000) {
                GlyphManager.runCommandSync("echo \"$LED_MEDIUM_3 $br\" > $BASE_PATH/single_brightness")
                SystemClock.sleep(200)
                GlyphManager.runCommandSync("echo \"$LED_MEDIUM_3 0\" > $BASE_PATH/single_brightness")
                SystemClock.sleep(150)
            }
        }.apply {
            isDaemon = true
            name = "FlipNotificationBlink"
            start()
        }
    }

    /**
     * Flip to Glyph Activation Animation:
     * 1.5s cascade sweep across steps 0..7, LED 20, and LED 33.
     */
    fun playFlipAnimation(context: Context) {
        if (!isMainEnabled) return
        Thread {
            val br = (255 * globalBrightness).toInt().coerceIn(0, 255)
            // Cascade Up
            for (step in 0 until 8) {
                val sb = StringBuilder()
                for (led in STEP_COLUMNS[step]) {
                    sb.append("echo \"$led $br\" > $BASE_PATH/single_brightness; ")
                }
                GlyphManager.runCommandSync(sb.toString())
                SystemClock.sleep(60)
            }
            // Flash mini & medium LEDs
            GlyphManager.runCommandSync("echo \"$LED_MINI_2 $br\" > $BASE_PATH/single_brightness; echo \"$LED_MEDIUM_3 $br\" > $BASE_PATH/single_brightness")
            SystemClock.sleep(250)
            // Cascade Down Off
            for (step in 7 downTo 0) {
                val sb = StringBuilder()
                for (led in STEP_COLUMNS[step]) {
                    sb.append("echo \"$led 0\" > $BASE_PATH/single_brightness; ")
                }
                GlyphManager.runCommandSync(sb.toString())
                SystemClock.sleep(40)
            }
            GlyphManager.runCommandSync("echo \"$LED_MINI_2 0\" > $BASE_PATH/single_brightness; echo \"$LED_MEDIUM_3 0\" > $BASE_PATH/single_brightness")
        }.apply {
            isDaemon = true
            name = "FlipActivationAnimation"
            start()
        }
    }

    /**
     * Music Progress Track (0% to 100% on LEDs 0-31):
     */
    fun updateMusicProgress(progress: Float) {
        if (!isMainEnabled) return
        val clamped = progress.coerceIn(0.0f, 1.0f)
        val activeSteps = (clamped * 8).toInt().coerceIn(0, 8)
        val maxBrightness = (255 * globalBrightness).toInt().coerceIn(0, 255)

        val sb = StringBuilder()
        for (stepIdx in 0 until 8) {
            val br = if (stepIdx < activeSteps) maxBrightness else 0
            for (led in STEP_COLUMNS[stepIdx]) {
                sb.append("echo \"$led $br\" > $BASE_PATH/single_brightness; ")
            }
        }
        GlyphManager.runCommand(sb.toString(), priority = 2)
    }

    /**
     * 3-Part Music Visualizer:
     * 1. Bass: LEDs 0-31
     * 2. Vocal: LED 20 (Mini LED no-2)
     * 3. Instruments: LED 33 (Medium LED no-3)
     */
    fun updateMusicVisualizer3Part(bassEnergy: Int, vocalEnergy: Int, instrumentEnergy: Int) {
        if (!isMainEnabled || NotificationService.isTorchOn) return

        val now = SystemClock.uptimeMillis()
        if (now - lastVisualizerTime < 45) return
        lastVisualizerTime = now

        val maxBrightness = (255 * globalBrightness).toInt().coerceIn(0, 255)

        // 1. Bass mapping (0..32 LEDs on 0-31)
        val activeLeds = ((bassEnergy / 255f) * 32f).toInt().coerceIn(0, 32)

        val sb = StringBuilder()
        sb.append("echo 1 > $BASE_PATH/operating_mode; ")
        for (i in 0..31) {
            val br = if (i < activeLeds) maxBrightness else 0
            sb.append("echo \"$i $br\" > $BASE_PATH/single_brightness; ")
        }

        // 2. Vocal mapping (LED 20 with hold time)
        val isVocalActive = vocalEnergy > 35
        if (isVocalActive) {
            vocalHoldUntil = now + 80
            sb.append("echo \"$LED_MINI_2 $maxBrightness\" > $BASE_PATH/single_brightness; ")
        } else if (now >= vocalHoldUntil) {
            sb.append("echo \"$LED_MINI_2 0\" > $BASE_PATH/single_brightness; ")
        }

        // 3. Instrument / Treble mapping (LED 33 with hold time)
        val isInstrumentActive = instrumentEnergy > 30
        if (isInstrumentActive) {
            instrumentHoldUntil = now + 80
            sb.append("echo \"$LED_MEDIUM_3 $maxBrightness\" > $BASE_PATH/single_brightness; ")
        } else if (now >= instrumentHoldUntil) {
            sb.append("echo \"$LED_MEDIUM_3 0\" > $BASE_PATH/single_brightness; ")
        }

        GlyphManager.runCommand(sb.toString(), priority = 3)
    }

    /**
     * Charging Status Indicator:
     * On connect: LEDs 0-31 show battery % for 3 seconds.
     * On disconnect: Blinks 2 times using all_brightness.
     */
    fun updateChargingConnected(batteryPercentage: Int) {
        if (!isMainEnabled) return
        Thread {
            val clamped = batteryPercentage.coerceIn(0, 100)
            val activeLeds = ((clamped / 100.0f) * 32).toInt().coerceIn(1, 32)
            val maxBrightness = (255 * globalBrightness).toInt().coerceIn(0, 255)

            val sb = StringBuilder()
            sb.append("echo 1 > $BASE_PATH/operating_mode; ")
            for (i in 0 until activeLeds) {
                sb.append("echo \"$i $maxBrightness\" > $BASE_PATH/single_brightness; ")
            }
            GlyphManager.runCommandSync(sb.toString())

            // Hold for 3 seconds
            SystemClock.sleep(3000)
            // Fade out
            clearStepLeds()
        }.apply {
            isDaemon = true
            name = "ChargingConnectedIndicator"
            start()
        }
    }

    fun updateChargingDisconnected() {
        if (!isMainEnabled) return
        Thread {
            val br = (255 * globalBrightness).toInt().coerceIn(0, 255)
            for (i in 0 until 2) {
                GlyphManager.runCommandSync("echo $br > $BASE_PATH/all_brightness; echo $br > $BASE_PATH/all_white_brightness")
                SystemClock.sleep(180)
                GlyphManager.runCommandSync("echo 0 > $BASE_PATH/all_brightness; echo 0 > $BASE_PATH/all_white_brightness")
                SystemClock.sleep(150)
            }
        }.apply {
            isDaemon = true
            name = "ChargingDisconnectedIndicator"
            start()
        }
    }

    /**
     * Plays Ringtone Patterns (20 patterns)
     */
    fun playCallAnimation(context: Context, patternName: String) {
        if (!isMainEnabled) return
        val fileName = if (patternName.endsWith(".csv")) patternName else "$patternName.csv"
        val fullPath = if (fileName.startsWith("call/")) fileName else "call/$fileName"
        
        try {
            context.assets.open(fullPath).close()
            GlyphManager.playAnimation(context, fullPath, isNotification = false)
        } catch (e: Exception) {
            // If specific CSV isn't found, play dynamic rhythmic pattern engine
            playAlgorithmicPattern(patternName)
        }
    }

    private fun playAlgorithmicPattern(patternName: String) {
        GlyphManager.stopAnimation()
        Thread {
            val br = (255 * globalBrightness).toInt().coerceIn(0, 255)
            while (isMainEnabled && GlyphManager.isPlaying) {
                // Rhythmic pulse matching pattern
                val sb = StringBuilder()
                for (i in 0..31) {
                    sb.append("echo \"$i $br\" > $BASE_PATH/single_brightness; ")
                }
                sb.append("echo \"$LED_MINI_2 $br\" > $BASE_PATH/single_brightness; ")
                sb.append("echo \"$LED_MEDIUM_3 $br\" > $BASE_PATH/single_brightness; ")
                GlyphManager.runCommandSync(sb.toString())
                SystemClock.sleep(80)
                clearStepLeds()
                SystemClock.sleep(100)
            }
        }.apply {
            isDaemon = true
            name = "AlgorithmicPatternPlayer"
            start()
        }
    }

    fun stopCallAnimation() {
        GlyphManager.stopAnimation()
        clearAllLedsSmoothly()
    }

    fun clearStepLeds() {
        val sb = StringBuilder()
        for (i in 0..31) {
            sb.append("echo \"$i 0\" > $BASE_PATH/single_brightness; ")
        }
        sb.append("echo \"$LED_MINI_2 0\" > $BASE_PATH/single_brightness; ")
        sb.append("echo \"$LED_MEDIUM_3 0\" > $BASE_PATH/single_brightness; ")
        GlyphManager.runCommand(sb.toString())
    }

    fun clearAllLeds() {
        GlyphManager.runCommand("echo 0 > $BASE_PATH/all_brightness; echo 0 > $BASE_PATH/all_white_brightness", priority = 10)
    }

    fun clearAllLedsSmoothly() {
        GlyphManager.clearAllLedsSmoothly()
    }
}
