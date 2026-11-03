package com.synapse.social.studioasinc.feature.echo.room

import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionPlan
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoErrorKind
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRole
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomDetail
import com.synapse.social.studioasinc.shared.domain.service.EchoAudioConnection

enum class EchoRoomPhase {
    /** Membership or the first audio connection is still being established. */
    JOINING,
    LIVE,
    ENDED,

    /** Nothing was joined; the room itself refused or could not be reached. */
    FAILED
}

/** What the microphone was wanted for when a permission got in the way. */
enum class EchoMicPurpose { JOIN, SPEAK, UNMUTE }

/**
 * A permission plan waiting for the screen's launcher.
 *
 * [requestKey] changes only when the user is deliberately asking again, so an answer that has just
 * been given cannot reopen the system dialog.
 */
data class EchoPermissionPrompt(
    val plan: CallPermissionPlan,
    val purpose: EchoMicPurpose,
    val requestKey: Long
)

/** One floating reaction; [id] is what lets two people send the same emoji at the same time. */
data class EchoFloatingReaction(
    val id: Long,
    val emoji: String,
    val userId: String,
    val displayName: String?
)

data class EchoRoomUiState(
    val phase: EchoRoomPhase = EchoRoomPhase.JOINING,
    val detail: EchoRoomDetail? = null,
    val audio: EchoAudioConnection = EchoAudioConnection.DISCONNECTED,
    val activeSpeakerIds: Set<String> = emptySet(),
    val micEnabled: Boolean = false,
    /** The platform will not ask about the microphone again; only Settings can undo it. */
    val micBlocked: Boolean = false,
    /** Whether a microphone was ever opened here; a speaker who muted is not a new invitee. */
    val hasUsedMicrophone: Boolean = false,
    val hasConnected: Boolean = false,
    val handRaised: Boolean = false,
    val reactions: List<EchoFloatingReaction> = emptyList(),
    val permissionPrompt: EchoPermissionPrompt? = null,
    /** The member whose host actions are on screen. */
    val participantMenu: String? = null,
    val busy: Boolean = false,
    val error: EchoErrorKind? = null,
    val closeRequested: Boolean = false
) {
    val roomId: String? get() = detail?.room?.id
    val title: String? get() = detail?.room?.title
    val topic: String? get() = detail?.room?.topic
    val myRole: EchoRole? get() = detail?.myRole
    val isHost: Boolean get() = myRole == EchoRole.HOST
    val canSpeak: Boolean get() = myRole?.canPublish == true
    val isAudience: Boolean get() = myRole == EchoRole.AUDIENCE

    /** Publishing and actually on the air, which is what the microphone control reflects. */
    val onAir: Boolean get() = canSpeak && micEnabled && audio == EchoAudioConnection.CONNECTED

    /**
     * Promoted, but the microphone has not been opened yet: the room offers the stage instead.
     * Hosts never see this — nobody moved them there, and the invite copy would be addressed to
     * the host on the host's own screen.
     */
    val invitedToSpeak: Boolean
        get() = phase == EchoRoomPhase.LIVE &&
            myRole == EchoRole.SPEAKER &&
            !micEnabled &&
            !hasUsedMicrophone

    /** A connection this screen once had is being rebuilt, or has been lost. */
    val connectionTroubled: Boolean
        get() = phase == EchoRoomPhase.LIVE && hasConnected && audio != EchoAudioConnection.CONNECTED

    fun participant(userId: String) = detail?.participants?.firstOrNull { it.userId == userId }
}
