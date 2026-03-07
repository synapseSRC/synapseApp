package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.UserProfileManager
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch



@HiltViewModel
class SettingsHubViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _userProfileSummary = MutableStateFlow<UserProfileSummary?>(null)
    val userProfileSummary: StateFlow<UserProfileSummary?> = _userProfileSummary.asStateFlow()

    private val _settingsGroups = MutableStateFlow<List<SettingsGroup>>(emptyList())
    val settingsGroups: StateFlow<List<SettingsGroup>> = _settingsGroups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUserProfile()
        loadSettingsCategories()
    }



    private fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = UserProfileManager.getCurrentUserProfile()
                if (currentUser != null) {
                    val displayName = currentUser.displayName?.takeIf { it.isNotBlank() }
                        ?: currentUser.username?.takeIf { it.isNotBlank() }
                        ?: "User"

                    android.util.Log.d("SettingsHubViewModel", "Profile loaded - avatarUrl: ${currentUser.avatar}")
                    _userProfileSummary.value = UserProfileSummary(
                        id = currentUser.uid,
                        displayName = displayName,
                        email = currentUser.email ?: "",
                        avatarUrl = currentUser.avatar
                    )
                } else {

                    try {
                        val authService = SupabaseAuthenticationService.getInstance(getApplication())
                        val authUser = authService.getCurrentUser()

                        if (authUser != null) {
                            _userProfileSummary.value = UserProfileSummary(
                                id = authUser.id,
                                displayName = "User",
                                email = authUser.email,
                                avatarUrl = null
                            )
                        } else {

                            _userProfileSummary.value = UserProfileSummary(
                                id = "",
                                displayName = "User",
                                email = "",
                                avatarUrl = null
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsHubViewModel", "Failed to load auth user", e)
                         _userProfileSummary.value = UserProfileSummary(
                            id = "",
                            displayName = "User",
                            email = "",
                            avatarUrl = null
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsHubViewModel", "Failed to load user profile", e)

                _userProfileSummary.value = UserProfileSummary(
                    id = "",
                    displayName = "User",
                    email = "",
                    avatarUrl = null
                )
            } finally {
                _isLoading.value = false
            }
        }
    }



    private fun loadSettingsCategories() {
        _settingsGroups.value = SettingsDataProvider.getSettingsGroups()
    }



    fun onNavigateToCategory(destination: SettingsDestination) {


        android.util.Log.d("SettingsHubViewModel", "Navigating to: ${destination.route}")
    }



    fun refreshUserProfile() {

        UserProfileManager.clearCache()
        loadUserProfile()
    }
}
