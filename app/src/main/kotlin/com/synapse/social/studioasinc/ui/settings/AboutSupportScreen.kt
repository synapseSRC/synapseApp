package com.synapse.social.studioasinc.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSupportScreen(
    onBackClick: () -> Unit,
    onNavigateToLicenses: () -> Unit = {},
    onNavigateToWhatsNew: () -> Unit = {},
    onNavigateToFeedback: () -> Unit = {},
    viewModel: AboutSupportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val appVersion by viewModel.appVersion.collectAsState()
    val buildNumber by viewModel.buildNumber.collectAsState()
    val commitHash by viewModel.commitHash.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val shownVersion = appVersion.substringBefore('-').ifBlank { context.getString(R.string.about_version_unknown) }
    val shownBuild = buildNumber.ifBlank { context.getString(R.string.about_version_unknown) }
    // Gradle stamps the commit into BuildConfig; without it the label falls back to version + build.
    val versionLabel = if (commitHash.isBlank()) {
        stringResource(R.string.about_version_build, shownVersion, shownBuild)
    } else {
        stringResource(R.string.about_version_build_commit, shownVersion, commitHash, shownBuild)
    }

    // Keep the status truthful: the row is checked on entry and remains manually refreshable.
    LaunchedEffect(viewModel) { viewModel.checkForUpdates() }

    AboutAppTheme {
        Scaffold(
            containerColor = AppColors.screenBackground,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.about_app_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.about_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        context.getString(R.string.about_share_message, versionLabel)
                                    )
                                }
                                context.startActivity(
                                    Intent.createChooser(share, context.getString(R.string.about_share_chooser))
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = stringResource(R.string.about_share)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppColors.screenBackground,
                        scrolledContainerColor = AppColors.cardBackground,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        ) { insets ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets),
                contentPadding = PaddingValues(
                    start = Spacing.space16,
                    top = Spacing.space16,
                    end = Spacing.space16,
                    bottom = Spacing.space24
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.space16)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.space4),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { IntentUtils.openUrl(context, AboutSupportViewModel.APP_WEBSITE_URL) },
                            modifier = Modifier.height(dimensionResource(R.dimen.about_connected_button_height)),
                            shape = RoundedCornerShape(
                                topStart = 50.dp,
                                bottomStart = 50.dp,
                                topEnd = dimensionResource(R.dimen.about_list_inner_corner),
                                bottomEnd = dimensionResource(R.dimen.about_list_inner_corner)
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = Spacing.space16)
                        ) {
                            Text(stringResource(R.string.about_product_name))
                        }
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(versionLabel))
                                Toast.makeText(context, R.string.about_version_copied, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.height(dimensionResource(R.dimen.about_connected_button_height)),
                            shape = RoundedCornerShape(
                                topStart = dimensionResource(R.dimen.about_list_inner_corner),
                                bottomStart = dimensionResource(R.dimen.about_list_inner_corner),
                                topEnd = 50.dp,
                                bottomEnd = 50.dp
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = Spacing.space16)
                        ) {
                            Text(versionLabel)
                        }
                    }
                }

                item {
                    AboutListGroup(
                        items = listOf(
                            AboutLinkItem(
                                title = stringResource(R.string.about_whats_new_title),
                                supportingText = stringResource(R.string.about_whats_new_supporting),
                                leadingIcon = Icons.Filled.NewReleases,
                                trailingIcon = Icons.Filled.ChevronRight,
                                onClick = onNavigateToWhatsNew
                            ),
                            AboutLinkItem(
                                title = stringResource(R.string.about_licenses_title),
                                supportingText = stringResource(R.string.about_licenses_supporting),
                                leadingIcon = Icons.Filled.Code,
                                trailingIcon = Icons.Filled.ChevronRight,
                                onClick = onNavigateToLicenses
                            ),
                            AboutLinkItem(
                                title = stringResource(R.string.about_feedback_title),
                                supportingText = stringResource(R.string.about_feedback_supporting),
                                leadingIcon = Icons.Filled.RateReview,
                                trailingIcon = Icons.Filled.ChevronRight,
                                onClick = onNavigateToFeedback
                            ),
                            AboutLinkItem(
                                title = stringResource(R.string.about_privacy_title),
                                supportingText = stringResource(R.string.about_privacy_supporting),
                                leadingIcon = Icons.Filled.Policy,
                                trailingIcon = Icons.Filled.OpenInNew,
                                onClick = { IntentUtils.openUrl(context, viewModel.getPrivacyPolicyUrl()) }
                            ),
                            AboutLinkItem(
                                title = stringResource(R.string.about_terms_title),
                                supportingText = stringResource(R.string.about_terms_supporting),
                                leadingIcon = Icons.Filled.Description,
                                trailingIcon = Icons.Filled.OpenInNew,
                                onClick = { IntentUtils.openUrl(context, viewModel.getTermsOfServiceUrl()) }
                            )
                        )
                    )
                }

                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Spacing.space16),
                        thickness = dimensionResource(R.dimen.about_divider_thickness),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                item {
                    AboutListGroup(
                        items = listOf(
                            AboutLinkItem(
                                title = stringResource(R.string.about_update_title),
                                supportingText = updateStatus.supportingText(context),
                                leadingIcon = Icons.Filled.SystemUpdate,
                                trailingIcon = if (updateStatus is AboutUpdateStatus.UpToDate) {
                                    Icons.Filled.Done
                                } else {
                                    Icons.Filled.ChevronRight
                                },
                                onClick = {
                                    val state = updateStatus
                                    if (state is AboutUpdateStatus.UpdateAvailable) {
                                        IntentUtils.openUrl(context, state.releaseUrl)
                                    } else {
                                        viewModel.checkForUpdates()
                                    }
                                }
                            ),
                            AboutLinkItem(
                                title = stringResource(R.string.about_rate_title),
                                supportingText = stringResource(R.string.about_rate_supporting),
                                leadingIcon = Icons.Filled.Star,
                                trailingIcon = Icons.Filled.Launch,
                                onClick = {
                                    IntentUtils.openUrl(
                                        context,
                                        context.getString(R.string.about_play_store_url, context.packageName)
                                    )
                                }
                            )
                        )
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.about_footer_copyright),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.space4),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

private fun AboutUpdateStatus.supportingText(context: android.content.Context): String = when (this) {
    AboutUpdateStatus.Idle -> context.getString(R.string.about_update_latest)
    AboutUpdateStatus.Checking -> context.getString(R.string.about_update_checking)
    AboutUpdateStatus.UpToDate -> context.getString(R.string.about_update_latest)
    AboutUpdateStatus.Unavailable -> context.getString(R.string.about_update_unavailable)
    is AboutUpdateStatus.UpdateAvailable -> context.getString(R.string.about_update_available, version)
}

internal data class AboutLinkItem(
    val title: String,
    val supportingText: String,
    val leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val description: String? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
internal fun AboutListGroup(items: List<AboutLinkItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.about_list_item_gap))) {
        items.forEachIndexed { index, item ->
            AboutListItem(
                item = item,
                shape = aboutListShape(index, items.size),
                minimumHeight = dimensionResource(R.dimen.about_list_item_min_height)
            )
        }
    }
}

@Composable
internal fun AboutListItem(
    item: AboutLinkItem,
    shape: androidx.compose.ui.graphics.Shape,
    minimumHeight: Dp
) {
    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minimumHeight)
                .padding(horizontal = Spacing.space16, vertical = Spacing.space8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(dimensionResource(R.dimen.about_icon_container_size)),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(R.dimen.about_icon_size))
                    )
                }
            }
            Spacer(modifier = Modifier.width(Spacing.space16))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.space4)
                    )
                }
            }
            item.trailingIcon?.let { icon ->
                Spacer(modifier = Modifier.width(Spacing.space8))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.about_icon_size)),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (item.onClick == null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = AppColors.cardBackground,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) { rowContent() }
    } else {
        Surface(
            onClick = item.onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = AppColors.cardBackground,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) { rowContent() }
    }
}

@Composable
internal fun aboutListShape(index: Int, count: Int): androidx.compose.ui.graphics.Shape {
    val outer = dimensionResource(R.dimen.about_list_outer_corner)
    val inner = dimensionResource(R.dimen.about_list_inner_corner)
    return when {
        count <= 1 -> RoundedCornerShape(outer)
        index == 0 -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        index == count - 1 -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
        else -> RoundedCornerShape(inner)
    }
}
