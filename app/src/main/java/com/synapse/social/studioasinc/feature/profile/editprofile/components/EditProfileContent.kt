package com.synapse.social.studioasinc.presentation.editprofile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileEvent
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileUiState
import com.synapse.social.studioasinc.ui.settings.SettingsCard
import com.synapse.social.studioasinc.ui.settings.SettingsNavigationItem
import com.synapse.social.studioasinc.ui.settings.SettingsSpacing

@Composable
fun EditProfileContent(
    uiState: EditProfileUiState,
    paddingValues: PaddingValues,
    onAvatarClick: () -> Unit,
    onCoverClick: () -> Unit,
    onNavigateToRegionSelection: (String) -> Unit,
    onEvent: (EditProfileEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = SettingsSpacing.screenPadding,
            end = SettingsSpacing.screenPadding,
            top = paddingValues.calculateTopPadding() + 8.dp,
            bottom = paddingValues.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
    ) {

        item {
            ProfileImageSection(
                coverUrl = uiState.coverUrl,
                avatarUrl = uiState.avatarUrl,
                avatarUploadState = uiState.avatarUploadState,
                coverUploadState = uiState.coverUploadState,
                onCoverClick = onCoverClick,
                onAvatarClick = onAvatarClick,
                onRetryAvatarUpload = { onEvent(EditProfileEvent.RetryAvatarUpload) },
                onRetryCoverUpload = { onEvent(EditProfileEvent.RetryCoverUpload) }
            )
        }

        item {
            ProfileFormFields(
                username = uiState.username,
                onUsernameChange = { onEvent(EditProfileEvent.UsernameChanged(it)) },
                usernameValidation = uiState.usernameValidation,
                nickname = uiState.nickname,
                onNicknameChange = { onEvent(EditProfileEvent.NicknameChanged(it)) },
                nicknameError = uiState.nicknameError,
                bio = uiState.bio,
                onBiographyChange = { onEvent(EditProfileEvent.BiographyChanged(it)) },
                bioError = uiState.bioError
            )
        }

        item {
            GenderSelector(
                selectedGender = uiState.selectedGender,
                onGenderSelected = { onEvent(EditProfileEvent.GenderSelected(it)) }
            )
        }

        item {
            SettingsCard {
                SettingsNavigationItem(
                    title = "Region",
                    subtitle = uiState.selectedRegion ?: "Not set",
                    icon = R.drawable.ic_location,
                    onClick = {
                        onNavigateToRegionSelection(uiState.selectedRegion ?: "")
                    }
                )
            }
        }

        item {
            SettingsCard {
                SettingsNavigationItem(
                    title = "Profile Photo History",
                    subtitle = "View and restore previous photos",
                    icon = null,
                    onClick = { onEvent(EditProfileEvent.ProfileHistoryClicked) }
                )

                com.synapse.social.studioasinc.ui.settings.SettingsDivider()

                SettingsNavigationItem(
                    title = "Cover Photo History",
                    subtitle = "View and restore previous covers",
                    icon = null,
                    onClick = { onEvent(EditProfileEvent.CoverHistoryClicked) }
                )
            }
        }
    }
}
