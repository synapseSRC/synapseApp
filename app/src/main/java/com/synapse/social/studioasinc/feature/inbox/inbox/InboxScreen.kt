package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.feature.inbox.inbox.components.FolderTabRow
import com.synapse.social.studioasinc.feature.inbox.inbox.components.InboxTopAppBar
import com.synapse.social.studioasinc.feature.inbox.inbox.screens.CallsTabScreen
import com.synapse.social.studioasinc.feature.inbox.inbox.screens.ChatsTabScreen
import com.synapse.social.studioasinc.feature.inbox.inbox.screens.ContactsTabScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String, String?, String?, String?) -> Unit,
    onNavigateToCreateGroup: () -> Unit = {},
    viewModel: InboxViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUserProfile by viewModel.currentUserProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val chatListLayout by viewModel.chatListLayout.collectAsState()
    val chatSwipeGesture by viewModel.chatSwipeGesture.collectAsState()
    val chatFolders by viewModel.chatFolders.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val filteredConversations by viewModel.filteredConversations.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),

        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreateGroup) {
                Icon(Icons.Filled.Group, contentDescription = "Create Group")
            }
        },
        topBar = {
            InboxTopAppBar(
                title = stringResource(R.string.title_inbox),
                avatarUrl = currentUserProfile?.avatar,
                scrollBehavior = scrollBehavior,
                onProfileClick = { currentUserProfile?.id?.let(onNavigateToProfile) }
            )
        },
        bottomBar = {
             NavigationBar(
                 containerColor = MaterialTheme.colorScheme.surfaceContainer,
                 contentColor = MaterialTheme.colorScheme.onSurface
             ) {
                 val tabs = listOf(stringResource(R.string.tab_messages), "Calls", "Contacts")
                 tabs.forEachIndexed { index, title ->
                     val selected = pagerState.currentPage == index
                     NavigationBarItem(
                         selected = selected,
                         onClick = {
                             scope.launch { pagerState.animateScrollToPage(index) }
                         },
                         label = {
                             Text(
                                 text = title,
                                 color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                             )
                         },
                         icon = {
                             Icon(
                                 imageVector = when (index) {
                                     0 -> if (selected) Icons.Filled.Email else Icons.Outlined.Email
                                     1 -> if (selected) Icons.Filled.Call else Icons.Outlined.Call
                                     else -> if (selected) Icons.Filled.Group else Icons.Outlined.Group
                                 },
                                 contentDescription = title,
                                 tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                             )
                         },
                         colors = NavigationBarItemDefaults.colors(
                             selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                             selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                             indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                             unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                             unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                     )
                 }
             }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { paddingValues ->

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> Column(modifier = Modifier.fillMaxSize()) {
                    if (chatFolders.isNotEmpty()) {
                        FolderTabRow(
                            folders = chatFolders,
                            selectedFolderId = selectedFolderId,
                            onFolderSelected = { viewModel.selectFolder(it) }
                        )
                    }
                    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
                    androidx.compose.material3.Scaffold(
                        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
                        modifier = Modifier.fillMaxSize()
                    ) { innerInnerPadding ->
                        ChatsTabScreen(
                            conversations = filteredConversations,
                            isLoading = isLoading,
                            error = error,
                            onConversationClick = { chatId, userId, userName, avatar ->
                                if (viewModel.isChatLocked(chatId)) {
                                    val activity = (context as? androidx.fragment.app.FragmentActivity)
                                    if (activity != null) {
                                        viewModel.authenticateChat(
                                            activity = activity,
                                            onSuccess = { onNavigateToChat(chatId, userId, userName, avatar) },
                                            onError = { /* Handle error, maybe show Snackbar */ }
                                        )
                                    }
                                } else {
                                    onNavigateToChat(chatId, userId, userName, avatar)
                                }
                            },
                            isLocked = { viewModel.isChatLocked(it) },
                            onRetry = { viewModel.loadConversations() },
                            chatListLayout = chatListLayout,
                            chatSwipeGesture = chatSwipeGesture,
                            contentPadding = innerInnerPadding,
                            folders = chatFolders,
                            snackbarHostState = snackbarHostState,
                            onArchive = { viewModel.archiveConversation(it) },
                            onDelete = { viewModel.deleteConversation(it) },
                            onUndoArchive = { viewModel.undoArchiveConversation(it) },
                            onAssignToFolder = { chatId, folderId -> viewModel.assignConversationToFolder(chatId, folderId) }
                        )
                    }
                }
                1 -> CallsTabScreen()
                2 -> ContactsTabScreen()
            }
        }
    }
}
