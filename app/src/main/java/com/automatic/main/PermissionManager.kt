package com.automatic.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionManager(
    private val activity: Activity,
    private val registry: ActivityResultRegistry
) {

    private lateinit var requestPermissionLauncher_post: ActivityResultLauncher<String>
    private lateinit var requestPermissionLauncher_listen: ActivityResultLauncher<Intent>

    init {
        requestPermissionLauncher_post =
            registry.register(
                "requestPermission_post",
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (!isGranted) {
                    showToast(activity, "Разрешение на отправку уведомления отклонено")
                }
            }

        requestPermissionLauncher_listen =
            registry.register(
                "requestPermission_listen",
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    if (!checkNotificationPermission_listen()) {
                        showToast(activity, "Разрешение на прослушивание уведомлений отклонено")
                    }
                }
            }
    }

    fun requestNotificationPermission_post() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    showToast(activity, "Разрешение на отправку уведомлений уже предоставлено")
                }

                else -> {
                    requestPermissionLauncher_post.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    fun requestNotificationPermission_listen() {
        if (!checkNotificationPermission_listen()) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            requestPermissionLauncher_listen.launch(intent)
        } else {
            showToast(activity, "Разрешение на прослушивание уведомлений уже предоставлено")
        }
    }

    private fun checkNotificationPermission_listen(): Boolean {
        val enabledListeners =
            Settings.Secure.getString(activity.contentResolver, "enabled_notification_listeners")
        return enabledListeners != null && enabledListeners.contains(activity.packageName)
    }

    @SuppressLint("BatteryLife")
    fun checkAndDisableBatteryOptimization(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            Log.d("BatteryOptimization", "Battery optimization is enabled, requesting to disable.")
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("BatteryOptimization", "Failed to open battery optimization settings", e)
            }
        } else {
            Log.d("BatteryOptimization", "Battery optimization is already disabled.")
        }
    }

    fun checkAndRedirectAutoStart(context: Context) {
        if (checkAutoStartForMIUI(context)) return
        if (checkAutoStartForHuawei(context)) return
        if (checkAutoStartForOppo(context)) return
        if (checkAutoStartForVivo(context)) return
        if (checkAutoStartForSamsung(context)) return

        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
            showToast(context, "Включите автозапуск")
            Log.d("AutoStart", "Redirecting to default application settings as fallback.")
        } catch (e: Exception) {
            Log.e("AutoStart", "Failed to open default application settings.", e)
        }
    }

    private fun checkAutoStartForMIUI(context: Context): Boolean {
        try {
            val intent = Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            )
            context.packageManager.resolveActivity(intent, 0) ?: return false
            context.startActivity(intent)
            Log.d("AutoStart", "Redirecting to MIUI AutoStart settings.")
            return true
        } catch (e: Exception) {
            Log.e("AutoStart", "Failed to open MIUI AutoStart settings.", e)
            return false
        }
    }

    private fun checkAutoStartForHuawei(context: Context): Boolean {
        try {
            val intent = Intent().setComponent(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            )
            context.packageManager.resolveActivity(intent, 0) ?: return false
            context.startActivity(intent)
            Log.d("AutoStart", "Redirecting to Huawei AutoStart settings.")
            return true
        } catch (e: Exception) {
            Log.e("AutoStart", "Failed to open Huawei AutoStart settings.", e)
            return false
        }
    }

    private fun checkAutoStartForOppo(context: Context): Boolean {
        try {
            val intent = Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            )
            context.packageManager.resolveActivity(intent, 0) ?: return false
            context.startActivity(intent)
            Log.d("AutoStart", "Redirecting to Oppo AutoStart settings.")
            return true
        } catch (e: Exception) {
            Log.e("AutoStart", "Failed to open Oppo AutoStart settings.", e)
            return false
        }
    }

    private fun checkAutoStartForVivo(context: Context): Boolean {
        try {
            val intent = Intent().setComponent(
                ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            )
            context.packageManager.resolveActivity(intent, 0) ?: return false
            context.startActivity(intent)
            Log.d("AutoStart", "Redirecting to Vivo AutoStart settings.")
            return true
        } catch (e: Exception) {
            Log.e("AutoStart", "Failed to open Vivo AutoStart settings.", e)
            return false
        }
    }

    private fun checkAutoStartForSamsung(context: Context): Boolean {
        try {
            val intent = Intent().setComponent(
                ComponentName(
                    "com.samsung.android.sm",
                    "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity"
                )
            )
            context.packageManager.resolveActivity(intent, 0) ?: return false
            context.startActivity(intent)
            Log.d("AutoStart", "Redirecting to Samsung AutoStart settings.")
            return true
        } catch (e: Exception) {
            Log.e("AutoStart", "Failed to open Samsung AutoStart settings.", e)
            return false
        }
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, message.length).show()
        }
    }
}