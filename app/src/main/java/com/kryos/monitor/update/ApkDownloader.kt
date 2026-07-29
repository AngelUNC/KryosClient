package com.kryos.monitor.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ApkDownloader(private val context: Context) {

    suspend fun download(
        url: String,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {

        try {

            UpdateLogger.info("===== DOWNLOAD INICIADO =====")
            UpdateLogger.info("URL: $url")

            UpdateLogger.info("Abriendo conexión...")
            val connection = URL(url).openConnection() as HttpURLConnection

            connection.connectTimeout = 15000
            connection.readTimeout = 30000

            connection.connect()

            UpdateLogger.info("Conectado.")
            UpdateLogger.info("HTTP=${connection.responseCode}")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                UpdateLogger.error("HTTP inválido: ${connection.responseCode}")
                return@withContext Result.failure(
                    Exception("HTTP ${connection.responseCode}")
                )
            }

            val totalSize = connection.contentLengthLong
            UpdateLogger.info("Tamaño APK: $totalSize bytes")

            val apkFile = File(context.cacheDir, "update.apk")
            UpdateLogger.info("Archivo destino: ${apkFile.absolutePath}")

            var lastProgress = -1

            connection.inputStream.use { input ->

                UpdateLogger.info("InputStream abierto")

                FileOutputStream(apkFile).use { output ->

                    UpdateLogger.info("OutputStream abierto")

                    val buffer = ByteArray(8192)

                    var downloaded = 0L
                    var bytesRead: Int

                    UpdateLogger.info("Entrando al while...")

                    while (true) {

                        bytesRead = input.read(buffer)

                        if (bytesRead == -1) {
                            UpdateLogger.info("EOF recibido (-1)")
                            break
                        }

                        output.write(buffer, 0, bytesRead)

                        downloaded += bytesRead

                        if (totalSize > 0) {

                            val progress =
                                ((downloaded * 100) / totalSize).toInt()

                            if (progress != lastProgress) {
                                lastProgress = progress
                                UpdateLogger.info(
                                    "Progreso=$progress% ($downloaded/$totalSize)"
                                )
                                onProgress(progress)
                            }
                        }
                    }

                    UpdateLogger.info("Fin del while")

                    output.flush()
                    UpdateLogger.info("flush OK")
                }

                UpdateLogger.info("OutputStream cerrado")
            }

            UpdateLogger.info("InputStream cerrado")

            connection.disconnect()
            UpdateLogger.info("disconnect OK")

            UpdateLogger.info("Existe APK=${apkFile.exists()}")
            UpdateLogger.info("Tamaño archivo=${apkFile.length()}")

            UpdateLogger.info("APK guardada en:")
            UpdateLogger.info(apkFile.absolutePath)

            UpdateLogger.info("===== DOWNLOAD FINALIZADO =====")

            Result.success(apkFile)

        } catch (e: Exception) {

            UpdateLogger.error("Excepción en download()", e)

            Result.failure(e)
        }
    }
}
