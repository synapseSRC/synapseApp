package com.synapse.social.studioasinc.shared.domain.model.call

/**
 * A runtime capability a call needs from the user.
 *
 * Named after the *capability*, not the platform constant: the domain cannot know that a
 * microphone is called `RECORD_AUDIO` on one operating system and `NSMicrophoneUsageDescription` on
 * another. Mapping these to whatever the host platform calls them is the data layer's job, and only
 * the data layer's.
 */
enum class CallPermission { MICROPHONE, CAMERA }

/** What the user answered when asked. */
enum class CallPermissionOutcome { GRANTED, DENIED, DENIED_PERMANENTLY }

/**
 * Whether a call may start, and what has to happen first.
 *
 * [needsSettings] is separate from [missing] on purpose. A permission that was declined once can
 * still be asked for again; one that was declined with "don't ask again" cannot be granted by
 * asking, so the honest next step is the system settings screen. Folding the two together would
 * produce a call screen that loops a dialog the user has already closed forever.
 */
data class CallPermissionPlan(
    val mediaType: CallMediaType,
    val required: List<CallPermission>,
    val missing: List<CallPermission> = emptyList(),
    val needsSettings: List<CallPermission> = emptyList()
) {
    /** Nothing stands between this media type and a call. */
    val isReady: Boolean get() = missing.isEmpty() && needsSettings.isEmpty()

    /** Asking the user can still help, which is the only case a permission dialog belongs in. */
    val canAsk: Boolean get() = missing.isNotEmpty() && needsSettings.isEmpty()

    /** Permissions the platform must be asked for, in the order the caller should request them. */
    fun requestable(): List<CallPermission> = missing
}

/**
 * Which capabilities a call of [mediaType] consumes.
 *
 * A voice call never needs the camera, so it must never be asked for one — and a video call needs
 * both, because turning the camera on mid-audio-call is exactly the request a user should not have
 * to make twice.
 */
fun requiredCallPermissions(mediaType: CallMediaType): List<CallPermission> = when (mediaType) {
    CallMediaType.VOICE -> listOf(CallPermission.MICROPHONE)
    CallMediaType.VIDEO -> listOf(CallPermission.MICROPHONE, CallPermission.CAMERA)
}
