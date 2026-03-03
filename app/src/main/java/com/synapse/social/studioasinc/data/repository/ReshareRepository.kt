package com.synapse.social.studioasinc.data.repository

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject



class ReshareRepository @Inject constructor(
    private val client: SupabaseClient = com.synapse.social.studioasinc.shared.core.network.SupabaseClient.client
) {

    @Serializable
    private data class Reshare(
        val id: String? = null,
        @SerialName("post_id") val postId: String,
        @SerialName("user_id") val userId: String,
        @SerialName("reshare_text") val reshareText: String? = null
    )



    suspend fun hasReshared(postId: String): Result<Boolean> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("Not authenticated"))

        val reshares = client.from("reshares")
            .select(Columns.list("id")) {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            .decodeList<Reshare>()

        reshares.isNotEmpty()
    }



    suspend fun createReshare(postId: String, commentary: String? = null): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("Not authenticated"))


        val existing = client.from("reshares")
            .select(Columns.list("id")) {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            .decodeList<Reshare>()
            .firstOrNull()

        if (existing != null) {
            return Result.failure(Exception("Already reshared"))
        }

        client.from("reshares")
            .insert(Reshare(postId = postId, userId = userId, reshareText = commentary))


        Log.d(TAG, "Reshare created: $postId")
    }

    companion object {
        private const val TAG = "ReshareRepository"
    }
}
