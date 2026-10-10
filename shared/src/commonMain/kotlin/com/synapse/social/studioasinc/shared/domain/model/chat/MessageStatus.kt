package com.synapse.social.studioasinc.shared.domain.model.chat

/**
 * Represents the delivery and read status of a message.
 *
 * Status progression:
 * PENDING → SENT → DELIVERED → READ
 *
 * Or: PENDING → FAILED (if sending fails)
 */
enum class MessageStatus {
    /**
     * Message is queued locally, waiting to be sent.
     * Occurs when offline or encryption is in progress.
     */
    PENDING,

    /**
     * Message has been successfully sent to the server.
     * Server has acknowledged receipt of the encrypted message.
     */
    SENT,

    /**
     * Message has been delivered to recipient's device(s).
     * At least one recipient device has fetched the message.
     */
    DELIVERED,

    /**
     * Message has been read by the recipient.
     * Recipient has opened the chat and viewed the message.
     */
    READ,

    /**
     * Message failed to send.
     * Could be due to network error, encryption failure, or server rejection.
     */
    FAILED
}
