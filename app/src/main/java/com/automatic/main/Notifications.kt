package com.automatic.main

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MyNotificationListenerService : NotificationListenerService() {

    private val TAG = "NotificationListenerService"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val title = sbn.notification?.extras?.getString("android.title")
        val text = sbn.notification?.extras?.getString("android.text")

        Log.d(TAG, "Notification posted: Package - $packageName, Title - $title, Text - $text")

        val intent = Intent("com.automatic.NOTIFICATION_LISTENER")
        intent.putExtra("package_name", packageName)
        intent.putExtra("title", title)
        intent.putExtra("text", text)

        sendBroadcast(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "Notification removed: ${sbn?.packageName}")
    }
}
