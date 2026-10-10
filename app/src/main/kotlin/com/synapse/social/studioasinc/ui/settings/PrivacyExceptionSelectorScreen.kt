package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyExceptionSelectorScreen(
    title: String,
    uiState: PrivacyExceptionSelectorUiState,
    onSearchQueryChanged: (String) -> Unit,
    onToggleUserSelection: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SettingsSpacing.screenPadding)
                ) {
                    Button(
                        onClick = onSaveClicked,
                        enabled = !uiState.isSaving && !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = SettingsSpacing.minTouchTarget),
                        shape = SettingsShapes.itemShape
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.privacy_save_exception_list, uiState.selectedUserIds.size),
                                style = SettingsTypography.buttonText
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SettingsSpacing.screenPadding, vertical = Spacing.Small),
                placeholder = { Text(stringResource(R.string.settings_search_settings_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.privacy_clear_search)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = SettingsShapes.inputShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.Large),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.Large),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.searchQuery.isEmpty()) stringResource(R.string.privacy_no_contacts_found) else stringResource(R.string.privacy_no_matching_contacts),
                        style = SettingsTypography.itemSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = SettingsSpacing.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing),
                    contentPadding = PaddingValues(bottom = Spacing.Large)
                ) {
                    itemsIndexed(
                        items = uiState.filteredContacts,
                        key = { _, user -> user.uid }
                    ) { index, user ->
                        val position = when {
                            uiState.filteredContacts.size == 1 -> SettingsItemPosition.Single
                            index == 0 -> SettingsItemPosition.Top
                            index == uiState.filteredContacts.lastIndex -> SettingsItemPosition.Bottom
                            else -> SettingsItemPosition.Middle
                        }

                        val isSelected = uiState.selectedUserIds.contains(user.uid)
                        val displayName = user.displayName ?: user.username ?: stringResource(R.string.default_user_name)
                        val handle = user.username?.let { "@$it" } ?: ""
                        val selectedText = if (isSelected) stringResource(R.string.privacy_selected) else stringResource(R.string.privacy_not_selected)
                        val cd = stringResource(R.string.privacy_contact_selection_cd, displayName, handle, selectedText)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = position.getShape(),
                            color = SettingsColors.cardBackground
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        role = Role.Checkbox,
                                        onClick = { onToggleUserSelection(user.uid) }
                                    )
                                    .semantics(mergeDescendants = true) {
                                        role = Role.Checkbox
                                        contentDescription = cd
                                    }
                                    .padding(
                                        horizontal = SettingsSpacing.itemHorizontalPadding,
                                        vertical = SettingsSpacing.itemVerticalPadding
                                    )
                                    .heightIn(min = SettingsSpacing.minTouchTarget),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                if (!user.avatar.isNullOrBlank()) {
                                    AsyncImage(
                                        model = user.avatar,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(SettingsColors.iconContainerBackground),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = SettingsColors.itemIcon,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = displayName,
                                        style = SettingsTypography.itemTitle,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (handle.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                                        Text(
                                            text = handle,
                                            style = SettingsTypography.itemSubtitle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(Spacing.Medium))

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
