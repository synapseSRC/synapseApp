package com.synapse.social.studioasinc.presentation.editprofile.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.ImageLoader
import com.synapse.social.studioasinc.presentation.editprofile.UploadState
import com.synapse.social.studioasinc.ui.settings.SettingsColors
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun ProfileIdentityPreview(
    coverUrl: String?,
    avatarUrl: String?,
    nickname: String,
    username: String,
    bio: String,
    pendingAvatarUri: Uri? = null,
    pendingCoverUri: Uri? = null,
    avatarUploadState: UploadState = UploadState.Idle,
    coverUploadState: UploadState = UploadState.Idle,
    onCoverClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onRetryAvatarUpload: () -> Unit,
    onRetryCoverUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Sizes.CornerExtraLarge),
        color = SettingsColors.cardBackground,
        tonalElevation = Spacing.None
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = Spacing.Medium)
        ) {
            // Cover / Banner Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Sizes.HeightExtraLarge)
                    .clip(RoundedCornerShape(topStart = Sizes.CornerExtraLarge, topEnd = Sizes.CornerExtraLarge))
                    .clickable(
                        onClick = onCoverClick,
                        onClickLabel = stringResource(R.string.cd_edit_cover_photo)
                    )
            ) {
                val coverModel = when {
                    pendingCoverUri != null -> pendingCoverUri
                    !coverUrl.isNullOrBlank() -> ImageLoader.buildImageRequest(context, coverUrl)
                    else -> null
                }

                AsyncImage(
                    model = coverModel,
                    contentDescription = stringResource(R.string.cd_cover_photo),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = rememberVectorPainter(Icons.Filled.Image),
                    error = rememberVectorPainter(Icons.Filled.Image)
                )

                when (coverUploadState) {
                    is UploadState.Uploading -> {
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
                    is UploadState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = stringResource(R.string.upload_failed),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(Sizes.IconHuge)
                                )
                                if (coverUploadState.canRetry) {
                                    Spacer(modifier = Modifier.height(Spacing.Small))
                                    TextButton(
                                        onClick = onRetryCoverUpload,
                                        colors = ButtonDefaults.textButtonColors(
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
                                contentDescription = stringResource(R.string.cd_edit_cover_photo),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(Sizes.IconHuge)
                            )
                        }
                    }
                }
            }

            // Avatar Section (overlapping cover bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-36).dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(Sizes.AvatarProfile)
                        .clickable(
                            onClick = onAvatarClick,
                            onClickLabel = stringResource(R.string.cd_edit_profile_picture)
                        )
                ) {
                    Surface(
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(
                            Sizes.AvatarBorder,
                            MaterialTheme.colorScheme.surface
                        ),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val model = when {
                            pendingAvatarUri != null -> pendingAvatarUri
                            !avatarUrl.isNullOrBlank() -> ImageLoader.buildImageRequest(context, avatarUrl)
                            else -> null
                        }

                        if (model != null) {
                            AsyncImage(
                                model = model,
                                contentDescription = stringResource(R.string.cd_profile_picture),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
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

                    // Avatar Edit Badge on bottom right of Avatar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.cd_edit_profile_picture),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    when (avatarUploadState) {
                        is UploadState.Uploading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(Sizes.IconLarge),
                                    strokeWidth = Sizes.BorderDefault
                                )
                            }
                        }
                        is UploadState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.Error,
                                        contentDescription = stringResource(R.string.upload_failed),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(Sizes.IconDefault)
                                    )
                                    if (avatarUploadState.canRetry) {
                                        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                                        TextButton(
                                            onClick = onRetryAvatarUpload,
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            contentPadding = PaddingValues(
                                                horizontal = Spacing.Small,
                                                vertical = Spacing.Tiny
                                            )
                                        ) {
                                            Text(
                                                stringResource(R.string.action_retry),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        else -> { }
                    }
                }
            }

            // Display Name, Username, Bio Block (adjusted offset due to avatar)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = (-28).dp)
                    .padding(horizontal = Spacing.Medium)
            ) {
                Text(
                    text = nickname.ifBlank { stringResource(R.string.display_name) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (username.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                    Text(
                        text = "@$username",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                if (bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.Small))
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
