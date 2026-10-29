package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.sound.SoundPreferences
import kotlinx.coroutines.flow.Flow

/**
 * The user's sound-effect choices.
 *
 * [currentPreferences] exists beside the flow because the play path needs one answer now, inside a
 * transition, and would otherwise have to hold its own cache of the flow to avoid racing it.
 */
interface SoundSettingsRepository {
    val soundPreferences: Flow<SoundPreferences>

    suspend fun currentPreferences(): SoundPreferences

    suspend fun setMasterEnabled(enabled: Boolean)

    suspend fun setMessageTonesEnabled(enabled: Boolean)

    suspend fun setCallTonesEnabled(enabled: Boolean)
}
