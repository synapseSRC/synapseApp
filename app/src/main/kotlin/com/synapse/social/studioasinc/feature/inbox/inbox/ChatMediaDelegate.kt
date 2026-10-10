package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.core.util.UploadProgressManager
import com.synapse.social.studioasinc.feature.shared.components.picker.PickedFile
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.model.settings.MediaUploadQuality
import com.synapse.social.studioasinc.shared.domain.repository.FileUploader
import com.synapse.social.studioasinc.shared.domain.service.MediaCompressor
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SendMessageUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.UploadMediaUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    private val onOptimisticMessageUpdated: (String, String) -> Unit,
    private val onOptimisticMessageSuccess: (String, Message) -> Unit,
    private val onOptimisticMessageFailed: (String, String?) -> Unit,
    private val onError: (String?) -> Unit,
    private val mediaCompressor: MediaCompressor? = null,
    private val replyToMessageIdProvider: () -> String? = { null },
    private val onReplyConsumed: (String?) -> Unit = {}
) {

    suspend fun uploadAndSendVoiceMessageSuspend(
        audioFile: java.io.File,
        durationMs: Long = 0L,
        storageConfig: com.synapse.social.studioasinc.shared.domain.model.StorageConfig,
        voiceUploadService: com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceUploadService,
        sendMessageUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.SendMessageUseCase = this.sendMessageUseCase,
        currentUserIdProvider: () -> String? = this.currentUserIdProvider,
        chatIdProvider: () -> String? = this.chatIdProvider,
        onResult: ((Boolean, String?) -> Unit)? = null,
        replyToId: String? = replyToMessageIdProvider(),
        clearReplySelection: Boolean = true
    ) {
        val chatId = chatIdProvider()
        val currentUserId = currentUserIdProvider()

        if (chatId.isNullOrBlank() || currentUserId.isNullOrBlank()) {
            val err = "Missing chat or user ID"
            onError(err)
            onResult?.invoke(false, err)
            return
        }

        val selectedReplyToId = replyToId
        val durationSeconds = (durationMs / 1000).toInt().coerceAtLeast(1)
        val tempId = UUID.randomUUID().toString()
        val audioAttachment = com.synapse.social.studioasinc.shared.domain.model.chat.MessageAttachment(
            url = audioFile.absolutePath,
            type = com.synapse.social.studioasinc.shared.domain.model.chat.AttachmentType.AUDIO,
            size = audioFile.length(),
            duration = durationSeconds,
            uploadProgress = 0.0f
        )
        val optimisticMessage = Message(
            id = tempId,
            chatId = chatId,
            senderId = currentUserId,
            content = "",
            mediaUrl = audioFile.absolutePath,
            messageType = MessageType.AUDIO,
            deliveryStatus = DeliveryStatus.SENDING,
            createdAt = Instant.now().toString(),
            attachments = listOf(audioAttachment),
            replyToId = selectedReplyToId
        )
        onOptimisticMessageAdded(optimisticMessage, tempId)

        try {
            val uploadResult = voiceUploadService.upload(
                audioFile = audioFile,
                chatId = chatId,
                config = storageConfig
            )

            uploadResult.onSuccess { mediaUrl ->
                sendMessageUseCase(
                    chatId = chatId,
                    content = "",
                    mediaUrl = mediaUrl,
                    messageType = MessageType.AUDIO.name.lowercase(),
                    attachments = listOf(audioAttachment.copy(url = mediaUrl, uploadProgress = 1.0f)),
                    replyToId = selectedReplyToId
                ).onSuccess { actualMessage ->
                    onOptimisticMessageSuccess(tempId, actualMessage)
                    if (clearReplySelection) onReplyConsumed(selectedReplyToId)
                    onResult?.invoke(true, null)
                }.onFailure { e ->
                    val err = "Failed to send: ${e.message}"
                    onOptimisticMessageFailed(tempId, err)
                    onResult?.invoke(false, err)
                }
            }.onFailure { e ->
                val err = "Voice upload failed: ${e.message}"
                onOptimisticMessageFailed(tempId, err)
                onResult?.invoke(false, err)
            }
        } catch (e: CancellationException) {
            onOptimisticMessageFailed(tempId, "Cancelled")
            onResult?.invoke(false, "Cancelled")
            throw e
        } catch (e: Exception) {
            val err = "Upload error: ${e.message}"
            onOptimisticMessageFailed(tempId, err)
            onResult?.invoke(false, err)
        }
    }

    fun uploadAndSendVoiceMessage(
        audioFile: java.io.File,
        durationMs: Long = 0L,
        storageConfig: com.synapse.social.studioasinc.shared.domain.model.StorageConfig,
        voiceUploadService: com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceUploadService,
        sendMessageUseCase: com.synapse.social.studioasinc.shared.domain.usecase.chat.SendMessageUseCase = this.sendMessageUseCase,
        currentUserIdProvider: () -> String? = this.currentUserIdProvider,
        chatIdProvider: () -> String? = this.chatIdProvider,
        viewModelScope: kotlinx.coroutines.CoroutineScope = this.viewModelScope,
        onResult: ((Boolean, String?) -> Unit)? = null
    ): Job {
        val selectedReplyToId = replyToMessageIdProvider()
        return viewModelScope.launch {
            uploadAndSendVoiceMessageSuspend(
                audioFile = audioFile,
                durationMs = durationMs,
                storageConfig = storageConfig,
                voiceUploadService = voiceUploadService,
                sendMessageUseCase = sendMessageUseCase,
                currentUserIdProvider = currentUserIdProvider,
                chatIdProvider = chatIdProvider,
                replyToId = selectedReplyToId,
                clearReplySelection = true,
                onResult = onResult
            )
        }
    }

    fun sendMediaMessage(
        mediaUrl: String,
        fileName: String,
        contentType: String,
        messageType: String,
        caption: String? = null
    ) {
        val chatId = chatIdProvider() ?: return
        val replyToId = replyToMessageIdProvider()
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
                deliveryStatus = DeliveryStatus.SENDING,
                createdAt = Instant.now().toString(),
                replyToId = replyToId
            )
            onOptimisticMessageAdded(newMessage, tempId)
            sendMessageUseCase(
                chatId = chatId,
                content = if (!caption.isNullOrBlank()) caption else fileName,
                mediaUrl = mediaUrl,
                messageType = messageType,
                replyToId = replyToId
            ).onSuccess { actualMessage ->
                onOptimisticMessageSuccess(tempId, actualMessage)
                onReplyConsumed(replyToId)
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
        caption: String? = null,
        quality: MediaUploadQuality = MediaUploadQuality.STANDARD
    ) {
        val chatId = chatIdProvider() ?: return
        val replyToId = replyToMessageIdProvider()

        viewModelScope.launch {
            var uploadPath = filePath
            if (messageType == "image" && mediaCompressor != null) {
                mediaCompressor.compress(filePath, quality).onSuccess { compressedPath ->
                    uploadPath = compressedPath
                }
            }

            val fileSize = fileUploader.getFileSize(uploadPath)
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
                mediaUrl = filePath,
                messageType = type,
                deliveryStatus = DeliveryStatus.SENDING,
                createdAt = Instant.now().toString(),
                replyToId = replyToId
            )
            onOptimisticMessageAdded(newMessage, tempId)

            uploadMediaUseCase(
                chatId = chatId,
                filePath = uploadPath,
                fileName = fileName,
                contentType = contentType,
                onProgress = { progress ->
                    uploadProgressManager.updateProgress(chatId, fileName, progress)
                    onOptimisticMessageUpdated(tempId, "Uploading... $progress%")
                }
            ).onSuccess { mediaUrl ->
                val finalContent = if (!caption.isNullOrBlank()) caption else fileName
                sendMessageUseCase(
                    chatId = chatId,
                    content = finalContent,
                    mediaUrl = mediaUrl,
                    messageType = messageType,
                    replyToId = replyToId
                ).onSuccess { actualMessage ->
                    onOptimisticMessageSuccess(tempId, actualMessage)
                    onReplyConsumed(replyToId)
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

    fun uploadAndSendMultipleMedia(
        files: List<Pair<String, PickedFile>>,
        caption: String? = null,
        quality: MediaUploadQuality = MediaUploadQuality.STANDARD
    ) {
        if (files.isEmpty()) return
        if (files.size == 1) {
            val (filePath, pickedFile) = files.first()
            val type = when {
                pickedFile.mimeType.startsWith("image/") -> "image"
                pickedFile.mimeType.startsWith("video/") -> "video"
                pickedFile.mimeType.startsWith("audio/") -> "audio"
                else -> "file"
            }
            uploadAndSendMedia(
                filePath = filePath,
                fileName = pickedFile.fileName,
                contentType = pickedFile.mimeType,
                messageType = type,
                caption = caption,
                quality = quality
            )
            return
        }

        val chatId = chatIdProvider() ?: return
        val replyToId = replyToMessageIdProvider()
        viewModelScope.launch {
            val batchMediaGroupId = UUID.randomUUID().toString()
            val tempId = UUID.randomUUID().toString()

            var currentAttachments = files.map { (_, pickedFile) ->
                val type = if (pickedFile.mimeType.startsWith("video/")) com.synapse.social.studioasinc.shared.domain.model.chat.AttachmentType.VIDEO else com.synapse.social.studioasinc.shared.domain.model.chat.AttachmentType.IMAGE
                com.synapse.social.studioasinc.shared.domain.model.chat.MessageAttachment(
                    url = pickedFile.uri.toString(),
                    type = type,
                    size = pickedFile.size,
                    mediaGroupId = batchMediaGroupId,
                    uploadProgress = 0.0f
                )
            }

            var optimisticMessage = Message(
                id = tempId,
                chatId = chatId,
                senderId = currentUserIdProvider() ?: "",
                content = if (!caption.isNullOrBlank()) caption else "",
                messageType = MessageType.MEDIA_GROUP,
                deliveryStatus = DeliveryStatus.SENDING,
                createdAt = Instant.now().toString(),
                attachments = currentAttachments,
                mediaGroupId = batchMediaGroupId,
                replyToId = replyToId
            )
            onOptimisticMessageAdded(optimisticMessage, tempId)

            val uploadedAttachments = mutableListOf<com.synapse.social.studioasinc.shared.domain.model.chat.MessageAttachment>()
            var hasError = false
            var errorMessageStr: String? = null

            for (index in files.indices) {
                val (filePath, pickedFile) = files[index]
                val isImage = pickedFile.mimeType.startsWith("image/")
                val isVideo = pickedFile.mimeType.startsWith("video/")

                var uploadPath = filePath
                if (isImage && mediaCompressor != null) {
                    mediaCompressor.compress(filePath, quality).onSuccess { compressedPath ->
                        uploadPath = compressedPath
                    }
                }

                val fileSize = fileUploader.getFileSize(uploadPath)
                val maxVideoSize = 50 * 1024 * 1024L
                val maxImageSize = 10 * 1024 * 1024L

                if (isVideo && fileSize > maxVideoSize) {
                    hasError = true
                    errorMessageStr = "Video file size exceeds 50MB limit"
                    currentAttachments = currentAttachments.mapIndexed { idx, att ->
                        if (idx == index) att.copy(isUploadFailed = true) else att
                    }
                    onOptimisticMessageAdded(optimisticMessage.copy(attachments = currentAttachments), tempId)
                    break
                }
                if (isImage && fileSize > maxImageSize) {
                    hasError = true
                    errorMessageStr = "Image file size exceeds 10MB limit"
                    currentAttachments = currentAttachments.mapIndexed { idx, att ->
                        if (idx == index) att.copy(isUploadFailed = true) else att
                    }
                    onOptimisticMessageAdded(optimisticMessage.copy(attachments = currentAttachments), tempId)
                    break
                }

                val uploadResult = uploadMediaUseCase(
                    chatId = chatId,
                    filePath = uploadPath,
                    fileName = pickedFile.fileName,
                    contentType = pickedFile.mimeType,
                    onProgress = { progress ->
                        val floatProgress = (progress / 100f).coerceIn(0f, 1f)
                        currentAttachments = currentAttachments.mapIndexed { idx, att ->
                            if (idx == index) att.copy(uploadProgress = floatProgress) else att
                        }
                        onOptimisticMessageAdded(optimisticMessage.copy(attachments = currentAttachments), tempId)
                    }
                )

                if (uploadResult.isSuccess) {
                    val mediaUrl = uploadResult.getOrThrow()
                    val attachmentType = if (isVideo) com.synapse.social.studioasinc.shared.domain.model.chat.AttachmentType.VIDEO else com.synapse.social.studioasinc.shared.domain.model.chat.AttachmentType.IMAGE
                    currentAttachments = currentAttachments.mapIndexed { idx, att ->
                        if (idx == index) att.copy(url = mediaUrl, uploadProgress = 1.0f) else att
                    }
                    uploadedAttachments.add(
                        com.synapse.social.studioasinc.shared.domain.model.chat.MessageAttachment(
                            url = mediaUrl,
                            type = attachmentType,
                            size = fileSize,
                            mediaGroupId = batchMediaGroupId,
                            uploadProgress = 1.0f
                        )
                    )
                    onOptimisticMessageAdded(optimisticMessage.copy(attachments = currentAttachments), tempId)
                } else {
                    val err = uploadResult.exceptionOrNull()
                    hasError = true
                    errorMessageStr = "Upload failed: ${err?.message}"
                    currentAttachments = currentAttachments.mapIndexed { idx, att ->
                        if (idx == index) att.copy(isUploadFailed = true) else att
                    }
                    onOptimisticMessageAdded(optimisticMessage.copy(attachments = currentAttachments), tempId)
                    break
                }
            }

            if (hasError) {
                onError(errorMessageStr)
                onOptimisticMessageFailed(tempId, errorMessageStr)
                return@launch
            }

            val finalContent = if (!caption.isNullOrBlank()) caption else ""
            sendMessageUseCase(
                chatId = chatId,
                content = finalContent,
                mediaUrl = uploadedAttachments.firstOrNull()?.url,
                messageType = "media_group",
                attachments = uploadedAttachments,
                replyToId = replyToId
            ).onSuccess { actualMessage ->
                onOptimisticMessageSuccess(tempId, actualMessage)
                onReplyConsumed(replyToId)
            }.onFailure { e ->
                onOptimisticMessageFailed(tempId, "Failed to send: ${e.message}")
            }
        }
    }
}
