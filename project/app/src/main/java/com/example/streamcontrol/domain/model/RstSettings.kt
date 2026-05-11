package com.example.streamcontrol.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RstSettings(
    val feedbackCoeffs: List<Double>,
    val controlActionCoeffs: List<Double>,
    val feedforwardCoeffs: List<Double>,
    val plantPoles: List<Double>,
    val plantZeros: List<Double>,
    val desiredPoles: List<Double>,
    val hasIntegrator: Boolean
) {
    init {
        require(feedbackCoeffs.isNotEmpty()) { "R coefficients must not be empty" }
        require(controlActionCoeffs.isNotEmpty()) { "S coefficients must not be empty" }
        require(feedforwardCoeffs.isNotEmpty()) { "T coefficients must not be empty" }
        require(plantPoles.isNotEmpty()) { "A coefficients must not be empty" }
        require(plantZeros.isNotEmpty()) { "B coefficients must not be empty" }
        require(desiredPoles.isNotEmpty()) { "P coefficients must not be empty" }
    }

    companion object {
        fun default() = RstSettings(
            feedbackCoeffs = listOf(1.0),
            controlActionCoeffs = listOf(1.0),
            feedforwardCoeffs = listOf(1.0),
            plantPoles = listOf(1.0),
            plantZeros = listOf(1.0),
            desiredPoles = listOf(1.0),
            hasIntegrator = false
        )
    }
}