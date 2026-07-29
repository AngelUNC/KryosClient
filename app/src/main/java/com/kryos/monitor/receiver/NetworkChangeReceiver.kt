package com.kryos.monitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.kryos.monitor.api.ApiService
import com.kryos.monitor.service.MonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch






class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "KryosNetworkReceiver"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            if (isInternetAvailable(context)) {
                Log.i(TAG, "Conexión a internet detectada, sincronizando datos offline...")

                scope.launch {
                    try {
                        val apiService = ApiService.getInstance(context)

                        
                        apiService.syncOfflineData()
                        apiService.cleanup()

                        
                        if (!MonitoringService.isRunning) {
                            val prefs = context.getSharedPreferences("kryos_config", Context.MODE_PRIVATE)
                            if (prefs.getBoolean("monitoring_enabled", true)) {
                                MonitoringService.start(context)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sincronizando después de cambio de red: ${e.message}")
                    }
                }
            }
        }
    }

    


    private fun isInternetAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return false
                val capabilities = cm.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                @Suppress("DEPRECATION")
                val activeNetwork = cm.activeNetworkInfo
                @Suppress("DEPRECATION")
                activeNetwork?.isConnectedOrConnecting == true
            }
        } catch (e: Exception) {
            false
        }
    }
}