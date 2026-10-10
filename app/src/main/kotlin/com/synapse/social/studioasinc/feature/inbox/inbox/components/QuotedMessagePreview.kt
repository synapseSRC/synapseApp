package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.AutoDownloadManager
import com.synapse.social.studioasinc.data.repository.SettingsRepositoryImpl
import com.synapse.social.studioasinc.data.repository.SettingsRepository
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.ui.settings.MediaType

/**
 * A compact, reusable quoted-message preview shared by the composer and sent message bubbles.
 * It is deliberately read-only: tapping it only locates the quoted message when a target exists.
 */
@Composable
internal fun QuotedMessagePreview(
    originalMessage: Message?,
    originalMessageId: String?,
    currentUserId: String,
    originalSenderName: String?,
    fontScale: Float = 1f,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentColor: Color? = null,
    accentColor: Color? = null,
    shape: Shape = RoundedCornerShape(Sizes.CornerMedium),
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    val preview = remember(originalMessage) { quotedMessagePreviewModel(originalMessage) }
    val resolvedContainerColor = containerColor ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val resolvedContentColor = contentColor ?: MaterialTheme.colorScheme.onSurface
    val resolvedAccentColor = accentColor ?: MaterialTheme.colorScheme.primary
    val isOriginalFromCurrentUser = originalMessage != null && currentUserId.isNotBlank() && originalMessage.senderId == currentUserId
    val senderName = resolveQuotedSenderName(
        message = originalMessage,
        currentUserId = currentUserId,
        providedName = originalSenderName,
        youLabel = stringResource(R.string.chat_reply_sender_you),
        unknownSenderLabel = stringResource(R.string.chat_reply_sender_unknown),
        unavailableLabel = stringResource(R.string.chat_reply_unavailable_title)
    )
    val label = quotedKindLabel(preview.kind)
    val detail = preview.excerpt?.takeIf(String::isNotBlank)
    val duration = preview.durationSeconds?.let { seconds ->
        stringResource(R.string.chat_reply_duration, seconds / 60, seconds % 60)
    }
    val fileSize = preview.fileSizeBytes?.let { bytes ->
        when {
            bytes < 1024L -> stringResource(R.string.chat_reply_size_bytes, bytes)
            bytes < 1024L * 1024L -> stringResource(R.string.chat_reply_size_kilobytes, bytes / 1024f)
            else -> stringResource(R.string.chat_reply_size_megabytes, bytes / (1024f * 1024f))
        }
    }
    val excerpt = when (preview.kind) {
        QuotedMessageKind.UNAVAILABLE -> stringResource(R.string.chat_reply_message_unavailable)
        QuotedMessageKind.DELETED -> stringResource(R.string.chat_reply_message_deleted)
        QuotedMessageKind.TEXT -> detail ?: label
        QuotedMessageKind.POLL -> detail ?: label
        else -> listOfNotNull(label, detail?.takeUnless { it.equals(label, ignoreCase = true) }, duration, fileSize)
            .joinToString(separator = " · ")
    }
    val accessibleDescription = stringResource(R.string.chat_reply_preview_accessibility, senderName, excerpt)
    val applicationContext = context.applicationContext
    val settingsRepository: SettingsRepository = remember(applicationContext) {
        SettingsRepositoryImpl.getInstance(applicationContext)
    }
    // produceState cancels the rules collector when this preview leaves or changes target.
    val canLoadThumbnail by produceState(
        !preview.thumbnailUrl.isNullOrBlank() && isOriginalFromCurrentUser,
        preview.thumbnailUrl,
        originalMessageId,
        isOriginalFromCurrentUser,
        settingsRepository
    ) {
        if (preview.thumbnailUrl.isNullOrBlank()) {
            value = false
        } else if (isOriginalFromCurrentUser) {
            value = true
        } else {
            settingsRepository.autoDownloadRules.collect { rules ->
                value = AutoDownloadManager.isAutoDownloadAllowed(applicationContext, rules, MediaType.PHOTO)
            }
        }
    }

    val clickable = onClick != null && !originalMessageId.isNullOrBlank()
    val rootModifier = if (clickable) {
        modifier
            .semantics { contentDescription = accessibleDescription }
            .clickable(role = Role.Button, onClick = { onClick?.invoke() })
    } else {
        modifier
    }

    Surface(
        modifier = rootModifier,
        color = resolvedContainerColor,
        contentColor = resolvedContentColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = Sizes.HeightDefault)
                .drawBehind {
                    val indicatorWidth = Sizes.BorderDefault.toPx()
                    drawRect(
                        color = resolvedAccentColor,
                        size = androidx.compose.ui.geometry.Size(indicatorWidth, size.height)
                    )
                }
                .padding(start = Spacing.Medium, end = Spacing.ExtraSmall, top = Spacing.ExtraSmall, bottom = Spacing.ExtraSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            val thumbnailUrl = preview.thumbnailUrl?.takeIf { canLoadThumbnail }
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(thumbnailUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(Sizes.IconMassive)
                        .clip(RoundedCornerShape(Sizes.CornerSmall))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(Sizes.IconMassive)
                        .clip(RoundedCornerShape(Sizes.CornerSmall))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = quotedKindIcon(preview.kind),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Sizes.IconDefault)
                    )
                }
            }

            val textModifier = if (trailingContent != null) Modifier.weight(1f) else Modifier.widthIn(max = Sizes.BubbleMaxWidth)
            Column(modifier = textModifier) {
                // My design decision (not in M3): one sender line and a two-line excerpt keep quotes compact.
                Text(
                    text = senderName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = MaterialTheme.typography.labelMedium.fontSize * fontScale,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = resolvedContentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = excerpt,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = MaterialTheme.typography.bodySmall.fontSize * fontScale
                    ),
                    color = resolvedContentColor.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (trailingContent != null) {
                trailingContent.invoke(this)
            }
        }
    }
}

@Composable
private fun quotedKindLabel(kind: QuotedMessageKind): String = when (kind) {
    QuotedMessageKind.UNAVAILABLE -> stringResource(R.string.chat_reply_unavailable_title)
    QuotedMessageKind.DELETED -> stringResource(R.string.chat_reply_message_deleted)
    QuotedMessageKind.TEXT -> stringResource(R.string.chat_reply_type_text)
    QuotedMessageKind.PHOTO -> stringResource(R.string.chat_reply_type_photo)
    QuotedMessageKind.VIDEO -> stringResource(R.string.chat_reply_type_video)
    QuotedMessageKind.VOICE -> stringResource(R.string.chat_reply_type_voice)
    QuotedMessageKind.AUDIO -> stringResource(R.string.chat_reply_type_audio)
    QuotedMessageKind.MEDIA -> stringResource(R.string.chat_reply_type_media)
    QuotedMessageKind.FILE -> stringResource(R.string.chat_reply_type_file)
    QuotedMessageKind.POLL -> stringResource(R.string.chat_reply_type_poll)
    QuotedMessageKind.CONTACT -> stringResource(R.string.chat_reply_type_contact)
    QuotedMessageKind.LOCATION -> stringResource(R.string.chat_reply_type_location)
    QuotedMessageKind.LIVE_LOCATION -> stringResource(R.string.chat_reply_type_live_location)
    QuotedMessageKind.GIF -> stringResource(R.string.chat_reply_type_gif)
    QuotedMessageKind.STICKER -> stringResource(R.string.chat_reply_type_sticker)
    QuotedMessageKind.LINK -> stringResource(R.string.chat_reply_type_link)
    QuotedMessageKind.MUSIC -> stringResource(R.string.chat_reply_type_music)
    QuotedMessageKind.SHARED_POST -> stringResource(R.string.chat_reply_type_post)
    QuotedMessageKind.STORY -> stringResource(R.string.chat_reply_type_story)
    QuotedMessageKind.EVENT -> stringResource(R.string.chat_reply_type_event)
    QuotedMessageKind.PRODUCT -> stringResource(R.string.chat_reply_type_product)
    QuotedMessageKind.PAYMENT -> stringResource(R.string.chat_reply_type_payment)
    QuotedMessageKind.MAP -> stringResource(R.string.chat_reply_type_map)
    QuotedMessageKind.CODE -> stringResource(R.string.chat_reply_type_code)
    QuotedMessageKind.CALL -> stringResource(R.string.chat_reply_type_call)
    QuotedMessageKind.SYSTEM -> stringResource(R.string.chat_reply_type_system)
    QuotedMessageKind.VIEW_ONCE -> stringResource(R.string.chat_reply_type_view_once)
}

private fun quotedKindIcon(kind: QuotedMessageKind): ImageVector = when (kind) {
    QuotedMessageKind.PHOTO, QuotedMessageKind.MEDIA -> Icons.Filled.Image
    QuotedMessageKind.VIDEO -> Icons.Filled.PlayCircle
    QuotedMessageKind.VOICE -> Icons.Filled.Mic
    QuotedMessageKind.AUDIO -> Icons.Filled.GraphicEq
    QuotedMessageKind.POLL -> Icons.Filled.Poll
    QuotedMessageKind.FILE -> Icons.AutoMirrored.Filled.InsertDriveFile
    QuotedMessageKind.CONTACT -> Icons.Filled.Person
    QuotedMessageKind.LOCATION, QuotedMessageKind.LIVE_LOCATION, QuotedMessageKind.MAP -> Icons.Filled.Place
    QuotedMessageKind.VIEW_ONCE -> Icons.Filled.Visibility
    else -> Icons.Filled.ChatBubbleOutline
}
