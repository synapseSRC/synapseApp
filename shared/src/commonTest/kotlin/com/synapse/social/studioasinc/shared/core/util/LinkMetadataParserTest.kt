package com.synapse.social.studioasinc.shared.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinkMetadataParserTest {

    @Test
    fun acceptsAbsoluteAndWwwUrlsOnly() {
        assertNotNull(LinkMetadataParser.normalizeUrl("https://example.com/article"))
        assertNotNull(LinkMetadataParser.normalizeUrl("www.example.com"))
        assertNull(LinkMetadataParser.normalizeUrl("example.com"))
        assertNull(LinkMetadataParser.normalizeUrl("ftp://example.com"))
        assertNull(LinkMetadataParser.normalizeUrl("just some prose"))
        assertNull(LinkMetadataParser.normalizeUrl("   "))
        assertNull(LinkMetadataParser.normalizeUrl(null))
    }

    @Test
    fun upgradesBareWwwUrlsToHttps() {
        val normalized = assertNotNull(LinkMetadataParser.normalizeUrl("www.example.com"))
        assertTrue(normalized.startsWith("https://www.example.com"))
    }

    @Test
    fun trimsProsePunctuationButKeepsBalancedBrackets() {
        assertEquals("https://example.com", LinkMetadataParser.trimTrailingPunctuation("https://example.com."))
        assertEquals("https://example.com", LinkMetadataParser.trimTrailingPunctuation("https://example.com)."))
        assertEquals("https://example.com", LinkMetadataParser.trimTrailingPunctuation("https://example.com,"))
        assertEquals(
            "https://en.wikipedia.org/wiki/Foo_(bar)",
            LinkMetadataParser.trimTrailingPunctuation("https://en.wikipedia.org/wiki/Foo_(bar)")
        )
        assertEquals(
            "https://en.wikipedia.org/wiki/Foo_(bar)",
            LinkMetadataParser.trimTrailingPunctuation("https://en.wikipedia.org/wiki/Foo_(bar))")
        )
    }

    @Test
    fun derivesDomainAndFaviconFromTheUrl() {
        assertEquals("example.com", LinkMetadataParser.domainOf("https://www.example.com/a/b"))
        assertEquals("blog.example.com", LinkMetadataParser.domainOf("https://blog.example.com/post"))
        assertNull(LinkMetadataParser.domainOf("not a url"))
        assertEquals("https://www.example.com/favicon.ico", LinkMetadataParser.faviconUrl("https://www.example.com/a/b"))
        assertNull(LinkMetadataParser.faviconUrl("not a url"))
    }

    @Test
    fun resolvesRelativeImageReferences() {
        val base = "https://example.com/posts/one"
        assertEquals("https://cdn.example.com/a.png", LinkMetadataParser.absoluteUrl(base, "https://cdn.example.com/a.png"))
        assertEquals("https://cdn.example.com/a.png", LinkMetadataParser.absoluteUrl(base, "//cdn.example.com/a.png"))
        assertEquals("https://example.com/static/a.png", LinkMetadataParser.absoluteUrl(base, "/static/a.png"))
        assertEquals("https://example.com/posts/a.png", LinkMetadataParser.absoluteUrl(base, "a.png"))
        assertNull(LinkMetadataParser.absoluteUrl(base, "   "))
        assertNull(LinkMetadataParser.absoluteUrl(base, null))
    }

    @Test
    fun parsesCompleteOpenGraphMetadata() {
        val html = """
            <html>
            <head>
            <meta property="og:title" content="Synapse Release Notes" />
            <meta property="og:description" content="What changed in v1.0.2" />
            <meta property="og:site_name" content="Synapse" />
            <meta property="og:image" content="https://cdn.example.com/cover.png" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val preview = LinkMetadataParser.parse("https://example.com/releases/1", html)

        assertEquals("Synapse Release Notes", preview.title)
        assertEquals("What changed in v1.0.2", preview.description)
        assertEquals("Synapse", preview.siteName)
        assertEquals("https://cdn.example.com/cover.png", preview.imageUrl)
        assertEquals("example.com", preview.domain)
        assertTrue(LinkMetadataParser.hasRenderableMetadata(preview))
    }

    @Test
    fun fallsBackToTwitterCardThenPlainHtml() {
        val html = """
            <html>
            <head>
            <title>Plain page title</title>
            <meta name="description" content="Plain description" />
            <meta name="twitter:image" content="/images/twitter.png" />
            </head>
            </html>
        """.trimIndent()

        val preview = LinkMetadataParser.parse("https://example.com/blog/post", html)

        assertEquals("Plain page title", preview.title)
        assertEquals("Plain description", preview.description)
        assertEquals("https://example.com/images/twitter.png", preview.imageUrl)
    }

    @Test
    fun keepsPreviewUsableWhenMetadataIsMissing() {
        val html = """
            <html>
            <head></head>
            <body>nothing to describe</body>
            </html>
        """.trimIndent()

        val preview = LinkMetadataParser.parse("https://example.com/post", html)

        assertNull(preview.title)
        assertNull(preview.description)
        assertNull(preview.imageUrl)
        assertEquals("example.com", preview.domain)
        assertEquals("https://example.com/post", preview.url)
        assertFalse(LinkMetadataParser.hasRenderableMetadata(preview))
    }

    @Test
    fun neverThrowsOnBrokenOrBlankDocuments() {
        val broken = """
            <html><head><meta property=broken><body><div>
        """.trimIndent()

        assertEquals("example.com", LinkMetadataParser.parse("https://example.com", broken).domain)
        assertEquals("example.com", LinkMetadataParser.parse("https://example.com", null).domain)
        assertEquals("example.com", LinkMetadataParser.parse("https://example.com", "").domain)
        assertEquals("https://example.com", LinkMetadataParser.parse("https://example.com", null).url)
    }

    @Test
    fun renderableMetadataRequiresAtLeastOneField() {
        assertFalse(
            LinkMetadataParser.hasRenderableMetadata(
                com.synapse.social.studioasinc.shared.domain.model.LinkPreview(url = "https://example.com")
            )
        )
        assertTrue(
            LinkMetadataParser.hasRenderableMetadata(
                com.synapse.social.studioasinc.shared.domain.model.LinkPreview(url = "https://example.com", title = "T")
            )
        )
        assertTrue(
            LinkMetadataParser.hasRenderableMetadata(
                com.synapse.social.studioasinc.shared.domain.model.LinkPreview(url = "https://example.com", imageUrl = "https://example.com/a.png")
            )
        )
        assertFalse(LinkMetadataParser.hasRenderableMetadata(null))
    }
}
