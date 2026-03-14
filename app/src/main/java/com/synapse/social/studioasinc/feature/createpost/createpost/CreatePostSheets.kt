package com.synapse.social.studioasinc.ui.createpost

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.createpost.CreatePostSearchUiState

@Composable
fun CreatePostSheets(
    uiState: CreatePostUiState,
    searchUiState: CreatePostSearchUiState,
    showPrivacySheet: Boolean,
    onPrivacySheetDismiss: () -> Unit,
    onPrivacySelected: (String) -> Unit,
    showPollSheet: Boolean,
    onPollSheetDismiss: () -> Unit,
    onCreatePoll: (PollData) -> Unit,
    showTagScreen: Boolean,
    onTagScreenDismiss: () -> Unit,
    tagSearchQuery: String,
    onTagSearchQueryChange: (String) -> Unit,
    onToggleUser: (com.synapse.social.studioasinc.domain.model.User) -> Unit,
    showLocationScreen: Boolean,
    onLocationScreenDismiss: () -> Unit,
    locationSearchQuery: String,
    onLocationSearchQueryChange: (String) -> Unit,
    onLocationSelected: (com.synapse.social.studioasinc.domain.model.LocationData) -> Unit,
    showFeelingScreen: Boolean,
    onFeelingScreenDismiss: () -> Unit,
    feelingSearchQuery: String,
    onFeelingSearchQueryChange: (String) -> Unit,
    onFeelingSelected: (com.synapse.social.studioasinc.domain.model.FeelingActivity) -> Unit,
    showYoutubeDialog: Boolean,
    onYoutubeDialogDismiss: () -> Unit,
    onYoutubeUrlAdd: (String) -> Unit
) {
    val context = LocalContext.current

    if (showPrivacySheet) {
        PrivacySelectionSheet(
            currentPrivacy = uiState.privacy,
            onPrivacySelected = onPrivacySelected,
            onDismiss = onPrivacySheetDismiss
        )
    }

    if (showPollSheet) {
        PollCreationSheet(
            onDismiss = onPollSheetDismiss,
            onCreatePoll = {
                if (uiState.mediaItems.isNotEmpty()) {
                    Toast.makeText(context, context.getString(R.string.warn_remove_media_for_poll), Toast.LENGTH_SHORT).show()
                } else {
                    onCreatePoll(it)
                }
            }
        )
    }


    if (showTagScreen) {
        TagPeopleScreen(
            onDismiss = onTagScreenDismiss,
            onDone = onTagScreenDismiss,
            searchQuery = tagSearchQuery,
            onSearchQueryChange = onTagSearchQueryChange,
            searchResults = searchUiState.userSearchResults,
            selectedUsers = uiState.taggedPeople,
            onToggleUser = onToggleUser,
            isLoading = searchUiState.isSearchLoading
        )
    }

    if (showLocationScreen) {
        LocationSelectScreen(
            onDismiss = onLocationScreenDismiss,
            searchQuery = locationSearchQuery,
            onSearchQueryChange = onLocationSearchQueryChange,
            searchResults = searchUiState.locationSearchResults,
            onLocationSelected = onLocationSelected,
            isLoading = searchUiState.isSearchLoading
        )
    }

    if (showFeelingScreen) {
        FeelingSelectScreen(
            onDismiss = onFeelingScreenDismiss,
            searchQuery = feelingSearchQuery,
            onSearchQueryChange = onFeelingSearchQueryChange,
            feelings = searchUiState.feelingSearchResults,
            onFeelingSelected = onFeelingSelected
        )
    }


    if (showYoutubeDialog) {
        YoutubeAddDialog(
            onDismiss = onYoutubeDialogDismiss,
            onAddUrl = { url ->
                if (url.contains("youtube") || url.contains("youtu.be")) {
                    onYoutubeUrlAdd(url)
                } else {
                    Toast.makeText(context, context.getString(R.string.error_invalid_youtube_url), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
