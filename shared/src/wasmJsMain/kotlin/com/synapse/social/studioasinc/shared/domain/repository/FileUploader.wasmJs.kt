package com.synapse.social.studioasinc.shared.domain.repository

import io.ktor.utils.io.ByteReadChannel

actual class FileUploader {
    actual fun getFileSize(path: String): Long = 0L
    actual fun getFileName(path: String): String = "dummy"
    actual suspend fun readFile(path: String, offset: Long): ByteReadChannel = io.ktor.utils.io.ByteReadChannel.Empty
    actual fun deleteFile(path: String) {}
}
