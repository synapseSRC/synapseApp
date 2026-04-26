package com.synapse.social.studioasinc.desktop.di

import com.synapse.social.studioasinc.desktop.ui.DesktopAuthViewModel
import com.synapse.social.studioasinc.desktop.ui.DesktopChatViewModel
import com.synapse.social.studioasinc.shared.data.auth.SupabaseAuthenticationService
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import com.synapse.social.studioasinc.shared.data.repository.SupabaseAuthRepository
import com.synapse.social.studioasinc.shared.data.repository.SupabaseChatRepository
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.ChatRepository
import com.synapse.social.studioasinc.shared.domain.usecase.auth.SignInUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetConversationsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.GetMessagesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.chat.SendMessageUseCase
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import org.koin.dsl.module

val desktopModule = module {
    // Auth
    single<AuthRepository> { SupabaseAuthRepository(SupabaseClient.client) }
    single { SignInUseCase(get()) }
    factory { DesktopAuthViewModel(get()) }

    // Chat
    single { SupabaseChatDataSource(SupabaseClient.client) }

    single<ChatRepository> {
        SupabaseChatRepository(
            dataSource = get(),
            client = SupabaseClient.client,
            signalProtocolManager = null,
            mediaUploadRepository = get(),
            presenceRepository = getOrNull(),
            offlineActionRepository = getOrNull(),
            cachedMessageDao = getOrNull(),
            cachedConversationDao = getOrNull()
        )
    }

    single { GetConversationsUseCase(get()) }
    single { GetMessagesUseCase(get()) }
    single { SendMessageUseCase(get(), null) }

    factory { DesktopChatViewModel(get(), get(), get()) }
}
