package com.example.streamcontrol.features.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.streamcontrol.domain.model.ControllerType
import com.example.streamcontrol.domain.model.ProcessSample
import com.example.streamcontrol.ui.components.StreamControlCheckbox
import com.example.streamcontrol.ui.components.StreamControlDropdown
import kotlin.math.max

private const val TAG = "HomeScreen"

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

            Spacer(modifier = Modifier.height(12.dp))

            GraphArea(
                dataBuffer = state.dataBuffer,
                selectedVariable = state.selectedVariable,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Variable + Controller in one row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VariableSelector(
                    selectedVariable = state.selectedVariable,
                    onVariableSelected = { viewModel.selectVariable(it) },
                    modifier = Modifier.weight(1f)
                )

                ControllerSelector(
                    activeController = state.activeController,
                    onControllerSelected = { viewModel.setActiveController(it) },
                    enabled = !state.isRunning,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom buttons with icons
            ActionButtonsRow(
                isRunning = state.isRunning,
                hasData = state.dataBuffer.isNotEmpty(),
                perturbationEnabled = state.perturbationEnabled,
                onTogglePerturbation = { viewModel.togglePerturbation(it) },
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
            .padding(12.dp)
    ) {
        if (dataBuffer.isEmpty()) {
            Text(
                text = "Sin datos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val chartConfig = getChartConfig(selectedVariable)
            ProfessionalChart(
                dataBuffer = dataBuffer,
                selectedVariable = selectedVariable,
                config = chartConfig,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private data class ChartConfig(
    val yMin: Float,
    val yMax: Float,
    val yLabel: String,
    val lineColor: Color,
    val unit: String
)

private fun getChartConfig(variable: GraphVariable): ChartConfig {
    return when (variable) {
        GraphVariable.TEMPERATURE -> ChartConfig(0f, 100f, "Temperatura", Color(0xFFE53935), "°C")
        GraphVariable.FIRING_ANGLE -> ChartConfig(0f, 90f, "Ángulo disparo", Color(0xFF1E88E5), "°")
        GraphVariable.FAN_PWM -> ChartConfig(0f, 255f, "PWM", Color(0xFF43A047), "%")
    }
}

@Composable
private fun ProfessionalChart(
    dataBuffer: List<ProcessSample>,
    selectedVariable: GraphVariable,
    config: ChartConfig,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val gridColor = Color.Gray.copy(alpha = 0.3f)

    val leftMargin = 55.dp
    val bottomMargin = 40.dp
    val topMargin = 20.dp
    val rightMargin = 20.dp

    val density = LocalDensity.current

    Canvas(modifier = modifier) {
        val chartLeft = with(density) { leftMargin.toPx() }
        val chartRight = size.width - with(density) { rightMargin.toPx() }
        val chartTop = with(density) { topMargin.toPx() }
        val chartBottom = size.height - with(density) { bottomMargin.toPx() }
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        if (chartWidth <= 0 || chartHeight <= 0 || dataBuffer.isEmpty()) return@Canvas

        val dataPoints = dataBuffer.map { sample ->
            val value = when (selectedVariable) {
                GraphVariable.TEMPERATURE -> sample.temperature.toFloat()
                GraphVariable.FIRING_ANGLE -> sample.firingAngle.toFloat()
                GraphVariable.FAN_PWM -> sample.fanPwm.toFloat()
            }
            sample.elapsedTimeMs.toFloat() to value
        }

        val minTime = dataPoints.minOf { it.first }
        val maxTime = dataPoints.maxOf { it.first }
        val timeRange = max(maxTime - minTime, 1f)
        val yRange = config.yMax - config.yMin

        // Draw grid lines
        val ySteps = 5
        for (i in 0..ySteps) {
            val y = chartBottom - (chartHeight * i / ySteps)
            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 1f
            )
            val yValue = config.yMin + (yRange * i / ySteps)
            val label = if (config.unit == "%") {
                "${yValue.toInt()}"
            } else {
                String.format("%.1f", yValue)
            }
            drawContext.canvas.nativeCanvas.drawText(
                label,
                chartLeft - 8,
                y + 4,
                android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        // X-axis labels
        val xSteps = 5
        for (i in 0..xSteps) {
            val x = chartLeft + (chartWidth * i / xSteps)
            val timeValue = minTime + (timeRange * i / xSteps)
            val timeSeconds = timeValue / 1000f
            val label = "${timeSeconds.toInt()}s"
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                chartBottom + 20,
                android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }

        // Y-axis title
        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.rotate(-90f, 15f, size.height / 2)
        drawContext.canvas.nativeCanvas.drawText(
            "${config.yLabel} (${config.unit})",
            15f,
            size.height / 2,
            android.graphics.Paint().apply {
                color = textColor.hashCode()
                textSize = with(density) { 11.sp.toPx() }
                textAlign = android.graphics.Paint.Align.CENTER
            }
        )
        drawContext.canvas.nativeCanvas.restore()

        // X-axis title
        drawContext.canvas.nativeCanvas.drawText(
            "Tiempo",
            chartLeft + chartWidth / 2,
            size.height - 5,
            android.graphics.Paint().apply {
                color = textColor.hashCode()
                textSize = with(density) { 11.sp.toPx() }
                textAlign = android.graphics.Paint.Align.CENTER
            }
        )

        // Draw axes
        drawLine(
            color = textColor,
            start = Offset(chartLeft, chartTop),
            end = Offset(chartLeft, chartBottom),
            strokeWidth = 2f
        )
        drawLine(
            color = textColor,
            start = Offset(chartLeft, chartBottom),
            end = Offset(chartRight, chartBottom),
            strokeWidth = 2f
        )

        // Draw data line
        if (dataPoints.size > 1) {
            val path = Path()
            var first = true

            dataPoints.forEachIndexed { index, (time, value) ->
                val x = chartLeft + ((time - minTime) / timeRange) * chartWidth
                val y = chartBottom - ((value - config.yMin) / yRange) * chartHeight

                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }

                if (index == dataPoints.lastIndex) {
                    drawCircle(
                        color = config.lineColor,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            drawPath(
                path = path,
                color = config.lineColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        } else if (dataPoints.size == 1) {
            val x = chartLeft + chartWidth / 2
            val y = chartBottom - ((dataPoints[0].second - config.yMin) / yRange) * chartHeight
            drawCircle(
                color = config.lineColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // Draw legend
        val legendBoxLeft = chartRight - 100.dp.toPx()
        val legendBoxTop = chartTop + 5.dp.toPx()
        val legendBoxWidth = 95.dp.toPx()
        val legendBoxHeight = 25.dp.toPx()

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.6f),
            topLeft = Offset(legendBoxLeft, legendBoxTop),
            size = androidx.compose.ui.geometry.Size(legendBoxWidth, legendBoxHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
        )

        drawCircle(
            color = config.lineColor,
            radius = 5.dp.toPx(),
            center = Offset(legendBoxLeft + 15.dp.toPx(), legendBoxTop + legendBoxHeight / 2)
        )

        drawContext.canvas.nativeCanvas.drawText(
            config.yLabel,
            legendBoxLeft + 28.dp.toPx(),
            legendBoxTop + legendBoxHeight / 2 + 4.dp.toPx(),
            android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = with(density) { 10.sp.toPx() }
                textAlign = android.graphics.Paint.Align.LEFT
            }
        )
    }
}

@Composable
private fun VariableSelector(
    selectedVariable: GraphVariable,
    onVariableSelected: (GraphVariable) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("Temperatura", "Ángulo", "PWM")
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
        modifier = modifier
    )
}

@Composable
private fun ControllerSelector(
    activeController: ControllerType,
    onControllerSelected: (ControllerType) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val options = ControllerType.entries.map { it.name }
    val selectedIndex = ControllerType.entries.indexOf(activeController)

    StreamControlDropdown(
        label = "Controlador",
        selectedValue = options[selectedIndex],
        options = options,
        onValueChange = { newValue ->
            val index = options.indexOf(newValue)
            if (index >= 0) {
                onControllerSelected(ControllerType.entries[index])
            }
        },
        enabled = enabled,
        modifier = modifier
    )
}

@Composable
private fun ActionButtonsRow(
    isRunning: Boolean,
    hasData: Boolean,
    perturbationEnabled: Boolean,
    onTogglePerturbation: (Boolean) -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Perturbation toggle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = { onTogglePerturbation(!perturbationEnabled) },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (perturbationEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Waves,
                    contentDescription = "Perturbación",
                    tint = if (perturbationEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Text(
                text = "Pert.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Start/Stop button
        Button(
            onClick = if (isRunning) onStopClick else onStartClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Color(0xFFF44336) else MaterialTheme.colorScheme.primary
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (isRunning) "Parar" else "Iniciar"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isRunning) "PARAR" else "INICIAR")
        }

        // Save button
        OutlinedButton(
            onClick = onSaveClick,
            modifier = Modifier
                .size(56.dp),
            enabled = hasData,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Save,
                contentDescription = "Guardar"
            )
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