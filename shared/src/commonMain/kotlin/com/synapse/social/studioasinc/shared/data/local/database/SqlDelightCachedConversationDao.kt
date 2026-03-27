package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.data.database.CachedConversation
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class SqlDelightCachedConversationDao(
    private val db: StorageDatabase
) : CachedConversationDao {

    override suspend fun upsertAll(conversations: List<Conversation>) = withContext(Dispatchers.Default) {
        val now = Clock.System.now().toEpochMilliseconds()
        db.transaction {
            conversations.forEach { conv ->
                db.cachedConversationQueries.upsertConversation(toCachedConversation(conv, now))
            }
        }
    }

    override suspend fun getAll(): List<Conversation> = withContext(Dispatchers.Default) {
        db.cachedConversationQueries.selectAll().executeAsList().map { toDomainConversation(it) }
    }

        override suspend fun deleteByChatId(chatId: String) {
        withContext(Dispatchers.Default) { db.cachedConversationQueries.deleteConversation(chatId) }
    }
    override suspend fun deleteAll() {
        withContext(Dispatchers.Default) {
            db.cachedConversationQueries.deleteAll()
        }
    }

    private fun toCachedConversation(domain: Conversation, cachedAt: Long): CachedConversation {
        return CachedConversation(
            chat_id = domain.chatId,
            participant_id = domain.participantId,
            participant_name = domain.participantName,
            participant_avatar = domain.participantAvatar,
            last_message = domain.lastMessage,
            last_message_time = domain.lastMessageTime,
            unread_count = domain.unreadCount.toLong(),
            is_online = if (domain.isOnline) 1L else 0L,
            is_group = if (domain.isGroup) 1L else 0L,
            cached_at = cachedAt
        )
    }

    private fun toDomainConversation(cached: CachedConversation): Conversation {
        return Conversation(
            chatId = cached.chat_id,
            participantId = cached.participant_id,
            participantName = cached.participant_name,
            participantAvatar = cached.participant_avatar,
            lastMessage = cached.last_message,
            lastMessageTime = cached.last_message_time,
            unreadCount = cached.unread_count.toInt(),
            isOnline = cached.is_online == 1L,
            isGroup = cached.is_group == 1L,
            groupMembers = emptyList() // Not cached currently
        )
    }
}
