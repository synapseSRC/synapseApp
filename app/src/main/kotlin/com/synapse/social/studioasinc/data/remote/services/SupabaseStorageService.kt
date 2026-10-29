package com.synapse.social.studioasinc.data.remote.services

import android.content.Context
import io.github.jan.supabase.storage.upload
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlin.time.Duration.Companion.minutes
import javax.inject.Inject
import com.synapse.social.studioasinc.shared.domain.model.UploadError
import java.io.File
import java.util.*
import kotlin.math.pow

class SupabaseStorageService @Inject constructor() {
    companion object {
        private const val TAG = "SupabaseStorageService"
    }

    private val client = SupabaseClient.client
    private val storage = client.storage

    suspend fun uploadAvatar(userId: String, filePath: String): Result<String> {
        return uploadImage(SupabaseClient.BUCKET_USER_AVATARS, userId, filePath)
    }

    suspend fun uploadCover(userId: String, filePath: String): Result<String> {
        return uploadImage(SupabaseClient.BUCKET_USER_COVERS, userId, filePath)
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
                    } catch (cancelled: CancellationException) {
                        throw cancelled
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

                // Covers are private. Verify the owner-scoped object is readable before saving its reference.
                if (bucket == SupabaseClient.BUCKET_USER_COVERS) {
                    try {
                        storage.from(bucket).createSignedUrl(path = path, expiresIn = 15.minutes)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (verificationError: Exception) {
                        return@withContext Result.failure(
                            UploadError.StorageVerificationError(
                                "Cover uploaded, but private Storage signing failed: ${verificationError.message ?: "unknown error"}"
                            )
                        )
                    }
                }

                val mediaReference = if (bucket in setOf("covers", "story-media", "story-thumbnails", "reels")) {
                    "private-media://$bucket/$path"
                } else {
                    storage.from(bucket).publicUrl(path)
                }

                if (mediaReference.isBlank()) {
                    android.util.Log.e("SupabaseStorage", "Failed to create storage media reference")
                    return@withContext Result.failure(Exception("Failed to create storage media reference"))
                }

                // Log public URL only if safe? Public URL usually contains path.
                // But if it's public, maybe it's okay?
                // The comment was about system logs leaking PII.
                // Let's be safe and log just success.
                android.util.Log.d("SupabaseStorage", "Upload successful")
                Result.success(mediaReference)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                android.util.Log.e("SupabaseStorage", "Upload failed", e)
                Result.failure(e)
            }
        }
    }
}

