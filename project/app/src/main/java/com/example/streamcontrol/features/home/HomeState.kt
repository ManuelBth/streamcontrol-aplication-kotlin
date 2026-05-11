package com.example.streamcontrol.features.home

import com.example.streamcontrol.domain.model.ProcessSample

data class HomeState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val controlState: ControlState = ControlState.IDLE,
    val isRunning: Boolean = false,
    val elapsedTimeMs: Long = 0,
    val perturbationEnabled: Boolean = false,
    val dataBuffer: List<ProcessSample> = emptyList(),
    val selectedVariable: GraphVariable = GraphVariable.TEMPERATURE,
    val errorMessage: String? = null,
    val showSaveDialog: Boolean = false,
    val showDisconnectAlert: Boolean = false
)

enum class ControlState {
    IDLE,
    STARTING,
    RUNNING,
    STOPPING
}

enum class GraphVariable {
    TEMPERATURE,
    FIRING_ANGLE,
    FAN_PWM
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}