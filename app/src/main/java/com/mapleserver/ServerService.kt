package com.mapleserver

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.activity.ComponentActivity
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
        val channelId = intent?.getStringExtra("channel_id") ?: "default_id"
        val pendingIntent = intent?.getParcelableExtra("pending_intent", PendingIntent::class.java)
        val initializationThread = Thread {
            val args = arrayOf("-Xmx2048m", "-Dwz-path=wz", "-Djava.net.preferIPv4Stack=true")
            Server.getInstance(applicationContext)
            Server.main(args)
        }
        initializationThread.start()

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
        notificationManager.notify(0, notification);
        startForeground(startId, notification)
        return START_STICKY
    }
}