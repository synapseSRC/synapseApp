package com.synapse.social.studioasinc.shared.core.util

import kotlinx.datetime.Clock

actual fun getCurrentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
actual fun getCurrentIsoTime(): String = Clock.System.now().toString()
