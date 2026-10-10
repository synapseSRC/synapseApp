package com.synapse.social.studioasinc.feature.profile.editprofile.location

sealed interface LocationNavigation {
    data object NavigateBack : LocationNavigation
    data class NavigateToRegionSelection(val currentRegion: String) : LocationNavigation
}
