package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource

internal class ChatGroupRepository(
    private val dataSource: SupabaseChatDataSource
) {

    suspend fun createGroupChat(name: String, participantIds: List<String>, avatarUrl: String?): Result<String> = try {
        val chatId = dataSource.createGroupChat(name, participantIds, avatarUrl) ?: throw Exception("Failed to create group chat")
        Result.success(chatId)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error creating group chat", e)
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
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error adding group member", e)
        Result.failure(e)
    }

    suspend fun removeGroupMember(chatId: String, userId: String): Result<Unit> = try {
        dataSource.removeGroupMember(chatId, userId)
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error removing group member", e)
        Result.failure(e)
    }

    suspend fun promoteToAdmin(chatId: String, userId: String): Result<Unit> = try {
        dataSource.promoteToAdmin(chatId, userId)
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error promoting admin", e)
        Result.failure(e)
    }

    suspend fun demoteAdmin(chatId: String, userId: String): Result<Unit> = try {
        dataSource.demoteAdmin(chatId, userId)
        Result.success(Unit)
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error demoting admin", e)
        Result.failure(e)
    }

    suspend fun leaveGroup(chatId: String): Result<Unit> = try {
        dataSource.leaveGroup(chatId)
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
        with(com.synapse.social.studioasinc.shared.data.mapper.ChatMapper) {
            Result.success(info?.toDomain())
        }
    } catch (e: Exception) {
        io.github.aakira.napier.Napier.e("Error getting chat info", e)
        Result.failure(e)
    }
}
