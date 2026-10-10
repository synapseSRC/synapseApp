package com.synapse.social.studioasinc.presentation.editprofile.social

data class SocialLinksUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val githubProfile: String = "",
    val githubError: String? = null,
    val discordTag: String = "",
    val discordError: String? = null,
    val personalWebsite: String = "",
    val websiteError: String? = null,
    val publicEmail: String = "",
    val publicEmailError: String? = null,
    val hasChanges: Boolean = false,
    val error: String? = null
)

sealed class SocialLinksEvent {
    data class GithubProfileChanged(val value: String) : SocialLinksEvent()
    data class DiscordTagChanged(val value: String) : SocialLinksEvent()
    data class PersonalWebsiteChanged(val value: String) : SocialLinksEvent()
    data class PublicEmailChanged(val value: String) : SocialLinksEvent()
    object SaveClicked : SocialLinksEvent()
    object BackClicked : SocialLinksEvent()
    object RetryClicked : SocialLinksEvent()
    object ErrorDismissed : SocialLinksEvent()
}

sealed class SocialLinksNavigation {
    object NavigateBack : SocialLinksNavigation()
}
