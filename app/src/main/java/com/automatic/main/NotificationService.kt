package com.automatic.main

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log

class NotificationService : Service() {

    private lateinit var notificationHistoryManager: NotificationHistoryManager
    private lateinit var sharedPreferencesManager: SharedPreferencesManager

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.getStringExtra("package_name")
            val title = intent?.getStringExtra("title")
            val text = intent?.getStringExtra("text")

            // Process notification
            val notificationInfo = "Package: $packageName\nTitle: $title\nText: $text"

            // Save notification to history
            notificationHistoryManager.addNotification(notificationInfo)

            // Log notification info for debugging
            Log.d(TAG, "Received notification: $notificationInfo")

            // Filtering the notification data
            filter(packageName, title, text)
        }
    }

    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()

        registerReceiver(
            notificationReceiver, IntentFilter("com.automatic.NOTIFICATION_LISTENER"),
            RECEIVER_NOT_EXPORTED
        )

        sharedPreferencesManager = SharedPreferencesManager(this)
        notificationHistoryManager = NotificationHistoryManager(sharedPreferencesManager)

        Log.d(TAG, "NotificationService created and receiver registered")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver when service is destroyed
        unregisterReceiver(notificationReceiver)
        Log.d(TAG, "NotificationService destroyed and receiver unregistered")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not required for this case
    }

    private fun filter(packageName: String?, title: String?, text: String?) {
        if (packageName == "ru.bankuralsib.mb.android") {
            val pattern = Regex(
                """^Perevod SBP ot ([A-Z ]+)\. iz ([A-Za-z ]+)\. Summa (\d+\.\d{2}) RUR na schet \*(\d{4})\. Ispolnen (0[1-9]|[12][0-9]|3[01])\.(0[1-9]|1[0-2])\.(\d{4}) ([01][0-9]|2[0-3]):([0-5][0-9])$"""
            )

            // Use let for null checks and regex matching
            val matchResult = text?.let { pattern.matchEntire(it) }

            if (matchResult != null) {
                // Extract matching groups
                val sender = matchResult.groups[1]?.value
                val bankName = matchResult.groups[2]?.value
                val amount = matchResult.groups[3]?.value
                val accountNumber = matchResult.groups[4]?.value
                val day = matchResult.groups[5]?.value
                val month = matchResult.groups[6]?.value
                val year = matchResult.groups[7]?.value
                val hour = matchResult.groups[8]?.value
                val minute = matchResult.groups[9]?.value

                // Log extracted values for debugging
                Log.d(
                    TAG,
                    "Extracted Info - Sender: $sender, Bank: $bankName, Amount: $amount RUR, Account: $accountNumber, Date: $day.$month.$year, Time: $hour:$minute"
                )
            } else {
                Log.d(TAG, "Text does not match expected format.")
            }
        } else {
            Log.d(TAG, "Incorrect package name: $packageName")
        }
    }

    companion object {
        private const val TAG = "NotificationService"
    }
}
