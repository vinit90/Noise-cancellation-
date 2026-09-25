package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Representation of an audio hardware endpoint (mic, speaker, Bluetooth, headset).
 */
data class AudioDeviceItem(
    val id: Int,
    val name: String,
    val type: Int,
    val isInput: Boolean,
    val isBluetooth: Boolean,
    val isBuiltIn: Boolean,
    val description: String
)

class AudioDeviceManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioDeviceManager"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _availableInputs = MutableStateFlow<List<AudioDeviceItem>>(emptyList())
    val availableInputs: StateFlow<List<AudioDeviceItem>> = _availableInputs.asStateFlow()

    private val _availableOutputs = MutableStateFlow<List<AudioDeviceItem>>(emptyList())
    val availableOutputs: StateFlow<List<AudioDeviceItem>> = _availableOutputs.asStateFlow()

    private val _selectedInputId = MutableStateFlow<Int?>(null)
    val selectedInputId: StateFlow<Int?> = _selectedInputId.asStateFlow()

    private val _selectedOutputId = MutableStateFlow<Int?>(null)
    val selectedOutputId: StateFlow<Int?> = _selectedOutputId.asStateFlow()

    private val _isBluetoothOutputConnected = MutableStateFlow(false)
    val isBluetoothOutputConnected: StateFlow<Boolean> = _isBluetoothOutputConnected.asStateFlow()

    private val _isFeedbackRiskHigh = MutableStateFlow(true)
    val isFeedbackRiskHigh: StateFlow<Boolean> = _isFeedbackRiskHigh.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices added: ${addedDevices?.size}")
            refreshDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices removed: ${removedDevices?.size}")
            refreshDevices()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler)
        refreshDevices()
    }

    fun release() {
        try {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering audio device callback: ${e.message}")
        }
    }

    /**
     * Enumerates connected hardware devices and categorizes them into Inputs and Outputs.
     */
    fun refreshDevices() {
        val inputDevices = mutableListOf<AudioDeviceItem>()
        val outputDevices = mutableListOf<AudioDeviceItem>()

        var hasBtOutput = false

        try {
            val allDevices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)
            for (device in allDevices) {
                val isInput = device.isSource
                val isBt = isBluetoothDevice(device.type)
                val isBuiltIn = isBuiltInDevice(device.type)
                val friendlyName = getDeviceDisplayName(device)
                val desc = getDeviceTypeDescription(device.type)

                val item = AudioDeviceItem(
                    id = device.id,
                    name = friendlyName,
                    type = device.type,
                    isInput = isInput,
                    isBluetooth = isBt,
                    isBuiltIn = isBuiltIn,
                    description = desc
                )

                if (isInput) {
                    inputDevices.add(item)
                } else {
                    outputDevices.add(item)
                    if (isBt) hasBtOutput = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying audio devices: ${e.message}")
        }

        _availableInputs.value = inputDevices
        _availableOutputs.value = outputDevices
        _isBluetoothOutputConnected.value = hasBtOutput

        // Check if selected devices are still connected
        val currentIn = _selectedInputId.value
        if (currentIn != null && inputDevices.none { it.id == currentIn }) {
            _selectedInputId.value = null // Revert to system default
        }

        val currentOut = _selectedOutputId.value
        if (currentOut != null && outputDevices.none { it.id == currentOut }) {
            _selectedOutputId.value = null // Revert to system default
        }

        updateFeedbackRisk()
    }

    fun selectInput(deviceId: Int?) {
        _selectedInputId.value = deviceId
        updateFeedbackRisk()
    }

    fun selectOutput(deviceId: Int?) {
        _selectedOutputId.value = deviceId
        updateFeedbackRisk()
    }

    /**
     * Applies the user's preferred input device to the active AudioRecord.
     */
    fun applyPreferredInput(record: AudioRecord?): Boolean {
        if (record == null) return false
        val deviceId = _selectedInputId.value
        if (deviceId == null) {
            return record.setPreferredDevice(null)
        }
        val target = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull { it.id == deviceId }
        return if (target != null) {
            val success = record.setPreferredDevice(target)
            Log.i(TAG, "Applied preferred input: ${target.productName} -> $success")
            success
        } else {
            record.setPreferredDevice(null)
            false
        }
    }

    /**
     * Applies the user's preferred output device to the active AudioTrack.
     */
    fun applyPreferredOutput(track: AudioTrack?): Boolean {
        if (track == null) return false
        val deviceId = _selectedOutputId.value
        if (deviceId == null) {
            return track.setPreferredDevice(null)
        }
        val target = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.id == deviceId }
        return if (target != null) {
            val success = track.setPreferredDevice(target)
            Log.i(TAG, "Applied preferred output: ${target.productName} -> $success")
            success
        } else {
            track.setPreferredDevice(null)
            false
        }
    }

    /**
     * Launches the system Bluetooth Settings page so the user can quickly pair/connect a speaker.
     */
    fun openBluetoothSettings() {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Bluetooth settings: ${e.message}")
        }
    }

    private fun updateFeedbackRisk() {
        val selectedOut = _availableOutputs.value.firstOrNull { it.id == _selectedOutputId.value }
        // If selected output is Bluetooth or Wired Headphones, the transducers are physically isolated!
        val isOutputIsolated = selectedOut?.let {
            it.isBluetooth || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        } ?: false

        // High feedback risk occurs when playing through phone loudspeaker while listening with phone mic
        _isFeedbackRiskHigh.value = !isOutputIsolated
    }

    private fun isBluetoothDevice(type: Int): Boolean {
        return when (type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLE_BROADCAST -> true
            else -> false
        }
    }

    private fun isBuiltInDevice(type: Int): Boolean {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> true
            else -> false
        }
    }

    private fun getDeviceDisplayName(device: AudioDeviceInfo): String {
        val prodName = device.productName?.toString()?.trim()
        if (!prodName.isNullOrEmpty() && prodName != "null") {
            return prodName
        }
        return when (device.type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Phone Built-in Mic"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone Loudspeaker"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Phone Earpiece"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Speaker / Audio"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Voice Headset"
            AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE Headset"
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> "BLE Speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
            AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio Device"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            else -> "Audio Device #${device.id}"
        }
    }

    private fun getDeviceTypeDescription(type: Int): String {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Onboard chassis microphone (close to speaker)"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Onboard chassis speaker (causes mic feedback)"
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Top earpiece receiver"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Wireless Bluetooth A2DP audio output"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Wireless Bluetooth bidirectional headset"
            AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth Low Energy headset"
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth Low Energy speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "3.5mm / wired headset with mic"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "3.5mm / wired stereo headphones"
            AudioDeviceInfo.TYPE_USB_HEADSET -> "Type-C / USB digital headset"
            else -> "Hardware audio endpoint"
        }
    }
}
