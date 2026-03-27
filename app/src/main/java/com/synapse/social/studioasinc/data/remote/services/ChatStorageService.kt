package com.synapse.social.studioasinc.data.remote.services

import android.content.Context
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.UUID

class ChatStorageService {

    companion object {
        internal const val TAG = "ChatStorageService"
        internal const val CHAT_MEDIA_BUCKET = "chat-media"
        internal const val MAX_RETRY_ATTEMPTS = 3
        internal const val BASE_RETRY_DELAY_MS = 1000L
        internal const val PROGRESS_UPDATE_INTERVAL_MS = 100L
    }

    private val client = SupabaseClient.client
    private val storage = client.storage

    suspend fun uploadFile(
        file: File,
        path: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            if (!file.exists()) {
                return@withContext Result.failure(StorageException.FileNotFound("File not found"))
            }
            if (file.length() == 0L) {
                return@withContext Result.failure(StorageException.InvalidFile("File is empty"))
            }

            retryWithExponentialBackoff(
                maxAttempts = MAX_RETRY_ATTEMPTS,
                operation = "uploadFile",
                block = {
                    uploadFileInternal(file, path, onProgress)
                }
            )
        }
    }

    suspend fun uploadFileBytes(
        bytes: ByteArray,
        path: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            retryWithExponentialBackoff(
                maxAttempts = MAX_RETRY_ATTEMPTS,
                operation = "uploadFileBytes",
                block = {
                    uploadFileBytesInternal(bytes, path, onProgress)
                }
            )
        }
    }

    private suspend fun uploadFileInternal(
        file: File,
        path: String,
        onProgress: (Float) -> Unit
    ): Result<String> {
        try {
            android.util.Log.d(TAG, "Uploading file to chat-media (${file.length()} bytes)")
            onProgress(0.0f)
            storage.from(CHAT_MEDIA_BUCKET).upload(path, file) { upsert = true }
            val publicUrl = storage.from(CHAT_MEDIA_BUCKET).publicUrl(path)
            onProgress(1.0f)
            android.util.Log.d(TAG, "Upload successful")
            return Result.success(publicUrl)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Upload failed", e)
            return Result.failure(mapStorageException(e, "Upload failed"))
        }
    }

    private suspend fun uploadFileBytesInternal(
        fileBytes: ByteArray,
        path: String,
        onProgress: (Float) -> Unit
    ): Result<String> {
        try {
            android.util.Log.d(TAG, "Uploading bytes to chat-media (${fileBytes.size} bytes)")
            if (fileBytes.isEmpty()) {
                return Result.failure(StorageException.InvalidFile("File bytes are empty"))
            }
            onProgress(0.1f)
            storage.from(CHAT_MEDIA_BUCKET).upload(path, fileBytes) { upsert = true }
            onProgress(0.9f)
            val publicUrl = storage.from(CHAT_MEDIA_BUCKET).publicUrl(path)
            onProgress(1.0f)
            android.util.Log.d(TAG, "Upload successful")
            return Result.success(publicUrl)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Upload failed", e)
            return Result.failure(mapStorageException(e, "Upload failed"))
        }
    }

    suspend fun downloadFile(url: String): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            retryWithExponentialBackoff(
                maxAttempts = MAX_RETRY_ATTEMPTS,
                operation = "downloadFile",
                block = {
                    downloadFileInternal(url)
                }
            )
        }
    }

    private suspend fun downloadFileInternal(url: String): Result<ByteArray> {
        try {
            android.util.Log.d(TAG, "Downloading file...")
            if (url.isBlank()) {
                return Result.failure(StorageException.InvalidUrl("URL cannot be empty"))
            }
            val path = extractPathFromUrl(url, CHAT_MEDIA_BUCKET)
                ?: return Result.failure(StorageException.InvalidUrl("Invalid URL format"))
            val fileBytes = storage.from(CHAT_MEDIA_BUCKET).downloadAuthenticated(path)
            if (fileBytes.isEmpty()) {
                return Result.failure(StorageException.EmptyFile("Downloaded file is empty"))
            }
            android.util.Log.d(TAG, "Download successful: ${fileBytes.size} bytes")
            return Result.success(fileBytes)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Download failed", e)
            return Result.failure(mapStorageException(e, "Download failed"))
        }
    }

    suspend fun deleteFile(path: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            retryWithExponentialBackoff(
                maxAttempts = MAX_RETRY_ATTEMPTS,
                operation = "deleteFile",
                block = {
                    deleteFileInternal(path)
                }
            )
        }
    }

    private suspend fun deleteFileInternal(path: String): Result<Unit> {
        try {
            android.util.Log.d(TAG, "Deleting file...")
            if (path.isBlank()) {
                return Result.failure(StorageException.InvalidPath("Path cannot be empty"))
            }
            storage.from(CHAT_MEDIA_BUCKET).delete(path)
            android.util.Log.d(TAG, "Delete successful")
            return Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Delete failed", e)
            return Result.failure(mapStorageException(e, "Delete failed"))
        }
    }

    fun getPublicUrl(path: String): String {
        return storage.from(CHAT_MEDIA_BUCKET).publicUrl(path)
    }

    fun generateStoragePath(chatId: String, fileName: String): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
        val fileExtension = fileName.substringAfterLast(".", "")
        val uniqueFileName = "${UUID.randomUUID()}_${fileName.substringBeforeLast(".")}"
        val finalFileName = if (fileExtension.isNotEmpty()) {
            "$uniqueFileName.$fileExtension"
        } else {
            uniqueFileName
        }
        return "$chatId/$year/$month/$day/$finalFileName"
    }

    suspend fun testStorageInfrastructure(context: Context): Result<String> {
        return try {
            Result.success("Storage infrastructure test removed")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error running storage infrastructure tests", e)
            Result.failure(e)
        }
    }

    fun extractPathFromUrl(url: String, bucket: String): String? {
        return try {
            val bucketPath = "/storage/v1/object/public/$bucket/"
            if (url.contains(bucketPath)) {
                url.substringAfter(bucketPath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
