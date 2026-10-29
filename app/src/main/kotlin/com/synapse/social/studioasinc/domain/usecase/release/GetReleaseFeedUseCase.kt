package com.synapse.social.studioasinc.domain.usecase.release

import com.synapse.social.studioasinc.domain.model.release.AppRelease
import com.synapse.social.studioasinc.domain.model.release.ReleaseFeed
import com.synapse.social.studioasinc.domain.model.release.ReleaseFeedSource
import com.synapse.social.studioasinc.domain.model.release.ReleaseSnapshot
import com.synapse.social.studioasinc.domain.model.release.ReleaseVersion
import com.synapse.social.studioasinc.domain.repository.AppReleaseRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Reads the published release list and decides what is still waiting to be read.
 *
 * A device that has never opened a release list is seeded silently on the first successful read:
 * releases published before an install must not be announced as new. The seed tag is the release
 * that matches the installed version when GitHub has one, otherwise the newest release on file.
 */
class GetReleaseFeedUseCase @Inject constructor(
    private val repository: AppReleaseRepository
) {

    /** Stored releases as they change, which is all the Settings indicator needs — no network. */
    fun observe(): Flow<ReleaseFeed> =
        combine(repository.observeCachedReleases(), repository.observeLastSeenTag()) { releases, lastSeenTag ->
            ReleaseFeed(
                releases = releases,
                unreadTag = lastSeenTag?.let { newestUnreadTag(releases, it) },
                source = ReleaseFeedSource.Cache
            )
        }

    /** Releases for the What's New screen, refreshed from GitHub when [forceRefresh] or out of date. */
    suspend operator fun invoke(
        installedVersion: String,
        forceRefresh: Boolean
    ): Result<ReleaseFeed> = repository.loadReleases(forceRefresh).fold(
        onSuccess = { snapshot -> Result.success(feedOf(snapshot, installedVersion)) },
        onFailure = { failure -> Result.failure(failure) }
    )

    private suspend fun feedOf(snapshot: ReleaseSnapshot, installedVersion: String): ReleaseFeed {
        val storedTag = repository.observeLastSeenTag().first() ?: return seed(snapshot, installedVersion)
        return ReleaseFeed(
            releases = snapshot.releases,
            unreadTag = newestUnreadTag(snapshot.releases, storedTag),
            source = snapshot.source
        )
    }

    /** Records the seed tag and reports nothing as unread, so a fresh install sees no badge. */
    private suspend fun seed(snapshot: ReleaseSnapshot, installedVersion: String): ReleaseFeed {
        val seedTag = seedTagFor(snapshot.releases, installedVersion)
        if (seedTag != null) repository.markSeen(seedTag)
        return ReleaseFeed(
            releases = snapshot.releases,
            unreadTag = null,
            source = snapshot.source
        )
    }

    private fun seedTagFor(releases: List<AppRelease>, installedVersion: String): String? {
        val installed = ReleaseVersion.of(installedVersion)
        if (installed != null) {
            releases.firstOrNull { candidate -> versionOf(candidate)?.let { it <= installed } == true }
                ?.let { return it.tag }
        }
        return releases.firstOrNull()?.tag
    }

    /** Newest-first list, so the first entry newer than the read tag is the one worth flagging. */
    private fun newestUnreadTag(releases: List<AppRelease>, lastSeenTag: String): String? {
        val seen = ReleaseVersion.of(lastSeenTag) ?: return null
        return releases.firstOrNull { candidate -> versionOf(candidate)?.let { it > seen } == true }?.tag
    }

    private fun versionOf(release: AppRelease): ReleaseVersion? = ReleaseVersion.of(release.tag)
}
