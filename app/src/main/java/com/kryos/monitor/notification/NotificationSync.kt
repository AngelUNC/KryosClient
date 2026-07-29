package com.kryos.monitor.notification

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.kryos.monitor.notification.NotificationApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationSync private constructor(
    context: Context
) {

    companion object {

        private const val TAG = "NotificationSync"
        private const val SYNC_BATCH_SIZE = 100

        @Volatile
        private var instance: NotificationSync? = null

        fun getInstance(context: Context): NotificationSync {
            return instance ?: synchronized(this) {
                instance ?: NotificationSync(
                    context.applicationContext
                ).also {
                    instance = it
                }
            }
        }
    }

    private val appContext = context.applicationContext

    private val database = NotificationDatabase(appContext)

    private val api = NotificationApi.getInstance(appContext)

    



    suspend fun send(report: NotificationReport): Boolean =
        withContext(Dispatchers.IO) {

            try {

                if (!isInternetAvailable()) {

                    Log.i(TAG, "Sin Internet, guardando offline")

                    database.saveNotification(
                        report.toEntity()
                    )

                    return@withContext false
                }

                val sent = api.sendNotification(report)

                if (!sent) {

                    Log.i(TAG, "Error enviando, guardando offline")

                    database.saveNotification(
                        report.toEntity()
                    )

                    return@withContext false
                }

                return@withContext true

            } catch (e: Exception) {

                Log.e(TAG, "Error enviando notificación", e)

                database.saveNotification(
                    report.toEntity()
                )

                return@withContext false
            }
        }

    


    suspend fun syncOffline(): Boolean =
        withContext(Dispatchers.IO) {

            if (!isInternetAvailable()) {
                return@withContext false
            }

            val pending =
                database.getUnsyncedNotifications(
                    SYNC_BATCH_SIZE
                )

            if (pending.isEmpty()) {
                return@withContext true
            }

            val syncedIds = mutableListOf<Long>()

            for (entity in pending) {

                val success =
                    api.sendNotification(
    entity.toReport(isOfflineRecovery = true)
                    )

                if (success) {

                    syncedIds.add(entity.id)

                } else {

                    break
                }
            }

            if (syncedIds.isNotEmpty()) {

                database.markSynced(syncedIds)

                database.cleanupOldSynced()

                Log.i(
                    TAG,
                    "Sincronizadas ${syncedIds.size} notificaciones"
                )
            }

            return@withContext syncedIds.isNotEmpty()
        }

    fun getPendingCount(): Int {
        return database.getPendingCount()
    }

    private fun isInternetAvailable(): Boolean {

        val cm =
            appContext.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        val network = cm.activeNetwork ?: return false

        val capabilities =
            cm.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
    }
}




private fun NotificationReport.toEntity(): NotificationEntity {

    return NotificationEntity(

        deviceId = deviceId,
        deviceName = deviceName,
        packageName = packageName,
        notificationId = notificationId,
        tag = tag,
        key = key,
        title = title,
        text = text,
        postTime = postTime,
        receivedAt = System.currentTimeMillis(),
        isOngoing = isOngoing,
        isClearable = isClearable
    )
}

private fun NotificationEntity.toReport(
    isOfflineRecovery: Boolean = false
): NotificationReport {






    return NotificationReport(

        deviceId = deviceId,
        deviceName = deviceName,
        packageName = packageName,
        notificationId = notificationId,
        tag = tag,
        key = key,
        title = title,
        text = text,
        postTime = postTime,
        isOngoing = isOngoing,
        isClearable = isClearable,
	isOfflineRecovery = isOfflineRecovery
    )
}
