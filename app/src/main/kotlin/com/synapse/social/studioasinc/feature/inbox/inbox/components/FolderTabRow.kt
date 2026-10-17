package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatFolder
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FolderTabRow(
    folders: List<ChatFolder>,
    selectedFolderId: String?,
    onFolderSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val filterChipShapes = FilterChipDefaults.shapes()

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space8),
        horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
    ) {
        item {
            FilterChip(
                selected = selectedFolderId == null,
                onClick = { onFolderSelected(null) },
                label = { Text(stringResource(R.string.label_all)) },
                shapes = filterChipShapes
            )
        }
        items(folders, key = { it.id }) { folder ->
            FilterChip(
                selected = selectedFolderId == folder.id,
                onClick = { onFolderSelected(folder.id) },
                label = { Text(folder.name) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.IconMedium)
                    )
                },
                shapes = filterChipShapes
            )
        }
    }
}
