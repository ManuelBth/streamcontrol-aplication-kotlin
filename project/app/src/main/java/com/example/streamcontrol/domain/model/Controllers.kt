package com.example.streamcontrol.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Controllers(
    val pid: PidSettings,
    val imc: ImcSettings,
    val rst: RstSettings
) {
    companion object {
        fun default() = Controllers(
            pid = PidSettings.default(),
            imc = ImcSettings.default(),
            rst = RstSettings.default()
        )
    }
}