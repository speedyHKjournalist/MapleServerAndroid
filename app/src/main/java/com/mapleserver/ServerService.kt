package com.mapleserver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import net.server.Server
import kotlin.system.exitProcess


class ServerService : Service() {
    private var isRunning = false
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): ServerService = this@ServerService
    }

    fun isRunning(): Boolean {
        return isRunning
    }
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRunning) {
            Server.getInstance(applicationContext).shutdown(false).run()
            exitProcess(0)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = intent?.getStringExtra("channel_id") ?: "default_id"
        val pendingIntent : PendingIntent?
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingIntent = intent?.getParcelableExtra("pending_intent", PendingIntent::class.java)
        } else {
            pendingIntent = intent?.getParcelableExtra<PendingIntent>("pending_intent")
        }
        val initializationThread = Thread {
            val args = arrayOf("-Xmx2048m", "-Dwz-path=wz", "-Djava.net.preferIPv4Stack=true")
            Server.getInstance(applicationContext)
            Server.main(args)
        }
        initializationThread.start()
        isRunning = true

        val notificationManager = getSystemService(ComponentActivity.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "OpenMapleServer Channel", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(this, "1")
        builder.setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("OpenMapleServer Notification")
            .setContentText("OpenMapleServer is Running")
            .setContentIntent(pendingIntent)
            .setChannelId("1")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notification = builder.build()
        startForeground(startId, notification)
        return START_STICKY
    }
}