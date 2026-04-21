package com.synapse.social.studioasinc.shared.domain.repository

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import java.io.File

actual class FileUploader {
    actual fun getFileSize(path: String): Long {
        val file = File(path)
        return if (file.exists()) file.length() else 0L
    }

    actual fun getFileName(path: String): String {
        return File(path).name
    }

    actual suspend fun readFile(path: String, offset: Long): ByteReadChannel {
        val file = File(path)
        if (!file.exists()) return io.ktor.utils.io.ByteReadChannel.Empty

        val stream = file.inputStream()
        if (offset > 0) {
            stream.skip(offset)
        }
        return stream.toByteReadChannel()
    }

    actual fun deleteFile(path: String) {
        val file = File(path)
        if (file.exists()) file.delete()
    }
}
