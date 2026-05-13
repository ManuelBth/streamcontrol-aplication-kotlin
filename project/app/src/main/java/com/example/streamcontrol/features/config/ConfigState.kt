package com.example.streamcontrol.features.config

import android.bluetooth.BluetoothDevice

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val device: BluetoothDevice
)

data class ConfigState(
    val deviceName: String = "",
    val bleServiceUuid: String = "0000FFF0-0000-1000-8000-00805F9B34FB",
    val bleTxCharacteristicUuid: String = "0000FFF1-0000-1000-8000-00805F9B34FB",
    val bleRxCharacteristicUuid: String = "0000FFF2-0000-1000-8000-00805F9B34FB",
    val sampleIntervalMs: Int = 1000,
    val maxControlDurationMs: Int = 60000,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isScanning: Boolean = false,
    val discoveredDevices: List<DiscoveredDevice> = emptyList(),
    val errorMessage: String? = null
)