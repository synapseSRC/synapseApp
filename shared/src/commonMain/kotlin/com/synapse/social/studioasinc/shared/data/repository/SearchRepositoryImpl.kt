package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.core.util.sanitizeSearchQuery
import com.synapse.social.studioasinc.shared.domain.model.SearchAccount
import com.synapse.social.studioasinc.shared.domain.model.SearchHashtag
import com.synapse.social.studioasinc.shared.domain.model.SearchNews
import com.synapse.social.studioasinc.shared.domain.model.SearchPost
import com.synapse.social.studioasinc.shared.domain.model.MediaItem
import com.synapse.social.studioasinc.shared.domain.repository.ISearchRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.postgrest.query.filter.TextSearchType
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib

@Serializable
private data class PostDto(
    val id: String,
    val post_text: String? = null,
    val author_uid: String,
    val likes_count: Int = 0,
    val comments_count: Int = 0,
    val reshares_count: Int = 0,
    val created_at: String,
    val media_items: List<MediaItem>? = null,
    val author: AuthorDto? = null
)

@Serializable
private data class AuthorDto(
    val display_name: String? = null,
    val username: String? = null,
    val avatar: String? = null
)

/**
 * Concrete implementation of [ISearchRepository] that interfaces with Supabase Postgrest
 * to provide search capabilities across posts, hashtags, news, and users.
 *
 * This implementation focuses on remote-first data retrieval with basic sanitization
 * to prevent query injection while maintaining flexibility for partial matches.
 */
@Serializable
private data class TrendingHashtagDto(
    val id: String,
    val tag: String,
    val trending_usage_count: Int
)

class SearchRepositoryImpl(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClient.client
) : ISearchRepository {

    /**
     * Searches for posts containing the given query string.
     * Uses [ilike] for case-insensitive partial matching on the 'post_text' column.
     *
     * Result set includes joined author data from the 'users' table to minimize round-trips
     * when displaying feed items.
     */
    override suspend fun searchPosts(query: String): Result<List<SearchPost>> = runCatching {

        val columns = Columns.raw("id, post_text, author_uid, likes_count, comments_count, reshares_count, created_at, media_items, author:users!posts_author_uid_fkey(display_name, username, avatar)")

        // For Full Text Search, we do not escape % and _ as they are not wildcards in FTS.
        // We trim and limit to prevent excessively long query strings that might strain the backend.
        val sanitizedQuery = query.trim().take(100)

        val result = client.postgrest["posts"].select(columns = columns) {
            if (sanitizedQuery.isNotBlank()) {
                filter {
                    ilike("post_text", "%$sanitizedQuery%")
                }
            }
            order("created_at", Order.DESCENDING)
            limit(20)
        }.decodeList<PostDto>()

        result.map { dto ->
            SearchPost(
                id = dto.id,
                content = dto.post_text,
                authorId = dto.author_uid,
                likesCount = dto.likes_count,
                commentsCount = dto.comments_count,
                boostCount = dto.reshares_count,
                createdAt = dto.created_at,
                mediaUrls = dto.media_items?.map { item ->
                    SupabaseClient.constructMediaUrl(item.url)
                },
                authorName = dto.author?.display_name,
                authorHandle = dto.author?.username,
                authorAvatar = dto.author?.avatar?.let { path ->
                    SupabaseClient.constructStorageUrl(SupabaseClient.BUCKET_USER_AVATARS, path)
                }
            )
        }
    }

    /**
     * Finds hashtags starting with the query string.
     * Matches are prioritized by 'usage_count' to surface the most relevant/popular tags first.
     */
    override suspend fun searchHashtags(query: String): Result<List<SearchHashtag>> = runCatching {
        val sanitizedQuery = sanitizeSearchQuery(query)

        client.postgrest["hashtags"].select {
            if (sanitizedQuery.isNotBlank()) {
                filter {
                    ilike("tag", "$sanitizedQuery%")
                }
            }
            order("usage_count", Order.DESCENDING)
            limit(20)
        }.decodeList<SearchHashtag>()
    }

    /**
     * Retrieves the top trending hashtags based purely on usage in the last 24 hours.
     */
    override suspend fun getTrendingHashtags(): Result<List<SearchHashtag>> = runCatching {
        val response = client.postgrest.rpc("get_trending_hashtags", buildJsonObject {
            put("limit_count", 10)
        })

        response.decodeList<TrendingHashtagDto>().map { dto ->
            SearchHashtag(
                id = dto.id,
                tag = dto.tag,
                count = dto.trending_usage_count
            )
        }
    }

    /**
     * Searches news articles by headline.
     * Results are ordered by publication date to ensure the most recent news is shown first.
     */
    override suspend fun searchNews(query: String): Result<List<SearchNews>> = runCatching {
        val sanitizedQuery = sanitizeSearchQuery(query)

        client.postgrest["news_articles"].select {
            if (sanitizedQuery.isNotBlank()) {
                filter {
                    ilike("headline", "%$sanitizedQuery%")
                }
            }
            order("published_at", Order.DESCENDING)
            limit(20)
        }.decodeList()
    }

    /**
     * Searches for users based on username, display name, or bio.
     * If no query is provided, it falls back to suggesting accounts with the highest follower counts.
     */
    override suspend fun getSuggestedAccounts(query: String): Result<List<SearchAccount>> = runCatching {
        val sanitizedQuery = sanitizeSearchQuery(query)

        client.postgrest["users"].select {
            if (sanitizedQuery.isNotBlank()) {
                filter {
                    or {
                        ilike("username", "%$sanitizedQuery%")
                        ilike("display_name", "%$sanitizedQuery%")
                        ilike("bio", "%$sanitizedQuery%")
                    }
                }
            } else {
                 // Fallback for discovery mode when the search field is empty
                 order("followers_count", Order.DESCENDING)
            }
            limit(20)
        }.decodeList()
    }
}
