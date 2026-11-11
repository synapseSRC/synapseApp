package com.synapse.social.studioasinc.shared.domain.model

/**
 * Raised by the messaging layer when a block forbids a message from being sent or delivered.
 *
 * The carried [blockStatus] keeps the two sides distinguishable so the presentation layer can
 * decide whether an unblock control belongs on screen without re-querying the relationship. The
 * messages are deliberately neutral for the side that did not create the block: the blocked
 * account must not be told that the other participant blocked it.
 */
class BlockedConversationException(
    val blockStatus: BlockStatus
) : IllegalStateException(
    if (blockStatus.blockedByMe) {
        "Messaging is disabled because you blocked this account"
    } else {
        "Messaging is currently unavailable"
    }
)
