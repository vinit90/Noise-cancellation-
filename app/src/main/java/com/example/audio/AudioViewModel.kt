package com.example.audio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioViewModel(application: Application) : AndroidViewModel(application) {

    private val audioEngine = AudioEngine(application.applicationContext)

    val engineStatus: StateFlow<EngineStatus> = audioEngine.engineStatus
    val telemetry: StateFlow<AudioEngineTelemetry> = audioEngine.telemetry

    // Audio Hardware Routing & BLE states from AudioDeviceManager
    val availableInputs: StateFlow<List<AudioDeviceItem>> = audioEngine.deviceManager.availableInputs
    val availableOutputs: StateFlow<List<AudioDeviceItem>> = audioEngine.deviceManager.availableOutputs
    val selectedInputId: StateFlow<Int?> = audioEngine.deviceManager.selectedInputId
    val selectedOutputId: StateFlow<Int?> = audioEngine.deviceManager.selectedOutputId
    val isBluetoothOutputConnected: StateFlow<Boolean> = audioEngine.deviceManager.isBluetoothOutputConnected
    val isFeedbackRiskHigh: StateFlow<Boolean> = audioEngine.deviceManager.isFeedbackRiskHigh

    // UI state mirrors
    private val _gain = MutableStateFlow(1.0f)
    val gain: StateFlow<Float> = _gain.asStateFlow()

    private val _isPhaseInverted = MutableStateFlow(true) // Default to Anti-Phase for ANC demo
    val isPhaseInverted: StateFlow<Boolean> = _isPhaseInverted.asStateFlow()

    private val _delayMs = MutableStateFlow(0)
    val delayMs: StateFlow<Int> = _delayMs.asStateFlow()

    private val _isLimiterEnabled = MutableStateFlow(true)
    val isLimiterEnabled: StateFlow<Boolean> = _isLimiterEnabled.asStateFlow()

    private val _isFeedbackShieldEnabled = MutableStateFlow(true)
    val isFeedbackShieldEnabled: StateFlow<Boolean> = _isFeedbackShieldEnabled.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isTestToneEnabled = MutableStateFlow(false)
    val isTestToneEnabled: StateFlow<Boolean> = _isTestToneEnabled.asStateFlow()

    private val _testToneFrequency = MutableStateFlow(440f)
    val testToneFrequency: StateFlow<Float> = _testToneFrequency.asStateFlow()

    init {
        // Sync initial state to audio engine
        audioEngine.gain = _gain.value
        audioEngine.isPhaseInverted = _isPhaseInverted.value
        audioEngine.delayMs = _delayMs.value
        audioEngine.isLimiterEnabled = _isLimiterEnabled.value
        audioEngine.isFeedbackShieldEnabled = _isFeedbackShieldEnabled.value
        audioEngine.isMuted = _isMuted.value
        audioEngine.isTestToneEnabled = _isTestToneEnabled.value
        audioEngine.testToneFrequencyHz = _testToneFrequency.value
    }

    fun toggleEngine() {
        if (engineStatus.value is EngineStatus.Running) {
            stopEngine()
        } else {
            startEngine()
        }
    }

    fun startEngine() {
        audioEngine.start()
    }

    fun stopEngine() {
        audioEngine.stop()
    }

    fun setGain(newGain: Float) {
        val clamped = newGain.coerceIn(0.0f, 2.0f)
        _gain.value = clamped
        if (clamped > 0.01f && _isMuted.value) {
            _isMuted.value = false
            audioEngine.isMuted = false
        }
        audioEngine.gain = clamped
    }

    fun setPhaseInverted(inverted: Boolean) {
        _isPhaseInverted.value = inverted
        audioEngine.isPhaseInverted = inverted
    }

    /**
     * Delay in milliseconds. Supports up to 200ms to allow fine alignment
     * across acoustic flight time (34.3cm/ms) as well as Bluetooth speaker buffer latency.
     */
    fun setDelayMs(ms: Int) {
        val clamped = ms.coerceIn(0, 200)
        _delayMs.value = clamped
        audioEngine.delayMs = clamped
    }

    fun setLimiterEnabled(enabled: Boolean) {
        _isLimiterEnabled.value = enabled
        audioEngine.isLimiterEnabled = enabled
    }

    fun setFeedbackShieldEnabled(enabled: Boolean) {
        _isFeedbackShieldEnabled.value = enabled
        audioEngine.isFeedbackShieldEnabled = enabled
    }

    fun selectInputDevice(deviceId: Int?) {
        audioEngine.selectPreferredInput(deviceId)
    }

    fun selectOutputDevice(deviceId: Int?) {
        audioEngine.selectPreferredOutput(deviceId)
    }

    fun openBluetoothSettings() {
        audioEngine.deviceManager.openBluetoothSettings()
    }

    fun refreshAudioDevices() {
        audioEngine.deviceManager.refreshDevices()
    }

    fun emergencyMute() {
        _isMuted.value = true
        _gain.value = 0.0f
        audioEngine.emergencyMute()
    }

    fun unmute() {
        _isMuted.value = false
        _gain.value = 1.0f
        audioEngine.unmute(1.0f)
    }

    fun toggleTestTone() {
        val next = !_isTestToneEnabled.value
        _isTestToneEnabled.value = next
        audioEngine.isTestToneEnabled = next
    }

    fun setTestToneFrequency(freq: Float) {
        _testToneFrequency.value = freq
        audioEngine.testToneFrequencyHz = freq
    }

    /**
     * Called when the app lifecycle enters background (onStop / onPause)
     * to guarantee microphone access and speaker playback stop immediately.
     */
    fun onAppBackgrounded() {
        if (engineStatus.value is EngineStatus.Running) {
            stopEngine()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
        audioEngine.deviceManager.release()
    }
}
