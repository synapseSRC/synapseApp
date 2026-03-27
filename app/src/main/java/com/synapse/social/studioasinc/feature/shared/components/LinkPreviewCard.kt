package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.LinkPreview
import com.synapse.social.studioasinc.shared.domain.usecase.GetLinkMetadataUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.runtime.compositionLocalOf

val LocalLinkMetadataUseCase = compositionLocalOf<GetLinkMetadataUseCase?> { null }

@Composable
fun LinkPreviewCard(
    url: String,
    useCase: GetLinkMetadataUseCase,
    onRemove: (() -> Unit)? = null,
    initialPreview: LinkPreview? = null,
    modifier: Modifier = Modifier
) {
    var linkPreview by remember(url) { mutableStateOf<LinkPreview?>(initialPreview) }
    var isLoading by remember(url) { mutableStateOf(true) }
    var hasError by remember(url) { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(url, initialPreview) {
        // Skip fetch if metadata is already present in initialPreview
        if (initialPreview != null && (!initialPreview.title.isNullOrBlank() || !initialPreview.description.isNullOrBlank())) {
            linkPreview = initialPreview
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        hasError = false
        try {
            val preview = withContext(Dispatchers.IO) {
                useCase(url)
            }
            if (preview != null && (preview.title != null || preview.description != null)) {
                linkPreview = preview
            } else {
                hasError = true
            }
        } catch (e: Exception) {
            hasError = true
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(Sizes.CornerMedium))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(Sizes.IconLarge))
        }
    } else if (linkPreview != null) {
        val preview = linkPreview ?: return
        Card(
            shape = RoundedCornerShape(Sizes.CornerMedium),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        uriHandler.openUri(url)
                    } catch (e: Exception) {
                        // TODO: Log this exception with a proper logger
                    }
                }
        ) {
            Box {
                Column {
                    if (!preview.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = preview.imageUrl,
                            contentDescription = "Link Preview Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(Spacing.SmallMedium)) {
                        if (!preview.domain.isNullOrBlank()) {
                            Text(
                                text = preview.domain ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                        }
                        if (!preview.title.isNullOrBlank()) {
                            Text(
                                text = preview.title ?: "",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!preview.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                            Text(
                                text = preview.description ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (onRemove != null) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(Spacing.ExtraSmall)
                            .size(Sizes.IconLarge)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), shape = RoundedCornerShape(Sizes.CornerDefault))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Link Preview",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
