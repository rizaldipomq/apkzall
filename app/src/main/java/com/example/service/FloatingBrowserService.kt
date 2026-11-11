package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ZallBrowserApplication
import com.example.data.preferences.BrowserPreferences
import com.example.floating.FloatingWindowManager

class FloatingBrowserService : Service() {

    private lateinit var preferences: BrowserPreferences
    private var windowManagerHelper: FloatingWindowManager? = null

    override fun onCreate() {
        super.onCreate()
        preferences = BrowserPreferences(this)
        windowManagerHelper = FloatingWindowManager(
            context = this,
            preferences = preferences,
            onStopServiceRequested = {
                stopSelf()
            }
        )
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        // Always ensure foreground notification is posted immediately
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        when (action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_SHOW_BUBBLE -> {
                windowManagerHelper?.showBubble()
            }

            ACTION_OPEN_URL -> {
                val url = intent?.getStringExtra(EXTRA_URL) ?: preferences.homeUrl
                windowManagerHelper?.showBrowserWindow(url)
            }

            ACTION_SHOW_WINDOW, ACTION_START -> {
                if (preferences.startAsBubble) {
                    windowManagerHelper?.showBubble()
                } else {
                    val url = intent?.getStringExtra(EXTRA_URL)
                    windowManagerHelper?.showBrowserWindow(url)
                }
            }
        }

        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        windowManagerHelper?.onConfigurationChanged()
    }

    override fun onDestroy() {
        isRunning = false
        windowManagerHelper?.closeAll()
        windowManagerHelper = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showWindowIntent = Intent(this, FloatingBrowserService::class.java).apply {
            action = ACTION_SHOW_WINDOW
        }
        val showWindowPendingIntent = PendingIntent.getService(
            this,
            2,
            showWindowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingBrowserService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ZallBrowserApplication.CHANNEL_FLOATING_BROWSER)
            .setContentTitle("Zall Floating Browser Aktif")
            .setContentText("Browser mengambang siap digunakan di atas game & aplikasi")
            .setSmallIcon(R.drawable.ic_zall_logo)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(appPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Buka Browser", showWindowPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Tutup", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.action.START"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_SHOW_WINDOW = "com.example.action.SHOW_WINDOW"
        const val ACTION_SHOW_BUBBLE = "com.example.action.SHOW_BUBBLE"
        const val ACTION_OPEN_URL = "com.example.action.OPEN_URL"

        const val EXTRA_URL = "extra_url"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context, url: String? = null) {
            val intent = Intent(context, FloatingBrowserService::class.java).apply {
                action = if (url != null) ACTION_OPEN_URL else ACTION_START
                if (url != null) putExtra(EXTRA_URL, url)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingBrowserService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
