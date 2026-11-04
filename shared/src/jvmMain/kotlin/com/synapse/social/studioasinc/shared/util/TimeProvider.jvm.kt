package com.synapse.social.studioasinc.shared.util

import kotlin.time.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
actual object TimeProvider {
    actual fun nowMillis(): Long = System.currentTimeMillis()
    actual fun nowInstant(): Instant = Instant.parse(java.time.Instant.now().toString())
}
