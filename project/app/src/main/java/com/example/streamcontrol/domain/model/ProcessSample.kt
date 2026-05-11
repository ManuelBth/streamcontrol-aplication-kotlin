package com.example.streamcontrol.domain.model

data class ProcessSample(
    val elapsedTimeMs: Double,
    val temperature: Double,
    val firingAngle: Double,
    val fanPwm: Int
)