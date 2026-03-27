package com.synapse.social.studioasinc.shared.util

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

@OptIn(kotlin.time.ExperimentalTime::class)
internal object DateFormatterUtil {
    val amzDateFormatter = LocalDateTime.Format {
        year()
        monthNumber(Padding.ZERO)
        dayOfMonth(Padding.ZERO)
        char('T')
        hour(Padding.ZERO)
        minute(Padding.ZERO)
        second(Padding.ZERO)
        char('Z')
    }

    fun formatAmzDate(instant: Instant): String {
        val dateTime = instant.toLocalDateTime(TimeZone.UTC)
        return amzDateFormatter.format(dateTime)
    }
}
