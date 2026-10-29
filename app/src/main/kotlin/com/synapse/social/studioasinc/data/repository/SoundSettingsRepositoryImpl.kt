package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.local.database.SettingsDataStore
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundPreferences
import com.synapse.social.studioasinc.shared.domain.repository.SoundSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Reads and writes the sound-effect switches through the app's preference store.
 *
 * [currentPreferences] is a single read rather than a cache on purpose: DataStore holds the loaded
 * preferences in memory after the first read, so the play path can ask for the truth every time and
 * still not touch disk. A cache here would be one more thing that can disagree with the Settings screen.
 */
@Singleton
class SoundSettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SoundSettingsRepository {

    override val soundPreferences: Flow<SoundPreferences> = settingsDataStore.soundPreferences

    override suspend fun currentPreferences(): SoundPreferences =
        settingsDataStore.soundPreferences.first()

    override suspend fun setMasterEnabled(enabled: Boolean) =
        settingsDataStore.setSoundMasterEnabled(enabled)

    override suspend fun setMessageTonesEnabled(enabled: Boolean) =
        settingsDataStore.setMessageTonesEnabled(enabled)

    override suspend fun setCallTonesEnabled(enabled: Boolean) =
        settingsDataStore.setCallTonesEnabled(enabled)
}
