package com.mapleserver

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.util.*
import kotlin.concurrent.fixedRateTimer

class LogViewModel(context: Context) {
    var logMessage = mutableStateOf("")
    private val handler = Handler(Looper.getMainLooper())

    init {
        updateLogMessages(context)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateLogMessages(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            val logFile = File(context.filesDir, "server.log")
            // Create a timer for periodic updates (every 5 seconds)
            fixedRateTimer(initialDelay = 0, period = 1000) {
                handler.post {
                    if (logFile.exists()) {
                        // Read the log file content and update logMessage
                        val logContent = readLogFileContent(logFile)
                        logMessage.value = logContent
                    }
                }
            }
        }
    }

    private fun readLogFileContent(logFile: File): String {
        val lineList = LinkedList<String>()
        try {
            val reader = BufferedReader(FileReader(logFile))
            var line: String?

            // Read the file line by line in reverse order and add to the list
            while (reader.readLine().also { line = it } != null) {
                lineList.addFirst(line)
                if (lineList.size > 200) {
                    // Remove the oldest line if more than 200 lines have been read
                    lineList.removeLast()
                }
            }

            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Join the lines to form the log content
        return lineList.joinToString("\n")
    }

}