package com.kryos.monitor.config

object ServerConfig {
    const val BASE_URL = "http://kryos001.duckdns.org:8080"
    const val API_REPORT = "$BASE_URL/api/report"
    const val API_DEVICES = "$BASE_URL/api/devices"
    const val API_DEVICE = "$BASE_URL/api/device"
    const val SYNC_INTERVAL_MS = 5000L 
}
