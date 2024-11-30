package com.automatic.main

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

    // Сохранение строки
    fun save(key: String, value: String?) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    // Сохранение множества строк
    fun save(key: String, value: HashSet<String>) {
        sharedPreferences.edit().putStringSet(key, value).apply()
    }

    // Загрузка строки
    fun load(key: String): String? = sharedPreferences.getString(key, null)

    // Загрузка множества строк
    fun load(key: String, default: HashSet<String>): HashSet<String> {
        return HashSet(sharedPreferences.getStringSet(key, default) ?: default)
    }
}
