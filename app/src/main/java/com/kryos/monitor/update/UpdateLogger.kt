package com.kryos.monitor.update

import android.util.Log

object UpdateLogger {
    
    private const val TAG = "KryosUpdate"

    fun info(message: String) {
        Log.i(TAG, message)
    }

    fun warning(message: String) {
        Log.w(TAG, message)
    }

    fun error(message: String) {
        Log.e(TAG, message)
    }

    fun error(message: String, throwable: Throwable) {
        Log.e(TAG, message, throwable)
    }
}
