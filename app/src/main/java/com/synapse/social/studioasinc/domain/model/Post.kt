package com.synapse.social.studioasinc.domain.model

import com.synapse.social.studioasinc.core.util.json
import com.synapse.social.studioasinc.core.util.toJsonElement
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class PostMetadata(
    @SerialName("layout_type") val layoutType: String? = null,
    @SerialName("tagged_people") val taggedPeople: List<User>? = null,
    @SerialName("feeling") val feeling: FeelingActivity? = null,
    @SerialName("background_color") val backgroundColor: Long? = null
)

@Serializable(with = FeelingTypeSerializer::class)
enum class FeelingType {
    MOOD,
    ACTIVITY
}

object FeelingTypeSerializer : KSerializer<FeelingType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FeelingType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: FeelingType) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): FeelingType {
        return try {
            FeelingType.valueOf(decoder.decodeString())
        } catch (e: IllegalArgumentException) {
            FeelingType.MOOD
        } catch (e: SerializationException) {
            FeelingType.MOOD
        }
    }
}

@Serializable
data class FeelingActivity(
    val emoji: String,
    val text: String,
    val type: FeelingType = FeelingType.MOOD
)



@Serializable
data class Post(
    val id: String = "",
    val key: String? = null,
    @SerialName("author_uid")
    val authorUid: String = "",
    @SerialName("post_text")
    val postText: String? = null,
    @SerialName("post_image")
    var postImage: String? = null,
    @SerialName("post_type")
    var postType: String? = null,
    @SerialName("post_hide_views_count")
    val postHideViewsCount: String? = null,
    @SerialName("post_hide_like_count")
    val postHideLikeCount: String? = null,
    @SerialName("post_hide_comments_count")
    val postHideReplyCount: String? = null,
    @SerialName("post_disable_comments")
    val postDisableReplies: String? = null,
    @SerialName("post_visibility")
    val postVisibility: String? = null,
    @SerialName("publish_date")
    val publishDate: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("likes_count")
    val likesCount: Int = 0,
    @SerialName("comments_count")
    val replyCount: Int = 0,
    @SerialName("views_count")
    val viewsCount: Int = 0,
    @SerialName("reshares_count")
    val resharesCount: Int = 0,

        @SerialName("reply_to_post_id")
    val inReplyToPostId: String? = null,
    @SerialName("root_post_id")
    val rootPostId: String? = null,
    @Transient
    val inReplyToPost: Post? = null,

    @SerialName("quoted_post_id")
    val quotedPostId: String? = null,
    @Transient
    val quotedPost: Post? = null,
    @SerialName("is_quote")
    val isQuote: Boolean = false,

    @Transient
    val captionLikesCount: Int = 0,
    @Transient
    val captionReplyCount: Int = 0,
    @Transient
    val userHasLikedCaption: Boolean = false,

    @SerialName("media_items")
    var mediaItems: MutableList<MediaItem>? = null,

    @SerialName("is_encrypted")
    val isEncrypted: Boolean? = null,
    @SerialName("encrypted_content")
    @Transient
    val encryptedContent: Map<String, String>? = null,
    val nonce: String? = null,
    @SerialName("encryption_key_id")
    val encryptionKeyId: String? = null,

    @SerialName("is_deleted")
    val isDeleted: Boolean? = null,
    @SerialName("is_edited")
    val isEdited: Boolean? = null,
    @SerialName("edited_at")
    val editedAt: String? = null,
    @SerialName("deleted_at")
    val deletedAt: String? = null,

    @SerialName("has_poll")
    val hasPoll: Boolean? = null,
    @SerialName("poll_question")
    val pollQuestion: String? = null,
    @SerialName("poll_options")
    val pollOptions: List<PollOption>? = null,
    @SerialName("poll_end_time")
    val pollEndTime: String? = null,
    @SerialName("poll_allow_multiple")
    val pollAllowMultiple: Boolean? = null,

    @SerialName("has_location")
    val hasLocation: Boolean? = null,
    @SerialName("location_name")
    val locationName: String? = null,
    @SerialName("location_address")
    val locationAddress: String? = null,
    @SerialName("location_latitude")
    val locationLatitude: Double? = null,
    @SerialName("location_longitude")
    val locationLongitude: Double? = null,
    @SerialName("location_place_id")
    val locationPlaceId: String? = null,

    @SerialName("youtube_url")
    val youtubeUrl: String? = null,

    @SerialName("link_previews")
    val linkPreviews: List<LinkPreview>? = null,

    @Transient
    var reactions: Map<ReactionType, Int>? = null,
    @Transient
    var userReaction: ReactionType? = null,
    @SerialName("author_username")
    var username: String? = null,
    @Transient
    var displayName: String? = null,
    @SerialName("author_avatar_url")
    var avatarUrl: String? = null,
    @SerialName("author_is_verified")
    var isVerified: Boolean = false,
    @Transient
    var userPollVote: Int? = null,
    @Transient
    var latestCommentText: String? = null,
    @Transient
    var latestCommentAuthor: String? = null,
    @Transient
    var isBookmarked: Boolean = false,
    @Transient
    var isReshared: Boolean = false,

    @SerialName("metadata")
    val metadata: PostMetadata? = null
) {
    fun determinePostType() {
        postType = when {
            mediaItems?.any { it.type == MediaType.VIDEO } == true -> "VIDEO"
            mediaItems?.any { it.type == MediaType.IMAGE } == true -> "IMAGE"
            !postText.isNullOrEmpty() -> "TEXT"
            else -> "TEXT"
        }
    }

    fun getTotalReactionsCount(): Int = reactions?.values?.sum() ?: likesCount

    fun getTopReactions(): List<Pair<ReactionType, Int>> =
        reactions?.entries?.sortedByDescending { it.value }?.take(3)?.map { it.key to it.value } ?: emptyList()

    fun hasUserReacted(): Boolean = userReaction != null

    fun getReactionSummary(): String {
        val topReactions = getTopReactions()
        if (topReactions.isEmpty()) return ""
        val emojis = topReactions.take(2).joinToString(" ") { it.first.emoji }
        val total = getTotalReactionsCount()
        return when {
            total == 0 -> ""
            total == 1 -> "$emojis 1 person"
            else -> "$emojis $total"
        }
    }

    fun hasMultipleMedia(): Boolean = (mediaItems?.size ?: 0) > 1

    fun toDetailItems(): List<PostDetailItem> {
        val items = mutableListOf<PostDetailItem>()


        if (!postText.isNullOrEmpty()) {
            items.add(
                PostDetailItem.Caption(
                    postId = id,
                    text = postText,
                    likesCount = captionLikesCount,
                    replyCount = captionReplyCount,
                    userHasLiked = userHasLikedCaption
                )
            )
        }


        mediaItems?.forEach { media ->
            when (media.type) {
                MediaType.IMAGE -> items.add(PostDetailItem.Image(media, id))
                MediaType.VIDEO -> items.add(PostDetailItem.Video(media, id))
            }
        }

        return items
    }
}

fun HashMap<String, Any>.toPost(): Post = json.decodeFromJsonElement(this.toJsonElement())
