package com.synapse.social.studioasinc.presentation.editprofile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.Gender
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileUiState
import com.synapse.social.studioasinc.ui.settings.SettingsColors
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

data class ProfileCompletenessInfo(
    val percentage: Int,
    val recommendedImprovementsCount: Int,
    val missingItemResIds: List<Int>
)

fun calculateProfileCompleteness(uiState: EditProfileUiState): ProfileCompletenessInfo {
    val items = mutableListOf<Pair<Boolean, Int>>() // Pair(isCompleted, weight)
    val missingResIds = mutableListOf<Int>()

    val hasAvatar = !uiState.avatarUrl.isNullOrBlank() || uiState.pendingAvatarUri != null
    items.add(Pair(hasAvatar, 15))
    if (!hasAvatar) missingResIds.add(R.string.completeness_item_avatar)

    val hasCover = !uiState.coverUrl.isNullOrBlank() || uiState.pendingCoverUri != null
    items.add(Pair(hasCover, 10))
    if (!hasCover) missingResIds.add(R.string.completeness_item_cover)

    val hasName = uiState.nickname.isNotBlank()
    items.add(Pair(hasName, 15))
    if (!hasName) missingResIds.add(R.string.completeness_item_name)

    val hasUsername = uiState.username.isNotBlank()
    items.add(Pair(hasUsername, 15))
    if (!hasUsername) missingResIds.add(R.string.completeness_item_username)

    val hasBio = uiState.bio.isNotBlank()
    items.add(Pair(hasBio, 15))
    if (!hasBio) missingResIds.add(R.string.completeness_item_bio)

    val hasGender = uiState.selectedGender != Gender.Hidden
    items.add(Pair(hasGender, 10))
    if (!hasGender) missingResIds.add(R.string.completeness_item_gender)

    val hasLocation = !uiState.selectedRegion.isNullOrBlank() || uiState.currentCity.isNotBlank() || uiState.hometown.isNotBlank()
    items.add(Pair(hasLocation, 10))
    if (!hasLocation) missingResIds.add(R.string.completeness_item_location)

    val hasWorkOrLinks = uiState.occupation.isNotBlank() || uiState.workplace.isNotBlank() ||
            uiState.education.isNotBlank() || uiState.personalWebsite.isNotBlank() ||
            uiState.githubProfile.isNotBlank() || uiState.discordTag.isNotBlank()
    items.add(Pair(hasWorkOrLinks, 10))
    if (!hasWorkOrLinks) missingResIds.add(R.string.completeness_item_work_links)

    val totalScore = items.filter { it.first }.sumOf { it.second }
    return ProfileCompletenessInfo(
        percentage = totalScore.coerceIn(0, 100),
        recommendedImprovementsCount = missingResIds.size,
        missingItemResIds = missingResIds
    )
}

@Composable
fun ProfileCompletionCard(
    uiState: EditProfileUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completeness = calculateProfileCompleteness(uiState)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Sizes.CornerExtraLarge),
        color = SettingsColors.cardBackground,
        tonalElevation = Spacing.None
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.profile_completeness_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${completeness.percentage}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Small))

            LinearProgressIndicator(
                progress = { completeness.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            Spacer(modifier = Modifier.height(Spacing.Small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (completeness.percentage == 100) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                        Text(
                            text = stringResource(R.string.profile_complete),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.complete_your_profile),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(
                                R.string.improvements_recommended,
                                completeness.recommendedImprovementsCount
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
