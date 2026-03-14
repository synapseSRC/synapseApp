package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.widget.TextView
import com.synapse.social.studioasinc.R
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    fun setTime(currentTime: Double, textView: TextView, context: Context) {
        val c1 = Calendar.getInstance()
        val c2 = Calendar.getInstance()
        val timeDiff = c1.timeInMillis - currentTime

        when {
            timeDiff < 60000 -> {
                val seconds = (timeDiff / 1000).toLong()
                if (seconds < 2) {
                    textView.text = "1s"
                } else {
                    textView.text = "${seconds}s"
                }
            }
            timeDiff < (60 * 60000) -> {
                val minutes = (timeDiff / 60000).toLong()
                textView.text = "${minutes}m"
            }
            timeDiff < (24 * 60 * 60000) -> {
                val hours = (timeDiff / (60 * 60000)).toLong()
                textView.text = "${hours}h"
            }
            timeDiff < (7 * 24 * 60 * 60000) -> {
                val days = (timeDiff / (24 * 60 * 60000)).toLong()
                textView.text = "${days}d"
            }
            else -> {
                val weeks = (timeDiff / (7 * 24 * 60 * 60000)).toLong()
                textView.text = "${weeks}w"
            }
        }
    }



    fun formatTimestamp(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - timestamp

        return when {
            diff < 0 -> "1s"
            diff < 60_000 -> "${(diff / 1000).coerceAtLeast(1)}s"
            diff < 3600_000 -> "${diff / 60_000}m"
            diff < 86400_000 -> "${diff / 3600_000}h"
            diff < 604800_000 -> "${diff / 86400_000}d"
            else -> "${diff / 604800_000}w"
        }
    }



    private val fallbackFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(java.time.ZoneId.of("UTC"))

    fun getTimeAgo(isoTimestamp: String): String {
        if (isoTimestamp.isBlank()) return "1s"

        // Fast path for ISO timestamps from Supabase
        if (isoTimestamp.length > 10 && isoTimestamp[10] == 'T') {
            try {
                // If it ends with Z or has an offset
                if (isoTimestamp.endsWith("Z") || isoTimestamp.contains("+") || (isoTimestamp.indexOf("-", 11) != -1)) {
                    val odt = java.time.OffsetDateTime.parse(isoTimestamp)
                    return formatTimestamp(odt.toInstant().toEpochMilli())
                }

                // Fallback for timezone-less strings
                val cleanTimestamp = isoTimestamp.substringBefore('+').substringBefore('Z').substringBefore('.')
                val localDateTime = java.time.LocalDateTime.parse(cleanTimestamp, fallbackFormatter)
                val timestamp = localDateTime.atZone(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
                return formatTimestamp(timestamp)
            } catch (e: Exception) {
                // Ignore parsing errors and return default
            }
        }

        // Final fallback if parsing failed or string format is totally unrecognized
        return "1s"
    }
}
