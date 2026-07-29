package com.kryos.monitor.notification




data class NotificationEntity(

    
    val id: Long = 0L,

    
    val deviceId: String,
    val deviceName: String,

    
    val packageName: String,

    
    val notificationId: Int,
    val tag: String?,
    val key: String,

    
    val title: String?,
    val text: String?,

    
    val postTime: Long,

    
    val receivedAt: Long = System.currentTimeMillis(),

    
    val isOngoing: Boolean,
    val isClearable: Boolean,

    
    val synced: Boolean = false
)
