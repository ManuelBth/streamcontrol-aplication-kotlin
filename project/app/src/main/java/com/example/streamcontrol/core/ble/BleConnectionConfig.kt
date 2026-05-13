package com.example.streamcontrol.core.ble

import java.util.UUID

data class BleConnectionConfig(
    val serviceUuid: UUID,
    val txCharacteristicUuid: UUID,
    val rxCharacteristicUuid: UUID,
    val deviceName: String
) {
    companion object {
        val DEFAULT_SERVICE_UUID: UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB")
        val DEFAULT_TX_CHAR_UUID: UUID = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB")
        val DEFAULT_RX_CHAR_UUID: UUID = UUID.fromString("0000FFF2-0000-1000-8000-00805F9B34FB")

        fun default() = BleConnectionConfig(
            serviceUuid = DEFAULT_SERVICE_UUID,
            txCharacteristicUuid = DEFAULT_TX_CHAR_UUID,
            rxCharacteristicUuid = DEFAULT_RX_CHAR_UUID,
            deviceName = ""
        )
    }
}