package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.shared.data.local.entity.PostEntity
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.LinkPreview as AppLinkPreview
import com.synapse.social.studioasinc.shared.domain.model.LinkPreview as SharedLinkPreview
import com.synapse.social.studioasinc.domain.model.MediaItem as AppMediaItem
import com.synapse.social.studioasinc.shared.domain.model.MediaItem as SharedMediaItem
import com.synapse.social.studioasinc.domain.model.PollOption as AppPollOption
import com.synapse.social.studioasinc.shared.domain.model.PollOption as SharedPollOption
import com.synapse.social.studioasinc.domain.model.PostMetadata as AppPostMetadata
import com.synapse.social.studioasinc.shared.domain.model.PostMetadata as SharedPostMetadata
import com.synapse.social.studioasinc.domain.model.ReactionType as AppReactionType
import com.synapse.social.studioasinc.shared.domain.model.ReactionType as SharedReactionType
import com.synapse.social.studioasinc.domain.model.MediaType as AppMediaType
import com.synapse.social.studioasinc.shared.domain.model.MediaType as SharedMediaType

object PostMapper {
    fun toEntity(post: Post): PostEntity {
        return PostEntity(
            id = post.id,
            key = post.key,
            authorUid = post.authorUid,
            postText = post.postText,
            postImage = post.postImage,
            postType = post.postType,
            postHideViewsCount = post.postHideViewsCount,
            postHideLikeCount = post.postHideLikeCount,
            postHideCommentsCount = post.postHideReplyCount,
            postDisableComments = post.postDisableReplies,
            postVisibility = post.postVisibility,
            publishDate = post.publishDate,
            createdAt = post.createdAt,
            timestamp = post.timestamp,
            likesCount = post.likesCount,
            commentsCount = post.replyCount,
            viewsCount = post.viewsCount,
            resharesCount = post.resharesCount,
            mediaItems = post.mediaItems?.map { toSharedMediaItem(it) },
            isEncrypted = post.isEncrypted,
            nonce = post.nonce,
            encryptionKeyId = post.encryptionKeyId,
            encryptedContent = post.encryptedContent,
            isDeleted = post.isDeleted,
            isEdited = post.isEdited,
            editedAt = post.editedAt,
            deletedAt = post.deletedAt,
            hasPoll = post.hasPoll,
            pollQuestion = post.pollQuestion,
            pollOptions = post.pollOptions?.map { toSharedPollOption(it) },
            pollEndTime = post.pollEndTime,
            pollAllowMultiple = post.pollAllowMultiple,
            hasLocation = post.hasLocation,
            locationName = post.locationName,
            locationAddress = post.locationAddress,
            locationLatitude = post.locationLatitude,
            locationLongitude = post.locationLongitude,
            locationPlaceId = post.locationPlaceId,
            youtubeUrl = post.youtubeUrl,
            linkPreviews = post.linkPreviews?.map { toSharedLinkPreview(it) },
            reactions = post.reactions?.mapKeys { toSharedReactionType(it.key) },
            userReaction = post.userReaction?.let { toSharedReactionType(it) },
            username = post.username,
            displayName = post.displayName,
            avatarUrl = post.avatarUrl,
            isVerified = post.isVerified,
            userPollVote = post.userPollVote,
            metadata = post.metadata?.let { toSharedPostMetadata(it) },
            quotedPostId = post.quotedPostId,
            isQuote = post.isQuote,
            rootPostId = post.rootPostId
        )
    }

    fun toModel(entity: PostEntity): Post {
        return Post(
            id = entity.id,
            key = entity.key,
            authorUid = entity.authorUid,
            postText = entity.postText,
            postImage = entity.postImage,
            postType = entity.postType,
            postHideViewsCount = entity.postHideViewsCount,
            postHideLikeCount = entity.postHideLikeCount,
            postHideReplyCount = entity.postHideCommentsCount,
            postDisableReplies = entity.postDisableComments,
            postVisibility = entity.postVisibility,
            publishDate = entity.publishDate,
            timestamp = entity.timestamp,
            createdAt = entity.createdAt,
            updatedAt = null,
            likesCount = entity.likesCount,
            replyCount = entity.commentsCount,
            viewsCount = entity.viewsCount,
            resharesCount = entity.resharesCount,
            mediaItems = entity.mediaItems?.map { toAppMediaItem(it) }?.toMutableList(),
            isEncrypted = entity.isEncrypted,
            encryptedContent = null,
            nonce = entity.nonce,
            encryptionKeyId = entity.encryptionKeyId,
            isDeleted = entity.isDeleted,
            isEdited = entity.isEdited,
            editedAt = entity.editedAt,
            deletedAt = entity.deletedAt,
            hasPoll = entity.hasPoll,
            pollQuestion = entity.pollQuestion,
            pollOptions = entity.pollOptions?.map { toAppPollOption(it) },
            pollEndTime = entity.pollEndTime,
            pollAllowMultiple = entity.pollAllowMultiple,
            hasLocation = entity.hasLocation,
            locationName = entity.locationName,
            locationAddress = entity.locationAddress,
            locationLatitude = entity.locationLatitude,
            locationLongitude = entity.locationLongitude,
            locationPlaceId = entity.locationPlaceId,
            youtubeUrl = entity.youtubeUrl,
            linkPreviews = entity.linkPreviews?.map { toAppLinkPreview(it) },
            reactions = entity.reactions?.mapKeys { toAppReactionType(it.key) },
            userReaction = entity.userReaction?.let { toAppReactionType(it) },
            username = entity.username,
            displayName = entity.displayName,
            avatarUrl = entity.avatarUrl,
            isVerified = entity.isVerified,
            userPollVote = entity.userPollVote,
            metadata = entity.metadata?.let { toAppPostMetadata(it) },
            quotedPostId = entity.quotedPostId,
            isQuote = entity.isQuote,
            rootPostId = entity.rootPostId
        )
    }

    private fun toSharedLinkPreview(item: AppLinkPreview): SharedLinkPreview {
        return SharedLinkPreview(
            url = item.url,
            title = item.title,
            description = item.description,
            imageUrl = item.imageUrl,
            siteName = item.siteName,
            domain = item.domain
        )
    }

    private fun toAppLinkPreview(item: SharedLinkPreview): AppLinkPreview {
        return AppLinkPreview(
            url = item.url,
            title = item.title,
            description = item.description,
            imageUrl = item.imageUrl,
            siteName = item.siteName,
            domain = item.domain
        )
    }

    private fun toSharedMediaItem(item: AppMediaItem): SharedMediaItem {
        return SharedMediaItem(
            id = item.id,
            url = item.url,
            type = SharedMediaType.valueOf(item.type.name),
            thumbnailUrl = item.thumbnailUrl
        )
    }

    private fun toAppMediaItem(item: SharedMediaItem): AppMediaItem {
        return AppMediaItem(
            id = item.id,
            url = item.url,
            type = AppMediaType.valueOf(item.type.name),
            thumbnailUrl = item.thumbnailUrl
        )
    }

    private fun toSharedPollOption(item: AppPollOption): SharedPollOption {
        return SharedPollOption(
            text = item.text,
            votes = item.votes
        )
    }

    private fun toAppPollOption(item: SharedPollOption): AppPollOption {
        return AppPollOption(
            text = item.text,
            votes = item.votes
        )
    }

    private fun toSharedReactionType(type: AppReactionType): SharedReactionType {
        return try {
            SharedReactionType.valueOf(type.name)
        } catch (e: Exception) {
            SharedReactionType.LIKE
        }
    }

    private fun toAppReactionType(type: SharedReactionType): AppReactionType {
        return try {
            AppReactionType.valueOf(type.name)
        } catch (e: Exception) {
            AppReactionType.LIKE
        }
    }

    private fun toSharedPostMetadata(item: AppPostMetadata): SharedPostMetadata {
        return SharedPostMetadata(
            layoutType = item.layoutType,
            backgroundColor = item.backgroundColor
        )
    }

    private fun toAppPostMetadata(item: SharedPostMetadata): AppPostMetadata {
        return AppPostMetadata(
            layoutType = item.layoutType,
            backgroundColor = item.backgroundColor
        )
    }
}
