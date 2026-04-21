package com.synapse.social.studioasinc.shared.util

private fun cryptoRandomUUID(): String = js("self.crypto.randomUUID()")

actual object UUIDUtils {
    actual fun randomUUID(): String = cryptoRandomUUID()
}
