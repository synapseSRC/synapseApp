package com.synapse.social.studioasinc.domain.model.release

import java.net.URI

/**
 * Reduces a GitHub release body to heading-grouped change lines.
 *
 * Only the markdown a changelog realistically uses is understood: ATX headings, bullet and ordered
 * lists, task-list checkboxes, quotes, table rows and soft-wrapped paragraphs. Inline `**bold**` and
 * `` `code` `` survive because the screen renders them; images, HTML tags, reference links and code
 * fences are dropped, and a link is kept only when it points at a plain http(s) address, so release
 * notes cannot deliver markup or `javascript:` links to the UI.
 */
object ReleaseNotesParser {

    private val htmlComment = Regex("<!--[\\s\\S]*?-->")
    private val htmlTag = Regex("</?[a-zA-Z][^>]*>")
    private val image = Regex("!\\[[^]]*]\\([^)]*\\)")
    private val referenceLink = Regex("\\[([^]]+)]\\[[^]]*]")
    private val inlineLink = Regex("\\[([^]]+)]\\(([^)]*)\\)")
    private val autolink = Regex("<((?:https?|ftp)://[^>\\s]+)>", RegexOption.IGNORE_CASE)
    private val heading = Regex("^\\s{0,3}#{1,6}\\s*(.+)$")
    private val boldHeading = Regex("^\\s*\\*\\*(.+?)\\*\\*\\s*[:：]?\\s*$")
    private val bullet = Regex("^\\s*[-*+]\\s*(.*)$")
    private val ordered = Regex("^\\s*\\d+[.)]\\s*(.*)$")
    private val quote = Regex("^\\s*>+\\s*(.*)$")
    private val checkbox = Regex("^\\[[ xX]]\\s*(.*)$")
    private val codeFence = Regex("^\\s*(?:```|~~~)")
    private val horizontalRule = Regex("^\\s*(?:[-*_]\\s*){3,}$")
    private val tableRow = Regex("^\\s*\\|(.+)\\|\\s*$")
    private val tableDelimiter = Regex("^:?-+:?$")

    private val fixKeywords = listOf("fix", "bug", "resolv", "patch", "regress", "crash", "hotfix")
    private val featureKeywords = listOf("feature", "what's new", "whats new", "added", "addition", "launch")
    private val improvementKeywords =
        listOf("improv", "enhanc", "optimi", "performance", "polish", "refactor", "change", "update")

    /** Untitled group that collects notes written before any heading. */
    private const val UNTITLED_KEY = "other:"

    /** Structured notes for [body]; empty when the release has nothing presentable. */
    fun parse(body: String?): List<ReleaseChangeGroup> {
        if (body.isNullOrBlank()) return emptyList()
        val text = body.replace(htmlComment, "").replace("\r\n", "\n").replace('\r', '\n')

        val groups = LinkedHashMap<String, MutableGroup>()
        var activeKey: String? = null
        val paragraph = StringBuilder()
        var insideCodeFence = false

        fun append(item: String) {
            if (item.isBlank()) return
            val key = activeKey ?: UNTITLED_KEY.also { activeKey = it }
            groups.getOrPut(key) { MutableGroup(kindOf(key), titleOf(key)) }.items.add(item)
        }

        fun flushParagraph() {
            if (paragraph.isEmpty()) return
            append(inline(paragraph.toString()))
            paragraph.setLength(0)
        }

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trimEnd()
            if (codeFence.matches(line)) {
                flushParagraph()
                insideCodeFence = !insideCodeFence
                continue
            }
            if (insideCodeFence) continue

            when {
                line.isBlank() || horizontalRule.matches(line) -> flushParagraph()

                else -> {
                    val parsedHeading = matchHeading(line)
                    if (parsedHeading != null) {
                        flushParagraph()
                        val key = parsedHeading.title.toMutableKey(parsedHeading.kind)
                        activeKey = key
                        groups.getOrPut(key) { MutableGroup(parsedHeading.kind, parsedHeading.title) }
                        continue
                    }
                    val bulletText = matchListItem(line)
                    if (bulletText != null) {
                        flushParagraph()
                        append(inline(bulletText))
                        continue
                    }
                    val row = matchTableRow(line)
                    if (row != null) {
                        flushParagraph()
                        append(row)
                        continue
                    }
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append((quote.find(line)?.groupValues?.getOrNull(1) ?: line).trim())
                }
            }
        }
        flushParagraph()

        return groups.values
            .filter { it.items.isNotEmpty() }
            .map { ReleaseChangeGroup(it.kind, it.title, it.items.toList()) }
    }

    private class MutableGroup(
        val kind: ReleaseChangeHeading,
        val title: String,
        val items: MutableList<String> = mutableListOf()
    )

    private class Heading(val title: String, val kind: ReleaseChangeHeading)

    private fun matchHeading(line: String): Heading? {
        val raw = heading.find(line)?.groupValues?.getOrNull(1)
            ?: boldHeading.find(line)?.groupValues?.getOrNull(1)
            ?: return null
        val title = cleanHeading(raw)
        if (title.isEmpty()) return null
        return Heading(title, classify(title))
    }

    /** Text of a list line with its bullet and checkbox markers removed, or `null` for other lines. */
    private fun matchListItem(line: String): String? {
        val withoutQuote = quote.find(line)?.groupValues?.getOrNull(1) ?: line
        val marker = bullet.find(withoutQuote)?.groupValues?.getOrNull(1)
            ?: ordered.find(withoutQuote)?.groupValues?.getOrNull(1)
            ?: return null
        return checkbox.find(marker.trim())?.groupValues?.getOrNull(1) ?: marker
    }

    /**
     * A markdown table row flattened to one text line, or `null` for other lines.
     *
     * A whole table joined as a paragraph reads as a wall of pipes, so each row stands on its own
     * with its cells kept in order; the separator row has no content and comes back blank.
     */
    private fun matchTableRow(line: String): String? {
        val raw = tableRow.find(line)?.groupValues?.getOrNull(1) ?: return null
        val cells = raw.split('|')
            .map { cell -> cell.trim() }
            .filter { cell -> cell.isNotEmpty() }
        if (cells.isEmpty() || cells.all { cell -> tableDelimiter.matches(cell) }) return ""
        return cells.joinToString(" | ") { cell -> inline(cell) }
    }

    private fun cleanHeading(value: String): String = inline(value)
        .trim { !it.isLetterOrDigit() }
        .replace(Regex("\\s{2,}"), " ")
        .take(HEADING_MAX_LENGTH)

    /** Collapses a line to the safe inline subset the screen understands. */
    private fun inline(value: String): String = value
        .replace(htmlTag, " ")
        .replace(image, " ")
        .replace(autolink) { match -> safeLink(match.groupValues[1], match.groupValues[1]) }
        .replace(referenceLink) { match -> match.groupValues[1] }
        .replace(inlineLink) { match -> safeLink(match.groupValues[1], match.groupValues[2]) }
        .replace("\\|", "|")
        .replace(Regex("^[ \\t]+"), "")
        .replace(Regex("\\s{2,}"), " ")
        .trim()

    /** Keeps markdown link syntax only for safe destinations, otherwise the label alone. */
    private fun safeLink(label: String, destination: String): String =
        if (isWebLink(destination)) "[$label]($destination)" else inline(label)

    private fun isWebLink(value: String): Boolean = try {
        val uri = URI(value.trim())
        (uri.scheme.equals("https", ignoreCase = true) || uri.scheme.equals("http", ignoreCase = true)) &&
            !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null
    } catch (_: IllegalArgumentException) {
        false
    }

    private fun classify(title: String): ReleaseChangeHeading {
        val normalized = title.lowercase()
        return when {
            fixKeywords.any(normalized::contains) -> ReleaseChangeHeading.BugFix
            featureKeywords.any(normalized::contains) -> ReleaseChangeHeading.NewFeature
            improvementKeywords.any(normalized::contains) -> ReleaseChangeHeading.Improvement
            else -> ReleaseChangeHeading.Other
        }
    }

    /** Known headings merge when they repeat; free-text headings only merge when the text matches. */
    private fun String.toMutableKey(kind: ReleaseChangeHeading): String =
        if (kind == ReleaseChangeHeading.Other) "$UNTITLED_KEY${lowercase()}" else kind.name

    private fun kindOf(key: String): ReleaseChangeHeading =
        ReleaseChangeHeading.entries.firstOrNull { it.name == key } ?: ReleaseChangeHeading.Other

    private fun titleOf(key: String): String =
        if (key.startsWith(UNTITLED_KEY)) key.removePrefix(UNTITLED_KEY) else ""

    private const val HEADING_MAX_LENGTH = 80
}
