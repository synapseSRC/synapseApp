package com.synapse.social.studioasinc.shared.data.datasource
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers

import com.synapse.social.studioasinc.shared.data.dto.chat.MessageDto
import com.synapse.social.studioasinc.shared.data.dto.chat.NewMessageDto
import com.synapse.social.studioasinc.shared.data.dto.chat.UserDeletedMessageDto
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

internal class ChatMessageDataSource(private val client: SupabaseClientLib) {

    private fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getMessages(chatId: String, limit: Int = 50, before: String? = null): List<MessageDto> =
        withContext(AppDispatchers.IO) {
            try {
                client.postgrest.from("messages").select {
                    filter {
                        eq("chat_id", chatId)
                        eq("is_deleted", false)
                    or {
                        filter("expires_at", FilterOperator.IS, "null")
                        gt("expires_at", kotlinx.datetime.Clock.System.now().toString())
                    }
                    if (before != null) {
                        lt("created_at", before)
                    }
                }
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }.decodeList<MessageDto>().reversed()
            } catch (e: Exception) {
                Napier.e("Error loading messages for $chatId", e)
                emptyList()
            }
        }

    suspend fun sendMessage(
        chatId: String,
        content: String,
        mediaUrl: String? = null,
        messageType: String = "text",
        expiresAt: String? = null,
        replyToId: String? = null
    ): MessageDto =
        withContext(AppDispatchers.IO) {
            val senderId = getCurrentUserId() ?: throw NotAuthenticatedException("User not authenticated")
            val newMessage = NewMessageDto(chatId, senderId, content, messageType, mediaUrl, expiresAt, replyToId)
            client.postgrest.from("messages").insert(newMessage) { select() }.decodeSingle<MessageDto>()
        }

    suspend fun sendMessageNotification(recipientId: String, senderId: String, message: String, chatId: String) = withContext(AppDispatchers.IO) {
        try {
            client.functions.invoke("send-push-notification", buildJsonObject {
                put("recipient_id", recipientId)
                put("sender_id", senderId)
                put("message", message)
                put("type", "NEW_MESSAGE")
                putJsonObject("data") {
                    put("chat_id", chatId)
                }
            })
        } catch (e: Exception) {
            Napier.e("Failed to send notification", e)
        }
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
            val now = kotlinx.datetime.Clock.System.now().toString()

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
            val currentUserId = getCurrentUserId() ?: return@withContext
            client.postgrest.from("messages").update({
                set("delivery_status", "delivered")
            }) {
                filter {
                    eq("chat_id", chatId)
                    neq("sender_id", currentUserId)
                    eq("delivery_status", "sent")
                }
            }
        } catch (e: Exception) {
            Napier.e("Error marking messages as delivered", e)
        }
    }
}
