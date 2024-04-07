package com.mapleserver

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
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
        startMapleService()
        return START_NOT_STICKY
    }

    fun startMapleService() {
        if (!isRunning()) {
            notifyServerStarting()
            val initializationThread = Thread {
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