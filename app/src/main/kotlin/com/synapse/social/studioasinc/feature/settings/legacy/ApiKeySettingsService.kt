package com.synapse.social.studioasinc.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import com.synapse.social.studioasinc.data.local.database.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ApiKeyInfo(
    val id: String,
    val provider: String,
    val keyName: String,
    val isActive: Boolean,
    val usageLimit: Int?,
    val usageCount: Int,
    val createdAt: String
)

@Serializable
data class ProviderSettings(
    val preferredProvider: String = "platform",
    val fallbackToPlatform: Boolean = true,
    val maxTokens: Int = 1000,
    val temperature: Double = 0.7,
    val customModel: String? = null
)

@Singleton
class ApiKeySettingsService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient
) {
    private val settingsDataStore by lazy { SettingsDataStore.getInstance(context) }
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "synapse_api_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _apiKeys = MutableStateFlow<List<ApiKeyInfo>>(emptyList())
    val apiKeys: StateFlow<List<ApiKeyInfo>> = _apiKeys

    private val _providerSettings = MutableStateFlow(ProviderSettings())
    val providerSettings: StateFlow<ProviderSettings> = _providerSettings

    // Keys are stored as JSON under "keys_index" (list of ApiKeyInfo without the raw key)
    // Raw key stored separately under "key_<id>"

    suspend fun loadApiKeys(): Result<List<ApiKeyInfo>> {
        return try {
            val json = prefs.getString("keys_index", "[]") ?: "[]"
            val keys = Json.decodeFromString<List<ApiKeyInfo>>(json)
            _apiKeys.value = keys
            Result.success(keys)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun storeApiKey(
        provider: String,
        apiKey: String,
        keyName: String? = null,
        usageLimit: Int? = null
    ): Result<String> {
        val byokProviders = getAvailableProviders().filter { it != "platform" }
        if (provider.isBlank() || provider !in byokProviders)
            return Result.failure(IllegalArgumentException("Invalid provider"))
        if (apiKey.isBlank())
            return Result.failure(IllegalArgumentException("API key cannot be empty"))
        if (usageLimit != null && usageLimit <= 0)
            return Result.failure(IllegalArgumentException("Usage limit must be positive"))

        return try {
            val id = UUID.randomUUID().toString()
            val info = ApiKeyInfo(
                id = id,
                provider = provider,
                keyName = keyName?.takeIf { it.isNotBlank() } ?: "$provider Key",
                isActive = true,
                usageLimit = usageLimit,
                usageCount = 0,
                createdAt = System.currentTimeMillis().toString()
            )
            val current = _apiKeys.value.toMutableList()
            current.add(info)
            prefs.edit()
                .putString("keys_index", Json.encodeToString<List<ApiKeyInfo>>(current))
                .putString("key_$id", apiKey)
                .apply()
            _apiKeys.value = current
            Result.success("API key stored successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteApiKey(keyId: String): Result<String> {
        if (keyId.isBlank())
            return Result.failure(IllegalArgumentException("Key ID cannot be empty"))
        return try {
            val current = _apiKeys.value.filter { it.id != keyId }
            prefs.edit()
                .putString("keys_index", Json.encodeToString<List<ApiKeyInfo>>(current))
                .remove("key_$keyId")
                .apply()
            _apiKeys.value = current
            Result.success("API key deleted successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Retrieve the raw key value for use in AI requests */
    fun getRawApiKey(keyId: String): String? = prefs.getString("key_$keyId", null)

    suspend fun loadProviderSettings(): Result<ProviderSettings> {
        return try {
            val settings = ProviderSettings(
                preferredProvider = settingsDataStore.getAiPreferredProvider(),
                fallbackToPlatform = settingsDataStore.getAiFallbackToPlatform(),
                customModel = settingsDataStore.getAiCustomModel()
            )
            _providerSettings.value = settings
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProviderSettings(settings: ProviderSettings): Result<Unit> {
        return try {
            settingsDataStore.setAiPreferredProvider(settings.preferredProvider)
            settingsDataStore.setAiFallbackToPlatform(settings.fallbackToPlatform)
            settingsDataStore.setAiCustomModel(settings.customModel)
            _providerSettings.value = settings
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePreferredProvider(provider: String) {
        updateProviderSettings(_providerSettings.value.copy(preferredProvider = provider))
    }

    suspend fun updateFallbackSetting(fallbackToPlatform: Boolean) {
        updateProviderSettings(_providerSettings.value.copy(fallbackToPlatform = fallbackToPlatform))
    }

    suspend fun updateCustomModel(model: String?) {
        val current = _providerSettings.value
        updateProviderSettings(current.copy(customModel = model))
    }

    fun getAvailableProviders() = listOf("platform", "openai", "gemini", "anthropic", "openrouter")

    fun getProviderDisplayName(provider: String) = when (provider) {
        "platform"   -> "Synapse (Free)"
        "openai"     -> "OpenAI GPT"
        "gemini"     -> "Google Gemini"
        "anthropic"  -> "Anthropic Claude"
        "openrouter" -> "OpenRouter"
        else         -> provider.replaceFirstChar { it.titlecase() }
    }

    suspend fun getUserApiKeys(): List<ApiKeyInfo> = _apiKeys.value
    suspend fun getProviderSettings(): ProviderSettings = _providerSettings.value
}
