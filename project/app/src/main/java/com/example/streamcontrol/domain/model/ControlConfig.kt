package com.example.streamcontrol.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ControlConfig(
    val controllers: Controllers,
    val connection: ConnectionConfig
) {
    companion object {
        fun default() = ControlConfig(
            controllers = Controllers.default(),
            connection = ConnectionConfig.default()
        )
    }
}