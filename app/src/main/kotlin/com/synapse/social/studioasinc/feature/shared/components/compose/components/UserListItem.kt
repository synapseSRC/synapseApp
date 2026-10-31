package com.synapse.social.studioasinc.feature.shared.components.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography
import com.synapse.social.studioasinc.feature.shared.theme.GroupedItemPosition
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.ui.components.CircularAvatar

/**
 * Contained-list treatment shared with the Settings rows: 72dp two-line rows on the
 * [AppColors.cardBackground] tone, 28/8dp stacked corners and expressive press feedback.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UserListItem(
    user: User,
    position: GroupedItemPosition,
    onUserClick: () -> Unit,
    onMessageClick: () -> Unit,
    onFollowError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = user.displayName ?: user.username ?: user.nickname ?: ""

    Surface(
        onClick = onUserClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppSpacing.rowMinHeight),
        shape = position.shape,
        color = AppColors.cardBackground,
        contentColor = AppColors.itemContent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSpacing.rowHorizontalPadding,
                    vertical = AppSpacing.rowVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularAvatar(
                imageUrl = user.avatar,
                contentDescription = stringResource(R.string.author_avatar),
                size = Sizes.AvatarDefault,
                displayName = displayName
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.space16)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        style = AppTypography.itemTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (user.verify) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = Spacing.space4)
                                .size(AppSpacing.rowIconSize),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "@${user.username ?: displayName}",
                    style = AppTypography.itemSubtitle,
                    color = AppColors.supportingText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(Spacing.space12))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.space8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = onMessageClick,
                    modifier = Modifier.size(AppSpacing.iconBadgeSize),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AppColors.iconContainerBackground,
                        contentColor = AppColors.itemIcon
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Message,
                        contentDescription = stringResource(R.string.m_message),
                        modifier = Modifier.size(AppSpacing.rowIconSize)
                    )
                }

                FollowButtonCompose(
                    targetUserId = user.uid,
                    onError = onFollowError
                )
            }
        }
    }
}
