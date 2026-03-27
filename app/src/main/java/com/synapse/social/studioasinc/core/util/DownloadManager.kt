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

    private val imageExtensions = mapOf(
        ".png" to "image/png",
        ".gif" to "image/gif",
        ".webp" to "image/webp",
        ".bmp" to "image/bmp",
        ".jpg" to "image/jpeg",
        ".jpeg" to "image/jpeg"
    )

    private val videoExtensions = mapOf(
        ".mov" to "video/quicktime",
        ".avi" to "video/x-msvideo",
        ".mkv" to "video/x-matroska",
        ".webm" to "video/webm",
        ".mp4" to "video/mp4"
    )

    fun downloadImage(context: Context?, imageUrl: String?, fileName: String?, callback: DownloadCallback?) {
        if (context == null || imageUrl.isNullOrEmpty()) {
            callback?.onError("Invalid parameters")
            return
        }
        val applicationContext = context.applicationContext
        scope.launch {
            try {
                val fileInfo = FileUtils.detectFileInfoFromUrl(
                    imageUrl,
                    fileName ?: "image",
                    ".jpg",
                    "image/jpeg",
                    "image/",
                    imageExtensions
                )
                val finalFileName = fileInfo.fileName
                val mimeType = fileInfo.mimeType

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, finalFileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Synapse")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    } else {
                        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                        @Suppress("DEPRECATION")
                        put(MediaStore.Images.Media.DATA, "$picturesDir/Synapse/$fileName")
                    }
                }

                val resolver = applicationContext.contentResolver
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri == null) {
                    callback?.onError("Failed to create media entry")
                    return@launch
                }

                try {
                    resolver.openOutputStream(imageUri)?.use { outputStream ->
                        downloadToStream(imageUrl, outputStream, callback)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(imageUri, contentValues, null, null)
                        }
                        callback?.onSuccess(imageUri, fileName ?: "image")
                    } ?: throw IOException("Failed to open output stream")
                } catch (e: Exception) {
                    resolver.delete(imageUri, null, null)
                    throw e
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading image: " + e.message, e)
                callback?.onError("Download failed: " + e.message)
            }
        }
    }

    fun downloadVideo(context: Context?, videoUrl: String?, fileName: String?, callback: DownloadCallback?) {
        if (context == null || videoUrl.isNullOrEmpty()) {
            callback?.onError("Invalid parameters")
            return
        }
        val applicationContext = context.applicationContext
        scope.launch {
            try {
                val fileInfo = FileUtils.detectFileInfoFromUrl(
                    videoUrl,
                    fileName ?: "video",
                    ".mp4",
                    "video/mp4",
                    "video/",
                    videoExtensions
                )
                val finalFileName = fileInfo.fileName
                val mimeType = fileInfo.mimeType

                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, finalFileName)
                    put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Synapse")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    } else {
                        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()
                        @Suppress("DEPRECATION")
                        put(MediaStore.Video.Media.DATA, "$moviesDir/Synapse/$fileName")
                    }
                }

                val resolver = applicationContext.contentResolver
                val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (videoUri == null) {
                    callback?.onError("Failed to create media entry")
                    return@launch
                }

                try {
                    resolver.openOutputStream(videoUri)?.use { outputStream ->
                        downloadToStream(videoUrl, outputStream, callback)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                            resolver.update(videoUri, contentValues, null, null)
                        }
                        callback?.onSuccess(videoUri, fileName ?: "video")
                    } ?: throw IOException("Failed to open output stream")
                } catch (e: Exception) {
                    resolver.delete(videoUri, null, null)
                    throw e
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading video: " + e.message, e)
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
