package com.synapse.social.studioasinc.domain.repository

import com.synapse.social.studioasinc.domain.model.ProfileFollowRequest
import com.synapse.social.studioasinc.domain.model.ProfileFollowStatus

interface ProfilePrivacyRepository {
    suspend fun lockProfile(userId: String, isLocked: Boolean): Result<Unit>
    suspend fun getFollowStatus(targetUid: String): Result<ProfileFollowStatus>
    suspend fun getPendingFollowRequests(): Result<List<ProfileFollowRequest>>
    suspend fun respondToFollowRequest(requesterUid: String, accept: Boolean): Result<Unit>
}
