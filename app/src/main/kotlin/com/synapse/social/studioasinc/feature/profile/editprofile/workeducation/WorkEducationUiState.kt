package com.synapse.social.studioasinc.feature.profile.editprofile.workeducation

import com.synapse.social.studioasinc.domain.model.UserProfile

data class WorkEducationUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val profile: UserProfile? = null,
    val occupation: String = "",
    val workplace: String = "",
    val education: String = "",
    val initialOccupation: String = "",
    val initialWorkplace: String = "",
    val initialEducation: String = "",
    val error: String? = null
) {
    val hasChanges: Boolean
        get() = occupation != initialOccupation ||
                workplace != initialWorkplace ||
                education != initialEducation
}

sealed class WorkEducationEvent {
    data class OccupationChanged(val occupation: String) : WorkEducationEvent()
    data class WorkplaceChanged(val workplace: String) : WorkEducationEvent()
    data class EducationChanged(val education: String) : WorkEducationEvent()
    object SaveClicked : WorkEducationEvent()
    object BackClicked : WorkEducationEvent()
    object DismissError : WorkEducationEvent()
}

sealed class WorkEducationNavigation {
    object NavigateBack : WorkEducationNavigation()
}