package com.synapse.social.studioasinc.feature.inbox.inbox.components

import com.synapse.social.studioasinc.shared.domain.model.chat.ContentStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.chat.AttachmentType

/** Presentation-level message categories used by the shared quoted-message preview. */
internal enum class QuotedMessageKind {
    UNAVAILABLE, DELETED, TEXT, PHOTO, VIDEO, VOICE, AUDIO, MEDIA, FILE, POLL,
    CONTACT, LOCATION, LIVE_LOCATION, GIF, STICKER, LINK, MUSIC, SHARED_POST,
    STORY, EVENT, PRODUCT, PAYMENT, MAP, CODE, CALL, SYSTEM, VIEW_ONCE
}

/** Safe, type-aware display data for quoting a message; URLs are only exposed as image thumbnails. */
internal data class QuotedMessagePreviewModel(
    val kind: QuotedMessageKind,
    val excerpt: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null,
    val fileSizeBytes: Long? = null
)

internal fun quotedMessagePreviewModel(message: Message?): QuotedMessagePreviewModel {
    if (message == null) return QuotedMessagePreviewModel(QuotedMessageKind.UNAVAILABLE)
    if (message.isDeleted || message.contentStatus == ContentStatus.DELETED) {
        return QuotedMessagePreviewModel(QuotedMessageKind.DELETED)
    }

    val content = message.content.trim().takeIf(String::isNotEmpty)
    val duration = when (message.messageType) {
        MessageType.VIDEO -> message.attachments.firstOrNull { it.type == AttachmentType.VIDEO }?.duration
        MessageType.VOICE, MessageType.AUDIO -> message.attachments.firstOrNull { it.type == AttachmentType.AUDIO }?.duration
        MessageType.MEDIA_GROUP -> {
            // A grouped preview shows the combined duration of its timed attachments, never an arbitrary first item.
            message.attachments.mapNotNull { it.duration?.takeIf { seconds -> seconds > 0 } }
                .sum()
                .takeIf { it > 0 }
        }
        else -> null
    }?.takeIf { it > 0 }
    val fileSize = if (message.messageType == MessageType.FILE) {
        message.attachments.firstNotNullOfOrNull { it.size?.takeIf { bytes -> bytes > 0L } }
    } else {
        null
    }

    return when (message.messageType) {
        MessageType.TEXT -> QuotedMessagePreviewModel(QuotedMessageKind.TEXT, excerpt = content)
        MessageType.IMAGE -> QuotedMessagePreviewModel(
            QuotedMessageKind.PHOTO,
            excerpt = content,
            thumbnailUrl = message.mediaUrl?.takeIf(String::isNotBlank)
                ?: message.attachments.firstOrNull { it.type == AttachmentType.IMAGE }?.url?.takeIf(String::isNotBlank)
        )
        MessageType.VIDEO -> QuotedMessagePreviewModel(
            QuotedMessageKind.VIDEO,
            excerpt = content,
            durationSeconds = duration
        )
        MessageType.VOICE -> QuotedMessagePreviewModel(
            QuotedMessageKind.VOICE,
            excerpt = content,
            durationSeconds = duration
        )
        MessageType.AUDIO -> QuotedMessagePreviewModel(
            QuotedMessageKind.AUDIO,
            excerpt = content,
            durationSeconds = duration
        )
        MessageType.MEDIA_GROUP -> QuotedMessagePreviewModel(
            QuotedMessageKind.MEDIA,
            excerpt = content,
            thumbnailUrl = message.attachments.firstOrNull { it.type == AttachmentType.IMAGE }?.url?.takeIf(String::isNotBlank),
            durationSeconds = duration
        )
        MessageType.FILE -> QuotedMessagePreviewModel(
            QuotedMessageKind.FILE,
            excerpt = content?.takeIf(::isSafeFileLabel),
            fileSizeBytes = fileSize
        )
        MessageType.POLL -> QuotedMessagePreviewModel(
            QuotedMessageKind.POLL,
            excerpt = message.metadataContainer?.poll?.question?.trim()?.takeIf(String::isNotEmpty) ?: content
        )
        MessageType.CONTACT -> QuotedMessagePreviewModel(
            QuotedMessageKind.CONTACT,
            excerpt = message.metadataContainer?.contact?.name?.trim()?.takeIf(String::isNotEmpty)
        )
        MessageType.LOCATION -> QuotedMessagePreviewModel(
            QuotedMessageKind.LOCATION,
            excerpt = message.metadataContainer?.location?.let { it.title?.takeIf(String::isNotBlank) ?: it.address?.takeIf(String::isNotBlank) }
        )
        MessageType.LIVE_LOCATION -> QuotedMessagePreviewModel(QuotedMessageKind.LIVE_LOCATION)
        MessageType.GIF -> QuotedMessagePreviewModel(QuotedMessageKind.GIF, excerpt = content)
        MessageType.STICKER -> QuotedMessagePreviewModel(QuotedMessageKind.STICKER, excerpt = content)
        MessageType.LINK_PREVIEW -> QuotedMessagePreviewModel(QuotedMessageKind.LINK, excerpt = content)
        MessageType.MUSIC -> QuotedMessagePreviewModel(
            QuotedMessageKind.MUSIC,
            excerpt = message.metadataContainer?.music?.let { music ->
                listOfNotNull(music.title.takeIf(String::isNotBlank), music.artist.takeIf(String::isNotBlank))
                    .joinToString(" — ").takeIf(String::isNotBlank)
            }
        )
        MessageType.SHARED_POST -> QuotedMessagePreviewModel(
            QuotedMessageKind.SHARED_POST,
            excerpt = message.metadataContainer?.sharedPost?.let { post ->
                post.content?.trim()?.takeIf(String::isNotEmpty) ?: post.authorName.takeIf(String::isNotBlank)
            }
        )
        MessageType.STORY_SHARE -> QuotedMessagePreviewModel(
            QuotedMessageKind.STORY,
            excerpt = message.metadataContainer?.sharedStory?.authorName?.takeIf(String::isNotBlank)
        )
        MessageType.EVENT -> QuotedMessagePreviewModel(
            QuotedMessageKind.EVENT,
            excerpt = message.metadataContainer?.event?.title?.takeIf(String::isNotBlank)
        )
        MessageType.PRODUCT -> QuotedMessagePreviewModel(
            QuotedMessageKind.PRODUCT,
            excerpt = message.metadataContainer?.product?.title?.takeIf(String::isNotBlank)
        )
        MessageType.PAYMENT -> QuotedMessagePreviewModel(
            QuotedMessageKind.PAYMENT,
            excerpt = message.metadataContainer?.payment?.let { payment ->
                payment.note?.takeIf(String::isNotBlank)
                    ?: listOf(payment.amount, payment.currency).filter(String::isNotBlank).joinToString(" ").takeIf(String::isNotBlank)
            }
        )
        MessageType.MAP -> QuotedMessagePreviewModel(
            QuotedMessageKind.MAP,
            excerpt = message.metadataContainer?.map?.locationName?.takeIf(String::isNotBlank)
        )
        MessageType.CODE_SNIPPET -> QuotedMessagePreviewModel(
            QuotedMessageKind.CODE,
            excerpt = message.metadataContainer?.codeSnippet?.let { snippet ->
                snippet.filename?.takeIf(String::isNotBlank) ?: snippet.language.takeIf(String::isNotBlank)
            }
        )
        MessageType.CALL -> QuotedMessagePreviewModel(
            QuotedMessageKind.CALL,
            durationSeconds = message.metadataContainer?.call?.durationSeconds?.takeIf { it > 0 }
        )
        MessageType.SYSTEM -> QuotedMessagePreviewModel(QuotedMessageKind.SYSTEM, excerpt = content)
        MessageType.EPHEMERAL_MEDIA -> QuotedMessagePreviewModel(QuotedMessageKind.VIEW_ONCE)
    }
}

/** Prevent storage paths, URLs, and opaque identifiers from becoming file names in a quote. */
private fun isSafeFileLabel(value: String): Boolean {
    if (value.isBlank() || value.contains('/') || value.contains('\\') || value.contains("://")) return false
    if (value.matches(Regex("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"))) return false
    // A recognizable extension indicates a user-facing filename, not a bare opaque storage ID.
    if (value.matches(Regex("(?i)^[0-9a-f]{24,}$"))) return false
    return true
}

internal fun quotedSenderDisplayName(
    message: Message?,
    currentUserId: String,
    isGroupChat: Boolean,
    groupMemberNames: Map<String, String>,
    participantDisplayName: String?,
    initialParticipantName: String?
): String? {
    if (message == null || (currentUserId.isNotBlank() && message.senderId == currentUserId)) return null
    return if (isGroupChat) {
        groupMemberNames[message.senderId]?.takeIf(String::isNotBlank)
    } else {
        participantDisplayName?.takeIf(String::isNotBlank)
            ?: initialParticipantName?.takeIf(String::isNotBlank)
    }
}

internal fun resolveQuotedSenderName(
    message: Message?,
    currentUserId: String,
    providedName: String?,
    youLabel: String,
    unknownSenderLabel: String,
    unavailableLabel: String
): String = when {
    message == null -> unavailableLabel
    currentUserId.isNotBlank() && message.senderId == currentUserId -> youLabel
    !providedName.isNullOrBlank() -> providedName.trim()
    else -> unknownSenderLabel
}
