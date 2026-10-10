package com.synapse.social.studioasinc.presentation.editprofile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.settings.SettingsHeaderItem
import com.synapse.social.studioasinc.ui.settings.SettingsItemPosition
import com.synapse.social.studioasinc.ui.settings.SettingsNavigationItem
import com.synapse.social.studioasinc.ui.settings.SettingsSpacing

enum class ProfileCategoryType {
    PROFILE_MEDIA,
    PERSONAL_INFO,
    LOCATION,
    WORK_EDUCATION,
    SOCIAL_LINKS,
    INTERESTS_SKILLS,
    PROFESSIONAL_PROFILE,
    PROFILE_APPEARANCE,
    PROFILE_LAYOUT,
    FEATURED_CONTENT
}

@Composable
fun ProfileCategorySection(
    selectedRegion: String?,
    onNavigateToProfileHistory: () -> Unit,
    onNavigateToCoverHistory: () -> Unit,
    onNavigateToRegionSelection: (String) -> Unit,
    onNavigateToPersonalInfo: () -> Unit,
    onNavigateToLocation: () -> Unit = {},
    onCategoryClick: (ProfileCategoryType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsHeaderItem(title = stringResource(R.string.section_profile))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing)
        ) {
            // 1. Profile Media (Profile Photo History) - Top
            SettingsNavigationItem(
                title = stringResource(R.string.nav_profile_media),
                subtitle = stringResource(R.string.nav_profile_media_sub),
                imageVector = Icons.Filled.PermMedia,
                onClick = { onCategoryClick(ProfileCategoryType.PROFILE_MEDIA) },
                position = SettingsItemPosition.Top
            )

            // 3. Personal Information - Middle
            SettingsNavigationItem(
                title = stringResource(R.string.nav_personal_info),
                subtitle = stringResource(R.string.nav_personal_info_sub),
                imageVector = Icons.Filled.PersonOutline,
                onClick = onNavigateToPersonalInfo,
                position = SettingsItemPosition.Middle
            )

            // 4. Location - Middle
            SettingsNavigationItem(
                title = stringResource(R.string.nav_location),
                subtitle = selectedRegion?.ifBlank { null } ?: stringResource(R.string.nav_location_sub),
                imageVector = Icons.Filled.LocationOn,
                onClick = { onCategoryClick(ProfileCategoryType.LOCATION) },
                position = SettingsItemPosition.Middle
            )

            // 5. Work & Education - Middle
            SettingsNavigationItem(
                title = stringResource(R.string.nav_work_education),
                subtitle = stringResource(R.string.nav_work_education_sub),
                imageVector = Icons.Filled.Work,
                onClick = { onCategoryClick(ProfileCategoryType.WORK_EDUCATION) },
                position = SettingsItemPosition.Middle
            )

            // 6. Social & Links - Middle
            SettingsNavigationItem(
                title = stringResource(R.string.nav_social_links),
                subtitle = stringResource(R.string.nav_social_links_sub),
                imageVector = Icons.Filled.Language,
                onClick = { onCategoryClick(ProfileCategoryType.SOCIAL_LINKS) },
                position = SettingsItemPosition.Middle
            )

            // 7. Interests & Skills - Bottom
            SettingsNavigationItem(
                title = stringResource(R.string.nav_interests_skills),
                subtitle = stringResource(R.string.nav_interests_skills_sub),
                imageVector = Icons.Filled.Star,
                onClick = { onCategoryClick(ProfileCategoryType.INTERESTS_SKILLS) },
                position = SettingsItemPosition.Bottom
            )
        }
    }
}

@Composable
fun ProfessionalCategorySection(
    onCategoryClick: (ProfileCategoryType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsHeaderItem(title = stringResource(R.string.section_professional))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing)
        ) {
            SettingsNavigationItem(
                title = stringResource(R.string.nav_professional_profile),
                subtitle = stringResource(R.string.nav_professional_profile_sub),
                imageVector = Icons.Filled.BusinessCenter,
                onClick = { onCategoryClick(ProfileCategoryType.PROFESSIONAL_PROFILE) },
                position = SettingsItemPosition.Single
            )
        }
    }
}

@Composable
fun CustomizationCategorySection(
    onCategoryClick: (ProfileCategoryType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsHeaderItem(title = stringResource(R.string.section_customize))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing)
        ) {
            // Profile Appearance - Top
            SettingsNavigationItem(
                title = stringResource(R.string.nav_profile_appearance),
                subtitle = stringResource(R.string.nav_profile_appearance_sub),
                imageVector = Icons.Filled.Palette,
                onClick = { onCategoryClick(ProfileCategoryType.PROFILE_APPEARANCE) },
                position = SettingsItemPosition.Top
            )

            // Profile Layout - Middle
            SettingsNavigationItem(
                title = stringResource(R.string.nav_profile_layout),
                subtitle = stringResource(R.string.nav_profile_layout_sub),
                imageVector = Icons.Filled.ViewQuilt,
                onClick = { onCategoryClick(ProfileCategoryType.PROFILE_LAYOUT) },
                position = SettingsItemPosition.Middle
            )

            // Featured Content - Bottom
            SettingsNavigationItem(
                title = stringResource(R.string.nav_featured_content),
                subtitle = stringResource(R.string.nav_featured_content_sub),
                imageVector = Icons.Filled.AutoAwesome,
                onClick = { onCategoryClick(ProfileCategoryType.FEATURED_CONTENT) },
                position = SettingsItemPosition.Bottom
            )
        }
    }
}
