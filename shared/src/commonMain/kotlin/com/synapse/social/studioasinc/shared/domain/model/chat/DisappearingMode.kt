package com.synapse.social.studioasinc.shared.domain.model.chat

enum class DisappearingMode(val seconds: Long?) {
    OFF(null),
    TWENTY_FOUR_HOURS(24 * 60 * 60),
    SEVEN_DAYS(7 * 24 * 60 * 60)
}
