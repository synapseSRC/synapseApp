package com.synapse.social.studioasinc.shared.util

import kotlinx.datetime.Instant

@OptIn(kotlin.time.ExperimentalTime::class)
expect object TimeProvider {
    fun nowMillis(): Long
    fun nowInstant(): Instant
}
