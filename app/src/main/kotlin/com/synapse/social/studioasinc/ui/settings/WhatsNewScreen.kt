package com.synapse.social.studioasinc.ui.settings

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.domain.model.release.AppRelease
import com.synapse.social.studioasinc.domain.model.release.ReleaseChangeGroup
import com.synapse.social.studioasinc.domain.model.release.ReleaseChangeHeading
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.ui.components.EmptyState
import com.synapse.social.studioasinc.ui.components.ErrorState
import com.synapse.social.studioasinc.ui.components.shimmer
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography

/** Inline markup the release-notes parser is allowed to hand over: bold, code and safe links. */
private val inlineMarkup = Regex("\\*\\*(.+?)\\*\\*|`([^`]*)`|\\[([^]]+)]\\(([^)]*)\\)")

/** Share of the loading card each placeholder line takes, so the block hints at the real layout. */
private const val SkeletonWidthBadge = 0.25f
private const val SkeletonWidthTitle = 0.5f
private const val SkeletonWidthBody = 0.85f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WhatsNewScreen(
    onBackClick: () -> Unit,
    viewModel: WhatsNewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val githubReleasesUrl = stringResource(R.string.about_github_releases_url)
    val pullToRefreshState = rememberPullToRefreshState()

    AboutAppTheme {
        Scaffold(
            containerColor = AppColors.screenBackground,
            topBar = {
                AboutDetailTopAppBar(
                    title = stringResource(R.string.about_whats_new_screen_title),
                    onBackClick = onBackClick
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { IntentUtils.openUrl(context, githubReleasesUrl) },
                    modifier = Modifier.height(dimensionResource(R.dimen.about_releases_fab_height)),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    icon = { Icon(Icons.Filled.Code, contentDescription = null) },
                    text = { Text(stringResource(R.string.about_github_releases)) }
                )
            }
        ) { insets ->
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                state = pullToRefreshState,
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        state = pullToRefreshState,
                        isRefreshing = state.isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets)
            ) {
                val failure = state.errorMessage?.takeIf { state.releases.isEmpty() }
                when {
                    state.isLoading -> WhatsNewSkeleton()
                    failure != null -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorState(
                            title = stringResource(R.string.whats_new_error_title),
                            message = failure.asString(),
                            onRetry = viewModel::retry
                        )
                    }
                    state.releases.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Filled.NewReleases,
                            title = stringResource(R.string.whats_new_empty_title),
                            message = stringResource(R.string.whats_new_empty_message)
                        )
                    }
                    else -> ReleaseFeedList(
                        state = state,
                        onToggle = viewModel::toggleRelease,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReleaseFeedList(
    state: WhatsNewUiState,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val latest = state.latest ?: return
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = Spacing.space16,
            top = Spacing.space16,
            end = Spacing.space16,
            bottom = Spacing.space24
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.space16)
    ) {
        item(key = "highlight-${latest.tag}") {
            ReleaseHighlightCard(
                release = latest,
                isNew = state.unreadTag == latest.tag,
                onOpenRelease = { IntentUtils.openUrl(context, latest.url) }
            )
        }
        if (state.isStale) {
            item(key = "stale-notice") { StaleFeedNotice() }
        }
        val history = state.history
        if (history.isNotEmpty()) {
            item(key = "history-header") {
                Text(
                    text = stringResource(R.string.whats_new_release_history),
                    style = AppTypography.sectionHeader,
                    color = AppColors.sectionTitle,
                    modifier = Modifier.padding(top = Spacing.space4)
                )
            }
            itemsIndexed(history, key = { _, release -> release.tag }) { index, release ->
                ReleaseHistoryRow(
                    release = release,
                    shape = aboutListShape(index, history.size),
                    expanded = release.tag in state.expandedTags,
                    onToggle = { onToggle(release.tag) },
                    onOpenRelease = { IntentUtils.openUrl(context, release.url) }
                )
            }
        }
        item(key = "bottom-spacer") { Spacer(modifier = Modifier.height(Spacing.space48)) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReleaseHighlightCard(
    release: AppRelease,
    isNew: Boolean,
    onOpenRelease: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = AppColors.cardBackgroundElevated,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box {
            Icon(
                imageVector = Icons.Filled.Celebration,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.space16)
                    .size(dimensionResource(R.dimen.about_release_accent_size)),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            )
            Column(
                modifier = Modifier.padding(Spacing.space16),
                verticalArrangement = Arrangement.spacedBy(Spacing.space12)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                    ReleaseBadge(
                        text = release.tag,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (isNew) {
                        ReleaseBadge(
                            text = stringResource(R.string.whats_new_badge_new),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    } else {
                        ReleaseBadge(
                            text = stringResource(R.string.whats_new_badge_latest),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                    Text(
                        text = release.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    release.publishedAtMillis?.let { publishedAt ->
                        Text(
                            text = stringResource(
                                R.string.whats_new_published_on,
                                formatReleaseDate(publishedAt)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.supportingText
                        )
                    }
                }
                ReleaseNotes(groups = release.groups)
                TextButton(
                    onClick = onOpenRelease,
                    modifier = Modifier.heightIn(min = dimensionResource(R.dimen.about_touch_target_min_height)),
                    contentPadding = PaddingValues(horizontal = Spacing.space4, vertical = Spacing.space4)
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(R.dimen.about_release_action_icon_size))
                    )
                    Spacer(modifier = Modifier.width(Spacing.space8))
                    Text(stringResource(R.string.whats_new_view_on_github))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReleaseHistoryRow(
    release: AppRelease,
    shape: Shape,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenRelease: () -> Unit
) {
    val hasNotes = release.groups.isNotEmpty()
    val expandedStateDescription =
        stringResource(if (expanded) R.string.about_expanded else R.string.about_collapsed)
    val contents: @Composable () -> Unit = {
        Column(
            modifier = Modifier.animateContentSize(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = dimensionResource(R.dimen.about_release_item_min_height))
                    .padding(horizontal = Spacing.space16, vertical = Spacing.space8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AboutLeadingIcon(Icons.Filled.NewReleases)
                Spacer(modifier = Modifier.width(Spacing.space16))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space4)
                ) {
                    Text(
                        text = release.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    release.publishedAtMillis?.let { publishedAt ->
                        Text(
                            text = formatReleaseDate(publishedAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.supportingText
                        )
                    }
                }
                if (hasNotes) {
                    Spacer(modifier = Modifier.width(Spacing.space8))
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(
                            if (expanded) R.string.about_collapse_release else R.string.about_expand_release
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // A release without notes has nothing to expand; the row opens it on GitHub.
                    Spacer(modifier = Modifier.width(Spacing.space8))
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.whats_new_view_on_github),
                        modifier = Modifier.size(dimensionResource(R.dimen.about_release_action_icon_size)),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (expanded && hasNotes) {
                Column(
                    modifier = Modifier.padding(
                        start = Spacing.space16 + dimensionResource(R.dimen.about_icon_container_size) + Spacing.space16,
                        end = Spacing.space16,
                        bottom = Spacing.space16
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space12)
                ) {
                    ReleaseNotes(groups = release.groups)
                    TextButton(
                        onClick = onOpenRelease,
                        modifier = Modifier.heightIn(min = dimensionResource(R.dimen.about_touch_target_min_height)),
                        contentPadding = PaddingValues(horizontal = Spacing.space4, vertical = Spacing.space4)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(dimensionResource(R.dimen.about_release_action_icon_size))
                        )
                        Spacer(modifier = Modifier.width(Spacing.space8))
                        Text(stringResource(R.string.whats_new_view_on_github))
                    }
                }
            }
        }
    }
    if (hasNotes) {
        Surface(
            onClick = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = expandedStateDescription },
            shape = shape,
            color = AppColors.cardBackground,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) { contents() }
    } else {
        Surface(
            onClick = onOpenRelease,
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = AppColors.cardBackground,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) { contents() }
    }
}

@Composable
private fun ReleaseNotes(groups: List<ReleaseChangeGroup>) {
    if (groups.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space12)) {
        groups.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (group.heading) {
                            ReleaseChangeHeading.NewFeature -> Icons.Filled.AutoAwesome
                            ReleaseChangeHeading.Improvement -> Icons.Filled.Tune
                            ReleaseChangeHeading.BugFix -> Icons.Filled.BugReport
                            ReleaseChangeHeading.Other -> Icons.Filled.Check
                        },
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(R.dimen.about_release_heading_icon_size)),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = group.title.ifBlank { stringResource(group.heading.labelRes()) },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                group.items.forEach { item ->
                    ReleaseChangeLine(text = item)
                }
            }
        }
    }
}

@Composable
private fun ReleaseChangeLine(text: String) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeColor = MaterialTheme.colorScheme.onSurfaceVariant
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
    ) {
        Box(
            modifier = Modifier
                .padding(top = Spacing.space8)
                .size(dimensionResource(R.dimen.about_release_bullet_size))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Text(
            text = renderReleaseItem(
                text = text,
                linkColor = linkColor,
                codeColor = codeColor,
                codeBackground = codeBackground
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Renders the safe inline subset of the notes; anything unrecognised stays plain text. */
@Composable
private fun renderReleaseItem(
    text: String,
    linkColor: Color,
    codeColor: Color,
    codeBackground: Color
): AnnotatedString {
    val boldStyle = SpanStyle(fontWeight = FontWeight.SemiBold)
    val codeStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        color = codeColor,
        background = codeBackground
    )
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = linkColor,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline
        )
    )
    val context = LocalContext.current
    return remember(text, linkColor, codeColor, codeBackground) {
        buildAnnotatedString {
            var cursor = 0
            inlineMarkup.findAll(text).forEach { match ->
                append(text.substring(cursor, match.range.first))
                when {
                    match.groups[1] != null -> withStyle(boldStyle) { append(match.groupValues[1]) }
                    match.groups[2] != null -> withStyle(codeStyle) { append(match.groupValues[2]) }
                    else -> {
                        val url = match.groupValues[4]
                        pushLink(
                            LinkAnnotation.Clickable(
                                tag = url,
                                styles = linkStyles,
                                linkInteractionListener = { IntentUtils.openUrl(context, url) }
                            )
                        )
                        append(match.groupValues[3])
                        pop()
                    }
                }
                cursor = match.range.last + 1
            }
            append(text.substring(cursor))
        }
    }
}

@Composable
private fun ReleaseBadge(text: String, containerColor: Color, contentColor: Color) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Spacing.space12, vertical = Spacing.space4)
        )
    }
}

@Composable
private fun StaleFeedNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(AppColors.cardBackground)
            .padding(Spacing.space12),
        horizontalArrangement = Arrangement.spacedBy(Spacing.space8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen.about_release_heading_icon_size)),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.whats_new_offline_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.supportingText
        )
    }
}

@Composable
private fun WhatsNewSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.space16),
        verticalArrangement = Arrangement.spacedBy(Spacing.space16)
    ) {
        SkeletonCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.about_skeleton_hero_height)),
            shape = MaterialTheme.shapes.extraLarge,
            color = AppColors.cardBackgroundElevated
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space12)) {
                SkeletonLine(
                    modifier = Modifier
                        .fillMaxWidth(SkeletonWidthBadge)
                        .height(Sizes.ShimmerTextSmall)
                )
                SkeletonLine(
                    modifier = Modifier
                        .fillMaxWidth(SkeletonWidthTitle)
                        .height(Sizes.ShimmerTextMedium)
                )
                SkeletonLine(
                    modifier = Modifier
                        .fillMaxWidth(SkeletonWidthBody)
                        .height(Sizes.ShimmerTextSmall)
                )
                SkeletonLine(
                    modifier = Modifier
                        .fillMaxWidth(SkeletonWidthBody)
                        .height(Sizes.ShimmerTextSmall)
                )
            }
        }
        repeat(3) { index ->
            SkeletonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.about_release_item_min_height)),
                shape = aboutListShape(index, 3),
                color = AppColors.cardBackground
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space16)
                ) {
                    SkeletonLine(
                        modifier = Modifier.size(dimensionResource(R.dimen.about_icon_container_size)),
                        shape = CircleShape
                    )
                    SkeletonLine(
                        modifier = Modifier
                            .width(Sizes.ShimmerWidthExtraLarge)
                            .height(Sizes.ShimmerTextSmall)
                    )
                }
            }
        }
    }
}

/**
 * Placeholder of a real card: the screen's own surface colours carry the layout, while the shared
 * sweep runs underneath so the block reads as loading rather than as an empty panel.
 */
@Composable
private fun SkeletonCard(
    modifier: Modifier,
    shape: Shape,
    color: Color,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.clip(shape).background(color).shimmer()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.space16),
            contentAlignment = Alignment.CenterStart
        ) { content() }
    }
}

@Composable
private fun SkeletonLine(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun formatReleaseDate(epochMillis: Long): String {
    val formatter = remember { DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()) }
    return remember(epochMillis) { formatter.format(Date(epochMillis)) }
}

/**
 * Localised label of a recognised heading.
 *
 * The parser leaves [ReleaseChangeGroup.title] blank for the headings it maps onto a role, so the
 * screen names them in the user's language; anything it could not map keeps the author's own wording.
 */
@StringRes
private fun ReleaseChangeHeading.labelRes(): Int = when (this) {
    ReleaseChangeHeading.NewFeature -> R.string.whats_new_group_new_features
    ReleaseChangeHeading.Improvement -> R.string.whats_new_group_improvements
    ReleaseChangeHeading.BugFix -> R.string.whats_new_group_bug_fixes
    ReleaseChangeHeading.Other -> R.string.whats_new_group_changes
}
