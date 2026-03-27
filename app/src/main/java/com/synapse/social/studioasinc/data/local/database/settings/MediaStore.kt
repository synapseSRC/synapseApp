package com.synapse.social.studioasinc.data.local.database.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface MediaStore {
    val keepMediaDays: Flow<Int>
    val maxCacheSizeGB: Flow<Int>
    val autoDownloadRules: Flow<com.synapse.social.studioasinc.ui.settings.AutoDownloadRules>
    val mediaUploadQuality: Flow<com.synapse.social.studioasinc.ui.settings.MediaUploadQuality>
    val useLessDataCalls: Flow<Boolean>

    suspend fun setKeepMediaDays(days: Int)
    suspend fun setMaxCacheSizeGB(gb: Int)
    suspend fun setAutoDownloadRule(networkType: String, mediaTypes: Set<com.synapse.social.studioasinc.ui.settings.MediaType>)
    suspend fun setMediaUploadQuality(quality: com.synapse.social.studioasinc.ui.settings.MediaUploadQuality)
    suspend fun setUseLessDataCalls(enabled: Boolean)
}

class MediaStoreImpl(private val dataStore: DataStore<Preferences>) : MediaStore {
    override val keepMediaDays: Flow<Int> = dataStore.safePreferencesFlow().map { it[SettingsConstants.KEY_KEEP_MEDIA_DAYS] ?: SettingsConstants.DEFAULT_KEEP_MEDIA_DAYS }
    override val maxCacheSizeGB: Flow<Int> = dataStore.safePreferencesFlow().map { it[SettingsConstants.KEY_MAX_CACHE_SIZE_GB] ?: SettingsConstants.DEFAULT_MAX_CACHE_SIZE_GB }

    override suspend fun setKeepMediaDays(days: Int) { dataStore.edit { it[SettingsConstants.KEY_KEEP_MEDIA_DAYS] = days } }
    override suspend fun setMaxCacheSizeGB(gb: Int) { dataStore.edit { it[SettingsConstants.KEY_MAX_CACHE_SIZE_GB] = gb } }

    override val autoDownloadRules: Flow<com.synapse.social.studioasinc.ui.settings.AutoDownloadRules> = dataStore.safePreferencesFlow().map { preferences ->
        com.synapse.social.studioasinc.ui.settings.AutoDownloadRules(
            mobileData = preferences[SettingsConstants.KEY_AUTO_DOWNLOAD_MOBILE]?.mapNotNull {
                runCatching { com.synapse.social.studioasinc.ui.settings.MediaType.valueOf(it) }.getOrNull()
            }?.toSet() ?: setOf(com.synapse.social.studioasinc.ui.settings.MediaType.PHOTO),

            wifi = preferences[SettingsConstants.KEY_AUTO_DOWNLOAD_WIFI]?.mapNotNull {
                runCatching { com.synapse.social.studioasinc.ui.settings.MediaType.valueOf(it) }.getOrNull()
            }?.toSet() ?: SettingsConstants.DEFAULT_AUTO_DOWNLOAD_WIFI_TYPES,

            roaming = preferences[SettingsConstants.KEY_AUTO_DOWNLOAD_ROAMING]?.mapNotNull {
                runCatching { com.synapse.social.studioasinc.ui.settings.MediaType.valueOf(it) }.getOrNull()
            }?.toSet() ?: emptySet()
        )
    }

    override suspend fun setAutoDownloadRule(networkType: String, mediaTypes: Set<com.synapse.social.studioasinc.ui.settings.MediaType>) {
        dataStore.edit { preferences ->
            val key = when (networkType) {
                "mobile" -> SettingsConstants.KEY_AUTO_DOWNLOAD_MOBILE
                "wifi" -> SettingsConstants.KEY_AUTO_DOWNLOAD_WIFI
                "roaming" -> SettingsConstants.KEY_AUTO_DOWNLOAD_ROAMING
                else -> return@edit
            }
            preferences[key] = mediaTypes.map { it.name }.toSet()
        }
    }

    override val mediaUploadQuality: Flow<com.synapse.social.studioasinc.ui.settings.MediaUploadQuality> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_MEDIA_UPLOAD_QUALITY]?.let { value ->
            runCatching { com.synapse.social.studioasinc.ui.settings.MediaUploadQuality.valueOf(value) }
                .getOrDefault(com.synapse.social.studioasinc.ui.settings.MediaUploadQuality.STANDARD)
        } ?: com.synapse.social.studioasinc.ui.settings.MediaUploadQuality.STANDARD
    }

    override suspend fun setMediaUploadQuality(quality: com.synapse.social.studioasinc.ui.settings.MediaUploadQuality) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_MEDIA_UPLOAD_QUALITY] = quality.name
        }
    }

    override val useLessDataCalls: Flow<Boolean> = dataStore.safePreferencesFlow().map { preferences ->
        preferences[SettingsConstants.KEY_USE_LESS_DATA_CALLS] ?: false
    }

    override suspend fun setUseLessDataCalls(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsConstants.KEY_USE_LESS_DATA_CALLS] = enabled
        }
    }
}
