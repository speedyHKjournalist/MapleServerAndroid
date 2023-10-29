package com.mapleserver

import android.R
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapleserver.ui.theme.MapleServerTheme
import net.server.Server


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
    @Composable
    fun MyApp() {
        val isServerOnline = remember { mutableStateOf(false) }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        MapleServerTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Button(
                        onClick = {
                            startMapleServer(this@MainActivity, isServerOnline)
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Start")
                    }
                    if (isServerOnline.value) {
                        Text(
                            text = "Maplestory server is now online",
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    private fun startMapleServer(context: Context, isServerOnline: MutableState<Boolean>) {
        try {
            val args = arrayOf("-Xmx2048m", "-Dwz-path=wz", "-Djava.net.preferIPv4Stack=true")
            Server.main(args, context)
            isServerOnline.value = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}