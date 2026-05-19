package com.example.streamcontrol.features.control

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.streamcontrol.core.ble.BleConnectionState
import com.example.streamcontrol.core.ble.BleManager
import com.example.streamcontrol.core.storage.GlobalConfigStorage
import com.example.streamcontrol.domain.model.ConnectionConfig
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
            try {
                val config = configStorage.controlConfigFlow.first()
                val connectionConfig = configStorage.connectionConfigFlow.first()

                // Validate and sanitize loaded PID values
                val safePid = try {
                    val p = config.controllers.pid
                    PidSettings(
                        proportionalGain = p.proportionalGain.coerceIn(0.0, 100.0),
                        integralTime = p.integralTime.coerceIn(0.0, 100.0),
                        derivativeTime = p.derivativeTime.coerceIn(0.0, 100.0),
                        setpoint = p.setpoint.coerceIn(0.0, 120.0)
                    )
                } catch (e: Exception) {
                    PidSettings.default()
                }

                // Validate and sanitize loaded IMC values
                val safeImc = try {
                    val i = config.controllers.imc
                    ImcSettings(
                        processGain = i.processGain.coerceIn(0.001, Double.MAX_VALUE),
                        timeConstant = i.timeConstant.coerceIn(0.001, Double.MAX_VALUE),
                        deadTime = i.deadTime.coerceIn(0.0, Double.MAX_VALUE),
                        lambda = i.lambda.coerceIn(0.001, Double.MAX_VALUE)
                    )
                } catch (e: Exception) {
                    ImcSettings.default()
                }

                _state.update {
                    it.copy(
                        pidSettings = safePid,
                        imcSettings = safeImc,
                        rstSettings = config.controllers.rst,
                        sampleIntervalMs = connectionConfig.sampleIntervalMs,
                        maxControlDurationMs = connectionConfig.maxControlDurationMs
                    )
                }
            } catch (e: Exception) {
                Log.e("ControlViewModel", "Error loading config: ${e.message}", e)
                // Use defaults on error
            }
        }
    }

    private fun observeBleState() {
        viewModelScope.launch {
            bleManager.connectionState.collect { bleState ->
                Log.d("ControlViewModel", "BLE state changed to: $bleState")
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
        val newKp = kp.toDoubleOrNull() ?: return // Ignore if invalid number
        val newKi = ki.toDoubleOrNull() ?: return
        val newKd = kd.toDoubleOrNull() ?: return
        val newSetpoint = setpoint.toDoubleOrNull() ?: return

        // Validate ranges before applying
        if (newKp !in 0.0..100.0 || newKi !in 0.0..100.0 || newKd !in 0.0..100.0 || newSetpoint !in 0.0..120.0) {
            return // Ignore out-of-range values while typing
        }

        val newPidSettings = try {
            PidSettings(
                proportionalGain = newKp,
                integralTime = newKi,
                derivativeTime = newKd,
                setpoint = newSetpoint
            )
        } catch (e: IllegalArgumentException) {
            return // Safety net: ignore if validation fails
        }
        _state.update { it.copy(pidSettings = newPidSettings) }
    }

    fun updateImcSettings(k: String, tau: String, theta: String, lambda: String) {
        val newK = k.toDoubleOrNull() ?: return
        val newTau = tau.toDoubleOrNull() ?: return
        val newTheta = theta.toDoubleOrNull() ?: return
        val newLambda = lambda.toDoubleOrNull() ?: return

        // Validate ranges before applying
        if (newK <= 0.0 || newTau <= 0.0 || newTheta < 0.0 || newLambda <= 0.0) {
            return // Ignore out-of-range values while typing
        }

        val newImcSettings = try {
            ImcSettings(
                processGain = newK,
                timeConstant = newTau,
                deadTime = newTheta,
                lambda = newLambda
            )
        } catch (e: IllegalArgumentException) {
            return // Safety net: ignore if validation fails
        }
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

    fun updateSampleInterval(interval: Int) {
        _state.update { it.copy(sampleIntervalMs = interval) }
    }

    fun updateMaxControlDuration(duration: Int) {
        _state.update { it.copy(maxControlDurationMs = duration.coerceIn(0, 120000)) }
    }

    private fun parseCoeffs(text: String): List<Double> {
        return text.split(",", " ", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toDoubleOrNull() }
            .ifEmpty { listOf(1.0) }
    }

    fun saveAndSync() {
        Log.d("ControlViewModel", "saveAndSync() called, connectionState: ${_state.value.connectionState}")
        if (_state.value.connectionState != ConnectionState.CONNECTED) {
            Log.e("ControlViewModel", "Not connected! State: ${_state.value.connectionState}")
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

                // Save interval and duration to connection config
                val currentConnectionConfig = configStorage.connectionConfigFlow.first()
                configStorage.saveConnectionConfig(
                    currentConnectionConfig.copy(
                        sampleIntervalMs = _state.value.sampleIntervalMs,
                        maxControlDurationMs = _state.value.maxControlDurationMs
                    )
                )

                val syncMessage = buildConfigSyncMessage()
                Log.d("ControlViewModel", "Built sync message (${syncMessage.length} bytes): $syncMessage")

                bleManager.sendData(syncMessage)
                Log.d("ControlViewModel", "sendData() called on bleManager")

                _state.update {
                    it.copy(
                        isSaving = false,
                        isSyncing = false,
                        syncSuccess = true
                    )
                }
            } catch (e: Exception) {
                Log.e("ControlViewModel", "Error during sync: ${e.message}", e)
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