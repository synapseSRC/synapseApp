package com.synapse.social.studioasinc.presentation.editprofile.social

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.EditProfileRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SocialLinksViewModel @Inject constructor(
    application: Application,
    private val repository: EditProfileRepositoryImpl
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SocialLinksUiState())
    val uiState: StateFlow<SocialLinksUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<SocialLinksNavigation>()
    val navigationEvents: SharedFlow<SocialLinksNavigation> = _navigationEvents.asSharedFlow()

    private var initialGithub: String = ""
    private var initialDiscord: String = ""
    private var initialWebsite: String = ""
    private var initialPublicEmail: String = ""

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = repository.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
                return@launch
            }

            repository.getUserProfile(userId).collect { result ->
                result.fold(
                    onSuccess = { profile ->
                        initialGithub = profile.githubProfile ?: ""
                        initialDiscord = profile.discordTag ?: ""
                        initialWebsite = profile.personalWebsite ?: ""
                        initialPublicEmail = profile.publicEmail ?: ""

                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                githubProfile = initialGithub,
                                discordTag = initialDiscord,
                                personalWebsite = initialWebsite,
                                publicEmail = initialPublicEmail,
                                hasChanges = false,
                                githubError = null,
                                discordError = null,
                                websiteError = null,
                                publicEmailError = null,
                                error = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message ?: "Failed to load profile") }
                    }
                )
            }
        }
    }

    fun onEvent(event: SocialLinksEvent) {
        when (event) {
            is SocialLinksEvent.GithubProfileChanged -> {
                val value = event.value
                val error = validateGithub(value)
                _uiState.update {
                    it.copy(
                        githubProfile = value,
                        githubError = error,
                        hasChanges = checkHasChanges(
                            github = value,
                            discord = it.discordTag,
                            website = it.personalWebsite,
                            email = it.publicEmail
                        )
                    )
                }
            }
            is SocialLinksEvent.DiscordTagChanged -> {
                val value = event.value
                val error = validateDiscord(value)
                _uiState.update {
                    it.copy(
                        discordTag = value,
                        discordError = error,
                        hasChanges = checkHasChanges(
                            github = it.githubProfile,
                            discord = value,
                            website = it.personalWebsite,
                            email = it.publicEmail
                        )
                    )
                }
            }
            is SocialLinksEvent.PersonalWebsiteChanged -> {
                val value = event.value
                val error = validateWebsite(value)
                _uiState.update {
                    it.copy(
                        personalWebsite = value,
                        websiteError = error,
                        hasChanges = checkHasChanges(
                            github = it.githubProfile,
                            discord = it.discordTag,
                            website = value,
                            email = it.publicEmail
                        )
                    )
                }
            }
            is SocialLinksEvent.PublicEmailChanged -> {
                val value = event.value
                val error = validatePublicEmail(value)
                _uiState.update {
                    it.copy(
                        publicEmail = value,
                        publicEmailError = error,
                        hasChanges = checkHasChanges(
                            github = it.githubProfile,
                            discord = it.discordTag,
                            website = it.personalWebsite,
                            email = value
                        )
                    )
                }
            }
            SocialLinksEvent.SaveClicked -> {
                saveSocialLinks()
            }
            SocialLinksEvent.BackClicked -> {
                viewModelScope.launch {
                    _navigationEvents.emit(SocialLinksNavigation.NavigateBack)
                }
            }
            SocialLinksEvent.RetryClicked -> {
                loadProfile()
            }
            SocialLinksEvent.ErrorDismissed -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun checkHasChanges(github: String, discord: String, website: String, email: String): Boolean {
        return github.trim() != initialGithub.trim() ||
                discord.trim() != initialDiscord.trim() ||
                website.trim() != initialWebsite.trim() ||
                email.trim() != initialPublicEmail.trim()
    }

    private fun validateGithub(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.length > 100) return "GitHub link/username too long"
        return null
    }

    private fun validateDiscord(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.length > 50) return "Discord tag too long"
        return null
    }

    private fun validateWebsite(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        val normalized = normalizeUrl(trimmed)
        val urlRegex = Regex("^(https?://)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(/.*)?$")
        if (!urlRegex.matches(normalized) && !normalized.contains(".")) {
            return "Invalid website URL"
        }
        return null
    }

    private fun validatePublicEmail(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!emailRegex.matches(trimmed)) {
            return "Invalid email address format"
        }
        return null
    }

    fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""
        return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    private fun saveSocialLinks() {
        val state = _uiState.value
        if (state.githubError != null || state.discordError != null || state.websiteError != null || state.publicEmailError != null) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val userId = repository.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isSaving = false, error = "User not logged in") }
                return@launch
            }

            val github = state.githubProfile.trim().let { if (it.isNotEmpty()) normalizeGithub(it) else null }
            val discord = state.discordTag.trim().ifEmpty { null }
            val website = state.personalWebsite.trim().let { if (it.isNotEmpty()) normalizeUrl(it) else null }
            val email = state.publicEmail.trim().ifEmpty { null }

            val updateData = mutableMapOf<String, Any?>()
            updateData["github_profile"] = github
            updateData["discord_tag"] = discord
            updateData["personal_website"] = website
            updateData["public_email"] = email

            val result = repository.updateProfile(userId, updateData)
            result.fold(
                onSuccess = {
                    initialGithub = github ?: ""
                    initialDiscord = discord ?: ""
                    initialWebsite = website ?: ""
                    initialPublicEmail = email ?: ""

                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            hasChanges = false,
                            githubProfile = initialGithub,
                            discordTag = initialDiscord,
                            personalWebsite = initialWebsite,
                            publicEmail = initialPublicEmail
                        )
                    }
                    _navigationEvents.emit(SocialLinksNavigation.NavigateBack)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to save social links"
                        )
                    }
                }
            )
        }
    }

    private fun normalizeGithub(value: String): String {
        val trimmed = value.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        if (trimmed.startsWith("github.com/")) {
            return "https://$trimmed"
        }
        return if (!trimmed.contains(".")) {
            "https://github.com/$trimmed"
        } else {
            "https://$trimmed"
        }
    }
}
