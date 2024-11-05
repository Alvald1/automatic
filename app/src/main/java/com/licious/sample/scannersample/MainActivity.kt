package com.licious.sample.scannersample


import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

import java.io.IOException
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import androidx.core.app.ActivityCompat
import com.licious.sample.scannersample.ui.ScannerActivity
import com.licious.sample.scannersample.ui.theme.AutomaticTheme
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull


class MainActivity : ComponentActivity() {

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = result.data?.getStringExtra("SCAN_RESULT")
                scanResult?.let {
                    Toast.makeText(this, "Результат сканирования: $it", Toast.LENGTH_SHORT).show()
                    val url = "https://cryptoflow.com.ru/"

                    val params = mapOf("key" to it, "status" to "on")

                    sendGetRequest(url, params) { response ->
                        if (response != null) {
                            println("Response: $response")
                        } else {
                            println("Failed to get response")
                        }
                    }
                    // Сохранение результата сканирования
                    saveScanResult(this, it)
                }
            }
        }

    // Новый обработчик для запроса разрешений
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            sendGlobalNotification(this, "Заголовок уведомления", "Текст уведомления")
        } else {
            Toast.makeText(this, "Разрешение на отправку уведомлений отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(notificationReceiver, IntentFilter("com.licious.sample.NOTIFICATION_LISTENER"),
            RECEIVER_NOT_EXPORTED
        )

        // Загрузка сохранённого результата сканирования
        val lastScanResult = loadScanResult(this)
        lastScanResult?.let {
            Toast.makeText(this, "Последний результат сканирования: $it", Toast.LENGTH_LONG).show()
        }

        setContent {
            AutomaticTheme {
                MainScreen(onScanQrClicked = { openScanner() })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(notificationReceiver)
    }

    private fun openScanner() {
        // Проверка наличия разрешения на отправку уведомлений для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Запрашиваем разрешение с помощью requestPermissionLauncher
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Отправляем уведомление, если разрешение уже предоставлено
            sendGlobalNotification(this, "Заголовок уведомления", "Текст уведомления")
        }

        val intent = Intent(this, ScannerActivity::class.java)
        scannerLauncher.launch(intent)
    }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.getStringExtra("package_name")
            val title = intent?.getStringExtra("title")
            val text = intent?.getStringExtra("text")
            filter(packageName,title,text)
            Toast.makeText(context, "Уведомление от $packageName: $title - $text", Toast.LENGTH_LONG).show()
        }
    }
}

fun sendGetRequest(url: String, params: Map<String, String>, callback: (String?) -> Unit) {
    // Создаем OkHttpClient
    val client = OkHttpClient()

    // Строим URL с параметрами запроса
    val httpUrlBuilder = url.toHttpUrlOrNull()?.newBuilder() ?: return
    for ((key, value) in params) {
        httpUrlBuilder.addQueryParameter(key, value)
    }

    val finalUrl = httpUrlBuilder.build().toString()

    // Создаем запрос GET
    val request = Request.Builder()
        .url(finalUrl)
        .get()
        .build()

    // Выполняем запрос асинхронно
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            // Возвращаем null, если произошла ошибка
            callback(null)
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                // Получаем тело ответа в виде строки
                val responseBody = response.body?.string()
                callback(responseBody)
            } else {
                // Возвращаем null, если ответ неуспешный
                callback(null)
            }
        }
    })
}



fun saveScanResult(context: Context, scanResult: String) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("ScannerPreferences", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putString("SCAN_RESULT", scanResult)
        apply()
    }
}

fun loadScanResult(context: Context): String? {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("ScannerPreferences", Context.MODE_PRIVATE)
    return sharedPreferences.getString("SCAN_RESULT", null)
}


fun filter(packageName: String?, title: String?, text: String?) {
    if (packageName == "ru.bankuralsib.mb.android") {
        val pattern = Regex(
            """^Perevod SBP ot ([A-Z ]+)\. iz ([A-Za-z ]+)\. Summa (\d+\.\d{2}) RUR na schet \*(\d{4})\. Ispolnen (0[1-9]|[12][0-9]|3[01])\.(0[1-9]|1[0-2])\.(\d{4}) ([01][0-9]|2[0-3]):([0-5][0-9])$"""
        )

        // Использование let для проверки на null и матчинг
        val matchResult = text?.let { pattern.matchEntire(it) }

        if (matchResult != null) {
            // Извлечение групп
            val sender = matchResult.groups[1]?.value
            val bankName = matchResult.groups[2]?.value
            val amount = matchResult.groups[3]?.value
            val accountNumber = matchResult.groups[4]?.value
            val day = matchResult.groups[5]?.value
            val month = matchResult.groups[6]?.value
            val year = matchResult.groups[7]?.value
            val hour = matchResult.groups[8]?.value
            val minute = matchResult.groups[9]?.value

            // Вывод извлечённых значений
            println("Отправитель: $sender")
            println("Банк: $bankName")
            println("Сумма: $amount RUR")
            println("Номер счета: $accountNumber")
            println("Дата исполнения: $day.$month.$year")
            println("Время исполнения: $hour:$minute")
        } else {
            println("Строка не соответствует ожидаемому формату.")
        }
    } else {
        println("Некорректное имя пакета.")
    }
}


@SuppressLint("MissingPermission")
fun sendGlobalNotification(activity: Activity, title: String, message: String) {
    // Проверка наличия разрешения POST_NOTIFICATIONS для Android 13 и выше
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Запрашиваем разрешение, если оно не предоставлено
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001 // Код запроса
            )
            return // Останавливаем выполнение до получения разрешения
        }
    }

    // Создаем канал уведомлений, если его нет
    val channelId = "global_channel_id"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelName = "Global Notifications"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = "Channel for global notifications"
        }

        // Регистрация канала в NotificationManager
        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Настройка намерения для открытия MainActivity при нажатии на уведомление
    val intent = Intent(activity, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    // Создание и настройка уведомления
    val builder = NotificationCompat.Builder(activity, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info) // Убедитесь, что добавлена иконка
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    // Проверяем наличие разрешения еще раз перед отправкой уведомления
    try {
        NotificationManagerCompat.from(activity).notify(1, builder.build()) // ID должен быть уникальным
    } catch (e: SecurityException) {
        Log.e("NotificationError", "Разрешение на уведомления отсутствует: ${e.message}")
    } catch (e: Exception) {
        Log.e("NotificationError", "Ошибка отправки уведомления: ${e.message}")
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
