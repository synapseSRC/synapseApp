package com.synapse.social.studioasinc.presentation.editprofile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.Button
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.ImageLoader
import com.synapse.social.studioasinc.ui.settings.SettingsColors
import com.synapse.social.studioasinc.ui.settings.SettingsShapes
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun ProfileImageSection(
    coverUrl: String?,
    avatarUrl: String?,
    avatarUploadState: com.synapse.social.studioasinc.presentation.editprofile.UploadState,
    coverUploadState: com.synapse.social.studioasinc.presentation.editprofile.UploadState,
    onCoverClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onRetryAvatarUpload: () -> Unit,
    onRetryCoverUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Sizes.CornerExtraLarge),
        color = SettingsColors.cardBackgroundElevated,
        tonalElevation = 2.dp
    ) {
        Column {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Sizes.HeightExtraLarge)
                    .clip(RoundedCornerShape(topStart = Sizes.CornerExtraLarge, topEnd = Sizes.CornerExtraLarge))
                    .clickable(onClick = onCoverClick)
            ) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = if (coverUrl != null && coverUrl.isNotBlank()) {
                            ImageLoader.buildImageRequest(context, coverUrl)
                        } else {
                            null
                        },
                        contentDescription = "Cover photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = rememberVectorPainter(Icons.Filled.Image),
                        error = rememberVectorPainter(Icons.Filled.Image)
                    )


                    when (coverUploadState) {
                        is com.synapse.social.studioasinc.presentation.editprofile.UploadState.Uploading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(Sizes.IconHuge)
                                )
                            }
                        }
                        is com.synapse.social.studioasinc.presentation.editprofile.UploadState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.Error,
                                        contentDescription = "Upload failed",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(Sizes.IconHuge)
                                    )
                                    if (coverUploadState.canRetry) {
                                        Spacer(modifier = Modifier.height(Spacing.Small))
                                        androidx.compose.material3.TextButton(
                                            onClick = onRetryCoverUpload,
                                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Text(stringResource(R.string.action_retry))
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit cover photo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(Sizes.IconHuge)
                                )
                            }
                        }
                    }
            }


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-48).dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clickable(onClick = onAvatarClick)
                ) {
                    Surface(
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (avatarUrl != null && avatarUrl.isNotBlank()) {
                            android.util.Log.d("ProfileImageSection", "Loading avatar from URL: $avatarUrl")
                            AsyncImage(
                                model = ImageLoader.buildImageRequest(LocalContext.current, avatarUrl),
                                contentDescription = "Profile photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(Sizes.IconGiant)
                                )
                            }
                        }
                    }


                    when (avatarUploadState) {
                        is com.synapse.social.studioasinc.presentation.editprofile.UploadState.Uploading -> {
                            Box(
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(Sizes.IconLarge),
                                    strokeWidth = Sizes.BorderDefault
                                )
                            }
                        }
                        is com.synapse.social.studioasinc.presentation.editprofile.UploadState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.Error,
                                        contentDescription = "Upload failed",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(Sizes.IconDefault)
                                    )
                                    if (avatarUploadState.canRetry) {
                                        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                                        androidx.compose.material3.TextButton(
                                            onClick = onRetryAvatarUpload,
                                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            contentPadding = PaddingValues(horizontal = Spacing.Small, vertical = Spacing.Tiny)
                                        ) {
                                            Text(stringResource(R.string.action_retry), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                        else -> {

                        }
                    }
                }
            }
        }
    }
}
