package com.kryos.monitor.notification

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.kryos.monitor.notification.NotificationApi
import com.kryos.monitor.notification.NotificationReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "KryosNotification"
    }

    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "===================================")
        Log.i(TAG, "onCreate()")
        Log.i(TAG, "SDK=${Build.VERSION.SDK_INT}")
        Log.i(TAG, "===================================")
    }

    override fun onDestroy() {

        Log.i(TAG, "===================================")
        Log.i(TAG, "onDestroy()")
        Log.i(TAG, "===================================")

        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        Log.i(TAG, "===================================")
        Log.i(TAG, "onListenerConnected()")
        Log.i(TAG, "NotificationListener conectado")
        Log.i(TAG, "===================================")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()

        Log.w(TAG, "===================================")
        Log.w(TAG, "onListenerDisconnected()")
        Log.w(TAG, "NotificationListener desconectado")
        Log.w(TAG, "===================================")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {

	 if (sbn.packageName == packageName) {
        return
    }

        Log.i(TAG, "===================================")
        Log.i(TAG, "onNotificationPosted() INVOCADO")
        Log.i(TAG, "===================================")

        try {

            val notification = sbn.notification
            val extras = notification.extras

            val title =
                extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()

            val text =
                extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()

            Log.i(TAG, "========== NUEVA NOTIFICACIÓN ==========")
            Log.i(TAG, "Paquete      : ${sbn.packageName}")
            Log.i(TAG, "ID           : ${sbn.id}")
            Log.i(TAG, "Tag          : ${sbn.tag}")
            Log.i(TAG, "Key          : ${sbn.key}")
            Log.i(TAG, "Post Time    : ${sbn.postTime}")
            Log.i(TAG, "Title        : $title")
            Log.i(TAG, "Text         : $text")
            Log.i(TAG, "Clearable    : ${sbn.isClearable}")
            Log.i(TAG, "Ongoing      : ${sbn.isOngoing}")
            Log.i(TAG, "========================================")

            val report = NotificationReport(
                deviceId = Build.MODEL,
                deviceName = Build.MODEL,
                packageName = sbn.packageName,
                notificationId = sbn.id,
                tag = sbn.tag,
                key = sbn.key,
                title = title,
                text = text,
                postTime = sbn.postTime,
                isOngoing = sbn.isOngoing,
                isClearable = sbn.isClearable,
		isOfflineRecovery = false
            )

            CoroutineScope(Dispatchers.IO).launch {

                val ok = NotificationSync
    .getInstance(this@NotificationListener)
    .send(report)

                Log.i(TAG, "Notificación enviada al servidor: $ok")
            }

        } catch (e: Exception) {

            Log.e(TAG, "Excepción en onNotificationPosted()", e)
        }

        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {

        Log.i(TAG, "===================================")
        Log.i(TAG, "onNotificationRemoved()")
        Log.i(TAG, "Paquete : ${sbn.packageName}")
        Log.i(TAG, "ID      : ${sbn.id}")
        Log.i(TAG, "===================================")

        super.onNotificationRemoved(sbn)
    }

    override fun onListenerHintsChanged(hints: Int) {
        super.onListenerHintsChanged(hints)

        Log.i(TAG, "onListenerHintsChanged(): $hints")
    }

    override fun onInterruptionFilterChanged(interruptionFilter: Int) {
        super.onInterruptionFilterChanged(interruptionFilter)

        Log.i(
            TAG,
            "onInterruptionFilterChanged(): $interruptionFilter"
        )
    }
}
