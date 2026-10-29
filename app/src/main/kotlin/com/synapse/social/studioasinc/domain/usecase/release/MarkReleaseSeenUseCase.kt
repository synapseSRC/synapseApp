package com.synapse.social.studioasinc.domain.usecase.release

import com.synapse.social.studioasinc.domain.repository.AppReleaseRepository
import javax.inject.Inject

/**
 * Records that the release carrying a tag has been read, which is what clears the What's New
 * indicator. Callers only invoke it once the release is actually on screen, so the badge cannot be
 * dismissed by a request that the user never looked at.
 */
class MarkReleaseSeenUseCase @Inject constructor(
    private val repository: AppReleaseRepository
) {
    suspend operator fun invoke(tag: String) = repository.markSeen(tag)
}
