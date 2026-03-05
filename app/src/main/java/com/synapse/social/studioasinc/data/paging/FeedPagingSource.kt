package com.synapse.social.studioasinc.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.repository.ReactionRepository
import com.synapse.social.studioasinc.domain.model.FeedItem
import com.synapse.social.studioasinc.domain.model.Post
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

private val json = Json { ignoreUnknownKeys = true }

class FeedPagingSource(
    private val client: io.github.jan.supabase.SupabaseClient
) : PagingSource<Int, FeedItem>() {

    private val reactionRepository = ReactionRepository()
    private val pollRepository = com.synapse.social.studioasinc.data.repository.PollRepository()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FeedItem> {
        val position = params.key ?: 0
        val pageSize = params.loadSize
        return try {
            Log.d("FeedPagingSource", "Loading feed timeline at position: $position, pageSize: $pageSize")

            val timelineResponse = withContext(Dispatchers.IO) {
                client.from("feed_timeline")
                    .select {
                        order("timestamp", order = Order.DESCENDING)
                        range(position.toLong(), (position + pageSize - 1).toLong())
                    }
                    .decodeList<JsonObject>()
            }

            Log.d("FeedPagingSource", "Loaded ${timelineResponse.size} feed items")

            if (timelineResponse.isEmpty()) {
                return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
            }

            val postIds = timelineResponse.filter { it["item_type"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull == "post" }.mapNotNull { it["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull }
            val commentIds = timelineResponse.filter { it["item_type"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull == "comment" }.mapNotNull { it["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull }

            // 1. Fetch full Posts
            val postsMap = if (postIds.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    client.from("posts")
                        .select(
                            columns = Columns.raw("""
                                *,
                                users!author_uid(username, display_name, avatar, verify),
                                latest_comments:comments(id, content, user_id, created_at, users(username)),
                                quoted_post:posts!quoted_post_id(*, users!author_uid(username, display_name, avatar, verify))
                            """.trimIndent())
                        ) {
                            filter { isIn("id", postIds) }
                        }
                        .decodeList<JsonObject>()
                }.mapNotNull { jsonElement ->
                    try {
                        val post = json.decodeFromJsonElement<Post>(jsonElement)
                        val userData = jsonElement["users"]?.jsonObject
                        post.username = userData?.get("username")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
                        post.displayName = userData?.get("display_name")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
                        post.avatarUrl = userData?.get("avatar")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull?.let { avatarPath ->
                            SupabaseClient.constructStorageUrl(SupabaseClient.BUCKET_USER_AVATARS, avatarPath)
                        }
                        post.isVerified = userData?.get("verify")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: false

                        val commentsArray = jsonElement["latest_comments"]?.jsonArray
                        if (!commentsArray.isNullOrEmpty()) {
                            val latestComment = commentsArray.map { it.jsonObject }
                                .maxByOrNull { it["created_at"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: "" }

                            if (latestComment != null) {
                                post.latestCommentText = latestComment["content"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
                                val commentUser = latestComment["users"]?.jsonObject
                                post.latestCommentAuthor = commentUser?.get("username")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
                            }
                        }
                        post.id to post
                    } catch (e: Exception) {
                        null
                    }
                }.toMap()
            } else emptyMap()

            // Map and enrich posts
            val postsList = postsMap.values.toList()
            val postsWithReactions = reactionRepository.populatePostReactions(postsList)
            val postsWithPolls = populatePostPolls(postsWithReactions)
            val enrichedPostsMap = postsWithPolls.associateBy { it.id }

            // 2. Map Comments from timeline response
            val commentsMap = timelineResponse.filter { it["item_type"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull == "comment" }.mapNotNull { timelineItem ->
                val id = timelineItem["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return@mapNotNull null
                val userId = timelineItem["user_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: ""
                val content = timelineItem["content"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: ""
                val parentPostId = timelineItem["parent_post_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
                val parentCommentId = timelineItem["parent_comment_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
                val parentAuthorUsername = timelineItem["parent_author_username"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
                val createdAt = timelineItem["created_at"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
                val timestamp = timelineItem["timestamp"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.longOrNull ?: 0L
                val likeCount = timelineItem["likes_count"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.intOrNull ?: 0
                val commentCount = timelineItem["comments_count"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.intOrNull ?: 0
                
                // We need to fetch the author details for comments.
                id to FeedItem.CommentItem(
                    id = id,
                    timestamp = timestamp,
                    userId = userId,
                    content = content,
                    parentPostId = parentPostId,
                    parentCommentId = parentCommentId,
                    parentAuthorUsername = parentAuthorUsername,
                    createdAt = createdAt,
                    likeCount = likeCount,
                    commentCount = commentCount
                )
            }.toMap().toMutableMap()
            
            // Fetch users for comments
            val commentUserIds = commentsMap.values.map { it.userId }.distinct()
            if (commentUserIds.isNotEmpty()) {
                val usersResponse = withContext(Dispatchers.IO) {
                    client.from("users").select(Columns.list("uid", "username", "display_name", "avatar", "verify")) {
                        filter { isIn("uid", commentUserIds) }
                    }.decodeList<JsonObject>()
                }
                val userMap = usersResponse.associateBy { it["uid"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: "" }
                commentsMap.keys.forEach { id ->
                    val commentItem = commentsMap[id]!!
                    val user = userMap[commentItem.userId]
                    commentsMap[id] = commentItem.copy(
                        username = user?.get("username")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: "",
                        userFullName = user?.get("display_name")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: user?.get("username")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: "",
                        avatarUrl = user?.get("avatar")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull?.let { avatarPath ->
                            SupabaseClient.constructStorageUrl(SupabaseClient.BUCKET_USER_AVATARS, avatarPath)
                        },
                        isVerified = user?.get("verify")?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.booleanOrNull ?: false
                    )
                }
            }

            // Build final unified list
            val feedItems = timelineResponse.mapNotNull { item ->
                val id = item["id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull ?: return@mapNotNull null
                val type = item["item_type"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it else null }?.contentOrNull
                if (type == "post") {
                    enrichedPostsMap[id]?.let { FeedItem.PostItem(it) }
                } else if (type == "comment") {
                    commentsMap[id]
                } else null
            }

            LoadResult.Page(
                data = feedItems,
                prevKey = if (position == 0) null else (position - pageSize).coerceAtLeast(0),
                nextKey = if (timelineResponse.isEmpty()) null else position + pageSize
            )
        } catch (e: Exception) {
            Log.e("FeedPagingSource", "Error loading feed items", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, FeedItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    private suspend fun populatePostPolls(posts: List<Post>): List<Post> {
        val pollPosts = posts.filter { it.hasPoll == true }
        if (pollPosts.isEmpty()) return posts
        val postIds = pollPosts.map { it.id }
        val userVotesResult = pollRepository.getBatchUserVotes(postIds)
        val userVotes = userVotesResult.getOrNull() ?: emptyMap()
        val pollCountsResult = pollRepository.getBatchPollVotes(postIds)
        val pollCounts = pollCountsResult.getOrNull() ?: emptyMap()
        return posts.map { post ->
            if (post.hasPoll == true) {
                val userVote = userVotes[post.id]
                val counts = pollCounts[post.id] ?: emptyMap()
                val updatedOptions = post.pollOptions?.mapIndexed { index, option ->
                    option.copy(votes = counts[index] ?: 0)
                }
                post.copy(pollOptions = updatedOptions).apply { userPollVote = userVote }
            } else post
        }
    }
}
