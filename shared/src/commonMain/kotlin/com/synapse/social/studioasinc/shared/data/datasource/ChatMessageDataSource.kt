package com.synapse.social.studioasinc.shared.data.datasource
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.data.dto.chat.NewMessageDto
import com.synapse.social.studioasinc.shared.data.dto.chat.UserDeletedMessageDto
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class ChatMessageDataSource(private val client: SupabaseClientLib) {

    private fun getCurrentUserId(): String? = runCatching {
        client.pluginManager.getPluginOrNull(io.github.jan.supabase.auth.Auth)?.currentUserOrNull()?.id
    }.getOrNull()

    suspend fun getMessages(chatId: String, limit: Int = 50, before: String? = null, beforeId: String? = null): List<MessageDto> =
        getMessagesPage(chatId, limit, before, beforeId, null, null)

    suspend fun getMessagesAfter(chatId: String, limit: Int = 50, after: String, afterId: String): List<MessageDto> =
        getMessagesPage(chatId, limit, null, null, after, afterId)

    private suspend fun getMessagesPage(
        chatId: String,
        limit: Int = 50,
        before: String? = null,
        beforeId: String? = null,
        after: String? = null,
        afterId: String? = null
    ): List<MessageDto> = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId()
                ?: throw NotAuthenticatedException("User not authenticated")
            require(before == null || after == null) { "before and after cursors are mutually exclusive" }

            val expiresAt = com.synapse.social.studioasinc.shared.util.TimeProvider.nowInstant().toString()
            val serverMessages = client.postgrest.from("messages").select {
                filter {
                    eq("chat_id", chatId)
                    eq("is_deleted", false)
                    if (before != null || after != null) {
                        and {
                            or {
                                filter("expires_at", FilterOperator.IS, "null")
                                gt("expires_at", expiresAt)
                            }
                            when {
                                before != null && beforeId != null -> or {
                                    lt("created_at", before)
                                    and {
                                        eq("created_at", before)
                                        lt("id", beforeId)
                                    }
                                }
                                before != null -> lt("created_at", before)
                                after != null && afterId != null -> or {
                                    gt("created_at", after)
                                    and {
                                        eq("created_at", after)
                                        gt("id", afterId)
                                    }
                                }
                                after != null -> gt("created_at", after)
                            }
                        }
                    } else {
                        or {
                            filter("expires_at", FilterOperator.IS, "null")
                            gt("expires_at", expiresAt)
                        }
                    }
                }
                if (after != null) {
                    order("created_at", Order.ASCENDING)
                    order("id", Order.ASCENDING)
                } else {
                    order("created_at", Order.DESCENDING)
                    order("id", Order.DESCENDING)
                }
                limit(limit.toLong())
            }.decodeList<MessageDto>()

            val fetchedIds = serverMessages.mapNotNull { it.id }.distinct()
            val hiddenIds = getUserDeletedMessageIds(fetchedIds)
            val visibleMessages = filterOutUserDeletedMessages(serverMessages, hiddenIds)
            if (after != null) visibleMessages else visibleMessages.reversed()
        } catch (e: Exception) {
            Napier.e("Error loading messages for $chatId", e)
            throw e
        }
    }

    suspend fun getUserDeletedMessageIds(messageIds: List<String>): Set<String> =
        withContext(AppDispatchers.IO) {
            if (messageIds.isEmpty()) return@withContext emptySet()
            val currentUserId = getCurrentUserId()
                ?: throw NotAuthenticatedException("User not authenticated")

            client.postgrest.from("user_deleted_messages").select {
                filter {
                    eq("user_id", currentUserId)
                    isIn("message_id", messageIds.distinct())
                }
            }.decodeList<UserDeletedMessageDto>()
                .mapTo(mutableSetOf()) { it.messageId }
        }

    suspend fun sendMessage(
        chatId: String,
        content: String,
        mediaUrl: String? = null,
        messageType: String = "text",
        expiresAt: String? = null,
        replyToId: String? = null,
        attachments: List<com.synapse.social.studioasinc.shared.data.dto.chat.MessageAttachmentDto>? = null,
        metadata: String? = null
    ): MessageDto =
        withContext(AppDispatchers.IO) {
            val senderId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
            val newMessage = NewMessageDto(chatId, senderId, content, messageType, mediaUrl, expiresAt, replyToId, attachments = attachments, metadata = metadata)
            client.postgrest.from("messages").insert(newMessage) { select() }.decodeSingle<MessageDto>()
        }

    suspend fun getMessageById(messageId: String): MessageDto? = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.from("messages").select {
                filter { eq("id", messageId) }
                limit(1)
            }.decodeSingleOrNull<MessageDto>()
        } catch (e: Exception) {
            Napier.e("Error fetching message $messageId", e)
            null
        }
    }

    suspend fun editMessage(
        messageId: String,
        newContent: String
    ) = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.from("messages").update({
                set("content", newContent)
                set("is_edited", true)
            }) {
                filter { eq("id", messageId) }
            }
        } catch (e: Exception) {
            Napier.e("Error editing message", e)
            throw e
        }
    }

    suspend fun editMessageMetadata(messageId: String, metadata: String?) = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.from("messages").update({
                set("metadata", metadata)
            }) {
                filter { eq("id", messageId) }
            }
        } catch (e: Exception) {
            Napier.e("Error editing message metadata", e)
            throw e
        }
    }

    suspend fun markViewOnceConsumed(messageId: String) = withContext(AppDispatchers.IO) {
        try {
            val metadataJson = buildJsonObject {
                put("viewOnce", buildJsonObject {
                    put("isConsumed", true)
                })
            }
            client.postgrest.from("messages").update({
                set("metadata", metadataJson)
            }) {
                filter { eq("id", messageId) }
            }
        } catch (e: Exception) {
            Napier.e("Error marking view-once consumed", e)
            throw e
        }
    }

    suspend fun deleteMessage(messageId: String): Unit =
        withContext(AppDispatchers.IO) {
            try {
                client.postgrest.from("messages").update({
                    set("is_deleted", true)
                }) {
                    filter { eq("id", messageId) }
                }
            } catch (e: Exception) {
                Napier.e("Error deleting message", e)
                throw e
            }
        }

    suspend fun deleteMessages(messageIds: List<String>): Unit =
        withContext(AppDispatchers.IO) {
            try {
                client.postgrest.from("messages").update({
                    set("is_deleted", true)
                }) {
                    filter { isIn("id", messageIds) }
                }
            } catch (e: Exception) {
                Napier.e("Error deleting messages", e)
                throw e
            }
        }

    suspend fun deleteMessageForMe(messageId: String): Unit =
        withContext(AppDispatchers.IO) {
            val userId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
            val deletion = UserDeletedMessageDto(messageId = messageId, userId = userId)
            client.postgrest.from("user_deleted_messages").insert(deletion)
        }

    suspend fun deleteMessagesForMe(messageIds: List<String>): Unit =
        withContext(AppDispatchers.IO) {
            val userId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
            val deletions = messageIds.map { UserDeletedMessageDto(messageId = it, userId = userId) }
            client.postgrest.from("user_deleted_messages").insert(deletions)
        }

    suspend fun markMessagesAsRead(chatId: String) = withContext(AppDispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: return@withContext
            val now = com.synapse.social.studioasinc.shared.util.TimeProvider.nowInstant().toString()

            client.postgrest.from("chat_participants").update({
                set("last_read_at", now)
            }) {
                filter {
                    eq("chat_id", chatId)
                    eq("user_id", currentUserId)
                }
            }

            // Optimize: Call an RPC to perform a bulk update on the database side
            // This replaces 1 SELECT and M UPDATE queries with a single network call.
            client.postgrest.rpc(
                function = "mark_messages_as_read",
                parameters = mapOf(
                    "p_chat_id" to chatId,
                    "p_user_id" to currentUserId
                )
            )
        } catch (e: Exception) {
            Napier.e("Error marking messages as read", e)
        }
    }

    suspend fun markMessagesAsDelivered(chatId: String) = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.rpc(
                function = "mark_messages_as_delivered",
                parameters = mapOf(
                    "p_chat_id" to chatId
                )
            )
        } catch (e: Exception) {
            Napier.e("Error marking received messages as delivered", e)
        }
    }
}

internal fun filterOutUserDeletedMessages(
    messages: List<MessageDto>,
    deletedMessageIds: Set<String>
): List<MessageDto> = messages.filterNot { message ->
    message.id?.let(deletedMessageIds::contains) == true
}
