package com.kryos.monitor.api

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.kryos.monitor.data.DeviceReport
import com.kryos.monitor.data.OfflineDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit





class ApiService private constructor(context: Context) {

    companion object {
        private const val TAG = "KryosApiService"
        private const val SERVER_URL = "http://kryos001.duckdns.org:8080"
        private const val REPORT_ENDPOINT = "$SERVER_URL/api/report"
        private const val SYNC_BATCH_SIZE = 50

        @Volatile
        private var instance: ApiService? = null

        fun getInstance(context: Context): ApiService {
            return instance ?: synchronized(this) {
                instance ?: ApiService(context.applicationContext).also { instance = it }
            }
        }
    }

    private val gson = Gson()
    private val offlineDb = OfflineDatabase(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    




    suspend fun sendReport(report: DeviceReport): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(report)
            val body = json.toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url(REPORT_ENDPOINT)
                .post(body)
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Reporte enviado exitosamente: ${response.code}")
                    
                    syncOfflineData()
                    return@withContext true
                } else {
                    Log.w(TAG, "Error del servidor: ${response.code}")
                    saveOffline(report)
                    return@withContext false
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Sin conexión, guardando offline: ${e.message}")
            saveOffline(report)
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando reporte: ${e.message}")
            saveOffline(report)
            return@withContext false
        }
    }

    



    suspend fun syncOfflineData(): Boolean = withContext(Dispatchers.IO) {
        try {
            val pendingReports = offlineDb.getUnsyncedReports(SYNC_BATCH_SIZE)

            if (pendingReports.isEmpty()) {
                return@withContext true
            }

            Log.i(TAG, "Sincronizando ${pendingReports.size} reportes offline...")

            var syncedCount = 0

            for (report in pendingReports) {
                try {
                    val offlineReport = report.copy(isOfflineRecovery = true)
                    val json = gson.toJson(offlineReport)
                    val body = json.toRequestBody(jsonMediaType)

                    val request = Request.Builder()
                        .url(REPORT_ENDPOINT)
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            syncedCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error sincronizando reporte individual: ${e.message}")
                    break 
                }
            }

            if (syncedCount > 0) {
                offlineDb.markSynced(syncedCount)
                Log.i(TAG, "$syncedCount reportes sincronizados exitosamente")
            }

            return@withContext syncedCount > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización offline: ${e.message}")
            return@withContext false
        }
    }

    


    fun getPendingCount(): Int {
        return offlineDb.getPendingCount()
    }

    


    fun cleanup() {
        offlineDb.cleanupOldSynced()
    }

    


    private fun saveOffline(report: DeviceReport) {
        offlineDb.saveReport(report)
        Log.d(TAG, "Reporte guardado offline. Pendientes: ${offlineDb.getPendingCount()}")
    }

    


    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue

                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo IP local: ${e.message}")
        }
        return null
    }
}
