package com.synapse.social.studioasinc.shared.domain.model.auth

enum class SocialProvider {
    GOOGLE,
    APPLE,
    DISCORD,
    GITHUB,
    TWITTER,
    FACEBOOK,
    SPOTIFY,
    SLACK;

    val displayName: String
        get() = when (this) {
            GOOGLE -> "Google"
            APPLE -> "Apple"
            DISCORD -> "Discord"
            GITHUB -> "GitHub"
            TWITTER -> "Twitter"
            FACEBOOK -> "Facebook"
            SPOTIFY -> "Spotify"
            SLACK -> "Slack"
        }
}
