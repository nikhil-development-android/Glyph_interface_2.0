package com.glyphinterface

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.util.Log
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

/**
 * 3-Band Music Visualizer:
 * 1. Bass: Maps to LEDs 0-31
 * 2. Vocal: Maps to Mini LED 20
 * 3. Instruments/High: Maps to Medium LED 33
 */
class MusicVisualizer(private val context: Context) {
    private var visualizer: Visualizer? = null
    private var audioRecord: AudioRecord? = null
    private var audioRecordThread: Thread? = null

    @Volatile
    private var isRunning = false

    // Smooth filters and dynamic AGC
    private var smoothBass = 0f
    private var smoothVocal = 0f
    private var smoothInstrument = 0f
    private var peakRms = 1200f

    fun start(on3BandData: (bass: Int, vocal: Int, instrument: Int) -> Unit) {
        if (isRunning) return
        isRunning = true

        try {
            Log.d(TAG, "Initializing System AudioFx Visualizer...")
            visualizer = Visualizer(0).apply {
                val captureSizeRange = Visualizer.getCaptureSizeRange()
                val captureSize = if (captureSizeRange.size > 1) captureSizeRange[1] else 1024
                setCaptureSize(captureSize)
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED

                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        if (waveform != null && isRunning) {
                            processWaveform(waveform, on3BandData)
                        }
                    }

                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null && isRunning) {
                            processFft(fft, captureSize, samplingRate, on3BandData)
                        }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)

                enabled = true
            }
            Log.d(TAG, "System AudioFx Visualizer initialized")
        } catch (e: Exception) {
            Log.w(TAG, "System Visualizer failed (${e.message}).")
        }

        // Always start high-sensitivity audio record fallback to guarantee responsiveness
        startAudioRecordFallback(on3BandData)
    }

    private fun processWaveform(
        waveform: ByteArray,
        on3BandData: (bass: Int, vocal: Int, instrument: Int) -> Unit
    ) {
        var sumSquares = 0.0
        for (b in waveform) {
            val sample = (b.toInt() and 0xFF) - 128
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / waveform.size).toFloat()
        if (rms < 2.0f) return // likely silent or muted buffer

        val energy = (rms * 5.0f).coerceIn(0f, 255f)

        smoothBass = max(energy * 1.2f, smoothBass * 0.70f).coerceIn(0f, 255f)
        smoothVocal = max(energy * 1.0f, smoothVocal * 0.65f).coerceIn(0f, 255f)
        smoothInstrument = max(energy * 0.8f, smoothInstrument * 0.60f).coerceIn(0f, 255f)

        on3BandData(smoothBass.toInt(), smoothVocal.toInt(), smoothInstrument.toInt())
    }

    private fun processFft(
        fft: ByteArray,
        captureSize: Int,
        samplingRate: Int,
        on3BandData: (bass: Int, vocal: Int, instrument: Int) -> Unit
    ) {
        val n = fft.size / 2
        if (n <= 0) return

        val magnitudes = FloatArray(n)
        magnitudes[0] = abs(fft[0].toInt()).toFloat()
        var totalMag = magnitudes[0]
        for (k in 1 until n) {
            val rIndex = k * 2
            val iIndex = rIndex + 1
            if (iIndex < fft.size) {
                magnitudes[k] = hypot(fft[rIndex].toFloat(), fft[iIndex].toFloat())
                totalMag += magnitudes[k]
            }
        }
        if (totalMag < 5.0f) return // silent buffer

        val binFreq = (samplingRate / 1000f) / captureSize.coerceAtLeast(1)
        var bassSum = 0f
        var vocalSum = 0f
        var instrumentSum = 0f

        for (k in 1 until n) {
            val freq = k * binFreq * 1000f
            val mag = magnitudes[k]
            when {
                freq < 240f -> bassSum += mag * 2.2f
                freq in 240f..2800f -> vocalSum += mag * 1.8f
                else -> instrumentSum += mag * 1.5f
            }
        }

        val rawBass = (bassSum / (n * 0.018f)).coerceIn(0f, 255f)
        val rawVocal = (vocalSum / (n * 0.04f)).coerceIn(0f, 255f)
        val rawInstrument = (instrumentSum / (n * 0.06f)).coerceIn(0f, 255f)

        // Smooth decay to prevent LED flicker
        smoothBass = max(rawBass, smoothBass * 0.70f)
        smoothVocal = max(rawVocal, smoothVocal * 0.65f)
        smoothInstrument = max(rawInstrument, smoothInstrument * 0.60f)

        on3BandData(
            smoothBass.toInt(),
            smoothVocal.toInt(),
            smoothInstrument.toInt()
        )
    }

    @SuppressLint("MissingPermission")
    private fun startAudioRecordFallback(
        on3BandData: (bass: Int, vocal: Int, instrument: Int) -> Unit
    ) {
        if (audioRecordThread != null) return
        try {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = max(minBufferSize, 2048)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            Log.d(TAG, "AudioRecord fallback started successfully")

            audioRecordThread = Thread {
                val buffer = ShortArray(1024)
                while (isRunning) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0.0
                        var bassSum = 0.0
                        var trebleSum = 0.0

                        for (i in 0 until read) {
                            val sample = buffer[i].toDouble()
                            sum += sample * sample
                            if (i % 4 == 0) bassSum += sample * sample
                            if (i % 2 == 1) trebleSum += sample * sample
                        }

                        val rms = sqrt(sum / read).toFloat()
                        val bassRms = sqrt(bassSum / (read / 4)).toFloat()
                        val trebleRms = sqrt(trebleSum / (read / 2)).toFloat()

                        // Dynamic AGC
                        peakRms = max(rms, peakRms * 0.96f).coerceAtLeast(300f)
                        val gain = (255f / peakRms)

                        val rawEnergy = (rms * gain).coerceIn(0f, 255f)
                        val rawBass = (bassRms * gain * 1.35f).coerceIn(0f, 255f)
                        val rawTreble = (trebleRms * gain * 1.15f).coerceIn(0f, 255f)

                        smoothBass = max(rawBass, smoothBass * 0.70f).coerceIn(0f, 255f)
                        smoothVocal = max(rawEnergy, smoothVocal * 0.65f).coerceIn(0f, 255f)
                        smoothInstrument = max(rawTreble, smoothInstrument * 0.60f).coerceIn(0f, 255f)

                        on3BandData(
                            smoothBass.toInt(),
                            smoothVocal.toInt(),
                            smoothInstrument.toInt()
                        )
                    }

                    try {
                        Thread.sleep(45)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }.apply {
                isDaemon = true
                name = "Glyph3BandAudioFallback"
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord fallback: ${e.message}", e)
        }
    }

    fun stop() {
        isRunning = false
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Visualizer: ${e.message}")
        } finally {
            visualizer = null
        }

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
            audioRecordThread = null
        }
    }

    companion object {
        private const val TAG = "MusicVisualizer"
    }
}
