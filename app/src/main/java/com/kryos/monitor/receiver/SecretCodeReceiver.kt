package com.kryos.monitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kryos.monitor.MainActivity
import android.util.Log

class SecretCodeReceiver : BroadcastReceiver() {

     override fun onReceive(context: Context, intent: Intent?) {
    Log.d("SecretCode", "Secret code recibido")

    val launch = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(launch)
}
}
