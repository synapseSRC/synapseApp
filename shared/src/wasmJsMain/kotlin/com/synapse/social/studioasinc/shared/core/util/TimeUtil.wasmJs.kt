package com.synapse.social.studioasinc.shared.core.util

import kotlin.time.Clock

actual fun getCurrentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
actual fun getCurrentIsoTime(): String = Clock.System.now().toString()
