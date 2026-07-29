package com.kryos.monitor.notification

data class NotificationReport(
    val deviceId: String,
    val deviceName: String,
    val packageName: String,
    val notificationId: Int,
    val tag: String?,
    val key: String,
    val title: String?,
    val text: String?,
    val postTime: Long,
    val isOngoing: Boolean,
    val isClearable: Boolean,
    val isOfflineRecovery: Boolean = false
)
