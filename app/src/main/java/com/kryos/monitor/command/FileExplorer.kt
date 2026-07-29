package com.kryos.monitor.command

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale










class FileExplorer(
    private val session: CommandSession
) {

    


    fun list(): List<FileEntry> {

        val current = session.getCurrentDirectory()

        val files = current.listFiles() ?: return emptyList()

        return files
            .sortedWith(
                compareBy<File>({ !it.isDirectory }, { it.name.lowercase() })
            )
            .map {

                FileEntry(
                    name = it.name,
                    type = if (it.isDirectory) "dir" else "file",
                    size = if (it.isDirectory) 0 else it.length()
                )

            }

    }

    


    fun stat(name: String): FileInformation? {

        val file = session.resolve(name)

        if (!file.exists())
            return null

        return FileInformation(
            name = file.name,
            path = file.absolutePath,
            size = if (file.isDirectory) 0 else file.length(),
            lastModified = file.lastModified(),
            type = if (file.isDirectory) "dir" else "file"
        )

    }

    


    fun getFile(name: String): File? {

        val file = session.resolve(name)

        if (!file.exists())
            return null

        if (!file.isFile)
            return null

        if (!file.canRead())
            return null

        return file

    }

    


    fun exists(name: String): Boolean {

        return session.resolve(name).exists()

    }

    


    fun isDirectory(name: String): Boolean {

        return session.resolve(name).isDirectory

    }

    


    fun size(name: String): Long {

        val file = session.resolve(name)

        if (!file.exists())
            return -1

        return file.length()

    }

    


    fun lastModified(name: String): String {

        val file = session.resolve(name)

        if (!file.exists())
            return ""

        val sdf = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        )

        return sdf.format(Date(file.lastModified()))

    }

}
