package com.synapse.social.studioasinc.feature.profile.personalinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.model.RelationshipStatus
import com.synapse.social.studioasinc.domain.model.Gender
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.ui.settings.SettingsClickableItem
import com.synapse.social.studioasinc.ui.settings.SettingsHeaderItem
import com.synapse.social.studioasinc.ui.settings.SettingsItemPosition
import com.synapse.social.studioasinc.ui.settings.SettingsSpacing

@Composable
fun PersonalInformationContent(
    uiState: PersonalInformationUiState,
    paddingValues: PaddingValues,
    onPronounsClick: () -> Unit,
    onBirthdayClick: () -> Unit,
    onGenderClick: () -> Unit,
    onRelationshipStatusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unsetLabel = stringResource(R.string.not_set)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = SettingsSpacing.screenPadding,
            end = SettingsSpacing.screenPadding,
            top = paddingValues.calculateTopPadding() + Spacing.Small,
            bottom = paddingValues.calculateBottomPadding() + Spacing.Large
        ),
        verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
    ) {
        // Section 1: PERSONAL IDENTITY
        item {
            Column {
                SettingsHeaderItem(title = stringResource(R.string.section_personal_identity))

                SettingsClickableItem(
                    title = stringResource(R.string.pronouns),
                    subtitle = uiState.pronouns.ifBlank { unsetLabel },
                    imageVector = Icons.Filled.Person,
                    onClick = onPronounsClick,
                    position = SettingsItemPosition.Single
                )
            }
        }

        // Section 2: BASIC DETAILS
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing)
            ) {
                SettingsHeaderItem(title = stringResource(R.string.section_basic_details))

                val formattedBirthday = PersonalInformationViewModel.formatLocaleDate(uiState.birthday)
                SettingsClickableItem(
                    title = stringResource(R.string.birthday),
                    subtitle = formattedBirthday.ifBlank { unsetLabel },
                    imageVector = Icons.Filled.DateRange,
                    onClick = onBirthdayClick,
                    position = SettingsItemPosition.Top
                )

                val genderText = when (uiState.gender) {
                    Gender.Male -> stringResource(R.string.gender_male)
                    Gender.Female -> stringResource(R.string.gender_female)
                    Gender.Hidden -> stringResource(R.string.gender_gone_title)
                }
                SettingsClickableItem(
                    title = stringResource(R.string.gender),
                    subtitle = genderText,
                    imageVector = Icons.Filled.PersonOutline,
                    onClick = onGenderClick,
                    position = SettingsItemPosition.Middle
                )

                val relationshipText = uiState.relationshipStatus?.displayName ?: unsetLabel
                SettingsClickableItem(
                    title = stringResource(R.string.relationship),
                    subtitle = relationshipText,
                    imageVector = Icons.Filled.Favorite,
                    onClick = onRelationshipStatusClick,
                    position = SettingsItemPosition.Bottom
                )
            }
        }
    }
}
