package com.example.streamcontrol.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ConnectionConfig(
    val deviceName: String,
    val pairingPin: String,
    val bleServiceUuid: String,
    val bleTxCharacteristicUuid: String,
    val bleRxCharacteristicUuid: String,
    val sampleIntervalMs: Int,
    val maxControlDurationMs: Int
) {
    init {
        require(sampleIntervalMs in listOf(1000, 2000)) { "Sample interval must be 1000 or 2000 ms" }
        require(maxControlDurationMs in 0..120000) { "Max control duration must be between 0 and 120000 ms" }
    }

    companion object {
        val DEFAULT_SERVICE_UUID = "0000FFF0-0000-1000-8000-00805F9B34FB"
        val DEFAULT_TX_CHAR_UUID = "0000FFF1-0000-1000-8000-00805F9B34FB"
        val DEFAULT_RX_CHAR_UUID = "0000FFF2-0000-1000-8000-00805F9B34FB"

        fun default() = ConnectionConfig(
            deviceName = "",
            pairingPin = "",
            bleServiceUuid = DEFAULT_SERVICE_UUID,
            bleTxCharacteristicUuid = DEFAULT_TX_CHAR_UUID,
            bleRxCharacteristicUuid = DEFAULT_RX_CHAR_UUID,
            sampleIntervalMs = 1000,
            maxControlDurationMs = 60000
        )
    }
}