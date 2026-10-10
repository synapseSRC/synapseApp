package com.synapse.social.studioasinc.feature.profile.editprofile.media

import android.net.Uri
import com.synapse.social.studioasinc.presentation.editprofile.UploadState

data class ProfileMediaUiState(
    val isLoading: Boolean = true,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val pendingAvatarUri: Uri? = null,
    val pendingCoverUri: Uri? = null,
    val avatarUploadState: UploadState = UploadState.Idle,
    val coverUploadState: UploadState = UploadState.Idle,
    val error: String? = null,
    val showRemoveAvatarDialog: Boolean = false,
    val showRemoveCoverDialog: Boolean = false,
    val showShareToFeedDialog: Boolean = false
)

sealed class ProfileMediaEvent {
    data class AvatarSelected(val uri: Uri) : ProfileMediaEvent()
    data class AvatarCropped(val uri: Uri) : ProfileMediaEvent()
    data class CoverSelected(val uri: Uri) : ProfileMediaEvent()
    data class CoverCropped(val uri: Uri) : ProfileMediaEvent()
    object RequestRemoveAvatar : ProfileMediaEvent()
    object ConfirmRemoveAvatar : ProfileMediaEvent()
    object DismissRemoveAvatarDialog : ProfileMediaEvent()
    object RequestRemoveCover : ProfileMediaEvent()
    object ConfirmRemoveCover : ProfileMediaEvent()
    object DismissRemoveCoverDialog : ProfileMediaEvent()
    object RetryAvatarUpload : ProfileMediaEvent()
    object RetryCoverUpload : ProfileMediaEvent()
    object BackClicked : ProfileMediaEvent()
    object ViewAvatarHistoryClicked : ProfileMediaEvent()
    object ViewCoverHistoryClicked : ProfileMediaEvent()
    object DismissShareToFeedDialog : ProfileMediaEvent()
}

sealed class ProfileMediaNavigation {
    object NavigateBack : ProfileMediaNavigation()
    object NavigateToAvatarHistory : ProfileMediaNavigation()
    object NavigateToCoverHistory : ProfileMediaNavigation()
}
