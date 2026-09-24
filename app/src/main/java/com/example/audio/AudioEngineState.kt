package com.example.audio

/**
 * Real-time telemetry snapshot pushed from the audio DSP loop to the UI.
 */
data class AudioEngineTelemetry(
    val inputRms: Float = 0f,
    val inputDbfs: Float = -60f,
    val outputRms: Float = 0f,
    val outputDbfs: Float = -60f,
    val isLimitingActive: Boolean = false,
    val activeSource: String = "UNPROCESSED",
    val bufferSizeFrames: Int = 192,
    val estimatedHardwareLatencyMs: Float = 5.0f,
    val totalDelayMs: Float = 0f,
    val equivalentDistanceCm: Float = 0f,
    val inputWaveform: FloatArray = FloatArray(128),
    val outputWaveform: FloatArray = FloatArray(128)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioEngineTelemetry

        if (inputRms != other.inputRms) return false
        if (inputDbfs != other.inputDbfs) return false
        if (outputRms != other.outputRms) return false
        if (outputDbfs != other.outputDbfs) return false
        if (isLimitingActive != other.isLimitingActive) return false
        if (activeSource != other.activeSource) return false
        if (bufferSizeFrames != other.bufferSizeFrames) return false
        if (estimatedHardwareLatencyMs != other.estimatedHardwareLatencyMs) return false
        if (totalDelayMs != other.totalDelayMs) return false
        if (equivalentDistanceCm != other.equivalentDistanceCm) return false
        if (!inputWaveform.contentEquals(other.inputWaveform)) return false
        if (!outputWaveform.contentEquals(other.outputWaveform)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = inputRms.hashCode()
        result = 31 * result + inputDbfs.hashCode()
        result = 31 * result + outputRms.hashCode()
        result = 31 * result + outputDbfs.hashCode()
        result = 31 * result + isLimitingActive.hashCode()
        result = 31 * result + activeSource.hashCode()
        result = 31 * result + bufferSizeFrames
        result = 31 * result + estimatedHardwareLatencyMs.hashCode()
        result = 31 * result + totalDelayMs.hashCode()
        result = 31 * result + equivalentDistanceCm.hashCode()
        result = 31 * result + inputWaveform.contentHashCode()
        result = 31 * result + outputWaveform.contentHashCode()
        return result
    }
}

sealed interface EngineStatus {
    data object Idle : EngineStatus
    data object Starting : EngineStatus
    data object Running : EngineStatus
    data class Error(val message: String) : EngineStatus
}
