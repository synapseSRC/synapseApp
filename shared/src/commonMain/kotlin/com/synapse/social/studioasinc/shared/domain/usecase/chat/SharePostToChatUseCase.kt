package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageMetadataContainer
import com.synapse.social.studioasinc.shared.domain.model.chat.SharedPostMetadata

/**
 * Sends a feed post into a conversation as a rich `shared_post` message.
 *
 * The plain [previewText] is stored as the message body so inbox previews and notifications have
 * something readable to show; the card itself is rendered from [sharedPost] in the metadata.
 */
class SharePostToChatUseCase(
    private val sendMessageUseCase: SendMessageUseCase
) {

    suspend operator fun invoke(
        chatId: String,
        sharedPost: SharedPostMetadata,
        previewText: String
    ): Result<Message> {
        if (chatId.isBlank() || sharedPost.postId.isBlank()) {
            return Result.failure(IllegalArgumentException("Chat and post identifiers are required"))
        }
        return sendMessageUseCase(
            chatId = chatId,
            content = previewText,
            messageType = MESSAGE_TYPE,
            metadataContainer = MessageMetadataContainer(
                sharedPost = sharedPost.copy(content = sharedPost.content?.trim()?.take(MAX_CONTENT_LENGTH))
            )
        )
    }

    private companion object {
        const val MESSAGE_TYPE = "shared_post"
        const val MAX_CONTENT_LENGTH = 300
    }
}
