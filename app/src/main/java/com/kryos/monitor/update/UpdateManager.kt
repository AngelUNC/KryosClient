package com.kryos.monitor.update

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpdateManager(private val context: Context) {

    private val downloader = ApkDownloader(context)
    private val installer = ApkInstaller(context)
    private val session = UpdateSession(context)

    companion object {
        private const val TAG = "UpdateManager"
    }

    private var state: UpdateState = UpdateState.IDLE

    fun getState(): UpdateState = state

    private fun setState(newState: UpdateState) {
        UpdateLogger.info("setState() -> $newState")
        state = newState
        UpdateLogger.info("[$TAG] Estado actual = $state")
    }

    fun startUpdate(url: String) {

        UpdateLogger.info("========================================")
        UpdateLogger.info("startUpdate() llamado")
        UpdateLogger.info("Estado actual = $state")
        UpdateLogger.info("URL = $url")
        UpdateLogger.info("========================================")

        if (state != UpdateState.IDLE &&
            state != UpdateState.FAILED &&
            state != UpdateState.COMPLETED) {

            UpdateLogger.warning("Ya existe una actualización en progreso.")
            return
        }

        UpdateLogger.info("Guardando URL...")
        session.saveUrl(url)

        UpdateLogger.info("Cambiando estado a DOWNLOADING")
        setState(UpdateState.DOWNLOADING)
        session.saveState(UpdateState.DOWNLOADING)

        CoroutineScope(Dispatchers.Main).launch {

            try {

                UpdateLogger.info("Invocando downloader.download()")

                val result = downloader.download(url) { progress ->

                    session.saveProgress(progress)
                    UpdateLogger.info("Progress callback -> $progress%")
                }

                UpdateLogger.info("download() REGRESÓ")

                if (result.isSuccess) {
                    UpdateLogger.info("Result = SUCCESS")
                } else {
                    UpdateLogger.info("Result = FAILURE")
                }

                result.onSuccess { apk ->

                    UpdateLogger.info("Entró a onSuccess()")

                    UpdateLogger.info("Ruta APK:")
                    UpdateLogger.info(apk.absolutePath)

                    UpdateLogger.info("Existe=${apk.exists()}")
                    UpdateLogger.info("Tamaño=${apk.length()}")

                    UpdateLogger.info("Cambiando estado a INSTALLING")
                    setState(UpdateState.INSTALLING)
                    session.saveState(UpdateState.INSTALLING)

                    UpdateLogger.info("Llamando installer.install()")

                    installer.install(apk)

                    UpdateLogger.info("installer.install() terminó")
                }

                result.onFailure {

                    UpdateLogger.error(
                        "onFailure(): ${it.message ?: "sin mensaje"}",
                        it
                    )

                    updateFailed(
                        it.message ?: "Error desconocido"
                    )
                }

                UpdateLogger.info("Fin de launch()")

            } catch (e: Exception) {

                UpdateLogger.error(
                    "Excepción dentro de startUpdate()",
                    e
                )

                updateFailed(
                    e.message ?: "Excepción inesperada"
                )
            }
        }
    }

    fun updateCompleted() {

        UpdateLogger.info("updateCompleted()")

        setState(UpdateState.COMPLETED)
    }

    fun updateFailed(reason: String) {

        UpdateLogger.error("updateFailed(): $reason")

        setState(UpdateState.FAILED)
    }

    fun reset() {

        UpdateLogger.info("reset()")

        setState(UpdateState.IDLE)
    }
}
