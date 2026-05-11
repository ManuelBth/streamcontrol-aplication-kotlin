package com.example.streamcontrol.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ImcSettings(
    val processGain: Double,
    val timeConstant: Double,
    val deadTime: Double,
    val lambda: Double
) {
    init {
        require(processGain > 0.0) { "K must be greater than 0" }
        require(timeConstant > 0.0) { "τ must be greater than 0" }
        require(deadTime >= 0.0) { "θ must be non-negative" }
        require(lambda > 0.0) { "λ must be greater than 0" }
    }

    companion object {
        fun default() = ImcSettings(1.0, 1.0, 0.0, 1.0)
    }
}