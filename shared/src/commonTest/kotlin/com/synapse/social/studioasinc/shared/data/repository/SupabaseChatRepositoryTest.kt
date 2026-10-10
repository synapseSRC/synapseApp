package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.model.UploadResult
import com.synapse.social.studioasinc.shared.domain.model.chat.ContentStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.DeliveryStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.chat.MessageType
import com.synapse.social.studioasinc.shared.domain.repository.MediaUploadRepository
import io.github.jan.supabase.createSupabaseClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SupabaseChatRepositoryTest {

    private class FakeMediaUploadRepository : MediaUploadRepository {
        override suspend fun upload(
            filePath: String,
            provider: StorageProvider,
            config: StorageConfig,
            bucketName: String?,
            onProgress: (Float) -> Unit
        ): Result<UploadResult> {
            return Result.failure(Exception("Not implemented"))
        }

        override fun deleteFile(filePath: String) {}

        override suspend fun deleteImgBbImages(deleteUrls: List<String>) {}
    }

    private fun createRepository(): SupabaseChatRepository {
        val dummyClient = createSupabaseClient(
            supabaseUrl = "https://placeholder.supabase.co",
            supabaseKey = "placeholder-anon-key"
        ) {}
        val dummyDataSource = SupabaseChatDataSource(dummyClient)
        return SupabaseChatRepository(
            dataSource = dummyDataSource,
            client = dummyClient,
            mediaUploadRepository = FakeMediaUploadRepository()
        )
    }

    private fun createDummyMessage(
        id: String,
        sentAt: String,
        deliveryStatus: DeliveryStatus = DeliveryStatus.SENT,
        contentStatus: ContentStatus = ContentStatus.ACTIVE
    ): Message {
        return Message(
            id = id,
            chatId = "chat_1",
            senderId = "user_1",
            content = "Message $id",
            mediaUrl = null,
            messageType = MessageType.TEXT,
            sentAt = sentAt,
            createdAt = sentAt,
            deliveryStatus = deliveryStatus,
            contentStatus = contentStatus,
            isDeleted = contentStatus == ContentStatus.DELETED
        )
    }

    @Test
    fun testReconcileServerMessagesWithCache_preservesOlderMessagesNotReturnedInLimitedWindow() {
        val repo = createRepository()

        val cachedOld1 = createDummyMessage("msg_old_1", "2026-01-01T10:00:00Z")
        val cachedOld2 = createDummyMessage("msg_old_2", "2026-01-01T10:05:00Z")
        val cachedRecent1 = createDummyMessage("msg_recent_1", "2026-01-01T12:00:00Z")
        val cachedRecent2 = createDummyMessage("msg_recent_2", "2026-01-01T12:05:00Z")

        val cached = listOf(cachedOld1, cachedOld2, cachedRecent1, cachedRecent2)

        val freshServerMessages = listOf(
            createDummyMessage("msg_recent_1", "2026-01-01T12:00:00Z"),
            createDummyMessage("msg_recent_2", "2026-01-01T12:05:00Z")
        )

        val deletedIds = repo.reconcileServerMessagesWithCache(cached, freshServerMessages)

        assertFalse(deletedIds.contains("msg_old_1"))
        assertFalse(deletedIds.contains("msg_old_2"))
        assertTrue(deletedIds.isEmpty())
    }

    @Test
    fun testReconcileServerMessagesWithCache_detectsDeletionsWithinServerWindow() {
        val repo = createRepository()

        val cached1 = createDummyMessage("msg_1", "2026-01-01T12:00:00Z")
        val cachedDeletedOnServer = createDummyMessage("msg_2", "2026-01-01T12:02:00Z")
        val cached3 = createDummyMessage("msg_3", "2026-01-01T12:05:00Z")

        val cached = listOf(cached1, cachedDeletedOnServer, cached3)

        val freshServerMessages = listOf(
            createDummyMessage("msg_1", "2026-01-01T12:00:00Z"),
            createDummyMessage("msg_3", "2026-01-01T12:05:00Z")
        )

        val deletedIds = repo.reconcileServerMessagesWithCache(cached, freshServerMessages)

        assertTrue(deletedIds.contains("msg_2"))
        assertEquals(1, deletedIds.size)
    }

    @Test
    fun testReconcileServerMessagesWithCache_preservesPendingSendingMessages() {
        val repo = createRepository()

        val cachedSending = createDummyMessage(
            "msg_sending",
            "2026-01-01T12:03:00Z",
            deliveryStatus = DeliveryStatus.SENDING
        )

        val cached = listOf(
            createDummyMessage("msg_1", "2026-01-01T12:00:00Z"),
            cachedSending
        )

        val freshServerMessages = listOf(
            createDummyMessage("msg_1", "2026-01-01T12:00:00Z")
        )

        val deletedIds = repo.reconcileServerMessagesWithCache(cached, freshServerMessages)

        assertFalse(deletedIds.contains("msg_sending"))
    }
}
