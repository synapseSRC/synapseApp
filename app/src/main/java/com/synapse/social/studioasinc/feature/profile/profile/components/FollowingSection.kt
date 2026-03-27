package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing


enum class FollowingFilter {
    ALL, MUTUAL, RECENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingSection(
    users: List<FollowingUser>,
    selectedFilter: FollowingFilter,
    onFilterSelected: (FollowingFilter) -> Unit,
    onUserClick: (FollowingUser) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (users.isEmpty()) {
        EmptyFollowingState(modifier = modifier)
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
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
                        text = stringResource(R.string.following),
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onSeeAllClick) {
                        Text(stringResource(R.string.see_all))
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.Small))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    FollowingFilter.values().forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { onFilterSelected(filter) },
                            label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.SmallMedium))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.SmallMedium)
                ) {
                    items(users) { user ->
                        FollowingUserItem(
                            user = user,
                            onClick = { onUserClick(user) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFollowingState(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.ExtraLarge),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.no_following),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.find_people_to_follow),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
private fun FollowingSectionPreview() {
    MaterialTheme {
        FollowingSection(
            users = List(5) {
                FollowingUser(
                    id = it.toString(),
                    username = "user$it",
                    name = "User $it",
                    avatarUrl = null,
                    isMutual = it % 2 == 0
                )
            },
            selectedFilter = FollowingFilter.ALL,
            onFilterSelected = {},
            onUserClick = {},
            onSeeAllClick = {}
        )
    }
}
