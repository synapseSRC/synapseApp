package com.synapse.social.studioasinc.feature.settings.backup

import androidx.datastore.preferences.core.preferencesOf
import com.synapse.social.studioasinc.data.local.database.settings.SettingsConstants
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupCodecTest {
    @Test
    fun roundTripPreservesSupportedTypedPreferencesAndOmitsSensitiveKeys() {
        val preferences = preferencesOf(
            SettingsConstants.KEY_THEME_MODE to "DARK",
            SettingsConstants.KEY_DYNAMIC_COLOR_ENABLED to true,
            SettingsConstants.KEY_CHAT_FONT_SCALE to 1.25f,
            SettingsConstants.KEY_AUTO_DOWNLOAD_WIFI to setOf("PHOTO", "VIDEO"),
            SettingsConstants.KEY_TWO_FACTOR_ENABLED to true,
            SettingsConstants.KEY_SEARCH_HISTORY to "private query"
        )

        val backup = SettingsBackupCodec.fromPreferences(preferences, timestamp = 1234L)
        val decoded = SettingsBackupCodec.decode(SettingsBackupCodec.encode(backup))

        assertEquals(2, decoded.version)
        assertEquals(1234L, decoded.timestamp)
        assertEquals(backup.settings, decoded.settings)
        assertFalse(decoded.settings.containsKey("two_factor_enabled"))
        assertFalse(decoded.settings.containsKey("search_history"))
        assertTrue(decoded.settings.containsKey("theme_mode"))
    }

    @Test
    fun rejectsUnsupportedVersionBeforeRestore() {
        val backup = SettingsBackup(version = 3, timestamp = 1234L, settings = emptyMap())

        assertThrows(UnsupportedSettingsBackupVersionException::class.java) {
            SettingsBackupCodec.prepareRestore(backup)
        }
    }

    @Test
    fun rejectsLegacyVersionOneSharedPreferencesBackupSafely() {
        val legacy = "{\"version\":1,\"timestamp\":1234,\"settings\":{\"theme_mode\":\"DARK\"}}"

        assertThrows(UnsupportedSettingsBackupVersionException::class.java) {
            SettingsBackupCodec.decode(legacy)
        }
    }

    @Test
    fun rejectsMalformedKnownPreferenceWithoutProducingAValidSnapshot() {
        val backup = SettingsBackup(
            version = 2,
            timestamp = 1234L,
            settings = mapOf("theme_mode" to SettingsBackupValue("boolean", JsonPrimitive(true)))
        )

        assertThrows(InvalidSettingsBackupException::class.java) {
            SettingsBackupCodec.prepareRestore(backup)
        }
    }

    @Test
    fun ignoresUnknownPreferenceKeysForForwardCompatibilityWithinVersion() {
        val backup = SettingsBackup(
            version = 2,
            timestamp = 1234L,
            settings = mapOf("future_setting" to SettingsBackupValue("string", JsonPrimitive("value")))
        )

        assertTrue(SettingsBackupCodec.validate(backup).settings.isEmpty())
    }
}
