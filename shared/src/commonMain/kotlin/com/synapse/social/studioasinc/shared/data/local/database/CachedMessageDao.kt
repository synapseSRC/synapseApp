package com.synapse.social.studioasinc.shared.data.local.database

import com.synapse.social.studioasinc.shared.domain.model.chat.Message

interface CachedMessageDao {
    suspend fun upsertAll(messages: List<Message>)
    suspend fun getMessages(chatId: String, limit: Int = 50): List<Message>
    suspend fun upsert(message: Message)
    suspend fun updateContent(id: String, content: String)
    suspend fun markDeleted(id: String)
    suspend fun markDeleted(ids: List<String>)
    suspend fun trimToLimit(chatId: String, limit: Int = 50)
    suspend fun deleteAll()
}
