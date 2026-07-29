package com.kryos.monitor.command

import android.os.Environment
import java.io.File







class CommandSession {

    










    private val rootDirectory: File =
        Environment.getExternalStorageDirectory().canonicalFile

    


    private var currentDirectory: File = rootDirectory

    


    fun getCurrentDirectory(): File {
        return currentDirectory
    }

    


    fun pwd(): String {
        return currentDirectory.absolutePath
    }

    


    fun goRoot(): Boolean {
        currentDirectory = rootDirectory
        return true
    }

    





    fun goBack(): Boolean {

        val parent = currentDirectory.parentFile ?: return false

        if (!parent.canonicalPath.startsWith(rootDirectory.canonicalPath))
            return false

        currentDirectory = parent.canonicalFile

        return true
    }

    


    fun changeDirectory(name: String): Boolean {

        if (name == ".")
            return true

        if (name == "..")
            return goBack()

        if (name == "/")
            return goRoot()

        val destination = File(currentDirectory, name)

        if (!destination.exists())
            return false

        if (!destination.isDirectory)
            return false

        val canonical = destination.canonicalFile

        if (!canonical.path.startsWith(rootDirectory.canonicalPath))
            return false

        currentDirectory = canonical

        return true
    }

    


    fun resolve(relative: String): File {

        return File(currentDirectory, relative).canonicalFile

    }

}
