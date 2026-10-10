package com.synapse.social.studioasinc.feature.profile.editprofile.appearance

import android.net.Uri
import com.synapse.social.studioasinc.domain.model.UserProfile
import com.synapse.social.studioasinc.presentation.editprofile.UploadState

data class ProfileAppearanceUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val profile: UserProfile? = null,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val pendingCoverUri: Uri? = null,
    val uploadState: UploadState = UploadState.Idle,
    val error: String? = null,
    val hasPendingChanges: Boolean = false,
    val showRemoveCoverDialog: Boolean = false,
    val showUnsavedChangesDialog: Boolean = false
)
