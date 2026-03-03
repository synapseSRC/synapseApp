package com.synapse.social.studioasinc.core.util



data class ScrollPositionState(
    val position: Int,
    val offset: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {

        const val DEFAULT_TIMEOUT_MS = 5 * 60 * 1000L
    }



    fun isExpired(timeout: Long = DEFAULT_TIMEOUT_MS): Boolean {
        return System.currentTimeMillis() - timestamp > timeout
    }
}
