package com.mapleserver

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.AndroidViewModel

class SharedUtil(application: Application) : AndroidViewModel(application) {
    companion object {
        fun startMapleServer(context: Context, notificationPendingIntent: PendingIntent) {
            val serviceIntent = Intent(context, ServerService::class.java)
            serviceIntent.putExtra("channel_id", "1")
            serviceIntent.putExtra("pending_intent", notificationPendingIntent)
            startForegroundService(context, serviceIntent)
        }
        fun stopMapleServer(context: Context) {
            val serviceIntent = Intent(context, ServerService::class.java)
            context.stopService(serviceIntent)
        }
    }
}