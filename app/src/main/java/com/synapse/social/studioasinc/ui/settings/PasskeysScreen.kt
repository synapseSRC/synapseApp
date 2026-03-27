package com.synapse.social.studioasinc.ui.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.shared.domain.model.auth.Passkey
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasskeysScreen(
    viewModel: PasskeysViewModel,
    onBackClick: () -> Unit
) {
    val passkeys by viewModel.passkeys.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_passkeys_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.addPasskey(context) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_passkey)) }
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
                ) {
                    Text(error ?: "")
                }
            }
        }
    ) { paddingValues ->
        if (isLoading && passkeys.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (passkeys.isEmpty()) {
            EmptyPasskeysState(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(paddingValues)
                    .padding(horizontal = SettingsSpacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
            ) {
                item {
                    SettingsHeaderItem(title = "Your Passkeys")
                }

                item {
                    SettingsCard {
                        passkeys.forEachIndexed { index, passkey ->
                            PasskeyItemRow(
                                passkey = passkey,
                                onDeleteClick = { viewModel.removePasskey(passkey.id) }
                            )
                            if (index < passkeys.size - 1) {
                                SettingsDivider()
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Sizes.HeightLarge))
                }
            }
        }
    }
}

@Composable
private fun PasskeyItemRow(
    passkey: Passkey,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column {
                Text(
                    text = passkey.deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val addedDate = DateUtils.getRelativeTimeSpanString(passkey.dateAdded)
                Text(
                    text = "Added $addedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete passkey",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun EmptyPasskeysState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Key,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(Spacing.Medium))
        Text(
            text = "No Passkeys",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        Text(
            text = "Passkeys allow you to sign in securely without a password using your device's biometrics.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
