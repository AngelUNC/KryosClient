package com.kryos.monitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kryos.monitor.service.MonitoringService






class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "KryosBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.i(TAG, "Dispositivo iniciado, arrancando Kryos Monitor...")
                
                try {
                    
                    val prefs = context.getSharedPreferences("kryos_config", Context.MODE_PRIVATE)
                    val monitoringEnabled = prefs.getBoolean("monitoring_enabled", true)
                    
                    if (monitoringEnabled) {
                        MonitoringService.start(context)
                        Log.i(TAG, "Servicio iniciado exitosamente después del boot")
                    } else {
                        Log.d(TAG, "Monitoreo deshabilitado, no se inicia el servicio")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error iniciando servicio después del boot: ${e.message}")
                }
            }
        }
    }
}