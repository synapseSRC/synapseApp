package com.synapse.social.studioasinc.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import com.synapse.social.studioasinc.data.local.database.settings.PrivacyStoreImpl
import com.synapse.social.studioasinc.data.local.database.settings.SettingsConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PrivacyStoreTest {

    private class TestDataStore(initialPreferences: Preferences = preferencesOf()) : DataStore<Preferences> {
        private val state = MutableStateFlow(initialPreferences)
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    @Test
    fun legacyProfileVisibilityMigration_mapsToNewVisibilities() = runTest {
        val prefs = preferencesOf(
            SettingsConstants.KEY_PROFILE_VISIBILITY to ProfileVisibility.FOLLOWERS_ONLY.name,
            SettingsConstants.KEY_CONTENT_VISIBILITY to ContentVisibility.ONLY_ME.name
        )
        val dataStore = TestDataStore(prefs)
        val privacyStore = PrivacyStoreImpl(dataStore)

        val settings = privacyStore.privacySettings.first()

        assertEquals(LastSeenVisibility.MY_CONTACTS, settings.lastSeenVisibility)
        assertEquals(ProfilePhotoVisibility.MY_CONTACTS, settings.profilePhotoVisibility)
        assertEquals(AboutVisibility.MY_CONTACTS, settings.aboutVisibility)
        assertEquals(StatusVisibility.NOBODY, settings.statusVisibility)
    }

    @Test
    fun independentKeys_overrideLegacySettings() = runTest {
        val prefs = preferencesOf(
            SettingsConstants.KEY_PROFILE_VISIBILITY to ProfileVisibility.PUBLIC.name,
            SettingsConstants.KEY_LAST_SEEN_VISIBILITY to LastSeenVisibility.NOBODY.name,
            SettingsConstants.KEY_STATUS_VISIBILITY to StatusVisibility.MY_CONTACTS.name
        )
        val dataStore = TestDataStore(prefs)
        val privacyStore = PrivacyStoreImpl(dataStore)

        val settings = privacyStore.privacySettings.first()

        assertEquals(LastSeenVisibility.NOBODY, settings.lastSeenVisibility)
        assertEquals(ProfilePhotoVisibility.EVERYONE, settings.profilePhotoVisibility)
        assertEquals(AboutVisibility.EVERYONE, settings.aboutVisibility)
        assertEquals(StatusVisibility.MY_CONTACTS, settings.statusVisibility)
    }

    @Test
    fun setIndividualVisibilities_persistsCorrectly() = runTest {
        val dataStore = TestDataStore()
        val privacyStore = PrivacyStoreImpl(dataStore)

        privacyStore.setLastSeenVisibility(LastSeenVisibility.MY_CONTACTS_EXCEPT)
        privacyStore.setProfilePhotoVisibility(ProfilePhotoVisibility.NOBODY)
        privacyStore.setAboutVisibility(AboutVisibility.EVERYONE)
        privacyStore.setStatusVisibility(StatusVisibility.MY_CONTACTS)

        val settings = privacyStore.privacySettings.first()

        assertEquals(LastSeenVisibility.MY_CONTACTS_EXCEPT, settings.lastSeenVisibility)
        assertEquals(ProfilePhotoVisibility.NOBODY, settings.profilePhotoVisibility)
        assertEquals(AboutVisibility.EVERYONE, settings.aboutVisibility)
        assertEquals(StatusVisibility.MY_CONTACTS, settings.statusVisibility)
    }
}
