package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.components.FolderTabRow
import com.synapse.social.studioasinc.feature.inbox.inbox.models.InboxFilter
import com.synapse.social.studioasinc.feature.inbox.inbox.models.InboxSearchCategory
import com.synapse.social.studioasinc.feature.call.CallLaunchRequest
import com.synapse.social.studioasinc.feature.inbox.inbox.screens.CallsTabScreen
import com.synapse.social.studioasinc.feature.inbox.inbox.screens.ContactsTabScreen
import com.synapse.social.studioasinc.feature.inbox.inbox.screens.InboxArchivedScreen
import com.synapse.social.studioasinc.feature.inbox.inbox.screens.InboxConversationRow
import com.synapse.social.studioasinc.feature.inbox.inbox.screens.InboxSearchScreen
import com.synapse.social.studioasinc.feature.inbox.inbox.screens.InboxSelectionToolbar
import com.synapse.social.studioasinc.feature.inbox.inbox.theme.InboxExpressiveDimens
import com.synapse.social.studioasinc.feature.shared.components.UserAvatar
import com.synapse.social.studioasinc.feature.shared.theme.GroupedItemPosition
import com.synapse.social.studioasinc.feature.shared.theme.GroupedListSpacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.cardSurfaceColor
import com.synapse.social.studioasinc.shared.domain.model.chat.Conversation
import com.synapse.social.studioasinc.shared.domain.model.settings.ChatFolder
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private enum class InboxDestination { INBOX, SEARCH, ARCHIVED, SELECTION }

@Composable
fun InboxScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String, String?, String?, String?) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCreateGroup: () -> Unit = {},
    onStartCall: (CallLaunchRequest) -> Unit = {},
    onOpenEchoRooms: () -> Unit = {},
    debugToolsEnabled: Boolean = false,
    viewModel: InboxViewModel = hiltViewModel()
) {
    InboxScreenContent(
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToChat = onNavigateToChat,
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToCreateGroup = onNavigateToCreateGroup,
        onStartCall = onStartCall,
        onOpenEchoRooms = onOpenEchoRooms,
        debugToolsEnabled = debugToolsEnabled,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InboxScreenContent(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String, String?, String?, String?) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onStartCall: (CallLaunchRequest) -> Unit,
    onOpenEchoRooms: () -> Unit,
    debugToolsEnabled: Boolean,
    viewModel: InboxViewModel
) {
    var destination by rememberSaveable { mutableStateOf(InboxDestination.INBOX) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedChatIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedArchivedIds by remember { mutableStateOf(emptySet<String>()) }
    var showCreateMenu by rememberSaveable { mutableStateOf(false) }
    var showFolderSheet by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSelectionMenu by remember { mutableStateOf(false) }
    var showInboxMenu by remember { mutableStateOf(false) }
    var showArchivedMenu by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUserProfile by viewModel.currentUserProfile.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val filteredConversations by viewModel.filteredConversations.collectAsState()
    val archivedConversations by viewModel.archivedConversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingArchived by viewModel.isLoadingArchived.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val folders by viewModel.chatFolders.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val unreadThreadCount = conversations.count { it.unreadCount > 0 }
    val selectedConversations = conversations.filter { it.chatId in selectedChatIds }
    val allSelectedPinned = selectedConversations.isNotEmpty() && selectedConversations.all { it.isPinned }
    val allSelectedMuted = selectedConversations.isNotEmpty() && selectedConversations.all { it.isMuted }
    val noFolderLabel = stringResource(R.string.inbox_no_folder)
    val assignedFolderNames = folders.filter { folder -> selectedChatIds.any { it in folder.includedChatIds } }.map { it.name }.distinct()
    val currentFolderLabel = assignedFolderNames.takeIf { it.isNotEmpty() }?.joinToString() ?: noFolderLabel

    LaunchedEffect(Unit) { viewModel.loadArchivedConversations() }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isFirstResume by rememberSaveable { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    viewModel.onVisibilityChanged(true)
                    if (isFirstResume) {
                        isFirstResume = false
                    } else {
                        viewModel.onAppResumed()
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> viewModel.onVisibilityChanged(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            snackbarHostState.showSnackbar(error.orEmpty())
            viewModel.clearError()
        }
    }
    LaunchedEffect(destination, selectedTab) {
        if (destination != InboxDestination.INBOX || selectedTab != 0) showCreateMenu = false
        when (destination) {
            InboxDestination.SEARCH -> viewModel.searchInbox("", InboxSearchCategory.UNREAD)
            InboxDestination.ARCHIVED -> viewModel.loadArchivedConversations()
            else -> Unit
        }
    }

    fun returnToInbox() {
        destination = InboxDestination.INBOX
        selectedChatIds = emptySet()
        selectedArchivedIds = emptySet()
        showSelectionMenu = false
        showArchivedMenu = false
        viewModel.searchInbox("")
    }

    fun archiveSelected() {
        viewModel.archiveConversations(selectedChatIds) { archivedIds ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.resources.getQuantityString(R.plurals.inbox_archived_snackbar, archivedIds.size, archivedIds.size),
                    actionLabel = context.getString(R.string.inbox_undo),
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.unarchiveConversations(archivedIds)
            }
        }
        returnToInbox()
    }

    fun openConversation(conversation: Conversation) {
        val participantId = conversation.participantId.takeIf { !conversation.isGroup }
        val navigate = { onNavigateToChat(conversation.chatId, participantId, conversation.participantName, conversation.participantAvatar) }
        if (viewModel.isChatLocked(conversation.chatId)) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                viewModel.authenticateChat(
                    activity = activity,
                    onSuccess = navigate,
                    onError = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
                )
            } else {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.inbox_chat_locked_unavailable)) }
            }
        } else {
            navigate()
        }
    }

    BackHandler(enabled = destination != InboxDestination.INBOX) {
        if (destination == InboxDestination.ARCHIVED && selectedArchivedIds.isNotEmpty()) selectedArchivedIds = emptySet()
        else returnToInbox()
    }

    Scaffold(
        topBar = {
            val barColors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            when (destination) {
                InboxDestination.INBOX -> TopAppBar(
                    title = { Text(stringResource(R.string.inbox_brand)) },
                    navigationIcon = {
                        val accountLabel = stringResource(R.string.inbox_account)
                        IconButton(
                            onClick = {
                                val profileId = currentUserProfile?.uid
                                if (profileId != null) onNavigateToProfile(profileId)
                                else scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.inbox_profile_unavailable)) }
                            },
                            modifier = Modifier.semantics { contentDescription = accountLabel }
                        ) {
                            UserAvatar(
                                avatarUrl = currentUserProfile?.avatar,
                                displayName = currentUserProfile?.displayName ?: currentUserProfile?.username,
                                size = Sizes.AvatarSmall
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { destination = InboxDestination.SEARCH }) {
                            Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.inbox_search_action), tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Box {
                            IconButton(onClick = { showInboxMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.inbox_menu_more), tint = MaterialTheme.colorScheme.onSurface)
                            }
                            DropdownMenu(expanded = showInboxMenu, onDismissRequest = { showInboxMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.inbox_menu_archived, archivedConversations.size)) },
                                    leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
                                    onClick = { showInboxMenu = false; destination = InboxDestination.ARCHIVED }
                                )
                            }
                        }
                    },
                    colors = barColors
                )
                InboxDestination.SEARCH -> TopAppBar(
                    title = { Text(stringResource(R.string.inbox_search_title)) },
                    navigationIcon = { IconButton(onClick = ::returnToInbox) { Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.inbox_back)) } },
                    colors = barColors
                )
                InboxDestination.ARCHIVED -> if (selectedArchivedIds.isEmpty()) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.inbox_archive_screen_title)) },
                        navigationIcon = { IconButton(onClick = ::returnToInbox) { Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.inbox_back)) } },
                        colors = barColors
                    )
                } else {
                    TopAppBar(
                        title = { Text(pluralStringResource(R.plurals.inbox_selected_conversations, selectedArchivedIds.size, selectedArchivedIds.size)) },
                        navigationIcon = { IconButton(onClick = { selectedArchivedIds = emptySet() }) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.inbox_close_selection)) } },
                        actions = {
                            IconButton(onClick = { viewModel.unarchiveConversations(selectedArchivedIds); selectedArchivedIds = emptySet() }) {
                                Icon(Icons.Rounded.Unarchive, contentDescription = stringResource(R.string.inbox_action_unarchive))
                            }
                            Box {
                                IconButton(onClick = { showArchivedMenu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.inbox_more_actions)) }
                                DropdownMenu(expanded = showArchivedMenu, onDismissRequest = { showArchivedMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.inbox_select_all)) },
                                        onClick = { selectedArchivedIds = archivedConversations.map { it.chatId }.toSet(); showArchivedMenu = false }
                                    )
                                }
                            }
                        },
                        colors = barColors
                    )
                }
                InboxDestination.SELECTION -> TopAppBar(
                    title = { Text(pluralStringResource(R.plurals.inbox_selected_conversations, selectedChatIds.size, selectedChatIds.size)) },
                    navigationIcon = { IconButton(onClick = ::returnToInbox) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.inbox_close_selection)) } },
                    actions = {
                        Box {
                            IconButton(onClick = { showSelectionMenu = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.inbox_more_actions)) }
                            DropdownMenu(expanded = showSelectionMenu, onDismissRequest = { showSelectionMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.inbox_select_all)) },
                                    onClick = { selectedChatIds = conversations.map { it.chatId }.toSet(); showSelectionMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.inbox_cancel)) },
                                    onClick = { returnToInbox() }
                                )
                            }
                        }
                    },
                    colors = barColors
                )
            }
        },
        bottomBar = {
            if (destination == InboxDestination.INBOX) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface) {
                    val labels = listOf(R.string.inbox_messages_tab, R.string.inbox_calls_tab, R.string.inbox_contacts_tab)
                    labels.forEachIndexed { index, labelRes ->
                        val selected = selectedTab == index
                        NavigationBarItem(
                            selected = selected,
                            onClick = { selectedTab = index },
                            label = { Text(stringResource(labelRes)) },
                            icon = {
                                val icon = when (index) {
                                    0 -> if (selected) Icons.Filled.Email else Icons.Outlined.Email
                                    1 -> if (selected) Icons.Filled.Call else Icons.Outlined.Call
                                    else -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
                                }
                                Icon(icon, contentDescription = null)
                            },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (destination == InboxDestination.INBOX && selectedTab == 0) {
                FloatingActionButtonMenu(
                    expanded = showCreateMenu,
                    button = {
                        FloatingActionButton(
                            onClick = { showCreateMenu = !showCreateMenu },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = if (showCreateMenu) Icons.Rounded.Close else Icons.Rounded.Add,
                                contentDescription = stringResource(if (showCreateMenu) R.string.inbox_fab_close_options else R.string.inbox_fab_open_options)
                            )
                        }
                    }
                ) {
                    FloatingActionButtonMenuItem(
                        onClick = { showCreateMenu = false; selectedTab = 2 },
                        icon = { Icon(Icons.Rounded.Chat, contentDescription = null) },
                        text = { Text(stringResource(R.string.inbox_fab_create_chat)) }
                    )
                    FloatingActionButtonMenuItem(
                        onClick = { showCreateMenu = false; onNavigateToCreateGroup() },
                        icon = { Icon(Icons.Rounded.Group, contentDescription = null) },
                        text = { Text(stringResource(R.string.inbox_fab_create_group)) }
                    )
                    FloatingActionButtonMenuItem(
                        onClick = {
                            showCreateMenu = false
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.inbox_fab_create_channel_coming_soon)) }
                        },
                        icon = { Icon(Icons.Rounded.Campaign, contentDescription = null) },
                        text = { Text(stringResource(R.string.inbox_fab_create_channel)) }
                    )
                    FloatingActionButtonMenuItem(
                        onClick = {
                            showCreateMenu = false
                            onOpenEchoRooms()
                        },
                        icon = { Icon(Icons.Rounded.Mic, contentDescription = null) },
                        text = { Text(stringResource(R.string.inbox_fab_echo_room)) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        AnimatedContent(
            targetState = destination,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            transitionSpec = {
                when {
                    targetState == InboxDestination.SEARCH || initialState == InboxDestination.SEARCH ->
                        fadeIn(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f)) togetherWith fadeOut(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f))
                    else ->
                        fadeIn(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f)) togetherWith fadeOut(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f))
                }.using(SizeTransform(clip = false))
            },
            label = "inbox-destination"
        ) { page ->
            when (page) {
                InboxDestination.INBOX -> AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = { fadeIn(spring(dampingRatio = 1f, stiffness = 1600f)) togetherWith fadeOut(spring(dampingRatio = 1f, stiffness = 1600f)) },
                    label = "inbox-bottom-destinations"
                ) { tab ->
                    when (tab) {
                        0 -> HomeMessages(
                            conversations = filteredConversations,
                            isLoading = isLoading,
                            isRefreshing = isRefreshing,
                            error = error,
                            selectedFilter = selectedFilter,
                            unreadThreadCount = unreadThreadCount,
                            folders = folders,
                            selectedFolderId = selectedFolderId,
                            archivedCount = archivedConversations.size,
                            onFilterChange = viewModel::selectFilter,
                            onFolderSelected = viewModel::selectFolder,
                            onOpenArchive = { destination = InboxDestination.ARCHIVED },
                            onRetry = viewModel::loadConversations,
                            onOpenConversation = ::openConversation,
                            onLongPressConversation = { chatId -> selectedChatIds = setOf(chatId); destination = InboxDestination.SELECTION }
                        )
                        1 -> CallsTabScreen(
                            contentPadding = PaddingValues(),
                            // The simulated-call launcher is only meaningful when the mock call repository
                            // is the one backing the screen; on a real backend it would place real calls.
                            debugToolsEnabled = debugToolsEnabled && BuildConfig.CALL_SIMULATION_ENABLED,
                            onStartCall = onStartCall
                        )
                        else -> ContactsTabScreen(
                            onNavigateToChat = onNavigateToChat,
                            onNavigateToSearch = onNavigateToSearch,
                            onStartCall = onStartCall,
                            contentPadding = PaddingValues()
                        )
                    }
                }
                InboxDestination.SEARCH -> InboxSearchScreen(
                    state = searchState,
                    recentConversations = conversations.sortedByDescending { it.lastMessageTime ?: "" },
                    onQueryChange = { viewModel.searchInbox(it) },
                    onCategoryChange = { viewModel.searchInbox(searchState.query, it) },
                    onOpenConversation = ::openConversation,
                    onOpenPerson = { person -> onNavigateToChat("new", person.uid, person.displayName ?: person.username, person.avatar) }
                )
                InboxDestination.ARCHIVED -> InboxArchivedScreen(
                    conversations = archivedConversations,
                    isLoading = isLoadingArchived,
                    selectedChatIds = selectedArchivedIds,
                    onOpenConversation = ::openConversation,
                    onToggleSelection = { chatId ->
                        selectedArchivedIds = if (chatId in selectedArchivedIds) selectedArchivedIds - chatId else selectedArchivedIds + chatId
                    },
                    onUnarchive = { chatId -> viewModel.unarchiveConversations(setOf(chatId)) }
                )
                InboxDestination.SELECTION -> SelectionContent(
                    conversations = conversations,
                    selectedChatIds = selectedChatIds,
                    currentFolderLabel = currentFolderLabel,
                    allSelectedPinned = allSelectedPinned,
                    allSelectedMuted = allSelectedMuted,
                    onToggleSelection = { chatId ->
                        val updated = if (chatId in selectedChatIds) selectedChatIds - chatId else selectedChatIds + chatId
                        selectedChatIds = updated
                        if (updated.isEmpty()) destination = InboxDestination.INBOX
                    },
                    onPin = { viewModel.setConversationsPinned(selectedChatIds, selectedConversations.any { !it.isPinned }) },
                    onMute = { viewModel.setConversationsMuted(selectedChatIds, selectedConversations.any { !it.isMuted }) },
                    onArchive = ::archiveSelected,
                    onFolder = { showFolderSheet = true },
                    onDelete = { showDeleteDialog = true },
                    onDone = ::returnToInbox
                )
            }
        }
    }

    if (showFolderSheet) {
        ModalBottomSheet(onDismissRequest = { showFolderSheet = false }) {
            Text(stringResource(R.string.inbox_folder_picker_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = Spacing.space24, vertical = Spacing.space8))
            if (folders.isEmpty()) {
                Text(stringResource(R.string.inbox_folder_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Spacing.space24, vertical = Spacing.space8))
            }
            folders.forEach { folder ->
                ListItem(
                    headlineContent = { Text(folder.name) },
                    leadingContent = { Icon(Icons.Rounded.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.assignConversationsToFolder(selectedChatIds, folder.id)
                        showFolderSheet = false
                    }
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.inbox_folder_create)) },
                leadingContent = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth().clickable { showFolderSheet = false; showCreateFolderDialog = true }
            )
            Spacer(Modifier.height(Spacing.space24))
        }
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false; newFolderName = "" },
            title = { Text(stringResource(R.string.inbox_folder_create)) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text(stringResource(R.string.inbox_folder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = newFolderName.isNotBlank(),
                    onClick = {
                        viewModel.createFolderAndAssign(newFolderName, selectedChatIds)
                        showCreateFolderDialog = false
                        newFolderName = ""
                    }
                ) { Text(stringResource(R.string.inbox_folder_save)) }
            },
            dismissButton = { TextButton(onClick = { showCreateFolderDialog = false; newFolderName = "" }) { Text(stringResource(R.string.inbox_cancel)) } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.inbox_delete_title)) },
            text = { Text(stringResource(R.string.inbox_delete_message, selectedChatIds.size)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteConversations(selectedChatIds)
                    showDeleteDialog = false
                    returnToInbox()
                }) { Text(stringResource(R.string.inbox_delete_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.inbox_cancel)) } }
        )
    }
}

@Composable
private fun HomeMessages(
    conversations: List<Conversation>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    error: String?,
    selectedFilter: InboxFilter,
    unreadThreadCount: Int,
    folders: List<ChatFolder>,
    selectedFolderId: String?,
    archivedCount: Int,
    onFilterChange: (InboxFilter) -> Unit,
    onFolderSelected: (String?) -> Unit,
    onOpenArchive: () -> Unit,
    onRetry: () -> Unit,
    onOpenConversation: (Conversation) -> Unit,
    onLongPressConversation: (String) -> Unit
) {
    val sections = remember(conversations) { groupConversations(conversations) }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.space16, vertical = Spacing.space8),
            horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
        ) {
            val unreadLabel = if (unreadThreadCount > 0) stringResource(R.string.inbox_filter_unread_count, unreadThreadCount) else stringResource(R.string.inbox_filter_unread)
            listOf(
                InboxFilter.ALL to stringResource(R.string.inbox_filter_all),
                InboxFilter.UNREAD to unreadLabel,
                InboxFilter.FAVORITES to stringResource(R.string.inbox_filter_favorites),
                InboxFilter.GROUPS to stringResource(R.string.inbox_filter_groups)
            ).forEach { (filter, label) ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(label) },
                    leadingIcon = if (selectedFilter == filter) {{ Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }} else null
                )
            }
        }
        if (folders.isNotEmpty()) {
            FolderTabRow(folders = folders, selectedFolderId = selectedFolderId, onFolderSelected = onFolderSelected)
        }
        when {
            isLoading && conversations.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator() }
            error != null && conversations.isEmpty() -> Column(Modifier.fillMaxSize().padding(Spacing.space24), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(error, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(Spacing.space12))
                Button(onClick = onRetry) { Text(stringResource(R.string.inbox_retry)) }
            }
            conversations.isEmpty() -> Column(Modifier.fillMaxSize().padding(Spacing.space24), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Spacing.space48))
                Spacer(Modifier.height(Spacing.space12))
                Text(stringResource(R.string.inbox_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.inbox_empty_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (archivedCount > 0) {
                    Spacer(Modifier.height(Spacing.space16))
                    Button(onClick = onOpenArchive) { Text(stringResource(R.string.inbox_archived_title, archivedCount)) }
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Spacing.space16, end = Spacing.space16, top = Spacing.space8, bottom = Spacing.space24)
            ) {
                if (archivedCount > 0) {
                    item(key = "archived-conversations") {
                        ArchivedConversationsEntry(
                            count = archivedCount,
                            onClick = onOpenArchive,
                            modifier = Modifier.padding(bottom = Spacing.space8)
                        )
                    }
                }
                sections.forEach { (section, sectionConversations) ->
                    item(key = "conversation-header-${section.name}") {
                        Text(
                            text = stringResource(section.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = Spacing.space4, top = Spacing.space16, bottom = Spacing.space8)
                                .semantics { heading() }
                        )
                    }
                    itemsIndexed(sectionConversations, key = { _, conversation -> conversation.chatId }) { index, conversation ->
                        InboxConversationRow(
                            conversation = conversation,
                            position = GroupedItemPosition.at(index, sectionConversations.size),
                            onClick = { onOpenConversation(conversation) },
                            onLongClick = { onLongPressConversation(conversation.chatId) },
                            modifier = if (index == 0) Modifier else Modifier.padding(top = GroupedListSpacing)
                        )
                    }
                }
            }
        }
    }
}

private enum class ConversationSection(@StringRes val labelRes: Int) {
    PINNED(R.string.inbox_section_pinned),
    TODAY(R.string.inbox_section_today),
    YESTERDAY(R.string.inbox_section_yesterday),
    EARLIER(R.string.inbox_section_earlier)
}

/**
 * Section labels only: conversations keep the ViewModel's pinned-first, newest-first order because
 * they are bucketed in arrival order and the buckets render in a fixed order.
 */
private fun groupConversations(
    conversations: List<Conversation>
): List<Pair<ConversationSection, List<Conversation>>> {
    val zone = TimeZone.currentSystemDefault()
    val todayEpochDays = Clock.System.now().toLocalDateTime(zone).date.toEpochDays()
    val buckets = LinkedHashMap<ConversationSection, MutableList<Conversation>>()

    conversations.forEach { conversation ->
        val lastMessageEpochDays = TimestampFormatter.parseInstant(conversation.lastMessageTime)
            ?.toLocalDateTime(zone)?.date?.toEpochDays()
        val section = when {
            conversation.isPinned -> ConversationSection.PINNED
            lastMessageEpochDays == todayEpochDays -> ConversationSection.TODAY
            lastMessageEpochDays == todayEpochDays - 1 -> ConversationSection.YESTERDAY
            else -> ConversationSection.EARLIER
        }
        buckets.getOrPut(section) { mutableListOf() }.add(conversation)
    }

    return ConversationSection.entries.mapNotNull { section ->
        buckets[section]?.let { section to it }
    }
}

@Composable
private fun ArchivedConversationsEntry(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = cardSurfaceColor()),
        shape = GroupedItemPosition.Single.shape,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(InboxExpressiveDimens.ConversationRowHeight).padding(horizontal = Spacing.space16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(InboxExpressiveDimens.ConversationAvatarSize))
            Spacer(Modifier.width(Spacing.space12))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.inbox_archived_title, count), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.inbox_archived_summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SelectionContent(
    conversations: List<Conversation>,
    selectedChatIds: Set<String>,
    currentFolderLabel: String,
    allSelectedPinned: Boolean,
    allSelectedMuted: Boolean,
    onToggleSelection: (String) -> Unit,
    onPin: () -> Unit,
    onMute: () -> Unit,
    onArchive: () -> Unit,
    onFolder: () -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = Spacing.space16)
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = Spacing.space8), contentAlignment = Alignment.Center) {
            InboxSelectionToolbar(
                allPinned = allSelectedPinned,
                allMuted = allSelectedMuted,
                onPin = onPin,
                onMute = onMute,
                onArchive = onArchive,
                onFolder = onFolder,
                onDelete = onDelete
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Spacing.space8)
        ) {
            items(conversations, key = { it.chatId }) { conversation ->
                InboxConversationRow(
                    conversation = conversation,
                    selected = conversation.chatId in selectedChatIds,
                    selectionMode = true,
                    onClick = { onToggleSelection(conversation.chatId) },
                    onLongClick = { onToggleSelection(conversation.chatId) }
                )
            }
            item(key = "folder-assignment") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(InboxExpressiveDimens.OutlineWidth, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.space8, bottom = Spacing.space16)
                ) {
                    Column(Modifier.padding(Spacing.space16), verticalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                        Text(stringResource(R.string.inbox_assign_folder_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.inbox_assign_folder_body, currentFolderLabel), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(InboxExpressiveDimens.MediumActionButtonHeight)) {
                            Icon(Icons.Rounded.Check, contentDescription = null)
                            Spacer(Modifier.width(Spacing.space8))
                            Text(stringResource(R.string.inbox_done_organizing))
                        }
                    }
                }
            }
        }
    }
}
