package com.mapleserver

import MainCompose
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapleserver.ui.theme.MapleServerTheme
import com.mapleserver.ui.theme.ServerConfigScreen
import java.io.File
import java.io.FileOutputStream


class MainActivity : ComponentActivity() {
    private lateinit var serverParameter: ServerParameter
    private val mainViewModel: MainViewModel by viewModels()
    private val importViewModel: ImportViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val isFirstRun = preferences.getBoolean("isFirstRun", true)
        if (isFirstRun) {
            copyAssetFileApplication("config.yaml")
            preferences.edit().putBoolean("isFirstRun", false).apply()
        }
        serverParameter = ServerParameter(this, mainViewModel)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        registerReceiver(serverParameter.statusReceiver, serverParameter.intentFilter, RECEIVER_NOT_EXPORTED)
        bindService(serverParameter.serviceIntent, serverParameter.mConnection, Context.BIND_AUTO_CREATE)
        setContent {
            MapleServerTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "main_screen"
                ) {
                    composable("main_screen") {
                        MainCompose(this@MainActivity, navController, serverParameter, mainViewModel)
                    }
                    composable("config_editor_screen") {
                        ServerConfigScreen(this@MainActivity, navController, serverParameter)
                    }
                    composable("export_db_screen") {
                        ExportCompose(this@MainActivity, navController)
                    }
                    composable("import_db_screen") {
                        ImportCompose(this@MainActivity, navController, importViewModel, serverParameter)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(serverParameter.statusReceiver)
    }

    private fun copyAssetFileApplication(assetFileName: String) {
        try {
            val appDir: File = applicationContext.filesDir
            val yamlConfig = File(appDir, "config.yaml")
            val inputStream = assets.open(assetFileName)
            val outputStream = FileOutputStream(yamlConfig)

            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            inputStream.close()
            outputStream.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}