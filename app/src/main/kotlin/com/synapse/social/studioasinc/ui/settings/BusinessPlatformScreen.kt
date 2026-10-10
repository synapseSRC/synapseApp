package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.business.AccountType
import com.synapse.social.studioasinc.shared.domain.model.business.AnalyticsData
import com.synapse.social.studioasinc.shared.domain.model.business.VerificationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessPlatformScreen(
    viewModel: BusinessPlatformViewModel,
    onBackClick: () -> Unit,
    onNavigateToBrandPartnerships: () -> Unit = {},
    onNavigateToScheduledPosts: () -> Unit = {},
    onNavigateToContentCalendar: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_business_platform_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            if (error != null) {
                Snackbar(
                    modifier = Modifier.padding(Spacing.Medium),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(R.string.action_dismiss))
                        }
                    }
                ) { Text(error ?: "") }
            }
        }
    ) { paddingValues ->
        if (isLoading && state.analytics == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(paddingValues)
                    .padding(horizontal = SettingsSpacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.Large)
            ) {
                item {
                    AccountTypeSection(state = state, onSwitchToBusiness = { viewModel.switchToBusinessAccount() })
                }
                if (state.isBusinessAccount) {
                    item { AnalyticsDashboardSection(state.analytics) }
                    item {
                        MonetizationSection(
                            state = state,
                            onToggleMonetization = { viewModel.toggleMonetization(it) }
                        )
                    }
                    item {
                        ProfessionalToolsSection(
                            onNavigateToBrandPartnerships = onNavigateToBrandPartnerships,
                            onNavigateToScheduledPosts = onNavigateToScheduledPosts,
                            onNavigateToContentCalendar = onNavigateToContentCalendar,
                            onNavigateToAnalytics = onNavigateToAnalytics
                        )
                    }
                    item {
                        VerificationSection(
                            status = state.verificationStatus,
                            onApply = { viewModel.applyForVerification() }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(Spacing.Large)) }
            }
        }
    }
}

@Composable
private fun AccountTypeSection(
    state: BusinessPlatformState,
    onSwitchToBusiness: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.business_section_account_type)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            modifier = Modifier.padding(vertical = SettingsSpacing.itemVerticalPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SettingsSpacing.itemHorizontalPadding)
            ) {
                Icon(
                    imageVector = when (state.accountType) {
                        AccountType.BUSINESS -> Icons.Default.Business
                        AccountType.CREATOR -> Icons.Default.Work
                        else -> Icons.Default.Group
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(SettingsSpacing.iconSize)
                )
                Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(
                        text = state.accountType.name.lowercase()
                            .replaceFirstChar { it.uppercase() } + " " +
                                stringResource(R.string.business_account_type_suffix),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (!state.isBusinessAccount) {
                        Text(
                            text = stringResource(R.string.business_switch_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (!state.isBusinessAccount) {
                Button(
                    onClick = onSwitchToBusiness,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SettingsSpacing.itemHorizontalPadding)
                ) {
                    Text(stringResource(R.string.action_switch_to_business))
                }
            }
        }
    }
}

@Composable
private fun AnalyticsDashboardSection(analytics: AnalyticsData?) {
    if (analytics == null) return

    SettingsSection(title = stringResource(R.string.business_section_analytics)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            modifier = Modifier.padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = SettingsSpacing.itemVerticalPadding
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                AnalyticsCard(
                    title = stringResource(R.string.business_analytics_profile_views),
                    value = analytics.profileViews.toString(),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsCard(
                    title = stringResource(R.string.business_analytics_engagement),
                    value = "${analytics.engagementRate}%",
                    modifier = Modifier.weight(1f)
                )
            }

            if (analytics.topPosts.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SettingsColors.cardBackgroundElevated)
                ) {
                    Column(modifier = Modifier.padding(Spacing.Medium)) {
                        Text(
                            text = stringResource(R.string.business_analytics_top_content),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = Spacing.Medium)
                        )
                        analytics.topPosts.forEachIndexed { index, post ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.Small),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}. ${post.title}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = Spacing.Medium)
                                )
                                Text(
                                    text = "${post.views} ${stringResource(R.string.business_analytics_views_suffix)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (index < analytics.topPosts.size - 1) {
                                SettingsDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SettingsColors.cardBackgroundElevated)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonetizationSection(
    state: BusinessPlatformState,
    onToggleMonetization: (Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.business_section_monetization)) {
        Column {
            SettingsToggleItem(
                title = stringResource(R.string.business_monetization_enable),
                subtitle = stringResource(R.string.business_monetization_subtitle),
                checked = state.monetizationEnabled,
                onCheckedChange = onToggleMonetization,
                position = if (state.monetizationEnabled && state.revenue != null)
                    SettingsItemPosition.Top else SettingsItemPosition.Single
            )
            if (state.monetizationEnabled && state.revenue != null) {
                SettingsDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = SettingsSpacing.itemHorizontalPadding,
                            vertical = SettingsSpacing.itemVerticalPadding
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        Text(
                            text = stringResource(R.string.business_monetization_total_earnings),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$${state.revenue.totalEarnings}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        modifier = Modifier.size(Spacing.Huge),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                SettingsDivider()
                SettingsNavigationItem(
                    title = stringResource(R.string.business_payout_settings),
                    subtitle = stringResource(R.string.business_payout_subtitle),
                    imageVector = Icons.Filled.Security,
                    onClick = {},
                    position = SettingsItemPosition.Bottom
                )
            }
        }
    }
}

@Composable
private fun ProfessionalToolsSection(
    onNavigateToBrandPartnerships: () -> Unit,
    onNavigateToScheduledPosts: () -> Unit,
    onNavigateToContentCalendar: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.business_section_tools)) {
        Column {
            SettingsNavigationItem(
                title = stringResource(R.string.business_tool_scheduled_posts),
                subtitle = stringResource(R.string.business_tool_scheduled_posts_subtitle),
                imageVector = Icons.Default.Schedule,
                onClick = onNavigateToScheduledPosts,
                position = SettingsItemPosition.Top
            )
            SettingsDivider()
            SettingsNavigationItem(
                title = stringResource(R.string.business_tool_content_calendar),
                subtitle = stringResource(R.string.business_tool_content_calendar_subtitle),
                imageVector = Icons.Default.CalendarToday,
                onClick = onNavigateToContentCalendar,
                position = SettingsItemPosition.Middle
            )
            SettingsDivider()
            SettingsNavigationItem(
                title = stringResource(R.string.business_tool_brand_partnerships),
                subtitle = stringResource(R.string.business_tool_brand_partnerships_subtitle),
                imageVector = Icons.Default.Work,
                onClick = onNavigateToBrandPartnerships,
                position = SettingsItemPosition.Middle
            )
            SettingsDivider()
            SettingsNavigationItem(
                title = stringResource(R.string.business_tool_analytics),
                subtitle = stringResource(R.string.business_tool_analytics_subtitle),
                imageVector = Icons.Default.Analytics,
                onClick = onNavigateToAnalytics,
                position = SettingsItemPosition.Bottom
            )
        }
    }
}

@Composable
private fun VerificationSection(
    status: VerificationStatus,
    onApply: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.business_section_verification)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SettingsSpacing.itemHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                Text(
                    text = stringResource(R.string.business_verification_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(
                        when (status) {
                            VerificationStatus.VERIFIED -> R.string.business_verification_verified
                            VerificationStatus.PENDING -> R.string.business_verification_pending
                            VerificationStatus.REJECTED -> R.string.business_verification_rejected
                            VerificationStatus.NOT_APPLIED -> R.string.business_verification_not_applied
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (status) {
                        VerificationStatus.VERIFIED -> MaterialTheme.colorScheme.primary
                        VerificationStatus.REJECTED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            when (status) {
                VerificationStatus.NOT_APPLIED, VerificationStatus.REJECTED -> {
                    Button(onClick = onApply) {
                        Text(stringResource(R.string.action_apply))
                    }
                }
                VerificationStatus.VERIFIED -> {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = stringResource(R.string.business_verification_verified),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                VerificationStatus.PENDING -> {
                    CircularProgressIndicator(modifier = Modifier.size(Spacing.Large))
                }
            }
        }
    }
}
