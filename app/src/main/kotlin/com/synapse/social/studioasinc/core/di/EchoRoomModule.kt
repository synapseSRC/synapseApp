package com.synapse.social.studioasinc.core.di

import android.content.Context
import com.synapse.social.studioasinc.data.echo.EchoForegroundServiceHold
import com.synapse.social.studioasinc.data.echo.EchoMediaHold
import com.synapse.social.studioasinc.data.echo.audio.LiveKitEchoAudioEngine
import com.synapse.social.studioasinc.shared.data.datasource.EchoRoomRealtimeDataSource
import com.synapse.social.studioasinc.shared.data.repository.SupabaseEchoRoomRepository
import com.synapse.social.studioasinc.shared.data.repository.SupabaseEchoTokenRepository
import com.synapse.social.studioasinc.shared.domain.repository.EchoRoomRepository
import com.synapse.social.studioasinc.shared.domain.repository.EchoTokenRepository
import com.synapse.social.studioasinc.shared.domain.service.EchoAudioEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import javax.inject.Singleton

/**
 * Backend half of Echo Rooms.
 *
 * The repository is stateless — every read is an RPC and every change is announced through the
 * realtime data source — but the data source is a singleton so a room screen that recomposes does
 * not open a second channel to the same room.
 */
@Module
@InstallIn(SingletonComponent::class)
object EchoRoomModule {

    @Provides
    @Singleton
    fun provideEchoRoomRealtimeDataSource(
        client: SupabaseClientType
    ): EchoRoomRealtimeDataSource = EchoRoomRealtimeDataSource(client)

    @Provides
    @Singleton
    fun provideEchoRoomRepository(
        client: SupabaseClientType,
        realtime: EchoRoomRealtimeDataSource
    ): EchoRoomRepository = SupabaseEchoRoomRepository(client, realtime)

    @Provides
    @Singleton
    fun provideEchoTokenRepository(client: SupabaseClientType): EchoTokenRepository =
        SupabaseEchoTokenRepository(client)

    @Provides
    @Singleton
    fun provideEchoAudioEngine(engine: LiveKitEchoAudioEngine): EchoAudioEngine = engine

    @Provides
    @Singleton
    fun provideEchoMediaHold(@ApplicationContext context: Context): EchoMediaHold =
        EchoForegroundServiceHold(context)
}
