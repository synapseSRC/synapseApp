package com.synapse.social.studioasinc.feature.profile.personalinfo

import com.synapse.social.studioasinc.data.model.RelationshipStatus
import com.synapse.social.studioasinc.domain.model.Gender

data class PersonalInformationUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val pronouns: String = "",
    val birthday: String = "", // Raw date string e.g., "YYYY-MM-DD"
    val gender: Gender = Gender.Hidden,
    val relationshipStatus: RelationshipStatus? = null,
    val hasChanges: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

sealed class PersonalInformationEvent {
    data class PronounsChanged(val pronouns: String) : PersonalInformationEvent()
    data class BirthdayChanged(val birthday: String) : PersonalInformationEvent()
    data class GenderSelected(val gender: Gender) : PersonalInformationEvent()
    data class RelationshipStatusSelected(val status: RelationshipStatus?) : PersonalInformationEvent()
    object SaveClicked : PersonalInformationEvent()
    object BackClicked : PersonalInformationEvent()
    object ErrorDismissed : PersonalInformationEvent()
}

sealed class PersonalInformationNavigation {
    object NavigateBack : PersonalInformationNavigation()
}
