package com.kryos.monitor.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import java.io.File

class ApkInstaller(
    private val context: Context
) {

    fun install(apkFile: File) {

        try {

            UpdateLogger.info("===== INSTALLER =====")
            UpdateLogger.info("APK: ${apkFile.absolutePath}")
            UpdateLogger.info("Existe: ${apkFile.exists()}")
            UpdateLogger.info("Tamaño: ${apkFile.length()}")

            val authority = "${context.packageName}.provider"
            UpdateLogger.info("Authority: $authority")

            val apkUri = FileProvider.getUriForFile(
                context,
                authority,
                apkFile
            )

            UpdateLogger.info("URI: $apkUri")

            val intent = Intent(Intent.ACTION_VIEW).apply {

                setDataAndType(
                    apkUri,
                    "application/vnd.android.package-archive"
                )

                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val pm = context.packageManager

            val activity = intent.resolveActivity(pm)

            if (activity == null) {
                UpdateLogger.error("No existe actividad para instalar APK")
                return
            }

            UpdateLogger.info("Resolver: $activity")

            UpdateLogger.info("Lanzando instalador...")

            context.startActivity(intent)

            UpdateLogger.info("startActivity() ejecutado correctamente")

        } catch (e: Exception) {

            UpdateLogger.error("Error iniciando instalación", e)

        }
    }
}
