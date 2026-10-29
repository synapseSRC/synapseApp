package com.synapse.social.studioasinc.shared.data.sync

import com.synapse.social.studioasinc.shared.data.local.database.CachedConversationDao
import com.synapse.social.studioasinc.shared.data.local.database.CachedMessageDao
import com.synapse.social.studioasinc.shared.data.local.database.PostDao
import com.synapse.social.studioasinc.shared.data.local.database.StoryDao
import com.synapse.social.studioasinc.shared.domain.repository.OfflineActionRepository
import io.github.aakira.napier.Napier

/**
 * Handles local data cleanup on account switch and logout.
 *
 * Ensures data from one account never leaks into another session.
 * Each DAO call is wrapped independently so a single failure doesn't
 * prevent the rest of the cleanup from completing.
 */
object AccountSyncManager {
    private const val TAG = "AccountSyncManager"

    /**
     * Clear all user-scoped local data.
     * Call before signing in as a different account, or immediately after signOut().
     */
    suspend fun clearAllLocalData(
        cachedMessageDao: CachedMessageDao? = null,
        cachedConversationDao: CachedConversationDao? = null,
        postDao: PostDao? = null,
        storyDao: StoryDao? = null,
        offlineActionRepository: OfflineActionRepository? = null,
        syncManager: SyncManager? = null
    ) {
        Napier.d("Clearing all local data for account switch/logout", tag = TAG)

        safeRun("cachedMessages")     { cachedMessageDao?.deleteAll() }
        safeRun("cachedConversations") { cachedConversationDao?.deleteAll() }
        safeRun("posts")              { postDao?.deleteAll() }
        safeRun("stories")            { storyDao?.deleteAll() }

        safeRun("pendingActions") {
            offlineActionRepository?.getPendingActions()?.forEach { action ->
                offlineActionRepository.removeAction(action.id)
            }
        }

        // SyncManager.clearAllData() also clears its own entity syncers
        safeRun("syncManager") { syncManager?.clearAllData() }

        Napier.d("Local data cleared", tag = TAG)
    }

    private suspend inline fun safeRun(label: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Napier.e("Failed to clear $label during account cleanup", e, TAG)
        }
    }
}
