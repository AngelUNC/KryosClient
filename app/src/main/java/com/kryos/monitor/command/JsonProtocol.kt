package com.kryos.monitor.command

import com.google.gson.annotations.SerializedName
















data class CommandRequest(

    @SerializedName("id")
    val id: Long,

    @SerializedName("cmd")
    val cmd: String,

    @SerializedName("path")
    val path: String? = null,
    
    @SerializedName("url")
    val url: String? = null,

    @SerializedName("file")
    val file: String? = null

)








data class CommandResponse(

    @SerializedName("id")
    val id: Long,

    @SerializedName("status")
    val status: String,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("cwd")
    val cwd: String? = null,

    @SerializedName("items")
    val items: List<FileEntry>? = null,

    @SerializedName("file")
    val file: FileInformation? = null

)








data class FileEntry(

    @SerializedName("name")
    val name: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("size")
    val size: Long

)








data class FileInformation(

    @SerializedName("name")
    val name: String,

    @SerializedName("path")
    val path: String,

    @SerializedName("size")
    val size: Long,

    @SerializedName("lastModified")
    val lastModified: Long,

    @SerializedName("type")
    val type: String

)



