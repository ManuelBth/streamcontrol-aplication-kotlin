package com.example.streamcontrol.core.scope

sealed class AppScope(val name: String) {
    data object Home : AppScope("home")
    data object Control : AppScope("control")
    data object Files : AppScope("files")
    data object Config : AppScope("config")
    data object Ble : AppScope("ble")
    data object Storage : AppScope("storage")

    companion object {
        private val allScopes = listOf(Home, Control, Files, Config, Ble, Storage)

        fun fromFeature(featureName: String): AppScope {
            return allScopes.find { it.name == featureName } ?: Home
        }
    }
}

object AppScopes {
    val allScopes = listOf(
        AppScope.Home,
        AppScope.Control,
        AppScope.Files,
        AppScope.Config,
        AppScope.Ble,
        AppScope.Storage
    )

    fun getScopeForFeature(feature: String): AppScope = AppScope.fromFeature(feature)

    fun getScopeForClass(className: String): AppScope {
        return when {
            className.contains("Home", ignoreCase = true) -> AppScope.Home
            className.contains("Control", ignoreCase = true) -> AppScope.Control
            className.contains("Files", ignoreCase = true) -> AppScope.Files
            className.contains("Config", ignoreCase = true) -> AppScope.Config
            className.contains("Ble", ignoreCase = true) -> AppScope.Ble
            className.contains("Storage", ignoreCase = true) -> AppScope.Storage
            else -> AppScope.Home
        }
    }
}