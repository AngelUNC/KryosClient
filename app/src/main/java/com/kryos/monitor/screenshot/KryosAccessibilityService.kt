package com.kryos.monitor.screenshot

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent





class KryosAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "KryosScreenshot"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        ScreenshotManager.setAccessibilityService(this)

        Log.i(
            TAG,
            "AccessibilityService conectado."
        )
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        
    }

    override fun onInterrupt() {
        Log.w(
            TAG,
            "AccessibilityService interrumpido."
        )
    }

    override fun onDestroy() {
        ScreenshotManager.clearAccessibilityService()

        Log.i(
            TAG,
            "AccessibilityService destruido."
        )

        super.onDestroy()
    }
}
