package com.mapleserver

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class FileViewModel(application: Application) : AndroidViewModel(application) {
    var fileContent = mutableStateOf("")

    init {
        try {
            val configFile = File(application.dataDir, "config.yaml")
            val inputStream = FileInputStream(configFile)
            if (inputStream != null) {
                val size = inputStream.available()
                val buffer = ByteArray(size)
                inputStream.read(buffer)
                inputStream.close()
                val yamlContent = String(buffer, Charsets.UTF_8)
                fileContent.value = yamlContent
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}