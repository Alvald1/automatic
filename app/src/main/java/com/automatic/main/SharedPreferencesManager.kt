package com.automatic.main

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

    private val lock = Any()

    // Сохранение строки
    fun save(key: String, value: String?) {
        synchronized(lock) {
            sharedPreferences.edit().putString(key, value).apply()
        }
    }

    // Сохранение множества строк
    fun save(key: String, value: List<String>) {
        synchronized(lock) {
            sharedPreferences.edit().putStringSet(key, value.toSet()).apply()
        }
    }

    // Загрузка строки
    fun load(key: String): String? = sharedPreferences.getString(key, null)

    // Загрузка множества строк
    fun load(key: String, default: List<String>): MutableList<String> {
        return sharedPreferences.getStringSet(key, default.toSet())?.toMutableList()
            ?: default.toMutableList()
    }

    // Register listener
    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    // Unregister listener
    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
