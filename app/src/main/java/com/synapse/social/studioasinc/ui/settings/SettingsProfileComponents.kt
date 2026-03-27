package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.synapse.social.studioasinc.core.util.ImageLoader
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun ProfileHeaderCard(
    displayName: String,
    email: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier
) {
    val profileAvatarDescription = stringResource(R.string.settings_profile_avatar_description)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsShapes.cardShape,
        color = SettingsColors.cardBackgroundElevated,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SettingsSpacing.profileHeaderPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Box(
                modifier = Modifier.size(SettingsSpacing.avatarSize)
            ) {
                if (avatarUrl != null && avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageLoader.buildImageRequest(LocalContext.current, avatarUrl),
                        contentDescription = "$profileAvatarDescription, $displayName",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "$profileAvatarDescription, $displayName",
                                modifier = Modifier.size(Sizes.IconHuge),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }


                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        width = Spacing.Tiny,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {}
            }

            Spacer(modifier = Modifier.width(Spacing.Medium))


            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayName,
                    style = SettingsTypography.profileName,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = email,
                    style = SettingsTypography.profileEmail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



@Composable
fun SettingsInfoItem(
    title: String,
    value: String,
    icon: ImageVector? = null,
    onCopy: (() -> Unit)? = null
) {
    val modifier = if (onCopy != null) {
        Modifier
            .clickable(onClick = onCopy)
            .padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = SettingsSpacing.itemVerticalPadding
            )
    } else {
        Modifier
            .padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = SettingsSpacing.itemVerticalPadding
            )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(SettingsSpacing.iconSize),
                    tint = SettingsColors.itemIcon
                )
            }

            Column {
                Text(
                    text = title,
                    style = SettingsTypography.itemTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (value.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                    Text(
                        text = value,
                        style = SettingsTypography.itemSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (onCopy != null) {
             Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "Copy",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
