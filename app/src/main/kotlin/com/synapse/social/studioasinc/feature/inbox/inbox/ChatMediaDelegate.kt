package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.core.util.UploadProgressManager
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.repository.FileUploader
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SendMessageUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.UploadMediaUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class ChatMediaDelegate(
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val fileUploader: FileUploader,
    private val uploadProgressManager: UploadProgressManager,
    private val viewModelScope: CoroutineScope,
    private val currentUserIdProvider: () -> String?,
    private val chatIdProvider: () -> String?,
    private val onOptimisticMessageAdded: (Message, String) -> Unit,
    private val onOptimisticMessageUpdated: (String, String, Int?) -> Unit,
    private val onOptimisticMessageSuccess: (String, Message) -> Unit,
    private val onOptimisticMessageFailed: (String, String?) -> Unit,
    private val onError: (String?) -> Unit
) {
    data class PendingUploadInfo(
        val tempId: String,
        val filePath: String,
        val fileName: String,
        val contentType: String,
        val messageType: String,
        val caption: String?
    )

    private val pendingUploads = mutableMapOf<String, PendingUploadInfo>()

    fun sendMediaMessage(
        mediaUrl: String,
        fileName: String,
        contentType: String,
        messageType: String,
        caption: String? = null
    ) {
        val chatId = chatIdProvider() ?: return
        viewModelScope.launch {
            val tempId = UUID.randomUUID().toString()
            val type = when (messageType) {
                "image" -> MessageType.IMAGE
                "video" -> MessageType.VIDEO
                "audio" -> MessageType.AUDIO
                else -> MessageType.FILE
            }
            val newMessage = Message(
                id = tempId,
                chatId = chatId,
                senderId = currentUserIdProvider() ?: "",
                content = "Sending...",
                messageType = type,
                deliveryStatus = DeliveryStatus.SENT,
                createdAt = Instant.now().toString()
            )
            onOptimisticMessageAdded(newMessage, tempId)
            sendMessageUseCase(
                chatId = chatId,
                content = if (!caption.isNullOrBlank()) caption else fileName,
                mediaUrl = mediaUrl,
                messageType = messageType
            ).onSuccess { actualMessage ->
                onOptimisticMessageSuccess(tempId, actualMessage)
            }.onFailure { e ->
                onOptimisticMessageFailed(tempId, "Failed to send: ${e.message}")
            }
        }
    }

    fun uploadAndSendMedia(
        filePath: String,
        fileName: String,
        contentType: String,
        messageType: String,
        caption: String? = null
    ) {
        val chatId = chatIdProvider() ?: return

        viewModelScope.launch {
            val fileSize = fileUploader.getFileSize(filePath)
            val maxVideoSize = 50 * 1024 * 1024L // 50MB
            val maxImageSize = 10 * 1024 * 1024L // 10MB

            if (messageType == "video" && fileSize > maxVideoSize) {
                onError("Video file size exceeds 50MB limit")
                return@launch
            }
            if (messageType == "image" && fileSize > maxImageSize) {
                onError("Image file size exceeds 10MB limit")
                return@launch
            }

            // Optimistic update
            val tempId = UUID.randomUUID().toString()

            val type = when(messageType) {
                "image" -> MessageType.IMAGE
                "video" -> MessageType.VIDEO
                "audio" -> MessageType.AUDIO
                else -> MessageType.FILE
            }
            val newMessage = Message(
                id = tempId,
                chatId = chatId,
                senderId = currentUserIdProvider() ?: "",
                content = "Uploading...",
                messageType = type,
                deliveryStatus = DeliveryStatus.SENT,
                createdAt = Instant.now().toString()
            )
            onOptimisticMessageAdded(newMessage, tempId)

            pendingUploads[tempId] = PendingUploadInfo(
                tempId = tempId,
                filePath = filePath,
                fileName = fileName,
                contentType = contentType,
                messageType = messageType,
                caption = caption
            )

            executeUpload(tempId, chatId, filePath, fileName, contentType, messageType, caption)
        }
    }

    fun retryUpload(tempId: String) {
        val info = pendingUploads[tempId] ?: return
        val chatId = chatIdProvider() ?: return

        viewModelScope.launch {
            onOptimisticMessageUpdated(tempId, "Uploading... 0%", 0)
            executeUpload(
                tempId = info.tempId,
                chatId = chatId,
                filePath = info.filePath,
                fileName = info.fileName,
                contentType = info.contentType,
                messageType = info.messageType,
                caption = info.caption
            )
        }
    }

    private suspend fun executeUpload(
        tempId: String,
        chatId: String,
        filePath: String,
        fileName: String,
        contentType: String,
        messageType: String,
        caption: String?
    ) {
        uploadMediaUseCase(
            chatId = chatId,
            filePath = filePath,
            fileName = fileName,
            contentType = contentType,
            onProgress = { progress ->
                uploadProgressManager.updateProgress(chatId, fileName, progress)
                onOptimisticMessageUpdated(tempId, "Uploading... $progress%", progress)
            }
        ).onSuccess { mediaUrl ->
            val finalContent = if (!caption.isNullOrBlank()) caption else if (messageType == "image" || messageType == "video") "Media message" else fileName
            sendMessageUseCase(
                chatId = chatId,
                content = finalContent,
                mediaUrl = mediaUrl,
                messageType = messageType
            ).onSuccess { actualMessage ->
                pendingUploads.remove(tempId)
                onOptimisticMessageSuccess(tempId, actualMessage)
                uploadProgressManager.dismissProgress(chatId, fileName)
            }.onFailure { e ->
                uploadProgressManager.finishProgress(chatId, fileName, false, "Upload Failed")
                onOptimisticMessageFailed(tempId, "Failed to send: ${e.message}")
            }
        }.onFailure { e ->
            uploadProgressManager.finishProgress(chatId, fileName, false, "Upload Failed")
            onOptimisticMessageFailed(tempId, "Upload failed: ${e.message}")
        }
    }
}
