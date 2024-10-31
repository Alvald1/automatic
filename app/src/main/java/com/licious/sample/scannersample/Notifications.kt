package com.licious.sample.scannersample

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MyNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Здесь вы можете обрабатывать каждое новое уведомление
        val notification = sbn.notification
        val packageName = sbn.packageName
        val title = notification?.extras?.getString("android.title")
        val text = notification?.extras?.getString("android.text")

        // Логирование или обработка уведомления
        Log.d("NotificationListener", "Уведомление от $packageName: $title - $text")

        // Вы можете отправить уведомление в UI или обработать его другим способом
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Обработка удаления уведомления (если нужно)
        Log.d("NotificationListener", "Уведомление удалено: ${sbn.packageName}")
    }
}
