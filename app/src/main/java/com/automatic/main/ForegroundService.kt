package com.automatic.main


import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import org.json.JSONObject

class MyForegroundService : Service() {

    private var scanStatus: Boolean = false
    private lateinit var notificationHistoryManager: NotificationHistoryManager
    private var notificationHistory = mutableStateListOf<String>()
    private lateinit var deviceManager: DeviceManager
    private var pem_pub_: String? = ""
    private var id_: String? = ""
    private lateinit var sharedPreferencesManager: SharedPreferencesManager

    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()

        // Создание уведомления для Foreground Service
        val notification = createNotification()

        // Устанавливаем уведомление
        startForeground(1, notification)

        // Initialize variables
        scanStatus = true // or load actual value
        notificationHistoryManager = NotificationHistoryManager(SharedPreferencesManager(this))
        notificationHistory = notificationHistoryManager.getHistory().toMutableStateList()
        deviceManager = DeviceManager(NetworkManager())
        pem_pub_ = "" // or load actual value
        id_ = "" // or load actual value

        // Initialize SharedPreferencesManager
        sharedPreferencesManager = SharedPreferencesManager(this)
        scanStatus = sharedPreferencesManager.load("SCAN_STATUS")?.toBoolean() ?: false
        id_ = sharedPreferencesManager.load("ID")
        pem_pub_ = sharedPreferencesManager.load("PEM_PUB")

        // Ensure id_ and pem_pub_ are initialized
        id_ = sharedPreferencesManager.load("ID")
        pem_pub_ = sharedPreferencesManager.load("PEM_PUB")

        // Register notificationReceiver
        registerReceiver(
            notificationReceiver,
            IntentFilter("com.automatic.NOTIFICATION_LISTENER"), RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Выполнение работы в службе
        return START_STICKY // или другой режим в зависимости от логики
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister notificationReceiver
        unregisterReceiver(notificationReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "foreground_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Service is Running") // Заголовок уведомления
            .setContentText("Performing background tasks") // Описание
            .setSmallIcon(R.drawable.notification_icon) // Иконка
            .setPriority(NotificationCompat.PRIORITY_LOW) // Для API < 26
            .setOngoing(true) // Фиксированное уведомление
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS) // Настройки по умолчанию
            .build()
    }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                // Load the latest scanStatus
                scanStatus = sharedPreferencesManager.load("SCAN_STATUS")?.toBoolean() ?: false

                val packageName = intent?.getStringExtra("package_name")
                val title = intent?.getStringExtra("title")
                val text = intent?.getStringExtra("text")
                val time = intent?.getStringExtra("time")

                val notificationInfo =
                    "Time: $time\nPackage: $packageName\nTitle: $title\nText: $text"

                if (scanStatus) {
                    notificationHistoryManager.addNotification(notificationInfo)
                    // No need to update notificationHistory here
                    // Changes will be reflected in MainActivity via SharedPreferences listener

                    filter(packageName, title, text)
                }
            } catch (e: Exception) {
                Log.e("ForegroundService", "Error processing notification", e)
            }
        }
    }

    private fun filter(packageName: String?, title: String?, text: String?) {
        if (packageName == "ru.bankuralsib.mb.android") {
            val pattern = Regex(
                """^Perevod SBP ot ([A-Z ]+)\. iz ([A-Za-z ]+)\. Summa (\d+\.\d{2}) RUR na schet \*(\d{4})\. Ispolnen (0[1-9]|[12][0-9]|3[01])\.(0[1-9]|1[0-2])\.(\d{4}) ([01][0-9]|2[0-3]):([0-5][0-9])$"""
            )

            val matchResult = text?.let { pattern.matchEntire(it) }

            if (matchResult != null) {
                // Extract information from the notification
                val sender = matchResult.groups[1]?.value
                val bankName = matchResult.groups[2]?.value
                val amount = matchResult.groups[3]?.value
                val accountNumber = matchResult.groups[4]?.value
                val day = matchResult.groups[5]?.value
                val month = matchResult.groups[6]?.value
                val year = matchResult.groups[7]?.value
                val hour = matchResult.groups[8]?.value
                val minute = matchResult.groups[9]?.value

                val json = JSONObject().apply {
                    put("sender", sender)
                    put("bankName", bankName)
                    put("amount", amount)
                    put("accountNumber", accountNumber)
                    put("day", day)
                    put("month", month)
                    put("year", year)
                    put("hour", hour)
                    put("minute", minute)
                }

                val jsonString = json.toString()

                deviceManager.sendMessage(
                    this@MyForegroundService,
                    id_,
                    pem_pub_ ?: "",
                    jsonString
                ) { responseData ->
                    when (responseData.code) {
                        0 -> {
                            showToast(this@MyForegroundService, "Сообщение доставлено")
                        }

                        1 -> {
                            showToast(this@MyForegroundService, "Сообщение уже зарегистрировано")
                        }

                        2 -> {
                            showToast(this@MyForegroundService, "Устройство не найдено")
                        }
                    }
                }

                Log.d(
                    "ForegroundService",
                    "Extracted Info - Sender: $sender, Bank: $bankName, Amount: $amount RUR, Account: $accountNumber, Date: $day.$month.$year, Time: $hour:$minute"
                )
            } else {
                Log.d("ForegroundService", "Text does not match expected format.")
            }
        } else {
            Log.d("ForegroundService", "Incorrect package name: $packageName")
        }
    }
}

