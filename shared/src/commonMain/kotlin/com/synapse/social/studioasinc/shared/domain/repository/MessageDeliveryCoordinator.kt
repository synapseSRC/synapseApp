package com.synapse.social.studioasinc.shared.domain.repository

/** Receives and acknowledges messages for the signed-in session, never marking them read. */
interface MessageDeliveryCoordinator {
    /** Starts one account-scoped receiver. Repeated calls are safe, including during session restoration. */
    fun start()

    /** Requests catch-up after foreground resume or a push hint; no payload is trusted as a receipt. */
    fun requestSync()

    /** Runs bounded catch-up for background work. Failures must be retried by the caller. */
    suspend fun synchronize(): Result<Unit>
}
