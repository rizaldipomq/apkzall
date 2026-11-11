package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class ZallBrowserApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_FLOATING_BROWSER,
                "Zall Floating Browser Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Layanan aktif untuk jendela browser mengambang dan floating bubble"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_FLOATING_BROWSER = "zall_floating_browser_channel"
    }
}
