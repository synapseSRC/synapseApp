package com.synapse.social.studioasinc.domain.model.release

/**
 * Where a release list came from, so the screen can be honest about staleness.
 *
 * [CacheFallback] is a stored list handed back after GitHub could not be reached; the screen marks it
 * as needing a connection instead of presenting it as current.
 */
enum class ReleaseFeedSource { Network, Cache, CacheFallback }

/** Releases newest-first plus where they came from, as handed back by the repository. */
data class ReleaseSnapshot(
    val releases: List<AppRelease>,
    val source: ReleaseFeedSource
)

/**
 * Published releases together with what this device has already read.
 *
 * [unreadTag] is the newest release newer than the tag the user last opened, or `null` when nothing
 * is waiting for them; the Settings indicator keys off it.
 */
data class ReleaseFeed(
    val releases: List<AppRelease>,
    val unreadTag: String?,
    val source: ReleaseFeedSource
) {
    val latest: AppRelease? get() = releases.firstOrNull()
}

/** GitHub refused the releases request because the anonymous quota is spent. */
class ReleaseRateLimitedException : Exception("GitHub release rate limit exceeded")
