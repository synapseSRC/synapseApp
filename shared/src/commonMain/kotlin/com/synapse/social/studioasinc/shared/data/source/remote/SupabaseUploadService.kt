package com.synapse.social.studioasinc.shared.data.source.remote

import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.UploadError
import com.synapse.social.studioasinc.shared.domain.model.UploadResult
import com.synapse.social.studioasinc.shared.util.TimeProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.auth.auth
import io.github.aakira.napier.Napier
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.coroutineScope
import kotlinx.io.readByteArray
import kotlin.time.Duration.Companion.minutes

class SupabaseUploadService(private val supabase: SupabaseClient) : UploadService {
    private val protectedMediaBuckets = setOf("covers", "story-media", "story-thumbnails", "reels")
    private val userScopedBuckets = protectedMediaBuckets + com.synapse.social.studioasinc.shared.core.network.SupabaseClient.BUCKET_USER_AVATARS

    override suspend fun upload(
        fileProvider: suspend (Long) -> ByteReadChannel,
        fileSize: Long,
        fileName: String,
        config: StorageConfig,
        bucketName: String?,
        onProgress: (Float) -> Unit
    ): UploadResult {
        val targetBucket = bucketName.orEmpty()
            .ifBlank { config.primaryBucket }
            .ifBlank { com.synapse.social.studioasinc.shared.core.network.SupabaseClient.BUCKET_POST_MEDIA }

        Napier.d("Uploading to Supabase bucket: $targetBucket, file: $fileName", tag = "SupabaseUpload")

        try {
            val bucket = supabase.storage.from(targetBucket)
            val objectName = "${TimeProvider.nowMillis()}_$fileName"
            val path = if (targetBucket in userScopedBuckets) {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: throw UploadError.BackendError("An authenticated user is required to upload profile media")
                "$userId/$objectName"
            } else {
                objectName
            }

            coroutineScope {
                val channel = fileProvider(0L)
                val bytes = channel.readBuffer().readByteArray()
                bucket.upload(path, bytes) {
                    this.upsert = false
                }
                // Covers can be signed before persistence when owner-scoped paths are used.
                // Post/story/reel policies require a linked content row, which is created after upload.
                if (targetBucket == com.synapse.social.studioasinc.shared.core.network.SupabaseClient.BUCKET_USER_COVERS) {
                    try {
                        bucket.createSignedUrl(path = path, expiresIn = 15.minutes)
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (verificationError: Exception) {
                        throw UploadError.StorageVerificationError(
                            "Upload succeeded, but cover signing verification failed: ${verificationError.message ?: "unknown error"}"
                        )
                    }
                }
                onProgress(1.0f)
            }

            // Keep the main-branch public URL path while returning private references for protected buckets.
            val url = createPublicStorageUrl(supabase, targetBucket, path)
                .let { publicUrl ->
                    if (targetBucket in protectedMediaBuckets) "private-media://$targetBucket/$path" else publicUrl
                }
            Napier.d("Supabase upload successful to $targetBucket", tag = "SupabaseUpload")
            return UploadResult(url = url, deleteUrl = null)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: UploadError.StorageVerificationError) {
            Napier.e("Supabase upload succeeded but protected object verification failed: $targetBucket", e, tag = "SupabaseUpload")
            throw e
        } catch (e: Exception) {
            Napier.e("Supabase upload failed to bucket: $targetBucket", e, tag = "SupabaseUpload")
            val fallbackBucket = if (targetBucket in userScopedBuckets) {
                null // Protected profile media must never fall back into a public/configurable bucket.
            } else {
                config.primaryBucket.ifBlank {
                    com.synapse.social.studioasinc.shared.core.network.SupabaseClient.BUCKET_POST_MEDIA
                }
            }

            if (fallbackBucket != null && targetBucket != fallbackBucket) {
                try {
                    Napier.d("Retrying Supabase upload to fallback bucket: $fallbackBucket", tag = "SupabaseUpload")
                    val bucket = supabase.storage.from(fallbackBucket)
                    val fallbackObjectName = "${TimeProvider.nowMillis()}_$fileName"
                    val path = if (fallbackBucket in protectedMediaBuckets) {
                        val userId = supabase.auth.currentUserOrNull()?.id
                            ?: throw UploadError.BackendError("An authenticated user is required to upload protected media")
                        "$userId/$fallbackObjectName"
                    } else {
                        fallbackObjectName
                    }

                    coroutineScope {
                        val channel = fileProvider(0L)
                        val bytes = channel.readBuffer().readByteArray()
                        bucket.upload(path, bytes) {
                            this.upsert = false
                        }
                        onProgress(1.0f)
                    }

                    val url = createPublicStorageUrl(supabase, fallbackBucket, path)
                        .let { publicUrl ->
                            if (fallbackBucket in protectedMediaBuckets) "private-media://$fallbackBucket/$path" else publicUrl
                        }
                    Napier.d("Supabase fallback upload successful to $fallbackBucket", tag = "SupabaseUpload")
                    return UploadResult(url = url, deleteUrl = null)
                } catch (fallbackEx: Exception) {
                    Napier.e("Supabase fallback upload failed to bucket: $fallbackBucket", fallbackEx, tag = "SupabaseUpload")
                }
            }

            throw UploadError.BackendError("Supabase upload failed: ${e.message ?: e.toString()}")
        }
    }
}

internal fun createPublicStorageUrl(
    supabase: SupabaseClient,
    bucketName: String,
    path: String
): String = try {
    supabase.storage.from(bucketName).publicUrl(path)
} catch (e: Exception) {
    // In headless test environments where Settings/ResumableCache is not initialized,
    // construct the standard Supabase public storage endpoint URL.
    val base = supabase.supabaseUrl.trimEnd('/')
    "$base/storage/v1/object/public/$bucketName/$path"
}
