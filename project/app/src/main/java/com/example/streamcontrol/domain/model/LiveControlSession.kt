package com.example.streamcontrol.domain.model

data class LiveControlSession(
    val dataBuffer: ArrayList<ProcessSample>,
    val isRunning: Boolean,
    val elapsedTimeMs: Long
) {
    companion object {
        const val MAX_SAMPLES = 2400

        fun empty() = LiveControlSession(
            dataBuffer = ArrayList(MAX_SAMPLES),
            isRunning = false,
            elapsedTimeMs = 0L
        )
    }
}