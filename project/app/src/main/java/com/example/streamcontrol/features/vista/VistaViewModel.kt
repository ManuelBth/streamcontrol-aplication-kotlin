package com.example.streamcontrol.features.vista

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VistaViewModel : ViewModel() {

    private val _state = MutableStateFlow(VistaState())
    val state: StateFlow<VistaState> = _state.asStateFlow()

    fun setPower(power: Int) {
        _state.update { it.copy(power = power.coerceIn(5, 95)) }
    }

    fun setFrequency(frequency: Int) {
        _state.update { it.copy(frequency = frequency.coerceIn(50, 60)) }
    }
}