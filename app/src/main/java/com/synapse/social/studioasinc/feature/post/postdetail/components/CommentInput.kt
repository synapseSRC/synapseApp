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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    replyingToUsername: String? = null,
    postAuthorUsername: String? = null,
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
            verticalAlignment = Alignment.Top
        ) {
            com.synapse.social.studioasinc.ui.components.CircularAvatar(
                imageUrl = userAvatarUrl,
                contentDescription = "My Avatar",
                size = Sizes.AvatarSmall
            )

            Spacer(modifier = Modifier.width(Spacing.SmallMedium))

            Column(modifier = Modifier.weight(1f)) {
                if (replyingToUsername != null) {
                    val label = if (postAuthorUsername != null && postAuthorUsername != replyingToUsername)
                        "Replying to @$postAuthorUsername and @$replyingToUsername"
                    else
                        "Replying to @$replyingToUsername"
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Spacing.Medium, bottom = Spacing.ExtraSmall, top = Spacing.ExtraSmall)
                    )
                }
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { 
                        Text(
                            "Post your reply",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    maxLines = 10,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    enabled = !isSending
                )

                if (selectedMediaUri != null) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
                            .size(160.dp)
                            .clip(RoundedCornerShape(Sizes.CornerDefault))
                            .border(
                                Sizes.BorderThin,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(Sizes.CornerDefault)
                            )
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            if ((text.isNotBlank() || selectedMediaUri != null) && !isSending) {
                                isSending = true
                                onSend(text, selectedMediaUri)
                                text = ""
                                selectedMediaUri = null
                                isSending = false
                            }
                        },
                        enabled = (text.isNotBlank() || selectedMediaUri != null) && !isSending,
                        contentPadding = PaddingValues(horizontal = Spacing.Medium, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(Sizes.CornerLarge),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            "Reply",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
