package com.kryos.monitor.receiver

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import com.kryos.monitor.command.DeviceLockdownManager

class KryosDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i("KryosDeviceAdmin", "Device Owner activado, aplicando restricciones")
        DeviceLockdownManager.applyLockdown(context)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w("KryosDeviceAdmin", "Device Owner desactivado")
    }
}
