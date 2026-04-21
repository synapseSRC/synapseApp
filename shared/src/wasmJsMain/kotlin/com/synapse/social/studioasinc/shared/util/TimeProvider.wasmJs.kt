package com.synapse.social.studioasinc.shared.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
actual object TimeProvider {
    actual fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
    actual fun nowInstant(): Instant = Clock.System.now()
}
