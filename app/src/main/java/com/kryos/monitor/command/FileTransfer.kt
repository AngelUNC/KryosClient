package com.kryos.monitor.command

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.net.Socket



















object FileTransfer {

    private const val TAG = "FileTransfer"

    private const val CONNECT_TIMEOUT = 10000
    private const val BUFFER_SIZE = 8192

    private val gson = Gson()

    private data class TransferHeader(
        @SerializedName("requestId") val requestId: Long,
        @SerializedName("name") val name: String,
        @SerializedName("size") val size: Long
    )

    







    fun send(serverIp: String, port: Int, requestId: Long, file: File) {

        if (!file.exists() || !file.isFile || !file.canRead()) {
            Log.e(TAG, "Archivo inválido o no legible: ${file.absolutePath}")
            return
        }

        var socket: Socket? = null

        try {

            socket = Socket()
            socket.connect(InetSocketAddress(serverIp, port), CONNECT_TIMEOUT)

            val output = BufferedOutputStream(socket.getOutputStream())

            val header = TransferHeader(
                requestId = requestId,
                name = file.name,
                size = file.length()
            )

            val headerLine = gson.toJson(header) + "\n"
            output.write(headerLine.toByteArray(Charsets.UTF_8))
            output.flush()

            var sent = 0L
            val buffer = ByteArray(BUFFER_SIZE)

            FileInputStream(file).use { input ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    sent += read
                }
            }

            output.flush()

            Log.i(TAG, "Archivo enviado: ${file.name} ($sent bytes)")

        } catch (e: Exception) {

            Log.e(TAG, "Error enviando archivo '${file.name}': ${e.message}", e)

        } finally {

            try {
                socket?.close()
            } catch (_: Exception) {
            }

        }

    }

}
