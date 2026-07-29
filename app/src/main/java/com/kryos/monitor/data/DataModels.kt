package com.kryos.monitor.data

import com.google.gson.annotations.SerializedName





data class DeviceReport(
    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("employee_name")
    val employeeName: String? = null,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("accuracy")
    val accuracy: Float? = null,

    @SerializedName("public_ip")
    val publicIp: String? = null,

    @SerializedName("local_ip")
    val localIp: String? = null,

    @SerializedName("connection_type")
    val connectionType: String, 

    @SerializedName("wifi_name")
    val wifiName: String? = null,

    @SerializedName("battery_level")
    val batteryLevel: Int,

    @SerializedName("is_charging")
    val isCharging: Boolean,

    @SerializedName("device_model")
    val deviceModel: String? = null,

    @SerializedName("manufacturer")
    val manufacturer: String? = null,

    @SerializedName("android_version")
    val androidVersion: String? = null,

    @SerializedName("timestamp")
    val timestamp: String? = null,

    @SerializedName("is_offline_recovery")
    val isOfflineRecovery: Boolean = false
)




data class ReportResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("employee_name")
    val employeeName: String? = null,

    @SerializedName("is_online")
    val isOnline: Boolean = true,

    @SerializedName("last_seen")
    val lastSeen: String? = null
)




data class DeviceListResponse(
    @SerializedName("devices")
    val devices: List<DeviceInfo>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("online_count")
    val onlineCount: Int,

    @SerializedName("offline_count")
    val offlineCount: Int
)




data class DeviceInfo(
    @SerializedName("id")
    val id: Int,

    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("employee_name")
    val employeeName: String? = null,

    @SerializedName("is_online")
    val isOnline: Boolean = false,

    @SerializedName("last_battery_level")
    val lastBatteryLevel: Int? = null
)




data class OfflineEntry(
    val id: Long = 0,
    val jsonData: String,
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)