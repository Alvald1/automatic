package com.licious.sample.scannersample


import okhttp3.*
import java.net.URL
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection


// Данные для десериализации JSON-ответа
@Serializable
data class ResponseData(val success: Boolean, val data: DataContent)

@Serializable
data class DataContent(val status: String, val name: String)

var device_name_:String?="Neo"
var id_:String?=""
var scanStatus:Boolean=false


class MainActivity : ComponentActivity() {


    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = result.data?.getStringExtra("SCAN_RESULT")
                scanResult?.let {

                    val url = "https://cryptoflow.com.ru/"
                    val params = mapOf("key" to it, "status" to "on")
                    sendGetRequest(url, params) { response ->
                        if (response != null) {
                            val obj=Json.decodeFromString<ResponseData>(response)
                            device_name_=obj.data.name
                            saveResult(this, "DEVICE_NAME", device_name_)
                            scanStatus=true
                        } else {
                            println("Failed to get response")
                        }
                    }
                    id_=it
                    saveResult(this, "SCAN_RESULT", it)
                }
            }
        }


    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(notificationReceiver, IntentFilter("com.licious.sample.NOTIFICATION_LISTENER"),
            RECEIVER_NOT_EXPORTED
        )

        // Загрузка сохранённого результата сканирования
        id_ = loadResult(this,"SCAN_RESULT")
        device_name_=loadResult(this,"DEVICE_NAME")


        setContent {
            AutomaticTheme {
                AppNavigation(onScanQrClicked = { openScanner() })

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(notificationReceiver)
    }

    fun openScanner() {
        val intent = Intent(this, ScannerActivity::class.java)
        scannerLauncher.launch(intent)
    }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val packageName = intent?.getStringExtra("package_name")
            val title = intent?.getStringExtra("title")
            val text = intent?.getStringExtra("text")
            filter(packageName,title,text)
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

fun turnOn(context: Context) {
    if (id_ != null && id_ != "") {
        val id:String= id_ as String
        val url = "https://cryptoflow.com.ru/"
        val params = mapOf("key" to id, "status" to "on")
        sendGetRequest(url, params) { response ->
            if (response != null) {
                scanStatus = true
            } else {
                println("Failed to get response")
            }
        }
    }else{
        Toast.makeText(context, "Устройство не подключено", Toast.LENGTH_SHORT).show()
    }
}

fun turnOff(context: Context) {
    if (id_ != null && id_ != "") {
        val id:String= id_ as String
        val url = "https://cryptoflow.com.ru/"
        val params = mapOf("key" to id, "status" to "off")
        sendGetRequest(url, params) { response ->
            if (response != null) {
                scanStatus=false
            } else {
                println("Failed to get response")
            }
        }
    }else{
        Toast.makeText(context, "Устройство не подключено", Toast.LENGTH_SHORT).show()
    }
}




fun saveResult(context: Context,key:String, scanResult: String?) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("ScannerPreferences", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putString(key, scanResult)
        apply()
    }
}

fun loadResult(context: Context,key:String): String? {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("ScannerPreferences", Context.MODE_PRIVATE)
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
    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return enabledListeners != null && enabledListeners.contains(context.packageName)
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

            Button(onClick = { turnOn(context) }) {
                Text(text = "Включить")
            }

            Button(onClick = { turnOff(context) }) {
                Text(text = "Выключить")
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
fun HistoryScreen(onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("История") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Здесь будет отображаться история.")
            // Здесь можно добавить код для отображения истории
        }
    }
}


@Composable
fun AppNavigation(onScanQrClicked: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onScanQrClicked =  onScanQrClicked,
                onHistoryClicked = { navController.navigate("history") }
            )
        }
        composable("history") {
            HistoryScreen(onBack = { navController.popBackStack() })
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

