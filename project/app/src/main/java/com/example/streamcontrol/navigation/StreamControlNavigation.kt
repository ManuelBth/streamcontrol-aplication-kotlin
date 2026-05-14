package com.example.streamcontrol.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.streamcontrol.core.ble.BleManager
import com.example.streamcontrol.core.storage.CsvFileManager
import com.example.streamcontrol.core.storage.GlobalConfigStorage
import com.example.streamcontrol.features.config.ConfigScreen
import com.example.streamcontrol.features.config.ConfigViewModel
import com.example.streamcontrol.features.control.ControlScreen
import com.example.streamcontrol.features.control.ControlViewModel
import com.example.streamcontrol.features.files.FilesScreen
import com.example.streamcontrol.features.files.FilesViewModel
import com.example.streamcontrol.features.home.HomeScreen
import com.example.streamcontrol.features.home.HomeViewModel
import com.example.streamcontrol.features.vista.VistaScreen
import com.example.streamcontrol.features.vista.VistaViewModel

@Composable
fun StreamControlNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current

    val bleManager = remember { BleManager(context) }
    val configStorage = remember { GlobalConfigStorage(context) }
    val csvFileManager = remember { CsvFileManager(context) }
    val configViewModel = remember {
        ConfigViewModel.Factory(bleManager, configStorage).create(ConfigViewModel::class.java)
    }
    val controlViewModel = remember {
        ControlViewModel.Factory(bleManager, configStorage).create(ControlViewModel::class.java)
    }
    val homeViewModel = remember {
        HomeViewModel.Factory(bleManager, csvFileManager).create(HomeViewModel::class.java)
    }
    val filesViewModel = remember {
        FilesViewModel.Factory(csvFileManager).create(FilesViewModel::class.java)
    }
    val vistaViewModel = remember { VistaViewModel() }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(viewModel = homeViewModel) }
            composable(Screen.Control.route) { ControlScreen(viewModel = controlViewModel) }
            composable(Screen.Files.route) { FilesScreen(viewModel = filesViewModel) }
            composable(Screen.Vista.route) { VistaScreen(viewModel = vistaViewModel) }
            composable(Screen.Config.route) { ConfigScreen(viewModel = configViewModel) }
        }
    }
}