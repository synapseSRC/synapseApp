package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

private data class CheckupCategoryConfig(
    val title: String,
    val heroSubtitle: String,
    val heroIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyCheckupCategoryScreen(
    categoryId: String,
    onNavigateToGroups: () -> Unit,
    onNavigateToBlockedContacts: () -> Unit,
    onNavigateToProfilePhoto: () -> Unit,
    onNavigateToLastSeen: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToStatus: () -> Unit,
    onNavigateToAppLock: () -> Unit,
    onNavigateToDisappearingMessages: () -> Unit,
    onNavigateToAccountInfo: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val config = when (categoryId) {
        "contact" -> CheckupCategoryConfig(
            title = "Choose who can contact you",
            heroSubtitle = "You're in control of your privacy. Choose who can contact you and help prevent unwanted calls or messages.",
            heroIcon = Icons.Filled.Call
        )
        "personal_info" -> CheckupCategoryConfig(
            title = "Control your personal info",
            heroSubtitle = "Choose the best audience for your personal info, like online status and activity.",
            heroIcon = Icons.Filled.Person
        )
        "chats" -> CheckupCategoryConfig(
            title = "Add more privacy to your chats",
            heroSubtitle = "For even more privacy, limit access to your messages and media with these privacy features.",
            heroIcon = Icons.Filled.Chat
        )
        "account" -> CheckupCategoryConfig(
            title = "Add more protection to your account",
            heroSubtitle = "Review account-protection options that help secure access to your SynapseApp account.",
            heroIcon = Icons.Filled.Lock
        )
        else -> CheckupCategoryConfig(
            title = "Privacy Checkup",
            heroSubtitle = "Review your privacy controls.",
            heroIcon = Icons.Filled.Security
        )
    }

    var silenceUnknownCallers by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(config.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsShapes.cardShape,
                    color = SettingsColors.cardBackgroundElevated,
                    tonalElevation = Sizes.ElevationLow
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.Large),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(Sizes.Height100),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = config.heroIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(Sizes.IconGiant),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.Medium))

                        Text(
                            text = config.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(Spacing.Small))

                        Text(
                            text = config.heroSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing)
                ) {
                    when (categoryId) {
                        "contact" -> {
                            SettingsNavigationItem(
                                title = stringResource(R.string.settings_groups_title),
                                subtitle = "Decide if you want everyone to add you to groups or just your contacts",
                                imageVector = Icons.Filled.Group,
                                position = SettingsItemPosition.Top,
                                onClick = onNavigateToGroups
                            )
                            SettingsToggleItem(
                                title = "Silence unknown callers",
                                subtitle = "Calls from unknown numbers will be silenced automatically",
                                imageVector = Icons.Filled.VolumeOff,
                                checked = silenceUnknownCallers,
                                onCheckedChange = { silenceUnknownCallers = it },
                                position = SettingsItemPosition.Middle
                            )
                            SettingsNavigationItem(
                                title = stringResource(R.string.blocked_contacts),
                                subtitle = "Stop receiving calls, messages, and status updates from selected contacts",
                                imageVector = Icons.Filled.Block,
                                position = SettingsItemPosition.Bottom,
                                onClick = onNavigateToBlockedContacts
                            )
                        }

                        "personal_info" -> {
                            SettingsNavigationItem(
                                title = stringResource(R.string.settings_profile_photo_title),
                                subtitle = "Choose who can view your profile photo",
                                imageVector = Icons.Filled.Person,
                                position = SettingsItemPosition.Top,
                                onClick = onNavigateToProfilePhoto
                            )
                            SettingsNavigationItem(
                                title = stringResource(R.string.settings_last_seen_title),
                                subtitle = "Control who can see your online status and last seen",
                                imageVector = Icons.Filled.Visibility,
                                position = SettingsItemPosition.Middle,
                                onClick = onNavigateToLastSeen
                            )
                            SettingsNavigationItem(
                                title = stringResource(R.string.settings_about_privacy_title),
                                subtitle = "Control who can read your bio and about info",
                                imageVector = Icons.Filled.Info,
                                position = SettingsItemPosition.Middle,
                                onClick = onNavigateToAbout
                            )
                            SettingsNavigationItem(
                                title = stringResource(R.string.settings_status_title),
                                subtitle = "Choose who can view your status updates",
                                imageVector = Icons.Filled.Circle,
                                position = SettingsItemPosition.Bottom,
                                onClick = onNavigateToStatus
                            )
                        }

                        "chats" -> {
                            SettingsNavigationItem(
                                title = stringResource(R.string.settings_app_lock_title),
                                subtitle = "Require authentication to open SynapseApp on your device",
                                imageVector = Icons.Filled.Lock,
                                position = SettingsItemPosition.Top,
                                onClick = onNavigateToAppLock
                            )
                            SettingsNavigationItem(
                                title = stringResource(R.string.settings_disappearing_messages_title),
                                subtitle = "Start new chats with disappearing messages set to your timer",
                                imageVector = Icons.Filled.Timer,
                                position = SettingsItemPosition.Bottom,
                                onClick = onNavigateToDisappearingMessages
                            )
                        }

                        "account" -> {
                            SettingsNavigationItem(
                                title = stringResource(R.string.request_account_info_title),
                                subtitle = "Request a report of your Synapse account information and settings",
                                imageVector = Icons.Filled.Description,
                                position = SettingsItemPosition.Single,
                                onClick = onNavigateToAccountInfo
                            )
                        }
                    }
                }
            }
        }
    }
}
