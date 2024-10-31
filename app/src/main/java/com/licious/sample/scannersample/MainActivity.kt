package com.licious.sample.scannersample

import android.app.Activity
import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutomaticTheme {
                MainScreen(onScanQrClicked = { openScanner() })
            }
        }
    }

    // Открытие ScannerActivity
    private fun openScanner() {
        val intent = Intent(this, ScannerActivity::class.java)
        scannerLauncher.launch(intent)  // Запускаем ScannerActivity через scannerLauncher
    }
}



    @Composable
fun MainScreen(onScanQrClicked: () -> Unit) {
    val isOnline = checkNetworkStatus()
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
