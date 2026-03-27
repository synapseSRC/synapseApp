package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatFolder
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatFoldersScreen(
    viewModel: ChatFoldersViewModel,
    onNavigateBack: () -> Unit
) {
    val folders by viewModel.chatFolders.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<ChatFolder?>(null) }
    var dialogName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_chat_folders_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                dialogName = ""
                showAddDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_folder_title))
            }
        }
    ) { paddingValues ->
        if (folders.isEmpty()) {
            EmptyFoldersPlaceholder(Modifier.padding(paddingValues))
        } else {
            FolderList(
                folders = folders,
                modifier = Modifier.padding(paddingValues),
                onRename = { folder ->
                    folderToRename = folder
                    dialogName = folder.name
                },
                onDelete = { viewModel.removeFolder(it.id) },
                onReorder = { from, to -> viewModel.reorderFolders(from, to) }
            )
        }
    }

    // Add dialog
    if (showAddDialog) {
        FolderNameDialog(
            title = stringResource(R.string.create_folder_title),
            name = dialogName,
            onNameChange = { dialogName = it },
            onConfirm = {
                viewModel.addFolder(dialogName)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Rename dialog
    folderToRename?.let { folder ->
        FolderNameDialog(
            title = stringResource(R.string.rename_folder_title),
            name = dialogName,
            onNameChange = { dialogName = it },
            onConfirm = {
                viewModel.renameFolder(folder.id, dialogName)
                folderToRename = null
            },
            onDismiss = { folderToRename = null }
        )
    }
}

@Composable
private fun EmptyFoldersPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(Spacing.Medium))
        Text(
            text = stringResource(R.string.no_folders_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.Small))
        Text(
            text = stringResource(R.string.no_folders_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FolderList(
    folders: List<ChatFolder>,
    modifier: Modifier = Modifier,
    onRename: (ChatFolder) -> Unit,
    onDelete: (ChatFolder) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit
) {
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableIntStateOf(-1) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        itemsIndexed(folders, key = { _, f -> f.id }) { index, folder ->
            val elevation by animateDpAsState(
                targetValue = if (draggingIndex == index) 8.dp else 0.dp,
                label = "drag_elevation"
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(folder.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggingIndex = index },
                            onDragEnd = { draggingIndex = -1 },
                            onDragCancel = { draggingIndex = -1 },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val targetIndex = (index + (dragAmount.y / 80).toInt())
                                    .coerceIn(0, folders.lastIndex)
                                if (targetIndex != index) onReorder(index, targetIndex)
                            }
                        )
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(end = Spacing.Small)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (folder.includedChatIds.isNotEmpty()) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.folder_chats_count,
                                    folder.includedChatIds.size,
                                    folder.includedChatIds.size
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { onRename(folder) }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.rename_folder_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDelete(folder) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_folder_cd),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderNameDialog(
    title: String,
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.label_folder_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
