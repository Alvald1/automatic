package com.automatic.main


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.automatic.main.ui.ScannerActivity
import com.automatic.main.ui.theme.AutomaticTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


// Данные для десериализации JSON-ответа
@Serializable
data class ResponseData(
    val message: String,
    val code: Int,
    val name: String? = null
)

@Serializable
data class EncryptedData(
    val encryptedKey: String,
    val iv: String,
    val ciphertext: String
)


var device_name_: String? = "Neo"
var id_: String? = ""
var pem_pub_: String? = ""
var scanStatus: Boolean = false


class MainActivity : ComponentActivity() {
    private val notificationHistory = mutableStateListOf<String>() // Хранение истории уведомлений
    private lateinit var sharedPrefs: SharedPreferences


    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = result.data?.getStringExtra("SCAN_RESULT")
                scanResult?.let {
                    turnOff(this)
                    val url = "http://213.189.205.6:8080/api"
                    val params = mapOf("key" to it, "status" to "on")
                    sendPostRequest(url, params) { response ->
                        if (response != null) {
                            val obj = Json.decodeFromString<ResponseData>(response)
                            if (obj.code != 0) {
                                showToast(this, obj.message)
                            } else {
                                pem_pub_ = obj.message
                                device_name_ = obj.name
                                id_ = it
                                saveResult(this, "DEVICE_NAME", device_name_)
                                saveResult(this, "ID", it)
                                scanStatus = true
                            }
                        } else {
                            println("Failed to get response")
                        }
                    }
                }
            }
        }


    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(
            notificationReceiver, IntentFilter("com.automatic.NOTIFICATION_LISTENER"),
            RECEIVER_NOT_EXPORTED
        )
        sharedPrefs = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        loadNotificationHistory()
        // Загрузка сохранённого результата сканирования
        id_ = loadResult(this, "ID")
        device_name_ = loadResult(this, "DEVICE_NAME")

        requestNotificationPermission_post()
        //startForegroundService(Intent(this, MyForegroundService::class.java))


        setContent {
            AutomaticTheme {
                AppNavigation()

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        turnOff(this)
        unregisterReceiver(notificationReceiver)
    }

    private fun openScanner() {
        val intent = Intent(this, ScannerActivity::class.java)
        scannerLauncher.launch(intent)
    }

    // Логика очистки истории
    private fun clearHistory() {
        notificationHistory.clear()
        saveNotificationHistory()
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    onScanQrClicked = { openScanner() },
                    onHistoryClicked = { navController.navigate("history") }
                )
            }
            composable("history") {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    notificationHistory = notificationHistory,
                    onClearHistory = { clearHistory() })
            }
        }
    }

    private fun saveNotificationHistory() {
        val historyString =
            notificationHistory.joinToString(separator = "||") // Используем разделитель "||"
        sharedPrefs.edit().putString("notification_history", historyString).apply()
    }

    // Загружаем историю уведомлений из SharedPreferences
    private fun loadNotificationHistory() {
        val historyString = sharedPrefs.getString("notification_history", null)
        if (!historyString.isNullOrEmpty()) {
            val historyList = historyString.split("||") // Разделяем строки по "||"
            notificationHistory.clear()
            notificationHistory.addAll(historyList)
        }
    }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.getStringExtra("package_name")
            val title = intent?.getStringExtra("title")
            val text = intent?.getStringExtra("text")

            // Добавление уведомления в историю
            val notificationInfo = "Пакет: $packageName\nЗаголовок: $title\nТекст: $text"
            notificationHistory.add(notificationInfo)
            saveNotificationHistory()

            filter(packageName, title, text)
        }
    }

    private val requestPermissionLauncher_post =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                showToast(this, "Разрешение на отправку уведомления предоставлено")
            } else {
                showToast(this, "Разрешение на отправку уведомления отклонено")
            }
        }

    private fun requestNotificationPermission_post() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    showToast(this, "Разрешение уже предоставлено")
                }

                else -> {
                    // Запрос разрешения
                    requestPermissionLauncher_post.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

}


fun sendPostRequest(
    url: String,
    params: Map<String, String>,
    callback: (String?) -> Unit
) {

    val formBodyBuilder = FormBody.Builder()
    for ((key, value) in params) {
        formBodyBuilder.add(key, value)
    }
    val requestBody = formBodyBuilder.build()

    // Создаем HTTP-клиент
    val client = OkHttpClient()

    // Формируем запрос
    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    // Выполняем запрос
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


fun turnOn(context: Context) {
    if (id_ != null && id_ != "") {
        val id: String = id_ as String
        val url = "http://213.189.205.6:8080/api"
        val params = mapOf("key" to id, "status" to "on")
        sendPostRequest(url, params) { response ->
            if (response != null) {
                val obj = Json.decodeFromString<ResponseData>(response)
                if (obj.code == 0) {
                    pem_pub_ = obj.message
                    scanStatus = true
                    showToast(context, "Ok")
                } else
                    showToast(context, obj.message)
            } else {
                println("Failed to get response")
            }
        }
    } else {
        showToast(context, "Устройство не подключено")
    }
}

fun Test(context: Context) {
    if (id_ != null && id_ != "") {
        val id: String = id_ as String
        val url = "http://213.189.205.6:8080/api"
        val pem_pub = pem_pub_?.trimIndent()

        val encryptor = pem_pub?.let { HybridEncryptor(it) }
        val encryptedDataMap = encryptor!!.encrypt(url)
        val encryptedData = EncryptedData(
            encryptedKey = encryptedDataMap["encryptedKey"] ?: "",
            iv = encryptedDataMap["iv"] ?: "",
            ciphertext = encryptedDataMap["ciphertext"] ?: ""
        )
        val jsonString = Json.encodeToString(encryptedData)

        val params = mapOf("key" to id, "message" to jsonString)
        sendPostRequest(url, params) { response ->
            if (response != null) {
                val obj = Json.decodeFromString<ResponseData>(response)
                if (obj.code == 0) {
                    showToast(context, obj.message)
                } else
                    showToast(context, obj.message)
            } else {
                println("Failed to get response")
            }
        }
    } else {
        showToast(context, "Устройство не подключено")
    }
}

fun turnOff(context: Context) {
    if (id_ != null && id_ != "") {
        val id: String = id_ as String
        val url = "http://213.189.205.6:8080/api"
        val params = mapOf("key" to id, "status" to "off")
        sendPostRequest(url, params) { response ->
            if (response != null) {
                val obj = Json.decodeFromString<ResponseData>(response)
                if (obj.code == 0) {
                    scanStatus = false
                    showToast(context, obj.message)
                } else
                    showToast(context, obj.message)
            } else {
                println("Failed to get response")
            }
        }
    } else {
        showToast(context, "Устройство не подключено")
    }
}


fun showToast(context: Context, message: String) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

fun saveResult(context: Context, key: String, scanResult: String?) {
    val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("ScannerPreferences", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putString(key, scanResult)
        apply()
    }
}

fun loadResult(context: Context, key: String): String? {
    val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("ScannerPreferences", Context.MODE_PRIVATE)
    return sharedPreferences.getString(key, null)
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


fun checkNotificationPermission(context: Context): Boolean {
    val enabledListeners =
        Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
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
fun MainScreen(
    onScanQrClicked: () -> Unit,
    onHistoryClicked: () -> Unit
) {
    val isOnlineState = remember { mutableStateOf(false) }
    val context = LocalContext.current
    var hasNotificationPermission by remember { mutableStateOf(checkNotificationPermission(context)) }
    var nicknameState by remember { mutableStateOf(device_name_) }

    LaunchedEffect(Unit) {
        while (true) {
            hasNotificationPermission = checkNotificationPermission(context)
            nicknameState = device_name_
            isOnlineState.value = scanStatus
            kotlinx.coroutines.delay(1000)
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
            val name = nicknameState ?: "Neo"
            Greeting(name = name)
            Spacer(modifier = Modifier.height(16.dp))
            NetworkStatus(isOnlineState.value)
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onScanQrClicked) {
                Text(text = "Сканировать API-QR")
            }
            if (!hasNotificationPermission) {
                Button(onClick = { requestNotificationPermission(context) }) {
                    Text(text = "Разрешения не получены. Нажмите для перехода в настройки.")
                }
            }

            Button(onClick = { turnOn(context) }) {
                Text(text = "Включить")
            }

            Button(onClick = { turnOff(context) }) {
                Text(text = "Выключить")
            }
            Button(onClick = { Test(context) }) {
                Text(text = "Тест")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onHistoryClicked) {
                Text(text = "История")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    notificationHistory: List<String>,
    onClearHistory: () -> Unit
) {
    // Состояние для списка прокрутки
    val listState = rememberLazyListState()

    // Прокрутка до последнего элемента при изменении данных
    LaunchedEffect(notificationHistory) {
        if (notificationHistory.isEmpty())
            listState.scrollToItem(0)
        else
            listState.scrollToItem(notificationHistory.size - 1)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("История") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onClearHistory) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear History")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = listState,
            reverseLayout = true // Инвертирует порядок отображения элементов
        ) {
            items(notificationHistory) { notification ->
                Text(
                    text = notification,
                    modifier = Modifier
                        .padding(8.dp)
                )
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
fun Greeting(name: String?, modifier: Modifier = Modifier) {

    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

