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

    suspend fun getMessagesByIds(chatId: String, messageIds: List<String>): List<MessageDto> =
        withContext(AppDispatchers.IO) {
            getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
            messageIds.distinct().chunked(50).flatMap { ids ->
                client.postgrest.from("messages").select {
                    filter {
                        eq("chat_id", chatId)
                        isIn("id", ids)
                    }
                }.decodeList<MessageDto>()
            }
        }

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
            val visibleMessages = mutableListOf<MessageDto>()
            var cursorBefore = before
            var cursorBeforeId = beforeId
            var cursorAfter = after
            var cursorAfterId = afterId

            while (visibleMessages.size < limit) {
                val remaining = limit - visibleMessages.size
                val serverMessages = client.postgrest.from("messages").select {
                    filter {
                        eq("chat_id", chatId)
                        eq("is_deleted", false)
                        if (cursorBefore != null || cursorAfter != null) {
                            and {
                                or {
                                    filter("expires_at", FilterOperator.IS, "null")
                                    gt("expires_at", expiresAt)
                                }
                                when {
                                    cursorBefore != null && cursorBeforeId != null -> or {
                                        lt("created_at", cursorBefore)
                                        and {
                                            eq("created_at", cursorBefore)
                                            lt("id", cursorBeforeId)
                                        }
                                    }
                                    cursorBefore != null -> lt("created_at", cursorBefore)
                                    cursorAfter != null && cursorAfterId != null -> or {
                                        gt("created_at", cursorAfter)
                                        and {
                                            eq("created_at", cursorAfter)
                                            gt("id", cursorAfterId)
                                        }
                                    }
                                    cursorAfter != null -> gt("created_at", cursorAfter)
                                }
                            }
                        } else {
                            or {
                                filter("expires_at", FilterOperator.IS, "null")
                                gt("expires_at", expiresAt)
                            }
                        }
                    }
                    if (cursorAfter != null) {
                        order("created_at", Order.ASCENDING)
                        order("id", Order.ASCENDING)
                    } else {
                        order("created_at", Order.DESCENDING)
                        order("id", Order.DESCENDING)
                    }
                    limit(remaining.toLong())
                }.decodeList<MessageDto>()

                if (serverMessages.isEmpty()) break
                val fetchedIds = serverMessages.mapNotNull { it.id }.distinct()
                val hiddenIds = getUserDeletedMessageIds(fetchedIds)
                visibleMessages.addAll(filterOutUserDeletedMessages(serverMessages, hiddenIds))
                if (visibleMessages.size >= limit || serverMessages.size < remaining) break

                val lastRawMessage = serverMessages.last()
                val nextAt = lastRawMessage.createdAt ?: break
                val nextId = lastRawMessage.id ?: break
                if (cursorAfter != null) {
                    val progressed = if (cursorAfterId != null) {
                        compareMessageCursor(nextAt, nextId, cursorAfter, cursorAfterId) > 0
                    } else {
                        nextAt > cursorAfter
                    }
                    if (!progressed) break
                    cursorAfter = nextAt
                    cursorAfterId = nextId
                } else {
                    val progressed = if (cursorBefore != null && cursorBeforeId != null) {
                        compareMessageCursor(nextAt, nextId, cursorBefore, cursorBeforeId) < 0
                    } else if (cursorBefore != null) {
                        nextAt < cursorBefore
                    } else {
                        true
                    }
                    if (!progressed) break
                    cursorBefore = nextAt
                    cursorBeforeId = nextId
                }
            }
            if (after != null) visibleMessages.take(limit) else visibleMessages.take(limit).reversed()
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
        metadata: String? = null,
        deviceId: String? = null
    ): MessageDto =
        withContext(AppDispatchers.IO) {
            val senderId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
            val newMessage = NewMessageDto(chatId, senderId, content, messageType, mediaUrl, expiresAt, replyToId, attachments = attachments, metadata = metadata)
            val result = client.postgrest.from("messages").insert(newMessage) { select() }.decodeSingle<MessageDto>()
            
            // If deviceId is provided, update the message with it
            if (deviceId != null && result.id != null) {
                try {
                    client.postgrest.from("messages").update({
                        set("device_id", deviceId)
                    }) {
                        filter { eq("id", result.id) }
                    }
                } catch (e: Exception) {
                    Napier.w("Failed to update device_id for message ${result.id}", e)
                }
            }
            
            result
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
                    // Delete-for-everyone must not leave the payload readable to participants.
                    set("is_deleted", true)
                    set("content", "")
                    set("media_url", null as String?)
                    set("metadata", null as String?)
                    set("attachments", null as String?)
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
                    set("content", "")
                    set("media_url", null as String?)
                    set("metadata", null as String?)
                    set("attachments", null as String?)
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

    suspend fun markMessagesAsRead(chatId: String, deviceId: String? = null, shareReceipt: Boolean = true) = withContext(AppDispatchers.IO) {
        val currentUserId = getCurrentUserId()
            ?: throw NotAuthenticatedException("User not authenticated")
        if (shareReceipt) {
            val rpcParams = mutableMapOf(
                "p_chat_id" to chatId,
                "p_user_id" to currentUserId
            )
            if (deviceId != null) rpcParams["p_device_id"] = deviceId
            client.postgrest.rpc(
                function = "mark_messages_as_read",
                parameters = rpcParams
            )
        } else {
            // Receipts are off: acknowledge arrival only, so no read_by/read_at ever reaches the sender.
            markMessagesAsDelivered(chatId, deviceId)
        }
        val now = com.synapse.social.studioasinc.shared.util.TimeProvider.nowInstant().toString()
        client.postgrest.from("chat_participants").update({
            set("last_read_at", now)
            if (deviceId != null) set("last_read_device_id", deviceId)
        }) {
            filter {
                eq("chat_id", chatId)
                eq("user_id", currentUserId)
            }
        }
        Unit
    }

    suspend fun markMessagesAsDelivered(chatId: String, deviceId: String? = null) = withContext(AppDispatchers.IO) {
        getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
        val rpcParams = mutableMapOf("p_chat_id" to chatId)
        if (deviceId != null) {
            rpcParams["p_device_id"] = deviceId
        }
        client.postgrest.rpc(
            function = "mark_messages_as_delivered",
            parameters = rpcParams
        )
        Unit
    }

    suspend fun insertSystemMessage(chatId: String, content: String): MessageDto = withContext(AppDispatchers.IO) {
        try {
            val senderId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
            val newMessage = NewMessageDto(
                chatId = chatId,
                senderId = senderId,
                content = content,
                messageType = "system",
                expiresAt = null, // System messages don't expire
                replyToId = null,
                attachments = null,
                metadata = null
            )
            client.postgrest.from("messages").insert(newMessage) { select() }.decodeSingle<MessageDto>()
        } catch (e: Exception) {
            Napier.e("Error inserting system message", e)
            throw e
        }
    }

    suspend fun deleteExpiredMessages(chatId: String): Int = withContext(AppDispatchers.IO) {
        try {
            val now = com.synapse.social.studioasinc.shared.util.TimeProvider.nowInstant().toString()
            val result = client.postgrest.from("messages").update({
                set("is_deleted", true)
            }) {
                filter {
                    eq("chat_id", chatId)
                    eq("is_deleted", false)
                    lt("expires_at", now)
                }
            }
            // Supabase doesn't return affected count, so we estimate based on success
            1
        } catch (e: Exception) {
            Napier.e("Error deleting expired messages", e)
            0
        }
    }
}


private fun compareMessageCursor(leftCreatedAt: String, leftId: String, rightCreatedAt: String, rightId: String): Int {
    val timeComparison = try {
        kotlinx.datetime.Instant.parse(leftCreatedAt).compareTo(kotlinx.datetime.Instant.parse(rightCreatedAt))
    } catch (_: Exception) {
        leftCreatedAt.compareTo(rightCreatedAt)
    }
    return if (timeComparison != 0) timeComparison else leftId.compareTo(rightId)
}

internal fun filterOutUserDeletedMessages(
    messages: List<MessageDto>,
    deletedMessageIds: Set<String>
): List<MessageDto> = messages.filterNot { message ->
    message.id?.let(deletedMessageIds::contains) == true
}
