package com.synapse.social.studioasinc.domain.repository

import com.synapse.social.studioasinc.domain.model.release.AppRelease
import com.synapse.social.studioasinc.domain.model.release.ReleaseSnapshot
import kotlinx.coroutines.flow.Flow

/** Published releases of the app, and the local record of which one the user last read. */
interface AppReleaseRepository {

    /** Locally stored releases, newest-first; re-emits whenever a refresh rewrites them. */
    fun observeCachedReleases(): Flow<List<AppRelease>>

    /** Tag the user last opened, or `null` while this device has never read a release list. */
    fun observeLastSeenTag(): Flow<String?>

    /**
     * Releases from GitHub, falling back to the stored copy.
     *
     * [forceRefresh] bypasses the freshness window. A failed request that still has usable stored
     * releases is reported as a successful `Cache` snapshot, so the screen never blanks out while
     * offline; the failure is only surfaced when there is nothing to show.
     */
    suspend fun loadReleases(forceRefresh: Boolean): Result<ReleaseSnapshot>

    /** Records that the release carrying [tag] has been read. */
    suspend fun markSeen(tag: String)
}
