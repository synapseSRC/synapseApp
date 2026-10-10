package com.synapse.social.studioasinc.shared.domain.model.chat

/**
 * Defines the type of chat conversation.
 *
 * - **ONE_TO_ONE**: Private conversation between two users with pairwise encryption (X3DH + Double Ratchet)
 * - **GROUP**: Multi-member chat where all participants can send messages (Sender Keys protocol)
 * - **CHANNEL**: Broadcast-only chat where only owner/admins can send messages (Broadcast encryption)
 */
enum class ChatType {
    /**
     * 1-to-1 private conversation.
     * Uses Signal Protocol X3DH for initial key agreement and Double Ratchet for ongoing encryption.
     */
    ONE_TO_ONE,

    /**
     * Group chat with multiple participants.
     * All members can send and receive messages.
     * Uses Sender Keys protocol for efficient group encryption (O(1) instead of O(n)).
     */
    GROUP,

    /**
     * Broadcast channel (one-to-many).
     * Only owner/admins can send messages, subscribers can only read.
     * Uses broadcast encryption where owner encrypts once for all subscribers.
     */
    CHANNEL
}
