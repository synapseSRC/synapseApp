package com.synapse.social.studioasinc.shared.data

import io.ktor.utils.io.ByteReadChannel

expect class FileUploader {
    fun getFileSize(path: String): Long
    fun getFileName(path: String): String
    suspend fun readFile(path: String, offset: Long = 0L): ByteReadChannel
    fun deleteFile(path: String)
}
