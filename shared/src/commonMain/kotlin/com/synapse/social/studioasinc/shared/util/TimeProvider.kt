package com.synapse.social.studioasinc.shared.util

import kotlin.time.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
expect object TimeProvider {
    fun nowMillis(): Long
    fun nowInstant(): Instant
}
