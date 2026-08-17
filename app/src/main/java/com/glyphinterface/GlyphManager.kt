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
    const val BASE_PATH = "/sys/class/leds/aw20036_led"

    var currentBrightnessMultiplier = 1.0f

    @Volatile
    var isPlaying = false
        private set

    private val commandQueue = LinkedBlockingQueue<String>(120)

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
                        val rawValue = parts[0].trim().removeSurrounding("\"").removeSurrounding("'")
                        val path = parts[1].trim()
                        val file = File(path)
                        if (file.exists() && file.canWrite()) {
                            FileOutputStream(file).use { fos ->
                                fos.write(rawValue.toByteArray())
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
     * Executes shell command synchronously for critical timing operations (e.g. blinks)
     */
    fun runCommandSync(command: String) {
        executeInternal(command)
    }

    /**
     * Plays Nothing OS Glyph animation CSV or algorithmic pattern.
     * Maps 0-31 ladder, LED 20 (mini LED) and LED 33 (medium LED).
     */
    fun playAnimation(context: Context, assetPath: String, isNotification: Boolean = false) {
        stopAnimation()
        isPlaying = true

        Thread {
            try {
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
                                            val sb = StringBuilder()
                                            val brightnessMap = IntArray(34) { 0 }

                                            if (values.size >= 26) {
                                                // Map 26 channel Nothing OS pattern:
                                                // 0..15 -> Step columns
                                                for (i in 0..7) {
                                                    val stepVal = scalePwmValue(values[i % values.size])
                                                    val leds = RootUtils.STEP_COLUMNS[i]
                                                    for (led in leds) {
                                                        brightnessMap[led] = stepVal
                                                    }
                                                }
                                                // LED 20 (Mini LED no-2)
                                                if (20 < values.size) {
                                                    brightnessMap[20] = scalePwmValue(values[20])
                                                }
                                                // LED 33 (Medium LED no-3)
                                                if (25 < values.size) {
                                                    brightnessMap[33] = scalePwmValue(values[25])
                                                } else if (values.size > 8) {
                                                    brightnessMap[33] = scalePwmValue(values[8])
                                                }
                                            } else if (values.size >= 8) {
                                                for (i in 0..7) {
                                                    val stepVal = scalePwmValue(values[i])
                                                    for (led in RootUtils.STEP_COLUMNS[i]) {
                                                        brightnessMap[led] = stepVal
                                                    }
                                                }
                                                if (values.size > 8) brightnessMap[20] = scalePwmValue(values[8])
                                                if (values.size > 9) brightnessMap[33] = scalePwmValue(values[9])
                                            } else {
                                                val stepVal = scalePwmValue(values[0])
                                                for (i in 0..7) {
                                                    for (led in RootUtils.STEP_COLUMNS[i]) {
                                                        brightnessMap[led] = stepVal
                                                    }
                                                }
                                                brightnessMap[20] = stepVal
                                                brightnessMap[33] = stepVal
                                            }

                                            // Build batch sysfs command with echo "LED BRIGHTNESS"
                                            for (led in 0..31) {
                                                val br = brightnessMap[led]
                                                sb.append("echo \"$led $br\" > $BASE_PATH/single_brightness; ")
                                            }
                                            sb.append("echo \"20 ${brightnessMap[20]}\" > $BASE_PATH/single_brightness; ")
                                            sb.append("echo \"33 ${brightnessMap[33]}\" > $BASE_PATH/single_brightness; ")
                                            
                                            runCommand(sb.toString())
                                        }
                                    }
                                }

                                SystemClock.sleep(25)
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
                    clearAllLedsSmoothly()
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

    fun clearAllLedsSmoothly() {
        Thread {
            for (step in 3 downTo 0) {
                val factor = step / 3.0f
                val br = (255 * factor * currentBrightnessMultiplier).toInt().coerceIn(0, 255)
                val sb = StringBuilder()
                for (col in RootUtils.STEP_COLUMNS) {
                    for (led in col) {
                        sb.append("echo \"$led $br\" > $BASE_PATH/single_brightness; ")
                    }
                }
                sb.append("echo \"20 $br\" > $BASE_PATH/single_brightness; ")
                sb.append("echo \"33 $br\" > $BASE_PATH/single_brightness; ")
                runCommand(sb.toString())
                SystemClock.sleep(30)
            }
            runCommand("echo 0 > $BASE_PATH/all_brightness; echo 0 > $BASE_PATH/all_white_brightness")
        }.apply {
            isDaemon = true
            name = "GlyphSmoothClear"
            start()
        }
    }
}
