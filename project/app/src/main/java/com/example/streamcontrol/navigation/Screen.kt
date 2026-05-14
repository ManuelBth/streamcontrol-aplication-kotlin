package com.example.streamcontrol.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "Inicio",
        icon = Icons.Filled.Home
    )

    data object Control : Screen(
        route = "control",
        title = "Control",
        icon = Icons.Filled.Settings
    )

    data object Files : Screen(
        route = "files",
        title = "Archivos",
        icon = Icons.Filled.Folder
    )

    data object Config : Screen(
        route = "config",
        title = "Config",
        icon = Icons.Filled.Bluetooth
    )

    data object Vista : Screen(
        route = "vista",
        title = "Vista",
        icon = Icons.Filled.Timeline
    )
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Control,
    Screen.Vista,
    Screen.Files,
    Screen.Config
)
