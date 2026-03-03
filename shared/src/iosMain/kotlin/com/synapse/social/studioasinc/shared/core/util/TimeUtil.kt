package com.synapse.social.studioasinc.shared.core.util

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneWithAbbreviation

actual fun getCurrentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

actual fun getCurrentIsoTime(): String {
    val formatter = NSISO8601DateFormatter()
    val timeZone = NSTimeZone.timeZoneWithAbbreviation("UTC")
    if (timeZone != null) {
        formatter.timeZone = timeZone
    }
    return formatter.stringFromDate(NSDate())
}
