@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.synapse.social.studioasinc.feature.inbox.inbox

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.UserStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinRequest
import kotlinx.coroutines.launch

@Composable
internal fun MembersStage(
    users: List<User>,
    selectedUsers: List<User>,
    searchQuery: String,
    isLoading: Boolean,
    error: GroupCreationError?,
    focusRequester: FocusRequester,
    onSearch: (String) -> Unit,
    onToggleUser: (User) -> Unit,
    onRemoveUser: (User) -> Unit,
    onClearError: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space8),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.group_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) IconButton(onClick = { onSearch("") }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_search))
                    }
                },
                singleLine = true
            )
        }
        if (selectedUsers.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space8), modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.space8)) {
                    items(selectedUsers, key = { it.uid }) { user ->
                        InputChip(
                            selected = true,
                            onClick = { onRemoveUser(user) },
                            label = { Text(userDisplayName(user), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_member)) }
                        )
                    }
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.group_selected_count, selectedUsers.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = Spacing.space8)
            )
        }
        if (error != null) item { GroupErrorMessage(error, onClearError) }
        if (isLoading && users.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(Spacing.space32), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else if (users.isEmpty()) {
            item {
                Card(shape = MaterialTheme.shapes.largeIncreased, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(Spacing.space24), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Sizes.IconExtraLarge))
                        Spacer(Modifier.height(Spacing.space8))
                        Text(stringResource(R.string.group_no_contacts_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.group_no_contacts_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(users, key = { it.uid }) { user ->
                val selected = selectedUsers.any { it.uid == user.uid }
                val rowColor = MaterialTheme.colorScheme.surfaceContainer
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = rowColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text(userDisplayName(user), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Text(
                                userContactContext(user),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            com.synapse.social.studioasinc.feature.shared.components.UserAvatar(
                                avatarUrl = user.avatar,
                                displayName = userDisplayName(user),
                                size = Sizes.AvatarDefault
                            )
                        },
                        trailingContent = { Checkbox(checked = selected, onCheckedChange = { onToggleUser(user) }) },
                        modifier = Modifier.fillMaxWidth().clickable { onToggleUser(user) },
                        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = rowColor)
                    )
                }
            }

        }
        item { Spacer(Modifier.height(Spacing.space64)) }
    }
}

