package com.mapleserver

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapleserver.ui.theme.ConfigButton
import com.mapleserver.ui.theme.MapleServerTheme
import com.mapleserver.ui.theme.ServerConfigScreen
import com.mapleserver.ui.theme.StartButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.server.Server
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val isFirstRun = preferences.getBoolean("isFirstRun", true)
        if (isFirstRun) {
            copyAssetFileApplication("config.yaml")
            preferences.edit().putBoolean("isFirstRun", false).apply()
        }
        setContent {
            MapleServerTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "main_screen"
                ) {
                    composable("main_screen") {
                        MyApp(navController)
                    }
                    composable("config_editor_screen") {
                        ServerConfigScreen(this@MainActivity, navController)
                    }
                }
            }
        }
    }
    @Composable
    fun MyApp(navController: NavHostController) {
        val logView : LogViewModel = viewModel()
        var isStartButtonEnabled = remember { mutableStateOf(true) }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        MapleServerTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StartButton(
                            text = "Start",
                            startMapleServer = {
                                isStartButtonEnabled.value = false
                                startMapleServer(logView.logMessage)
                            },
                            isButtonEnabled = isStartButtonEnabled
                        )

                        ConfigButton(text = "Server Config",
                            onOpenEditor = {
                                navController.navigate("config_editor_screen")
                            })
                    }
                    Text(
                        text = logView.logMessage.value,
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    private fun startMapleServer(logMessage: MutableState<String>) {
        val args = arrayOf("-Xmx2048m", "-Dwz-path=wz", "-Djava.net.preferIPv4Stack=true")
        logMessage.value = "Prepare to start MapleStory Server"
        try {
            CoroutineScope(Dispatchers.Main).launch {
                Server.main(args, this@MainActivity, logMessage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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