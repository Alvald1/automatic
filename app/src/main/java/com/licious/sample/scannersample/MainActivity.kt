package com.licious.sample.scannersample

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.licious.sample.scannersample.ui.ScannerActivity
import com.licious.sample.scannersample.ui.scanner.ScannerFragment
import com.licious.sample.scannersample.ui.theme.AutomaticTheme
import kotlin.reflect.KMutableProperty1
import android.provider.Settings


class MainActivity : ComponentActivity() {

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = result.data?.getStringExtra("SCAN_RESULT")
                scanResult?.let {
                    Toast.makeText(this, "Результат сканирования: $it", Toast.LENGTH_SHORT).show()
                }
            }
        }



    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter("com.licious.sample.NOTIFICATION_LISTENER")
        registerReceiver(notificationReceiver, filter,RECEIVER_EXPORTED)

        setContent {
            AutomaticTheme {
                MainScreen(onScanQrClicked = { openScanner() })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Отменяем регистрацию BroadcastReceiver, чтобы избежать утечек памяти
        unregisterReceiver(notificationReceiver)
    }

    fun sendNotification(context: Context, packageName: String, title: String, text: String) {
        val intent = Intent("com.licious.sample.NOTIFICATION_LISTENER")
        intent.putExtra("package_name", packageName)
        intent.putExtra("title", title)
        intent.putExtra("text", text)
        context.sendBroadcast(intent)
    }


    // Открытие ScannerActivity
    private fun openScanner() {
        sendNotification(this, "com.example.app", "Новое сообщение", "Это текст уведомления")
        val intent = Intent(this, ScannerActivity::class.java)
        scannerLauncher.launch(intent)  // Запускаем ScannerActivity через scannerLauncher
    }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.getStringExtra("package_name")
            val title = intent?.getStringExtra("title")
            val text = intent?.getStringExtra("text")

            // Отображение уведомления через Toast
            Toast.makeText(context, "Уведомление от $packageName: $title - $text", Toast.LENGTH_LONG).show()
        }
    }

}

fun checkNotificationPermission(context: Context): Boolean {
    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return enabledListeners != null && enabledListeners.contains(context.packageName)
}

@SuppressLint("InlinedApi")
fun requestNotificationPermission(context: Context) {
    if (!checkNotificationPermission(context)) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        context.startActivity(intent)
    }
}


    @Composable
fun MainScreen(onScanQrClicked: () -> Unit) {
    val isOnline = checkNetworkStatus()

        val context = LocalContext.current
        var hasNotificationPermission by remember { mutableStateOf(checkNotificationPermission(context)) }
        // Запускаем проверку разрешений каждый раз при изменении активности
        LaunchedEffect(Unit) {
            while (true) {
                hasNotificationPermission = checkNotificationPermission(context)
                kotlinx.coroutines.delay(1000)  // Проверка состояния каждые 1 секунду
            }
        }


    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Greeting(name = "Neo")
            Spacer(modifier = Modifier.height(16.dp))
            NetworkStatus(isOnline)
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onScanQrClicked) {
                Text(text = "Сканировать API-QR")
            }
            if (!hasNotificationPermission) {
                Button(onClick = { requestNotificationPermission(context) }) {
                    Text(text = "Разрешения не получены. Нажмите для перехода в настройки.")
                }
            }
        }
    }
}


@Composable
fun NetworkStatus(isOnline: Boolean) {
    val backgroundColor = if (isOnline) Color.Green else Color.Red
    val statusText = if (isOnline) "Online" else "Offline"

    Box(
        modifier = Modifier
            .padding(16.dp)
            .background(color = backgroundColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = statusText,
            color = Color.White
        )
    }
}





// Функция для отображения приветственного сообщения
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

// Пример реализации проверки статуса сети (замените на свою)
fun checkNetworkStatus(): Boolean {
    // Здесь вы можете использовать ConnectivityManager для проверки сети
    return true // Вернуть true или false в зависимости от состояния сети
}
