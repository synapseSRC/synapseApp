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
import coil3.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.synapse.social.studioasinc.core.util.ImageLoader
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppShapes
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography

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
        shape = AppShapes.cardShape,
        color = AppColors.cardBackgroundElevated,
        tonalElevation = Sizes.ElevationLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.profileHeaderPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(Sizes.AvatarSemiLarge)
            ) {
                com.synapse.social.studioasinc.feature.shared.components.UserAvatar(
                    avatarUrl = avatarUrl,
                    displayName = displayName,
                    size = Sizes.AvatarSemiLarge,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        width = Sizes.BorderDefault,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {}
            }

            Spacer(modifier = Modifier.width(Spacing.space16))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayName,
                    style = AppTypography.profileName,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = email,
                    style = AppTypography.profileEmail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

