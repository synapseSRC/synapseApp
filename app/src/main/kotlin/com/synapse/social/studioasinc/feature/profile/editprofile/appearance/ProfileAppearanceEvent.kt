package com.synapse.social.studioasinc.feature.profile.editprofile.appearance

import android.net.Uri

sealed interface ProfileAppearanceEvent {
    data class CoverSelected(val uri: Uri) : ProfileAppearanceEvent
    data class CoverCropped(val uri: Uri) : ProfileAppearanceEvent
    object RequestRemoveCover : ProfileAppearanceEvent
    object ConfirmRemoveCover : ProfileAppearanceEvent
    object DismissRemoveCoverDialog : ProfileAppearanceEvent
    object ResetChanges : ProfileAppearanceEvent
    object ApplyChanges : ProfileAppearanceEvent
    object RetryCoverUpload : ProfileAppearanceEvent
    object BackClicked : ProfileAppearanceEvent
    object ConfirmDiscardUnsaved : ProfileAppearanceEvent
    object DismissUnsavedDialog : ProfileAppearanceEvent
}
