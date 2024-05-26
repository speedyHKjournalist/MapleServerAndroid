package com.mapleserver

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import java.io.*

class LogViewModel(context: Context) {
    var logMessage = mutableStateOf("")

    init {
        updateLogMessages(context)
    }

    private fun updateLogMessages(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            val logFile = File(context.filesDir, "server.log")
            while (true) {
                if (logFile.exists()) {
                    val logContent = readLogFileContent(logFile)
                    withContext(Dispatchers.Main) {
                        logMessage.value = logContent
                    }
                }
                delay(2000)
            }
        }
    }

    private fun readLogFileContent(logFile: File): String {
        try {
            val lines = logFile.readLines()
            val lastLines = lines.takeLast(5000)
            return lastLines.joinToString("\n")
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}