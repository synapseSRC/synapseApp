package com.synapse.social.studioasinc.ui.settings

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.animation.core.spring

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AboutAppTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = MaterialTheme.colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutDetailTopAppBar(title: String, onBackClick: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.about_back)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SettingsColors.screenBackground,
            scrolledContainerColor = SettingsColors.cardBackground,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

private data class ReleaseEntry(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WhatsNewScreen(
    onBackClick: () -> Unit,
    viewModel: WhatsNewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val releases = listOf(
        ReleaseEntry(
            stringResource(R.string.about_release_100_title),
            stringResource(R.string.about_release_100_description),
            Icons.Filled.Sell
        ),
        ReleaseEntry(
            stringResource(R.string.about_release_310_title),
            stringResource(R.string.about_release_310_description),
            Icons.Filled.OfflineBolt
        ),
        ReleaseEntry(
            stringResource(R.string.about_release_300_title),
            stringResource(R.string.about_release_300_description),
            Icons.Filled.Gesture
        ),
        ReleaseEntry(
            stringResource(R.string.about_release_295_title),
            stringResource(R.string.about_release_295_description),
            Icons.Filled.Security
        )
    )
    val whatsNewState by viewModel.uiState.collectAsState()
    val githubReleasesUrl = stringResource(R.string.about_github_releases_url)

    AboutAppTheme {
        Scaffold(
            containerColor = SettingsColors.screenBackground,
            topBar = {
                AboutDetailTopAppBar(
                    title = stringResource(R.string.about_whats_new_screen_title),
                    onBackClick = onBackClick
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        IntentUtils.openUrl(context, githubReleasesUrl)
                    },
                    modifier = Modifier.height(dimensionResource(R.dimen.about_releases_fab_height)),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    icon = { Icon(Icons.Filled.Code, contentDescription = null) },
                    text = { Text(stringResource(R.string.about_github_releases)) }
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
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.about_list_item_gap))
            ) {
                items(releases.size) { index ->
                    val release = releases[index]
                    ReleaseHistoryItem(
                        entry = release,
                        shape = aboutListShape(index, releases.size),
                        expandable = index == 0,
                        expanded = whatsNewState.expandedLatest,
                        onToggle = viewModel::toggleLatestRelease
                    )
                }
                item { Spacer(modifier = Modifier.height(Spacing.space48)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReleaseHistoryItem(
    entry: ReleaseEntry,
    shape: androidx.compose.ui.graphics.Shape,
    expandable: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val expandedStateDescription = stringResource(if (expanded) R.string.about_expanded else R.string.about_collapsed)
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
                AboutLeadingIcon(entry.icon)
                Spacer(modifier = Modifier.width(Spacing.space16))
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(entry.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (expandable) {
                    Spacer(modifier = Modifier.width(Spacing.space8))
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(if (expanded) R.string.about_collapse_release else R.string.about_expand_release),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (expandable && expanded) {
                Column(
                    modifier = Modifier.padding(
                        start = Spacing.space16 + dimensionResource(R.dimen.about_icon_container_size) + Spacing.space16,
                        end = Spacing.space16,
                        bottom = Spacing.space16
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space8)
                ) {
                    Text(
                        stringResource(R.string.about_release_highlights),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ReleaseHighlight(stringResource(R.string.about_release_highlight_memory))
                    ReleaseHighlight(stringResource(R.string.about_release_highlight_offline))
                }
            }
        }
    }
    if (expandable) {
        Surface(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth().semantics {
                stateDescription = expandedStateDescription
            },
            shape = shape,
            color = SettingsColors.cardBackground,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) { contents() }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = SettingsColors.cardBackground,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) { contents() }
    }
}

@Composable
private fun ReleaseHighlight(text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.space8)) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen.about_icon_size)),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun AboutLeadingIcon(imageVector: ImageVector) {
    Surface(
        modifier = Modifier.size(dimensionResource(R.dimen.about_icon_container_size)),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.about_icon_size))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutOpenSourceScreen(
    onBackClick: () -> Unit,
    viewModel: AboutOpenSourceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var filterMenuOpen by remember { mutableStateOf(false) }
    val licenseState by viewModel.uiState.collectAsState()
    val visibleEntries = viewModel.visibleEntries()

    AboutAppTheme {
        Scaffold(
            containerColor = SettingsColors.screenBackground,
            topBar = {
                AboutDetailTopAppBar(
                    title = stringResource(R.string.about_open_source_title),
                    onBackClick = onBackClick
                )
            }
        ) { insets ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(insets),
                contentPadding = PaddingValues(
                    start = Spacing.space16,
                    top = Spacing.space16,
                    end = Spacing.space16,
                    bottom = Spacing.space24
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.space16)
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = dimensionResource(R.dimen.about_search_height))
                                .padding(start = Spacing.space16, end = Spacing.space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(Spacing.space12))
                            BasicTextField(
                                value = licenseState.query,
                                onValueChange = viewModel::onQueryChanged,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (licenseState.query.isEmpty()) {
                                            Text(
                                                stringResource(R.string.about_license_search_placeholder),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            Box {
                                IconButton(onClick = { filterMenuOpen = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.FilterList,
                                        contentDescription = stringResource(R.string.about_license_filter)
                                    )
                                }
                                DropdownMenu(
                                    expanded = filterMenuOpen,
                                    onDismissRequest = { filterMenuOpen = false },
                                    modifier = Modifier.background(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(dimensionResource(R.dimen.about_menu_corner))
                                    )
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.about_license_sort_name)) },
                                        onClick = { viewModel.setSortByAuthor(false); filterMenuOpen = false },
                                        leadingIcon = { Icon(Icons.Filled.Code, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.about_license_sort_author)) },
                                        onClick = { viewModel.setSortByAuthor(true); filterMenuOpen = false },
                                        leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null) }
                                    )
                                }
                            }
                        }
                    }
                }

                when {
                    licenseState.isLoading -> item {
                        AboutLicenseStatusMessage(stringResource(R.string.about_license_loading))
                    }

                    licenseState.loadFailed -> item {
                        AboutLicenseStatusMessage(stringResource(R.string.about_license_load_failed))
                    }

                    visibleEntries.isEmpty() -> item {
                        AboutLicenseStatusMessage(stringResource(R.string.about_license_no_results))
                    }

                    else -> item {
                        AboutListGroup(
                            items = visibleEntries.map { license ->
                                AboutLinkItem(
                                    title = license.name,
                                    supportingText = stringResource(
                                        R.string.about_license_row,
                                        licenseLabel(license.licenseId),
                                        license.author
                                    ),
                                    description = license.licenseNote,
                                    leadingIcon = Icons.Filled.Code,
                                    trailingIcon = Icons.Filled.OpenInNew,
                                    onClick = { IntentUtils.openUrl(context, license.repositoryUrl) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Status line shown while the licence list loads, fails, or matches no query. */
@Composable
private fun AboutLicenseStatusMessage(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.space24),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Maps a stable licence token from the bundled asset to its localised label.
 * An unrecognised token is displayed verbatim rather than guessed at.
 */
@Composable
private fun licenseLabel(licenseId: String): String = when (licenseId) {
    "apache-2.0" -> stringResource(R.string.about_license_label_apache_2_0)
    "mit" -> stringResource(R.string.about_license_label_mit)
    "bsd-3-clause" -> stringResource(R.string.about_license_label_bsd_3_clause)
    "agpl-3.0" -> stringResource(R.string.about_license_label_agpl_3_0)
    "gpl-2.0-with-classpath-exception" -> stringResource(R.string.about_license_label_gpl_2_0_classpath_exception)
    "google-play-services-tos" -> stringResource(R.string.about_license_label_play_services_terms)
    else -> licenseId
}

private enum class FeedbackCategory(val labelRes: Int) {
    FeatureSuggestion(R.string.about_feedback_category_feature),
    BugReport(R.string.about_feedback_category_bug),
    PerformanceIssue(R.string.about_feedback_category_performance),
    Other(R.string.about_feedback_category_other)
}

private val feedbackCategories = FeedbackCategory.entries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBackClick: () -> Unit,
    onSubmitSuccess: () -> Unit,
    viewModel: FeedbackViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var category by rememberSaveable { mutableStateOf(FeedbackCategory.FeatureSuggestion) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var includeDiagnostics by rememberSaveable { mutableStateOf(true) }
    val submissionState by viewModel.submissionState.collectAsState()

    LaunchedEffect(submissionState) {
        if (submissionState == FeedbackSubmissionState.Sent) {
            Toast.makeText(context, R.string.about_feedback_sent, Toast.LENGTH_SHORT).show()
            onSubmitSuccess()
        }
    }

    val categoryLabel = stringResource(category.labelRes)
    val diagnosticsLabel = stringResource(R.string.about_feedback_diagnostics)
    val emailError = submissionState == FeedbackSubmissionState.InvalidEmail
    val descriptionError = submissionState == FeedbackSubmissionState.InvalidDescription

    AboutAppTheme {
        Scaffold(
            containerColor = SettingsColors.screenBackground,
            topBar = {
                AboutDetailTopAppBar(
                    title = stringResource(R.string.about_feedback_screen_title),
                    onBackClick = onBackClick
                )
            }
        ) { insets ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(insets),
                contentPadding = PaddingValues(
                    start = Spacing.space16,
                    top = Spacing.space16,
                    end = Spacing.space16,
                    bottom = Spacing.space32
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.space16)
            ) {
                item {
                    Text(
                        stringResource(R.string.about_feedback_heading),
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.space8),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = categoryMenuExpanded,
                        onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = stringResource(category.labelRes),
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text(stringResource(R.string.about_feedback_category_label)) },
                            leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false },
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(dimensionResource(R.dimen.about_menu_corner))
                            )
                        ) {
                            feedbackCategories.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(option.labelRes)) },
                                    onClick = { category = option; categoryMenuExpanded = false }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; if (emailError) viewModel.clearValidationError() },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.about_feedback_email_label)) },
                        leadingIcon = { Icon(Icons.Filled.Mail, contentDescription = null) },
                        supportingText = {
                            Text(
                                stringResource(
                                    if (emailError) R.string.about_feedback_email_invalid
                                    else R.string.about_feedback_email_supporting
                                )
                            )
                        },
                        isError = emailError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it; if (descriptionError) viewModel.clearValidationError() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.about_feedback_description_label)) },
                        leadingIcon = { Icon(Icons.Filled.EditNote, contentDescription = null) },
                        supportingText = {
                            Text(
                                stringResource(
                                    if (descriptionError) R.string.about_feedback_description_required
                                    else R.string.about_feedback_description_supporting
                                )
                            )
                        },
                        isError = descriptionError,
                        minLines = 4,
                        maxLines = 8,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = dimensionResource(R.dimen.about_touch_target_min_height)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.space16)
                    ) {
                        Text(
                            diagnosticsLabel,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = includeDiagnostics,
                            onCheckedChange = { includeDiagnostics = it },
                            modifier = Modifier.semantics { contentDescription = diagnosticsLabel }
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                        Button(
                            onClick = {
                                viewModel.submitFeedback(
                                    category = categoryLabel,
                                    email = email,
                                    description = description,
                                    includeDiagnostics = includeDiagnostics
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = dimensionResource(R.dimen.about_submit_button_height)),
                            enabled = submissionState != FeedbackSubmissionState.Sending,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(Spacing.space8))
                            Text(
                                stringResource(
                                    if (submissionState == FeedbackSubmissionState.Sending) R.string.about_feedback_sending
                                    else R.string.about_feedback_submit
                                )
                            )
                        }
                        if (submissionState == FeedbackSubmissionState.Failed) {
                            Text(
                                stringResource(R.string.about_feedback_submit_failed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutEmptyScreen(onBackClick: () -> Unit) {
    AboutAppTheme {
        Scaffold(
            containerColor = SettingsColors.screenBackground,
            topBar = {
                AboutDetailTopAppBar(
                    title = stringResource(R.string.about_screen_five_title),
                    onBackClick = onBackClick
                )
            }
        ) { insets ->
            Spacer(Modifier.fillMaxSize().padding(insets))
        }
    }
}
