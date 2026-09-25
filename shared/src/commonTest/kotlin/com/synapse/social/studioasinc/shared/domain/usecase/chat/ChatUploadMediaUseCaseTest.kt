package com.synapse.social.studioasinc.shared.domain.usecase.chat

import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.chat.ChatInfo
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction
import com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.repository.FileUploader
import com.synapse.social.studioasinc.shared.domain.repository.MediaUploadRepository
import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatUploadMediaUseCaseTest {

    private class FakeStorageRepository(private val config: StorageConfig) : StorageRepository {
        override fun getStorageConfig(): Flow<StorageConfig> = flowOf(config)
        override suspend fun saveStorageConfig(config: StorageConfig) {}
        override suspend fun updatePhotoProvider(provider: StorageProvider) {}
        override suspend fun updateVideoProvider(provider: StorageProvider) {}
        override suspend fun updateOtherProvider(provider: StorageProvider) {}
        override suspend fun updateImgBBConfig(key: String) {}
        override suspend fun updateCloudinaryConfig(cloudName: String, apiKey: String, apiSecret: String, uploadPreset: String) {}
        override suspend fun updateSupabaseConfig(url: String, key: String, bucket: String) {}
        override suspend fun updateR2Config(accountId: String, accessKeyId: String, secretAccessKey: String, bucketName: String) {}
        override suspend fun updateCompression(enabled: Boolean) {}
        override suspend fun clearProviderConfig(provider: StorageProvider) {}
        override suspend fun ensureDefault() {}
    }

    private class FakeFileUploader(private val fileSize: Long = 1000L) : FileUploader {
        override fun getFileSize(filePath: String): Long = fileSize
    }

    private class FakeMediaUploadRepository(
        private val uploadHandler: (filePath: String, provider: StorageProvider, bucketName: String?) -> Result<String>
    ) : MediaUploadRepository {
        var capturedBucketName: String? = null
        val attemptedProviders = mutableListOf<StorageProvider>()

        override suspend fun upload(
            filePath: String,
            provider: StorageProvider,
            config: StorageConfig,
            bucketName: String?,
            onProgress: (Float) -> Unit
        ): Result<String> {
            attemptedProviders.add(provider)
            capturedBucketName = bucketName
            return uploadHandler(filePath, provider, bucketName)
        }

        override fun deleteFile(filePath: String) {}
    }

    @Test
    fun testAudioUploadUsesChatAttachmentsBucket() = runTest {
        val config = StorageConfig(
            otherProvider = StorageProvider.SUPABASE,
            supabaseUrl = "https://example.supabase.co",
            supabaseKey = "anon_key"
        )
        val storageRepo = FakeStorageRepository(config)
        val fileUploader = FakeFileUploader()
        val uploadRepo = FakeMediaUploadRepository { _, provider, bucket ->
            if (provider == StorageProvider.SUPABASE) {
                Result.success("https://example.supabase.co/storage/v1/object/chat-attachments/voice.m4a")
            } else {
                Result.failure(Exception("Failed"))
            }
        }

        val useCase = UploadMediaUseCase(
            repository = createDummyChatRepository(),
            storageRepository = storageRepo,
            mediaUploadRepository = uploadRepo,
            fileUploader = fileUploader
        )

        val result = useCase(
            chatId = "chat_123",
            filePath = "/path/to/voice.m4a",
            fileName = "voice.m4a",
            contentType = "audio/mp4"
        )

        assertTrue(result.isSuccess)
        assertEquals("chat-attachments", uploadRepo.capturedBucketName)
        assertEquals("https://example.supabase.co/storage/v1/object/chat-attachments/voice.m4a", result.getOrNull())
    }
}

private fun createDummyChatRepository(): ChatRepository {
    return object : ChatRepository {
        override suspend fun getConversations(): Result<List<Conversation>> = Result.success(emptyList())
        override suspend fun getMessages(chatId: String, limit: Int, before: String?, beforeId: String?, forceNetwork: Boolean): Result<List<Message>> = Result.success(emptyList())
        override suspend fun getMessageById(messageId: String): Result<Message?> = Result.success(null)
        override suspend fun sendMessage(chatId: String, content: String, mediaUrl: String?, messageType: String, expiresAt: String?, replyToId: String?, senderPlaintext: String?): Result<Message> = Result.failure(Exception("Not implemented"))
        override suspend fun getOrCreateChat(otherUserId: String): Result<String> = Result.failure(Exception("Not implemented"))
        override suspend fun updateConversationArchiveStatus(chatId: String, isArchived: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun deleteConversation(chatId: String): Result<Unit> = Result.success(Unit)
        override suspend fun markMessagesAsRead(chatId: String): Result<Unit> = Result.success(Unit)
        override suspend fun markMessagesAsDelivered(chatId: String): Result<Unit> = Result.success(Unit)
        override suspend fun editMessage(messageId: String, newContent: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteMessage(messageId: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteMessages(messageIds: List<String>): Result<Unit> = Result.success(Unit)
        override suspend fun deleteMessageForMe(messageId: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteMessagesForMe(messageIds: List<String>): Result<Unit> = Result.success(Unit)
        override suspend fun uploadMedia(chatId: String, filePath: String, fileName: String, contentType: String, provider: StorageProvider?, config: StorageConfig?, onProgress: ((Int) -> Unit)?): Result<String> = Result.failure(Exception("Not implemented"))
        override suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean): Result<Unit> = Result.success(Unit)
        override fun subscribeToMessages(chatId: String): Flow<Message> = emptyFlow()
        override fun subscribeToInboxUpdates(chatIds: List<String>): Flow<Message> = emptyFlow()
        override fun subscribeToTypingStatus(chatId: String): Flow<TypingStatus> = emptyFlow()
        override fun subscribeToReadReceipts(chatId: String): Flow<Message> = emptyFlow()
        override suspend fun initializeE2EE(): Result<Unit> = Result.success(Unit)
        override suspend fun ensureSession(userId: String) {}
        override fun getCurrentUserId(): String? = "user-1"
        override suspend fun createGroupChat(name: String, participantIds: List<String>, avatarUrl: String?): Result<String> = Result.success("group-1")
        override suspend fun getParticipantIds(chatId: String): Result<List<String>> = Result.success(emptyList())
        override suspend fun getGroupMembers(chatId: String): Result<List<Pair<User, Boolean>>> = Result.success(emptyList())
        override suspend fun addGroupMembers(chatId: String, userIds: List<String>): Result<Unit> = Result.success(Unit)
        override suspend fun removeGroupMember(chatId: String, userId: String): Result<Unit> = Result.success(Unit)
        override suspend fun promoteToAdmin(chatId: String, userId: String): Result<Unit> = Result.success(Unit)
        override suspend fun demoteAdmin(chatId: String, userId: String): Result<Unit> = Result.success(Unit)
        override suspend fun leaveGroup(chatId: String): Result<Unit> = Result.success(Unit)
        override suspend fun toggleOnlyAdminsCanMessage(chatId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun getChatInfo(chatId: String): Result<ChatInfo?> = Result.failure(Exception("Not implemented"))
        override suspend fun toggleMessageReaction(messageId: String, emoji: String, chatId: String?): Result<Unit> = Result.success(Unit)
        override suspend fun getReactionsForMessage(messageId: String): Result<List<MessageReaction>> = Result.success(emptyList())
        override suspend fun getReactionsForMessages(messages: List<Message>): List<Message> = messages
        override suspend fun clearLocalCache() {}
        override fun subscribeToMessageReactions(chatId: String): Flow<MessageReaction> = emptyFlow()
        override suspend fun setDisappearingMode(chatId: String, mode: DisappearingMode): Result<Unit> = Result.success(Unit)
        override suspend fun getDisappearingMode(chatId: String): Result<DisappearingMode> = Result.success(DisappearingMode.OFF)
    }
}
