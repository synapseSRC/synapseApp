package com.synapse.social.studioasinc.feature.profile.editprofile.location

import com.synapse.social.studioasinc.domain.model.UserProfile

data class LocationUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val profile: UserProfile? = null,
    val selectedRegion: String = "",
    val currentCity: String = "",
    val hometown: String = "",
    val initialSelectedRegion: String = "",
    val initialCurrentCity: String = "",
    val initialHometown: String = "",
    val error: String? = null
) {
    val hasChanges: Boolean
        get() = selectedRegion != initialSelectedRegion ||
                currentCity != initialCurrentCity ||
                hometown != initialHometown
}
