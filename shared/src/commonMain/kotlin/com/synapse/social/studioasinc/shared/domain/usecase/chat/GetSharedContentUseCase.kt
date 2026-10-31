package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.ContentType
import com.synapse.social.studioasinc.shared.domain.model.chat.FileItem
import com.synapse.social.studioasinc.shared.domain.model.chat.FileType
import com.synapse.social.studioasinc.shared.domain.model.chat.LinkItem
import com.synapse.social.studioasinc.shared.domain.model.chat.MediaItem
import com.synapse.social.studioasinc.shared.domain.model.chat.MediaType
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageAttachment
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.chat.SharedContent
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.util.UUIDUtils
import io.ktor.http.Url

/**
 * Use case for fetching shared content (media, links, files) from a conversation.
 * Supports pagination and filtering by content type.
 */
class GetSharedContentUseCase(
    private val repository: ChatRepository
) {
    /**
     * Fetch shared content for a chat, optionally filtered by type.
     *
     * @param chatId The conversation ID
     * @param contentType Optional filter for specific content type
     * @param limit Maximum number of messages to fetch (default: 50)
     * @param beforeTimestamp Optional cursor for pagination (ISO timestamp)
     * @param beforeId Optional cursor for pagination (message ID)
     * @return Result containing SharedContent with items and pagination info
     */
    suspend operator fun invoke(
        chatId: String,
        contentType: ContentType? = null,
        limit: Int = 50,
        beforeTimestamp: String? = null,
        beforeId: String? = null
    ): Result<SharedContent> {
        if (chatId.isBlank()) {
            return Result.success(SharedContent())
        }

        return repository.getMessages(
            chatId = chatId,
            limit = limit,
            before = beforeTimestamp,
            beforeId = beforeId,
            forceNetwork = false
        ).map { messages ->
            categorizeMessages(messages, chatId, limit)
        }
    }

    /**
     * Categorize messages into media, links, and files.
     */
    private fun categorizeMessages(
        messages: List<Message>,
        chatId: String,
        limit: Int
    ): SharedContent {
        val mediaItems = mutableListOf<MediaItem>()
        val linkItems = mutableListOf<LinkItem>()
        val fileItems = mutableListOf<FileItem>()

        messages.forEach { message ->
            // Skip deleted and system messages
            if (message.isDeleted || message.messageType == MessageType.SYSTEM) {
                return@forEach
            }

            // Extract media from attachments or mediaUrl
            when {
                isMediaType(message.messageType) -> {
                    val attachments = message.attachments
                    if (attachments.isNotEmpty()) {
                        attachments.forEach { attachment ->
                            mediaItems.add(
                                attachment.toMediaItem(
                                    messageId = message.id,
                                    chatId = chatId,
                                    senderId = message.senderId,
                                    senderName = null, // Would need user lookup
                                    senderAvatar = null,
                                    timestamp = message.createdAt,
                                    messageType = message.messageType
                                )
                            )
                        }
                    } else if (message.mediaUrl != null) {
                        mediaItems.add(
                            MediaItem(
                                id = UUIDUtils.randomUUID(),
                                messageId = message.id,
                                chatId = chatId,
                                senderId = message.senderId,
                                senderName = null,
                                senderAvatar = null,
                                timestamp = message.createdAt,
                                url = message.mediaUrl,
                                thumbnailUrl = message.mediaUrl,
                                mediaType = getMediaType(message.messageType),
                                duration = message.metadataContainer?.music?.durationSeconds
                            )
                        )
                    }
                }

                isLinkType(message) -> {
                    val url = extractUrl(message)
                    if (url != null) {
                        linkItems.add(
                            LinkItem(
                                id = UUIDUtils.randomUUID(),
                                messageId = message.id,
                                chatId = chatId,
                                senderId = message.senderId,
                                senderName = null,
                                senderAvatar = null,
                                timestamp = message.createdAt,
                                url = url,
                                title = message.linkPreview?.title,
                                description = message.linkPreview?.description,
                                imageUrl = message.linkPreview?.imageUrl,
                                domain = extractDomain(url)
                            )
                        )
                    }
                }

                isFileType(message.messageType) -> {
                    val attachments = message.attachments
                    if (attachments.isNotEmpty()) {
                        attachments.forEach { attachment ->
                            fileItems.add(
                                attachment.toFileItem(
                                    messageId = message.id,
                                    chatId = chatId,
                                    senderId = message.senderId,
                                    senderName = null,
                                    senderAvatar = null,
                                    timestamp = message.createdAt,
                                    messageType = message.messageType
                                )
                            )
                        }
                    } else if (message.mediaUrl != null) {
                        fileItems.add(
                            FileItem(
                                id = UUIDUtils.randomUUID(),
                                messageId = message.id,
                                chatId = chatId,
                                senderId = message.senderId,
                                senderName = null,
                                senderAvatar = null,
                                timestamp = message.createdAt,
                                url = message.mediaUrl,
                                fileName = extractFileName(message.mediaUrl),
                                fileSize = 0L, // Metadata doesn't have fileSize directly
                                mimeType = "application/octet-stream", // Metadata doesn't have mimeType directly
                                fileType = getFileType(message.messageType),
                                duration = message.metadataContainer?.music?.durationSeconds
                            )
                        )
                    }
                }
            }
        }

        return SharedContent(
            mediaItems = mediaItems,
            linkItems = linkItems,
            fileItems = fileItems,
            mediaCount = mediaItems.size,
            linkCount = linkItems.size,
            fileCount = fileItems.size,
            hasMoreMedia = mediaItems.size >= limit,
            hasMoreLinks = linkItems.size >= limit,
            hasMoreFiles = fileItems.size >= limit
        )
    }

    private fun isMediaType(messageType: MessageType): Boolean {
        return messageType in listOf(
            MessageType.IMAGE,
            MessageType.VIDEO,
            MessageType.GIF,
            MessageType.STICKER,
            MessageType.MEDIA_GROUP
        )
    }

    private fun isFileType(messageType: MessageType): Boolean {
        return messageType in listOf(
            MessageType.FILE,
            MessageType.AUDIO,
            MessageType.VOICE
        )
    }

    private fun isLinkType(message: Message): Boolean {
        return message.linkPreview != null ||
                message.content.contains("http://") ||
                message.content.contains("https://")
    }

    private fun getMediaType(messageType: MessageType): MediaType {
        return when (messageType) {
            MessageType.IMAGE -> MediaType.IMAGE
            MessageType.VIDEO -> MediaType.VIDEO
            MessageType.GIF -> MediaType.GIF
            MessageType.STICKER -> MediaType.STICKER
            MessageType.AUDIO -> MediaType.AUDIO
            else -> MediaType.IMAGE
        }
    }

    private fun getFileType(messageType: MessageType): FileType {
        return when (messageType) {
            MessageType.FILE -> FileType.DOCUMENT
            MessageType.AUDIO -> FileType.AUDIO
            MessageType.VOICE -> FileType.VOICE
            MessageType.VIDEO -> FileType.VIDEO
            MessageType.IMAGE -> FileType.IMAGE
            else -> FileType.OTHER
        }
    }

    private fun extractUrl(message: Message): String? {
        // First check link preview
        message.linkPreview?.url?.let { return it }

        // Extract from content
        val content = message.content
        val httpIndex = content.indexOf("http://")
        val httpsIndex = content.indexOf("https://")

        val startIndex = when {
            httpIndex >= 0 && httpsIndex >= 0 -> minOf(httpIndex, httpsIndex)
            httpIndex >= 0 -> httpIndex
            httpsIndex >= 0 -> httpsIndex
            else -> return null
        }

        // Extract URL until whitespace or end
        val url = content.substring(startIndex).takeWhile { !it.isWhitespace() }
        return url.takeIf { it.isNotEmpty() }
    }

    private fun extractDomain(url: String): String {
        return try {
            Url(url).host.ifBlank { url }
        } catch (e: Exception) {
            url
        }
    }

    private fun extractFileName(url: String): String {
        return try {
            Url(url).rawSegments.lastOrNull().orEmpty()
        } catch (e: Exception) {
            url.substringAfterLast('/')
        }
    }

    private fun MessageAttachment.toMediaItem(
        messageId: String,
        chatId: String,
        senderId: String,
        senderName: String?,
        senderAvatar: String?,
        timestamp: String,
        messageType: MessageType
    ): MediaItem {
        return MediaItem(
            id = UUIDUtils.randomUUID(),
            messageId = messageId,
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            senderAvatar = senderAvatar,
            timestamp = timestamp,
            url = this.url,
            thumbnailUrl = this.url,
            mediaType = getMediaTypeFromAttachment(this.type.name, messageType),
            fileSize = this.size,
            duration = this.duration,
            mediaGroupId = this.mediaGroupId
        )
    }

    private fun MessageAttachment.toFileItem(
        messageId: String,
        chatId: String,
        senderId: String,
        senderName: String?,
        senderAvatar: String?,
        timestamp: String,
        messageType: MessageType
    ): FileItem {
        return FileItem(
            id = UUIDUtils.randomUUID(),
            messageId = messageId,
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            senderAvatar = senderAvatar,
            timestamp = timestamp,
            url = this.url,
            fileName = extractFileName(this.url),
            fileSize = this.size ?: 0L,
            mimeType = this.type.name,
            fileType = getFileType(messageType),
            duration = this.duration
        )
    }

    private fun getMediaTypeFromAttachment(
        attachmentType: String,
        messageType: MessageType
    ): MediaType {
        return when {
            attachmentType.contains("video", ignoreCase = true) -> MediaType.VIDEO
            attachmentType.contains("gif", ignoreCase = true) -> MediaType.GIF
            attachmentType.contains("image", ignoreCase = true) -> MediaType.IMAGE
            attachmentType.contains("audio", ignoreCase = true) -> MediaType.AUDIO
            else -> getMediaType(messageType)
        }
    }
}
