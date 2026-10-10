package com.synapse.social.studioasinc.feature.profile.editprofile.featured

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.ui.settings.SettingsHeaderItem
import com.synapse.social.studioasinc.ui.settings.SettingsItemPosition
import com.synapse.social.studioasinc.ui.settings.SettingsSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturedContentScreen(
    viewModel: FeaturedContentViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                FeaturedContentNavigation.NavigateBack -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(FeaturedContentEvent.DismissError)
        }
    }

    SynapseTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MediumTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.featured_content_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.onEvent(FeaturedContentEvent.BackClicked) },
                            modifier = Modifier.semantics {
                                contentDescription = "Navigate back"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    actions = {
                        if (uiState.hasPendingChanges) {
                            TextButton(
                                onClick = { viewModel.onEvent(FeaturedContentEvent.ApplyChanges) },
                                enabled = !uiState.isSaving
                            ) {
                                Text(
                                    text = stringResource(R.string.action_apply_changes),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    ExpressiveLoadingIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        horizontal = SettingsSpacing.screenPadding,
                        vertical = SettingsSpacing.sectionSpacing
                    ),
                    verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
                ) {
                    // 1. Backend Boundary & Capability Notice
                    if (uiState.showBackendCapabilityNotice) {
                        item {
                            BackendCapabilityNoticeCard(
                                onDismiss = { viewModel.onEvent(FeaturedContentEvent.DismissCapabilityNotice) }
                            )
                        }
                    }

                    // 2. Featured Content Live Preview Panel
                    item {
                        FeaturedContentPreviewCard(
                            uiState = uiState,
                            onClearAll = { viewModel.onEvent(FeaturedContentEvent.ClearAllSelected) }
                        )
                    }

                    // 3. Tab Filter Chips
                    item {
                        FeaturedTabFilterRow(
                            selectedTab = uiState.selectedTab,
                            postsCount = uiState.candidatePosts.size,
                            mediaCount = uiState.candidateMedia.size,
                            onTabSelected = { tab -> viewModel.onEvent(FeaturedContentEvent.TabSelected(tab)) }
                        )
                    }

                    // 4. Content Selection List / Grid based on Tab
                    when (uiState.selectedTab) {
                        FeaturedTab.POSTS -> {
                            if (uiState.candidatePosts.isEmpty()) {
                                item {
                                    EmptyCandidateContentCard(
                                        title = stringResource(R.string.featured_empty_posts_title),
                                        message = stringResource(R.string.featured_empty_posts_message)
                                    )
                                }
                            } else {
                                itemsIndexed(uiState.candidatePosts) { index, post ->
                                    val isSelected = uiState.selectedPostIds.contains(post.id)
                                    val orderIndex = if (isSelected) uiState.selectedPostIds.indexOf(post.id) + 1 else null
                                    val position = when {
                                        uiState.candidatePosts.size == 1 -> SettingsItemPosition.Single
                                        index == 0 -> SettingsItemPosition.Top
                                        index == uiState.candidatePosts.lastIndex -> SettingsItemPosition.Bottom
                                        else -> SettingsItemPosition.Middle
                                    }

                                    CandidatePostItemRow(
                                        post = post,
                                        isSelected = isSelected,
                                        orderIndex = orderIndex,
                                        position = position,
                                        onToggleSelected = { viewModel.onEvent(FeaturedContentEvent.TogglePostSelected(post.id)) },
                                        onMoveUp = if (isSelected && (orderIndex ?: 0) > 1) {
                                            { viewModel.onEvent(FeaturedContentEvent.MovePostOrderUp(post.id)) }
                                        } else null,
                                        onMoveDown = if (isSelected && (orderIndex ?: 0) < uiState.selectedPostIds.size) {
                                            { viewModel.onEvent(FeaturedContentEvent.MovePostOrderDown(post.id)) }
                                        } else null
                                    )
                                    if (index < uiState.candidatePosts.lastIndex) {
                                        Spacer(modifier = Modifier.height(SettingsSpacing.itemSpacing))
                                    }
                                }
                            }
                        }

                        FeaturedTab.MEDIA -> {
                            if (uiState.candidateMedia.isEmpty()) {
                                item {
                                    EmptyCandidateContentCard(
                                        title = stringResource(R.string.featured_empty_media_title),
                                        message = stringResource(R.string.featured_empty_media_message)
                                    )
                                }
                            } else {
                                item {
                                    CandidateMediaGrid(
                                        candidateMedia = uiState.candidateMedia,
                                        selectedMediaUrls = uiState.selectedMediaUrls,
                                        onToggleMedia = { url -> viewModel.onEvent(FeaturedContentEvent.ToggleMediaSelected(url)) }
                                    )
                                }
                            }
                        }

                        FeaturedTab.COLLECTIONS -> {
                            item {
                                CapabilityBoundariesInfoCard()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackendCapabilityNoticeCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.featured_backend_notice_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.featured_backend_notice_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Dismiss persistence notice"
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FeaturedContentPreviewCard(
    uiState: FeaturedContentUiState,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.featured_preview_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (uiState.hasSelectedContent) {
                    TextButton(
                        onClick = onClearAll,
                        modifier = Modifier.semantics {
                            contentDescription = "Clear all selected featured content"
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.action_clear_all),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!uiState.hasSelectedContent) {
                Text(
                    text = stringResource(R.string.featured_preview_empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.selectedPosts.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.featured_selected_posts_header, uiState.selectedPosts.size),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        uiState.selectedPosts.forEachIndexed { idx, post ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = post.postText?.takeIf { it.isNotBlank() } ?: stringResource(R.string.post_media_attachment_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (uiState.selectedMedia.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.featured_selected_media_header, uiState.selectedMedia.size),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uiState.selectedMedia.take(4).forEachIndexed { idx, mediaItem ->
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    AsyncImage(
                                        model = mediaItem.url,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedTabFilterRow(
    selectedTab: FeaturedTab,
    postsCount: Int,
    mediaCount: Int,
    onTabSelected: (FeaturedTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedTab == FeaturedTab.POSTS,
            onClick = { onTabSelected(FeaturedTab.POSTS) },
            label = { Text(text = stringResource(R.string.featured_tab_posts, postsCount)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.PostAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        FilterChip(
            selected = selectedTab == FeaturedTab.MEDIA,
            onClick = { onTabSelected(FeaturedTab.MEDIA) },
            label = { Text(text = stringResource(R.string.featured_tab_media, mediaCount)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.PermMedia,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        FilterChip(
            selected = selectedTab == FeaturedTab.COLLECTIONS,
            onClick = { onTabSelected(FeaturedTab.COLLECTIONS) },
            label = { Text(text = stringResource(R.string.featured_tab_collections)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Collections,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}

@Composable
private fun CandidatePostItemRow(
    post: Post,
    isSelected: Boolean,
    orderIndex: Int?,
    position: SettingsItemPosition,
    onToggleSelected: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(position.getShape())
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = position.getShape()
            )
            .clickable(onClick = onToggleSelected)
            .semantics(mergeDescendants = true) {
                contentDescription = if (isSelected) {
                    "Featured post #${orderIndex}: ${post.postText ?: "Media post"}. Double tap to deselect."
                } else {
                    "Candidate post: ${post.postText ?: "Media post"}. Double tap to feature."
                }
            },
        shape = position.getShape(),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Badge Checkbox
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected && orderIndex != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$orderIndex",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Post Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.postText?.takeIf { it.isNotBlank() } ?: stringResource(R.string.post_media_attachment_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!post.postImage.isNullOrBlank() || !post.mediaItems.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.post_contains_media_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Reorder controls if selected
            if (isSelected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onMoveUp != null) {
                        IconButton(
                            onClick = onMoveUp,
                            modifier = Modifier
                                .size(36.dp)
                                .semantics { contentDescription = "Move featured post up" }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (onMoveDown != null) {
                        IconButton(
                            onClick = onMoveDown,
                            modifier = Modifier
                                .size(36.dp)
                                .semantics { contentDescription = "Move featured post down" }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateMediaGrid(
    candidateMedia: List<FeaturedMediaItem>,
    selectedMediaUrls: List<String>,
    onToggleMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        candidateMedia.chunked(3).forEach { rowMedia ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowMedia.forEach { item ->
                    val isSelected = selectedMediaUrls.contains(item.url)
                    val orderIndex = if (isSelected) selectedMediaUrls.indexOf(item.url) + 1 else null

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onToggleMedia(item.url) }
                            .semantics(mergeDescendants = true) {
                                contentDescription = if (isSelected) {
                                    "Featured media #${orderIndex}. Double tap to deselect."
                                } else {
                                    "Candidate media item. Double tap to feature."
                                }
                            }
                    ) {
                        AsyncImage(
                            model = item.url,
                            contentDescription = item.caption ?: "Candidate media item",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (item.isVideo) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Video media",
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                                    .padding(4.dp)
                            )
                        }

                        // Order Badge Overlay
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ) {
                            if (isSelected && orderIndex != null) {
                                Text(
                                    text = "$orderIndex",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                }
                // Fill empty slots in partial rows
                repeat(3 - rowMedia.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EmptyCandidateContentCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CapabilityBoundariesInfoCard(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Collections,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.featured_collections_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.featured_collections_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
