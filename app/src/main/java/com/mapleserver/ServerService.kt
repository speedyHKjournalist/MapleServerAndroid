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


class ServerService : Service() {
    private lateinit var mapleServer: Server
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): ServerService = this@ServerService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    fun isRunning(): Boolean {
        return mapleServer.isOnline
    }

    override fun onCreate() {
        super.onCreate()
        mapleServer = Server.getInstance(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMapleService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMapleService(intent, startId)
        return START_NOT_STICKY
    }

    fun startMapleService(intent: Intent?, startId: Int) {
        if (!isRunning()) {
            val channelId = intent?.getStringExtra("channel_id") ?: "default_id"
            val pendingIntent : PendingIntent?
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pendingIntent = intent?.getParcelableExtra("pending_intent", PendingIntent::class.java)
            } else {
                pendingIntent = intent?.getParcelableExtra<PendingIntent>("pending_intent")
            }

            notifyServerStarting()

            val initializationThread = Thread {
                val notificationManager = getSystemService(ComponentActivity.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(channelId, "OpenMapleServer Channel", NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)

                val builder = NotificationCompat.Builder(this, channelId)
                builder.setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("OpenMapleServer Notification")
                    .setContentText("OpenMapleServer is Running")
                    .setContentIntent(pendingIntent)
                    .setChannelId(channelId)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                val notification = builder.build()
                startForeground(startId, notification)

                val args = arrayOf("-Xmx2048m", "-Dwz-path=wz", "-Djava.net.preferIPv4Stack=true")
                Server.getInstance(applicationContext)
                Server.main(args)

                notifyServerStarted()
            }
            initializationThread.start()
        }
    }

    fun stopMapleService() {
        notifyServerStopping()
        val initializationThread = Thread {
            Server.getInstance(applicationContext).shutdown(false).run()
            notifyServerStopped()
        }
        initializationThread.start()
    }

    private fun notifyServerStarted() {
        val intent = Intent("MapleServerMessage")
        intent.putExtra("Status", "ServerStarted")
        intent.setPackage(applicationContext.packageName)
        applicationContext.sendBroadcast(intent)
    }

    private fun notifyServerStarting() {
        val intent = Intent("MapleServerMessage")
        intent.putExtra("Status", "ServerStarting")
        intent.setPackage(applicationContext.packageName)
        applicationContext.sendBroadcast(intent)
    }

    private fun notifyServerStopping() {
        val intent = Intent("MapleServerMessage")
        intent.putExtra("Status", "ServerStopping")
        intent.setPackage(applicationContext.packageName)
        applicationContext.sendBroadcast(intent)
    }

    private fun notifyServerStopped() {
        val intent = Intent("MapleServerMessage")
        intent.putExtra("Status", "ServerStopped")
        intent.setPackage(applicationContext.packageName)
        applicationContext.sendBroadcast(intent)
    }
}