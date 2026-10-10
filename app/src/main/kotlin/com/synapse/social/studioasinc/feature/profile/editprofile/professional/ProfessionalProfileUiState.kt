package com.synapse.social.studioasinc.feature.profile.editprofile.professional

data class ProfessionalProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val accountType: com.synapse.social.studioasinc.shared.domain.model.business.AccountType = com.synapse.social.studioasinc.shared.domain.model.business.AccountType.PERSONAL,
    val isBusinessAccount: Boolean = false,
    val occupation: String = "",
    val workplace: String = "",
    val hasChanges: Boolean = false,
    val error: String? = null,
    val switchAccountError: String? = null
)
