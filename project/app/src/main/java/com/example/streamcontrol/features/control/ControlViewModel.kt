package com.example.streamcontrol.features.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.streamcontrol.core.ble.BleConnectionState
import com.example.streamcontrol.core.ble.BleManager
import com.example.streamcontrol.core.storage.GlobalConfigStorage
import com.example.streamcontrol.domain.model.Controllers
import com.example.streamcontrol.domain.model.ImcSettings
import com.example.streamcontrol.domain.model.PidSettings
import com.example.streamcontrol.domain.model.RstSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ControlViewModel(
    private val bleManager: BleManager,
    private val configStorage: GlobalConfigStorage
) : ViewModel() {

    private val _state = MutableStateFlow(ControlState())
    val state: StateFlow<ControlState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadSavedConfig()
        observeBleState()
    }

    private fun loadSavedConfig() {
        viewModelScope.launch {
            val config = configStorage.controlConfigFlow.first()
            _state.update {
                it.copy(
                    pidSettings = config.controllers.pid,
                    imcSettings = config.controllers.imc,
                    rstSettings = config.controllers.rst
                )
            }
        }
    }

    private fun observeBleState() {
        viewModelScope.launch {
            bleManager.connectionState.collect { bleState ->
                val newConnectionState = when (bleState) {
                    is BleConnectionState.Disconnected -> ConnectionState.DISCONNECTED
                    is BleConnectionState.Connecting -> ConnectionState.CONNECTING
                    is BleConnectionState.Connected -> ConnectionState.CONNECTED
                    is BleConnectionState.Error -> {
                        _state.update { it.copy(errorMessage = bleState.message) }
                        ConnectionState.ERROR
                    }
                }
                _state.update { it.copy(connectionState = newConnectionState) }
            }
        }
    }

    fun updatePidSettings(kp: String, ki: String, kd: String, setpoint: String) {
        val newPidSettings = _state.value.pidSettings.copy(
            proportionalGain = kp.toDoubleOrNull() ?: _state.value.pidSettings.proportionalGain,
            integralTime = ki.toDoubleOrNull() ?: _state.value.pidSettings.integralTime,
            derivativeTime = kd.toDoubleOrNull() ?: _state.value.pidSettings.derivativeTime,
            setpoint = setpoint.toDoubleOrNull() ?: _state.value.pidSettings.setpoint
        )
        _state.update { it.copy(pidSettings = newPidSettings) }
    }

    fun updateImcSettings(k: String, tau: String, theta: String, lambda: String) {
        val newImcSettings = _state.value.imcSettings.copy(
            processGain = k.toDoubleOrNull() ?: _state.value.imcSettings.processGain,
            timeConstant = tau.toDoubleOrNull() ?: _state.value.imcSettings.timeConstant,
            deadTime = theta.toDoubleOrNull() ?: _state.value.imcSettings.deadTime,
            lambda = lambda.toDoubleOrNull() ?: _state.value.imcSettings.lambda
        )
        _state.update { it.copy(imcSettings = newImcSettings) }
    }

    fun updateRstSettings(r: String, s: String, t: String, a: String, b: String, p: String) {
        val newRstSettings = _state.value.rstSettings.copy(
            feedbackCoeffs = parseCoeffs(r),
            controlActionCoeffs = parseCoeffs(s),
            feedforwardCoeffs = parseCoeffs(t),
            plantPoles = parseCoeffs(a),
            plantZeros = parseCoeffs(b),
            desiredPoles = parseCoeffs(p)
        )
        _state.update { it.copy(rstSettings = newRstSettings) }
    }

    fun updateRstIntegrator(hasIntegrator: Boolean) {
        _state.update {
            it.copy(rstSettings = it.rstSettings.copy(hasIntegrator = hasIntegrator))
        }
    }

    private fun parseCoeffs(text: String): List<Double> {
        return text.split(",", " ", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toDoubleOrNull() }
            .ifEmpty { listOf(1.0) }
    }

    fun saveAndSync() {
        if (_state.value.connectionState != ConnectionState.CONNECTED) {
            _state.update { it.copy(errorMessage = "Sin conexión a ESP32") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, isSyncing = true, errorMessage = null) }

            try {
                val controllers = Controllers(
                    pid = _state.value.pidSettings,
                    imc = _state.value.imcSettings,
                    rst = _state.value.rstSettings
                )
                configStorage.saveControllers(controllers)

                val syncMessage = buildConfigSyncMessage()
                bleManager.sendData(syncMessage)

                _state.update {
                    it.copy(
                        isSaving = false,
                        isSyncing = false,
                        syncSuccess = true
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        isSyncing = false,
                        errorMessage = e.message ?: "Error al sincronizar"
                    )
                }
            }
        }
    }

    private fun buildConfigSyncMessage(): String {
        val currentState = _state.value
        val configJson = buildString {
            append("""
            {
                "type": "config_sync",
                "pid": {
                    "kp": ${currentState.pidSettings.proportionalGain},
                    "ki": ${currentState.pidSettings.integralTime},
                    "kd": ${currentState.pidSettings.derivativeTime},
                    "setpoint": ${currentState.pidSettings.setpoint}
                },
                "imc": {
                    "K": ${currentState.imcSettings.processGain},
                    "tau": ${currentState.imcSettings.timeConstant},
                    "theta": ${currentState.imcSettings.deadTime},
                    "lambda": ${currentState.imcSettings.lambda}
                },
                "rst": {
                    "R": ${currentState.rstSettings.feedbackCoeffs},
                    "S": ${currentState.rstSettings.controlActionCoeffs},
                    "T": ${currentState.rstSettings.feedforwardCoeffs},
                    "A": ${currentState.rstSettings.plantPoles},
                    "B": ${currentState.rstSettings.plantZeros},
                    "P": ${currentState.rstSettings.desiredPoles},
                    "integrator": ${currentState.rstSettings.hasIntegrator}
                }
            }
            """.trimIndent())
        }
        return configJson
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null, syncSuccess = false) }
    }

    override fun onCleared() {
        super.onCleared()
    }

    class Factory(
        private val bleManager: BleManager,
        private val configStorage: GlobalConfigStorage
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ControlViewModel::class.java)) {
                return ControlViewModel(bleManager, configStorage) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}