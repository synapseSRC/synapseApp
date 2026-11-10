package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.core.util.LinkMetadataParser
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import com.synapse.social.studioasinc.shared.domain.model.LinkPreview
import com.synapse.social.studioasinc.shared.domain.repository.LinkPreviewRepository
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Fetches and caches link metadata.
 *
 * Caching happens in two layers: an in-memory session cache that also remembers failures so the
 * same URL is never fetched twice while the app is running, and the persistent LinkMetadataEntity
 * table that survives restarts. Only previews that actually carry metadata are written to the
 * database, so a page without Open Graph data can no longer poison the cache for a later visit.
 */
class LinkPreviewRepositoryImpl(
    private val database: StorageDatabase,
    private val httpClient: HttpClient
) : LinkPreviewRepository {

    private val sessionCache = mutableMapOf<String, LinkPreview?>()
    private val sessionCacheMutex = Mutex()

    override suspend fun getLinkPreview(url: String): LinkPreview? = withContext(AppDispatchers.IO) {
        val normalized = LinkMetadataParser.normalizeUrl(url) ?: return@withContext null

        val (alreadyResolved, remembered) = sessionCacheMutex.withLock {
            if (sessionCache.containsKey(normalized)) {
                true to sessionCache[normalized]
            } else {
                false to null
            }
        }
        if (alreadyResolved) return@withContext remembered

        val stored = readFromDatabase(normalized)
        if (stored != null) {
            cacheInSession(normalized, stored)
            return@withContext stored
        }

        val fetched = fetchMetadata(normalized)
        cacheInSession(normalized, fetched)
        if (fetched != null && LinkMetadataParser.hasRenderableMetadata(fetched)) {
            writeToDatabase(fetched)
        }
        fetched
    }

    private suspend fun cacheInSession(url: String, preview: LinkPreview?) {
        sessionCacheMutex.withLock { sessionCache[url] = preview }
    }

    private fun readFromDatabase(url: String): LinkPreview? = try {
        database.linkMetadataQueries.getLinkMetadata(url).executeAsOneOrNull()
            ?.let { row ->
                LinkPreview(
                    url = row.url,
                    title = row.title,
                    description = row.description,
                    imageUrl = row.imageUrl,
                    domain = row.domain
                )
            }
            ?.takeIf { LinkMetadataParser.hasRenderableMetadata(it) }
    } catch (t: Throwable) {
        Napier.e("Failed to read cached link metadata for " + url, t)
        null
    }

    private fun writeToDatabase(preview: LinkPreview) {
        try {
            database.linkMetadataQueries.insertLinkMetadata(
                url = preview.url,
                title = preview.title,
                description = preview.description,
                imageUrl = preview.imageUrl,
                domain = preview.domain
            )
        } catch (t: Throwable) {
            Napier.e("Failed to cache link metadata for " + preview.url, t)
        }
    }

    private suspend fun fetchMetadata(url: String): LinkPreview? = try {
        val response = httpClient.get(url) {
            header(HttpHeaders.UserAgent, MOBILE_USER_AGENT)
            header(HttpHeaders.Accept, ACCEPTED_CONTENT)
        }

        when {
            !response.status.isSuccess() -> null
            isBinary(response) -> null
            declaresOversizedBody(response) -> null
            else -> LinkMetadataParser.parse(url, response.bodyAsText())
        }
    } catch (t: Throwable) {
        Napier.e("Error fetching link preview for " + url, t)
        null
    }

    private fun declaresOversizedBody(response: HttpResponse): Boolean {
        val declaredLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: return false
        return declaredLength > LinkMetadataParser.MAX_RESPONSE_BYTES
    }

    /** Pages that can never contain HTML metadata are skipped before the body is read. */
    private fun isBinary(response: HttpResponse): Boolean {
        val contentType = response.headers[HttpHeaders.ContentType]
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            ?: return false

        return contentType.startsWith("image/") ||
            contentType.startsWith("video/") ||
            contentType.startsWith("audio/") ||
            contentType.startsWith("font/") ||
            contentType == "application/pdf" ||
            contentType == "application/zip" ||
            contentType == "application/octet-stream"
    }

    private companion object {
        const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
        const val ACCEPTED_CONTENT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    }
}
