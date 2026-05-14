package com.example.streamcontrol.features.vista

data class VistaState(
    val power: Int = 60,
    val frequency: Int = 50
) {
    val alphaDegrees: Float
        get() = (Math.PI * (1 - power / 100.0) * 180.0 / Math.PI).toFloat()

    val fireDelayMs: Float
        get() {
            val semi = 1.0 / (frequency * 2)
            return (semi * (1 - power / 100.0) * 1000).toFloat()
        }

    val autonomousConductionMs: Float
        get() {
            val semi = 1.0 / (frequency * 2)
            val fireT = semi * (1 - power / 100.0)
            return ((semi - fireT) * 1000).toFloat()
        }

    val rmsPower: Int
        get() {
            val alpha = Math.PI * (1 - power / 100.0)
            val rmsRatio = kotlin.math.sqrt(
                maxOf(0.0, 0.5 - alpha / (2 * Math.PI) + kotlin.math.sin(2 * alpha) / (4 * Math.PI))
            )
            return minOf(100, (rmsRatio * 141).toInt())
        }
}