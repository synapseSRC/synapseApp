package com.synapse.social.studioasinc.ui.createpost

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.createpost.CreatePostSearchUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDateTimePickerDialog(
    initialScheduledAt: String? = null,
    onDismiss: () -> Unit,
    onScheduleSet: (String?) -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) } // 0 = Date, 1 = Time

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 86400000L
    )

    val currentCal = remember { java.util.Calendar.getInstance() }
    val timePickerState = rememberTimePickerState(
        initialHour = currentCal.get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = currentCal.get(java.util.Calendar.MINUTE)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (step == 0) "Select Schedule Date" else "Select Schedule Time")
        },
        text = {
            if (step == 0) {
                DatePicker(state = datePickerState)
            } else {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (step == 0) {
                        step = 1
                    } else {
                        val dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                        cal.timeInMillis = dateMillis
                        cal.set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                        cal.set(java.util.Calendar.MINUTE, timePickerState.minute)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)

                        val chosenMillis = cal.timeInMillis
                        if (chosenMillis <= System.currentTimeMillis()) {
                            Toast.makeText(context, context.getString(R.string.scheduled_time_past_error), Toast.LENGTH_SHORT).show()
                        } else {
                            val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(chosenMillis)
                            onScheduleSet(instant.toString())
                            onDismiss()
                        }
                    }
                }
            ) {
                Text(if (step == 0) "Next" else "Set Schedule")
            }
        },
        dismissButton = {
            Row {
                if (initialScheduledAt != null) {
                    TextButton(onClick = {
                        onScheduleSet(null)
                        onDismiss()
                    }) {
                        Text("Clear Schedule", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

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
    onYoutubeUrlAdd: (String) -> Unit,
    showSchedulePicker: Boolean = false,
    onSchedulePickerDismiss: () -> Unit = {},
    onScheduleSet: (String?) -> Unit = {}
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

    if (showSchedulePicker) {
        ScheduleDateTimePickerDialog(
            initialScheduledAt = uiState.scheduledAt,
            onDismiss = onSchedulePickerDismiss,
            onScheduleSet = onScheduleSet
        )
    }
}
