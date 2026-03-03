package com.synapse.social.studioasinc.shared.core.util

external class Date() {
    companion object {
        fun now(): Double
    }
    fun toISOString(): String
}

actual fun getCurrentTimeMillis(): Long {
    return Date.now().toLong()
}

actual fun getCurrentIsoTime(): String {
    return Date().toISOString()
}
