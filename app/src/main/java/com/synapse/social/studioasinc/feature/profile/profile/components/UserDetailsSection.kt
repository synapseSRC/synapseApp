package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R

import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import kotlinx.coroutines.delay
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

data class UserDetails(
    val location: String? = null,
    val joinedDate: String? = null,
    val relationshipStatus: String? = null,
    val birthday: String? = null,
    val work: String? = null,
    val education: String? = null,
    val website: String? = null,
    val gender: String? = null,
    val pronouns: String? = null,
    val linkedAccounts: List<LinkedAccount> = emptyList(),
    // Extended fields
    val currentCity: String? = null,
    val hometown: String? = null,
    val occupation: String? = null,
    val workplace: String? = null,
    val discordTag: String? = null,
    val githubProfile: String? = null,
    val personalWebsite: String? = null,
    val publicEmail: String? = null
)

data class LinkedAccount(
    val platform: String,
    val username: String
)

@Composable
fun UserDetailsSection(
    details: UserDetails,
    isOwnProfile: Boolean,
    onCustomizeClick: () -> Unit,
    onWebsiteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val hasDetails = listOfNotNull(
        details.location, details.work, details.education, details.website,
        details.joinedDate, details.birthday, details.relationshipStatus,
        details.gender, details.pronouns, details.currentCity, details.hometown,
        details.occupation, details.workplace, details.discordTag,
        details.githubProfile, details.personalWebsite, details.publicEmail
    ).any { it.isNotBlank() } || details.linkedAccounts.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.about_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (hasDetails) {
                ExpandCollapseButton(
                    expanded = expanded,
                    onClick = { expanded = !expanded }
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Small))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
        ) {
            if (hasDetails) {
                if (!expanded) {
                    CollapsedSummary(details = details)
                } else {
                    ExpandedDetailsContent(
                        details = details,
                        onWebsiteClick = onWebsiteClick
                    )

                    if (isOwnProfile) {
                        Spacer(modifier = Modifier.height(Spacing.Medium))
                        Button(
                            onClick = onCustomizeClick,
                            modifier = Modifier.fillMaxWidth().height(Spacing.Huge),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(Sizes.IconMedium)
                            )
                            Spacer(modifier = Modifier.width(Spacing.Small))
                            Text(stringResource(R.string.action_edit_details))
                        }
                    }
                }
            }

            if (!hasDetails && isOwnProfile) {
                Spacer(modifier = Modifier.height(Spacing.Small))
                EmptyDetailsState(onAddClick = onCustomizeClick)
            }
        }
    }
}

@Composable
private fun ExpandCollapseButton(
    expanded: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "expandRotation"
    )

    TextButton(
        onClick = onClick,
        modifier = Modifier.minimumInteractiveComponentSize()
    ) {
        Text(if (expanded) stringResource(R.string.less) else stringResource(R.string.more))
        Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
            modifier = Modifier
                .size(Sizes.IconDefault)
                .rotate(rotation)
        )
    }
}

@Composable
private fun CollapsedSummary(details: UserDetails) {
    val workSummary = details.occupation?.takeIf { it.isNotBlank() }
        ?: details.workplace?.takeIf { it.isNotBlank() }
        ?: details.work?.takeIf { it.isNotBlank() }
    val locationSummary = details.currentCity?.takeIf { it.isNotBlank() }
        ?: details.location?.takeIf { it.isNotBlank() }

    val summaryText = buildString {
        workSummary?.let { append(it) }
        locationSummary?.let {
            if (isNotEmpty()) append(" • ")
            append(it)
        }
        details.joinedDate?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append(" • ")
            append("${stringResource(R.string.joined)} $it")
        }
        if (isEmpty()) {
            (details.personalWebsite ?: details.website)?.takeIf { it.isNotBlank() }?.let { append(it) }
        }
        if (isEmpty()) {
            details.relationshipStatus?.takeIf { it.isNotBlank() }?.let { append(it) }
        }
        if (isEmpty()) {
            if (details.linkedAccounts.isNotEmpty()) {
                append("${stringResource(R.string.linked_accounts)}: ${details.linkedAccounts.size}")
            } else {
                append(stringResource(R.string.tap_more_to_see_details))
            }
        }
    }

    Text(
        text = summaryText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ExpandedDetailsContent(
    details: UserDetails,
    onWebsiteClick: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    @Composable
    fun SectionHeader(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.Small, top = Spacing.SmallMedium, bottom = Spacing.ExtraSmall)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
        // Personal info (no section header — top-level)
        details.joinedDate?.takeIf { it.isNotBlank() }?.let {
            SimpleDetailItem(Icons.Outlined.CalendarToday, stringResource(R.string.joined), it, animationDelay = 0)
        }
        details.birthday?.takeIf { it.isNotBlank() }?.let {
            SimpleDetailItem(Icons.Outlined.Cake, stringResource(R.string.birthday), it, animationDelay = 30)
        }
        details.relationshipStatus?.takeIf { it.isNotBlank() }?.let {
            SimpleDetailItem(Icons.Outlined.Favorite, stringResource(R.string.relationship), it, animationDelay = 60)
        }
        details.gender?.takeIf { it.isNotBlank() }?.let {
            SimpleDetailItem(Icons.Outlined.Person, stringResource(R.string.gender_detail), it, animationDelay = 90)
        }
        details.pronouns?.takeIf { it.isNotBlank() }?.let {
            SimpleDetailItem(Icons.Outlined.Badge, stringResource(R.string.pronouns), it, animationDelay = 120)
        }

        // Location section
        val locationItems = listOfNotNull(
            details.location?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.LocationOn, stringResource(R.string.location), it) },
            details.currentCity?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.LocationOn, stringResource(R.string.current_city), it) },
            details.hometown?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.Home, stringResource(R.string.hometown), it) }
        )
        if (locationItems.isNotEmpty()) {
            SectionHeader(stringResource(R.string.section_location))
            locationItems.forEachIndexed { i, (icon, label, value) ->
                SimpleDetailItem(icon, label, value, animationDelay = 150 + i * 30)
            }
        }

        // Work & Education section
        val workItems = listOfNotNull(
            details.occupation?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.Work, stringResource(R.string.occupation), it) },
            details.workplace?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.Business, stringResource(R.string.workplace), it) },
            details.work?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.Work, stringResource(R.string.work), it) },
            details.education?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.School, stringResource(R.string.education), it) }
        )
        if (workItems.isNotEmpty()) {
            SectionHeader(stringResource(R.string.section_work_education))
            workItems.forEachIndexed { i, (icon, label, value) ->
                SimpleDetailItem(icon, label, value, animationDelay = 240 + i * 30)
            }
        }

        // Social & Web section
        val socialItems = listOfNotNull(
            details.website?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.Link, stringResource(R.string.website), it) },
            details.personalWebsite?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.Link, stringResource(R.string.personal_website), it) },
            details.githubProfile?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.Code, stringResource(R.string.github), it) },
            details.discordTag?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.Tag, stringResource(R.string.discord), it) },
            details.publicEmail?.takeIf { it.isNotBlank() }?.let { Triple(Icons.Outlined.Email, stringResource(R.string.public_email), it) }
        )
        if (socialItems.isNotEmpty()) {
            SectionHeader(stringResource(R.string.section_social_web))
            val labelGithub = stringResource(R.string.github)
            val labelEmail = stringResource(R.string.public_email)
            val labelWebsite = stringResource(R.string.website)
            val labelPersonalWebsite = stringResource(R.string.personal_website)
            socialItems.forEachIndexed { i, (icon, label, value) ->
                val isLink = label in listOf(labelWebsite, labelPersonalWebsite, labelGithub, labelEmail)
                val capturedLabel = label
                val capturedValue = value
                SimpleDetailItem(
                    icon = icon,
                    label = label,
                    value = value,
                    isClickable = isLink,
                    onClick = if (isLink) {
                        {
                            val url = when (capturedLabel) {
                                labelGithub -> "https://github.com/$capturedValue"
                                labelEmail -> "mailto:$capturedValue"
                                else -> capturedValue
                            }
                            try { uriHandler.openUri(url) } catch (_: Exception) { onWebsiteClick(url) }
                        }
                    } else null,
                    animationDelay = 330 + i * 30
                )
            }
        }

        if (details.linkedAccounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.SmallMedium))
            Text(
                text = stringResource(R.string.linked_accounts),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.Small)
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                modifier = Modifier.padding(horizontal = Spacing.Small)
            ) {
                details.linkedAccounts.forEach { account ->
                    LinkedAccountChip(account = account)
                }
            }
        }
    }
}

@Composable
private fun SimpleDetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    isClickable: Boolean = false,
    onClick: (() -> Unit)? = null,
    animationDelay: Int = 0,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "detailAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .then(
                if (isClickable && onClick != null) {
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(vertical = Spacing.Small, horizontal = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Sizes.IconDefault)
        )
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isClickable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isClickable) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LinkedAccountChip(account: LinkedAccount) {
    val uriHandler = LocalUriHandler.current
    AssistChip(
        onClick = {
            val url = getPlatformUrl(account.platform, account.username)
            if (url.isNotEmpty()) {
                try {
                    uriHandler.openUri(url)
                } catch (e: Exception) {

                }
            }
        },
        label = { Text(account.platform) },
        leadingIcon = {
            Icon(
                imageVector = getPlatformIcon(account.platform),
                contentDescription = null,
                modifier = Modifier.size(Sizes.IconSemiMedium)
            )
        }
    )
}

private fun getPlatformUrl(platform: String, username: String): String {
    return when (platform.lowercase()) {
        "twitter", "x" -> "https://x.com/$username"
        "instagram" -> "https://instagram.com/$username"
        "facebook" -> "https://facebook.com/$username"
        "linkedin" -> "https://linkedin.com/in/$username"
        "github" -> "https://github.com/$username"
        "youtube" -> "https://youtube.com/@$username"
        else -> ""
    }
}

private fun getPlatformIcon(platform: String): ImageVector {
    return when (platform.lowercase()) {
        "twitter", "x" -> Icons.Default.AlternateEmail
        "instagram" -> Icons.Default.CameraAlt
        "facebook" -> Icons.Default.Facebook
        "linkedin" -> Icons.Default.Work
        "github" -> Icons.Default.Code
        "youtube" -> Icons.Default.PlayCircle
        else -> Icons.Default.Link
    }
}

@Composable
private fun EmptyDetailsState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.share_more_about_yourself),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        TextButton(
            onClick = onAddClick,
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(Sizes.IconMedium)
            )
            Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
            Text(stringResource(R.string.add_details))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserDetailsSectionPreview() {
    MaterialTheme {
        UserDetailsSection(
            details = UserDetails(
                location = "San Francisco, CA",
                joinedDate = "January 2024",
                work = "Software Engineer",
                education = "Stanford University",
                website = "https://example.com"
            ),
            isOwnProfile = true,
            onCustomizeClick = {},
            onWebsiteClick = {}
        )
    }
}
