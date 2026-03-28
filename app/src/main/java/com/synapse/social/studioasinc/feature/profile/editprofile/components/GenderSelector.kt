package com.synapse.social.studioasinc.presentation.editprofile.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.Gender
import com.synapse.social.studioasinc.ui.settings.SettingsColors
import com.synapse.social.studioasinc.ui.settings.SettingsShapes
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun GenderSelector(
    selectedGender: Gender,
    onGenderSelected: (Gender) -> Unit,
    modifier: Modifier = Modifier
) {
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

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                CompactGenderOption(
                    label = stringResource(id = R.string.gender_male),
                    icon = Icons.Filled.Male,
                    selected = selectedGender == Gender.Male,
                    onClick = { onGenderSelected(Gender.Male) },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(Spacing.Small))

                CompactGenderOption(
                    label = stringResource(id = R.string.gender_female),
                    icon = Icons.Filled.Female,
                    selected = selectedGender == Gender.Female,
                    onClick = { onGenderSelected(Gender.Female) },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(Spacing.Small))

                CompactGenderOption(
                    label = stringResource(id = R.string.gender_other),
                    icon = Icons.Filled.Person,
                    selected = selectedGender == Gender.Hidden,
                    onClick = { onGenderSelected(Gender.Hidden) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CompactGenderOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        label = "bgColor"
    )
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "borderColor"
    )

    Surface(
        onClick = onClick,
        shape = SettingsShapes.itemShape,
        color = backgroundColor,
        border = BorderStroke(if (selected) Sizes.BorderDefault else Sizes.BorderThin, borderColor),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Spacing.SmallMedium)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizes.IconDefault)
            )

            Spacer(modifier = Modifier.height(Spacing.ExtraSmallMedium))

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GenderOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (selected) 1.0f else 0.98f, label = "scale")
    val backgroundColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        label = "bgColor"
    )
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "borderColor"
    )

    Surface(
        onClick = onClick,
        shape = SettingsShapes.itemShape,
        color = backgroundColor,
        border = BorderStroke(if (selected) Sizes.BorderDefault else Sizes.BorderThin, borderColor),
        modifier = Modifier.scale(scale)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Medium)
                .heightIn(min = Sizes.HeightDefault)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizes.IconLarge)
            )

            Spacer(modifier = Modifier.width(Spacing.Medium))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            RadioButton(
                selected = selected,
                onClick = null
            )
        }
    }
}
