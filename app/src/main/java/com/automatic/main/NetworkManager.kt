package com.automatic.main

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class NetworkManager(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()) {

    private val TAG = "NetworkManager"

    fun sendPostRequest(
        url: String,
        params: Map<String, String>,
        callback: (String?) -> Unit
    ) {
        Log.d(TAG, "sendPostRequest: Sending POST request to URL: $url with params: $params")

        val formBodyBuilder = FormBody.Builder()
        params.forEach { (key, value) ->
            formBodyBuilder.add(key, value)
            Log.d(TAG, "sendPostRequest: Param: $key = $value")  // Log each parameter
        }

        val requestBody = formBodyBuilder.build()

        val request = Request.Builder().url(url).post(requestBody).build()

        Log.d(TAG, "sendPostRequest: Request built. Executing request...")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "sendPostRequest: Request failed. Error: ${e.message}")
                e.printStackTrace()  // Log the stack trace
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "sendPostRequest: Response received: $responseBody")
                    callback(responseBody)
                } else {
                    Log.e(
                        TAG,
                        "sendPostRequest: Response failed with status code: ${response.code}"
                    )
                    callback(null)
                }
            }
        })
    }
}
