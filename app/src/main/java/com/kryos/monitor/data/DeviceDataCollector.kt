package com.kryos.monitor.data

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume





class DeviceDataCollector(private val context: Context) {

    companion object {
	private const val MAX_LOCATION_AGE_MS = 30000L
        private const val TAG = "KryosDataCollector"
        private const val LOCATION_TIMEOUT_MS = 8000L
        private const val LOCATION_UPDATE_INTERVAL = 4000L
        private const val LOCATION_FASTEST_INTERVAL = 2000L
    }

    
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
        LOCATION_UPDATE_INTERVAL
    ).apply {
        setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
        setWaitForAccurateLocation(true)
    }.build()

    
    private var locationCallback: LocationCallback? = null
    private var lastKnownLocation: Location? = null

    
    private var cachedBatteryLevel: Int = -1
    private var cachedIsCharging: Boolean = false

    init {
        setupBatteryReceiver()
        startLocationUpdates()
    }

    


    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: UUID.randomUUID().toString()
    }

   fun getDeviceName(): String {
    return Build.MODEL
}
    


    fun getEmployeeName(): String? {
        val prefs = context.getSharedPreferences("kryos_config", Context.MODE_PRIVATE)
        return prefs.getString("employee_name", null)
    }

    


    
    suspend fun getCurrentLocation(): LocationResult = withContext(Dispatchers.IO) {

    val cached = lastKnownLocation

    
    if (cached != null && isLocationFresh(cached)) {
        logLocation(cached, "CACHE")
        return@withContext LocationResult(
            latitude = cached.latitude,
            longitude = cached.longitude,
            accuracy = cached.accuracy
        )
    }

    
    val fresh = requestFreshLocation()

    if (fresh != null) {
        lastKnownLocation = fresh
        logLocation(fresh, "FRESH")
        return@withContext LocationResult(
            latitude = fresh.latitude,
            longitude = fresh.longitude,
            accuracy = fresh.accuracy
        )
    }

    
    if (cached != null) {
        logLocation(cached, "STALE")
        return@withContext LocationResult(
            latitude = cached.latitude,
            longitude = cached.longitude,
            accuracy = cached.accuracy
        )
    }

    Log.w(TAG, "No se pudo obtener ninguna ubicación")
    return@withContext LocationResult(0.0, 0.0, null)
}



    private fun isLocationFresh(location: Location): Boolean {
      return System.currentTimeMillis() - location.time <= MAX_LOCATION_AGE_MS
   }


   private suspend fun requestFreshLocation(): Location? {
    return try {
        withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    continuation.resume(location)
                }.addOnFailureListener { e ->
                    Log.w(TAG, "No se pudo obtener ubicación nueva: ${e.message}")
                    continuation.resume(null)
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error solicitando ubicación nueva", e)
        null
    }
}



private fun logLocation(location: Location, source: String) {
    val age = System.currentTimeMillis() - location.time

    Log.i(
        TAG,
        "$source -> " +
        "lat=${location.latitude}, " +
        "lon=${location.longitude}, " +
        "acc=${location.accuracy}, " +
        "age=${age}ms"
    )
}

    


    fun getBatteryLevel(): Int {
        if (cachedBatteryLevel >= 0) return cachedBatteryLevel

        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            if (level >= 0 && scale > 0) {
                (level * 100 / scale).also { cachedBatteryLevel = it }
            } else {
                -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo batería: ${e.message}")
            -1
        }
    }

    


    fun isCharging(): Boolean {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            cachedIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            cachedIsCharging
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando carga: ${e.message}")
            false
        }
    }

    


    suspend fun getPublicIp(): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(180000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(180000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            val request = okhttp3.Request.Builder()
                .url("https://api.ipify.org?format=text")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.trim()
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo obtener IP pública: ${e.message}")
            null
        }
    }

    


    fun getLocalIp(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue

                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo IP local: ${e.message}")
            null
        }
    }

    


    fun getConnectionType(): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return "unknown"
                val capabilities = cm.getNetworkCapabilities(network) ?: return "unknown"

                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile"
                    else -> "unknown"
                }
            } else {
                @Suppress("DEPRECATION")
                val activeNetwork = cm.activeNetworkInfo
                @Suppress("DEPRECATION")
                when (activeNetwork?.type) {
                    ConnectivityManager.TYPE_WIFI -> "wifi"
                    ConnectivityManager.TYPE_MOBILE -> "mobile"
                    else -> "unknown"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo tipo de conexión: ${e.message}")
            "unknown"
        }
    }

    


    fun getWifiName(): String? {
        return try {
            if (getConnectionType() != "wifi") return null

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            val ssid = info.ssid

            if (ssid != null && ssid != "<unknown ssid>") {
                ssid.replace("\"", "")
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo WiFi: ${e.message}")
            null
        }
    }

    


    fun getDeviceModel(): String {
        return Build.MODEL ?: "Unknown"
    }

    


    fun getManufacturer(): String {
        return Build.MANUFACTURER ?: "Unknown"
    }

    


    fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    


    fun getMexicoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("America/Mexico_City")
        return sdf.format(Date())
    }

    


    fun isInternetAvailable(): Boolean {
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

    


    suspend fun collectAllData(): DeviceReport {
        val location = getCurrentLocation()
        val publicIp = getPublicIp()

        return DeviceReport(
            deviceId = getDeviceId(),
            employeeName = getEmployeeName(),
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            publicIp = publicIp,
            localIp = getLocalIp(),
            connectionType = getConnectionType(),
            wifiName = getWifiName(),
            batteryLevel = getBatteryLevel(),
            isCharging = isCharging(),
            deviceModel = getDeviceModel(),
            manufacturer = getManufacturer(),
            androidVersion = getAndroidVersion(),
            timestamp = getMexicoTimestamp(),
            isOfflineRecovery = false
        )
    }

    


    private fun startLocationUpdates() {
        try {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    result.lastLocation?.let {
                        lastKnownLocation = it
                        logLocation(it, "CALLBACK")
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            Log.i(TAG, "Actualizaciones de ubicación iniciadas")
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando ubicación: ${e.message}")
        }
    }

    


    private fun setupBatteryReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_BATTERY_LOW)
                addAction(Intent.ACTION_BATTERY_OKAY)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }

            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        Intent.ACTION_BATTERY_CHANGED -> {
                            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                            if (level >= 0 && scale > 0) {
                                cachedBatteryLevel = level * 100 / scale
                            }
                            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                            cachedIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                    status == BatteryManager.BATTERY_STATUS_FULL
                        }
                    }
                }
            }, filter)
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando receptor de batería: ${e.message}")
        }
    }

    


    fun stop() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    


    data class LocationResult(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float?
    )
}
