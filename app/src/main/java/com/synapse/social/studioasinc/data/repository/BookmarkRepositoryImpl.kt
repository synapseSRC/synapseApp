package com.synapse.social.studioasinc.data.repository

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject



class BookmarkRepositoryImpl @Inject constructor(
    private val client: SupabaseClient = com.synapse.social.studioasinc.shared.core.network.SupabaseClient.client
) {

    @Serializable
    private data class Favorite(
        val id: String? = null,
        @SerialName("post_id") val postId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("collection_id") val collectionId: String? = null
    )



    suspend fun isBookmarked(postId: String): Result<Boolean> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("Not authenticated"))

        val favorites = client.from("favorites")
            .select(Columns.list("id")) {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            .decodeList<Favorite>()

        favorites.isNotEmpty()
    }



    suspend fun toggleBookmark(postId: String, collectionId: String? = null): Result<Boolean> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("Not authenticated"))

        val existing = client.from("favorites")
            .select(Columns.list("id")) {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            .decodeList<Favorite>()
            .firstOrNull()

        if (existing != null) {
            client.from("favorites")
                .delete { existing.id?.let { id -> filter { eq("id", id) } } }
            Log.d(TAG, "Bookmark removed: $postId")
            false
        } else {
            client.from("favorites")
                .insert(Favorite(postId = postId, userId = userId, collectionId = collectionId))
            Log.d(TAG, "Bookmark added: $postId")
            true
        }
    }

    companion object {
        private const val TAG = "BookmarkRepositoryImpl"
    }
}
