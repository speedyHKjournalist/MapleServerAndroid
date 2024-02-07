package com.mapleserver

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.telecom.Connection
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.AndroidViewModel

class SharedUtil(application: Application) : AndroidViewModel(application) {
    companion object {
        fun startMapleServer(context: Context, serviceIntent : Intent, notificationPendingIntent: PendingIntent) {
            serviceIntent.putExtra("channel_id", "1")
            serviceIntent.putExtra("pending_intent", notificationPendingIntent)
            startForegroundService(context, serviceIntent)
        }
        fun stopMapleServer(context: Context, serviceIntent : Intent, connection : ServiceConnection) {
            context.unbindService(connection)
            context.stopService(serviceIntent)
        }
    }
}