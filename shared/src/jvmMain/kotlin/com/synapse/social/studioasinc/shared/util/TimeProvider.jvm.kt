package com.synapse.social.studioasinc.shared.util

import kotlinx.datetime.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
actual object TimeProvider {
    actual fun nowMillis(): Long = System.currentTimeMillis()
    actual fun nowInstant(): Instant = kotlinx.datetime.Clock.System.now()
}
