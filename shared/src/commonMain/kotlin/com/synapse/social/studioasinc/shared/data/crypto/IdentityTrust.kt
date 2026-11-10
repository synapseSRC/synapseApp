package com.synapse.social.studioasinc.shared.data.crypto

/**
 * Local trust state for a peer's Signal identity key.
 *
 * The decision is derived only from what this device already stored, so it stays free of Android
 * and network types and can be unit tested on every target. Synapse trusts the first key it sees
 * (trust-on-first-use) and never silently adopts a replacement afterwards: adopting the new key
 * would erase both the evidence of the change and any safety-number verification the user made.
 */
enum class IdentityTrustOutcome {
    /** No identity key was stored for this peer yet. Trust-on-first-use. */
    FIRST_USE,

    /** The presented key is the one already stored for this peer. */
    UNCHANGED,

    /** The peer presented a different key than the stored one and the user has not accepted it. */
    CHANGED;

    /** Whether this outcome allows Signal session work (session build, encrypt, decrypt). */
    val isUsable: Boolean
        get() = this != CHANGED
}

/** A detected identity-key change, kept until the user re-verifies or accepts the new key. */
data class PendingIdentityChange(
    val peerId: String,
    val previousIdentityKey: String?,
    val currentIdentityKey: String,
    val detectedAtMs: Long
)

/** Pure comparison of the stored identity key with the one presented now. */
object IdentityTrustPolicy {

    fun decide(storedIdentityKey: String?, presentedIdentityKey: String): IdentityTrustOutcome {
        val presented = presentedIdentityKey.trim()
        // A blank key cannot be compared, and must never be trusted.
        if (presented.isEmpty()) return IdentityTrustOutcome.CHANGED
        val stored = storedIdentityKey?.takeIf { it.isNotBlank() } ?: return IdentityTrustOutcome.FIRST_USE
        return if (stored == presented) IdentityTrustOutcome.UNCHANGED else IdentityTrustOutcome.CHANGED
    }

    fun isTrusted(storedIdentityKey: String?, presentedIdentityKey: String): Boolean =
        decide(storedIdentityKey, presentedIdentityKey).isUsable
}

/** Raised when a peer's identity key changed and the user has not accepted the new key yet. */
class IdentityKeyChangedException(
    val peerId: String,
    message: String = "Identity key changed for $peerId"
) : Exception(message)
