package com.synapse.social.studioasinc.feature.inbox.inbox

import com.synapse.social.studioasinc.feature.inbox.inbox.models.ChatListItem
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object ChatItemsMapper {
    fun buildChatItems(messages: List<Message>, currentUserId: String): List<ChatListItem> {
        val sorted = messages.sortedBy { it.createdAt }
        val result = mutableListOf<ChatListItem>()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        var lastDate: LocalDate? = null

        // Find first unread index
        val firstUnreadIndex = sorted.indexOfFirst { currentUserId !in it.readBy && it.senderId != currentUserId }
        val hasReadBefore = firstUnreadIndex > 0
        val unreadCount = if (firstUnreadIndex >= 0) sorted.size - firstUnreadIndex else 0

        sorted.forEachIndexed { index, message ->
            val msgDate = Instant.parse(message.createdAt).atZone(zone).toLocalDate()
            if (msgDate != lastDate) {
                val label = when {
                    msgDate == today -> "Today"
                    msgDate == today.minusDays(1) -> "Yesterday"
                    msgDate.isAfter(today.minusDays(7)) -> msgDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    else -> msgDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                }
                result.add(ChatListItem.DateDivider(label))
                lastDate = msgDate
            }
            if (index == firstUnreadIndex && hasReadBefore) {
                result.add(ChatListItem.UnreadDivider(unreadCount))
            }
            result.add(ChatListItem.MessageItem(message))
        }
        return result
    }
}
