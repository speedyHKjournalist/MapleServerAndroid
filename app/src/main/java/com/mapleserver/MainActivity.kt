package com.mapleserver

import MainCompose
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapleserver.ui.theme.MapleServerTheme
import com.mapleserver.ui.theme.ServerConfigScreen
import java.io.File
import java.io.FileOutputStream


class MainActivity : ComponentActivity() {
    private val serverinit by lazy { ServerInit(this@MainActivity) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val isFirstRun = preferences.getBoolean("isFirstRun", true)
        if (isFirstRun) {
            copyAssetFileApplication("config.yaml")
            preferences.edit().putBoolean("isFirstRun", false).apply()
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MapleServerTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "main_screen"
                ) {
                    composable("main_screen") {
                        MainCompose(this@MainActivity, navController, serverinit)
                    }
                    composable("config_editor_screen") {
                        ServerConfigScreen(this@MainActivity, navController, serverinit)
                    }
                    composable("export_db_screen") {
                        ExportDBCompose(this@MainActivity, navController)
                    }
                    composable("import_db_screen") {
                        ImportDBCompose(this@MainActivity, navController)
                    }
                }
            }
        }
    }

    private fun copyAssetFileApplication(assetFileName: String) {
        try {
            val appDir: File = applicationContext.dataDir
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