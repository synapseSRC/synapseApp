package com.synapse.social.studioasinc.core.util

import android.util.Log
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException



object RetryHandler {

    private const val TAG = "RetryHandler"



    data class RetryConfig(
        val maxAttempts: Int = 3,
        val initialDelayMs: Long = 1000L,
        val maxDelayMs: Long = 4000L,
        val exponentialBase: Double = 2.0,
        val retryableExceptions: List<Class<out Throwable>> = listOf(
            IOException::class.java,
            SocketTimeoutException::class.java,
            UnknownHostException::class.java
        )
    )



    sealed class RetryResult<T> {
        data class Success<T>(val value: T, val attemptNumber: Int) : RetryResult<T>()
        data class Failure<T>(
            val exception: Throwable,
            val attemptNumber: Int,
            val isRetryable: Boolean
        ) : RetryResult<T>()
    }



    suspend fun <T> executeWithRetry(
        config: RetryConfig = RetryConfig(),
        operation: suspend (attemptNumber: Int) -> T
    ): RetryResult<T> {
        var currentAttempt = 1
        var lastException: Throwable? = null

        while (currentAttempt <= config.maxAttempts) {
            try {
                Log.d(TAG, "Executing operation - Attempt: $currentAttempt/${config.maxAttempts}")

                val result = operation(currentAttempt)

                if (currentAttempt > 1) {
                    Log.d(TAG, "Operation succeeded after $currentAttempt attempts")
                }

                return RetryResult.Success(result, currentAttempt)
            } catch (e: Exception) {
                lastException = e

                val isRetryable = isRetryableException(e, config)

                Log.w(TAG, "Operation failed - Attempt: $currentAttempt/${config.maxAttempts}, Retryable: $isRetryable, Error: ${e.message}")


                if (!isRetryable || currentAttempt >= config.maxAttempts) {
                    Log.e(TAG, "Operation failed permanently after $currentAttempt attempts", e)
                    return RetryResult.Failure(e, currentAttempt, isRetryable)
                }


                val delayMs = calculateBackoffDelay(
                    attemptNumber = currentAttempt,
                    initialDelayMs = config.initialDelayMs,
                    maxDelayMs = config.maxDelayMs,
                    exponentialBase = config.exponentialBase
                )

                Log.d(TAG, "Retrying after ${delayMs}ms delay")
                delay(delayMs)

                currentAttempt++
            }
        }


        return RetryResult.Failure(
            lastException ?: Exception("Unknown error"),
            currentAttempt,
            false
        )
    }



    suspend fun <T> executeWithRetryResult(
        config: RetryConfig = RetryConfig(),
        operation: suspend (attemptNumber: Int) -> T
    ): Result<T> {
        return when (val result = executeWithRetry(config, operation)) {
            is RetryResult.Success -> Result.success(result.value)
            is RetryResult.Failure -> Result.failure(result.exception)
        }
    }



    private fun isRetryableException(
        exception: Throwable,
        config: RetryConfig
    ): Boolean {

        val isRetryableType = config.retryableExceptions.any { retryableClass ->
            retryableClass.isInstance(exception)
        }

        if (isRetryableType) {
            return true
        }


        val message = exception.message?.lowercase() ?: ""
        val retryableKeywords = listOf(
            "timeout",
            "connection",
            "network",
            "temporary",
            "unavailable"
        )

        return retryableKeywords.any { keyword -> message.contains(keyword) }
    }



    fun calculateBackoffDelay(
        attemptNumber: Int,
        initialDelayMs: Long = 1000L,
        maxDelayMs: Long = 4000L,
        exponentialBase: Double = 2.0
    ): Long {
        val exponentialDelay = (initialDelayMs * Math.pow(exponentialBase, (attemptNumber - 1).toDouble())).toLong()
        return exponentialDelay.coerceAtMost(maxDelayMs)
    }



    fun shouldRetry(exception: Throwable, attemptNumber: Int, maxAttempts: Int = 3): Boolean {
        if (attemptNumber >= maxAttempts) {
            return false
        }

        return isRetryableException(exception, RetryConfig())
    }



    fun getRetryMessage(attemptNumber: Int, maxAttempts: Int): String {
        return when {
            attemptNumber < maxAttempts -> "Retrying... (Attempt $attemptNumber of $maxAttempts)"
            else -> "All retry attempts failed"
        }
    }
}
