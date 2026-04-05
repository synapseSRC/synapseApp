package com.synapse.social.studioasinc.shared.data.source.remote

import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.UploadError
import com.synapse.social.studioasinc.shared.util.TimeProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.github.aakira.napier.Napier
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.coroutineScope

class SupabaseUploadService(private val supabase: SupabaseClient) : UploadService {
    override suspend fun upload(
        fileProvider: suspend (Long) -> ByteReadChannel,
        fileSize: Long,
        fileName: String,
        config: StorageConfig,
        bucketName: String?,
        onProgress: (Float) -> Unit
    ): String {
        val targetBucket = bucketName ?: config.supabaseBucket
        val bucketToUse = if (targetBucket.isBlank()) "public" else targetBucket

        Napier.d("Uploading to Supabase bucket: $bucketToUse, file: $fileName", tag = "SupabaseUpload")

        try {
            val bucket = supabase.storage.from(bucketToUse)
            val path = "${TimeProvider.nowMillis()}_$fileName"

            coroutineScope {
                val channel = fileProvider(0L)
                val bytes = channel.readRemaining().readBytes()
                bucket.upload(path, bytes) {
                    this.upsert = false
                }
                onProgress(1.0f)
            }

            val publicUrl = bucket.publicUrl(path)
            Napier.d("Supabase upload successful: $publicUrl", tag = "SupabaseUpload")
            return publicUrl
        } catch (e: Exception) {
            Napier.e("Supabase upload failed to bucket: $bucketToUse", e, tag = "SupabaseUpload")
            throw UploadError.SupabaseError("Supabase upload failed: ${e.message}")
        }
    }
}
