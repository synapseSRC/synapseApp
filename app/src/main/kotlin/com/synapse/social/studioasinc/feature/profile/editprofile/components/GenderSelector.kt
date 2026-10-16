package com.synapse.social.studioasinc.presentation.editprofile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.Gender
import com.synapse.social.studioasinc.ui.settings.SettingsColors
import com.synapse.social.studioasinc.ui.settings.SettingsShapes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GenderSelector(
    selectedGender: Gender,
    onGenderSelected: (Gender) -> Unit,
    modifier: Modifier = Modifier
) {
    val maleLabel = stringResource(id = R.string.gender_male)
    val femaleLabel = stringResource(id = R.string.gender_female)
    val otherLabel = stringResource(id = R.string.gender_other)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsShapes.sectionShape,
        color = SettingsColors.cardBackground,
        tonalElevation = Spacing.None
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium)
        ) {
            Text(
                text = stringResource(id = R.string.gender),
                style = MaterialTheme.typography.titleMedium,
                color = SettingsColors.sectionTitle
            )

            Spacer(modifier = Modifier.height(Spacing.SmallMedium))

            ButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                overflowIndicator = { menuState ->
                    ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                },
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                toggleableItem(
                    checked = selectedGender == Gender.Male,
                    label = maleLabel,
                    onCheckedChange = { checked ->
                        if (checked) onGenderSelected(Gender.Male)
                    },
                    icon = {
                        Icon(imageVector = Icons.Filled.Male, contentDescription = null)
                    },
                    weight = 1f
                )

                toggleableItem(
                    checked = selectedGender == Gender.Female,
                    label = femaleLabel,
                    onCheckedChange = { checked ->
                        if (checked) onGenderSelected(Gender.Female)
                    },
                    icon = {
                        Icon(imageVector = Icons.Filled.Female, contentDescription = null)
                    },
                    weight = 1f
                )

                toggleableItem(
                    checked = selectedGender == Gender.Hidden,
                    label = otherLabel,
                    onCheckedChange = { checked ->
                        if (checked) onGenderSelected(Gender.Hidden)
                    },
                    icon = {
                        Icon(imageVector = Icons.Filled.Person, contentDescription = null)
                    },
                    weight = 1f
                )
            }
        }
    }
}
