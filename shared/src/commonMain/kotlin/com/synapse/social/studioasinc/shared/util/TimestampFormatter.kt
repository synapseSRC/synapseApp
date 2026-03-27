package com.synapse.social.studioasinc.shared.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object TimestampFormatter {
    fun formatRelative(timestamp: String?): String {
        if (timestamp == null) return ""
        
        return try {
            val instant = Instant.parse(timestamp)
            val now = Clock.System.now()
            val diff = now.minus(instant).inWholeSeconds
            
            when {
                diff < 60 -> "Just now"
                diff < 3600 -> "${diff / 60}m ago"
                diff < 86400 -> {
                    val localTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                    "${localTime.hour.toString().padStart(2, '0')}:${localTime.minute.toString().padStart(2, '0')}"
                }
                diff < 172800 -> "Yesterday"
                else -> {
                    val localTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                    val month = when (localTime.monthNumber) {
                        1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
                        5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
                        9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
                        else -> ""
                    }
                    "$month ${localTime.dayOfMonth}"
                }
            }
        } catch (e: Exception) {
            timestamp
        }
    }
}
