package com.automatic.main

import android.annotation.SuppressLint
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class MyNotificationListenerService : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationListener", "MyNotificationListenerService created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "MyNotificationListenerService connected")
    }

    @SuppressLint("SimpleDateFormat")
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d("NotificationListener", "Notification posted from ${sbn.packageName}")
        val packageName = sbn.packageName
        val title = sbn.notification?.extras?.getString("android.title")
        val text = sbn.notification?.extras?.getString("android.text")
        val postTime = sbn.postTime

        val date = Date(postTime)
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
        //dateFormat.timeZone = TimeZone.getTimeZone("UTC+3")
        val formattedDate = dateFormat.format(date)

        Log.d("NotificationListener", "Notification from $packageName: $title - $text")

        val intent = Intent("com.automatic.NOTIFICATION_LISTENER")
        intent.putExtra("package_name", packageName)
        intent.putExtra("title", title)
        intent.putExtra("text", text)
        intent.putExtra("time", formattedDate)
        sendBroadcast(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("NotificationListener", "Notification removed: ${sbn.packageName}")
    }
}