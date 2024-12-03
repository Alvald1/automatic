package com.automatic.main


import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.automatic.main.ui.ScannerActivity
import com.automatic.main.ui.theme.AutomaticTheme
import kotlinx.coroutines.flow.MutableStateFlow


const val url = "http://213.189.205.6:8080/api"

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferencesManager = SharedPreferencesManager(application)

    // Use MutableStateFlow for state management
    val deviceName = MutableStateFlow(sharedPreferencesManager.load("DEVICE_NAME") ?: "Neo")
    val id = MutableStateFlow(sharedPreferencesManager.load("ID"))
    val pemPub = MutableStateFlow(sharedPreferencesManager.load("PEM_PUB"))
    val scanStatus =
        MutableStateFlow(sharedPreferencesManager.load("SCAN_STATUS")?.toBoolean() ?: false)

    fun updateScanStatus(status: Boolean) {
        scanStatus.value = status
        sharedPreferencesManager.save("SCAN_STATUS", status.toString())
    }

    fun saveDeviceInfo(id: String?, deviceName: String?, pemPub: String?) {
        this.id.value = id
        this.deviceName.value = deviceName ?: "Neo"
        this.pemPub.value = pemPub

        sharedPreferencesManager.save("ID", id)
        sharedPreferencesManager.save("DEVICE_NAME", deviceName)
        sharedPreferencesManager.save("PEM_PUB", pemPub)
    }
}

class MainActivity : ComponentActivity() {
    private var notificationHistory = mutableStateListOf<String>()
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private lateinit var networkManager: NetworkManager
    private lateinit var deviceManager: DeviceManager
    private lateinit var notificationHistoryManager: NotificationHistoryManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var mainViewModel: MainViewModel

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val scanResult = result.data?.getStringExtra("SCAN_RESULT")
                scanResult?.let {

                    deviceManager.turnOff(this, mainViewModel.id.value) { _ -> }
                    deviceManager.turnOn(this, it) { responseData ->
                        when (responseData.code) {
                            0 -> {
                                mainViewModel.saveDeviceInfo(
                                    it,
                                    responseData.name,
                                    responseData.message
                                )
                                mainViewModel.updateScanStatus(true)
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

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "notification_history") {
                // Update notificationHistory when it changes in SharedPreferences
                val updatedHistory = notificationHistoryManager.getHistory()
                notificationHistory.clear()
                notificationHistory.addAll(updatedHistory)
            }
        }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferencesManager = SharedPreferencesManager(this)
        networkManager = NetworkManager()
        deviceManager = DeviceManager(networkManager)
        notificationHistoryManager = NotificationHistoryManager(sharedPreferencesManager)

        notificationHistory = notificationHistoryManager.getHistory().toMutableStateList()

        permissionManager = PermissionManager(this, activityResultRegistry)

        permissionManager.requestNotificationPermission_listen()
        permissionManager.requestNotificationPermission_post()
        permissionManager.checkAndDisableBatteryOptimization(this)
        permissionManager.checkAndRedirectAutoStart(this)

        startForegroundService(Intent(this, MyForegroundService::class.java))

        // Register SharedPreferences listener
        sharedPreferencesManager.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        setContent {
            AutomaticTheme {
                AppNavigation()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister SharedPreferences listener
        sharedPreferencesManager.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun openScanner() {
        val intent = Intent(this, ScannerActivity::class.java)
        scannerLauncher.launch(intent)
    }

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
                    mainViewModel = mainViewModel,
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
}


fun showToast(context: Context, message: String) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(context, message, message.length).show()
    }
}


@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onScanQrClicked: () -> Unit,
    deviceManager: DeviceManager,
    onHistoryClicked: () -> Unit
) {
    val context = LocalContext.current

    val isOnlineState by mainViewModel.scanStatus.collectAsState()
    val nickname by mainViewModel.deviceName.collectAsState()

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
                deviceManager.turnOn(context, mainViewModel.id.value) { responseData ->
                    when (responseData.code) {
                        0 -> {
                            mainViewModel.updateScanStatus(true)
                            mainViewModel.pemPub.value = responseData.message
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
                deviceManager.turnOff(context, mainViewModel.id.value) { responseData ->
                    when (responseData.code) {
                        0 -> {
                            mainViewModel.updateScanStatus(false)
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
                mainViewModel.pemPub.value?.let {
                    deviceManager.sendMessage(
                        context,
                        mainViewModel.id.value,
                        it,
                        "Test"
                    ) { responseData ->
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
    notificationHistory: List<String>,
    onClearHistory: () -> Unit
) {
    val listState = rememberLazyListState()

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
            reverseLayout = true
        ) {
            items(notificationHistory) { notification ->
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


@Composable
fun Greeting(name: String?, modifier: Modifier = Modifier) {

    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

