package com.synapse.social.studioasinc.core.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object DownloadManager {
    private const val TAG = "DownloadManager"
    private const val BUFFER_SIZE = 8192
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    interface DownloadCallback {
        fun onSuccess(savedUri: Uri, fileName: String)
        fun onProgress(progress: Int)
        fun onError(error: String)
    }

    fun downloadFile(
        context: Context?,
        fileUrl: String?,
        fileName: String?,
        callback: DownloadCallback?
    ) {
        if (context == null || fileUrl.isNullOrEmpty()) {
            callback?.onError("Invalid parameters")
            return
        }
        val applicationContext = context.applicationContext
        scope.launch {
            try {
                val fileInfo = FileUtils.detectFileInfoFromUrl(
                    fileUrl,
                    fileName ?: "file",
                    "",
                    "application/octet-stream",
                    "",
                    emptyMap()
                )
                val finalFileName = fileInfo.fileName
                val mimeType = fileInfo.mimeType

                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, finalFileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Synapse")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    } else {
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
                        @Suppress("DEPRECATION")
                        put(MediaStore.Downloads.DATA, "$downloadsDir/Synapse/$finalFileName")
                    }
                }

                val resolver = applicationContext.contentResolver
                val downloadUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (downloadUri == null) {
                    callback?.onError("Failed to create download entry")
                    return@launch
                }

                try {
                    resolver.openOutputStream(downloadUri)?.use { outputStream ->
                        downloadToStream(fileUrl, outputStream, callback)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                            resolver.update(downloadUri, contentValues, null, null)
                        }
                        callback?.onSuccess(downloadUri, fileName ?: "file")
                    } ?: throw IOException("Failed to open output stream")
                } catch (e: Exception) {
                    resolver.delete(downloadUri, null, null)
                    throw e
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading file: " + e.message, e)
                callback?.onError("Download failed: " + e.message)
            }
        }
    }

    @Throws(IOException::class)
    private suspend fun downloadToStream(urlString: String, outputStream: OutputStream, callback: DownloadCallback?) = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server response: " + connection.responseCode + " " + connection.responseMessage)
            }
            val fileLength = connection.contentLength
            connection.inputStream.use { inputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                var totalBytesRead = 0
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (callback != null && fileLength > 0) {
                        val progress = (totalBytesRead * 100L / fileLength).toInt()
                        callback.onProgress(progress)
                    }
                }
                outputStream.flush()
            }
        } finally {
            connection.disconnect()
        }
    }

    fun shutdown() {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}
