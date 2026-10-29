package com.synapse.social.studioasinc.shared.util

import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object TimestampFormatter {

    fun parseInstant(timestamp: String?): Instant? {
        if (timestamp.isNullOrBlank()) return null
        val numeric = timestamp.toLongOrNull()
        if (numeric != null) {
            return if (numeric > 100_000_000_000L) {
                Instant.fromEpochMilliseconds(numeric)
            } else {
                Instant.fromEpochSeconds(numeric)
            }
        }
        var s = timestamp.trim()
        if (s.length >= 11 && s[10] == ' ') {
            s = s.substring(0, 10) + 'T' + s.substring(11)
        }
        val shortTzMatch = Regex("""([+-]\d{2})$""").find(s)
        if (shortTzMatch != null) {
            s = s.substring(0, shortTzMatch.range.first) + shortTzMatch.value + ":00"
        } else if (!s.endsWith("Z", ignoreCase = true) && !Regex("""[+-]\d{2}:\d{2}$""").containsMatchIn(s)) {
            s += "Z"
        }
        return try {
            Instant.parse(s)
        } catch (_: Exception) {
            null
        }
    }

    fun isWithinActiveWindow(lastSeen: String?, windowMinutes: Long = 2): Boolean {
        if (lastSeen.isNullOrBlank()) return false
        val instant = parseInstant(lastSeen) ?: return false
        val now = Clock.System.now()
        val diffSeconds = now.epochSeconds - instant.epochSeconds
        // Allow up to 60s clock skew into future, and up to windowMinutes into past
        return diffSeconds >= -60 && diffSeconds <= (windowMinutes * 60)
    }

    fun formatRelative(timestamp: String?): String {
        if (timestamp == null) return ""
        val instant = parseInstant(timestamp) ?: return timestamp
        val now = Clock.System.now()
        val diff = now.epochSeconds - instant.epochSeconds

        return when {
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
    }

    fun formatLastSeen(timestamp: String?): String {
        if (timestamp.isNullOrBlank()) return ""
        val instant = parseInstant(timestamp) ?: return ""
        val now = Clock.System.now()
        val diff = now.epochSeconds - instant.epochSeconds

        return when {
            diff < 60 -> "Just now"
            diff < 3600 -> "${diff / 60}m ago"
            diff < 86400 -> {
                val localTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                val hour = localTime.hour
                val minute = localTime.minute.toString().padStart(2, '0')
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                "Today at $displayHour:$minute $amPm"
            }
            diff < 172800 -> {
                val localTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                val hour = localTime.hour
                val minute = localTime.minute.toString().padStart(2, '0')
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                "Yesterday at $displayHour:$minute $amPm"
            }
            else -> {
                val localTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                val month = when (localTime.monthNumber) {
                    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
                    5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
                    9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
                    else -> ""
                }
                val hour = localTime.hour
                val minute = localTime.minute.toString().padStart(2, '0')
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                "$month ${localTime.dayOfMonth} at $displayHour:$minute $amPm"
            }
        }
    }
}
