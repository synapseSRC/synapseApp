package com.synapse.social.studioasinc.feature.profile.editprofile.featured

sealed class FeaturedContentEvent {
    data class TabSelected(val tab: FeaturedTab) : FeaturedContentEvent()
    data class TogglePostSelected(val postId: String) : FeaturedContentEvent()
    data class ToggleMediaSelected(val mediaUrl: String) : FeaturedContentEvent()
    data class MovePostOrderUp(val postId: String) : FeaturedContentEvent()
    data class MovePostOrderDown(val postId: String) : FeaturedContentEvent()
    data class MoveMediaOrderUp(val mediaUrl: String) : FeaturedContentEvent()
    data class MoveMediaOrderDown(val mediaUrl: String) : FeaturedContentEvent()
    object ClearAllSelected : FeaturedContentEvent()
    object ApplyChanges : FeaturedContentEvent()
    object BackClicked : FeaturedContentEvent()
    object DismissError : FeaturedContentEvent()
    object DismissCapabilityNotice : FeaturedContentEvent()
}
