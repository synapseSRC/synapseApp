package com.synapse.social.studioasinc.feature.inbox.inbox.voice

data class VoiceGestureConfig(
    val holdThresholdMs: Long = 180L,
    val cancelThresholdDp: Float = 100f,
    val lockThresholdDp: Float = 80f,
    val directionSlopDp: Float = 12f
)
