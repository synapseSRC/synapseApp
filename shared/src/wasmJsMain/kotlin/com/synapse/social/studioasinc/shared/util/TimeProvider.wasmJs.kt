package com.synapse.social.studioasinc.shared.util

import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
actual object TimeProvider {
    actual fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
    actual fun nowInstant(): Instant = Clock.System.now()
}
