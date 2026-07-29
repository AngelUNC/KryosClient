package com.kryos.monitor.command

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException











import android.content.Context

class CommandClient(
    private val context: Context
) {
    companion object {
        
        private const val TAG = "CommandClient"
        private const val SERVER_IP = "kryos001.duckdns.org"
        private const val SERVER_PORT = 5000
        private const val CONNECT_TIMEOUT = 10000
        private const val RECONNECT_DELAY = 5000L
        private const val SOCKET_READ_TIMEOUT = 15000
        private const val HEARTBEAT_INTERVAL = 10000L
        private const val MAX_MISSED_PONGS = 3
    }

    private val gson = Gson()
    private val processor = CommandProcessor(context)

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var heartbeatJob: Job? = null

    @Volatile
    private var running = false

    @Volatile
    private var missedPongs = 0

    


    fun start() {
        if (running) return
        running = true

        scope.launch {
            while (running) {
                try {
                    connect()
                    startHeartbeat()
                    listen()
                } catch (e: Exception) {
                    Log.e(TAG, "Conexión perdida: ${e.message}", e)
                }
                stopHeartbeat()
                disconnect()
                if (running) delay(RECONNECT_DELAY)
            }
        }
    }

    


    fun stop() {
        running = false
        stopHeartbeat()
        disconnect()
        scope.cancel()
    }

    


    @Throws(Exception::class)
    private fun connect() {
        Log.i(TAG, "Conectando al servidor...")

        socket = Socket()
        socket!!.connect(
            InetSocketAddress(SERVER_IP, SERVER_PORT),
            CONNECT_TIMEOUT
        )

        socket!!.keepAlive = true
        socket!!.soTimeout = SOCKET_READ_TIMEOUT

        reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
        writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
        val hello = gson.toJson(DeviceInfo.hello(context))
	writer?.write(hello)
	writer?.newLine()
	writer?.flush()
        missedPongs = 0
        Log.i(TAG, "Conectado")
    }

    




    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (running && socket?.isConnected == true) {
                delay(HEARTBEAT_INTERVAL)

                if (missedPongs >= MAX_MISSED_PONGS) {
                    Log.w(TAG, "Sin respuesta tras $MAX_MISSED_PONGS pings, cerrando conexión")
                    socket?.close()
                    break
                }

                try {
                    writer?.write("{\"cmd\":\"ping\"}")
                    writer?.newLine()
                    writer?.flush()
                    missedPongs++
                    Log.d(TAG, "Ping enviado (missedPongs=$missedPongs)")
                } catch (e: Exception) {
                    Log.e(TAG, "Error enviando ping: ${e.message}")
                    socket?.close()
                    break
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    


    @Throws(Exception::class)
    private fun listen() {
        while (running && socket != null && socket!!.isConnected) {

            val line = try {
                reader?.readLine()
            } catch (e: SocketTimeoutException) {
                
                
                
                if (missedPongs >= MAX_MISSED_PONGS) {
                    Log.w(TAG, "Timeout de lectura + sin pongs, cerrando")
                    break
                }
                continue
            }

            if (line == null) break

            Log.d(TAG, "RX -> $line")

            
            val maybePong = try {
                gson.fromJson(line, Map::class.java)
            } catch (e: Exception) {
                null
            }

            if (maybePong?.get("cmd") == "pong") {
                missedPongs = 0
                Log.d(TAG, "Pong recibido, conexión sana")
                continue
            }

            val request = try {
                gson.fromJson(line, CommandRequest::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "JSON inválido recibido: $line")
                continue
            }

            when (val result = processor.execute(request)) {

                is CommandResult.Response -> {
                    val json = gson.toJson(result.response)
                    writer?.write(json)
                    writer?.newLine()
                    writer?.flush()
                    Log.d(TAG, "TX -> $json")
                }

                is CommandResult.Download -> {
                    Log.i(TAG, "Iniciando transferencia: ${result.file.absolutePath}")
                    FileTransfer.send(
                        serverIp = SERVER_IP,
                        port = 5001,
                        requestId = result.id,
                        file = result.file
                    )
                    Log.i(TAG, "Transferencia finalizada")
                }
            }
        }
    }

    


    fun sendJson(response: CommandResponse) {
        try {
            val json = gson.toJson(response)
            writer?.write(json)
            writer?.newLine()
            writer?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando JSON", e)
        }
    }

    


    private fun disconnect() {
        try { reader?.close() } catch (_: Exception) {}
        try { writer?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}

        reader = null
        writer = null
        socket = null
    }
}
