@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.components.ActiveStatusIndicator
import com.synapse.social.studioasinc.feature.shared.components.UserAvatar
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.UserStatus
import com.synapse.social.studioasinc.ui.components.ShimmerBox
import com.synapse.social.studioasinc.ui.components.ShimmerCircle

/**
 * The participant picker that opens the group-creation flow.
 *
 * Search and the running selection stay pinned above the list so both remain reachable while
 * scrolling, and the list carries rows only: a row states its selection through tonal fill and a
 * check badge rather than a card per contact.
 */
@Composable
internal fun MembersStage(
    users: List<User>,
    selectedUsers: List<User>,
    searchQuery: String,
    isLoading: Boolean,
    error: GroupCreationError?,
    onSearch: (String) -> Unit,
    onToggleUser: (User) -> Unit,
    onRemoveUser: (User) -> Unit,
    onClearSelection: () -> Unit,
    onClearError: () -> Unit
) {
    val query = searchQuery.trim()
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = Spacing.space16)) {
            Spacer(Modifier.height(Spacing.space8))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.group_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) IconButton(onClick = { onSearch("") }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_search))
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Search
                )
            )
            AnimatedVisibility(
                visible = selectedUsers.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.group_selected_count, selectedUsers.size),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f).padding(start = Spacing.space4, top = Spacing.space8)
                        )
                        TextButton(onClick = onClearSelection) {
                            Text(stringResource(R.string.clear_all))
                        }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.space8),
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.space8)
                    ) {
                        items(selectedUsers, key = { it.uid }) { user ->
                            SelectedMemberChip(user = user, onRemove = { onRemoveUser(user) })
                        }
                    }
                }
            }
            if (error != null) GroupErrorMessage(error, onClearError)
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading && users.isEmpty() -> MemberListSkeleton()
                users.isEmpty() -> MemberListEmptyState(query = query)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.space12,
                        end = Spacing.space16,
                        top = Spacing.space4,
                        bottom = Sizes.HeightLarge + Spacing.space32
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space4)
                ) {
                    items(users, key = { it.uid }) { user ->
                        MemberRow(
                            user = user,
                            selected = selectedUsers.any { it.uid == user.uid },
                            query = query,
                            onToggle = onToggleUser
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedMemberChip(user: User, onRemove: () -> Unit) {
    val name = userDisplayName(user)
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.remove_member), onClick = onRemove)
            .padding(horizontal = Spacing.space12, vertical = Spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
    ) {
        UserAvatar(avatarUrl = user.avatar, displayName = name, size = Sizes.AvatarTiny)
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = Sizes.WidthLarge)
        )
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(Sizes.IconSemiMedium)
        )
    }
}

@Composable
private fun MemberRow(
    user: User,
    selected: Boolean,
    query: String,
    onToggle: (User) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val name = userDisplayName(user)
    val handle = user.username?.takeIf { it.isNotBlank() }?.let { "@$it" }
    val context = handle ?: user.bio?.takeIf { it.isNotBlank() }
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "memberRowContainer"
    )
    val secondaryColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "memberRowSecondary"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle(user)
                }
            )
            .padding(horizontal = Spacing.space8, vertical = Spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space12)
    ) {
        Box {
            UserAvatar(avatarUrl = user.avatar, displayName = name, size = Sizes.AvatarDefault)
            ActiveStatusIndicator(
                isActive = user.status == UserStatus.ONLINE,
                size = Sizes.StatusDot,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlightQuery(name, query),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (context != null) {
                Text(
                    text = highlightQuery(context, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        SelectionBadge(selected = selected)
    }
}

@Composable
private fun SelectionBadge(selected: Boolean) {
    val fill by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "badgeFill"
    )
    val stroke by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "badgeStroke"
    )
    val checkProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "badgeCheck"
    )

    Box(
        modifier = Modifier
            .size(Sizes.IconExtraLarge)
            .clip(CircleShape)
            .background(fill)
            .border(Sizes.BorderThinAlt, stroke, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(Sizes.IconSemiMedium)
                .graphicsLayer {
                    alpha = checkProgress
                    scaleX = checkProgress
                    scaleY = checkProgress
                }
        )
    }
}

@Composable
private fun MemberListSkeleton(rows: Int = 6) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.space16, vertical = Spacing.space16),
        verticalArrangement = Arrangement.spacedBy(Spacing.space20)
    ) {
        repeat(rows) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.space12)
            ) {
                ShimmerCircle(size = Sizes.AvatarDefault)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space8)
                ) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.45f).height(Sizes.ShimmerLineHeightLarge))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.28f).height(Sizes.ShimmerLineHeight))
                }
            }
        }
    }
}

@Composable
private fun MemberListEmptyState(query: String) {
    val isSearching = query.isNotEmpty()
    Column(
        modifier = Modifier.fillMaxWidth().padding(Spacing.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSearching) Icons.Filled.PersonSearch else Icons.Filled.Group,
            contentDescription = null,
            tint = if (isSearching) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(Sizes.IconHuge)
        )
        Spacer(Modifier.height(Spacing.space16))
        Text(
            text = stringResource(
                if (isSearching) R.string.group_no_results_title else R.string.group_no_contacts_title
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.space4))
        Text(
            text = if (isSearching) {
                stringResource(R.string.group_no_results_body, query)
            } else {
                stringResource(R.string.group_no_contacts_body)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Bolds the substrings the current query matched, so a result row shows why it appeared. */
@Composable
private fun highlightQuery(text: String, query: String): AnnotatedString {
    val accent = MaterialTheme.colorScheme.primary
    val needle = query.lowercase().trim()
    if (needle.isBlank()) return AnnotatedString(text)
    val ranges = remember(text, needle) {
        val haystack = text.lowercase()
        buildList {
            var start = haystack.indexOf(needle)
            while (start >= 0) {
                add(start to start + needle.length)
                start = haystack.indexOf(needle, start + needle.length)
            }
        }
    }
    if (ranges.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var cursor = 0
        ranges.forEach { (from, to) ->
            append(text.substring(cursor, from))
            pushStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold))
            append(text.substring(from, to))
            pop()
            cursor = to
        }
        append(text.substring(cursor))
    }
}
