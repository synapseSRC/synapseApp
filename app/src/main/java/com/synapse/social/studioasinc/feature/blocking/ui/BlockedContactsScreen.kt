package com.synapse.social.studioasinc.feature.blocking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.blocking.BlockingUiState
import com.synapse.social.studioasinc.feature.blocking.BlockingViewModel
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.BlockedUser
import kotlinx.datetime.Clock

/**
 * Screen displaying list of blocked users.
 * 
 * Follows Material Theme guidelines with no hardcoded values.
 * Uses Scaffold with TopAppBar, LazyColumn for blocked users list,
 * loading indicator, empty state, and unblock confirmation dialog.
 * 
 * Validates Requirements 2.3, 2.4, 2.5, 3.1, 3.3, 3.4, 4.1, 4.4, 8.1, 8.2, 8.5
 * 
 * @param viewModel The BlockingViewModel for managing state
 * @param onNavigateBack Callback when back navigation is requested
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedContactsScreen(
    viewModel: BlockingViewModel,
    onNavigateBack: () -> Unit
) {
    val blockedUsers by viewModel.blockedUsers.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var userToUnblock by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Load blocked users when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadBlockedUsers()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.blocked_contacts),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Show loading indicator when loading and list is empty
                uiState is BlockingUiState.Loading && blockedUsers.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // Show empty state when no blocked users
                blockedUsers.isEmpty() && uiState !is BlockingUiState.Loading -> {
                    EmptyBlockedListContent()
                }
                // Show list of blocked users
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            vertical = Spacing.Small
                        )
                    ) {
                        items(
                            items = blockedUsers,
                            key = { it.id }
                        ) { blockedUser ->
                            BlockedUserItem(
                                blockedUser = blockedUser,
                                onUnblockClick = {
                                    userToUnblock = blockedUser.blockedUserId
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Show unblock confirmation dialog
    userToUnblock?.let { userId ->
        UnblockConfirmationDialog(
            onConfirm = {
                viewModel.unblockUser(userId)
                userToUnblock = null
            },
            onDismiss = {
                userToUnblock = null
            }
        )
    }
    
    // Handle UI state changes for success and error messages
    val unblockSuccessMessage = stringResource(R.string.unblock_success)
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is BlockingUiState.UnblockSuccess -> {
                snackbarHostState.showSnackbar(
                    message = unblockSuccessMessage,
                    duration = SnackbarDuration.Short
                )
                viewModel.resetState()
            }
            is BlockingUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetState()
            }
            else -> {}
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlockedContactsScreenPreviewWithUsers() {
    MaterialTheme {
        // Preview with mock data would require a preview-specific implementation
        // This is a placeholder for the preview
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Blocked Contacts Screen Preview")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlockedContactsScreenPreviewEmpty() {
    MaterialTheme {
        EmptyBlockedListContent()
    }
}
