package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing


enum class ViewAsMode {
    PUBLIC, FRIENDS, SPECIFIC_USER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewAsBottomSheet(
    onDismiss: () -> Unit,
    onViewAsPublic: () -> Unit,
    onViewAsFriends: () -> Unit,
    onViewAsSpecificUser: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = Spacing.Medium)) {
            Text(
                stringResource(R.string.view_as_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
            )
            Text(
                stringResource(R.string.see_how_your_profile_looks),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall)
            )

            Spacer(modifier = Modifier.height(Spacing.Small))

            ViewAsOption(
                icon = Icons.Default.Public,
                text = stringResource(R.string.public_view),
                description = stringResource(R.string.how_everyone_sees),
                onClick = { onViewAsPublic(); onDismiss() }
            )
            ViewAsOption(
                icon = Icons.Default.Group,
                text = stringResource(R.string.friends_view),
                description = stringResource(R.string.how_your_friends_see),
                onClick = { onViewAsFriends(); onDismiss() }
            )
            ViewAsOption(
                icon = Icons.Default.Person,
                text = stringResource(R.string.specific_user),
                description = stringResource(R.string.see_as_specific_person),
                onClick = { onViewAsSpecificUser(); onDismiss() }
            )
        }
    }
}

@Composable
private fun ViewAsOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(Sizes.IconLarge))
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Column {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
