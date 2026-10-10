package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.domain.repository.MediaUploadRepository
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class PostInteractionRepositoryImpl @Inject constructor(
    private val mediaUploadRepository: MediaUploadRepository? = null
) : com.synapse.social.studioasinc.domain.repository.PostInteractionRepository {
    private val client = SupabaseClient.client

    override suspend fun likePost(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("post_likes").insert(
                buildJsonObject {
                    put("post_id", postId)
                    put("user_id", userId)
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikePost(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("post_likes").delete {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun savePost(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("favorites").insert(
                buildJsonObject {
                    put("post_id", postId)
                    put("user_id", userId)
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unsavePost(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("favorites").delete {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePost(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            try {
                val dto = client.from("posts").select(Columns.list("media_items")) {
                    filter {
                        eq("id", postId)
                        eq("author_uid", userId)
                    }
                }.decodeSingleOrNull<JsonObject>()
                val deleteUrls = dto?.get("media_items")?.takeIf { it !is JsonNull }?.jsonArray?.mapNotNull { item ->
                    val obj = item.jsonObject
                    val deleteUrl = obj["delete_url"]?.let { if (it is JsonPrimitive) it else null }?.contentOrNull
                        ?: obj["deleteUrl"]?.let { if (it is JsonPrimitive) it else null }?.contentOrNull
                    deleteUrl?.takeIf { it.isNotBlank() }
                } ?: emptyList()

                if (deleteUrls.isNotEmpty()) {
                    mediaUploadRepository?.deleteImgBbImages(deleteUrls)
                }
            } catch (_: Exception) {
                // Best-effort remote image cleanup
            }

            client.from("posts").delete {
                filter {
                    eq("id", postId)
                    eq("author_uid", userId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reportPost(postId: String, userId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.from("post_reports").insert(
                buildJsonObject {
                    put("post_id", postId)
                    put("reporter_id", userId)
                    put("reason", reason)
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
