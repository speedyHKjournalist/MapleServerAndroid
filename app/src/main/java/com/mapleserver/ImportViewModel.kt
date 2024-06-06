package com.mapleserver

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ImportViewModel : ViewModel() {
    private val _shouldRecompose = MutableStateFlow(false)
    val shouldRecompose: StateFlow<Boolean> = _shouldRecompose

    fun updateComposeState(state: Boolean) {
        _shouldRecompose.value = state
    }
}
