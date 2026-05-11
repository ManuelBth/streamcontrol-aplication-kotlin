package com.example.streamcontrol.features.control

import com.example.streamcontrol.domain.model.ImcSettings
import com.example.streamcontrol.domain.model.PidSettings
import com.example.streamcontrol.domain.model.RstSettings

data class ControlState(
    val pidSettings: PidSettings = PidSettings.default(),
    val imcSettings: ImcSettings = ImcSettings.default(),
    val rstSettings: RstSettings = RstSettings.default(),
    val isSaving: Boolean = false,
    val isSyncing: Boolean = false,
    val syncSuccess: Boolean = false,
    val errorMessage: String? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}