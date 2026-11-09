package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.domain.model.LinkPreview
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.utils.UrlUtils
import com.synapse.social.studioasinc.shared.domain.usecase.GetLinkMetadataUseCase

/** Maximum number of previews rendered for a single post. */
const val MAX_LINK_PREVIEWS_PER_POST = 3

/**
 * The single entry point for rendering the link previews of a piece of post content.
 *
 * Every screen that renders a post goes through this composable, which is why feed cards, the
 * detailed post screen, profiles, shared posts and chat bubbles all show exactly the same preview
 * with the same metadata, styling and behaviour. URLs are taken from the server provided previews
 * first and from the post text afterwards, so a preview keeps rendering even when the backend did
 * not store any metadata for the post.
 */
@Composable
fun LinkPreviewsSection(
    text: String?,
    serverPreviews: List<LinkPreview>? = null,
    useCase: GetLinkMetadataUseCase? = null,
    modifier: Modifier = Modifier
) {
    val resolvedUseCase = rememberLinkMetadataUseCase(useCase)

    val urls = remember(text, serverPreviews) {
        val fromServer = serverPreviews.orEmpty().map { it.url }
        (fromServer + UrlUtils.extractUrls(text))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_LINK_PREVIEWS_PER_POST)
    }

    if (urls.isEmpty()) return

    val metadataByUrl = remember(serverPreviews) {
        serverPreviews.orEmpty()
            .filter { it.url.isNotBlank() }
            .associateBy { it.url.trim() }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.space8)
    ) {
        urls.forEach { url ->
            LinkPreviewCard(
                url = url,
                useCase = resolvedUseCase,
                initialPreview = metadataByUrl[url]?.toSharedLinkPreview(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** The post model lives in the app module while the metadata pipeline lives in shared. */
private fun LinkPreview.toSharedLinkPreview() =
    com.synapse.social.studioasinc.shared.domain.model.LinkPreview(
        url = url,
        title = title,
        description = description,
        imageUrl = imageUrl,
        siteName = siteName,
        domain = domain
    )
