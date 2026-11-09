package com.synapse.social.studioasinc.feature.shared.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.di.LinkPreviewEntryPoint
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.core.util.LinkMetadataParser
import com.synapse.social.studioasinc.shared.domain.model.LinkPreview
import com.synapse.social.studioasinc.shared.domain.usecase.GetLinkMetadataUseCase
import com.synapse.social.studioasinc.ui.components.ShimmerBox
import dagger.hilt.android.EntryPointAccessors
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lets a screen provide the shared link metadata use case. Providing it is optional: when no
 * provider is installed the card resolves the same singleton through [LinkPreviewEntryPoint], so
 * every screen renders identical previews without any per screen wiring.
 */
val LocalLinkMetadataUseCase = compositionLocalOf<GetLinkMetadataUseCase?> { null }

/** Standard Open Graph image ratio (1200x630). */
private const val PREVIEW_IMAGE_ASPECT_RATIO = 1.91f

@Composable
fun rememberLinkMetadataUseCase(explicit: GetLinkMetadataUseCase? = null): GetLinkMetadataUseCase? {
    val provided = LocalLinkMetadataUseCase.current
    val context = LocalContext.current
    return explicit ?: provided ?: remember(context) { resolveLinkMetadataUseCase(context) }
}

private fun resolveLinkMetadataUseCase(context: Context): GetLinkMetadataUseCase? = try {
    EntryPointAccessors
        .fromApplication(context.applicationContext, LinkPreviewEntryPoint::class.java)
        .linkMetadataUseCase()
} catch (t: Throwable) {
    Napier.e("Link preview use case is unavailable", t)
    null
}

/**
 * The single link preview card used by every surface that renders post content.
 *
 * The card is self sufficient: it resolves its own metadata use case, shows a loading skeleton
 * while metadata is on the way and, when a page has no usable metadata (or cannot be reached),
 * falls back to the site identity derived from the URL. It therefore never renders an empty
 * rounded container.
 */
@Composable
fun LinkPreviewCard(
    url: String,
    useCase: GetLinkMetadataUseCase?,
    onRemove: (() -> Unit)? = null,
    initialPreview: LinkPreview? = null,
    modifier: Modifier = Modifier
) {
    val sanitizedUrl = remember(url) { LinkMetadataParser.normalizeUrl(url) }
    if (sanitizedUrl == null) return

    val resolvedUseCase = rememberLinkMetadataUseCase(useCase)
    val uriHandler = LocalUriHandler.current

    val usableInitialPreview = remember(initialPreview) {
        initialPreview?.takeIf { LinkMetadataParser.hasRenderableMetadata(it) }
    }
    val fallbackPreview = remember(sanitizedUrl) {
        LinkPreview(url = sanitizedUrl, domain = LinkMetadataParser.domainOf(sanitizedUrl))
    }

    var preview by remember(sanitizedUrl) { mutableStateOf(usableInitialPreview ?: fallbackPreview) }
    var isLoading by remember(sanitizedUrl) { mutableStateOf(usableInitialPreview == null) }

    LaunchedEffect(sanitizedUrl, usableInitialPreview) {
        if (usableInitialPreview != null) {
            preview = usableInitialPreview
            isLoading = false
            return@LaunchedEffect
        }
        if (resolvedUseCase == null) {
            preview = fallbackPreview
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        val fetched = try {
            withContext(Dispatchers.IO) { resolvedUseCase(sanitizedUrl) }
        } catch (t: Throwable) {
            Napier.e("Failed to resolve link preview for " + sanitizedUrl, t)
            null
        }
        preview = fetched ?: fallbackPreview
        isLoading = false
    }

    if (isLoading) {
        LinkPreviewSkeleton(modifier = modifier)
        return
    }

    val current = preview ?: fallbackPreview
    val displayUrl = current.url.ifBlank { sanitizedUrl }
    val host = LinkMetadataParser.domainOf(displayUrl) ?: sanitizedUrl
    val siteLabel = current.siteName?.takeIf { it.isNotBlank() } ?: host
    val hasMetadata = LinkMetadataParser.hasRenderableMetadata(current)

    Card(
        shape = RoundedCornerShape(Sizes.CornerLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                try {
                    uriHandler.openUri(displayUrl)
                } catch (t: Throwable) {
                    Napier.e("Failed to open URI: " + displayUrl, t)
                }
            }
    ) {
        Box {
            Column {
                if (hasMetadata && !current.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = current.imageUrl,
                        contentDescription = current.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(PREVIEW_IMAGE_ASPECT_RATIO)
                    )
                }
                Column(modifier = Modifier.padding(Spacing.space12)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinkPreviewFavicon(faviconUrl = LinkMetadataParser.faviconUrl(displayUrl))
                        Spacer(modifier = Modifier.width(Spacing.space8))
                        Text(
                            text = siteLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!current.title.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(Spacing.space4))
                        Text(
                            text = current.title.orEmpty(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!current.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(Spacing.space4))
                        Text(
                            text = current.description.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (onRemove != null) {
                Surface(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.space8)
                        .size(Sizes.IconHuge),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = Sizes.ElevationMedium,
                    shadowElevation = Sizes.BorderDefault
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_remove),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(Sizes.IconDefault)
                        )
                    }
                }
            }
        }
    }
}

/** Favicon of the target site with a neutral globe behind it, so an unavailable icon still reads. */
@Composable
private fun LinkPreviewFavicon(faviconUrl: String?) {
    Box(
        modifier = Modifier
            .size(Sizes.IconSemiMedium)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Language,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Sizes.IconExtraSmall)
        )
        if (!faviconUrl.isNullOrBlank()) {
            AsyncImage(
                model = faviconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

/**
 * Compact shimmering placeholder. It reads unmistakably as a loading state and keeps the feed
 * layout stable, unlike a fixed height empty container.
 */
@Composable
private fun LinkPreviewSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Sizes.CornerLarge))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .padding(Spacing.space12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(
            modifier = Modifier.size(Sizes.IconLarge),
            shape = RoundedCornerShape(Sizes.CornerSmall)
        )
        Spacer(modifier = Modifier.width(Spacing.space12))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(Sizes.ShimmerLineHeight)
            )
            Spacer(modifier = Modifier.height(Spacing.space8))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(Sizes.ShimmerLineHeight)
            )
        }
    }
}
