package com.synapse.social.studioasinc.feature.profile.editprofile.interestsskills

sealed interface InterestsSkillsEvent {
    object LoadProfile : InterestsSkillsEvent
    data class InterestInputChanged(val text: String) : InterestsSkillsEvent
    object AddInterest : InterestsSkillsEvent
    data class RemoveInterest(val index: Int) : InterestsSkillsEvent
    data class MoveInterest(val fromIndex: Int, val toIndex: Int) : InterestsSkillsEvent
    object SaveClicked : InterestsSkillsEvent
    object BackClicked : InterestsSkillsEvent
    object DismissError : InterestsSkillsEvent
    object DismissSuccess : InterestsSkillsEvent
}

sealed interface InterestsSkillsNavigation {
    object NavigateBack : InterestsSkillsNavigation
}
