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
import androidx.compose.material.icons.filled.Edit
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
internal fun IdentityStage(
    selectedCount: Int,
    preset: String,
    hasCreatedGroup: Boolean,
    purpose: String,
    groupName: String,
    description: String,
    iconConcept: String,
    permissions: com.synapse.social.studioasinc.shared.domain.model.chat.GroupCreationOptions,
    isGenerating: Boolean,
    isCreating: Boolean,
    error: GroupCreationError?,
    onPurposeChange: (String) -> Unit,
    onGroupNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onRegenerate: () -> Unit,
    onOpenPermissions: () -> Unit,
    onCreate: () -> Unit,
    onReturnToHub: () -> Unit,
    onClearError: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space12),
            verticalArrangement = Arrangement.spacedBy(Spacing.space12)
        ) {
            item {
                Card(
                    shape = MaterialTheme.shapes.largeIncreased,
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(Spacing.space20)) {
                        Text(stringResource(R.string.group_ai_assistant_title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.height(Spacing.space4))
                        Text(stringResource(R.string.group_ai_assistant_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space8), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf(
                        stringResource(R.string.group_purpose_study),
                        stringResource(R.string.group_purpose_project),
                        stringResource(R.string.group_purpose_social)
                    ).forEach { value ->
                        FilterChip(selected = purpose == value, enabled = !hasCreatedGroup, onClick = { onPurposeChange(value) }, label = { Text(value) })
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = onGroupNameChange,
                    label = { Text(stringResource(R.string.group_name_label)) },
                    leadingIcon = { Icon(Icons.Filled.Group, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = hasCreatedGroup,
                    supportingText = { Text(stringResource(R.string.group_name_supporting)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.group_description_label)) },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    readOnly = hasCreatedGroup,
                    supportingText = { Text(stringResource(R.string.group_description_supporting)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
            if (iconConcept.isNotBlank()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(Spacing.space16), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Book, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(Spacing.space12))
                            Column {
                                Text(stringResource(R.string.group_ai_icon_concept), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                Text(iconConcept, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }
            }
            item {
                Button(onClick = onRegenerate, enabled = !isGenerating && !hasCreatedGroup, modifier = Modifier.fillMaxWidth().height(Sizes.ButtonHeightM3Medium)) {
                    if (isGenerating) CircularProgressIndicator(modifier = Modifier.size(Sizes.IconDefault), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(Spacing.space8))
                    Text(stringResource(R.string.group_regenerate_ai))
                }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.group_permissions_title)) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.group_permission_summary,
                                when (preset) {
                                    "announcement" -> stringResource(R.string.group_preset_announcement)
                                    "private" -> stringResource(R.string.group_preset_private)
                                    else -> stringResource(R.string.group_preset_collaborative)
                                },
                                disappearingLabel(permissions.disappearingMode)
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Filled.ArrowForward, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !hasCreatedGroup, onClick = onOpenPermissions)
                )
            }
            if (error != null) item { GroupErrorMessage(error, onClearError) }
            item { Spacer(Modifier.height(Spacing.space8)) }
        }
        Button(
            onClick = if (hasCreatedGroup) onReturnToHub else onCreate,
            enabled = hasCreatedGroup || (groupName.isNotBlank() && selectedCount > 0 && !isCreating),
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space16, vertical = Spacing.space12).height(Sizes.ButtonHeightM3Medium)
        ) {
            if (isCreating) CircularProgressIndicator(modifier = Modifier.size(Sizes.IconDefault), strokeWidth = 2.dp)
            else Icon(if (hasCreatedGroup) Icons.Filled.ArrowForward else Icons.Filled.Check, contentDescription = null)
            Spacer(Modifier.width(Spacing.space8))
            Text(if (hasCreatedGroup) stringResource(R.string.group_return_to_hub) else stringResource(R.string.group_create_count, selectedCount))
        }
    }
}

@Composable
internal fun PermissionsStage(
    options: com.synapse.social.studioasinc.shared.domain.model.chat.GroupCreationOptions,
    preset: String,
    onSelectPreset: (String) -> Unit,
    onSendMessagesChange: (Boolean) -> Unit,
    onMemberSettingsChange: (Boolean) -> Unit,
    onApprovalChange: (Boolean) -> Unit,
    onInvitesChange: (Boolean) -> Unit,
    onDisappearingChange: (DisappearingMode) -> Unit,
    error: GroupCreationError?,
    onSave: () -> Unit,
    onClearError: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.space16, vertical = Spacing.space12),
            verticalArrangement = Arrangement.spacedBy(Spacing.space12)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
                ) {
                    listOf(
                        "collaborative" to stringResource(R.string.group_preset_collaborative),
                        "announcement" to stringResource(R.string.group_preset_announcement),
                        "private" to stringResource(R.string.group_preset_private)
                    ).forEach { (id, label) ->
                        FilterChip(
                            selected = preset == id,
                            onClick = { onSelectPreset(id) },
                            modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                            label = {
                                Text(
                                    label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
            item {
                PermissionRow(Icons.Filled.Chat, stringResource(R.string.group_permission_send_messages), stringResource(R.string.group_permission_send_support), options.sendMessagesByMembers, onSendMessagesChange)
            }
            item {
                PermissionRow(Icons.Filled.Edit, stringResource(R.string.group_permission_edit_settings), stringResource(R.string.group_permission_edit_support), options.allowMemberSettingsEdits, onMemberSettingsChange)
            }
            item {
                PermissionRow(Icons.Filled.HowToReg, stringResource(R.string.group_permission_approve), stringResource(R.string.group_permission_approve_support), options.requireJoinApproval, onApprovalChange)
            }
            item {
                PermissionRow(Icons.Filled.Share, stringResource(R.string.group_permission_invites), stringResource(R.string.group_permission_invites_support), options.allowMemberInvites, onInvitesChange)
            }
            item {
                Box {
                    OutlinedTextField(
                        value = disappearingLabel(options.disappearingMode),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.group_disappearing_label)) },
                        leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Filled.ArrowForward, contentDescription = null) },
                        supportingText = { Text(stringResource(R.string.group_disappearing_support)) },
                        modifier = Modifier.fillMaxWidth().clickable { menuExpanded = true }
                    )
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        listOf(
                            DisappearingMode.OFF to stringResource(R.string.disappearing_mode_off),
                            DisappearingMode.TWENTY_FOUR_HOURS to stringResource(R.string.disappearing_mode_24_hours),
                            DisappearingMode.SEVEN_DAYS to stringResource(R.string.disappearing_mode_7_days),
                            DisappearingMode.NINETY_DAYS to stringResource(R.string.disappearing_mode_90_days)
                        ).forEach { (mode, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { onDisappearingChange(mode); menuExpanded = false })
                        }
                    }
                }
            }
            if (error != null) item { GroupErrorMessage(error, onClearError) }
            item { Spacer(Modifier.height(Spacing.space8)) }
        }
        Button(onClick = onSave, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space16, vertical = Spacing.space12).height(Sizes.ButtonHeightM3Medium)) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Spacer(Modifier.width(Spacing.space8))
            Text(stringResource(R.string.group_save_permissions))
        }
    }
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val rowColor = MaterialTheme.colorScheme.surfaceContainer
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = rowColor)
    ) {
        ListItem(
            headlineContent = { Text(title, style = MaterialTheme.typography.titleSmall) },
            supportingContent = {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            leadingContent = {
                androidx.compose.material3.Surface(
                    modifier = Modifier.size(40.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = rowColor)
        )
    }
}
