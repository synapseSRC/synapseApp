package com.synapse.social.studioasinc.feature.profile.profile

import com.synapse.social.studioasinc.data.model.UserProfile

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: UserProfile) : ProfileUiState()
    data class Error(val message: String, val exception: Throwable? = null) : ProfileUiState()
    object Empty : ProfileUiState()
}
