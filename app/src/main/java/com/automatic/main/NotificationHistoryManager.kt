package com.automatic.main

import android.util.Log

class NotificationHistoryManager(private val sharedPreferencesManager: SharedPreferencesManager) {

    private val historyKey = "notification_history"
    private val TAG = "NotificationHistoryManager"

    // Add a new notification to the history
    fun addNotification(notification: String) {
        val currentHistory = getHistory()
        val updatedHistory = currentHistory.toHashSet()
        updatedHistory.add(notification)

        sharedPreferencesManager.save(historyKey, updatedHistory)

        // Log the notification added
        Log.d(TAG, "Notification added: $notification")
        Log.d(TAG, "Updated history: $updatedHistory")
    }

    // Clear the notification history
    fun delete() {
        sharedPreferencesManager.save(historyKey, HashSet())

        // Log history cleared
        Log.d(TAG, "Notification history cleared")
    }

    // Retrieve the notification history
    fun getHistory(): HashSet<String> {
        val history = sharedPreferencesManager.load(historyKey, HashSet())

        // Log the retrieved history
        Log.d(TAG, "Notification history retrieved: $history")

        return history
    }
}
