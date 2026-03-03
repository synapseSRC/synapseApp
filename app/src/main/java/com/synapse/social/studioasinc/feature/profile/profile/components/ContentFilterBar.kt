package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.profile.profile.ProfileContentFilter
import com.synapse.social.studioasinc.feature.shared.theme.Spacing



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentFilterBar(
    selectedFilter: ProfileContentFilter,
    onFilterSelected: (ProfileContentFilter) -> Unit,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.SmallMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(ProfileContentFilter.values()) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = getFilterLabel(filter),
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    shape = CircleShape
                )
            }
        }
    }
}



private fun getFilterLabel(filter: ProfileContentFilter): String {
    return when (filter) {
        ProfileContentFilter.POSTS -> "Posts"
        ProfileContentFilter.PHOTOS -> "Photos"
        ProfileContentFilter.REELS -> "Reels"
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentFilterBarPreview() {
    MaterialTheme {
        ContentFilterBar(
            selectedFilter = ProfileContentFilter.POSTS,
            onFilterSelected = {}
        )
    }
}
