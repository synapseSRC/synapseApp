package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyCheckupScreen(
    onNavigateToCategory: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_privacy_checkup_section)) },
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
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(Sizes.IconGiant),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.Medium))

                        Text(
                            text = "Your privacy matters",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(Spacing.Small))

                        Text(
                            text = "Control your privacy settings and set up the privacy options that match how you want to use the app.",
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
                    SettingsNavigationItem(
                        title = "Choose who can contact you",
                        subtitle = "Prevent unwanted calls and control who can message or add you to groups",
                        imageVector = Icons.Filled.Call,
                        position = SettingsItemPosition.Top,
                        onClick = { onNavigateToCategory("contact") }
                    )
                    SettingsNavigationItem(
                        title = "Control your personal info",
                        subtitle = "Manage visibility of your profile photo, online status, and activity",
                        imageVector = Icons.Filled.Person,
                        position = SettingsItemPosition.Middle,
                        onClick = { onNavigateToCategory("personal_info") }
                    )
                    SettingsNavigationItem(
                        title = "Add more privacy to your chats",
                        subtitle = "Protect your messages and media with lock and timer controls",
                        imageVector = Icons.Filled.Chat,
                        position = SettingsItemPosition.Middle,
                        onClick = { onNavigateToCategory("chats") }
                    )
                    SettingsNavigationItem(
                        title = "Add more protection to your account",
                        subtitle = "Review security settings to help safeguard access to your account",
                        imageVector = Icons.Filled.Lock,
                        position = SettingsItemPosition.Bottom,
                        onClick = { onNavigateToCategory("account") }
                    )
                }
            }

            item {
                Text(
                    text = "Privacy Checkup helps you review your existing privacy settings.",
                    style = SettingsTypography.itemSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.Small)
                )
            }
        }
    }
}
