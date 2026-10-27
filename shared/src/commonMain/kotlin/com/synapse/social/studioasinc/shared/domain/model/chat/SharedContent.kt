package com.synapse.social.studioasinc.shared.domain.model.chat

/**
 * Sealed class representing different types of shared content in a conversation.
 */
sealed class SharedContentItem {
    abstract val id: String
    abstract val messageId: String
    abstract val chatId: String
    abstract val senderId: String
    abstract val senderName: String?
    abstract val senderAvatar: String?
    abstract val timestamp: String
}

/**
 * Media item (image, video, gif, sticker)
 */
data class MediaItem(
    override val id: String,
    override val messageId: String,
    override val chatId: String,
    override val senderId: String,
    override val senderName: String?,
    override val senderAvatar: String?,
    override val timestamp: String,
    val url: String,
    val thumbnailUrl: String?,
    val mediaType: MediaType,
    val fileSize: Long? = null,
    val duration: Int? = null, // in seconds for video/audio
    val mediaGroupId: String? = null
) : SharedContentItem()

/**
 * Link item (URL with or without preview)
 */
data class LinkItem(
    override val id: String,
    override val messageId: String,
    override val chatId: String,
    override val senderId: String,
    override val senderName: String?,
    override val senderAvatar: String?,
    override val timestamp: String,
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val domain: String
) : SharedContentItem()

/**
 * File item (document, audio, voice note)
 */
data class FileItem(
    override val id: String,
    override val messageId: String,
    override val chatId: String,
    override val senderId: String,
    override val senderName: String?,
    override val senderAvatar: String?,
    override val timestamp: String,
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val fileType: FileType,
    val duration: Int? = null // in seconds for audio/voice
) : SharedContentItem()

/**
 * Media type classification
 */
enum class MediaType {
    IMAGE,
    VIDEO,
    GIF,
    STICKER,
    AUDIO
}

/**
 * File type classification
 */
enum class FileType {
    DOCUMENT,
    AUDIO,
    VOICE,
    VIDEO,
    IMAGE,
    OTHER
}

/**
 * Container for shared content with pagination info
 */
data class SharedContent(
    val mediaItems: List<MediaItem> = emptyList(),
    val linkItems: List<LinkItem> = emptyList(),
    val fileItems: List<FileItem> = emptyList(),
    val mediaCount: Int = 0,
    val linkCount: Int = 0,
    val fileCount: Int = 0,
    val hasMoreMedia: Boolean = false,
    val hasMoreLinks: Boolean = false,
    val hasMoreFiles: Boolean = false
)

/**
 * Content type for filtering
 */
enum class ContentType {
    MEDIA,
    LINKS,
    FILES
}
