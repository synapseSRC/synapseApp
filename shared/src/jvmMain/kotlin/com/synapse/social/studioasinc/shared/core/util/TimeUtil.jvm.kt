package com.synapse.social.studioasinc.shared.core.util

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
actual fun getCurrentIsoTime(): String = java.time.Instant.now().toString()
