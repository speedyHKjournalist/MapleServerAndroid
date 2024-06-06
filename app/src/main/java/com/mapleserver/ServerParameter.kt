package com.mapleserver

import android.content.*
import android.os.IBinder
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import java.io.File

class ServerParameter(context: Context, viewModel: MainViewModel) {
    val serviceIntent = Intent(context, ServerService::class.java)
    val yamlMapper = YAMLMapper()
    var serverConfig: ServerConfig = yamlMapper.readValue(File(context.filesDir, "config.yaml"), ServerConfig::class.java)
    val intentFilter = IntentFilter("MapleServerMessage")

    val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra("Status")

            if (status == "ServerStarting") {
                viewModel.updateProcessingBar(true)
            } else if (status == "ServerStarted") {
                viewModel.updateProcessingBar(false)
                viewModel.updateStartButton(false)
                viewModel.updateStopButton(true)
            } else if (status == "ServerStopping") {
                viewModel.updateProcessingBar(true)
            } else if (status == "ServerStopped") {
                viewModel.updateProcessingBar(false)
                viewModel.updateStartButton(true)
                viewModel.updateStopButton(false)
            }
        }
    }

    val mConnection: ServiceConnection = object : ServiceConnection {
        lateinit var myService: ServerService

        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            val binder: ServerService.LocalBinder = service as ServerService.LocalBinder
            myService = binder.getService()

            viewModel.updateStartButton(!myService.isRunning())
            viewModel.updateStopButton(myService.isRunning())
        }

        override fun onServiceDisconnected(arg0: ComponentName) {}
    }
}