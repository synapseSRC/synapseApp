package com.synapse.social.studioasinc.domain.model

data class ChatVerificationInfo(
    val chatId: String,
    val participantId: String,
    /** Public identity key only; never a private Signal key. */
    val participantIdentityKey: String,
    val safetyNumber: String,
    val localQrPayload: String,
    val secureSessionActive: Boolean,
    val isVerified: Boolean,
    /**
     * True when this peer presented a different identity key than the one stored on this device and
     * the user has not accepted the new key yet. Sending to the peer is refused until then.
     */
    val identityChanged: Boolean = false
)

enum class ChatVerificationFailureReason {
    NOT_AUTHENTICATED, LOCAL_IDENTITY_MISSING, PARTICIPANT_IDENTITY_MISSING,
    NOT_ONE_TO_ONE_CHAT, INVALID_IDENTITY_KEY, IDENTITY_CHANGED, STORAGE_FAILURE, GENERAL
}

class ChatVerificationException(val reason: ChatVerificationFailureReason) : Exception()

enum class ChatVerificationFeedback {
    NONE, CODE_MATCHED, CODE_MISMATCH, MANUALLY_VERIFIED, VERIFICATION_CLEARED, SCANNER_UNAVAILABLE
}
