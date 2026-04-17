package com.synapse.social.studioasinc.shared.util

import java.util.UUID

actual object UUIDUtils {
    actual fun randomUUID(): String = UUID.randomUUID().toString()
}
