package com.example.streamcontrol.core.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

sealed class BleConnectionState {
    data object Disconnected : BleConnectionState()
    data object Connecting : BleConnectionState()
    data object Connected : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

@SuppressLint("MissingPermission")
class BleManager(
    private val context: Context
) {
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _receivedData = MutableStateFlow<String?>(null)
    val receivedData: StateFlow<String?> = _receivedData.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private var isScanning = false
    private var config: BleConnectionConfig? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val currentList = _discoveredDevices.value.toMutableList()
            if (!currentList.contains(device)) {
                currentList.add(device)
                _discoveredDevices.value = currentList
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            _connectionState.value = BleConnectionState.Error("Scan failed with error code: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = BleConnectionState.Connected
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = BleConnectionState.Disconnected
                    closeGatt()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                config?.let { cfg ->
                    val service = gatt.getService(cfg.serviceUuid)
                    txCharacteristic = service?.getCharacteristic(cfg.txCharacteristicUuid)
                    rxCharacteristic = service?.getCharacteristic(cfg.rxCharacteristicUuid)
                    rxCharacteristic?.let { char ->
                        gatt.setCharacteristicNotification(char, true)
                        val descriptor = char.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                        )
                        descriptor?.let {
                            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(it)
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            _receivedData.value = String(value, StandardCharsets.UTF_8)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            characteristic.value?.let { value ->
                _receivedData.value = String(value, StandardCharsets.UTF_8)
            }
        }
    }

    fun startScan() {
        if (isScanning) return
        _discoveredDevices.value = emptyList()
        isScanning = true
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner?.startScan(null, settings, scanCallback)
    }

    fun stopScan() {
        if (!isScanning) return
        isScanning = false
        scanner?.stopScan(scanCallback)
    }

    fun connect(config: BleConnectionConfig, device: BluetoothDevice? = null) {
        this.config = config
        _connectionState.value = BleConnectionState.Connecting

        val targetDevice = device ?: run {
            val discovered = _discoveredDevices.value.find {
                it.name == config.deviceName
            }
            discovered
        }

        if (targetDevice == null) {
            _connectionState.value = BleConnectionState.Error("Device not found: ${config.deviceName}")
            return
        }

        bluetoothGatt = targetDevice.connectGatt(
            context,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        closeGatt()
        _connectionState.value = BleConnectionState.Disconnected
    }

    fun sendData(data: String) {
        txCharacteristic?.let { char ->
            char.value = data.toByteArray(StandardCharsets.UTF_8)
            bluetoothGatt?.writeCharacteristic(char)
        }
    }

    fun clearReceivedData() {
        _receivedData.value = null
    }

    private fun closeGatt() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        txCharacteristic = null
        rxCharacteristic = null
    }

    fun release() {
        stopScan()
        disconnect()
    }
}