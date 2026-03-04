package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.domain.model.CommentWithUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlinx.serialization.SerialName
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject



class ReactionRepository @Inject constructor(
    private val client: SupabaseClient = com.synapse.social.studioasinc.shared.core.network.SupabaseClient.client
) {

    companion object {
        private const val TAG = "ReactionRepository"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 100L
    }





    suspend fun toggleReaction(
        targetId: String,
        targetType: String,
        reactionType: ReactionType,
        oldReaction: ReactionType? = null,
        skipCheck: Boolean = false
    ): Result<ReactionToggleResult> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("User must be authenticated to react"))

            val userId = currentUser.id
            val tableName = getTableName(targetType)
            val idColumn = getIdColumn(targetType)

            Log.d(TAG, "Toggling reaction: ${reactionType.name} for $targetType $targetId by user $userId")

            // Optimized path if oldReaction is known
            if (skipCheck) {
                try {
                     if (oldReaction == reactionType) {
                         // Removing - Single Round Trip
                         client.from(tableName)
                             .delete { filter { eq(idColumn, targetId); eq("user_id", userId) } }
                         Log.d(TAG, "Reaction removed for $targetType $targetId (Optimized)")
                         return@withContext Result.success(ReactionToggleResult.REMOVED)
                     } else {
                         // Updating/Inserting - Single Round Trip (Upsert)
                         client.from(tableName).upsert(buildJsonObject {
                            put("user_id", userId)
                            put(idColumn, targetId)
                            put("reaction_type", reactionType.name.lowercase())
                            put("updated_at", java.time.Instant.now().toString())
                        }) {
                            onConflict = "user_id, " + idColumn
                        }
                         Log.d(TAG, "Reaction updated to ${reactionType.name} for $targetType $targetId (Optimized)")
                         return@withContext Result.success(ReactionToggleResult.UPDATED)
                     }
                } catch (e: Exception) {
                     Log.w(TAG, "Optimized toggle failed, falling back to standard Check-Then-Act: ${e.message}")
                     // Fallthrough to standard logic
                }
            }

            var lastException: Exception? = null
            repeat(MAX_RETRIES) { attempt ->
                try {

                    val existingReaction = client.from(tableName)
                        .select { filter { eq(idColumn, targetId); eq("user_id", userId) } }
                        .decodeSingleOrNull<JsonObject>()

                    val result = if (existingReaction != null) {
                        val existingType = existingReaction["reaction_type"]?.jsonPrimitive?.contentOrNull
                        if (existingType == reactionType.name.lowercase()) {

                            client.from(tableName)
                                .delete { filter { eq(idColumn, targetId); eq("user_id", userId) } }
                            Log.d(TAG, "Reaction removed for $targetType $targetId")
                            ReactionToggleResult.REMOVED
                        } else {

                            client.from(tableName)
                                .update({
                                    set("reaction_type", reactionType.name.lowercase())
                                    set("updated_at", java.time.Instant.now().toString())
                                }) { filter { eq(idColumn, targetId); eq("user_id", userId) } }
                            Log.d(TAG, "Reaction updated to ${reactionType.name} for $targetType $targetId")
                            ReactionToggleResult.UPDATED
                        }
                    } else {

                        client.from(tableName).insert(buildJsonObject {
                            put("user_id", userId)
                            put(idColumn, targetId)
                            put("reaction_type", reactionType.name.lowercase())
                        })
                        Log.d(TAG, "New reaction ${reactionType.name} added for $targetType $targetId")
                        ReactionToggleResult.ADDED
                    }

                    return@withContext Result.success(result)
                } catch (e: Exception) {
                    lastException = e
                    val isRLSError = e.message?.contains("policy", true) == true
                    if (isRLSError || attempt == MAX_RETRIES - 1) throw e
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
            Result.failure(Exception(mapSupabaseError(lastException ?: Exception("Unknown error"))))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle reaction: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }



    suspend fun getReactionSummary(
        targetId: String,
        targetType: String
    ): Result<Map<ReactionType, Int>> = withContext(Dispatchers.IO) {
        try {
            // Optimized RPC calls for supported types
            if (targetType.equals("post", ignoreCase = true)) {
                try {
                    val summaryList = client.postgrest.rpc(
                        "get_posts_reactions_summary",
                        buildJsonObject { put("post_ids", buildJsonArray { add(JsonPrimitive(targetId)) }) }
                    ).decodeList<PostReactionSummary>()

                    val summary = summaryList.firstOrNull()?.reactionCounts?.entries
                        ?.associate { ReactionType.fromString(it.key) to it.value }
                        ?: emptyMap()
                    return@withContext Result.success(summary)
                } catch (e: Exception) {
                    Log.e("ReactionRepository", "RPC call failed, falling back to direct query", e)
                    // Fall through to fallback query
                }
            } else if (targetType.equals("comment", ignoreCase = true)) {
                try {
                    val summaryList = client.postgrest.rpc(
                        "get_comments_reactions_summary",
                        buildJsonObject { put("comment_ids", buildJsonArray { add(JsonPrimitive(targetId)) }) }
                    ).decodeList<CommentReactionSummary>()

                    val summary = summaryList.firstOrNull()?.reactionCounts?.entries
                        ?.associate { ReactionType.fromString(it.key) to it.value }
                        ?: emptyMap()
                    return@withContext Result.success(summary)
                } catch (e: Exception) {
                    Log.e("ReactionRepository", "RPC call failed, falling back to direct query", e)
                    // Fall through to fallback query
                }
            }

            // Fallback for other types or if RPC fails
            val tableName = getTableName(targetType)
            val idColumn = getIdColumn(targetType)

            val reactions = client.from(tableName)
                .select { filter { eq(idColumn, targetId) } }
                .decodeList<JsonObject>()

            val summary = reactions
                .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                .mapValues { it.value.size }

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }



    suspend fun getUserReaction(
        targetId: String,
        targetType: String
    ): Result<ReactionType?> = withContext(Dispatchers.IO) {
        try {
            val currentUser = client.auth.currentUserOrNull() ?: return@withContext Result.success(null)
            val userId = currentUser.id
            val tableName = getTableName(targetType)
            val idColumn = getIdColumn(targetType)

            val reaction = client.from(tableName)
                .select { filter { eq(idColumn, targetId); eq("user_id", userId) } }
                .decodeSingleOrNull<JsonObject>()

            val reactionType = reaction?.get("reaction_type")?.jsonPrimitive?.contentOrNull?.let {
                ReactionType.fromString(it)
            }
            Result.success(reactionType)
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }



    @Serializable
    internal data class PostReactionSummary(
        @SerialName("post_id") val postId: String,
        @SerialName("reaction_counts") val reactionCounts: Map<String, Int> = emptyMap(),
        @SerialName("user_reaction") val userReaction: String? = null
    )

    @Serializable
    internal data class CommentReactionSummary(
        @SerialName("comment_id") val commentId: String,
        @SerialName("reaction_counts") val reactionCounts: Map<String, Int> = emptyMap(),
        @SerialName("user_reaction") val userReaction: String? = null
    )

    internal fun applyReactionSummaries(
        posts: List<com.synapse.social.studioasinc.domain.model.Post>,
        summaries: List<PostReactionSummary>
    ): List<com.synapse.social.studioasinc.domain.model.Post> {
        val summariesByPost = summaries.associateBy { it.postId }

        return posts.map { post ->
            val summaryData = summariesByPost[post.id]

            val summary = summaryData?.reactionCounts?.entries
                ?.groupingBy { ReactionType.fromString(it.key) }
                ?.fold(0) { acc, entry -> acc + entry.value }
                ?: emptyMap()

            val userReactionType = summaryData?.userReaction?.let { ReactionType.fromString(it) }

            post.copy(
                reactions = summary,
                userReaction = userReactionType,
                likesCount = summary.values.sum()
            )
        }
    }

    internal fun applyCommentReactionSummaries(
        comments: List<CommentWithUser>,
        summaries: List<CommentReactionSummary>
    ): List<CommentWithUser> {
        val summariesByComment = summaries.associateBy { it.commentId }

        return comments.map { comment ->
            val summaryData = summariesByComment[comment.id]

            val summary = summaryData?.reactionCounts?.entries
                ?.groupingBy { ReactionType.fromString(it.key) }
                ?.fold(0) { acc, entry -> acc + entry.value }
                ?: emptyMap()

            val userReactionType = summaryData?.userReaction?.let { ReactionType.fromString(it) }

            comment.copy(
                reactionSummary = summary,
                userReaction = userReactionType,
                likesCount = summary.values.sum()
            )
        }
    }

    suspend fun populatePostReactions(posts: List<com.synapse.social.studioasinc.domain.model.Post>): List<com.synapse.social.studioasinc.domain.model.Post> = withContext(Dispatchers.IO) {
        if (posts.isEmpty()) return@withContext posts

        try {
            val allPostIds = posts.map { it.id }
            val semaphore = Semaphore(5)

            val summaries = supervisorScope {
                 allPostIds.chunked(20).map { chunkIds ->
                     async {
                         semaphore.withPermit {
                             try {
                                 val rpcSummaries = client.postgrest.rpc(
                                    "get_posts_reactions_summary",
                                    buildJsonObject { 
                                        put("post_ids", buildJsonArray { 
                                            chunkIds.forEach { add(JsonPrimitive(it)) }
                                        }) 
                                    }
                                 ).decodeList<PostReactionSummary>()

                                 val currentUser = client.auth.currentUserOrNull()
                                 if (currentUser != null) {
                                     val userReactions = client.from("reactions")
                                         .select(io.github.jan.supabase.postgrest.query.Columns.raw("post_id, reaction_type")) {
                                             filter {
                                                 isIn("post_id", chunkIds)
                                                 eq("user_id", currentUser.id)
                                             }
                                         }.decodeList<JsonObject>()

                                     val userReactionMap = userReactions.associate {
                                         it["post_id"]?.jsonPrimitive?.contentOrNull to it["reaction_type"]?.jsonPrimitive?.contentOrNull
                                     }

                                     rpcSummaries.map { summary ->
                                         val userReaction = userReactionMap[summary.postId]
                                         if (userReaction != null) {
                                             summary.copy(userReaction = userReaction)
                                         } else {
                                             summary
                                         }
                                     }
                                 } else {
                                     rpcSummaries
                                 }
                             } catch (e: Exception) {
                                 Log.e(TAG, "Failed to fetch reaction summaries for chunk via RPC, falling back", e)
                                 // Fallback: query reactions table directly
                                 val reactions = client.from("reactions")
                                     .select {
                                         filter {
                                             isIn("post_id", chunkIds)
                                         }
                                     }
                                     .decodeList<JsonObject>()

                                 val currentUser = client.auth.currentUserOrNull()
                                 val reactionsByPostId = reactions.groupBy { it["post_id"]?.jsonPrimitive?.contentOrNull }

                                 chunkIds.map { postId ->
                                     val postReactions = reactionsByPostId[postId] ?: emptyList()
                                     val summary = postReactions
                                         .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                                         .mapValues { it.value.size }
                                         .mapKeys { it.key.name.lowercase() }

                                     val userReaction = currentUser?.let { user ->
                                         postReactions.firstOrNull { it["user_id"]?.jsonPrimitive?.contentOrNull == user.id }
                                             ?.get("reaction_type")?.jsonPrimitive?.contentOrNull
                                     }

                                     PostReactionSummary(
                                         postId = postId,
                                         reactionCounts = summary,
                                         userReaction = userReaction
                                     )
                                 }
                             }
                         }
                     }
                 }.awaitAll().flatten()
            }

            applyReactionSummaries(posts, summaries)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to populate reactions", e)
            posts
        }
    }

    suspend fun populateCommentReactions(comments: List<CommentWithUser>): List<CommentWithUser> = withContext(Dispatchers.IO) {
        if (comments.isEmpty()) return@withContext comments

        try {
            val allCommentIds = comments.map { it.id }
            val semaphore = Semaphore(5)

            val summaries = supervisorScope {
                 allCommentIds.chunked(20).map { chunkIds ->
                     async {
                         semaphore.withPermit {
                             try {
                                 val rpcSummaries = client.postgrest.rpc(
                                    "get_comments_reactions_summary",
                                    buildJsonObject { 
                                        put("comment_ids", buildJsonArray { 
                                            chunkIds.forEach { add(JsonPrimitive(it)) }
                                        }) 
                                    }
                                 ).decodeList<CommentReactionSummary>()

                                 val currentUser = client.auth.currentUserOrNull()
                                 if (currentUser != null) {
                                     val userReactions = client.from("comment_reactions")
                                         .select(io.github.jan.supabase.postgrest.query.Columns.raw("comment_id, reaction_type")) {
                                             filter {
                                                 isIn("comment_id", chunkIds)
                                                 eq("user_id", currentUser.id)
                                             }
                                         }.decodeList<JsonObject>()

                                     val userReactionMap = userReactions.associate {
                                         it["comment_id"]?.jsonPrimitive?.contentOrNull to it["reaction_type"]?.jsonPrimitive?.contentOrNull
                                     }

                                     rpcSummaries.map { summary ->
                                         val userReaction = userReactionMap[summary.commentId]
                                         if (userReaction != null) {
                                             summary.copy(userReaction = userReaction)
                                         } else {
                                             summary
                                         }
                                     }
                                 } else {
                                     rpcSummaries
                                 }
                             } catch(e: Exception) {
                                 Log.e(TAG, "Failed to fetch reaction summaries for comment chunk via RPC, falling back", e)
                                 val reactions = client.from("comment_reactions")
                                     .select { filter { isIn("comment_id", chunkIds) } }
                                     .decodeList<JsonObject>()

                                 val currentUser = client.auth.currentUserOrNull()

                                 val reactionsByCommentId = reactions.groupBy { it["comment_id"]?.jsonPrimitive?.contentOrNull }

                                 chunkIds.map { commentId ->
                                     val commentReactions = reactionsByCommentId[commentId] ?: emptyList()
                                     val summary = commentReactions
                                         .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                                         .mapValues { it.value.size }
                                         .mapKeys { it.key.name.lowercase() }

                                     val userReaction = currentUser?.let { user ->
                                         commentReactions.firstOrNull { it["user_id"]?.jsonPrimitive?.contentOrNull == user.id }
                                             ?.get("reaction_type")?.jsonPrimitive?.contentOrNull
                                     }

                                     CommentReactionSummary(
                                         commentId = commentId,
                                         reactionCounts = summary,
                                         userReaction = userReaction
                                     )
                                 }
                             }
                         }
                     }
                 }.awaitAll().flatten()
            }

            applyCommentReactionSummaries(comments, summaries)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to populate comment reactions", e)
            comments
        }
    }



    suspend fun togglePostReaction(postId: String, reactionType: ReactionType, oldReaction: ReactionType? = null, skipCheck: Boolean = false) = toggleReaction(postId, "post", reactionType, oldReaction, skipCheck)

    suspend fun toggleCommentReaction(commentId: String, reactionType: ReactionType, oldReaction: ReactionType? = null, skipCheck: Boolean = false) = toggleReaction(commentId, "comment", reactionType, oldReaction, skipCheck)

    suspend fun getPostReactionSummary(postId: String) =
        getReactionSummary(postId, "post")

    suspend fun getCommentReactionSummary(commentId: String) =
        getReactionSummary(commentId, "comment")

    suspend fun getUserPostReaction(postId: String) =
        getUserReaction(postId, "post")

    suspend fun getUserCommentReaction(commentId: String) =
        getUserReaction(commentId, "comment")




    private fun getTableName(targetType: String): String {
        return when (targetType.lowercase()) {
            "post" -> "reactions"
            "comment" -> "comment_reactions"
            else -> "reactions"
        }
    }

    private fun getIdColumn(targetType: String): String {
        return when (targetType.lowercase()) {
            "post" -> "post_id"
            "comment" -> "comment_id"
            else -> "post_id"
        }
    }





    private fun mapSupabaseError(exception: Exception): String {
        val message = exception.message ?: "Unknown error"

        Log.e(TAG, "Supabase error: $message", exception)

        return when {
            message.contains("PGRST200") -> "Database table not found"
            message.contains("PGRST100") -> "Database column does not exist"
            message.contains("PGRST116") -> "Record not found"
            message.contains("relation", ignoreCase = true) -> "Database table does not exist"
            message.contains("column", ignoreCase = true) -> "Database column mismatch"
            message.contains("policy", ignoreCase = true) || message.contains("rls", ignoreCase = true) ->
                "Permission denied"
            message.contains("connection", ignoreCase = true) || message.contains("network", ignoreCase = true) ->
                "Connection failed. Please check your internet connection."
            message.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
            message.contains("unauthorized", ignoreCase = true) -> "Permission denied."
            message.contains("54001") -> "Server Configuration Error: Stack depth limit exceeded. Please contact support."
            else -> "Failed to process reaction: $message"
        }
    }





    fun determineToggleResult(
        existingReactionType: ReactionType?,
        newReactionType: ReactionType
    ): ReactionToggleResult {
        return when {
            existingReactionType == null -> ReactionToggleResult.ADDED
            existingReactionType == newReactionType -> ReactionToggleResult.REMOVED
            else -> ReactionToggleResult.UPDATED
        }
    }



    fun calculateReactionSummary(reactions: List<ReactionType>): Map<ReactionType, Int> {
        return reactions.groupingBy { it }.eachCount()
    }



    fun isReactionSummaryAccurate(summary: Map<ReactionType, Int>, totalReactions: Int): Boolean {
        return summary.values.sum() == totalReactions
    }
}



enum class ReactionToggleResult {

    ADDED,

    REMOVED,

    UPDATED
}
