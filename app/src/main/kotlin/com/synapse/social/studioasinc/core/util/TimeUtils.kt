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



    private val fallbackFormatter = java.time.format.DateTimeFormatter.ofPattern("[yyyy-MM-dd['T'][ ]HH:mm:ss[.SSSSSS][.SSS][.SS][.S][X]]").withZone(java.time.ZoneId.of("UTC"))

    fun getTimeAgo(isoTimestamp: String): String {
        if (isoTimestamp.isBlank()) return "1s"

        try {
            // Clean and normalize the timestamp string
            val normalized = isoTimestamp.trim()
                .replace(" ", "T") // Handle '2023-01-01 12:00:00' format
            
            // Try parsing as OffsetDateTime first (most common for Supabase)
            val timestamp = when {
                normalized.contains("+") || normalized.endsWith("Z") || (normalized.lastIndexOf("-") > 10) -> {
                    try {
                        java.time.OffsetDateTime.parse(normalized).toInstant().toEpochMilli()
                    } catch (e: Exception) {
                        // If it has 'T' and an offset but fails, try to strip fractional seconds if they are too long
                        if (normalized.contains(".")) {
                            val base = normalized.substringBefore(".")
                            val suffix = normalized.substringAfterLast("+", normalized.substringAfterLast("Z", ""))
                            val connector = if (normalized.contains("+")) "+" else if (normalized.endsWith("Z")) "Z" else ""
                            try {
                                java.time.OffsetDateTime.parse("${base}${connector}${suffix}").toInstant().toEpochMilli()
                            } catch (e2: Exception) {
                                null
                            }
                        } else null
                    }
                }
                else -> {
                    // Try parsing as LocalDateTime (no timezone info)
                    try {
                        val cleanTimestamp = normalized.substringBefore('.')
                        java.time.LocalDateTime.parse(cleanTimestamp, fallbackFormatter)
                            .atZone(java.time.ZoneId.of("UTC"))
                            .toInstant()
                            .toEpochMilli()
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            if (timestamp != null) {
                return formatTimestamp(timestamp)
            }
        } catch (e: Exception) {
            // Log error if needed: android.util.Log.e("TimeUtils", "Error parsing timestamp: $isoTimestamp", e)
        }

        // Final fallback
        return "1s"
    }
}
