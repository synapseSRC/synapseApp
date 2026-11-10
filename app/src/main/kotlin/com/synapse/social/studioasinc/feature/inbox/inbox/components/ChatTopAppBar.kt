package com.synapse.social.studioasinc.feature.inbox.inbox.components

import android.content.ClipData
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.synapse.social.studioasinc.feature.shared.components.SingleLineMarqueeText
import com.synapse.social.studioasinc.feature.shared.components.liquidGlassSurface
import com.synapse.social.studioasinc.feature.shared.components.rememberGlassPressProgress
import com.synapse.social.studioasinc.feature.shared.components.rememberLiquidGlassPaint
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpOffset
import com.kyant.backdrop.Backdrop
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.components.UserAvatarWithStatus
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.StatusOnline
import com.synapse.social.studioasinc.shared.domain.model.chat.ContentStatus
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.util.TimestampFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopAppBar(
    selectedMessageIds: Set<String>,
    messages: List<Message>,
    participantId: String?,
    participantProfile: User?,
    initialParticipantName: String?,
    initialParticipantAvatar: String?,
    typingStatus: com.synapse.social.studioasinc.shared.domain.model.chat.TypingStatus?,
    isParticipantActive: Boolean,
    chatId: String,
    disappearingMode: DisappearingMode,
    isGroupChat: Boolean = false,
    groupTitle: String? = null,
    groupAvatarUrl: String? = null,
    groupMemberCount: Int = 0,
    isMuted: Boolean = false,
    isMuteUpdating: Boolean = false,
    // Shared-element modifiers for the pill's picture, name and status line, applied by the screen
    // so each of them travels into Contact Info instead of being re-created there.
    avatarModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    statusModifier: Modifier = Modifier,
    // The conversation captured behind the header, which each floating surface samples so it
    // refracts the wallpaper and the messages moving underneath it. Null keeps the fallback
    // translucent surfaces.
    backdrop: Backdrop? = null,
    onClearSelection: () -> Unit,
    onDeleteSelectedMessages: () -> Unit,
    onNavigateBack: () -> Unit,
    onSummarizeChat: () -> Unit,
    onNavigateToGroupInfo: (String, String) -> Unit,
    onNavigateToChatInfo: (String, String) -> Unit,
    onOpenDisappearingMessages: () -> Unit,
    onStartVoiceCall: () -> Unit = {},
    onStartVideoCall: () -> Unit = {},
    showCallActions: Boolean = true,
    onSearchInChat: () -> Unit = {},
    onOpenMediaLinksAndDocs: () -> Unit = {},
    onToggleMuteNotifications: () -> Unit = {},
    onOpenChatTheme: () -> Unit = {},
    onReportChat: () -> Unit = {},
    onBlockChat: () -> Unit = {},
    onClearChat: () -> Unit = {},
    onExportChat: () -> Unit = {},
    onViewEarliestMessage: () -> Unit = {},
    onAddToChatFolder: () -> Unit = {},
    onDeleteChat: () -> Unit = {}
) {
    val clipboard = LocalClipboard.current
    val clipLabel = stringResource(R.string.clip_label_message)
    val scope = rememberCoroutineScope()

    if (selectedMessageIds.isNotEmpty()) {
        TopAppBar(
            title = { Text(stringResource(R.string.selected_count_title, selectedMessageIds.size)) },
            navigationIcon = {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                }
            },
            actions = {
                IconButton(onClick = {
                    val selectedContent = messages
                        .filter {
                            it.id in selectedMessageIds &&
                                !it.isDeleted &&
                                it.contentStatus != ContentStatus.DELETED
                        }
                        .joinToString("\n") { it.content }
                    if (selectedContent.isNotBlank()) {
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(clipLabel, selectedContent)))
                        }
                    }
                    onClearSelection()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                }
                IconButton(onClick = onDeleteSelectedMessages) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    } else {
        // One floating overlay header: a circular back button, a title pill carrying the avatar /
        // title / status, and an action pill carrying the call and overflow actions. The timeline
        // keeps drawing and scrolling behind all three surfaces.
        val isGroupLikeChat = isGroupChat || participantProfile?.accountType == "group"
        val onViewContact: () -> Unit = {
            if (isGroupLikeChat) {
                onNavigateToGroupInfo(
                    chatId,
                    groupTitle ?: participantProfile?.displayName ?: initialParticipantName ?: "Group"
                )
            } else {
                onNavigateToChatInfo(chatId, participantId ?: "")
            }
        }
        val contactInfoLabel = stringResource(
            if (isGroupLikeChat) R.string.chat_menu_group_info else R.string.chat_menu_contact_info
        )

        val titleText = if (isGroupChat) {
            groupTitle ?: initialParticipantName ?: stringResource(R.string.group_chat_fallback_title)
        } else {
            participantProfile?.displayName
                ?: participantProfile?.username
                ?: initialParticipantName
                ?: participantId ?: "Chat"
        }
        // A group header reports its roster; a DM reports what the other side is doing right now.
        val statusText = chatHeaderStatusText(
            isGroupChat = isGroupChat,
            groupMemberCount = groupMemberCount,
            isTyping = typingStatus?.isTyping == true,
            isParticipantActive = isParticipantActive,
            lastSeen = participantProfile?.lastSeen
        )
        val statusColor = when {
            !isGroupChat && typingStatus?.isTyping == true -> MaterialTheme.colorScheme.primary
            !isGroupChat && isParticipantActive -> StatusOnline
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        ChatFloatingTopBar(
            backdrop = backdrop,
            title = titleText,
            status = statusText,
            statusColor = statusColor,
            avatarUrl = if (isGroupChat) {
                groupAvatarUrl ?: initialParticipantAvatar
            } else {
                participantProfile?.avatar ?: initialParticipantAvatar
            },
            avatarUserId = if (isGroupChat) "" else participantId ?: "",
            avatarModifier = avatarModifier,
            titleModifier = titleModifier,
            statusModifier = statusModifier,
            avatarDisplayName = if (isGroupChat) {
                null // Group avatar comes from the first-letters API, not UI rendering.
            } else {
                participantProfile?.displayName ?: participantProfile?.username ?: initialParticipantName
            },
            showAvatarActiveStatus = !isGroupChat,
            isGroupChat = isGroupChat,
            disappearingEnabled = disappearingMode != DisappearingMode.OFF,
            contactInfoLabel = contactInfoLabel,
            onNavigateBack = onNavigateBack,
            onViewContact = onViewContact,
            showCallActions = showCallActions,
            onStartVoiceCall = onStartVoiceCall,
            onStartVideoCall = onStartVideoCall,
            isMuted = isMuted,
            isMuteUpdating = isMuteUpdating,
            canBlock = !isGroupChat && participantId != null,
            onSummarizeChat = onSummarizeChat,
            onSearchInChat = onSearchInChat,
            onOpenMediaLinksAndDocs = onOpenMediaLinksAndDocs,
            onOpenDisappearingMessages = onOpenDisappearingMessages,
            onToggleMuteNotifications = onToggleMuteNotifications,
            onOpenChatTheme = onOpenChatTheme,
            onReportChat = onReportChat,
            onBlockChat = onBlockChat,
            onClearChat = onClearChat,
            onExportChat = onExportChat,
            onViewEarliestMessage = onViewEarliestMessage,
            onAddToChatFolder = onAddToChatFolder,
            onDeleteChat = onDeleteChat
        )
    }
}

/**
 * Height shared by the three floating surfaces, so the circular back button and both pills line up
 * on one baseline. 48dp is the slimmest height that still leaves each pill's icon buttons their
 * full touch target, and it keeps the header from reading as a bar.
 */
private val FloatingHeaderHeight = Sizes.ButtonHeightLarge

/** Avatar diameter: a 2dp inset top and bottom, so the avatar nearly fills the pill it sits in. */
private val FloatingHeaderAvatarSize = FloatingHeaderHeight - Spacing.space4

/**
 * What the floating header occupies below the status bar - the tallest surface plus the row's own
 * vertical padding - so the timeline knows how far its oldest item has to scroll clear of it.
 */
val ChatFloatingHeaderHeight = FloatingHeaderHeight + Spacing.space4 * 2

/**
 * Fallback translucency for the floating surfaces on hardware that cannot render the glass: the
 * active theme's surface-container role at a high alpha, so the wallpaper still reads through
 * faintly without a blur or a refraction the device cannot draw.
 */
private const val FloatingHeaderSurfaceAlpha = 0.92f
private const val FloatingHeaderBorderAlpha = 0.45f

// The glass itself - its parameters, the theme decisions and the modifier that draws it - lives in
// feature/shared/components/LiquidGlass.kt, because the header and the chat composer are one
// material and should always be read from one place.

/**
 * The one line under the name in the header: a group's roster, or what the other side is doing right
 * now.
 *
 * It lives outside [ChatTopAppBar] because the conversation derives the same line itself when it
 * hands the status to Contact Info, so the two screens say the same words about the same person
 * rather than each composing their own.
 */
@Composable
internal fun chatHeaderStatusText(
    isGroupChat: Boolean,
    groupMemberCount: Int,
    isTyping: Boolean,
    isParticipantActive: Boolean,
    lastSeen: String?
): String = if (isGroupChat) {
    if (groupMemberCount > 0) {
        pluralStringResource(R.plurals.group_chat_member_count, groupMemberCount, groupMemberCount)
    } else {
        stringResource(R.string.group_chat_fallback_title)
    }
} else {
    val formattedLastSeen = lastSeen
        ?.let { formatChatTimestamp(it) }
        ?.takeIf { it.isNotBlank() }
    when {
        isTyping -> "Typing..."
        isParticipantActive -> "Online"
        formattedLastSeen != null -> "Last seen $formattedLastSeen"
        else -> "Offline"
    }
}

/**
 * Telegram-style floating chat header: three independent surfaces drawn over the conversation
 * instead of a full-width bar, so the wallpaper and the messages stay visible behind them.
 */
@Composable
private fun ChatFloatingTopBar(
    backdrop: Backdrop?,
    title: String,
    status: String,
    statusColor: Color,
    avatarUrl: String?,
    avatarUserId: String,
    avatarDisplayName: String?,
    avatarModifier: Modifier,
    titleModifier: Modifier,
    statusModifier: Modifier,
    showAvatarActiveStatus: Boolean,
    isGroupChat: Boolean,
    disappearingEnabled: Boolean,
    contactInfoLabel: String,
    onNavigateBack: () -> Unit,
    onViewContact: () -> Unit,
    showCallActions: Boolean,
    onStartVoiceCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    isMuted: Boolean,
    isMuteUpdating: Boolean,
    canBlock: Boolean,
    onSummarizeChat: () -> Unit,
    onSearchInChat: () -> Unit,
    onOpenMediaLinksAndDocs: () -> Unit,
    onOpenDisappearingMessages: () -> Unit,
    onToggleMuteNotifications: () -> Unit,
    onOpenChatTheme: () -> Unit,
    onReportChat: () -> Unit,
    onBlockChat: () -> Unit,
    onClearChat: () -> Unit,
    onExportChat: () -> Unit,
    onViewEarliestMessage: () -> Unit,
    onAddToChatFolder: () -> Unit,
    onDeleteChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Clear of the status icons only; the wallpaper still paints behind the transparent
            // status bar because nothing here reserves layout space.
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Spacing.space4, vertical = Spacing.space4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FloatingBackButton(backdrop = backdrop, onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.width(Spacing.space4))
        FloatingTitlePill(
            modifier = Modifier.weight(1f),
            backdrop = backdrop,
            title = title,
            status = status,
            statusColor = statusColor,
            avatarUrl = avatarUrl,
            avatarUserId = avatarUserId,
            avatarDisplayName = avatarDisplayName,
            avatarModifier = avatarModifier,
            titleModifier = titleModifier,
            statusModifier = statusModifier,
            showAvatarActiveStatus = showAvatarActiveStatus,
            disappearingEnabled = disappearingEnabled,
            onClickLabel = contactInfoLabel,
            onViewContact = onViewContact
        )
        Spacer(modifier = Modifier.width(Spacing.space4))
        FloatingActionsPill(
            backdrop = backdrop,
            isGroupChat = isGroupChat,
            onViewContact = onViewContact,
            showCallActions = showCallActions,
            onStartVoiceCall = onStartVoiceCall,
            onStartVideoCall = onStartVideoCall,
            isMuted = isMuted,
            isMuteUpdating = isMuteUpdating,
            canBlock = canBlock,
            onSummarizeChat = onSummarizeChat,
            onSearchInChat = onSearchInChat,
            onOpenMediaLinksAndDocs = onOpenMediaLinksAndDocs,
            onOpenDisappearingMessages = onOpenDisappearingMessages,
            onToggleMuteNotifications = onToggleMuteNotifications,
            onOpenChatTheme = onOpenChatTheme,
            onReportChat = onReportChat,
            onBlockChat = onBlockChat,
            onClearChat = onClearChat,
            onExportChat = onExportChat,
            onViewEarliestMessage = onViewEarliestMessage,
            onAddToChatFolder = onAddToChatFolder,
            onDeleteChat = onDeleteChat
        )
    }
}

/**
 * The single glass treatment behind every floating surface, so the three pieces of the header cannot
 * drift apart in tint, refraction or edge.
 *
 * [backdrop] is the conversation the screen captured behind the header - the wallpaper, the messages
 * and the top scrim - and each surface samples it through its own shape, so the glass follows
 * whatever scrolls underneath it. A null backdrop (the screen passes null below the RenderEffect API
 * level rather than paying for a layer nothing can draw) keeps the plain translucent panel this
 * header used before, so older hardware loses nothing.
 */
@Composable
private fun FloatingHeaderSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    backdrop: Backdrop?,
    interactionSource: MutableInteractionSource,
    content: @Composable () -> Unit
) {
    val paint = rememberLiquidGlassPaint()
    if (backdrop == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = paint.tint.copy(alpha = FloatingHeaderSurfaceAlpha),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = Sizes.ElevationLow,
            shadowElevation = Sizes.ElevationLow,
            border = BorderStroke(
                width = Sizes.BorderHairline,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = FloatingHeaderBorderAlpha)
            ),
            content = content
        )
    } else {
        // Held buttons shrink the glass disc they sit on; the surface reports through the same
        // interaction source either way.
        val pressProgress = rememberGlassPressProgress(interactionSource)
        Box(
            modifier = modifier
                .liquidGlassSurface(
                    backdrop = backdrop,
                    shape = shape,
                    paint = paint,
                    pressProgress = pressProgress
                )
                // Parity with the M3 Surface this Box replaces: the surface stays one traversal group
                // for accessibility, and the minimum constraints still reach its content.
                .semantics { isTraversalGroup = true },
            propagateMinConstraints = true
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                content()
            }
        }
    }
}

@Composable
private fun FloatingBackButton(backdrop: Backdrop?, onNavigateBack: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    FloatingHeaderSurface(
        modifier = Modifier.size(FloatingHeaderHeight),
        shape = CircleShape,
        backdrop = backdrop,
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // The button reports into the surface's own source, so holding it shrinks the glass
            // disc together with the icon.
            IconButton(onClick = onNavigateBack, interactionSource = interactionSource) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
        }
    }
}

@Composable
private fun FloatingTitlePill(
    modifier: Modifier,
    backdrop: Backdrop?,
    title: String,
    status: String,
    statusColor: Color,
    avatarUrl: String?,
    avatarUserId: String,
    avatarDisplayName: String?,
    avatarModifier: Modifier,
    titleModifier: Modifier,
    statusModifier: Modifier,
    showAvatarActiveStatus: Boolean,
    disappearingEnabled: Boolean,
    onClickLabel: String,
    onViewContact: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    FloatingHeaderSurface(
        modifier = modifier.height(FloatingHeaderHeight),
        shape = RoundedCornerShape(percent = 50),
        backdrop = backdrop,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                // One source for the whole pill: the tap that opens Contact Info and the pill's own
                // press feedback come from the same gesture instead of two competing ones.
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClickLabel = onClickLabel,
                    onClick = onViewContact
                )
                // Horizontal only: the avatar already carries its own inset inside the pill, so
                // a vertical padding here would only shrink it.
                .padding(horizontal = Spacing.space4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatarWithStatus(
                userId = avatarUserId,
                avatarUrl = avatarUrl,
                size = FloatingHeaderAvatarSize,
                modifier = avatarModifier,
                showActiveStatus = showAvatarActiveStatus,
                displayName = avatarDisplayName
            )
            Spacer(modifier = Modifier.width(Spacing.space8))
            Column(modifier = Modifier.weight(1f)) {
                // The name and the status below it are values Contact Info shows as well, so each
                // one sits in a host that keeps the marquee's width while the shared element itself
                // measures to the words: the travelling element is then a line of type in both
                // screens rather than a full-width slot, which is what lets it move between them
                // without the glyphs being stretched on the way over. basicMarquee measures its
                // content unbounded and only clamps the width it reports, so the marquee still
                // scrolls a name that is too long for the pill.
                Box(modifier = Modifier.fillMaxWidth()) {
                    SingleLineMarqueeText(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = titleModifier
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (disappearingEnabled) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = stringResource(R.string.chat_menu_disappearing_messages),
                            modifier = Modifier.size(Sizes.IconSmall),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(Spacing.space4))
                    }
                    // Wrap content height: a ColumnScope weight would mean a *vertical* fill and
                    // let the status line grow the pill instead of just scrolling. The host carries
                    // the weight so the shared element can stay the width of the words themselves.
                    Box(modifier = Modifier.weight(1f)) {
                        SingleLineMarqueeText(
                            text = status,
                            modifier = statusModifier,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingActionsPill(
    backdrop: Backdrop?,
    isGroupChat: Boolean,
    onViewContact: () -> Unit,
    showCallActions: Boolean,
    onStartVoiceCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    isMuted: Boolean,
    isMuteUpdating: Boolean,
    canBlock: Boolean,
    onSummarizeChat: () -> Unit,
    onSearchInChat: () -> Unit,
    onOpenMediaLinksAndDocs: () -> Unit,
    onOpenDisappearingMessages: () -> Unit,
    onToggleMuteNotifications: () -> Unit,
    onOpenChatTheme: () -> Unit,
    onReportChat: () -> Unit,
    onBlockChat: () -> Unit,
    onClearChat: () -> Unit,
    onExportChat: () -> Unit,
    onViewEarliestMessage: () -> Unit,
    onAddToChatFolder: () -> Unit,
    onDeleteChat: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    FloatingHeaderSurface(
        modifier = Modifier.height(FloatingHeaderHeight),
        shape = RoundedCornerShape(percent = 50),
        backdrop = backdrop,
        interactionSource = interactionSource
    ) {
        Row(
            // Wrap content: a fillMaxSize row inside a surface that only fixes its height claims
            // the whole remaining width of the header, and Row measures non-weighted children
            // first, so the weighted title pill next to it would be left with zero width.
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video calling lives in the overflow menu so the pill keeps the reference's two-icon
            // geometry (call + more options).
            if (showCallActions) {
                // Both buttons report into the pill's own source, so pressing either of them
                // moves the whole surface the way the back button does.
                IconButton(onClick = onStartVoiceCall, interactionSource = interactionSource) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = stringResource(R.string.chat_action_voice_call)
                    )
                }
            }
            // The overflow menu anchors to this, and this button is flush with the pill's trailing
            // edge, so the menu lines up with the right edge of the pill.
            ChatOverflowMenu(
                interactionSource = interactionSource,
                isGroupChat = isGroupChat,
                canBlock = canBlock,
                isMuted = isMuted,
                isMuteUpdating = isMuteUpdating,
                showVideoCall = showCallActions,
                onSummarizeChat = onSummarizeChat,
                onViewContact = onViewContact,
                onSearch = onSearchInChat,
                onMediaLinksAndDocs = onOpenMediaLinksAndDocs,
                onMuteNotifications = onToggleMuteNotifications,
                onDisappearingMessages = onOpenDisappearingMessages,
                onChatTheme = onOpenChatTheme,
                onStartVideoCall = onStartVideoCall,
                onReport = onReportChat,
                onBlock = onBlockChat,
                onClearChat = onClearChat,
                onExportChat = onExportChat,
                onViewEarliestMessage = onViewEarliestMessage,
                onAddToChatFolder = onAddToChatFolder,
                onDeleteChat = onDeleteChat
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSearchTopAppBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.search_messages_hint)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseSearch) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun ChatOverflowMenu(
    // The pill hands its own source down, so pressing the three-dot button reads as the pill's own
    // feedback rather than as a separate control lighting up.
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    isGroupChat: Boolean,
    canBlock: Boolean,
    isMuted: Boolean = false,
    isMuteUpdating: Boolean = false,
    showVideoCall: Boolean = true,
    onSummarizeChat: () -> Unit,
    onViewContact: () -> Unit,
    onSearch: () -> Unit,
    onMediaLinksAndDocs: () -> Unit,
    onMuteNotifications: () -> Unit,
    onDisappearingMessages: () -> Unit,
    onChatTheme: () -> Unit,
    onStartVideoCall: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onClearChat: () -> Unit,
    onExportChat: () -> Unit,
    onViewEarliestMessage: () -> Unit,
    onAddToChatFolder: () -> Unit,
    onDeleteChat: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }
    val dismissMenu: () -> Unit = {
        expanded = false
        moreExpanded = false
    }

    Box {
        IconButton(onClick = { expanded = true }, interactionSource = interactionSource) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.chat_action_more_options)
            )
        }
        // Anchored to the three-dot button, which sits flush with the trailing edge of the floating
        // pill, so the menu opens directly beneath that edge and M3 slides it inward when a narrow
        // screen cannot fit it. The popup takes focus, which is what stops a dismissing tap from
        // reaching the message underneath.
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = dismissMenu,
            offset = DpOffset(x = Spacing.space0, y = Spacing.space4)
        ) {
            ChatDropdownMenuItem(
                labelRes = if (isMuted) R.string.chat_menu_unmute_notifications else R.string.chat_menu_mute_notifications,
                icon = if (isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                enabled = !isMuteUpdating,
                trailingIcon = {
                    Text(
                        text = stringResource(
                            if (isMuted) R.string.chat_menu_mute_state_muted else R.string.chat_menu_mute_state_unmuted
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = {
                    expanded = false
                    onMuteNotifications()
                }
            )
            if (showVideoCall) {
                ChatDropdownMenuItem(
                    labelRes = R.string.chat_action_video_call,
                    icon = Icons.Default.Videocam,
                    onClick = {
                        expanded = false
                        onStartVideoCall()
                    }
                )
            }
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_search,
                icon = Icons.Default.Search,
                onClick = {
                    expanded = false
                    onSearch()
                }
            )
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_set_wallpaper,
                icon = Icons.Default.Palette,
                onClick = {
                    expanded = false
                    onChatTheme()
                }
            )
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_view_earliest_message,
                icon = Icons.Default.History,
                onClick = {
                    expanded = false
                    onViewEarliestMessage()
                }
            )
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_add_to_folder,
                icon = Icons.Default.Folder,
                onClick = {
                    expanded = false
                    onAddToChatFolder()
                }
            )

            // Destructive entries sit behind a divider so a mis-tap cannot reach them.
            ChatMenuDivider()
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_clear_chat,
                icon = Icons.Default.DeleteSweep,
                destructive = true,
                onClick = {
                    expanded = false
                    onClearChat()
                }
            )
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_delete_chat,
                icon = Icons.Default.Delete,
                destructive = true,
                onClick = {
                    expanded = false
                    onDeleteChat()
                }
            )

            // The pre-existing entries stay reachable, below the actions this header now promotes.
            ChatMenuDivider()
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_summary,
                icon = Icons.Default.AutoAwesome,
                onClick = {
                    expanded = false
                    onSummarizeChat()
                }
            )
            ChatDropdownMenuItem(
                labelRes = if (isGroupChat) R.string.chat_menu_group_info else R.string.chat_menu_contact_info,
                icon = if (isGroupChat) Icons.Default.Groups else Icons.Default.Person,
                onClick = {
                    expanded = false
                    onViewContact()
                }
            )
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_media_links_docs,
                icon = Icons.Default.PermMedia,
                onClick = {
                    expanded = false
                    onMediaLinksAndDocs()
                }
            )
            ChatDropdownMenuItem(
                labelRes = R.string.chat_menu_disappearing_messages,
                icon = Icons.Default.Timer,
                onClick = {
                    expanded = false
                    onDisappearingMessages()
                }
            )
            Box {
                ChatDropdownMenuItem(
                    labelRes = R.string.chat_menu_more,
                    icon = Icons.Default.MoreHoriz,
                    trailingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null
                        )
                    },
                    onClick = { moreExpanded = true }
                )
                DropdownMenu(
                    expanded = moreExpanded,
                    onDismissRequest = { moreExpanded = false }
                ) {
                    ChatDropdownMenuItem(
                        labelRes = R.string.chat_menu_report,
                        icon = Icons.Default.Report,
                        destructive = true,
                        onClick = {
                            dismissMenu()
                            onReport()
                        }
                    )
                    ChatDropdownMenuItem(
                        labelRes = R.string.chat_menu_block,
                        icon = Icons.Default.Block,
                        destructive = true,
                        hidden = !canBlock,
                        onClick = {
                            dismissMenu()
                            onBlock()
                        }
                    )
                    ChatDropdownMenuItem(
                        labelRes = R.string.chat_menu_export_chat,
                        icon = Icons.Default.Share,
                        onClick = {
                            dismissMenu()
                            onExportChat()
                        }
                    )
                }
            }
        }
    }
}

/** Subtle separation between the everyday actions and the ones that destroy something. */
@Composable
private fun ChatMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = Spacing.space4),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun ChatDropdownMenuItem(
    @StringRes labelRes: Int,
    icon: ImageVector,
    destructive: Boolean = false,
    hidden: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    if (hidden) return
    val contentColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(labelRes),
                color = contentColor
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )
        },
        trailingIcon = trailingIcon,
        enabled = enabled,
        onClick = onClick
    )
}

/**
 * Formats an ISO or DB timestamp for "last seen" display.
 */
private fun formatChatTimestamp(isoTimestamp: String): String {
    val formatted = TimestampFormatter.formatLastSeen(isoTimestamp)
    return formatted.ifBlank {
        TimestampFormatter.formatRelative(isoTimestamp)
    }
}
