package com.synapse.social.studioasinc.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.releaseCacheDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "synapse_release_cache"
)

/**
 * Local copy of the GitHub release list and the tag the user last read.
 *
 * Kept in its own DataStore file rather than the user-settings one: a network payload is not a
 * preference, and it must not travel with settings backup or restore.
 */
@Singleton
class ReleaseCacheStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val store: DataStore<Preferences> = context.releaseCacheDataStore

    /** Raw list payload as GitHub returned it, or `null` while nothing has been fetched yet. */
    val payload: Flow<String?> = store.data.map { preferences ->
        preferences[KEY_PAYLOAD]?.takeIf { value -> value.isNotBlank() }
    }

    /** Tag the user last opened, or `null` while the device has never read a release list. */
    val lastSeenTag: Flow<String?> = store.data.map { preferences -> preferences[KEY_LAST_SEEN] }

    val fetchedAtMillis: Flow<Long> = store.data.map { preferences -> preferences[KEY_FETCHED_AT] ?: 0L }

    suspend fun readPayload(): String? = payload.first()

    suspend fun readFetchedAtMillis(): Long = fetchedAtMillis.first()

    /** Stores a successful response with the moment it arrived, which is what drives freshness. */
    suspend fun write(payload: String, fetchedAtMillis: Long) {
        store.edit { preferences ->
            preferences[KEY_PAYLOAD] = payload
            preferences[KEY_FETCHED_AT] = fetchedAtMillis
        }
    }

    suspend fun markSeen(tag: String) {
        store.edit { preferences -> preferences[KEY_LAST_SEEN] = tag }
    }

    private companion object {
        val KEY_PAYLOAD = stringPreferencesKey("release_payload")
        val KEY_FETCHED_AT = longPreferencesKey("release_fetched_at_millis")
        val KEY_LAST_SEEN = stringPreferencesKey("last_seen_release_tag")
    }
}
