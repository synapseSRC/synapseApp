package com.synapse.social.studioasinc.feature.call

import com.synapse.social.studioasinc.ui.navigation.AppDestination

data class CallLaunchRequest(
    val participantId: String,
    val participantName: String,
    val participantAvatarUrl: String? = null,
    val chatId: String? = null,
    val isVideo: Boolean = false,
    val isIncoming: Boolean = false
) {
    fun toDestination(): AppDestination.Call = AppDestination.Call(
        peerUserId = participantId,
        peerName = participantName,
        peerAvatarUrl = participantAvatarUrl,
        chatId = chatId,
        isVideo = isVideo,
        isIncoming = isIncoming
    )
}
