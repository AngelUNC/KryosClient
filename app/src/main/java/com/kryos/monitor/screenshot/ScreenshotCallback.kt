package com.kryos.monitor.screenshot

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit




internal class ScreenshotCallback(
    private val cacheDir: File
) : AccessibilityService.TakeScreenshotCallback {

    companion object {
        private const val TAG = "KryosScreenshot"
    }

    @Volatile
    var result: File? = null
        private set

    @Volatile
    var error: String? = null
        private set

    private val latch = CountDownLatch(1)

    override fun onSuccess(
        result: AccessibilityService.ScreenshotResult
    ) {

        val hardwareBuffer = result.hardwareBuffer

        try {

            val bitmap = Bitmap.wrapHardwareBuffer(
                hardwareBuffer,
                result.colorSpace
            )

            if (bitmap == null) {

                error = "Failed to create Bitmap from HardwareBuffer"

                Log.e(
                    TAG,
                    error!!
                )

                return

            }

            val screenshotFile = File(
                cacheDir,
                generateFileName()
            )

            saveScreenshot(
                bitmap,
                screenshotFile
            )

            this.result = screenshotFile

            Log.i(
                TAG,
                "Screenshot guardado: ${screenshotFile.absolutePath}"
            )

        } catch (e: Exception) {

            error = e.message ?: "Unknown error during screenshot processing"

            Log.e(
                TAG,
                error,
                e
            )

        } finally {

            hardwareBuffer.close()

            latch.countDown()

        }

    }

    override fun onFailure(
        errorCode: Int
    ) {

        error = mapErrorCode(
            errorCode
        )

        Log.e(
            TAG,
            error!!
        )

        latch.countDown()

    }

    


    fun waitForResult(
        timeoutMs: Long = 5000L
    ): File? {

        return try {

            if (
                !latch.await(
                    timeoutMs,
                    TimeUnit.MILLISECONDS
                )
            ) {

                error = "Screenshot timeout"

                Log.e(
                    TAG,
                    error!!
                )

                null

            } else {

                result

            }

        } catch (e: InterruptedException) {

            error = "Interrupted while waiting for screenshot"

            Log.e(
                TAG,
                error,
                e
            )

            null

        }

    }

    


    private fun saveScreenshot(
        bitmap: Bitmap,
        file: File
    ) {

        FileOutputStream(file).use { fos ->

            if (
                !bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    fos
                )
            ) {

                throw IllegalStateException(
                    "Failed to compress bitmap."
                )

            }

            fos.flush()

        }

        if (!file.exists()) {

            throw IllegalStateException(
                "Screenshot file was not created."
            )

        }

    }

    


    private fun generateFileName(): String {

        val timestamp =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
            ).format(
                Date()
            )

        return "screenshot_${timestamp}.png"

    }

    


    private fun mapErrorCode(
        errorCode: Int
    ): String =
        when (errorCode) {

            AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR ->
                "Internal system error."

            AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT ->
                "Screenshots requested too quickly."

            AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY ->
                "Invalid display."

            AccessibilityService.ERROR_TAKE_SCREENSHOT_INVALID_WINDOW ->
                "Invalid window."

            AccessibilityService.ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS ->
                "Accessibility permission missing."

            AccessibilityService.ERROR_TAKE_SCREENSHOT_SECURE_WINDOW ->
                "Cannot capture a secure window."

            else ->
                "Unknown error ($errorCode)."

        }

}
