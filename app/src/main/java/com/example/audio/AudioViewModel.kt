package com.example.audio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class AudioViewModel(application: Application) : AndroidViewModel(application) {

    private val audioEngine = AudioEngine(application.applicationContext)

    val engineStatus: StateFlow<EngineStatus> = audioEngine.engineStatus
    val telemetry: StateFlow<AudioEngineTelemetry> = audioEngine.telemetry

    // UI state mirrors
    private val _gain = MutableStateFlow(1.0f)
    val gain: StateFlow<Float> = _gain.asStateFlow()

    private val _isPhaseInverted = MutableStateFlow(true) // Default to Anti-Phase for ANC demo
    val isPhaseInverted: StateFlow<Boolean> = _isPhaseInverted.asStateFlow()

    private val _delayMs = MutableStateFlow(0)
    val delayMs: StateFlow<Int> = _delayMs.asStateFlow()

    private val _isLimiterEnabled = MutableStateFlow(true)
    val isLimiterEnabled: StateFlow<Boolean> = _isLimiterEnabled.asStateFlow()

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

    fun setDelayMs(ms: Int) {
        val clamped = ms.coerceIn(0, 100)
        _delayMs.value = clamped
        audioEngine.delayMs = clamped
    }

    fun setLimiterEnabled(enabled: Boolean) {
        _isLimiterEnabled.value = enabled
        audioEngine.isLimiterEnabled = enabled
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
    }
}
