package com.mapleserver

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.app.Service
import android.os.IBinder
import androidx.core.app.NotificationCompat
import net.server.Server

class ServerService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
    override fun onDestroy() {
        super.onDestroy()
        Server.getInstance(applicationContext).shutdown(false).run()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = intent?.getStringExtra("channel_id") ?: "dafault_id"
        val pendingIntent = intent?.getParcelableExtra<PendingIntent>("pending_intent")
        val initializationThread = Thread {
            val args = arrayOf("-Xmx2048m", "-Dwz-path=wz", "-Djava.net.preferIPv4Stack=true")
            Server.getInstance(applicationContext)
            Server.main(args)
        }
        initializationThread.start()

        val notification = createNotification(channelId, pendingIntent)
        startForeground(startId, notification)
        return START_STICKY
    }
    private fun createNotification(channelId: String, pendingIntent: PendingIntent?): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("MapleServer is Running")
            .setContentText("Tap to open the app")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
        return notificationBuilder.build()
    }
}