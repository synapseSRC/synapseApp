package com.synapse.social.studioasinc.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * Payout configuration for business accounts that have monetization enabled.
 *
 * The screen is the destination behind the "Business payout settings" row in
 * [BusinessPlatformScreen]. Payout details are stored locally until the payout
 * backend is available, so the selections survive app restarts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayoutSettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(PAYOUT_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    val payoutMethods = listOf(
        stringResource(R.string.payout_method_bank_transfer),
        stringResource(R.string.payout_method_paypal),
        stringResource(R.string.payout_method_payoneer)
    )
    val payoutSchedules = listOf(
        stringResource(R.string.payout_schedule_weekly),
        stringResource(R.string.payout_schedule_monthly),
        stringResource(R.string.payout_schedule_manual)
    )

    var payoutMethod by rememberSaveable {
        mutableStateOf(preferences.getString(KEY_PAYOUT_METHOD, null) ?: payoutMethods.first())
    }
    var payoutSchedule by rememberSaveable {
        mutableStateOf(preferences.getString(KEY_PAYOUT_SCHEDULE, null) ?: payoutSchedules.first())
    }
    var payoutNotificationsEnabled by rememberSaveable {
        mutableStateOf(preferences.getBoolean(KEY_PAYOUT_NOTIFICATIONS, true))
    }

    Scaffold(
        containerColor = AppColors.screenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.payout_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.payout_settings_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.screenBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = AppSpacing.screenPadding,
                    vertical = Spacing.space16
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.space24)
        ) {
            SettingsSection(title = stringResource(R.string.payout_settings_config_section)) {
                SettingsSelectionItem(
                    title = stringResource(R.string.payout_settings_method_title),
                    subtitle = stringResource(R.string.payout_settings_method_subtitle),
                    icon = Icons.Default.AccountBalance,
                    options = payoutMethods,
                    selectedOption = payoutMethod,
                    onSelect = { method ->
                        payoutMethod = method
                        preferences.edit().putString(KEY_PAYOUT_METHOD, method).apply()
                    }
                )
            }

            SettingsSection(title = stringResource(R.string.payout_settings_schedule_section)) {
                SettingsSelectionItem(
                    title = stringResource(R.string.payout_settings_schedule_title),
                    subtitle = stringResource(R.string.payout_settings_schedule_subtitle),
                    icon = Icons.Default.Schedule,
                    options = payoutSchedules,
                    selectedOption = payoutSchedule,
                    onSelect = { schedule ->
                        payoutSchedule = schedule
                        preferences.edit().putString(KEY_PAYOUT_SCHEDULE, schedule).apply()
                    }
                )
            }

            SettingsSection(title = stringResource(R.string.payout_settings_notifications_section)) {
                SettingsToggleItem(
                    title = stringResource(R.string.payout_settings_notifications_title),
                    subtitle = stringResource(R.string.payout_settings_notifications_subtitle),
                    imageVector = Icons.Default.Notifications,
                    checked = payoutNotificationsEnabled,
                    onCheckedChange = { enabled ->
                        payoutNotificationsEnabled = enabled
                        preferences.edit().putBoolean(KEY_PAYOUT_NOTIFICATIONS, enabled).apply()
                    }
                )
            }

            Text(
                text = stringResource(R.string.payout_settings_storage_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AppSpacing.itemHorizontalPadding)
            )

            Spacer(modifier = Modifier.height(Spacing.space24))
        }
    }
}

private const val PAYOUT_PREFERENCES_NAME = "payout_settings"
private const val KEY_PAYOUT_METHOD = "payout_method"
private const val KEY_PAYOUT_SCHEDULE = "payout_schedule"
private const val KEY_PAYOUT_NOTIFICATIONS = "payout_notifications_enabled"
