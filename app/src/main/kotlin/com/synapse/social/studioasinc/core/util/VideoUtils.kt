package com.synapse.social.studioasinc.core.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException

object VideoUtils {

    private const val TAG = "VideoUtils"
    private const val BUFFER_SIZE = 8192

    fun saveVideoToGallery(context: Context, videoFile: File, fileName: String, subFolder: String?): Result<Uri> {
        val mimeType = "video/mp4"
        val finalFileName = if (fileName.endsWith(".mp4")) fileName else "$fileName.mp4"

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, finalFileName)
            put(MediaStore.Video.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var relativePath = Environment.DIRECTORY_MOVIES
                if (!subFolder.isNullOrEmpty()) {
                    relativePath += File.separator + subFolder
                }
                put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val itemUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return Result.failure(IOException("Failed to create new MediaStore entry for video."))

        return try {
            resolver.openOutputStream(itemUri)?.use { os ->
                FileInputStream(videoFile).use { inputStream ->
                    inputStream.copyTo(os, BUFFER_SIZE)
                }
            } ?: throw IOException("Failed to get output stream.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                resolver.update(itemUri, updateValues, null, null)
            }
            Result.success(itemUri)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save video.", e)
            resolver.delete(itemUri, null, null)
            Result.failure(e)
        }
    }
}
