package com.synapse.social.studioasinc.data.remote.services

import android.content.Context
import io.github.jan.supabase.storage.upload
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.math.pow



class SupabaseStorageService {

    companion object {
        private const val TAG = "SupabaseStorageService"
        private const val CHAT_MEDIA_BUCKET = "chat-media"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BASE_RETRY_DELAY_MS = 1000L
        private const val PROGRESS_UPDATE_INTERVAL_MS = 100L
    }

    private val client = SupabaseClient.client
    private val storage = client.storage



    suspend fun uploadAvatar(userId: String, filePath: String): Result<String> {
        return uploadImage(SupabaseClient.BUCKET_USER_AVATARS, userId, filePath)
    }



    suspend fun uploadCover(userId: String, filePath: String): Result<String> {
        return uploadImage(SupabaseClient.BUCKET_USER_COVERS, userId, filePath)
    }



    suspend fun uploadPostImage(userId: String, filePath: String): Result<String> {
        return uploadImage(SupabaseClient.BUCKET_POST_MEDIA, userId, filePath)
    }



    private suspend fun uploadImage(bucket: String, userId: String, filePath: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Sanitized log: Removed filePath
                android.util.Log.d("SupabaseStorage", "Uploading image to bucket: $bucket")

                val file = File(filePath)
                if (!file.exists()) {
                    // Sanitized log: Removed filePath
                    android.util.Log.e("SupabaseStorage", "File not found")
                    return@withContext Result.failure(Exception("File not found"))
                }

                if (file.length() == 0L) {
                    // Sanitized log: Removed filePath
                    android.util.Log.e("SupabaseStorage", "File is empty")
                    return@withContext Result.failure(Exception("File is empty"))
                }

                // Optimization: Do not read file into bytes. Use File object directly.
                android.util.Log.d("SupabaseStorage", "File size: ${file.length()} bytes")

                val fileName = "${UUID.randomUUID()}.${file.extension}"
                val path = "$userId/$fileName"

                // Sanitized log: Removed path
                android.util.Log.d("SupabaseStorage", "Uploading image...")


                var uploadSuccess = false
                var lastException: Exception? = null

                for (attempt in 1..3) {
                    try {
                        // Optimized: Upload using File object directly to avoid memory overhead
                        storage.from(bucket).upload(path, file) { upsert = true }
                        uploadSuccess = true
                        break
                    } catch (e: Exception) {
                        lastException = e
                        android.util.Log.w("SupabaseStorage", "Upload attempt $attempt failed", e)
                        if (attempt < 3) {
                            delay(1000L * attempt)
                        }
                    }
                }

                if (!uploadSuccess) {
                    android.util.Log.e("SupabaseStorage", "Upload failed after 3 attempts")
                    return@withContext Result.failure(lastException ?: Exception("Upload failed after retries"))
                }


                val publicUrl = storage.from(bucket).publicUrl(path)

                if (publicUrl.isBlank()) {
                    android.util.Log.e("SupabaseStorage", "Failed to get public URL")
                    return@withContext Result.failure(Exception("Failed to get public URL"))
                }

                // Log public URL only if safe? Public URL usually contains path.
                // But if it's public, maybe it's okay?
                // The comment was about system logs leaking PII.
                // Let's be safe and log just success.
                android.util.Log.d("SupabaseStorage", "Upload successful")
                Result.success(publicUrl)

            } catch (e: Exception) {
                android.util.Log.e("SupabaseStorage", "Upload failed", e)
                Result.failure(Exception("Upload failed: ${e.message}"))
            }
        }
    }



    suspend fun deleteImage(bucket: String, path: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                storage.from(bucket).delete(path)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }



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
            // Sanitized log: Removed path
            android.util.Log.d(TAG, "Uploading file to chat-media (${file.length()} bytes)")

            onProgress(0.0f)

            storage.from(CHAT_MEDIA_BUCKET).upload(path, file) { upsert = true }

            // Simplified progress: Removed 0.9f intermediate step as per review

            val publicUrl = storage.from(CHAT_MEDIA_BUCKET).publicUrl(path)

            onProgress(1.0f)
            // Sanitized log: Removed publicUrl
            android.util.Log.d(TAG, "Upload successful")

            return Result.success(publicUrl)

        } catch (e: Exception) {
            // Sanitized log: Removed path
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
            // Sanitized log: Removed path
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
            // Sanitized log: Removed path
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



    private suspend fun <T> retryWithExponentialBackoff(
        maxAttempts: Int,
        operation: String,
        block: suspend () -> Result<T>
    ): Result<T> {
        var lastException: Exception? = null

        repeat(maxAttempts) { attempt ->
            try {
                val result = block()
                if (result.isSuccess) {
                    if (attempt > 0) {
                        android.util.Log.d(TAG, "$operation succeeded on attempt ${attempt + 1}")
                    }
                    return result
                }


                lastException = result.exceptionOrNull() as? Exception
                    ?: Exception("Operation failed")

            } catch (e: Exception) {
                lastException = e
            }


            lastException?.let { exception ->
                if (!shouldRetry(exception)) {
                    android.util.Log.d(TAG, "$operation failed with non-retryable error: ${exception.message}")
                    return Result.failure(exception)
                }
            }


            if (attempt < maxAttempts - 1) {
                val delayMs = calculateBackoffDelay(attempt)
                android.util.Log.d(TAG, "$operation failed on attempt ${attempt + 1}, retrying in ${delayMs}ms")
                delay(delayMs)
            }
        }

        android.util.Log.e(TAG, "$operation failed after $maxAttempts attempts")
        return Result.failure(lastException ?: Exception("Operation failed after $maxAttempts attempts"))
    }



    private fun calculateBackoffDelay(attempt: Int): Long {
        return (BASE_RETRY_DELAY_MS * 2.0.pow(attempt.toDouble())).toLong()
    }



    private fun shouldRetry(exception: Exception): Boolean {
        return when (exception) {
            is StorageException.FileNotFound,
            is StorageException.InvalidFile,
            is StorageException.InvalidUrl,
            is StorageException.InvalidPath,
            is StorageException.AuthenticationError -> false
            is StorageException.NetworkError,
            is StorageException.StorageQuotaError,
            is StorageException.ServerError -> true
            else -> {

                val message = exception.message?.lowercase() ?: ""
                when {
                    message.contains("network") -> true
                    message.contains("timeout") -> true
                    message.contains("connection") -> true
                    message.contains("server error") -> true
                    message.contains("503") -> true
                    message.contains("502") -> true
                    message.contains("500") -> true
                    else -> false
                }
            }
        }
    }



    private fun mapStorageException(exception: Exception, defaultMessage: String): StorageException {
        val message = exception.message?.lowercase() ?: ""

        return when {
            message.contains("not found") || message.contains("404") ->
                StorageException.FileNotFound("File not found: ${exception.message}")
            message.contains("unauthorized") || message.contains("401") ->
                StorageException.AuthenticationError("Authentication failed: ${exception.message}")
            message.contains("quota") || message.contains("storage limit") ->
                StorageException.StorageQuotaError("Storage quota exceeded: ${exception.message}")
            message.contains("network") || message.contains("connection") || message.contains("timeout") ->
                StorageException.NetworkError("Network error: ${exception.message}")
            message.contains("500") || message.contains("502") || message.contains("503") ->
                StorageException.ServerError("Server error: ${exception.message}")
            else -> StorageException.UnknownError("$defaultMessage: ${exception.message}")
        }
    }
}



sealed class StorageException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class FileNotFound(message: String) : StorageException(message)
    class InvalidFile(message: String) : StorageException(message)
    class InvalidUrl(message: String) : StorageException(message)
    class InvalidPath(message: String) : StorageException(message)
    class NetworkError(message: String) : StorageException(message)
    class AuthenticationError(message: String) : StorageException(message)
    class StorageQuotaError(message: String) : StorageException(message)
    class ServerError(message: String) : StorageException(message)
    class EmptyFile(message: String) : StorageException(message)
    class UnknownError(message: String) : StorageException(message)
}
