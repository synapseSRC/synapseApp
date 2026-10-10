package com.synapse.social.studioasinc.data.local.database.settings

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

internal fun DataStore<Preferences>.safePreferencesFlow(tag: String = "SettingsDataStore"): Flow<Preferences> = this.data
    .catch { exception ->
        if (exception is IOException) {
            Log.e(tag, "Error reading preferences", exception)
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }
