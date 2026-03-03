package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SearchPost(
    @SerialName("id") val id: String,
    @SerialName("post_text") val content: String?,
    @SerialName("author_uid") val authorId: String,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("reshares_count") val boostCount: Int = 0,
    @SerialName("created_at") val createdAt: String,


    val authorName: String? = null,
    val authorHandle: String? = null,
    val authorAvatar: String? = null
)

@Serializable
data class SearchHashtag(
    @SerialName("id") val id: String,
    @SerialName("tag") val tag: String,
    @SerialName("usage_count") val count: Int,
    val sparklinePoints: List<Float> = emptyList()
)

@Serializable
data class SearchNews(
    @SerialName("id") val id: String,
    @SerialName("source_name") val source: String,
    @SerialName("headline") val headline: String,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("published_at") val publishedAt: String,
    @SerialName("url") val url: String?
)

@Serializable
data class SearchAccount(
    @SerialName("uid") val id: String,
    @SerialName("display_name") val displayName: String?,
    @SerialName("username") val handle: String?,
    @SerialName("followers_count") val followersCount: Int = 0,
    @SerialName("verify") val isVerified: Boolean = false,
    @SerialName("avatar") val avatarUrl: String? = null,
    @SerialName("bio") val bio: String? = null,
    val isFollowing: Boolean = false
)
