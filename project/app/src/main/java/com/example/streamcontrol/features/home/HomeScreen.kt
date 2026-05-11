package com.example.streamcontrol.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.streamcontrol.domain.model.ProcessSample
import com.example.streamcontrol.ui.components.StreamControlButton
import com.example.streamcontrol.ui.components.StreamControlCheckbox
import com.example.streamcontrol.ui.components.StreamControlDropdown

@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var saveFileName by remember { mutableStateOf("") }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            TopBar(
                connectionState = state.connectionState,
                elapsedTimeMs = state.elapsedTimeMs,
                isRunning = state.isRunning,
                onStopClick = { viewModel.stopControl() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            GraphArea(
                dataBuffer = state.dataBuffer,
                selectedVariable = state.selectedVariable,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            VariableSelector(
                selectedVariable = state.selectedVariable,
                onVariableSelected = { viewModel.selectVariable(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            StreamControlCheckbox(
                label = "Perturbación",
                checked = state.perturbationEnabled,
                onCheckedChange = { viewModel.togglePerturbation(it) },
                enabled = !state.isRunning
            )

            Spacer(modifier = Modifier.height(16.dp))

            BottomButtons(
                isRunning = state.isRunning,
                hasData = state.dataBuffer.isNotEmpty(),
                onStartClick = { viewModel.startControl() },
                onStopClick = { viewModel.stopControl() },
                onSaveClick = { viewModel.showSaveDialog() }
            )
        }
    }

    if (state.showSaveDialog) {
        SaveDialog(
            fileName = saveFileName,
            onFileNameChange = { saveFileName = it },
            onConfirm = {
                viewModel.saveData(saveFileName)
                saveFileName = ""
            },
            onDismiss = {
                viewModel.hideSaveDialog()
                saveFileName = ""
            }
        )
    }

    if (state.showDisconnectAlert) {
        DisconnectAlert(
            onConfirm = { viewModel.dismissDisconnectAlert() }
        )
    }
}

@Composable
private fun TopBar(
    connectionState: ConnectionState,
    elapsedTimeMs: Long,
    isRunning: Boolean,
    onStopClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when (connectionState) {
                            ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                            ConnectionState.CONNECTING -> Color(0xFFFFC107)
                            ConnectionState.ERROR -> Color(0xFFF44336)
                            ConnectionState.DISCONNECTED -> Color(0xFF9E9E9E)
                        }
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (connectionState) {
                    ConnectionState.CONNECTED -> "CONECTADO"
                    ConnectionState.CONNECTING -> "CONECTANDO"
                    ConnectionState.ERROR -> "ERROR"
                    ConnectionState.DISCONNECTED -> "DESCONECTADO"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatTime(elapsedTimeMs),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (isRunning) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onStopClick,
                    modifier = Modifier.size(32.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Parar",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GraphArea(
    dataBuffer: List<ProcessSample>,
    selectedVariable: GraphVariable,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(8.dp)
    ) {
        if (dataBuffer.isEmpty()) {
            Text(
                text = "Sin datos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val chartData = getChartData(dataBuffer, selectedVariable)
            val yRange = getYRange(selectedVariable)

            Chart(
                data = chartData,
                yRange = yRange,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun Chart(
    data: List<Pair<Float, Float>>,
    yRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val minX = data.minOf { it.first }
        val maxX = data.maxOf { it.first }
        val xRange = if (maxX - minX > 0) maxX - minX else 1f
        val yMin = yRange.start
        val yMax = yRange.endInclusive
        val yRangeSpan = yMax - yMin

        val paddingX = 40f
        val paddingY = 30f
        val chartWidth = size.width - paddingX * 2
        val chartHeight = size.height - paddingY * 2

        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = androidx.compose.ui.geometry.Offset(paddingX, paddingY),
            end = androidx.compose.ui.geometry.Offset(paddingX, size.height - paddingY),
            strokeWidth = 1f
        )
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = androidx.compose.ui.geometry.Offset(paddingX, size.height - paddingY),
            end = androidx.compose.ui.geometry.Offset(size.width - paddingX, size.height - paddingY),
            strokeWidth = 1f
        )

        val gridLines = 5
        for (i in 0..gridLines) {
            val y = paddingY + (chartHeight * i / gridLines)
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = androidx.compose.ui.geometry.Offset(paddingX, y),
                end = androidx.compose.ui.geometry.Offset(size.width - paddingX, y),
                strokeWidth = 1f
            )
        }

        if (data.size > 1) {
            for (i in 0 until data.size - 1) {
                val x1 = paddingX + ((data[i].first - minX) / xRange) * chartWidth
                val y1 = paddingY + chartHeight - ((data[i].second - yMin) / yRangeSpan) * chartHeight
                val x2 = paddingX + ((data[i + 1].first - minX) / xRange) * chartWidth
                val y2 = paddingY + chartHeight - ((data[i + 1].second - yMin) / yRangeSpan) * chartHeight

                drawLine(
                    color = primaryColor,
                    start = androidx.compose.ui.geometry.Offset(x1, y1),
                    end = androidx.compose.ui.geometry.Offset(x2, y2),
                    strokeWidth = 2f
                )
            }

            val lastPoint = data.last()
            val lastX = paddingX + ((lastPoint.first - minX) / xRange) * chartWidth
            val lastY = paddingY + chartHeight - ((lastPoint.second - yMin) / yRangeSpan) * chartHeight

            drawCircle(
                color = primaryColor,
                radius = 4f,
                center = androidx.compose.ui.geometry.Offset(lastX, lastY)
            )
        }
    }
}

private fun getChartData(data: List<ProcessSample>, variable: GraphVariable): List<Pair<Float, Float>> {
    return data.map { sample ->
        val value = when (variable) {
            GraphVariable.TEMPERATURE -> sample.temperature.toFloat()
            GraphVariable.FIRING_ANGLE -> sample.firingAngle.toFloat()
            GraphVariable.FAN_PWM -> sample.fanPwm.toFloat()
        }
        sample.elapsedTimeMs.toFloat() to value
    }
}

private fun getYRange(variable: GraphVariable): ClosedFloatingPointRange<Float> {
    return when (variable) {
        GraphVariable.TEMPERATURE -> 0f..180f
        GraphVariable.FIRING_ANGLE -> 0f..180f
        GraphVariable.FAN_PWM -> 0f..255f
    }
}

@Composable
private fun VariableSelector(
    selectedVariable: GraphVariable,
    onVariableSelected: (GraphVariable) -> Unit
) {
    val options = listOf("Temperatura", "Ángulo disparo", "PWM")
    val selectedIndex = when (selectedVariable) {
        GraphVariable.TEMPERATURE -> 0
        GraphVariable.FIRING_ANGLE -> 1
        GraphVariable.FAN_PWM -> 2
    }

    StreamControlDropdown(
        label = "Variable",
        selectedValue = options[selectedIndex],
        options = options,
        onValueChange = { newValue ->
            val index = options.indexOf(newValue)
            if (index >= 0) {
                onVariableSelected(
                    when (index) {
                        0 -> GraphVariable.TEMPERATURE
                        1 -> GraphVariable.FIRING_ANGLE
                        else -> GraphVariable.FAN_PWM
                    }
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BottomButtons(
    isRunning: Boolean,
    hasData: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isRunning) {
            Button(
                onClick = onStopClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("PARAR")
            }
        } else {
            StreamControlButton(
                text = "INICIAR",
                onClick = onStartClick,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedButton(
            onClick = onSaveClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            enabled = hasData,
            shape = MaterialTheme.shapes.medium
        ) {
            Text("GUARDAR")
        }
    }
}

@Composable
private fun SaveDialog(
    fileName: String,
    onFileNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(fileName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar datos") },
        text = {
            Column {
                Text("Ingrese el nombre del archivo:")
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        onFileNameChange(it)
                    },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = textValue.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun DisconnectAlert(
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onConfirm,
        title = { Text("Conexión perdida") },
        text = { Text("Se perdió la conexión con el dispositivo ESP32. El control se ha detenido.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Aceptar")
            }
        }
    )
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}