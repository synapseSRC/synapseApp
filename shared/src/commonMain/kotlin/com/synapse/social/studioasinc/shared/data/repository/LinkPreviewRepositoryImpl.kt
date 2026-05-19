package com.synapse.social.studioasinc.shared.data.repository

import com.fleeksoft.ksoup.Ksoup
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.domain.model.LinkPreview
import com.synapse.social.studioasinc.shared.domain.repository.LinkPreviewRepository
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import kotlinx.coroutines.withContext

class LinkPreviewRepositoryImpl(
    private val database: StorageDatabase,
    private val httpClient: HttpClient
) : LinkPreviewRepository {

    override suspend fun getLinkPreview(url: String): LinkPreview? = withContext(AppDispatchers.IO) {
        try {
            // Check cache
            val cached = database.linkMetadataQueries.getLinkMetadata(url).executeAsOneOrNull()
            if (cached != null) {
                return@withContext LinkPreview(
                    url = cached.url,
                    title = cached.title,
                    description = cached.description,
                    imageUrl = cached.imageUrl,
                    domain = cached.domain
                )
            }

            // Fetch from network with a reasonable timeout and mobile user agent
            val response = httpClient.get(url) {
                header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
            }

            // Limit the amount of text we read to avoid OOM for massive pages
            // If the response doesn't have a content length, we might want to read in chunks,
            // but for simplicity we'll assume bodyAsText() is okay if we use a background thread.
            // Ideally we'd limit this to first 512KB.
            val html = response.bodyAsText()

            // Parse metadata using Ksoup
            val document = Ksoup.parse(html)

            val title = document.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
                ?: document.title().takeIf { it.isNotBlank() }

            val description = document.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
                ?: document.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }

            val imageUrl = document.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }

            val domain = try {
                Url(url).host.removePrefix("www.")
            } catch (e: Exception) {
                null
            }

            // Cache it
            database.linkMetadataQueries.insertLinkMetadata(
                url = url,
                title = title,
                description = description,
                imageUrl = imageUrl,
                domain = domain
            )

            LinkPreview(
                url = url,
                title = title,
                description = description,
                imageUrl = imageUrl,
                domain = domain
            )
        } catch (e: Exception) {
            Napier.e("Error fetching link preview for $url", e)
            null
        }
    }
}
