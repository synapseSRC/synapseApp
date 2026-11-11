package com.synapse.social.studioasinc.shared.domain.model

/**
 * Directional relationship between the authenticated account and another account on the
 * messaging path.
 *
 * The relationship is directional on purpose: the account that created the block keeps an
 * unblock control, while the other side is only told that messaging is unavailable. Keeping the
 * two directions apart is what lets the presentation layer render the correct copy without
 * re-querying the block relationship.
 */
enum class BlockStatus {
    /** No block exists in either direction. */
    NONE,

    /** The authenticated account blocked the other account. */
    BLOCKED_BY_ME,

    /** The other account blocked the authenticated account. */
    BLOCKED_ME,

    /** Both accounts blocked each other. */
    BLOCKED_BOTH;

    /** True when a block exists in either direction, so messaging must be refused. */
    val isBlocked: Boolean
        get() = this != NONE

    /** True when the authenticated account created a block against the other account. */
    val blockedByMe: Boolean
        get() = this == BLOCKED_BY_ME || this == BLOCKED_BOTH

    /** True when the other account created a block against the authenticated account. */
    val blockedByOther: Boolean
        get() = this == BLOCKED_ME || this == BLOCKED_BOTH
}
