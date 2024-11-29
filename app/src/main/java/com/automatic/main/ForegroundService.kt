package com.automatic.main


import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class MyForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel() // Для Android 8.0+
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Приложение работает")
            .setContentText("Сервис активен в фоне")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()

        startForeground(NOTIFICATION_ID, notification) // Запускает сервис как foreground
        // TODO: Ваша логика (например, запуск фоновой задачи)

        return START_STICKY // Перезапуск сервиса, если ОС его завершит
    }

    override fun onDestroy() {
        super.onDestroy()
        // Очистите ресурсы, если нужно
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Не используется, если сервис не связан с Activity
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
        const val NOTIFICATION_ID = 1
    }
}
