package com.automatic.main

import android.util.Log

class NotificationHistoryManager(private val sharedPreferencesManager: SharedPreferencesManager) {

    private val historyKey = "notification_history"
    private val TAG = "NotificationHistoryManager"

    // Add a new notification to the history
    fun addNotification(notification: String) {
        val currentHistory = getHistory()
        currentHistory.add(notification)

        sharedPreferencesManager.save(historyKey, currentHistory)

        // Log the notification added
        Log.d(TAG, "Notification added: $notification")
        Log.d(TAG, "Updated history: $currentHistory")
    }

    // Clear the notification history
    fun delete() {
        sharedPreferencesManager.save(historyKey, emptyList())

        // Log history cleared
        Log.d(TAG, "Notification history cleared")
    }

    // Retrieve the notification history
    fun getHistory(): MutableList<String> {
        val history = sharedPreferencesManager.load(historyKey, emptyList())

        // Log the retrieved history
        Log.d(TAG, "Notification history retrieved: $history")

        return history
    }
}
