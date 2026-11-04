package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import com.synapse.social.studioasinc.shared.data.dto.chat.ChatDto
import com.synapse.social.studioasinc.shared.data.mapper.ChatMapper.toDomain
import com.synapse.social.studioasinc.shared.data.util.initialsAvatarUrl
import com.synapse.social.studioasinc.shared.domain.model.chat.ChatInfo

internal class ChatGroupRepository(
    private val dataSource: SupabaseChatDataSource,
    private val cipher: GroupMetadataCipher? = null
) {

    /**
     * Maps a [ChatDto] to the domain [ChatInfo], decrypting encrypted group metadata for the current
     * user and falling back to the legacy plaintext columns when no envelope is addressed to them.
     */
    private suspend fun decryptToDomain(dto: ChatDto): ChatInfo {
        val base = dto.toDomain()
        val c = cipher
        if (!dto.isGroup || c == null) return base
        val uid = dataSource.getCurrentUserId() ?: return base
        val chatId = dto.id
        val name = c.decryptField(c.parse(dto.encName), uid, chatId, "name") ?: base.name
        val description = c.decryptField(c.parse(dto.encDescription), uid, chatId, "description") ?: base.description
        // Groups always resolve to an image URL: the first-letters API replaces client-side initials rendering.
        val avatarUrl = c.decryptField(c.parse(dto.encAvatarUrl), uid, chatId, "avatar_url")
            ?: base.avatarUrl
            ?: name?.takeIf { it.isNotBlank() }?.let { initialsAvatarUrl(it, fallbackSeed = "group") }
        return base.copy(name = name, description = description, avatarUrl = avatarUrl)
    }

    /**
     * Best-effort map of participant uid -> display name, used to render group system messages
     * ("X joined the group", "Y is now an admin", ...). Returns an empty map when the roster can't
     * be read so callers fall back to a generic "A member" label instead of failing the mutation.
     */
    private suspend fun memberNames(chatId: String): Map<String, String> = try {
        dataSource.getGroupMembers(chatId).associate { (user, _) ->
            val name = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.nickname?.takeIf { it.isNotBlank() }
                ?: user.name?.takeIf { it.isNotBlank() }
                ?: user.username?.takeIf { it.isNotBlank() }
                ?: "A member"
            user.uid to name
        }
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error resolving group member names", e)
        emptyMap()
    }

    /**
     * Writes a plaintext group system message. System messages are never encrypted (they render as
     * centered pills) and are best-effort: a failure here must not roll back the completed mutation.
     */
    private suspend fun emitSystemMessage(chatId: String, content: String) {
        try {
            dataSource.insertSystemMessage(chatId, content)
        } catch (e: Exception) {
            io.github.aakira.napier.Napier.e("Error writing group system message", e)
        }
    }

    suspend fun createGroupChat(name: String, participantIds: List<String>, avatarUrl: String?): Result<String> = try {
        val c = cipher
        val uid = dataSource.getCurrentUserId()
        var encName: String? = null
        var encAvatar: String? = null
        if (c != null && uid != null) {
            val roster = participantIds + uid
            c.encryptField(name, roster, uid)?.let { encName = c.serialize(it) }
            val avatar = avatarUrl?.takeIf { it.isNotBlank() } ?: initialsAvatarUrl(name, fallbackSeed = "group")
            c.encryptField(avatar, roster, uid)?.let { encAvatar = c.serialize(it) }
        }
        val chatId = dataSource.createGroupChat(name, participantIds, avatarUrl, encName = encName, encAvatarUrl = encAvatar) ?: throw Exception("Failed to create group chat")
        Result.success(chatId)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error creating group chat", e)
        Result.failure(e)
    }

    suspend fun reportChat(chatId: String, reason: String): Result<Unit> = try {
        dataSource.reportChat(chatId, reason)
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error reporting chat", e)
        Result.failure(e)
    }

    suspend fun getParticipantIds(chatId: String): Result<List<String>> = try {
        val ids = dataSource.getParticipantIds(chatId)
        Result.success(ids)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error getting participant ids", e)
        Result.failure(e)
    }

    suspend fun getGroupMembers(chatId: String): Result<List<Pair<com.synapse.social.studioasinc.shared.domain.model.User, Boolean>>> = try {
        val members = dataSource.getGroupMembers(chatId)
        Result.success(members)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error getting group members", e)
        Result.failure(e)
    }

    suspend fun addGroupMembers(chatId: String, userIds: List<String>): Result<Unit> = try {
        dataSource.addGroupMembers(chatId, userIds)
        // Best-effort: re-fan-out encrypted metadata so newly added members receive envelopes.
        resyncMetadata(chatId)
        val names = memberNames(chatId)
        userIds.forEach { uid -> emitSystemMessage(chatId, "${names[uid] ?: "A member"} was added to the group") }
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error adding group member", e)
        Result.failure(e)
    }

    suspend fun removeGroupMember(chatId: String, userId: String): Result<Unit> = try {
        val name = memberNames(chatId)[userId] ?: "A member"
        dataSource.removeGroupMember(chatId, userId)
        emitSystemMessage(chatId, "$name was removed from the group")
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error removing group member", e)
        Result.failure(e)
    }

    suspend fun promoteToAdmin(chatId: String, userId: String): Result<Unit> = try {
        val name = memberNames(chatId)[userId] ?: "A member"
        dataSource.promoteToAdmin(chatId, userId)
        emitSystemMessage(chatId, "$name is now an admin")
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error promoting admin", e)
        Result.failure(e)
    }

    suspend fun demoteAdmin(chatId: String, userId: String): Result<Unit> = try {
        val name = memberNames(chatId)[userId] ?: "A member"
        dataSource.demoteAdmin(chatId, userId)
        emitSystemMessage(chatId, "$name is no longer an admin")
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error demoting admin", e)
        Result.failure(e)
    }

    suspend fun leaveGroup(chatId: String): Result<Unit> = try {
        val uid = dataSource.getCurrentUserId()
        val name = uid?.let { memberNames(chatId)[it] } ?: "A member"
        dataSource.leaveGroup(chatId)
        emitSystemMessage(chatId, "$name left the group")
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error leaving group", e)
        Result.failure(e)
    }

    suspend fun toggleOnlyAdminsCanMessage(chatId: String, enabled: Boolean): Result<Unit> = try {
        dataSource.toggleOnlyAdminsCanMessage(chatId, enabled)
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error toggling admins only message", e)
        Result.failure(e)
    }

    suspend fun getChatInfo(chatId: String): Result<com.synapse.social.studioasinc.shared.domain.model.chat.ChatInfo?> = try {
        val info = dataSource.getChatInfo(chatId)
        Result.success(info?.let { decryptToDomain(it) })
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error getting chat info", e)
        Result.failure(e)
    }

    suspend fun updateGroupInfo(chatId: String, name: String?, description: String?, avatarUrl: String?): Result<com.synapse.social.studioasinc.shared.domain.model.chat.ChatInfo> = try {
        val uid = dataSource.getCurrentUserId()
        var encNameStr: String? = null
        var encDescStr: String? = null
        var encAvatarStr: String? = null
        val c = cipher
        if (c != null && uid != null && (name != null || description != null || avatarUrl != null)) {
            val participants = dataSource.getParticipantIds(chatId)
            name?.let { v -> c.encryptField(v, participants, uid)?.let { f -> encNameStr = c.serialize(f) } }
            description?.let { v -> c.encryptField(v, participants, uid)?.let { f -> encDescStr = c.serialize(f) } }
            avatarUrl?.let { v -> c.encryptField(v, participants, uid)?.let { f -> encAvatarStr = c.serialize(f) } }
        }
        val dto = dataSource.updateGroupInfo(
            chatId,
            name = if (encNameStr != null) null else name,
            description = if (encDescStr != null) null else description,
            avatarUrl = if (encAvatarStr != null) null else avatarUrl,
            encName = encNameStr,
            encDescription = encDescStr,
            encAvatarUrl = encAvatarStr
        )
        Result.success(decryptToDomain(dto))
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error updating group info", e)
        Result.failure(e)
    }

    /**
     * Re-fans-out the group's encrypted metadata to the full current roster so that members who
     * joined after the last edit (and therefore have no envelope) can decrypt name/description/avatar.
     * Must be invoked by an existing member/admin who can already decrypt the current values.
     * No-op when encryption is unavailable or the chat is not a group.
     */
    suspend fun resyncMetadata(chatId: String): Result<Unit> {
        val c = cipher
        val uid = dataSource.getCurrentUserId()
        if (c == null || uid == null) return Result.success(Unit)
        return try {
            val dto = dataSource.getChatInfo(chatId) ?: return Result.success(Unit)
            if (!dto.isGroup) return Result.success(Unit)

            val currentName = c.decryptField(c.parse(dto.encName), uid, chatId, "name") ?: dto.name
            val currentDescription = c.decryptField(c.parse(dto.encDescription), uid, chatId, "description") ?: dto.description
            val currentAvatar = c.decryptField(c.parse(dto.encAvatarUrl), uid, chatId, "avatar_url")
                ?: dto.avatarUrl
                ?: currentName?.takeIf { it.isNotBlank() }?.let { initialsAvatarUrl(it, fallbackSeed = "group") }

            val participants = dataSource.getParticipantIds(chatId)
            val encName = currentName?.let { v -> c.encryptField(v, participants, uid)?.let { f -> c.serialize(f) } }
            val encDescription = currentDescription?.let { v -> c.encryptField(v, participants, uid)?.let { f -> c.serialize(f) } }
            val encAvatar = currentAvatar?.let { v -> c.encryptField(v, participants, uid)?.let { f -> c.serialize(f) } }

            if (encName == null && encDescription == null && encAvatar == null) return Result.success(Unit)

            dataSource.updateGroupInfo(
                chatId,
                name = null,
                description = null,
                avatarUrl = null,
                encName = encName,
                encDescription = encDescription,
                encAvatarUrl = encAvatar
            )
            Result.success(Unit)
        } catch (e: Exception) {
            io.github.aakira.napier.Napier.e("Error resyncing group metadata", e)
            Result.failure(e)
        }
    }

    suspend fun updateGroupPrivacy(chatId: String, allowMemberInvites: Boolean?, requireJoinApproval: Boolean?, allowMemberSettingsEdits: Boolean?): Result<Unit> = try {
        dataSource.updateGroupPrivacy(chatId, allowMemberInvites, requireJoinApproval, allowMemberSettingsEdits)
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error updating group privacy", e)
        Result.failure(e)
    }

    suspend fun deleteGroup(chatId: String): Result<Unit> = try {
        dataSource.deleteGroup(chatId)
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error deleting group", e)
        Result.failure(e)
    }
}
