package com.synapse.social.studioasinc.feature.post.postdetail.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun CommentInput(
    onSend: (String, Uri?) -> Unit,
    modifier: Modifier = Modifier,
    initialValue: String = "",
    userAvatarUrl: String? = null,
    replyToParticipants: List<String> = emptyList(),
    onReplyingToClick: (() -> Unit)? = null,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    var isSending by remember { mutableStateOf(false) }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedMediaUri = uri
        }
    }

    val canSend = (text.isNotBlank() || selectedMediaUri != null) && !isSending

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            thickness = Sizes.BorderHairline
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
            verticalAlignment = Alignment.Bottom
        ) {
            com.synapse.social.studioasinc.ui.components.CircularAvatar(
                imageUrl = userAvatarUrl,
                contentDescription = "My Avatar",
                size = Sizes.AvatarSmall
            )

            Spacer(modifier = Modifier.width(Spacing.SmallMedium))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                if (replyToParticipants.isNotEmpty()) {
                    val maxShown = 3
                    val shown = replyToParticipants.take(maxShown)
                    val overflow = replyToParticipants.size - shown.size
                    val label = buildString {
                        append("Replying to ")
                        shown.forEachIndexed { i, username ->
                            if (i > 0) append(if (i == shown.lastIndex && overflow == 0) " and " else ", ")
                            append("@$username")
                        }
                        if (overflow > 0) append(" and $overflow other${if (overflow > 1) "s" else ""}")
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = if (onReplyingToClick != null) Modifier.clickable { onReplyingToClick() } else Modifier
                    )
                }

                // Pill-shaped input container
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(
                        Sizes.BorderHairline,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        if (selectedMediaUri != null) {
                            Box(
                                modifier = Modifier
                                    .padding(start = Spacing.Medium, end = Spacing.Medium, top = Spacing.SmallMedium)
                                    .size(Sizes.MediaPreviewSmall)
                                    .clip(RoundedCornerShape(Sizes.CornerDefault))
                                    .border(Sizes.BorderThin, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Sizes.CornerDefault))
                            ) {
                                AsyncImage(
                                    model = selectedMediaUri,
                                    contentDescription = "Selected media",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(Spacing.Small)
                                        .size(Sizes.IconLarge)
                                        .clickable { selectedMediaUri = null }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove media",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(Spacing.ExtraSmall)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isSending
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = "Add media",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(Sizes.IconDefault)
                                )
                            }

                            TextField(
                                value = text,
                                onValueChange = { text = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                placeholder = {
                                    Text(
                                        "Post your reply",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                maxLines = 6,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                enabled = !isSending
                            )

                            IconButton(
                                onClick = {
                                    if (canSend) {
                                        isSending = true
                                        onSend(text, selectedMediaUri)
                                        text = ""
                                        selectedMediaUri = null
                                        isSending = false
                                    }
                                },
                                enabled = canSend
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send reply",
                                    tint = if (canSend) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(Sizes.IconDefault)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
