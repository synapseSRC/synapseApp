package com.synapse.social.studioasinc.data.repository.helpers

import com.synapse.social.studioasinc.shared.data.local.database.PostDao
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonPrimitive

internal class PostSyncHelper(
    private val postDao: PostDao,
    private val client: JanSupabaseClient,
    private val offlineActionRepository: OfflineActionRepository
) {

    private fun findDeletedIds(localChunk: List<String>, serverResponse: List<JsonObject>): List<String> {
        val serverIds = serverResponse.mapNotNull { it["id"]?.let { if (it is JsonPrimitive) it else null }?.contentOrNull }.toSet()

        // Hard deleted: in localChunk but not in serverIds
        val missingIds = localChunk.filter { !serverIds.contains(it) }

        // Soft deleted: in serverResponse with is_deleted=true
        val softDeletedIds = serverResponse.filter {
            it["is_deleted"]?.let { if (it is JsonPrimitive) it else null }?.booleanOrNull == true
        }.mapNotNull { it["id"]?.let { if (it is JsonPrimitive) it else null }?.contentOrNull }

        return (missingIds + softDeletedIds).distinct()
    }

    suspend fun syncDeletedPosts() = coroutineScope {
        try {
            val localIds = postDao.getAllPostIds()
            if (localIds.isEmpty()) return@coroutineScope

            val semaphore = Semaphore(5)

            // Process in chunks of 50 to respect URL length limits (approx 2KB max for safe GET)
            // 50 UUIDs * 36 chars = 1800 chars + overhead
            val idsToDelete = localIds.chunked(50).map { chunk ->
                async {
                    semaphore.withPermit {
                        try {
                            val response = client.from("posts")
                                .select(columns = Columns.raw("id, is_deleted")) {
                                    filter { isIn("id", chunk) }
                                }
                                .decodeList<JsonObject>()
                            findDeletedIds(chunk, response)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            android.util.Log.e(PostRepositoryUtils.TAG, "Failed to check chunk existence", e)
                            emptyList<String>()
                        }
                    }
                }
            }.awaitAll().flatten()

            if (idsToDelete.isNotEmpty()) {
                val uniqueIdsToDelete = idsToDelete.distinct()
                android.util.Log.d(PostRepositoryUtils.TAG, "Syncing deletions: removing ${uniqueIdsToDelete.size} posts")
                postDao.deleteByIds(uniqueIdsToDelete)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(PostRepositoryUtils.TAG, "Failed to sync deleted posts", e)
        }
    }
}
