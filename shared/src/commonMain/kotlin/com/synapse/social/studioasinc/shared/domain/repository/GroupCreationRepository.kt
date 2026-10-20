package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.chat.GroupCreationOptions
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupInvitePreview
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinOutcome
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinRequest

interface GroupCreationRepository {
    suspend fun createGroupChat(name: String, participantIds: List<String>, options: GroupCreationOptions): Result<String>
    suspend fun getGroupInviteCode(chatId: String): Result<String>
    suspend fun rotateGroupInviteCode(chatId: String): Result<String>
    suspend fun getPendingGroupJoinRequests(chatId: String): Result<List<GroupJoinRequest>>
    suspend fun approveGroupJoinRequest(requestId: String): Result<Unit>
    suspend fun declineGroupJoinRequest(requestId: String): Result<Unit>
    suspend fun resolveGroupInvite(inviteCode: String): Result<GroupInvitePreview?>
    suspend fun requestGroupJoinByInvite(inviteCode: String): Result<GroupJoinOutcome>
}
