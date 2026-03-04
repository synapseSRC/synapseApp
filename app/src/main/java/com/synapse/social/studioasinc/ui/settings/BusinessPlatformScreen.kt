package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.domain.model.business.AccountType
import com.synapse.social.studioasinc.shared.domain.model.business.AnalyticsData
import com.synapse.social.studioasinc.shared.domain.model.business.RevenueData
import com.synapse.social.studioasinc.shared.domain.model.business.VerificationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessPlatformScreen(
    viewModel: BusinessPlatformViewModel,
    onBackClick: () -> Unit,
    onNavigateToBrandPartnerships: () -> Unit = {},
    onNavigateToScheduledPosts: () -> Unit = {},
    onNavigateToContentCalendar: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("Business Platform") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            if (error != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error ?: "")
                }
            }
        }
    ) { paddingValues ->
        if (isLoading && state.analytics == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(paddingValues)
                    .padding(horizontal = SettingsSpacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                item {
                    AccountTypeSection(
                        state = state,
                        onSwitchToBusiness = { viewModel.switchToBusinessAccount() }
                    )
                }

                if (state.isBusinessAccount) {

                    item {
                        AnalyticsDashboardSection(state.analytics)
                    }


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
                            onNavigateToContentCalendar = onNavigateToContentCalendar
                        )
                    }


                    item {
                        VerificationSection(
                            status = state.verificationStatus,
                            onApply = { viewModel.applyForVerification() }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun AccountTypeSection(
    state: BusinessPlatformState,
    onSwitchToBusiness: () -> Unit
) {
    SettingsSection(title = "Account Type") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = SettingsSpacing.itemHorizontalPadding)
            ) {
                Icon(
                    imageVector = when (state.accountType) {
                        AccountType.BUSINESS -> Icons.Default.Business
                        AccountType.CREATOR -> Icons.Default.Work
                        else -> Icons.Default.Group
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = state.accountType.name.lowercase().replaceFirstChar { it.uppercase() } + " Account",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (!state.isBusinessAccount) {
                        Text(
                            text = "Switch to access professional tools",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!state.isBusinessAccount) {
                Button(
                    onClick = onSwitchToBusiness,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = SettingsSpacing.itemHorizontalPadding)
                ) {
                    Text("Switch to Business Account")
                }
            }
        }
    }
}

@Composable
fun AnalyticsDashboardSection(analytics: AnalyticsData?) {
    if (analytics == null) return

    SettingsSection(title = "Analytics Dashboard") {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = SettingsSpacing.itemHorizontalPadding)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnalyticsCard(
                    title = "Profile Views",
                    value = analytics.profileViews.toString(),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsCard(
                    title = "Engagement",
                    value = "${analytics.engagementRate}%",
                    modifier = Modifier.weight(1f)
                )
            }




            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SettingsColors.cardBackgroundElevated
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Follower Growth",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )




                    Text(
                        text = "Chart visualization coming soon",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
            }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Top Performing Content",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                analytics.topPosts.forEachIndexed { index, post ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${index + 1}. ${post.title}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${post.views} views",
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

@Composable
fun AnalyticsCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = SettingsColors.cardBackgroundElevated
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
fun MonetizationSection(
    state: BusinessPlatformState,
    onToggleMonetization: (Boolean) -> Unit
) {
    SettingsSection(title = "Monetization") {
        Column {
            SettingsToggleItem(
                title = "Enable Monetization",
                subtitle = "Earn revenue from your content",
                checked = state.monetizationEnabled,
                onCheckedChange = onToggleMonetization
            )

            if (state.monetizationEnabled && state.revenue != null) {
                SettingsDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = SettingsSpacing.itemHorizontalPadding, vertical = SettingsSpacing.itemVerticalPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Total Earnings",
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
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }

                SettingsDivider()
                SettingsNavigationItem(
                    title = "Payout Settings",
                    subtitle = "Manage payment methods",
                    icon = R.drawable.ic_security,
                    onClick = {  }
                )
            }
        }
    }
}

@Composable
fun ProfessionalToolsSection(
    onNavigateToBrandPartnerships: () -> Unit = {},
    onNavigateToScheduledPosts: () -> Unit = {},
    onNavigateToContentCalendar: () -> Unit = {}
) {
    SettingsSection(title = "Professional Tools") {
        Column {
            SettingsNavigationItem(
                title = "Scheduled Posts",
                subtitle = "Manage upcoming content",
                imageVector = Icons.Default.Schedule,
                onClick = onNavigateToScheduledPosts,
                position = SettingsItemPosition.Top
            )
            SettingsDivider()
            SettingsNavigationItem(
                title = "Content Calendar",
                subtitle = "Plan your strategy",
                imageVector = Icons.Default.CalendarToday,
                onClick = onNavigateToContentCalendar,
                position = SettingsItemPosition.Middle
            )
            SettingsDivider()
            SettingsNavigationItem(
                title = "Brand Partnerships",
                subtitle = "Manage collaborations",
                imageVector = Icons.Default.Work,
                onClick = onNavigateToBrandPartnerships,
                position = SettingsItemPosition.Bottom
            )
        }
    }
}

@Composable
fun VerificationSection(
    status: VerificationStatus,
    onApply: () -> Unit
) {
    SettingsSection(title = "Verification") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsSpacing.itemHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Business Verification",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when (status) {
                        VerificationStatus.VERIFIED -> "Your business is verified"
                        VerificationStatus.PENDING -> "Application under review"
                        VerificationStatus.REJECTED -> "Application rejected"
                        VerificationStatus.NOT_APPLIED -> "Apply for blue tick"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (status == VerificationStatus.VERIFIED)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (status == VerificationStatus.NOT_APPLIED) {
                Button(onClick = onApply) {
                    Text("Apply")
                }
            } else if (status == VerificationStatus.VERIFIED) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
