package com.automatic.main


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
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
import androidx.compose.foundation.layout.fillMaxWidth
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


// Данные для десериализации JSON-ответа


var device_name_: String? = "Neo"
var id_: String? = ""
var pem_pub_: String? = ""
var scanStatus: Boolean = false
val url = "http://213.189.205.6:8080/api"


class MainActivity : ComponentActivity() {
    private var notificationHistory =
        HashSet<String>()  // Mutable Set for storing notification history
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private lateinit var networkManager: NetworkManager
    private lateinit var deviceManager: DeviceManager
    private lateinit var notificationHistoryManager: NotificationHistoryManager

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanResult = result.data?.getStringExtra("SCAN_RESULT")
                scanResult?.let {

                    deviceManager.turnOff(this, id_) { _ -> }
                    deviceManager.turnOn(this, it) { responseData ->
                        when (responseData.code) {
                            0 -> {
                                pem_pub_ = responseData.message
                                device_name_ = responseData.name
                                id_ = it
                                sharedPreferencesManager.save("DEVICE_NAME", device_name_)
                                sharedPreferencesManager.save("ID", it)
                                scanStatus = true
                                showToast(this, "Устройство включено")
                            }

                            1 -> {
                                showToast(this, "Устройство уже авторизовано")
                            }

                            2 -> {
                                showToast(this, "Устройство не найдено")
                            }
                        }
                    }
                }
            }
        }


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
            Log.d("das", "Received notification: $notificationInfo")

            // Filtering the notification data
        }
    }


    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferencesManager = SharedPreferencesManager(this)
        networkManager = NetworkManager()
        deviceManager =
            DeviceManager(networkManager)
        notificationHistoryManager =
            NotificationHistoryManager(sharedPreferencesManager)


        registerReceiver(
            notificationReceiver, IntentFilter("com.automatic.NOTIFICATION_LISTENER"),
            RECEIVER_NOT_EXPORTED
        )


        notificationHistory = notificationHistoryManager.getHistory()
        id_ = sharedPreferencesManager.load("ID")
        device_name_ = sharedPreferencesManager.load("DEVICE_NAME")

        requestNotificationPermission_listen()
        requestNotificationPermission_post()

        //startForegroundService(Intent(this, MyForegroundService::class.java))

        val serviceIntent = Intent(this, NotificationService::class.java)
        startService(serviceIntent)

        setContent {
            AutomaticTheme {
                AppNavigation()

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun openScanner() {
        val intent = Intent(this, ScannerActivity::class.java)
        scannerLauncher.launch(intent)
    }

    // Логика очистки истории
    private fun clearHistory() {
        notificationHistory.clear()
        notificationHistoryManager.delete()
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    onScanQrClicked = { openScanner() },
                    deviceManager = deviceManager,
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

    private val requestPermissionLauncher_post =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
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
                    showToast(this, "Разрешение на отправку уведомлений уже предоставлено")
                }

                else -> {
                    // Запрос разрешения
                    requestPermissionLauncher_post.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private val requestPermissionLauncher_listen =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Check if permission was granted after navigating to settings
                if (!checkNotificationPermission_listen()) {
                    showToast(this, "Разрешение на прослушивание уведомлений отклонено")
                }
            }
        }

    private fun checkNotificationPermission_listen(): Boolean {
        val enabledListeners =
            Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners")
        return enabledListeners != null && enabledListeners.contains(this.packageName)
    }

    private fun requestNotificationPermission_listen() {
        if (!checkNotificationPermission_listen()) {
            // If permission isn't granted, launch the settings screen to enable it
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            requestPermissionLauncher_listen.launch(intent)
        } else {
            showToast(this, "Разрешение на прослушивание уведомлений уже предоставлено")
        }
    }


}


fun showToast(context: Context, message: String) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(context, message, message.length).show()
    }
}


@Composable
fun MainScreen(
    onScanQrClicked: () -> Unit,
    deviceManager: DeviceManager,
    onHistoryClicked: () -> Unit
) {
    val context = LocalContext.current

    var isOnlineState by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf(device_name_) }

    LaunchedEffect(Unit) {
        while (true) {
            nickname = device_name_
            isOnlineState = scanStatus
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
            val name = nickname ?: "Neo"
            Greeting(name = name)
            Spacer(modifier = Modifier.height(16.dp))
            NetworkStatus(isOnlineState)
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onScanQrClicked) {
                Text(text = "Сканировать API-QR")
            }

            Button(onClick = {
                deviceManager.turnOn(context, id_) { responseData ->
                    when (responseData.code) {
                        0 -> {
                            scanStatus = true
                            pem_pub_ = responseData.message
                            showToast(context, "Устройство включено")
                        }

                        1 -> {
                            showToast(context, "Устройство уже авторизовано")
                        }

                        2 -> {
                            showToast(context, "Устройство не найдено")
                        }
                    }
                }
            }) {
                Text(text = "Включить")
            }

            Button(onClick = {
                deviceManager.turnOff(context, id_) { responseData ->
                    when (responseData.code) {
                        0 -> {
                            scanStatus = false
                            showToast(context, "Устройство выключено")
                        }

                        2 -> {
                            showToast(context, "Устройство не найдено")
                        }
                    }
                }
            }) {
                Text(text = "Выключить")
            }

            Button(onClick = {
                pem_pub_?.let {
                    deviceManager.sendMessage(context, id_, it, "Test") { responseData ->
                        when (responseData.code) {
                            0 -> {
                                showToast(context, responseData.message)
                            }

                            2 -> {
                                showToast(context, "Устройство не найдено")
                            }
                        }
                    }
                }
            }) {
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
    notificationHistory: HashSet<String>,
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
            items(notificationHistory.toList()) { notification ->
                Text(
                    text = notification,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
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

