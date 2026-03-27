package com.synapse.social.studioasinc.feature.inbox.inbox.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewDialog(
    selectedMediaUri: Uri,
    selectedMediaType: String,
    context: Context,
    onDismissRequest: () -> Unit,
    onSendMedia: (Uri, String, String?) -> Unit
) {
    var mediaCaption by remember { mutableStateOf("") }
    var currentMediaUri by remember { mutableStateOf(selectedMediaUri) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val cropImageLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
            if (result.isSuccessful) {
                result.uriContent?.let { uri ->
                    currentMediaUri = uri
                }
            } else {
                val exception = result.error
                android.widget.Toast.makeText(context, context.getString(R.string.error_crop_failed, exception?.message ?: ""), android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.preview_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = "Close Preview")
                        }
                    },
                    actions = {
                        if (selectedMediaType == "image") {
                            IconButton(onClick = {
                                cropImageLauncher.launch(
                                    CropImageContractOptions(
                                        uri = currentMediaUri,
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
                    when (selectedMediaType) {
                        "image" -> {
                            AsyncImage(
                                model = currentMediaUri,
                                contentDescription = "Image Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        "video" -> {
                            VideoPlayerBox(mediaUrl = currentMediaUri.toString())
                        }
                        "audio" -> {
                            AudioPlayer(mediaUrl = currentMediaUri.toString(), tintColor = MaterialTheme.colorScheme.primary)
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Medium)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = mediaCaption,
                        onValueChange = { mediaCaption = it },
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
                    FloatingActionButton(
                        onClick = {
                            onSendMedia(currentMediaUri, selectedMediaType, mediaCaption.takeIf { it.isNotBlank() })
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
