package com.synapse.social.studioasinc.feature.settings.backup

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import com.synapse.social.studioasinc.data.local.database.settings.SettingsConstants
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

internal object SettingsBackupCodec {
    const val CURRENT_VERSION = 2
    private val serializer = Json { ignoreUnknownKeys = true }
    private val definitions = listOf(
        string(SettingsConstants.KEY_THEME_MODE),
        bool(SettingsConstants.KEY_DYNAMIC_COLOR_ENABLED),
        string(SettingsConstants.KEY_FONT_SCALE),
        string(SettingsConstants.KEY_APP_LANGUAGE),
        string(SettingsConstants.KEY_POST_VIEW_STYLE),
        string(SettingsConstants.KEY_SELECTED_FONT_ID),
        string(SettingsConstants.KEY_PROFILE_VISIBILITY),
        string(SettingsConstants.KEY_CONTENT_VISIBILITY),
        string(SettingsConstants.KEY_GROUP_PRIVACY),
        string(SettingsConstants.KEY_LAST_SEEN_VISIBILITY),
        string(SettingsConstants.KEY_PROFILE_PHOTO_VISIBILITY),
        string(SettingsConstants.KEY_ABOUT_VISIBILITY),
        string(SettingsConstants.KEY_STATUS_VISIBILITY),
        stringSet(SettingsConstants.KEY_LAST_SEEN_EXCLUDED_USER_IDS),
        stringSet(SettingsConstants.KEY_PROFILE_PHOTO_EXCLUDED_USER_IDS),
        stringSet(SettingsConstants.KEY_ABOUT_EXCLUDED_USER_IDS),
        stringSet(SettingsConstants.KEY_STATUS_EXCLUDED_USER_IDS),
        stringSet(SettingsConstants.KEY_GROUP_EXCLUDED_USER_IDS),
        int(SettingsConstants.KEY_KEEP_MEDIA_DAYS),
        int(SettingsConstants.KEY_MAX_CACHE_SIZE_GB),
        bool(SettingsConstants.KEY_NOTIFICATIONS_LIKES),
        bool(SettingsConstants.KEY_NOTIFICATIONS_COMMENTS),
        bool(SettingsConstants.KEY_NOTIFICATIONS_FOLLOWS),
        bool(SettingsConstants.KEY_NOTIFICATIONS_MESSAGES),
        bool(SettingsConstants.KEY_NOTIFICATIONS_MENTIONS),
        bool(SettingsConstants.KEY_IN_APP_NOTIFICATIONS),
        bool(SettingsConstants.KEY_READ_RECEIPTS_ENABLED),
        bool(SettingsConstants.KEY_TYPING_INDICATORS_ENABLED),
        string(SettingsConstants.KEY_MEDIA_AUTO_DOWNLOAD),
        float(SettingsConstants.KEY_CHAT_FONT_SCALE),
        string(SettingsConstants.KEY_CHAT_THEME_PRESET),
        string(SettingsConstants.KEY_CHAT_WALLPAPER_TYPE),
        string(SettingsConstants.KEY_CHAT_WALLPAPER_VALUE),
        float(SettingsConstants.KEY_CHAT_WALLPAPER_BLUR),
        int(SettingsConstants.KEY_CHAT_MESSAGE_CORNER_RADIUS),
        string(SettingsConstants.KEY_CHAT_LIST_LAYOUT),
        string(SettingsConstants.KEY_CHAT_SWIPE_GESTURE),
        string(SettingsConstants.KEY_CHAT_FOLDERS),
        int(SettingsConstants.KEY_CHAT_MAX_MESSAGE_CHUNK_SIZE),
        int(SettingsConstants.KEY_CHAT_MESSAGE_PAGINATION_LIMIT),
        string(SettingsConstants.KEY_AI_PREFERRED_PROVIDER),
        bool(SettingsConstants.KEY_AI_FALLBACK_TO_PLATFORM),
        string(SettingsConstants.KEY_AI_CUSTOM_MODEL),
        string(SettingsConstants.KEY_NAVIGATION_PLACEMENT),
        bool(SettingsConstants.KEY_MESSAGE_SUGGESTION_ENABLED),
        bool(SettingsConstants.KEY_CHAT_AVATAR_DISABLED),
        bool(SettingsConstants.KEY_DATA_SAVER_ENABLED),
        bool(SettingsConstants.KEY_ENTER_IS_SEND_ENABLED),
        bool(SettingsConstants.KEY_MEDIA_VISIBILITY_ENABLED),
        bool(SettingsConstants.KEY_VOICE_TRANSCRIPTS_ENABLED),
        bool(SettingsConstants.KEY_AUTO_BACKUP_ENABLED),
        bool(SettingsConstants.KEY_REMINDERS_ENABLED),
        bool(SettingsConstants.KEY_HIGH_PRIORITY_ENABLED),
        bool(SettingsConstants.KEY_REACTION_NOTIFICATIONS_ENABLED),
        string(SettingsConstants.KEY_MEDIA_UPLOAD_QUALITY),
        bool(SettingsConstants.KEY_USE_LESS_DATA_CALLS),
        stringSet(SettingsConstants.KEY_AUTO_DOWNLOAD_MOBILE),
        stringSet(SettingsConstants.KEY_AUTO_DOWNLOAD_WIFI),
        stringSet(SettingsConstants.KEY_AUTO_DOWNLOAD_ROAMING),
        bool(SettingsConstants.KEY_ACCOUNT_REPORTS_AUTO_CREATE),
        bool(SettingsConstants.KEY_CHANNELS_REPORTS_AUTO_CREATE),
        bool(SettingsConstants.KEY_HIDE_PROFILE_PIC_SUGGESTION),
        bool(SettingsConstants.KEY_INCREASE_CONTRAST_ENABLED),
        bool(SettingsConstants.KEY_HIGH_CONTRAST_TEXT_ENABLED),
        bool(SettingsConstants.KEY_REDUCE_ANIMATIONS_ENABLED),
        bool(SettingsConstants.KEY_AUTOPLAY_ANIMATIONS_ENABLED)
    ).associateBy { it.name }

    fun fromPreferences(preferences: Preferences, timestamp: Long): SettingsBackup {
        val settingValues = definitions.values.mapNotNull { definition ->
            definition.read(preferences)?.let { definition.name to SettingsBackupValue(definition.type, it) }
        }.toMap().toSortedMap()
        return SettingsBackup(CURRENT_VERSION, timestamp, settingValues)
    }

    fun encode(backup: SettingsBackup): String = serializer.encodeToString(validate(backup))

    fun decode(contents: String): SettingsBackup {
        if (contents.toByteArray(Charsets.UTF_8).size > 1_048_576) throw InvalidSettingsBackupException()
        val version = try {
            serializer.parseToJsonElement(contents).jsonObject["version"]?.jsonPrimitive?.intOrNull
                ?: throw InvalidSettingsBackupException()
        } catch (error: UnsupportedSettingsBackupVersionException) {
            throw error
        } catch (error: Exception) {
            throw InvalidSettingsBackupException(error)
        }
        if (version != CURRENT_VERSION) throw UnsupportedSettingsBackupVersionException()
        val backup = try {
            serializer.decodeFromString<SettingsBackup>(contents)
        } catch (error: Exception) {
            throw InvalidSettingsBackupException(error)
        }
        return validate(backup)
    }

    fun validate(backup: SettingsBackup): SettingsBackup {
        if (backup.version != CURRENT_VERSION) throw UnsupportedSettingsBackupVersionException()
        if (backup.timestamp <= 0L || backup.settings.size > 256) throw InvalidSettingsBackupException()
        val supported = backup.settings.filterKeys { definitions.containsKey(it) }.toSortedMap()
        try {
            supported.forEach { (name, value) ->
                val definition = definitions[name] ?: return@forEach
                if (definition.type != value.type) throw InvalidSettingsBackupException()
                definition.decode(value.value)
            }
        } catch (error: InvalidSettingsBackupException) {
            throw error
        } catch (error: Exception) {
            throw InvalidSettingsBackupException(error)
        }
        return backup.copy(settings = supported)
    }

    fun prepareRestore(backup: SettingsBackup): (MutablePreferences) -> Unit {
        val valid = validate(backup)
        val staged = valid.settings.mapNotNull { (name, value) ->
            definitions[name]?.let { definition -> definition to definition.decode(value.value) }
        }
        return { preferences ->
            definitions.values.forEach { it.remove(preferences) }
            staged.forEach { (definition, value) -> definition.write(preferences, value) }
        }
    }

    private data class PreferenceDefinition(
        val name: String,
        val type: String,
        val read: (Preferences) -> JsonElement?,
        val decode: (JsonElement) -> Any,
        val remove: (MutablePreferences) -> Unit,
        val write: (MutablePreferences, Any) -> Unit
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> definition(
        key: Preferences.Key<T>,
        type: String,
        encode: (T) -> JsonElement,
        decode: (JsonElement) -> T
    ) = PreferenceDefinition(
        name = key.name,
        type = type,
        read = { preferences -> preferences[key]?.let(encode) },
        decode = decode,
        remove = { preferences -> preferences.remove(key) },
        write = { preferences, value ->
            preferences[key] = value as T
        }
    )

    private fun string(key: Preferences.Key<String>) = definition(
        key,
        "string",
        { JsonPrimitive(it) },
        { element ->
            val primitive = element as? JsonPrimitive ?: throw InvalidSettingsBackupException()
            if (!primitive.isString) throw InvalidSettingsBackupException()
            primitive.content
        }
    )

    private fun bool(key: Preferences.Key<Boolean>) = definition(
        key,
        "boolean",
        { JsonPrimitive(it) },
        { element ->
            val primitive = element as? JsonPrimitive ?: throw InvalidSettingsBackupException()
            primitive.booleanOrNull ?: throw InvalidSettingsBackupException()
        }
    )

    private fun int(key: Preferences.Key<Int>) = definition(
        key,
        "int",
        { JsonPrimitive(it) },
        { element ->
            val primitive = element as? JsonPrimitive ?: throw InvalidSettingsBackupException()
            primitive.intOrNull ?: throw InvalidSettingsBackupException()
        }
    )

    private fun float(key: Preferences.Key<Float>) = definition(
        key,
        "float",
        { value ->
            if (!value.isFinite()) throw InvalidSettingsBackupException()
            JsonPrimitive(value)
        },
        { element ->
            val primitive = element as? JsonPrimitive ?: throw InvalidSettingsBackupException()
            val value = primitive.floatOrNull ?: throw InvalidSettingsBackupException()
            if (!value.isFinite()) throw InvalidSettingsBackupException()
            value
        }
    )

    private fun stringSet(key: Preferences.Key<Set<String>>) = definition(
        key,
        "string_set",
        { values -> JsonArray(values.sorted().map(::JsonPrimitive)) },
        { element ->
            val array = element as? JsonArray ?: throw InvalidSettingsBackupException()
            array.map { item ->
                val primitive = item as? JsonPrimitive ?: throw InvalidSettingsBackupException()
                if (!primitive.isString) throw InvalidSettingsBackupException()
                primitive.content
            }.toSet()
        }
    )
}
