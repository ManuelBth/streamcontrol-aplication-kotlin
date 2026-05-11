package com.example.streamcontrol.features.control

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.unit.dp
import com.example.streamcontrol.ui.components.StreamControlButton
import com.example.streamcontrol.ui.components.StreamControlCheckbox
import com.example.streamcontrol.ui.components.StreamControlTextField

@Composable
fun ControlScreen(
    viewModel: ControlViewModel
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var pidExpanded by remember { mutableStateOf(true) }
    var imcExpanded by remember { mutableStateOf(false) }
    var rstExpanded by remember { mutableStateOf(false) }

    val errorMessage = "Sin conexión a ESP32"

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.syncSuccess) {
        if (state.syncSuccess) {
            snackbarHostState.showSnackbar("Configuración sincronizada correctamente")
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "CONTROLADORES",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            AccordionSection(
                title = "PID",
                expanded = pidExpanded,
                onToggle = { pidExpanded = !pidExpanded }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StreamControlTextField(
                        value = state.pidSettings.proportionalGain.toString(),
                        onValueChange = { viewModel.updatePidSettings(it, state.pidSettings.integralTime.toString(), state.pidSettings.derivativeTime.toString(), state.pidSettings.setpoint.toString()) },
                        label = "Kp (0-100, 4 dec)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    StreamControlTextField(
                        value = state.pidSettings.integralTime.toString(),
                        onValueChange = { viewModel.updatePidSettings(state.pidSettings.proportionalGain.toString(), it, state.pidSettings.derivativeTime.toString(), state.pidSettings.setpoint.toString()) },
                        label = "Ki (0-100, 4 dec)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    StreamControlTextField(
                        value = state.pidSettings.derivativeTime.toString(),
                        onValueChange = { viewModel.updatePidSettings(state.pidSettings.proportionalGain.toString(), state.pidSettings.integralTime.toString(), it, state.pidSettings.setpoint.toString()) },
                        label = "Kd (0-100, 4 dec)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    StreamControlTextField(
                        value = state.pidSettings.setpoint.toString(),
                        onValueChange = { viewModel.updatePidSettings(state.pidSettings.proportionalGain.toString(), state.pidSettings.integralTime.toString(), state.pidSettings.derivativeTime.toString(), it) },
                        label = "Setpoint (0-120)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            AccordionSection(
                title = "IMC-ITAE",
                expanded = imcExpanded,
                onToggle = { imcExpanded = !imcExpanded }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StreamControlTextField(
                        value = state.imcSettings.processGain.toString(),
                        onValueChange = { viewModel.updateImcSettings(it, state.imcSettings.timeConstant.toString(), state.imcSettings.deadTime.toString(), state.imcSettings.lambda.toString()) },
                        label = "K",
                        modifier = Modifier.fillMaxWidth()
                    )
                    StreamControlTextField(
                        value = state.imcSettings.timeConstant.toString(),
                        onValueChange = { viewModel.updateImcSettings(state.imcSettings.processGain.toString(), it, state.imcSettings.deadTime.toString(), state.imcSettings.lambda.toString()) },
                        label = "τ (tau)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    StreamControlTextField(
                        value = state.imcSettings.deadTime.toString(),
                        onValueChange = { viewModel.updateImcSettings(state.imcSettings.processGain.toString(), state.imcSettings.timeConstant.toString(), it, state.imcSettings.lambda.toString()) },
                        label = "θ (theta)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    StreamControlTextField(
                        value = state.imcSettings.lambda.toString(),
                        onValueChange = { viewModel.updateImcSettings(state.imcSettings.processGain.toString(), state.imcSettings.timeConstant.toString(), state.imcSettings.deadTime.toString(), it) },
                        label = "λ (lambda)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            AccordionSection(
                title = "RST",
                expanded = rstExpanded,
                onToggle = { rstExpanded = !rstExpanded }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StreamControlTextField(
                        value = state.rstSettings.feedbackCoeffs.joinToString(", "),
                        onValueChange = { viewModel.updateRstSettings(it, state.rstSettings.controlActionCoeffs.joinToString(", "), state.rstSettings.feedforwardCoeffs.joinToString(", "), state.rstSettings.plantPoles.joinToString(", "), state.rstSettings.plantZeros.joinToString(", "), state.rstSettings.desiredPoles.joinToString(", ")) },
                        label = "R (feedback coeffs)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    StreamControlTextField(
                        value = state.rstSettings.controlActionCoeffs.joinToString(", "),
                        onValueChange = { viewModel.updateRstSettings(state.rstSettings.feedbackCoeffs.joinToString(", "), it, state.rstSettings.feedforwardCoeffs.joinToString(", "), state.rstSettings.plantPoles.joinToString(", "), state.rstSettings.plantZeros.joinToString(", "), state.rstSettings.desiredPoles.joinToString(", ")) },
                        label = "S (control action coeffs)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    StreamControlTextField(
                        value = state.rstSettings.feedforwardCoeffs.joinToString(", "),
                        onValueChange = { viewModel.updateRstSettings(state.rstSettings.feedbackCoeffs.joinToString(", "), state.rstSettings.controlActionCoeffs.joinToString(", "), it, state.rstSettings.plantPoles.joinToString(", "), state.rstSettings.plantZeros.joinToString(", "), state.rstSettings.desiredPoles.joinToString(", ")) },
                        label = "T (feedforward coeffs)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    StreamControlTextField(
                        value = state.rstSettings.plantPoles.joinToString(", "),
                        onValueChange = { viewModel.updateRstSettings(state.rstSettings.feedbackCoeffs.joinToString(", "), state.rstSettings.controlActionCoeffs.joinToString(", "), state.rstSettings.feedforwardCoeffs.joinToString(", "), it, state.rstSettings.plantZeros.joinToString(", "), state.rstSettings.desiredPoles.joinToString(", ")) },
                        label = "A (plant poles)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    StreamControlTextField(
                        value = state.rstSettings.plantZeros.joinToString(", "),
                        onValueChange = { viewModel.updateRstSettings(state.rstSettings.feedbackCoeffs.joinToString(", "), state.rstSettings.controlActionCoeffs.joinToString(", "), state.rstSettings.feedforwardCoeffs.joinToString(", "), state.rstSettings.plantPoles.joinToString(", "), it, state.rstSettings.desiredPoles.joinToString(", ")) },
                        label = "B (plant zeros)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    StreamControlTextField(
                        value = state.rstSettings.desiredPoles.joinToString(", "),
                        onValueChange = { viewModel.updateRstSettings(state.rstSettings.feedbackCoeffs.joinToString(", "), state.rstSettings.controlActionCoeffs.joinToString(", "), state.rstSettings.feedforwardCoeffs.joinToString(", "), state.rstSettings.plantPoles.joinToString(", "), state.rstSettings.plantZeros.joinToString(", "), it) },
                        label = "P (desired poles)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    StreamControlCheckbox(
                        label = "Integrador (1 - z^-1)",
                        checked = state.rstSettings.hasIntegrator,
                        onCheckedChange = { viewModel.updateRstIntegrator(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            StreamControlButton(
                text = "GUARDAR Y SINCRONIZAR",
                onClick = { viewModel.saveAndSync() },
                enabled = !state.isSaving && !state.isSyncing,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccordionSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Colapsar" else "Expandir",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}