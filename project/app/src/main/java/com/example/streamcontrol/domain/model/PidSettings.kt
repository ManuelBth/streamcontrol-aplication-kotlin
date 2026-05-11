package com.example.streamcontrol.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PidSettings(
    val proportionalGain: Double,
    val integralTime: Double,
    val derivativeTime: Double,
    val setpoint: Double
) {
    init {
        require(proportionalGain in 0.0..100.0) { "Kp must be between 0 and 100" }
        require(integralTime in 0.0..100.0) { "Ki must be between 0 and 100" }
        require(derivativeTime in 0.0..100.0) { "Kd must be between 0 and 100" }
        require(setpoint in 0.0..120.0) { "Setpoint must be between 0 and 120" }
    }

    companion object {
        fun default() = PidSettings(0.0, 0.0, 0.0, 0.0)
    }
}