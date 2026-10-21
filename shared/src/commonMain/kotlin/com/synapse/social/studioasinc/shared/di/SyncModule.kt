package com.synapse.social.studioasinc.shared.di

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.datasource.ChatMessageDataSource
import com.synapse.social.studioasinc.shared.data.local.InMemorySyncStateStorage
import com.synapse.social.studioasinc.shared.data.local.SyncStateStorage
import com.synapse.social.studioasinc.shared.data.local.database.CachedMessageDao
import com.synapse.social.studioasinc.shared.data.sync.MessageEntitySyncerFactory
import com.synapse.social.studioasinc.shared.data.sync.NetworkMonitor
import com.synapse.social.studioasinc.shared.data.sync.SyncManager
import org.koin.dsl.module

/**
 * Koin module for the synchronization infrastructure.
 * SyncManager is the central coordinator; entity syncers are registered on creation.
 */
val syncModule = module {

    single<SyncStateStorage> { InMemorySyncStateStorage() }

    single {
        SyncManager(
            supabaseClient = SupabaseClient.client,
            syncStateStorage = get(),
            offlineActionRepository = getOrNull()
        )
    }

    // Wire up the message syncer and register it with SyncManager
    single {
        val syncManager: SyncManager = get()
        val messageDataSource: ChatMessageDataSource = get()
        val cachedMessageDao: CachedMessageDao = get()

        val messageSyncer = MessageEntitySyncerFactory.create(messageDataSource, cachedMessageDao)
        syncManager.registerSyncer("message", messageSyncer)
        syncManager.subscribeRealtime("message", "messages")

        syncManager
    }

    single { NetworkMonitor(syncManager = get()) }
}
