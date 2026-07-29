package com.kryos.monitor.screenshot

import android.os.Build
import java.util.concurrent.Executor
import android.util.Log
import android.view.Display
import java.io.File
import java.util.concurrent.Executors




object ScreenshotManager {

    private const val TAG = "KryosScreenshot"

    @Volatile
    private var accessibilityService: KryosAccessibilityService? = null

    private val executorService =
        Executors.newSingleThreadExecutor()

    private val executor = Executor { runnable ->
        executorService.execute(runnable)
    }

    internal fun setAccessibilityService(
        service: KryosAccessibilityService
    ) {

        accessibilityService = service

        Log.i(
            TAG,
            "AccessibilityService registrado."
        )

    }

    internal fun clearAccessibilityService() {

        accessibilityService = null

        Log.i(
            TAG,
            "AccessibilityService liberado."
        )

    }

    


    fun captureScreenshot(): File? {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {

            Log.e(
                TAG,
                "Android 11+ requerido."
            )

            return null

        }

        val service = accessibilityService

        if (service == null) {

            Log.e(
                TAG,
                "AccessibilityService no disponible."
            )

            return null

        }

        val callback = ScreenshotCallback(
            service.cacheDir
        )

        try {

            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                executor,
                callback
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "takeScreenshot() lanzó una excepción.",
                e
            )

            return null

        }

        val result = callback.waitForResult(
            5000L
        )

        if (result == null) {

            Log.e(
                TAG,
                callback.error ?: "Error desconocido."
            )

            return null

        }

        if (!result.exists()) {

            Log.e(
                TAG,
                "El archivo de captura no existe."
            )

            return null

        }

        Log.i(
            TAG,
            "Captura completada: ${result.absolutePath}"
        )

        return result

    }

    fun isServiceAvailable(): Boolean =
        accessibilityService != null

}
