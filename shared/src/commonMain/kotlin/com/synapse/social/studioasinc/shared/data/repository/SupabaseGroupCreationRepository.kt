package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.data.crypto.SignalProtocolManager
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import com.synapse.social.studioasinc.shared.data.util.initialsAvatarUrl
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupCreationOptions
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupInvitePreview
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinOutcome
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinRequest
import com.synapse.social.studioasinc.shared.domain.repository.GroupCreationRepository

/** Supabase adapter for group creation and revocable invitation operations. */
class SupabaseGroupCreationRepository(
    private val dataSource: SupabaseChatDataSource,
    signalProtocolManager: SignalProtocolManager? = null
) : GroupCreationRepository {

    private val cipher = GroupMetadataCipher(signalProtocolManager, dataSource)

    override suspend fun createGroupChat(name: String, participantIds: List<String>, options: GroupCreationOptions): Result<String> = try {
        val members = participantIds.distinct()
        val uid = dataSource.getCurrentUserId()
        var encName: String? = null
        var encDescription: String? = null
        var encAvatar: String? = null
        if (uid != null) {
            val roster = members + uid
            cipher.encryptField(name, roster, uid)?.let { encName = cipher.serialize(it) }
            options.description?.let { d -> cipher.encryptField(d, roster, uid)?.let { encDescription = cipher.serialize(it) } }
            // Same technique as profile creation: the first-letters API supplies the avatar image, so
            // the group always has a real avatar without any client-side initials rendering.
            cipher.encryptField(initialsAvatarUrl(name, fallbackSeed = "group"), roster, uid)?.let { encAvatar = cipher.serialize(it) }
        }
        dataSource.createGroupChat(name, members, avatarUrl = null, options = options, encName = encName, encDescription = encDescription, encAvatarUrl = encAvatar)
            ?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("Group creation returned no chat id."))
    } catch (error: Exception) { Result.failure(error) }

    override suspend fun getGroupInviteCode(chatId: String): Result<String> = try {
        dataSource.getGroupInviteCode(chatId)?.takeIf { it.isNotBlank() }?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("No invite link is stored for this group."))
    } catch (error: Exception) { Result.failure(error) }

    override suspend fun rotateGroupInviteCode(chatId: String): Result<String> = try {
        Result.success(dataSource.rotateGroupInviteCode(chatId))
    } catch (error: Exception) { Result.failure(error) }

    override suspend fun getPendingGroupJoinRequests(chatId: String): Result<List<GroupJoinRequest>> = try {
        Result.success(dataSource.getPendingGroupJoinRequests(chatId))
    } catch (error: Exception) { Result.failure(error) }

    override suspend fun approveGroupJoinRequest(requestId: String): Result<Unit> = try {
        dataSource.approveGroupJoinRequest(requestId)
        Result.success(Unit)
    } catch (error: Exception) { Result.failure(error) }

    override suspend fun declineGroupJoinRequest(requestId: String): Result<Unit> = try {
        dataSource.declineGroupJoinRequest(requestId)
        Result.success(Unit)
    } catch (error: Exception) { Result.failure(error) }

    override suspend fun resolveGroupInvite(inviteCode: String): Result<GroupInvitePreview?> = try {
        Result.success(dataSource.resolveGroupInvite(inviteCode))
    } catch (error: Exception) { Result.failure(error) }

    override suspend fun requestGroupJoinByInvite(inviteCode: String): Result<GroupJoinOutcome> = try {
        Result.success(dataSource.requestGroupJoinByInvite(inviteCode))
    } catch (error: Exception) { Result.failure(error) }
}
