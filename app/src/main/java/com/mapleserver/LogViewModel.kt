package com.mapleserver

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class LogViewModel : ViewModel() {
    var logMessage = mutableStateOf("")
}