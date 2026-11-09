package com.synapse.social.studioasinc.shared.core.util

import com.fleeksoft.ksoup.Ksoup
import com.synapse.social.studioasinc.shared.domain.model.LinkPreview
import io.ktor.http.Url

/**
 * Pure helpers for the link preview pipeline.
 *
 * Everything that turns a raw URL plus an HTML document into a [LinkPreview] lives here so the
 * rules are identical everywhere a preview is produced and so the logic is unit testable without
 * a database or a network stack.
 */
object LinkMetadataParser {

    /** Upper bound of HTML handed to the parser. */
    const val MAX_HTML_LENGTH: Int = 400_000

    /** Upper bound of the response body we are willing to download for metadata. */
    const val MAX_RESPONSE_BYTES: Long = 2L * 1024L * 1024L

    private const val SCHEME_HTTP = "http://"
    private const val SCHEME_HTTPS = "https://"

    private val TRAILING_PUNCTUATION =
        charArrayOf('.', ',', ';', ':', '!', '?', '"', '\'', '|', ']', '}')

    /**
     * Returns a fetchable absolute http(s) URL, or null when the input cannot be one. Punctuation
     * that commonly trails a URL inside prose, for example "see https://example.com.", is removed.
     */
    fun normalizeUrl(raw: String?): String? {
        val candidate = trimTrailingPunctuation(raw?.trim().orEmpty())
        if (candidate.isEmpty()) return null

        val lowered = candidate.lowercase()
        val withScheme = when {
            lowered.startsWith(SCHEME_HTTPS) || lowered.startsWith(SCHEME_HTTP) -> candidate
            lowered.startsWith("www.") -> SCHEME_HTTPS + candidate
            else -> return null
        }

        return try {
            val parsed = Url(withScheme)
            val host = parsed.host
            if (host.isBlank() || !host.contains('.')) return null
            parsed.toString()
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Strips punctuation a URL picked up from surrounding prose. Closing brackets are only dropped
     * while unbalanced, so real URLs such as https://en.wikipedia.org/wiki/Foo_(bar) stay intact.
     */
    fun trimTrailingPunctuation(value: String): String {
        var result = value
        while (result.isNotEmpty()) {
            val last = result.last()
            val drop = TRAILING_PUNCTUATION.contains(last) ||
                (last == ')' && result.count { it == ')' } > result.count { it == '(' })
            if (!drop) break
            result = result.dropLast(1)
        }
        return result
    }

    /** Hostname without a leading www. prefix, or null when the URL is unusable. */
    fun domainOf(url: String): String? = try {
        Url(url).host.removePrefix("www.").takeIf { it.isNotBlank() }
    } catch (t: Throwable) {
        null
    }

    /** Best effort favicon location derived from the URL itself, so it costs no extra request. */
    fun faviconUrl(url: String): String? = try {
        val parsed = Url(url)
        if (parsed.host.isBlank()) null else parsed.protocol.name + "://" + parsed.host + "/favicon.ico"
    } catch (t: Throwable) {
        null
    }

    /** Resolves a possibly relative reference, such as og:image or an icon href, against baseUrl. */
    fun absoluteUrl(baseUrl: String, reference: String?): String? {
        val ref = reference?.trim().orEmpty()
        if (ref.isEmpty()) return null

        val lowered = ref.lowercase()
        if (lowered.startsWith(SCHEME_HTTP) || lowered.startsWith(SCHEME_HTTPS)) return ref

        val base = try {
            Url(baseUrl)
        } catch (t: Throwable) {
            return null
        }

        val protocol = base.protocol.name
        val port = base.port
        val isDefaultPort = (protocol == "https" && port == 443) || (protocol == "http" && port == 80)
        val origin = if (port <= 0 || isDefaultPort) {
            protocol + "://" + base.host
        } else {
            protocol + "://" + base.host + ":" + port
        }

        return when {
            ref.startsWith("//") -> protocol + ":" + ref
            ref.startsWith("/") -> origin + ref
            else -> {
                val directory = base.encodedPath.substringBeforeLast('/', "")
                origin + directory + "/" + ref
            }
        }
    }

    /** True when a preview actually carries something a card can draw. */
    fun hasRenderableMetadata(preview: LinkPreview?): Boolean = preview != null && (
        !preview.title.isNullOrBlank() ||
            !preview.description.isNullOrBlank() ||
            !preview.imageUrl.isNullOrBlank() ||
            !preview.siteName.isNullOrBlank()
        )

    /**
     * Extracts Open Graph, Twitter card and plain HTML metadata.
     *
     * Never throws: a document without usable metadata still yields a preview carrying the URL and
     * its domain, which is what lets the UI fall back to a domain identity instead of an empty
     * container.
     */
    fun parse(url: String, html: String?): LinkPreview {
        val domain = domainOf(url)
        if (html.isNullOrBlank()) return LinkPreview(url = url, domain = domain)

        return try {
            val document = Ksoup.parse(html.take(MAX_HTML_LENGTH))

            val title = document.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
                ?: document.select("meta[name=twitter:title]").attr("content").takeIf { it.isNotBlank() }
                ?: document.title().takeIf { it.isNotBlank() }

            val description = document.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
                ?: document.select("meta[name=twitter:description]").attr("content").takeIf { it.isNotBlank() }
                ?: document.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }

            val siteName = document.select("meta[property=og:site_name]").attr("content").takeIf { it.isNotBlank() }
                ?: document.select("meta[name=application-name]").attr("content").takeIf { it.isNotBlank() }

            val iconHref = document.select("link")
                .firstOrNull { link ->
                    link.attr("rel").contains("icon", ignoreCase = true) && link.attr("href").isNotBlank()
                }
                ?.attr("href")
                ?.takeIf { href ->
                    href.endsWith(".png", true) || href.endsWith(".jpg", true) ||
                        href.endsWith(".jpeg", true) || href.endsWith(".webp", true)
                }

            val rawImage = document.select("meta[property=og:image:secure_url]").attr("content").takeIf { it.isNotBlank() }
                ?: document.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
                ?: document.select("meta[name=twitter:image]").attr("content").takeIf { it.isNotBlank() }
                ?: document.select("meta[name=twitter:image:src]").attr("content").takeIf { it.isNotBlank() }
                ?: iconHref

            LinkPreview(
                url = url,
                title = title?.trim()?.takeIf { it.isNotEmpty() },
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                imageUrl = absoluteUrl(url, rawImage),
                siteName = siteName?.trim()?.takeIf { it.isNotEmpty() },
                domain = domain
            )
        } catch (t: Throwable) {
            LinkPreview(url = url, domain = domain)
        }
    }
}
