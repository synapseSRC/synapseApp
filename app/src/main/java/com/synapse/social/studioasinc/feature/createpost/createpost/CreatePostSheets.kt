package com.synapse.social.studioasinc.ui.createpost

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.synapse.social.studioasinc.R

@Composable
fun CreatePostSheets(
    uiState: CreatePostUiState,
    showPrivacySheet: Boolean,
    onPrivacySheetDismiss: () -> Unit,
    onPrivacySelected: (String) -> Unit,
    showPollSheet: Boolean,
    onPollSheetDismiss: () -> Unit,
    onCreatePoll: (PollData) -> Unit,
    showAddToPostSheet: Boolean,
    onAddToPostSheetDismiss: () -> Unit,
    onMediaClick: () -> Unit,
    onPollClick: () -> Unit,
    onYoutubeClick: () -> Unit,
    onLocationClick: () -> Unit,
    onTagClick: () -> Unit,
    onFeelingClick: () -> Unit,
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

    if (showAddToPostSheet) {
        AddToPostSheet(
            onDismiss = onAddToPostSheetDismiss,
            onMediaClick = onMediaClick,
            onPollClick = {
                if (uiState.mediaItems.isNotEmpty()) {
                    Toast.makeText(context, context.getString(R.string.warn_remove_media_for_poll), Toast.LENGTH_SHORT).show()
                } else {
                    onPollClick()
                }
            },
            onYoutubeClick = onYoutubeClick,
            onLocationClick = onLocationClick,
            onTagClick = onTagClick,
            onFeelingClick = onFeelingClick
        )
    }


    if (showTagScreen) {
        TagPeopleScreen(
            onDismiss = onTagScreenDismiss,
            onDone = onTagScreenDismiss,
            searchQuery = tagSearchQuery,
            onSearchQueryChange = onTagSearchQueryChange,
            searchResults = uiState.userSearchResults,
            selectedUsers = uiState.taggedPeople,
            onToggleUser = onToggleUser,
            isLoading = uiState.isSearchLoading
        )
    }

    if (showLocationScreen) {
        LocationSelectScreen(
            onDismiss = onLocationScreenDismiss,
            searchQuery = locationSearchQuery,
            onSearchQueryChange = onLocationSearchQueryChange,
            searchResults = uiState.locationSearchResults,
            onLocationSelected = onLocationSelected,
            isLoading = uiState.isSearchLoading
        )
    }

    if (showFeelingScreen) {
        FeelingSelectScreen(
            onDismiss = onFeelingScreenDismiss,
            searchQuery = feelingSearchQuery,
            onSearchQueryChange = onFeelingSearchQueryChange,
            feelings = uiState.feelingSearchResults,
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
