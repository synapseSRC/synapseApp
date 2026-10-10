package com.synapse.social.studioasinc.feature.profile.editprofile.location

sealed interface LocationEvent {
    data class RegionSelected(val region: String) : LocationEvent
    data class CurrentCityChanged(val city: String) : LocationEvent
    data class HometownChanged(val hometown: String) : LocationEvent
    data object SaveClicked : LocationEvent
    data object BackClicked : LocationEvent
    data object SelectRegionClicked : LocationEvent
    data object DismissError : LocationEvent
}
