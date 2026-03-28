package com.synapse.social.studioasinc.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.core.util.IntentUtils

private object AboutSupportConstants {
    const val URL_GITHUB_BUG_REPORT = "https://github.com/synapseSRC/synapseApp/issues/new?template=bug_report.md"
    const val URL_APP_WEBSITE = "https://synapsesocial.vercel.app"
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSupportScreen(
    onBackClick: () -> Unit,
    onNavigateToLicenses: () -> Unit = {},
    viewModel: AboutSupportViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val appVersion by viewModel.appVersion.collectAsState()
    val buildNumber by viewModel.buildNumber.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()


    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing),
            contentPadding = PaddingValues(vertical = Spacing.Small)
        ) {

            item {
                AppInfoHeaderCard(
                    appVersion = appVersion,
                    buildNumber = buildNumber
                )
            }


            item {
                SettingsSection(title = stringResource(R.string.about_section_legal)) {
                    SettingsNavigationItem(
                        title = stringResource(R.string.about_terms),
                        subtitle = stringResource(R.string.about_terms_subtitle),
                        imageVector = Icons.Filled.Description,
                        onClick = {
                            val url = viewModel.getTermsOfServiceUrl()
                            IntentUtils.openUrl(context, url)
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = stringResource(R.string.about_privacy_policy),
                        subtitle = stringResource(R.string.about_privacy_policy_subtitle),
                        imageVector = Icons.Filled.Shield,
                        onClick = {
                            val url = viewModel.getPrivacyPolicyUrl()
                            IntentUtils.openUrl(context, url)
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = stringResource(R.string.about_licenses),
                        subtitle = stringResource(R.string.about_licenses_subtitle),
                        imageVector = Icons.Filled.BugReport,
                        onClick = {
                            viewModel.navigateToLicenses()
                            onNavigateToLicenses()
                        }
                    )
                }
            }


            item {
                SettingsSection(title = stringResource(R.string.about_section_support)) {
                    SettingsNavigationItem(
                        title = stringResource(R.string.about_help_center),
                        subtitle = stringResource(R.string.about_help_center_subtitle),
                        imageVector = Icons.Filled.Info,
                        onClick = {
                            val url = viewModel.getHelpCenterUrl()
                            IntentUtils.openUrl(context, url)
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = stringResource(R.string.about_report_problem),
                        subtitle = stringResource(R.string.about_report_problem_subtitle),
                        imageVector = Icons.Filled.BugReport,
                        onClick = {
                            IntentUtils.openUrl(context, AboutSupportConstants.URL_GITHUB_BUG_REPORT)
                        }
                    )
                }
            }


            item {
                SettingsSection(title = stringResource(R.string.about_updates)) {
                    SettingsNavigationItem(
                        title = stringResource(R.string.about_check_updates),
                        subtitle = stringResource(R.string.about_check_updates_subtitle),
                        imageVector = Icons.Filled.Download,
                        onClick = {
                            IntentUtils.openUrl(context, AboutSupportConstants.URL_APP_WEBSITE)
                        }
                    )
                }
            }


            item {
                Text(
                    text = stringResource(R.string.about_copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.Medium)
                )
            }
        }
    }

}



@Composable
private fun AppInfoHeaderCard(
    appVersion: String,
    buildNumber: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsShapes.cardShape,
        color = SettingsColors.cardBackgroundElevated,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SettingsSpacing.profileHeaderPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {

            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(Sizes.AvatarExtraLarge)
            )


            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )


            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
            ) {
                Text(
                    text = stringResource(R.string.about_version) + " $appVersion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.about_build) + " $buildNumber",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
