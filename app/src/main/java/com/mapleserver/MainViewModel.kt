package com.mapleserver

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import java.io.File

class MainViewModel(context: Context) {
    val isStartButtonEnabled = mutableStateOf(true)
    val isStopButtonEnabled = mutableStateOf(false)
    val serviceIntent = Intent(context, ServerService::class.java)
    val yamlMapper = YAMLMapper()
    val serverConfig: ServerConfig = yamlMapper.readValue(File(context.dataDir, "config.yaml"), ServerConfig::class.java)

    val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            val binder: ServerService.LocalBinder = service as ServerService.LocalBinder
            val myService = binder.getService()

            isStartButtonEnabled.value = !myService.isRunning()
            isStopButtonEnabled.value = myService.isRunning()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
        }
    }
    val notificationPendingIntent: PendingIntent by lazy {
        val intent = Intent(context, MainActivity::class.java)
        PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}