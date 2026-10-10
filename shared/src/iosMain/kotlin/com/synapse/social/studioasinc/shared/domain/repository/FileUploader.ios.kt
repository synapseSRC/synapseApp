package com.synapse.social.studioasinc.shared.domain.repository

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import io.ktor.utils.io.*
import platform.Foundation.*

actual class FileUploader {
    actual fun getFileSize(path: String): Long {
        val manager = NSFileManager.defaultManager
        if (!manager.fileExistsAtPath(path)) return 0L
        val attrs = manager.attributesOfItemAtPath(path, null) ?: return 0L
        return (attrs[NSFileSize] as? Long) ?: 0L
    }

    actual fun getFileName(path: String): String {
        return (path as NSString).lastPathComponent
    }

    @OptIn(DelicateCoroutinesApi::class)
    actual suspend fun readFile(path: String, offset: Long): ByteReadChannel {
        return coroutineScope { writer(Dispatchers.Default) {
            val inputStream = NSInputStream.inputStreamWithFileAtPath(path)
            if (inputStream == null) {
                channel.close(Exception("Failed to open file at $path"))
                return@writer
            }

            inputStream.open()

            if (offset > 0) {
                inputStream.setProperty(NSNumber(longLong = offset), forKey = NSStreamFileCurrentOffsetKey)
            }

            try {
                val bufferSize = 4096
                val buffer = ByteArray(bufferSize)

                while (isActive) {
                    val read = buffer.usePinned { pinned ->
                        inputStream.read(pinned.addressOf(0).reinterpret<UByteVar>(), bufferSize.toULong())
                    }
                    if (read > 0) {
                        channel.writeFully(buffer, 0, read.toInt())
                    } else if (read == 0L) {
                        break // EOF
                    } else {
                         throw Exception("Read error from stream: $read")
                    }
                }
            } catch (e: Exception) {
                channel.close(e)
            } finally {
                inputStream.close()
            }
        } }.channel
    }

    actual fun deleteFile(path: String) {
        val manager = NSFileManager.defaultManager
        if (manager.fileExistsAtPath(path)) {
            manager.removeItemAtPath(path, null)
        }
    }
}
