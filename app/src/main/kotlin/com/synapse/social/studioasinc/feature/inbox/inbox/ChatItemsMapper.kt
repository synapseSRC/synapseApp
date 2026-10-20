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

    private fun normalizeTimestampString(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length < 19) return trimmed
        val withT = if (trimmed.length > 10 && trimmed[10] == ' ') {
            trimmed.substring(0, 10) + 'T' + trimmed.substring(11)
        } else {
            trimmed
        }
        val len = withT.length
        if (len >= 3) {
            val sign = withT[len - 3]
            val d1 = withT[len - 2]
            val d2 = withT[len - 1]
            if ((sign == '+' || sign == '-') && d1.isDigit() && d2.isDigit()) {
                return if (d1 == '0' && d2 == '0') withT.substring(0, len - 3) + "Z" else withT + ":00"
            }
        }
        return withT
    }

    private fun safeParseInstant(raw: String): Instant? {
        if (raw.isBlank()) return null
        return try {
            Instant.parse(raw)
        } catch (_: Exception) {
            try {
                Instant.parse(normalizeTimestampString(raw))
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun safeParseDate(raw: String, zone: ZoneId, fallback: LocalDate): LocalDate {
        return safeParseInstant(raw)?.atZone(zone)?.toLocalDate() ?: fallback
    }

    fun buildChatItems(messages: List<Message>, currentUserId: String): List<ChatListItem> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val sorted = messages.sortedWith(
            compareBy<Message> { safeParseInstant(it.createdAt)?.toEpochMilli() ?: 0L }
                .thenBy { it.createdAt }
                .thenBy { it.clientId }
        )
        val result = mutableListOf<ChatListItem>()
        var lastDate: LocalDate? = null

        // Find first unread index
        val firstUnreadIndex = sorted.indexOfFirst { currentUserId !in it.readBy && it.senderId != currentUserId }
        val hasReadBefore = firstUnreadIndex > 0
        val unreadCount = if (firstUnreadIndex >= 0) sorted.size - firstUnreadIndex else 0

        sorted.forEachIndexed { index, message ->
            val msgDate = safeParseDate(message.createdAt, zone, lastDate ?: today)
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
