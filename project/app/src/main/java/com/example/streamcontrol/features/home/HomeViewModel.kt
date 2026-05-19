package com.example.streamcontrol.features.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.streamcontrol.core.ble.BleConnectionState
import com.example.streamcontrol.core.ble.BleManager
import com.example.streamcontrol.core.storage.CsvFileManager
import com.example.streamcontrol.core.storage.GlobalConfigStorage
import com.example.streamcontrol.domain.model.ControllerType
import com.example.streamcontrol.domain.model.ProcessSample
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

private const val TAG = "HomeViewModel"

class HomeViewModel(
    private val bleManager: BleManager,
    private val csvFileManager: CsvFileManager,
    private val configStorage: GlobalConfigStorage? = null
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    private var timerJob: Job? = null
    private var startTimeMs: Long = 0

    init {
        observeBleConnection()
        observeReceivedData()
        loadConfig()
    }

    private fun loadConfig() {
        configStorage?.let { storage ->
            viewModelScope.launch {
                try {
                    val config = storage.connectionConfigFlow.first()
                    _state.update {
                        it.copy(
                            activeController = config.activeController,
                            sampleIntervalMs = config.sampleIntervalMs,
                            maxControlDurationMs = config.maxControlDurationMs
                        )
                    }
                } catch (e: Exception) {
                    // Use defaults
                }
            }
        }
    }

    private fun observeBleConnection() {
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

                if (bleState is BleConnectionState.Disconnected && _state.value.isRunning) {
                    _state.update { it.copy(showDisconnectAlert = true, isRunning = false) }
                    stopTimer()
                }
            }
        }
    }

    private fun observeReceivedData() {
        viewModelScope.launch {
            Log.d(TAG, "Starting to observe received data")
            bleManager.receivedData.collect { data ->
                Log.d(TAG, "Received data: ${data?.take(100)}")
                data?.let { jsonString ->
                    parseAndAddSamples(jsonString)
                    bleManager.clearReceivedData()
                }
            }
        }
    }

    private fun parseAndAddSamples(jsonString: String) {
        try {
            Log.d(TAG, "Parsing JSON: ${jsonString.take(100)}")
            val jsonElement = json.parseToJsonElement(jsonString)
            val jsonObject = jsonElement.jsonObject

            val typeValue = jsonObject.getValue("type")
            Log.d(TAG, "Message type: $typeValue")
            if (typeValue.toString() != "\"control_data\"") {
                Log.d(TAG, "Ignoring non-control_data message")
                return
            }

            val samplesArray = jsonObject.getValue("samples").jsonArray
            Log.d(TAG, "Found ${samplesArray.size} samples in array")

            val newSamples = samplesArray.mapNotNull { element ->
                val sample = element.jsonObject
                val elapsedTimeStr = sample.getValue("t").toString()
                Log.d(TAG, "DEBUG: elapsedTimeStr from ESP32 = '$elapsedTimeStr'")
                val tempStr = sample.getValue("temp").toString()
                val angleStr = sample.getValue("angle").toString()
                val pwmStr = sample.getValue("pwm").toString()

                val elapsedTime = elapsedTimeStr.toDoubleOrNull() ?: return@mapNotNull null.also { Log.w(TAG, "Failed to parse t: $elapsedTimeStr") }
                val temperature = tempStr.toDoubleOrNull() ?: return@mapNotNull null.also { Log.w(TAG, "Failed to parse temp: $tempStr") }
                val firingAngle = angleStr.toDoubleOrNull() ?: return@mapNotNull null.also { Log.w(TAG, "Failed to parse angle: $angleStr") }
                val fanPwm = pwmStr.toDoubleOrNull() ?: return@mapNotNull null.also { Log.w(TAG, "Failed to parse pwm: $pwmStr") }

                ProcessSample(
                    elapsedTimeMs = elapsedTime,
                    temperature = temperature,
                    firingAngle = firingAngle,
                    fanPwm = fanPwm.toInt()
                )
            }

            Log.d(TAG, "Parsed ${newSamples.size} samples successfully")

            _state.update { currentState ->
                val updatedBuffer = (currentState.dataBuffer + newSamples).takeLast(2400)
                Log.d(TAG, "Buffer size: ${updatedBuffer.size}")
                currentState.copy(
                    dataBuffer = updatedBuffer,
                    elapsedTimeMs = if (updatedBuffer.isNotEmpty()) {
                        updatedBuffer.maxOf { it.elapsedTimeMs }.toLong()
                    } else 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing samples: ${e.message}", e)
            // Ignore malformed JSON
        }
    }

    fun startControl() {
        if (_state.value.connectionState != ConnectionState.CONNECTED) {
            _state.update { it.copy(errorMessage = "Sin conexión a ESP32") }
            return
        }

        _state.update { it.copy(controlState = ControlState.STARTING) }

        val activeController = _state.value.activeController
        val durationMs = _state.value.maxControlDurationMs
        val sampleIntervalMs = _state.value.sampleIntervalMs

        val pid = activeController == ControllerType.PID
        val imc = activeController == ControllerType.IMC
        val rst = activeController == ControllerType.RST

        val startMessage = """
            {
                "type": "start_control",
                "perturbation": ${_state.value.perturbationEnabled},
                "duration_ms": $durationMs,
                "sample_interval_ms": $sampleIntervalMs,
                "pid": $pid,
                "imc": $imc,
                "rst": $rst
            }
        """.trimIndent()

        bleManager.sendData(startMessage)

        _state.update {
            it.copy(
                controlState = ControlState.RUNNING,
                isRunning = true,
                elapsedTimeMs = 0,
                dataBuffer = emptyList()
            )
        }

        startTimer()
    }

    fun stopControl() {
        _state.update { it.copy(controlState = ControlState.STOPPING) }

        val stopMessage = """
            {
                "type": "stop_control"
            }
        """.trimIndent()

        bleManager.sendData(stopMessage)

        stopTimer()

        _state.update {
            it.copy(
                controlState = ControlState.IDLE,
                isRunning = false
            )
        }
    }

    private fun startTimer() {
        startTimeMs = System.currentTimeMillis()
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - startTimeMs
                _state.update { it.copy(elapsedTimeMs = elapsed) }
                delay(100)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun togglePerturbation(enabled: Boolean) {
        _state.update { it.copy(perturbationEnabled = enabled) }
        val message = """{"type":"perturbation","pertur":$enabled}"""
        bleManager.sendData(message)
    }

    fun selectVariable(variable: GraphVariable) {
        _state.update { it.copy(selectedVariable = variable) }
    }

    fun setActiveController(controller: ControllerType) {
        _state.update { it.copy(activeController = controller) }
        // Persist to storage
        configStorage?.let { storage ->
            viewModelScope.launch {
                try {
                    val currentConfig = storage.connectionConfigFlow.first()
                    storage.saveConnectionConfig(currentConfig.copy(activeController = controller))
                } catch (e: Exception) {
                    // Ignore persistence errors
                }
            }
        }
    }

    fun showSaveDialog() {
        _state.update { it.copy(showSaveDialog = true) }
    }

    fun hideSaveDialog() {
        _state.update { it.copy(showSaveDialog = false) }
    }

    fun saveData(fileName: String) {
        val samples = _state.value.dataBuffer
        if (samples.isEmpty()) {
            _state.update { it.copy(errorMessage = "No hay datos para guardar", showSaveDialog = false) }
            return
        }

        val result = csvFileManager.saveToCsv(fileName, samples)
        result.fold(
            onSuccess = {
                _state.update { it.copy(showSaveDialog = false) }
            },
            onFailure = { error ->
                _state.update { it.copy(errorMessage = "Error al guardar: ${error.message}", showSaveDialog = false) }
            }
        )
    }

    fun dismissDisconnectAlert() {
        _state.update { it.copy(showDisconnectAlert = false) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    class Factory(
        private val bleManager: BleManager,
        private val csvFileManager: CsvFileManager,
        private val configStorage: GlobalConfigStorage? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(bleManager, csvFileManager, configStorage) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}