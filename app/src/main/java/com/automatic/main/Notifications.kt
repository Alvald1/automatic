package com.automatic.main

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log


class MyNotificationListenerService : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationListener", "MyNotificationListenerService created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "MyNotificationListenerService connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d("NotificationListener", "Notification posted from ${sbn.packageName}")
        val packageName = sbn.packageName
        val title = sbn.notification?.extras?.getString("android.title")
        val text = sbn.notification?.extras?.getString("android.text")

        Log.d("NotificationListener", "Notification from $packageName: $title - $text")

        val intent = Intent("com.automatic.NOTIFICATION_LISTENER")
        intent.putExtra("package_name", packageName)
        intent.putExtra("title", title)
        intent.putExtra("text", text)
        sendBroadcast(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("NotificationListener", "Notification removed: ${sbn.packageName}")
    }
}