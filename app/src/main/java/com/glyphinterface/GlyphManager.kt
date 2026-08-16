package com.glyphinterface

import android.content.Context
import android.os.SystemClock
import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.concurrent.LinkedBlockingQueue

object GlyphManager {
    private const val TAG = "GlyphManager"
    private const val BASE_PATH = "/sys/class/leds/aw20036_led"
    private const val VIBRATOR_DURATION_PATH = "/sys/class/leds/vibrator/duration"
    private const val VIBRATOR_ACTIVATE_PATH = "/sys/class/leds/vibrator/activate"

    var currentBrightnessMultiplier = 1.0f

    @Volatile
    private var isPlaying = false

    private val commandQueue = LinkedBlockingQueue<String>(100)

    @Volatile
    private var isRunning = true

    private var process: Process? = null
    private var os: DataOutputStream? = null

    init {
        val workerThread = Thread {
            initShell()
            while (isRunning) {
                try {
                    val cmd = commandQueue.take()
                    executeInternal(cmd)
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Worker thread error: ${e.message}")
                }
            }
            closeShell()
        }
        workerThread.isDaemon = true
        workerThread.name = "GlyphManagerWorker"
        workerThread.start()
    }

    private fun initShell() {
        try {
            process = ProcessBuilder("su").redirectErrorStream(true).start()
            process?.outputStream?.let {
                os = DataOutputStream(it)
            }
            Log.d(TAG, "Root shell acquired successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire root su process: ${e.message}. Using direct sysfs.")
            process = null
            os = null
        }
    }

    private fun closeShell() {
        try {
            os?.writeBytes("exit\n")
            os?.flush()
            os?.close()
            process?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing shell: ${e.message}")
        }
        os = null
        process = null
    }

    private fun executeInternal(command: String) {
        val stream = os
        if (stream != null) {
            try {
                stream.writeBytes("$command\n")
                stream.flush()
                return
            } catch (e: Exception) {
                Log.w(TAG, "Root shell write failed: ${e.message}. Attempting re-init.")
                initShell()
            }
        }
        // Direct sysfs file access fallback
        try {
            val statements = command.split(";")
            for (stmt in statements) {
                val trimmed = stmt.trim()
                if (trimmed.startsWith("echo ") && trimmed.contains(" > ")) {
                    val parts = trimmed.removePrefix("echo ").split(" > ")
                    if (parts.size == 2) {
                        val value = parts[0].trim()
                        val path = parts[1].trim()
                        val file = File(path)
                        if (file.exists() && file.canWrite()) {
                            FileOutputStream(file).use { fos ->
                                fos.write(value.toByteArray())
                                fos.flush()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct write error: ${e.message}")
        }
    }

    fun runCommand(command: String, priority: Int = 0) {
        if (!commandQueue.offer(command)) {
            // Queue full, drop oldest command to prevent lag during fast visualizer animations
            commandQueue.poll()
            commandQueue.offer(command)
        }
    }

    /**
     * Plays Nothing OS Glyph animation CSV.
     * Fixes LED 33 mapping and scales 12-bit (0..4095) CSV PWM values to 8-bit (0..255).
     */
    fun playAnimation(context: Context, assetPath: String, isNotification: Boolean = false) {
        stopAnimation()
        isPlaying = true

        Thread {
            try {
                // For call ringtones, loop until stopped; for notifications, play once
                do {
                    context.assets.open(assetPath).use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            var line = reader.readLine()
                            while (line != null && isPlaying) {
                                val cleanLine = line.trim()
                                if (cleanLine.isNotEmpty() && !cleanLine.startsWith("#")) {
                                    val tokens = cleanLine.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    
                                    if (tokens.isNotEmpty()) {
                                        val values = tokens.mapNotNull { it.toIntOrNull() }
                                        if (values.isNotEmpty()) {
                                            val ledBrightnessMap = IntArray(33) { 0 }

                                            if (values.size >= 33) {
                                                for (i in 0 until 33) {
                                                    val raw = values[i]
                                                    ledBrightnessMap[i] = scalePwmValue(raw)
                                                }
                                            } else if (values.size >= 25) {
                                                // Standard 26-zone Nothing Phone pattern
                                                // 0..15: Center loop (16 LEDs)
                                                for (i in 0..15) {
                                                    if (i < values.size) {
                                                        ledBrightnessMap[i] = scalePwmValue(values[i])
                                                    }
                                                }
                                                // 16..19: Slanted strips
                                                for (i in 16..19) {
                                                    if (i < values.size) {
                                                        ledBrightnessMap[i] = scalePwmValue(values[i])
                                                    }
                                                }
                                                // 20..23: Center vertical LEDs
                                                for (i in 20..23) {
                                                    if (i < values.size) {
                                                        ledBrightnessMap[i] = scalePwmValue(values[i])
                                                    }
                                                }
                                                // 24..31: Bottom strip indicator
                                                val bottomStripVal = if (24 < values.size) scalePwmValue(values[24]) else 0
                                                for (i in 24..31) {
                                                    ledBrightnessMap[i] = bottomStripVal
                                                }
                                                // 32 (LED 33): Bottom dot / indicator
                                                val led33Val = if (25 < values.size) scalePwmValue(values[25]) else bottomStripVal
                                                ledBrightnessMap[32] = led33Val
                                            } else if (values.size == 5) {
                                                // Nothing Phone (1) / 5-zone fallback
                                                for (i in 0..15) ledBrightnessMap[i] = scalePwmValue(values[0])
                                                for (i in 16..19) ledBrightnessMap[i] = scalePwmValue(values[1])
                                                for (i in 20..23) ledBrightnessMap[i] = scalePwmValue(values[2])
                                                for (i in 24..31) ledBrightnessMap[i] = scalePwmValue(values[3])
                                                ledBrightnessMap[32] = scalePwmValue(values[4])
                                            }

                                            // Build batch sysfs string
                                            val sb = StringBuilder()
                                            for (i in 0..32) {
                                                sb.append("echo $i ${ledBrightnessMap[i]} > $BASE_PATH/single_brightness; ")
                                            }
                                            runCommand(sb.toString())
                                        }
                                    }
                                }

                                // 20ms per frame (50 FPS Nothing OS standard frame rate)
                                SystemClock.sleep(20)
                                line = reader.readLine()
                            }
                        }
                    }
                } while (isPlaying && !isNotification)
            } catch (e: Exception) {
                Log.e(TAG, "Error playing animation ($assetPath): ${e.message}")
            } finally {
                if (isNotification) {
                    isPlaying = false
                    clearSmoothly()
                }
            }
        }.apply {
            isDaemon = true
            name = "GlyphAnimationPlayer"
            start()
        }
    }

    private fun scalePwmValue(rawVal: Int): Int {
        if (rawVal <= 0) return 0
        // Raw values can be up to 4095 (12-bit) or up to 255 (8-bit)
        val normalized = if (rawVal > 255) {
            (rawVal / 4095.0f) * 255.0f
        } else {
            rawVal.toFloat()
        }
        return (normalized * currentBrightnessMultiplier).toInt().coerceIn(0, 255)
    }

    fun stopAnimation() {
        isPlaying = false
    }

    fun clearSmoothly() {
        Thread {
            for (step in 4 downTo 0) {
                val factor = step / 4.0f
                val sb = StringBuilder()
                for (i in 0..32) {
                    val br = (255 * factor * currentBrightnessMultiplier).toInt().coerceIn(0, 255)
                    sb.append("echo $i $br > $BASE_PATH/single_brightness; ")
                }
                runCommand(sb.toString())
                SystemClock.sleep(25)
            }
            runCommand("echo 0 > $BASE_PATH/all_white_brightness")
        }.apply {
            isDaemon = true
            name = "GlyphSmoothClear"
            start()
        }
    }
}
