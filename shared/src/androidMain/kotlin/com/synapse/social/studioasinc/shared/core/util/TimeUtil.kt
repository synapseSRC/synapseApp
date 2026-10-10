package com.synapse.social.studioasinc.shared.core.util

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

actual fun getCurrentIsoTime(): String {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        java.time.Instant.now().toString()
    } else {

        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())
    }
}
