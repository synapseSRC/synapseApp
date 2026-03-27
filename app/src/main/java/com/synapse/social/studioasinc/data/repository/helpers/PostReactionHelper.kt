package com.synapse.social.studioasinc.data.repository.helpers

import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.domain.model.UserReaction
import com.synapse.social.studioasinc.shared.data.local.database.PostDao
import com.synapse.social.studioasinc.shared.domain.model.PendingAction
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import com.synapse.social.studioasinc.data.repository.PostMapper
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive

internal class PostReactionHelper(
    private val postDao: PostDao,
    private val client: JanSupabaseClient,
    private val offlineActionRepository: OfflineActionRepository
) {

    private val reactionRepository = com.synapse.social.studioasinc.data.repository.ReactionRepository(client)

    suspend fun toggleReaction(
        postId: String,
        userId: String,
        reactionType: ReactionType,
        oldReaction: ReactionType? = null,
        skipCheck: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // Optimistic update
        val post = postDao.getPostById(postId)?.let { PostMapper.toModel(it) }
        if (post != null) {
            val updatedReactions = post.reactions?.toMutableMap() ?: mutableMapOf()
            val currentCount = updatedReactions[reactionType] ?: 0

            if (oldReaction == reactionType) {
                updatedReactions[reactionType] = maxOf(0, currentCount - 1)
                post.userReaction = null
            } else {
                if (oldReaction != null) {
                    val oldTypeCount = updatedReactions[oldReaction] ?: 0
                    updatedReactions[oldReaction] = maxOf(0, oldTypeCount - 1)
                }
                updatedReactions[reactionType] = currentCount + 1
                post.userReaction = reactionType
            }

            val updatedPost = post.copy(
                reactions = updatedReactions,
                likesCount = updatedReactions.values.sum()
            )
            postDao.insert(PostMapper.toEntity(updatedPost))
        }

        try {
            reactionRepository.toggleReaction(postId, "post", reactionType, oldReaction, skipCheck)
                .map { Unit }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w(PostRepositoryUtils.TAG, "Network reaction toggle failed, queuing for background sync", e)
            offlineActionRepository.addAction(
                PendingAction(
                    id = java.util.UUID.randomUUID().toString(),
                    actionType = PendingAction.ActionType.LIKE,
                    targetId = postId,
                    payload = buildJsonObject {
                        put("reactionType", reactionType.name)
                        put("oldReaction", oldReaction?.name)
                    }.toString()
                )
            )
            Result.success(Unit) // Return success because it's queued
        }
    }

    suspend fun getReactionSummary(postId: String): Result<Map<ReactionType, Int>> =
        reactionRepository.getReactionSummary(postId, "post")

    suspend fun getUserReaction(postId: String, userId: String): Result<ReactionType?> =
        if (userId == client.auth.currentUserOrNull()?.id) {
             reactionRepository.getUserReaction(postId, "post")
        } else {
             withContext(Dispatchers.IO) {
                 try {
                     val reaction = client.from("reactions")
                         .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                         .decodeSingleOrNull<JsonObject>()
                     val typeStr = reaction?.get("reaction_type")?.let { if (it is JsonPrimitive) it else null }?.contentOrNull
                     Result.success(typeStr?.let { ReactionType.fromString(it) })
                 } catch (e: CancellationException) {
                     throw e
                 } catch (e: Exception) {
                     Result.failure(Exception("Error fetching user reaction"))
                 }
             }
        }

    suspend fun getUsersWhoReacted(
        postId: String,
        reactionType: ReactionType? = null
    ): Result<List<UserReaction>> = withContext(Dispatchers.IO) {
        try {
            val reactions = client.from("reactions")
                .select(Columns.raw("*, users!inner(uid, username, display_name, avatar, verify)")) {
                    filter {
                        eq("post_id", postId)
                        if (reactionType != null) eq("reaction_type", reactionType.name)
                    }
                }
                .decodeList<JsonObject>()

            val userReactions = reactions.mapNotNull { reaction ->
                val userId = reaction["user_id"]?.let { if (it is JsonPrimitive) it else null }?.contentOrNull ?: return@mapNotNull null
                val user = reaction["users"]?.jsonObject
                val dName = user?.get("display_name")?.let { if (it is JsonPrimitive) it else null }?.contentOrNull
                UserReaction(
                    userId = userId,
                    username = if (!dName.isNullOrBlank()) dName else (user?.get("username")?.let { if (it is JsonPrimitive) it else null }?.contentOrNull ?: "Unknown"),
                    profileImage = user?.get("avatar")?.let { if (it is JsonPrimitive) it else null }?.contentOrNull?.let { PostRepositoryUtils.constructAvatarUrl(it) },
                    isVerified = user?.get("verify")?.let { if (it is JsonPrimitive) it else null }?.booleanOrNull ?: false,
                    reactionType = reaction["reaction_type"]?.let { if (it is JsonPrimitive) it else null }?.contentOrNull ?: "LIKE",
                    reactedAt = reaction["created_at"]?.let { if (it is JsonPrimitive) it else null }?.contentOrNull
                )
            }
            Result.success(userReactions)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(PostRepositoryUtils.mapSupabaseError(e)))
        }
    }
}
