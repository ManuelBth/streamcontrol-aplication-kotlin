package com.example.streamcontrol.features.config

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType.Companion.Text
import androidx.compose.ui.unit.dp
import com.example.streamcontrol.ui.components.StreamControlButton
import com.example.streamcontrol.ui.components.StreamControlDropdown
import com.example.streamcontrol.ui.components.StreamControlOutlinedButton
import com.example.streamcontrol.ui.components.StreamControlTextField

@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAdvancedConfig by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "CONEXIÓN BLE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                StreamControlTextField(
                    value = state.deviceName,
                    onValueChange = viewModel::updateDeviceName,
                    label = "Dispositivo",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardType = Text
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedConfig = !showAdvancedConfig },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Configuración avanzada",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = if (showAdvancedConfig) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (showAdvancedConfig) "Colapsar" else "Expandir"
                    )
                }
            }

            item {
                AnimatedVisibility(visible = showAdvancedConfig) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StreamControlTextField(
                            value = state.bleServiceUuid,
                            onValueChange = viewModel::updateBleServiceUuid,
                            label = "Servicio UUID",
                            modifier = Modifier.fillMaxWidth()
                        )
                        StreamControlTextField(
                            value = state.bleTxCharacteristicUuid,
                            onValueChange = viewModel::updateBleTxCharacteristicUuid,
                            label = "TX Char UUID",
                            modifier = Modifier.fillMaxWidth()
                        )
                        StreamControlTextField(
                            value = state.bleRxCharacteristicUuid,
                            onValueChange = viewModel::updateBleRxCharacteristicUuid,
                            label = "RX Char UUID",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                StreamControlDropdown(
                    label = "Intervalo (ms)",
                    selectedValue = state.sampleIntervalMs.toString(),
                    options = listOf("1000", "2000"),
                    onValueChange = { viewModel.updateSampleInterval(it.toIntOrNull() ?: 1000) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                StreamControlTextField(
                    value = state.maxControlDurationMs.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let { duration -> viewModel.updateMaxControlDuration(duration) }
                    },
                    label = "Duración máxima (ms)",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardType = KeyboardType.Number,
                    isError = state.maxControlDurationMs > 120000,
                    errorMessage = if (state.maxControlDurationMs > 120000) "Máximo 120000 ms" else ""
                )
                Text(
                    text = "max 120000",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (state.connectionState == ConnectionState.CONNECTED) {
                        StreamControlOutlinedButton(
                            text = "DESCONECTAR",
                            onClick = viewModel::disconnect,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        StreamControlButton(
                            text = "CONECTAR",
                            onClick = { viewModel.connectToDevice() },
                            enabled = state.deviceName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            state.isScanning -> Icons.Filled.BluetoothSearching
                            state.connectionState == ConnectionState.CONNECTED -> Icons.Filled.BluetoothConnected
                            else -> Icons.Filled.BluetoothSearching
                        },
                        contentDescription = null,
                        tint = when (state.connectionState) {
                            ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                            ConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Estado: ${getConnectionStateText(state.connectionState)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (state.connectionState) {
                            ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                            ConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (state.isScanning && state.discoveredDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Dispositivos encontrados:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(state.discoveredDevices) { device ->
                    DeviceCard(
                        device = device,
                        onClick = { viewModel.connectToDevice(device) }
                    )
                }
            }

            if (state.isScanning && state.discoveredDevices.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Buscando dispositivos...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DeviceCard(
    device: DiscoveredDevice,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun getConnectionStateText(state: ConnectionState): String = when (state) {
    ConnectionState.DISCONNECTED -> "Desconectado"
    ConnectionState.CONNECTING -> "Conectando..."
    ConnectionState.CONNECTED -> "Conectado"
    ConnectionState.ERROR -> "Error"
}