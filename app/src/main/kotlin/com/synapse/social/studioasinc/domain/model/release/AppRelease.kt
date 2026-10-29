package com.synapse.social.studioasinc.domain.model.release

/**
 * A published release of the app, reduced to what can be presented as changelog.
 *
 * [publishedAtMillis] is `null` when GitHub supplied a missing or unparseable date, so the screen
 * can omit it instead of inventing one.
 */
data class AppRelease(
    val tag: String,
    val title: String,
    val url: String,
    val publishedAtMillis: Long?,
    val groups: List<ReleaseChangeGroup>
)

/** Role a run of release notes plays, decided from the heading the author wrote. */
enum class ReleaseChangeHeading {
    NewFeature,
    Improvement,
    BugFix,
    Other
}

/**
 * Notes that sit under one heading of a release.
 *
 * [title] keeps the heading exactly as the author wrote it; it is blank when the release had no
 * headings at all. Known headings are re-labelled with localised text by the screen, unknown ones
 * are shown verbatim rather than being sorted into a category the author never chose.
 */
data class ReleaseChangeGroup(
    val heading: ReleaseChangeHeading,
    val title: String,
    val items: List<String>
)
