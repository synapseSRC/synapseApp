package com.synapse.social.studioasinc.data.call

enum class CallSimulationAction {
    GOOD_CONNECTION,
    WEAK_CONNECTION,
    POOR_CONNECTION,
    RECONNECT,
    REMOTE_VIDEO_OFF,
    REMOTE_VIDEO_ON,
    REMOTE_DECLINES,
    NO_ANSWER,
    UNAVAILABLE,
    REMOTE_ENDS
}

interface CallSimulationController {
    fun simulate(action: CallSimulationAction)
}
