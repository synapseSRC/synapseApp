package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.dto.CommentDto
import com.synapse.social.studioasinc.shared.data.mapper.CommentMapper
import com.synapse.social.studioasinc.shared.domain.model.Comment
import com.synapse.social.studioasinc.shared.domain.repository.CommentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Supabase-backed implementation of [CommentRepository].
 * Manages comment persistence, retrieval, and soft-deletion using Supabase Postgrest.
 *
 * @property supabaseClient The initialized Supabase client for database operations.
 */
class SupabaseCommentRepository(
    private val supabaseClient: SupabaseClient
) : CommentRepository {

    /**
     * Fetches a list of comments for a specific post.
     *
     * @param postId The ID of the post to fetch comments for.
     * @param parentId If provided, fetches replies to this comment. If null, fetches top-level comments.
     * @return A [Result] containing a list of [Comment] objects.
     */
    override suspend fun getComments(postId: String, parentId: String?): Result<List<Comment>> = withContext(AppDispatchers.IO) {
        runCatching {
            val response = supabaseClient.from("comments").select(
                // Joins with the users table to retrieve author profile details in a single query
                columns = Columns.raw("*, author:users(username, avatar)")
            ) {
                filter {
                    eq("post_id", postId)
                    if (parentId == null) {
                        // Filters for comments without a parent to get top-level discussions
                        filter("parent_id", FilterOperator.IS, "null")
                    } else {
                        eq("parent_id", parentId)
                    }
                }
                order("created_at", Order.ASCENDING)
            }.decodeList<CommentDto>()
            CommentMapper.toDomainList(response)
        }
    }

    /**
     * Adds a new comment or reply to a post.
     *
     * @param postId The ID of the post being commented on.
     * @param content The text content of the comment.
     * @param parentId The ID of the parent comment if this is a reply, otherwise null.
     * @param mediaUrl Optional URL for media attached to the comment.
     * @return A [Result] containing the newly created [Comment].
     */
    override suspend fun addComment(postId: String, content: String, parentId: String?, mediaUrl: String?): Result<Comment> = withContext(AppDispatchers.IO) {
        runCatching {
            val userId = supabaseClient.auth.currentUserOrNull()?.id ?: throw Exception("User not authenticated")
            val commentDto = supabaseClient.from("comments").insert(
                mapOf(
                    "post_id" to postId,
                    "author_id" to userId,
                    "parent_id" to parentId,
                    "content" to content,
                    "media_url" to mediaUrl
                )
            ) {
                // Selects the inserted row along with author details for immediate UI update
                select(columns = Columns.raw("*, author:users(username, avatar)"))
            }.decodeSingle<CommentDto>()
            CommentMapper.toDomain(commentDto)
        }
    }

    /**
     * Performs a soft delete on a comment by marking it as deleted and recording the timestamp.
     *
     * @param commentId The ID of the comment to delete.
     * @return A [Result] indicating success or failure.
     */
    override suspend fun deleteComment(commentId: String): Result<Unit> = withContext(AppDispatchers.IO) {
        runCatching {
            // Soft delete preserves the comment record for audit/threaded context while hiding it from standard views
            supabaseClient.from("comments").update(
                mapOf(
                    "is_deleted" to true,
                    "deleted_at" to Clock.System.now().toString()
                )
            ) {
                filter { eq("id", commentId) }
            }
            Unit
        }
    }
}
