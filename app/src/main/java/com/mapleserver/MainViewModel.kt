package com.mapleserver

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {
    private val _showProcessingBar = MutableStateFlow(false)
    val showProcessingBar: StateFlow<Boolean> = _showProcessingBar

    private val _isStartButtonEnabled = MutableStateFlow(true)
    val isStartButtonEnabled: StateFlow<Boolean> = _isStartButtonEnabled

    private val _isStopButtonEnabled = MutableStateFlow(false)
    val isStopButtonEnabled: StateFlow<Boolean> = _isStopButtonEnabled

    fun updateProcessingBar(show: Boolean) {
        _showProcessingBar.value = show
    }

    fun updateStartButton(enabled: Boolean) {
        _isStartButtonEnabled.value = enabled
    }

    fun updateStopButton(enabled: Boolean) {
        _isStopButtonEnabled.value = enabled
    }
}