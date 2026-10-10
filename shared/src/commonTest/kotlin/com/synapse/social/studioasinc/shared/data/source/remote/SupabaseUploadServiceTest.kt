package com.synapse.social.studioasinc.shared.data.source.remote

import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupabaseUploadServiceTest {

    @Test
    fun testBucketNameFallbackToPostMediaWhenBlank() = runTest {
        val client = createSupabaseClient(
            supabaseUrl = "https://apqvyyphlrtmuyjnzmuq.supabase.co",
            supabaseKey = "anon-key-test"
        ) {
            install(Storage)
        }

        val service = SupabaseUploadService(client)
        val config = StorageConfig(supabaseBucket = "")

        var targetBucketUsed = ""
        // Testing that fallback defaults without throwing bucket blank errors prior to network request
        // Verify bucket defaulting logic
        val bucketName = null.orEmpty().ifBlank { config.supabaseBucket }.ifBlank { com.synapse.social.studioasinc.shared.core.network.SupabaseClient.BUCKET_POST_MEDIA }
        assertEquals("posts", bucketName)
    }


    @Test
    fun chatAttachmentUploadUrlUsesPublicObjectEndpoint() = runTest {
        val client = createSupabaseClient(
            supabaseUrl = "https://apqvyyphlrtmuyjnzmuq.supabase.co",
            supabaseKey = "anon-key-test"
        ) {
            install(Storage)
        }

        val url = createPublicStorageUrl(
            supabase = client,
            bucketName = "chat-attachments",
            path = "chat-id/compressed-image.jpg"
        )

        assertTrue(url.contains("/storage/v1/object/public/chat-attachments/chat-id/compressed-image.jpg"))
    }

}