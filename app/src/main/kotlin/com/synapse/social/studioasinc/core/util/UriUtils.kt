package com.synapse.social.studioasinc.core.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility class for handling URIs and retrieving file paths.
 */
object UriUtils {

    private const val TAG = "UriUtils"
    private const val BUFFER_SIZE = 8192

    /**
     * Get a file path from a Uri.
     * This will verify if the Uri is a file Uri and return the path, or copy the file to the cache directory
     * and return the path to the cached file.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @return The absolute file path, or null if it could not be resolved.
     */
    fun getPathFromUri(context: Context, uri: Uri?): String? {
        uri ?: return null

        if (ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }

        return copyToCache(context, uri)
    }

    /**
     * Get the file name from a Uri.
     *
     * @param context The context.
     * @param uri The Uri.
     * @return The file name, or null if it could not be determined.
     */
    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            result = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get file name for URI: $uri", e)
            }
        }
        if (result == null) {
            result = uri.path
            result?.let {
                val cut = it.lastIndexOf('/')
                if (cut != -1) {
                    result = it.substring(cut + 1)
                }
            }
        }
        return result
    }

    private fun copyToCache(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileName = getFileName(context, uri) ?: "temp_file"

                val ext = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ".tmp"
                val name = if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName

                val safeName = name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50)

                val cacheFile = File.createTempFile("${System.currentTimeMillis()}_$safeName", ext, context.cacheDir)

                FileOutputStream(cacheFile).use { outputStream ->
                    inputStream.copyTo(outputStream, BUFFER_SIZE)
                }
                cacheFile.absolutePath
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy URI to cache: $uri", e)
            null
        }
    }
}
