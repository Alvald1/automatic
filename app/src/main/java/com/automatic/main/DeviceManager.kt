package com.automatic.main

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ResponseData(
    val message: String = "",
    val code: Int = -1,
    val name: String? = null
)

class DeviceManager(private val networkManager: NetworkManager) {

    private val TAG = "DeviceManager"

    fun turnOn(context: Context, id: String?, callback: (ResponseData) -> Unit) {
        Log.d(TAG, "turnOn: Attempting to turn on device with ID: $id")
        turn(context, id, callback, "on")
    }

    fun turnOff(context: Context, id: String?, callback: (ResponseData) -> Unit) {
        Log.d(TAG, "turnOff: Attempting to turn off device with ID: $id")
        turn(context, id, callback, "off")
    }

    fun sendMessage(
        context: Context,
        id: String?,
        pem_pub: String,
        message: String,
        callback: (ResponseData) -> Unit
    ) {
        Log.d(TAG, "sendMessage: Attempting to send message to device with ID: $id")

        if (id.isNullOrEmpty()) {
            showToast(context, "Устройство не подключено")
            Log.d(TAG, "sendMessage: Device is not connected, message not sent.")
            return
        }

        val encryptor = HybridEncryptor(pem_pub)
        val encryptedDataMap = encryptor.encrypt(message)
        val encryptedData = EncryptedData(
            encryptedKey = encryptedDataMap["encryptedKey"] ?: "",
            iv = encryptedDataMap["iv"] ?: "",
            ciphertext = encryptedDataMap["ciphertext"] ?: ""
        )
        val jsonString = Json.encodeToString(encryptedData)
        val params = mapOf("key" to id, "message" to jsonString)

        Log.d(TAG, "sendMessage: Sending encrypted message: $jsonString")

        networkManager.sendPostRequest(url, params) { response ->
            if (response == null) {
                Log.d(TAG, "sendMessage: Response is null, handling failure.")
                callback(ResponseData())
            } else {
                Log.d(TAG, "sendMessage: Received response: $response")
                callback(handleDeviceResponse(response))
            }
        }
    }

    private fun turn(
        context: Context,
        id: String?,
        callback: (ResponseData) -> Unit,
        state: String
    ) {
        Log.d(TAG, "turn: Attempting to change device state to '$state' for device with ID: $id")

        if (id.isNullOrEmpty()) {
            showToast(context, "Устройство не подключено")
            Log.d(TAG, "turn: Device is not connected, state change not performed.")
            return
        }

        val params = mapOf("key" to id, "status" to state)

        networkManager.sendPostRequest(url, params) { response ->
            if (response == null) {
                Log.d(TAG, "turn: Response is null, handling failure.")
                callback(ResponseData())
            } else {
                Log.d(TAG, "turn: Received response: $response")
                callback(handleDeviceResponse(response))
            }
        }
    }

    private fun handleDeviceResponse(response: String?): ResponseData {
        return try {
            if (response != null) {
                Json.decodeFromString(response)
            } else {
                ResponseData(code = -1, message = "Empty response")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse response", e)
            ResponseData(code = -1, message = "Error parsing response")
        }
    }
}
