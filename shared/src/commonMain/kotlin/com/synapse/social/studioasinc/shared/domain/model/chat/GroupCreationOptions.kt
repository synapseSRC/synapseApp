package com.synapse.social.studioasinc.shared.domain.model.chat

/** Provider-neutral permissions selected while creating a group. */
data class GroupCreationOptions(
    val description: String? = null,
    val sendMessagesByMembers: Boolean = true,
    val allowMemberSettingsEdits: Boolean = false,
    val requireJoinApproval: Boolean = true,
    val allowMemberInvites: Boolean = false,
    val disappearingMode: DisappearingMode = DisappearingMode.OFF
)
