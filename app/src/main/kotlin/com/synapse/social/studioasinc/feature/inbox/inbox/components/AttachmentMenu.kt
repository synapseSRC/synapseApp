package com.synapse.social.studioasinc.feature.inbox.inbox.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.components.picker.PickedFile
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.settings.MediaUploadQuality

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewDialog(
    selectedFiles: List<PickedFile>,
    context: Context,
    onDismissRequest: () -> Unit,
    onSendMedia: (List<PickedFile>, String?, MediaUploadQuality) -> Unit
) {
    if (selectedFiles.isEmpty()) {
        LaunchedEffect(Unit) { onDismissRequest() }
        return
    }

    val mediaItems = remember { mutableStateListOf<PickedFile>().apply { addAll(selectedFiles) } }
    var activeIndex by remember { mutableIntStateOf(0) }
    var quality by remember { mutableStateOf(MediaUploadQuality.STANDARD) }
    var caption by remember { mutableStateOf("") }

    LaunchedEffect(mediaItems.size) {
        if (mediaItems.isEmpty()) {
            onDismissRequest()
        } else if (activeIndex >= mediaItems.size) {
            activeIndex = (mediaItems.size - 1).coerceAtLeast(0)
        }
    }

    if (mediaItems.isEmpty()) return

    val activeFile = mediaItems.getOrNull(activeIndex) ?: mediaItems.first()
    val activeMediaType = remember(activeFile) {
        when {
            activeFile.mimeType.startsWith("image/") -> "image"
            activeFile.mimeType.startsWith("video/") -> "video"
            activeFile.mimeType.startsWith("audio/") -> "audio"
            else -> "file"
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val cropImageLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
            if (result.isSuccessful) {
                result.uriContent?.let { croppedUri ->
                    val updated = activeFile.copy(uri = croppedUri)
                    if (activeIndex in mediaItems.indices) {
                        mediaItems[activeIndex] = updated
                    }
                }
            } else {
                val exception = result.error
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.error_crop_failed, exception?.message ?: ""),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (mediaItems.size > 1) {
                                "${activeIndex + 1} / ${mediaItems.size}"
                            } else {
                                stringResource(R.string.preview_title)
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = "Close Preview")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            quality = if (quality == MediaUploadQuality.STANDARD) MediaUploadQuality.HD else MediaUploadQuality.STANDARD
                            val toastText = if (quality == MediaUploadQuality.HD) {
                                context.getString(R.string.media_quality_hd_selected)
                            } else {
                                context.getString(R.string.media_quality_standard_selected)
                            }
                            android.widget.Toast.makeText(context, toastText, android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Surface(
                                shape = RoundedCornerShape(Sizes.CornerSmall),
                                color = if (quality == MediaUploadQuality.HD) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(Spacing.Tiny)
                            ) {
                                Text(
                                    text = if (quality == MediaUploadQuality.HD) stringResource(R.string.media_quality_hd) else stringResource(R.string.media_quality_standard),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (quality == MediaUploadQuality.HD) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall)
                                )
                            }
                        }

                        if (activeMediaType == "image") {
                            IconButton(onClick = {
                                cropImageLauncher.launch(
                                    CropImageContractOptions(
                                        uri = activeFile.uri,
                                        cropImageOptions = CropImageOptions().apply {
                                            guidelines = CropImageView.Guidelines.ON
                                            activityTitle = context.getString(R.string.title_edit_image)
                                            cropMenuCropButtonTitle = context.getString(R.string.action_save)
                                            showCropOverlay = true
                                            showProgressBar = true
                                        }
                                    )
                                )
                            }) {
                                Icon(Icons.Default.Crop, contentDescription = "Crop Image")
                            }
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { previewPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(previewPadding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (activeMediaType) {
                        "image" -> {
                            AsyncImage(
                                model = activeFile.uri,
                                contentDescription = "Image Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        "video" -> {
                            VideoPlayerBox(mediaUrl = activeFile.uri.toString())
                        }
                        "audio" -> {
                            VoiceMessagePlayer(mediaUrl = activeFile.uri.toString(), tintColor = MaterialTheme.colorScheme.primary, isFromMe = true)
                        }
                        else -> {
                            Icon(
                                Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = "File",
                                modifier = Modifier.size(Sizes.ShimmerWidthLarge),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (mediaItems.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(mediaItems, key = { _, item -> item.uri.toString() }) { index, file ->
                            val isSelected = index == activeIndex
                            Box(
                                modifier = Modifier
                                    .size(Sizes.AvatarLarge)
                                    .clip(RoundedCornerShape(Sizes.CornerMedium))
                                    .border(
                                        width = if (isSelected) Sizes.BorderSelected else Sizes.BorderThin,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(Sizes.CornerMedium)
                                    )
                                    .clickable { activeIndex = index }
                            ) {
                                AsyncImage(
                                    model = file.thumbnailUri ?: file.uri,
                                    contentDescription = file.fileName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(Spacing.Tiny)
                                        .size(Sizes.IconSmall)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(Spacing.Tiny)
                                        .size(Sizes.IconSmall)
                                        .background(MaterialTheme.colorScheme.error, CircleShape)
                                        .clickable {
                                            mediaItems.removeAt(index)
                                            if (activeIndex >= mediaItems.size) {
                                                activeIndex = (mediaItems.size - 1).coerceAtLeast(0)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove media",
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(Spacing.Small)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Medium)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = caption,
                        onValueChange = { caption = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.caption_hint)) },
                        maxLines = 4,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(Sizes.CornerExtraLarge)
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    BadgedBox(
                        badge = {
                            if (mediaItems.size > 1) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(mediaItems.size.toString())
                                }
                            }
                        }
                    ) {
                        FloatingActionButton(
                            onClick = {
                                onSendMedia(mediaItems.toList(), caption.takeIf { it.isNotBlank() }, quality)
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewDialog(
    selectedMediaUri: Uri,
    selectedMediaType: String,
    context: Context,
    onDismissRequest: () -> Unit,
    onSendMedia: (Uri, String, String?) -> Unit
) {
    val singleFile = remember(selectedMediaUri, selectedMediaType) {
        PickedFile(
            uri = selectedMediaUri,
            mimeType = when (selectedMediaType) {
                "image" -> "image/jpeg"
                "video" -> "video/mp4"
                "audio" -> "audio/mp4"
                else -> "application/octet-stream"
            },
            fileName = "attachment",
            size = 0L
        )
    }
    MediaPreviewDialog(
        selectedFiles = listOf(singleFile),
        context = context,
        onDismissRequest = onDismissRequest,
        onSendMedia = { files, caption, _ ->
            val first = files.firstOrNull()
            if (first != null) {
                val type = when {
                    first.mimeType.startsWith("image/") -> "image"
                    first.mimeType.startsWith("video/") -> "video"
                    first.mimeType.startsWith("audio/") -> "audio"
                    else -> "file"
                }
                onSendMedia(first.uri, type, caption)
            }
        }
    )
}
