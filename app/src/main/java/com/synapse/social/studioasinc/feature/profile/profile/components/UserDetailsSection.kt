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
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import kotlinx.coroutines.delay

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
    val linkedAccounts: List<LinkedAccount> = emptyList()
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
        details.location,
        details.work,
        details.education,
        details.website,
        details.joinedDate,
        details.birthday,
        details.relationshipStatus,
        details.gender,
        details.pronouns
    ).any { !it.isNullOrBlank() } || details.linkedAccounts.isNotEmpty()

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
                text = "About",
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
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.Small))
                            Text("Edit Details")
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
        Text(if (expanded) "Less" else "More")
        Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation)
        )
    }
}

@Composable
private fun CollapsedSummary(details: UserDetails) {
    val summaryText = buildString {
        details.work?.takeIf { it.isNotBlank() }?.let { append(it) }
        details.location?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append(" • ")
            append(it)
        }
        details.joinedDate?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append(" • ")
            append("Joined $it")
        }

        if (isEmpty()) {
            details.website?.takeIf { it.isNotBlank() }?.let { append(it) }
            if (isEmpty()) {
                details.relationshipStatus?.takeIf { it.isNotBlank() }?.let { append(it) }
            }
            if (isEmpty()) {
                 if (details.linkedAccounts.isNotEmpty()) {
                     append("Linked Accounts: ${details.linkedAccounts.size}")
                 } else {
                     append("Tap 'More' to see details")
                 }
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
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
        val detailItems = buildList<Triple<ImageVector, String, String>> {
            details.location?.takeIf { it.isNotBlank() }?.let {
                add(Triple(Icons.Outlined.LocationOn, "Location", it))
            }
            details.work?.takeIf { it.isNotBlank() }?.let {
                add(Triple(Icons.Outlined.Work, "Work", it))
            }
            details.education?.takeIf { it.isNotBlank() }?.let {
                add(Triple(Icons.Outlined.School, "Education", it))
            }
            details.website?.takeIf { it.isNotBlank() }?.let {
                add(Triple(Icons.Outlined.Link, "Website", it))
            }
            details.joinedDate?.takeIf { it.isNotBlank() }?.let {
                add(Triple(Icons.Outlined.CalendarToday, "Joined", it))
            }
            details.birthday?.takeIf { it.isNotBlank() }?.let {
                add(Triple(Icons.Outlined.Cake, "Birthday", it))
            }
            details.relationshipStatus?.takeIf { it.isNotBlank() }?.let {
                add(Triple(Icons.Outlined.Favorite, "Relationship", it))
            }
            details.gender?.takeIf { it.isNotBlank() }?.let {
                add(Triple(Icons.Outlined.Person, "Gender", it))
            }
            details.pronouns?.takeIf { it.isNotBlank() }?.let {
                add(Triple(Icons.Outlined.Badge, "Pronouns", it))
            }
        }

        detailItems.forEachIndexed { index, (icon, label, value) ->
            SimpleDetailItem(
                icon = icon,
                label = label,
                value = value,
                isClickable = label == "Website",
                onClick = if (label == "Website") { { onWebsiteClick(value) } } else null,
                animationDelay = index * 30
            )
        }

        if (details.linkedAccounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.SmallMedium))
            Text(
                text = "Linked Accounts",
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
            modifier = Modifier.size(20.dp)
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
                modifier = Modifier.size(16.dp)
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
            text = "Share more about yourself",
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
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
            Text("Add Details")
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
