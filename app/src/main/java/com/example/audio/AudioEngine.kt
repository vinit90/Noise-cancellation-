package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Process
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

class AudioEngine(private val context: Context) {

    companion object {
        private const val TAG = "AudioEngine"
        const val SAMPLE_RATE = 48000
        private const val RING_BUFFER_SIZE = 16384 // Power of 2, covers up to ~341ms at 48kHz
        private const val RING_BUFFER_MASK = RING_BUFFER_SIZE - 1
        private const val SPEED_OF_SOUND_CM_PER_MS = 34.3f // at 20°C in air
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Observable states
    private val _engineStatus = MutableStateFlow<EngineStatus>(EngineStatus.Idle)
    val engineStatus: StateFlow<EngineStatus> = _engineStatus.asStateFlow()

    private val _telemetry = MutableStateFlow(AudioEngineTelemetry())
    val telemetry: StateFlow<AudioEngineTelemetry> = _telemetry.asStateFlow()

    // DSP control parameters (thread-safe volatiles)
    @Volatile var gain: Float = 1.0f
    @Volatile var isPhaseInverted: Boolean = false
    @Volatile var delayMs: Int = 0
    @Volatile var isLimiterEnabled: Boolean = true
    @Volatile var isMuted: Boolean = false
    @Volatile var isTestToneEnabled: Boolean = false
    @Volatile var testToneFrequencyHz: Float = 440f

    // Worker thread & loop control
    @Volatile private var isRunning = false
    private var workerThread: Thread? = null

    // Native hardware handles
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    // DSP buffers
    private val ringBuffer = FloatArray(RING_BUFFER_SIZE)
    private var writeHead = 0

    // Downsampled waveforms for UI oscilloscope (128 points)
    private val uiInputWaveform = FloatArray(128)
    private val uiOutputWaveform = FloatArray(128)

    /**
     * Starts the audio engine on a dedicated high-priority audio thread.
     */
    @Synchronized
    fun start(): Boolean {
        if (isRunning) return true

        _engineStatus.value = EngineStatus.Starting
        Log.i(TAG, "Starting AudioEngine...")

        // Query hardware burst buffer size
        val framesPerBufferProp = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        val hardwareBurstFrames = framesPerBufferProp?.toIntOrNull()?.coerceIn(96, 512) ?: 192
        val bufferFrames = hardwareBurstFrames

        // 1. Configure and initialize AudioRecord
        var recordSource = MediaRecorder.AudioSource.UNPROCESSED
        var activeSourceName = "UNPROCESSED"
        var record: AudioRecord? = createAudioRecord(recordSource, bufferFrames)

        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            Log.w(TAG, "AudioSource.UNPROCESSED not supported, falling back to AudioSource.MIC")
            record?.release()
            recordSource = MediaRecorder.AudioSource.MIC
            activeSourceName = "MIC (Fallback)"
            record = createAudioRecord(recordSource, bufferFrames)
        }

        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            val errorMsg = "Failed to initialize AudioRecord (unsupported by hardware)"
            Log.e(TAG, errorMsg)
            record?.release()
            _engineStatus.value = EngineStatus.Error(errorMsg)
            return false
        }
        audioRecord = record

        // 2. Configure and initialize AudioTrack
        val track = createAudioTrack(bufferFrames)
        if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
            val errorMsg = "Failed to initialize low-latency AudioTrack"
            Log.e(TAG, errorMsg)
            audioRecord?.release()
            audioRecord = null
            track?.release()
            _engineStatus.value = EngineStatus.Error(errorMsg)
            return false
        }
        audioTrack = track

        // Clear ring buffer
        ringBuffer.fill(0f)
        writeHead = 0

        isRunning = true

        // 3. Launch dedicated high-priority audio worker thread
        workerThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            runAudioLoop(bufferFrames, activeSourceName)
        }, "PhaseLabAudioWorker").apply {
            start()
        }

        _engineStatus.value = EngineStatus.Running
        Log.i(TAG, "AudioEngine successfully started with bufferFrames=$bufferFrames, source=$activeSourceName")
        return true
    }

    /**
     * Stops the audio engine and releases resources.
     */
    @Synchronized
    fun stop() {
        if (!isRunning && workerThread == null) return
        Log.i(TAG, "Stopping AudioEngine...")

        isRunning = false
        try {
            workerThread?.interrupt()
            workerThread?.join(300)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupted while waiting for audio thread termination: ${e.message}")
        }
        workerThread = null

        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioRecord: ${e.message}")
        }
        audioRecord = null

        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                    it.flush()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioTrack: ${e.message}")
        }
        audioTrack = null

        _engineStatus.value = EngineStatus.Idle
        _telemetry.value = _telemetry.value.copy(
            inputRms = 0f,
            inputDbfs = -60f,
            outputRms = 0f,
            outputDbfs = -60f,
            isLimitingActive = false
        )
        Log.i(TAG, "AudioEngine stopped.")
    }

    /**
     * Emergency mute: immediately zeroes output and sets muted flag.
     */
    fun emergencyMute() {
        isMuted = true
        gain = 0.0f
    }

    /**
     * Unmutes and restores nominal gain.
     */
    fun unmute(restoredGain: Float = 1.0f) {
        isMuted = false
        gain = restoredGain.coerceIn(0f, 2.0f)
    }

    private fun createAudioRecord(audioSource: Int, burstFrames: Int): AudioRecord? {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) return null

        // Internal buffer: ensure at least 2 bursts and at least minBufferSize
        val bufferSizeBytes = maxOf(minBufferSize, burstFrames * 2 * 4)

        return try {
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build()

            val builder = AudioRecord.Builder()
                .setAudioSource(audioSource)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSizeBytes)

            builder.build()
        } catch (e: Exception) {
            Log.e(TAG, "createAudioRecord failed with source $audioSource: ${e.message}")
            null
        }
    }

    private fun createAudioTrack(burstFrames: Int): AudioTrack? {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) return null

        val bufferSizeBytes = maxOf(minBufferSize, burstFrames * 2 * 4)

        return try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val builder = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSizeBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }
            builder.build()
        } catch (e: Exception) {
            Log.e(TAG, "createAudioTrack failed: ${e.message}")
            null
        }
    }

    /**
     * Dedicated Real-Time DSP Audio Processing Loop.
     * Operates with minimum possible buffer frames to keep round-trip latency at the hardware floor.
     */
    private fun runAudioLoop(bufferFrames: Int, sourceName: String) {
        val record = audioRecord ?: return
        val track = audioTrack ?: return

        val inBuffer = ShortArray(bufferFrames)
        val outBuffer = ShortArray(bufferFrames)

        try {
            record.startRecording()
            track.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord or AudioTrack: ${e.message}")
            _engineStatus.value = EngineStatus.Error("Hardware stream start failure: ${e.localizedMessage}")
            return
        }

        // Test tone phase accumulator
        var tonePhase = 0.0
        val twoPi = 2.0 * Math.PI

        // Telemetry throttle (every ~35ms)
        var lastTelemetryTimeMs = System.currentTimeMillis()
        var limiterTriggeredInWindow = false

        val estimatedHardwareLatencyMs = (bufferFrames.toFloat() / SAMPLE_RATE) * 2000f // In + Out burst

        while (isRunning && !Thread.currentThread().isInterrupted) {
            // 1. Read raw PCM frame burst from microphone
            val samplesRead = record.read(inBuffer, 0, bufferFrames, AudioRecord.READ_BLOCKING)
            if (samplesRead <= 0) {
                continue
            }

            var sumSquareIn = 0.0
            var sumSquareOut = 0.0

            // Snapshot dynamic parameters
            val currentGain = if (isMuted) 0f else gain
            val invertPhase = isPhaseInverted
            val currentDelayMs = delayMs
            val limiterOn = isLimiterEnabled
            val testToneOn = isTestToneEnabled
            val toneFreq = testToneFrequencyHz

            // Calculate delay in samples (48 samples per millisecond)
            val delaySamples = (currentDelayMs * 48).coerceIn(0, RING_BUFFER_SIZE - bufferFrames - 1)

            // Step size for downsampling to 128 preview points
            val downsampleStep = maxOf(1, samplesRead / 128)

            val tonePhaseIncrement = (twoPi * toneFreq) / SAMPLE_RATE

            // 2. Process every sample in the burst through the DSP pipeline
            for (i in 0 until samplesRead) {
                // Convert 16-bit PCM input to normalized float [-1.0f, 1.0f]
                var inputSampleFloat = inBuffer[i] / 32768.0f

                // If test tone is active, inject a reference sine wave for acoustic calibration
                if (testToneOn) {
                    val toneSample = sin(tonePhase).toFloat() * 0.5f
                    tonePhase += tonePhaseIncrement
                    if (tonePhase >= twoPi) tonePhase -= twoPi
                    inputSampleFloat = (inputSampleFloat * 0.3f) + toneSample
                }

                sumSquareIn += (inputSampleFloat * inputSampleFloat).toDouble()

                // DSP Step 1: Write input sample to Circular Delay Ring Buffer
                ringBuffer[writeHead] = inputSampleFloat

                // DSP Step 2: Read delayed sample from the Ring Buffer
                val readHead = (writeHead - delaySamples + RING_BUFFER_SIZE) and RING_BUFFER_MASK
                var dspSample = ringBuffer[readHead]

                // Advance write pointer
                writeHead = (writeHead + 1) and RING_BUFFER_MASK

                // DSP Step 3: Phase Inversion Toggle (-1.0x polarity flip for anti-phase cancellation)
                if (invertPhase) {
                    dspSample = -dspSample
                }

                // DSP Step 4: Gain Adjustment
                dspSample *= currentGain

                // DSP Step 5: Safety Limiter / Acoustic Feedback Protection
                // Uses hyperbolic tangent (tanh) soft-knee saturation to prevent screeching feedback loops
                if (limiterOn) {
                    val threshold = 0.82f
                    val absVal = abs(dspSample)
                    if (absVal > threshold) {
                        limiterTriggeredInWindow = true
                        val excess = absVal - threshold
                        // Smoothly compress excess using tanh
                        val compressed = threshold + (1.0f - threshold) * tanh(excess.toDouble()).toFloat()
                        dspSample = if (dspSample > 0f) compressed else -compressed
                    }
                    // Hard ceiling safety clamp to avoid DAC wrap-around
                    dspSample = dspSample.coerceIn(-0.98f, 0.98f)
                } else {
                    dspSample = dspSample.coerceIn(-1.0f, 1.0f)
                }

                sumSquareOut += (dspSample * dspSample).toDouble()

                // Convert float back to 16-bit signed PCM short
                val outShort = (dspSample * 32767.0f).toInt().coerceIn(-32768, 32767).toShort()
                outBuffer[i] = outShort

                // Populate live UI oscilloscope buffers (downsampled)
                if (i % downsampleStep == 0) {
                    val uiIdx = (i / downsampleStep).coerceIn(0, 127)
                    uiInputWaveform[uiIdx] = inputSampleFloat
                    uiOutputWaveform[uiIdx] = dspSample
                }
            }

            // 3. Write processed anti-phase burst to low-latency AudioTrack
            track.write(outBuffer, 0, samplesRead, AudioTrack.WRITE_BLOCKING)

            // 4. Periodically publish telemetry to UI (approx. 25-30 fps)
            val now = System.currentTimeMillis()
            if (now - lastTelemetryTimeMs >= 35) {
                val inRms = sqrt(sumSquareIn / samplesRead).toFloat().coerceIn(0f, 1f)
                val outRms = sqrt(sumSquareOut / samplesRead).toFloat().coerceIn(0f, 1f)

                // Convert RMS to dBFS (-60 dB to 0 dB)
                val inDbfs = if (inRms > 0.001f) (20f * log10(inRms)).coerceIn(-60f, 0f) else -60f
                val outDbfs = if (outRms > 0.001f) (20f * log10(outRms)).coerceIn(-60f, 0f) else -60f

                val distanceCm = currentDelayMs * SPEED_OF_SOUND_CM_PER_MS

                _telemetry.value = AudioEngineTelemetry(
                    inputRms = inRms,
                    inputDbfs = inDbfs,
                    outputRms = outRms,
                    outputDbfs = outDbfs,
                    isLimitingActive = limiterTriggeredInWindow,
                    activeSource = sourceName,
                    bufferSizeFrames = bufferFrames,
                    estimatedHardwareLatencyMs = estimatedHardwareLatencyMs,
                    totalDelayMs = currentDelayMs.toFloat(),
                    equivalentDistanceCm = distanceCm,
                    inputWaveform = uiInputWaveform.copyOf(),
                    outputWaveform = uiOutputWaveform.copyOf()
                )

                limiterTriggeredInWindow = false
                lastTelemetryTimeMs = now
            }
        }

        Log.i(TAG, "Audio loop finished.")
    }
}
