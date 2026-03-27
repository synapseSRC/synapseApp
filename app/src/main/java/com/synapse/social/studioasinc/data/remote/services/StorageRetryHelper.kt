package com.synapse.social.studioasinc.data.remote.services

import kotlinx.coroutines.delay
import kotlin.math.pow

internal suspend fun <T> retryWithExponentialBackoff(
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
                    android.util.Log.d("StorageRetryHelper", "$operation succeeded on attempt ${attempt + 1}")
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
                android.util.Log.d("StorageRetryHelper", "$operation failed with non-retryable error: ${exception.message}")
                return Result.failure(exception)
            }
        }


        if (attempt < maxAttempts - 1) {
            val delayMs = calculateBackoffDelay(attempt)
            android.util.Log.d("StorageRetryHelper", "$operation failed on attempt ${attempt + 1}, retrying in ${delayMs}ms")
            delay(delayMs)
        }
    }

    android.util.Log.e("StorageRetryHelper", "$operation failed after $maxAttempts attempts")
    return Result.failure(lastException ?: Exception("Operation failed after $maxAttempts attempts"))
}



internal fun calculateBackoffDelay(attempt: Int): Long {
    return (ChatStorageService.BASE_RETRY_DELAY_MS * 2.0.pow(attempt.toDouble())).toLong()
}



internal fun shouldRetry(exception: Exception): Boolean {
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



internal fun mapStorageException(exception: Exception, defaultMessage: String): StorageException {
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
