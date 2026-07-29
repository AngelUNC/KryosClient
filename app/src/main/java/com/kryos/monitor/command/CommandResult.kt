package com.kryos.monitor.command

import java.io.File







sealed class CommandResult {

    


    data class Response(
        val response: CommandResponse
    ) : CommandResult()

    


    data class Download(
        val id: Long,
        val file: File
    ) : CommandResult()

}
