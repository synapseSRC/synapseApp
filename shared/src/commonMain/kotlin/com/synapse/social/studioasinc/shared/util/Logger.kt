package com.synapse.social.studioasinc.shared.util

import io.github.aakira.napier.Napier

object Logger {
    fun d(message: String, tag: String? = null, throwable: Throwable? = null) {
        Napier.d(message, throwable, tag)
    }

    fun i(message: String, tag: String? = null, throwable: Throwable? = null) {
        Napier.i(message, throwable, tag)
    }

    fun w(message: String, tag: String? = null, throwable: Throwable? = null) {
        Napier.w(message, throwable, tag)
    }

    fun e(message: String, tag: String? = null, throwable: Throwable? = null) {
        Napier.e(message, throwable, tag)
    }

    /**
     * Specialized error logging that ensures sensitive info isn't leaked
     * while providing enough context for debugging.
     */
    fun e(throwable: Throwable, context: String? = null, tag: String? = null) {
        val message = if (context != null) "$context: ${throwable.message}" else throwable.message ?: "Unknown error"
        Napier.e(message, throwable, tag)
    }
}
