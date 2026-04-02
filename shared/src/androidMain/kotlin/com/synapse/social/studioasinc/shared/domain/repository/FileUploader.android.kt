package com.synapse.social.studioasinc.shared.domain.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

actual class FileUploader(private val context: Context) {

    actual fun getFileSize(path: String): Long {
        if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (index != -1) {
                            return cursor.getLong(index)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore and fall through
            }
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    return pfd.statSize
                }
            } catch (e: Exception) {
                // Ignore
            }
            return 0L
        } else {
            val file = File(path)
            if (file.exists()) return file.length()
            return 0L
        }
    }

    actual fun getFileName(path: String): String {
        if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            var result: String? = null
            if (uri.scheme == "content") {
                 try {
                     context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                         if (cursor.moveToFirst()) {
                             val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                             if (index != -1) {
                                 result = cursor.getString(index)
                             }
                         }
                     }
                 } catch (e: Exception) {

                 }
            }
            if (result == null) {
                result = uri.path
                val cut = result?.lastIndexOf('/')
                if (cut != null && cut != -1) {
                    result = result?.substring(cut + 1)
                }
            }
            return result ?: "upload_file"
        } else {
            return File(path).name
        }
    }

    actual suspend fun readFile(path: String, offset: Long): ByteReadChannel {
        return if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw Exception("Failed to open file descriptor for $path")

            if (offset > 0) {
                android.system.Os.lseek(pfd.fileDescriptor, offset, android.system.OsConstants.SEEK_SET)
            }

            FileInputStream(pfd.fileDescriptor).toByteReadChannel(context = Dispatchers.IO)
        } else {
            val file = File(path)
            val fis = FileInputStream(file)
            if (offset > 0) {
                fis.channel.position(offset)
            }
            fis.toByteReadChannel(context = Dispatchers.IO)
        }
    }

    actual fun deleteFile(path: String) {
        if (!path.startsWith("content://")) {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
