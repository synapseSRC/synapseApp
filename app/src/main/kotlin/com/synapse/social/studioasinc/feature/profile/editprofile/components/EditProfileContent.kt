package com.synapse.social.studioasinc.presentation.editprofile.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileEvent
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileUiState
import com.synapse.social.studioasinc.ui.settings.SettingsSpacing

@Composable
fun EditProfileContent(
    uiState: EditProfileUiState,
    paddingValues: PaddingValues,
    onAvatarClick: () -> Unit,
    onCoverClick: () -> Unit,
    onNavigateToRegionSelection: (String) -> Unit,
    onNavigateToProfileMedia: () -> Unit,
    onNavigateToPersonalInfo: () -> Unit,
    onNavigateToSocialLinks: () -> Unit,
    onNavigateToProfileAppearance: () -> Unit = {},
    onNavigateToFeaturedContent: () -> Unit = {},
    onEvent: (EditProfileEvent) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = SettingsSpacing.screenPadding,
            end = SettingsSpacing.screenPadding,
            top = paddingValues.calculateTopPadding() + Spacing.Small,
            bottom = paddingValues.calculateBottomPadding() + Spacing.Large
        ),
        verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
    ) {
        // 1. Top Identity Preview
        item {
            ProfileIdentityPreview(
                coverUrl = uiState.coverUrl,
                avatarUrl = uiState.avatarUrl,
                nickname = uiState.nickname,
                username = uiState.username,
                bio = uiState.bio,
                pendingAvatarUri = uiState.pendingAvatarUri,
                pendingCoverUri = uiState.pendingCoverUri,
                avatarUploadState = uiState.avatarUploadState,
                coverUploadState = uiState.coverUploadState,
                onCoverClick = onCoverClick,
                onAvatarClick = onAvatarClick,
                onRetryAvatarUpload = { onEvent(EditProfileEvent.RetryAvatarUpload) },
                onRetryCoverUpload = { onEvent(EditProfileEvent.RetryCoverUpload) }
            )
        }

        // 2. Compact Profile Completion Summary
        item {
            ProfileCompletionCard(
                uiState = uiState,
                onClick = {
                    Toast.makeText(context, context.getString(R.string.complete_missing_details), Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 3. Basic Information Section
        item {
            BasicInformationSection(
                nickname = uiState.nickname,
                onNicknameChange = { onEvent(EditProfileEvent.NicknameChanged(it)) },
                nicknameError = uiState.nicknameError,
                username = uiState.username,
                onUsernameChange = { onEvent(EditProfileEvent.UsernameChanged(it)) },
                usernameValidation = uiState.usernameValidation,
                bio = uiState.bio,
                onBiographyChange = { onEvent(EditProfileEvent.BiographyChanged(it)) },
                bioError = uiState.bioError
            )
        }

        // 4. Category Navigation: PROFILE
        item {
            ProfileCategorySection(
                selectedRegion = uiState.selectedRegion,
                onNavigateToProfileHistory = { onEvent(EditProfileEvent.ProfileHistoryClicked) },
                onNavigateToCoverHistory = { onEvent(EditProfileEvent.CoverHistoryClicked) },
                onNavigateToRegionSelection = onNavigateToRegionSelection,
                onNavigateToPersonalInfo = onNavigateToPersonalInfo,
                onCategoryClick = { categoryType ->
                    when (categoryType) {
                        ProfileCategoryType.PROFILE_MEDIA -> onEvent(EditProfileEvent.ProfileMediaClicked)
                        ProfileCategoryType.LOCATION -> onNavigateToRegionSelection(uiState.selectedRegion ?: "")
                        ProfileCategoryType.INTERESTS_SKILLS -> onEvent(EditProfileEvent.InterestsSkillsClicked)
                        ProfileCategoryType.WORK_EDUCATION -> onEvent(EditProfileEvent.WorkEducationClicked)
                        ProfileCategoryType.SOCIAL_LINKS -> onNavigateToSocialLinks()
                        else -> Toast.makeText(context, context.getString(R.string.category_coming_soon, categoryType.name), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // 5. Category Navigation: PROFESSIONAL
        item {
            ProfessionalCategorySection(
                onCategoryClick = { _ ->
                    onEvent(EditProfileEvent.NavigateToProfessionalProfile)
                }
            )
        }

        // 6. Category Navigation: CUSTOMIZE
        item {
            CustomizationCategorySection(
                onCategoryClick = { categoryType ->
                    when (categoryType) {
                        ProfileCategoryType.PROFILE_APPEARANCE -> onNavigateToProfileAppearance()
                        ProfileCategoryType.FEATURED_CONTENT -> onNavigateToFeaturedContent()
                        else -> Toast.makeText(context, context.getString(R.string.category_coming_soon, categoryType.name), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
