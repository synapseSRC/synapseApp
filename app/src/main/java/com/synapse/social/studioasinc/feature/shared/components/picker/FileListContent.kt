package com.synapse.social.studioasinc.feature.shared.components.picker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.log10
import kotlin.math.pow
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.components.UserAvatar
import com.synapse.social.studioasinc.feature.shared.components.animatedShape
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun FileListContent(
    files: List<PickedFile>,
    isLoading: Boolean,
    category: FilePickerCategory,
    onFileClicked: (PickedFile) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (files.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.picker_empty_files),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    if (category == FilePickerCategory.CONTACT) {
        val groupedContacts = remember(files) {
            files.groupBy { it.fileName.firstOrNull()?.uppercaseChar() ?: '#' }
                .toSortedMap()
        }

        LazyColumn(modifier = modifier.fillMaxSize()) {
            groupedContacts.forEach { (initial, contacts) ->
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                            .padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall)
                    ) {
                        Text(
                            text = initial.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(contacts, key = { it.uri.toString() }) { file ->
                    ContactListItem(
                        file = file,
                        onClick = { onFileClicked(file) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = Spacing.Huge + Spacing.Large),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(files, key = { it.uri.toString() }) { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFileClicked(file) }
                        .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
                ) {
                    Icon(
                        imageVector = when (category) {
                            FilePickerCategory.AUDIO -> Icons.Default.AudioFile
                            else -> Icons.AutoMirrored.Filled.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Sizes.IconLarge)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatFileSize(file.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(start = Spacing.Huge))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ContactListItem(
    file: PickedFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ButtonDefaults.animatedShape())
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallPlus),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            avatarUrl = null, // Contacts don't have avatar URLs in this implementation yet, using default
            size = Sizes.AvatarMedium
        )

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.fileName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.picker_category_contact),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
    return "%.1f %s".format(bytes / 1024.0.pow(digitGroups), units[digitGroups])
}
