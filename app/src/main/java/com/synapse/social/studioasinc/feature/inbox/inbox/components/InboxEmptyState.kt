package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.models.EmptyStateType
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun InboxEmptyState(
    type: EmptyStateType,
    message: String? = null,
    onActionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val content = when (type) {
        EmptyStateType.CHATS -> EmptyStateContent(
            icon = Icons.Outlined.Email,
            title = stringResource(R.string.no_messages_yet),
            description = stringResource(R.string.your_conversations_will_appear_here),
            actionText = stringResource(R.string.new_message)
        )
        EmptyStateType.CALLS -> EmptyStateContent(
            icon = Icons.Outlined.Call,
            title = stringResource(R.string.no_calls_yet),
            description = stringResource(R.string.your_call_history_will_appear_here),
            actionText = null
        )
        EmptyStateType.CONTACTS -> EmptyStateContent(
            icon = Icons.Outlined.People,
            title = stringResource(R.string.connect_with_friends),
            description = stringResource(R.string.find_your_friends_on_synapse),
            actionText = stringResource(R.string.find_friends)
        )
        EmptyStateType.SEARCH_NO_RESULTS -> EmptyStateContent(
            icon = Icons.Outlined.SearchOff,
            title = stringResource(R.string.no_results_found),
            description = stringResource(R.string.try_searching_with_different_keywords),
            actionText = null
        )
        EmptyStateType.ARCHIVED -> EmptyStateContent(
            icon = Icons.Outlined.Archive,
            title = stringResource(R.string.no_archived_chats),
            description = stringResource(R.string.chats_you_archive_will_appear_here),
            actionText = null
        )
        EmptyStateType.ERROR -> EmptyStateContent(
            icon = Icons.Default.Warning,
            title = stringResource(R.string.something_went_wrong_title),
            description = message ?: stringResource(R.string.we_couldnt_load_your_messages),
            actionText = stringResource(R.string.action_retry)
        )
    }

    val icon = content.icon
    val title = content.title
    val description = content.description
    val actionText = content.actionText

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val iconScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = 200
        ),
        label = "contentAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(iconScale),
            contentAlignment = Alignment.Center
        ) {

            Surface(
                modifier = Modifier.size(100.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {}

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Sizes.AvatarLarge),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(Sizes.HeightSmall))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(contentAlpha)
        )

        Spacer(modifier = Modifier.height(Spacing.Small))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(contentAlpha)
                .padding(horizontal = Spacing.Large)
        )

        if (actionText != null) {
            Spacer(modifier = Modifier.height(Sizes.HeightSmall))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 400)) +
                    scaleIn(initialScale = 0.9f, animationSpec = tween(400, delayMillis = 400))
            ) {
                Button(
                    onClick = onActionClick,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.IconMedium)
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(actionText)
                }
            }
        }
    }
}

private data class EmptyStateContent(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionText: String?
)

@Composable
fun CompactEmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Sizes.IconGiant),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(Spacing.SmallMedium))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
