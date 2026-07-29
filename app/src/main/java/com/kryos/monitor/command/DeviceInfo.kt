package com.kryos.monitor.command

import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceInfo {

    fun hello(context: Context): Map<String, Any> {

        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        return mapOf(
            "cmd" to "hello",
            "deviceId" to deviceId,
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "android" to Build.VERSION.SDK_INT,
            "appVersion" to "1.0.0"
        )
    }
}
