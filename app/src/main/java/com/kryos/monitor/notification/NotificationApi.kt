package com.kryos.monitor.notification

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.concurrent.TimeUnit

class NotificationApi private constructor(context: Context) {

    companion object {
        private const val TAG = "KryosNotificationApi"

        private const val SERVER_URL = "http://kryos001.duckdns.org:8081"
        private const val NOTIFICATION_ENDPOINT =
            "$SERVER_URL/api/notifications"

        @Volatile
        private var instance: NotificationApi? = null

        fun getInstance(context: Context): NotificationApi {
            return instance ?: synchronized(this) {
                instance ?: NotificationApi(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .build()

    private val jsonMediaType =
        "application/json; charset=utf-8".toMediaType()

    suspend fun sendNotification(notification: NotificationReport): Boolean =
        withContext(Dispatchers.IO) {

            try {

                val json = gson.toJson(notification)

                val body = json.toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url(NOTIFICATION_ENDPOINT)
                    .post(body)
                    .header("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->

                    if (response.isSuccessful) {
                        Log.i(TAG, "Notificación enviada")
                        return@withContext true
                    }

                    Log.w(TAG, "Servidor respondió ${response.code}")
                    return@withContext false
                }

            } catch (e: IOException) {

                Log.e(TAG, "Sin conexión: ${e.message}")
                return@withContext false

            } catch (e: Exception) {

                Log.e(TAG, "Error: ${e.message}")
                return@withContext false
            }
        }
}
