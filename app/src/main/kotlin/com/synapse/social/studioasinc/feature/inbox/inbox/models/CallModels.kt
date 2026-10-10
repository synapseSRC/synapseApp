package com.synapse.social.studioasinc.feature.inbox.inbox.models

data class CallRecord(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val callType: CallType,
    val direction: CallDirection,
    val timestamp: Long,
    val duration: Int?, // seconds, null if missed
    val isVideo: Boolean
)

enum class CallType { AUDIO, VIDEO }
enum class CallDirection { INCOMING, OUTGOING, MISSED }
