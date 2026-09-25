package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.shared.domain.model.ReactionType
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.usecase.chat.ToggleMessageReactionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatReactionDelegateTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private class FakeChatRepository : ChatRepository {
        var shouldSucceed = true
        var lastToggledMessageId: String? = null
        var lastToggledEmoji: String? = null
        var lastToggledChatId: String? = null

        override suspend fun toggleMessageReaction(messageId: String, emoji: String, chatId: String?): Result<Unit> {
            lastToggledMessageId = messageId
            lastToggledEmoji = emoji
            lastToggledChatId = chatId
            return if (shouldSucceed) Result.success(Unit) else Result.failure(Exception("Network error"))
        }

        override suspend fun ensureSession(userId: String) {}
        override suspend fun getConversations() = Result.success(emptyList<com.synapse.social.studioasinc.shared.domain.model.chat.Conversation>())
        override suspend fun getMessages(chatId: String, limit: Int, before: String?, beforeId: String?, forceNetwork: Boolean) = Result.success(emptyList<Message>())
        override suspend fun getMessageById(messageId: String) = Result.success(null)
        override suspend fun sendMessage(chatId: String, content: String, mediaUrl: String?, messageType: String, expiresAt: String?, replyToId: String?, senderPlaintext: String?): Result<Message> = Result.failure(Exception("Not implemented"))
        override suspend fun getOrCreateChat(otherUserId: String) = Result.success("chat-1")
        override suspend fun updateConversationArchiveStatus(chatId: String, isArchived: Boolean) = Result.success(Unit)
        override suspend fun deleteConversation(chatId: String) = Result.success(Unit)
        override suspend fun markMessagesAsRead(chatId: String) = Result.success(Unit)
        override suspend fun markMessagesAsDelivered(chatId: String) = Result.success(Unit)
        override suspend fun deleteMessage(messageId: String) = Result.success(Unit)
        override suspend fun deleteMessages(messageIds: List<String>) = Result.success(Unit)
        override suspend fun setDisappearingMode(chatId: String, mode: com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode) = Result.success(Unit)
        override suspend fun getDisappearingMode(chatId: String) = Result.success(com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode.OFF)
        override suspend fun deleteMessageForMe(messageId: String) = Result.success(Unit)
        override suspend fun deleteMessagesForMe(messageIds: List<String>) = Result.success(Unit)
        override suspend fun editMessage(messageId: String, newContent: String) = Result.success(Unit)
        override suspend fun uploadMedia(chatId: String, filePath: String, fileName: String, contentType: String, provider: com.synapse.social.studioasinc.shared.domain.model.StorageProvider?, config: com.synapse.social.studioasinc.shared.domain.model.StorageConfig?, onProgress: ((Int) -> Unit)?) = Result.success("")
        override suspend fun broadcastTypingStatus(chatId: String, isTyping: Boolean) = Result.success(Unit)
        override fun subscribeToMessages(chatId: String) = kotlinx.coroutines.flow.emptyFlow<Message>()
        override fun subscribeToInboxUpdates(chatIds: List<String>) = kotlinx.coroutines.flow.emptyFlow<Message>()
        override fun subscribeToTypingStatus(chatId: String) = kotlinx.coroutines.flow.emptyFlow<com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus>()
        override fun subscribeToReadReceipts(chatId: String) = kotlinx.coroutines.flow.emptyFlow<Message>()
        override suspend fun initializeE2EE() = Result.success(Unit)
        override fun getCurrentUserId() = "user-1"
        override suspend fun createGroupChat(name: String, participantIds: List<String>, avatarUrl: String?) = Result.success("chat-1")
        override suspend fun getParticipantIds(chatId: String) = Result.success(emptyList<String>())
        override suspend fun getGroupMembers(chatId: String) = Result.success(emptyList<Pair<com.synapse.social.studioasinc.shared.domain.model.User, Boolean>>())
        override suspend fun addGroupMembers(chatId: String, userIds: List<String>) = Result.success(Unit)
        override suspend fun removeGroupMember(chatId: String, userId: String) = Result.success(Unit)
        override suspend fun promoteToAdmin(chatId: String, userId: String) = Result.success(Unit)
        override suspend fun demoteAdmin(chatId: String, userId: String) = Result.success(Unit)
        override suspend fun leaveGroup(chatId: String) = Result.success(Unit)
        override suspend fun toggleOnlyAdminsCanMessage(chatId: String, enabled: Boolean) = Result.success(Unit)
        override suspend fun getChatInfo(chatId: String) = Result.success(null)
        override suspend fun getReactionsForMessage(messageId: String) = Result.success(emptyList<com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction>())
        override suspend fun clearLocalCache() {}
        override suspend fun getReactionsForMessages(messages: List<Message>) = messages
        override fun subscribeToMessageReactions(chatId: String) = kotlinx.coroutines.flow.emptyFlow<com.synapse.social.studioasinc.shared.domain.model.chat.MessageReaction>()
    }

    private fun createTestMessage(
        id: String = "msg-1",
        chatId: String = "chat-123",
        userReaction: ReactionType? = null
    ): Message {
        return Message(
            id = id,
            chatId = chatId,
            senderId = "user-1",
            content = "Hello",
            messageType = MessageType.TEXT,
            deliveryStatus = DeliveryStatus.SENT,
            createdAt = "2025-01-01T00:00:00Z",
            userReaction = userReaction
        )
    }

    @Test
    fun `add reaction optimistically updates state and calls usecase`() = testScope.runTest {
        val fakeRepo = FakeChatRepository()
        val useCase = ToggleMessageReactionUseCase(fakeRepo)

        var optimisticMessageId: String? = null
        var optimisticNewReaction: ReactionType? = null
        var optimisticOldReaction: ReactionType? = null

        val delegate = ChatReactionDelegate(
            toggleMessageReactionUseCase = useCase,
            viewModelScope = this,
            onOptimisticReactionChanged = { msgId, _, newReaction, oldReaction ->
                optimisticMessageId = msgId
                optimisticNewReaction = newReaction
                optimisticOldReaction = oldReaction
            },
            onError = { _, _ -> }
        )

        val message = createTestMessage(userReaction = null)

        delegate.toggleMessageReaction("msg-1", ReactionType.LIKE, listOf(message))

        assertEquals("msg-1", optimisticMessageId)
        assertEquals(ReactionType.LIKE, optimisticNewReaction)
        assertNull(optimisticOldReaction)

        assertEquals("msg-1", fakeRepo.lastToggledMessageId)
        assertEquals(ReactionType.LIKE.emoji, fakeRepo.lastToggledEmoji)
        assertEquals("chat-123", fakeRepo.lastToggledChatId)
    }

    @Test
    fun `switch reaction optimistically updates state with new reaction`() = testScope.runTest {
        val fakeRepo = FakeChatRepository()
        val useCase = ToggleMessageReactionUseCase(fakeRepo)

        var optimisticNewReaction: ReactionType? = null
        var optimisticOldReaction: ReactionType? = null

        val delegate = ChatReactionDelegate(
            toggleMessageReactionUseCase = useCase,
            viewModelScope = this,
            onOptimisticReactionChanged = { _, _, newReaction, oldReaction ->
                optimisticNewReaction = newReaction
                optimisticOldReaction = oldReaction
            },
            onError = { _, _ -> }
        )

        val message = createTestMessage(userReaction = ReactionType.LIKE)

        delegate.toggleMessageReaction("msg-1", ReactionType.LOVE, listOf(message))

        assertEquals(ReactionType.LOVE, optimisticNewReaction)
        assertEquals(ReactionType.LIKE, optimisticOldReaction)
        assertEquals(ReactionType.LOVE.emoji, fakeRepo.lastToggledEmoji)
    }

    @Test
    fun `toggle same reaction removes reaction optimistically`() = testScope.runTest {
        val fakeRepo = FakeChatRepository()
        val useCase = ToggleMessageReactionUseCase(fakeRepo)

        var optimisticNewReaction: ReactionType? = null
        var optimisticOldReaction: ReactionType? = null

        val delegate = ChatReactionDelegate(
            toggleMessageReactionUseCase = useCase,
            viewModelScope = this,
            onOptimisticReactionChanged = { _, _, newReaction, oldReaction ->
                optimisticNewReaction = newReaction
                optimisticOldReaction = oldReaction
            },
            onError = { _, _ -> }
        )

        val message = createTestMessage(userReaction = ReactionType.LIKE)

        delegate.toggleMessageReaction("msg-1", ReactionType.LIKE, listOf(message))

        assertNull(optimisticNewReaction)
        assertEquals(ReactionType.LIKE, optimisticOldReaction)
    }

    @Test
    fun `usecase error triggers onError callback`() = testScope.runTest {
        val fakeRepo = FakeChatRepository().apply { shouldSucceed = false }
        val useCase = ToggleMessageReactionUseCase(fakeRepo)

        var errorOccurred = false

        val delegate = ChatReactionDelegate(
            toggleMessageReactionUseCase = useCase,
            viewModelScope = this,
            onOptimisticReactionChanged = { _, _, _, _ -> },
            onError = { errorMsg, _ ->
                errorOccurred = errorMsg != null
            }
        )

        val message = createTestMessage(userReaction = null)

        delegate.toggleMessageReaction("msg-1", ReactionType.LIKE, listOf(message))

        assertTrue(errorOccurred)
    }
}
