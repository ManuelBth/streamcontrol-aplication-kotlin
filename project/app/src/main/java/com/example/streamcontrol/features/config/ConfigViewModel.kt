package com.example.streamcontrol.features.config

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.streamcontrol.core.ble.BleConnectionConfig
import com.example.streamcontrol.core.ble.BleConnectionState
import com.example.streamcontrol.core.ble.BleManager
import com.example.streamcontrol.core.storage.GlobalConfigStorage
import com.example.streamcontrol.domain.model.ConnectionConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ConfigViewModel(
    private val bleManager: BleManager,
    private val configStorage: GlobalConfigStorage
) : ViewModel() {

    private val _state = MutableStateFlow(ConfigState())
    val state: StateFlow<ConfigState> = _state.asStateFlow()

    init {
        observeBleState()
        loadSavedConfig()
    }

    private fun observeBleState() {
        viewModelScope.launch {
            bleManager.connectionState.collect { bleState ->
                val newConnectionState = when (bleState) {
                    is BleConnectionState.Disconnected -> ConnectionState.DISCONNECTED
                    is BleConnectionState.Connecting -> ConnectionState.CONNECTING
                    is BleConnectionState.Connected -> ConnectionState.CONNECTED
                    is BleConnectionState.Error -> {
                        _state.update { it.copy(errorMessage = bleState.message) }
                        ConnectionState.ERROR
                    }
                }
                _state.update {
                    it.copy(
                        connectionState = newConnectionState,
                        isScanning = false
                    )
                }
            }
        }

        viewModelScope.launch {
            bleManager.discoveredDevices.collect { devices ->
                val discoveredDevices = devices.map { device ->
                    DiscoveredDevice(
                        name = device.name ?: "Unknown",
                        address = device.address,
                        device = device
                    )
                }
                _state.update { it.copy(discoveredDevices = discoveredDevices) }
            }
        }
    }

    private fun loadSavedConfig() {
        viewModelScope.launch {
            val config = configStorage.connectionConfigFlow.first()
            _state.update {
                it.copy(
                    deviceName = config.deviceName,
                    bleServiceUuid = config.bleServiceUuid,
                    bleTxCharacteristicUuid = config.bleTxCharacteristicUuid,
                    bleRxCharacteristicUuid = config.bleRxCharacteristicUuid,
                    sampleIntervalMs = config.sampleIntervalMs,
                    maxControlDurationMs = config.maxControlDurationMs
                )
            }
        }
    }

    fun updateDeviceName(name: String) {
        _state.update { it.copy(deviceName = name) }
    }

    fun updateBleServiceUuid(uuid: String) {
        _state.update { it.copy(bleServiceUuid = uuid) }
    }

    fun updateBleTxCharacteristicUuid(uuid: String) {
        _state.update { it.copy(bleTxCharacteristicUuid = uuid) }
    }

    fun updateBleRxCharacteristicUuid(uuid: String) {
        _state.update { it.copy(bleRxCharacteristicUuid = uuid) }
    }

    fun updateSampleInterval(interval: Int) {
        _state.update { it.copy(sampleIntervalMs = interval) }
    }

    fun updateMaxControlDuration(duration: Int) {
        _state.update { it.copy(maxControlDurationMs = duration.coerceIn(0, 120000)) }
    }

    fun saveConfig() {
        viewModelScope.launch {
            val currentState = _state.value
            val connectionConfig = ConnectionConfig(
                deviceName = currentState.deviceName,
                bleServiceUuid = currentState.bleServiceUuid,
                bleTxCharacteristicUuid = currentState.bleTxCharacteristicUuid,
                bleRxCharacteristicUuid = currentState.bleRxCharacteristicUuid,
                sampleIntervalMs = currentState.sampleIntervalMs,
                maxControlDurationMs = currentState.maxControlDurationMs
            )
            configStorage.saveConnectionConfig(connectionConfig)
        }
    }

    fun startScan() {
        _state.update { it.copy(isScanning = true, discoveredDevices = emptyList(), errorMessage = null) }
        bleManager.startScan()
    }

    fun stopScan() {
        bleManager.stopScan()
        _state.update { it.copy(isScanning = false) }
    }

    fun connectToDevice(device: DiscoveredDevice? = null) {
        val currentState = _state.value
        val bleConfig = BleConnectionConfig(
            serviceUuid = UUID.fromString(currentState.bleServiceUuid),
            txCharacteristicUuid = UUID.fromString(currentState.bleTxCharacteristicUuid),
            rxCharacteristicUuid = UUID.fromString(currentState.bleRxCharacteristicUuid),
            deviceName = currentState.deviceName
        )

        saveConfig()
        bleManager.connect(bleConfig, device?.device)
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.release()
    }

    class Factory(
        private val bleManager: BleManager,
        private val configStorage: GlobalConfigStorage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ConfigViewModel::class.java)) {
                return ConfigViewModel(bleManager, configStorage) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}