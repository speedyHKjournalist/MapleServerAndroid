package com.mapleserver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mapleserver.ui.theme.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private val CHANNEL_ID = "1"
    private val CHANNEL_NAME = "MapleServer Notification"
    private val notificationPendingIntent: PendingIntent by lazy {
        val intent = Intent(this, MainActivity::class.java)
        PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
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
        val logView = LogViewModel(this)
        var isStartButtonEnabled = rememberSaveable { mutableStateOf(true) }
        var isStopButtonEnabled = rememberSaveable { mutableStateOf(false) }

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
                            isButtonEnabled = isStartButtonEnabled,
                            startMapleServer = {
                                isStartButtonEnabled.value = false
                                isStopButtonEnabled.value = true
                                startMapleServer()
                            }
                        )
                        StoptButton(
                            text = "Stop",
                            isButtonEnabled = isStopButtonEnabled,
                            stopMapleServer = {
                                isStartButtonEnabled.value = true
                                isStopButtonEnabled.value = false
                                stopMapleServer()
                            }
                        )
                        ConfigButton(text = "Config",
                            onOpenEditor = {
                                navController.navigate("config_editor_screen")
                            })
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .background(Color.White)
                                    .padding(11.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = logView.logMessage.value,
                                    modifier = Modifier.padding(11.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    private fun startMapleServer() {
        val serviceIntent = Intent(this, ServerService::class.java)
        serviceIntent.putExtra("channel_id", CHANNEL_ID)
        serviceIntent.putExtra("pending_intent", notificationPendingIntent)
        startService(serviceIntent)
    }
    private fun stopMapleServer() {
        val serviceIntent = Intent(this, ServerService::class.java)
        stopService(serviceIntent)
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
    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "MapleServer"
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}