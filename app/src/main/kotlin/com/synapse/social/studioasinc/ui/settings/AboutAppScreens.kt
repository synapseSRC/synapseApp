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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.IntentUtils
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.animation.core.spring

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AboutAppTheme(content: @Composable () -> Unit) {
    val colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) {
        darkColorSchemeAbout()
    } else {
        lightColorSchemeAbout()
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

private fun lightColorSchemeAbout() = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF5A5C7C),
    secondaryContainer = Color(0xFFDCE2F9),
    onSecondaryContainer = Color(0xFF131C2B),
    tertiaryContainer = Color(0xFFFFD8EE),
    onTertiaryContainer = Color(0xFF2E1125),
    surface = Color(0xFFFAF9FD),
    surfaceContainerLow = Color(0xFFF3F3FA),
    surfaceContainer = Color(0xFFEEEDF3),
    surfaceContainerHigh = Color(0xFFE9E8EF),
    surfaceContainerHighest = Color(0xFFE3E2E6),
    onSurface = Color(0xFF1B1B1F),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    inverseSurface = Color(0xFF303034),
    inverseOnSurface = Color(0xFFF2F0F4),
    inversePrimary = Color(0xFFA8C7FA),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFAF9FD),
    onBackground = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE3E2E6)
)

private fun darkColorSchemeAbout() = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFFC0C2FD),
    onPrimary = Color(0xFF042C71),
    primaryContainer = Color(0xFF04409F),
    onPrimaryContainer = Color(0xFFE0E0FC),
    secondary = Color(0xFFC3C3E9),
    secondaryContainer = Color(0xFF424463),
    onSecondaryContainer = Color(0xFFE0E0FE),
    tertiaryContainer = Color(0xFF6E334E),
    onTertiaryContainer = Color(0xFFFFD8E7),
    surface = Color(0xFF131317),
    surfaceContainerLow = Color(0xFF1B1B1F),
    surfaceContainer = Color(0xFF1F1F23),
    surfaceContainerHigh = Color(0xFF2A2A2E),
    surfaceContainerHighest = Color(0xFF343439),
    onSurface = Color(0xFFE2E2E8),
    onSurfaceVariant = Color(0xFFC6C5D2),
    outline = Color(0xFF90909C),
    outlineVariant = Color(0xFF464651),
    inverseSurface = Color(0xFFE2E2E8),
    inverseOnSurface = Color(0xFF303034),
    inversePrimary = Color(0xFF3757BA),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF131317),
    onBackground = Color(0xFFE2E2E8),
    surfaceVariant = Color(0xFF343439)
)

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
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
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
fun WhatsNewScreen(onBackClick: () -> Unit) {
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
    var expandedLatest by rememberSaveable { mutableStateOf(false) }
    val githubReleasesUrl = stringResource(R.string.about_github_releases_url)

    AboutAppTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
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
                        expanded = expandedLatest,
                        onToggle = { expandedLatest = !expandedLatest }
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
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) { contents() }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
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

private data class LicenseEntry(
    val name: String,
    val licenseAndAuthor: String,
    val author: String,
    val repositoryUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutOpenSourceScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var filterMenuOpen by remember { mutableStateOf(false) }
    var sortByAuthor by rememberSaveable { mutableStateOf(false) }
    val allEntries = listOf(
        LicenseEntry(
            stringResource(R.string.about_license_compose),
            stringResource(R.string.about_license_compose_detail),
            stringResource(R.string.about_author_android),
            stringResource(R.string.about_license_url_android)
        ),
        LicenseEntry(
            stringResource(R.string.about_license_coroutines),
            stringResource(R.string.about_license_coroutines_detail),
            stringResource(R.string.about_author_jetbrains),
            stringResource(R.string.about_license_url_coroutines)
        ),
        LicenseEntry(
            stringResource(R.string.about_license_coil),
            stringResource(R.string.about_license_coil_detail),
            stringResource(R.string.about_author_coil),
            stringResource(R.string.about_license_url_coil)
        ),
        LicenseEntry(
            stringResource(R.string.about_license_retrofit),
            stringResource(R.string.about_license_retrofit_detail),
            stringResource(R.string.about_author_square),
            stringResource(R.string.about_license_url_retrofit)
        ),
        LicenseEntry(
            stringResource(R.string.about_license_lottie),
            stringResource(R.string.about_license_lottie_detail),
            stringResource(R.string.about_author_airbnb),
            stringResource(R.string.about_license_url_lottie)
        )
    )
    val visibleEntries = allEntries
        .filter { it.name.contains(query, ignoreCase = true) || it.licenseAndAuthor.contains(query, ignoreCase = true) }
        .let { entries -> if (sortByAuthor) entries.sortedBy { it.author } else entries }

    AboutAppTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
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
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (query.isEmpty()) {
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
                                        onClick = { sortByAuthor = false; filterMenuOpen = false },
                                        leadingIcon = { Icon(Icons.Filled.Code, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.about_license_sort_author)) },
                                        onClick = { sortByAuthor = true; filterMenuOpen = false },
                                        leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (visibleEntries.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.about_license_no_results),
                            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.space24),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    item {
                        AboutListGroup(
                            items = visibleEntries.map { entry ->
                                AboutLinkItem(
                                    title = entry.name,
                                    supportingText = entry.licenseAndAuthor,
                                    leadingIcon = Icons.Filled.Code,
                                    trailingIcon = Icons.Filled.Launch,
                                    onClick = { IntentUtils.openUrl(context, entry.repositoryUrl) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private enum class FeedbackCategory(val labelRes: Int) {
    FeatureSuggestion(R.string.about_feedback_category_feature),
    BugReport(R.string.about_feedback_category_bug),
    PerformanceIssue(R.string.about_feedback_category_performance),
    Other(R.string.about_feedback_category_other)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBackClick: () -> Unit,
    onSubmitSuccess: () -> Unit,
    viewModel: FeedbackViewModel = viewModel()
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
    val categories = FeedbackCategory.values()

    AboutAppTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
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
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false },
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(dimensionResource(R.dimen.about_menu_corner))
                            )
                        ) {
                            categories.forEach { option ->
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
            containerColor = MaterialTheme.colorScheme.surface,
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
