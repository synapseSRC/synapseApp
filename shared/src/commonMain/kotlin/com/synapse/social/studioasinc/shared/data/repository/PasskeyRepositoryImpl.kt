package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.config.SynapseConfig
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.mapper.PasskeyMapper.toDomain
import com.synapse.social.studioasinc.shared.data.model.PasskeyDto
import com.synapse.social.studioasinc.shared.data.model.PasskeySignInSessionDto
import com.synapse.social.studioasinc.shared.domain.model.auth.Passkey
import com.synapse.social.studioasinc.shared.domain.model.auth.PasskeyCeremonyOptions
import com.synapse.social.studioasinc.shared.domain.model.auth.PasskeySignInSession
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * [PasskeyRepository] backed by Supabase's native passkey (WebAuthn) auth endpoints.
 *
 * The supabase-kt SDK (3.8.0) has no passkey API yet, so requests are issued over a Ktor HTTP
 * client pointed at the auth REST routes. All Supabase-specific detail — endpoint paths, header
 * wiring and the wire JSON shapes — stays encapsulated here; responses are mapped to Domain models.
 *
 * NOTE: The JSON key names below follow Supabase's documented two-step passkey contract. Passkey
 * ceremonies can only be validated on a real device (biometric prompt), so if a live call fails on
 * a field name, adjust these constants here — never the Domain or presentation layers.
 */
class PasskeyRepositoryImpl(
    private val client: SupabaseClientType = SupabaseClient.client
) : PasskeyRepository {

    companion object {
        private const val REGISTRATION_OPTIONS = "/auth/v1/passkeys/registration/options"
        private const val REGISTRATION_VERIFY = "/auth/v1/passkeys/registration/verify"
        private const val AUTHENTICATION_OPTIONS = "/auth/v1/passkeys/authentication/options"
        private const val AUTHENTICATION_VERIFY = "/auth/v1/passkeys/authentication/verify"
        private const val PASSKEYS = "/auth/v1/passkeys"

        private val CHALLENGE_KEYS = listOf("challenge_id", "challengeId", "id")
        private val OPTION_KEYS = listOf("options", "publicKey", "creationOptions", "requestOptions")
        private val ERROR_KEYS = listOf("msg", "error_description", "error", "message", "code")
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private val http by lazy {
        HttpClient {
            install(ContentNegotiation) { json(json) }
        }
    }

    private val baseUrl: String
        get() = SynapseConfig.SUPABASE_URL.trimEnd('/')

    override suspend fun registrationOptions(friendlyName: String): Result<PasskeyCeremonyOptions> =
        withContext(AppDispatchers.IO) {
            runCatching {
                val body = buildJsonObject { put("friendly_name", friendlyName) }
                parseCeremony(postJson(REGISTRATION_OPTIONS, body, authenticated = true))
            }
        }

    override suspend fun verifyRegistration(challengeId: String, credentialJson: String): Result<Unit> =
        withContext(AppDispatchers.IO) {
            runCatching {
                postJson(REGISTRATION_VERIFY, verifyBody(challengeId, credentialJson), authenticated = true)
                Unit
            }
        }

    override suspend fun authenticationOptions(): Result<PasskeyCeremonyOptions> =
        withContext(AppDispatchers.IO) {
            runCatching {
                parseCeremony(postJson(AUTHENTICATION_OPTIONS, buildJsonObject { }, authenticated = false))
            }
        }

    override suspend fun verifyAuthentication(challengeId: String, credentialJson: String): Result<PasskeySignInSession> =
        withContext(AppDispatchers.IO) {
            runCatching {
                val response = postJson(AUTHENTICATION_VERIFY, verifyBody(challengeId, credentialJson), authenticated = false)
                json.decodeFromString(PasskeySignInSessionDto.serializer(), response)
                    .also {
                        if (it.access_token.isBlank() || it.refresh_token.isBlank()) {
                            throw IllegalStateException("Invalid passkey sign-in session response")
                        }
                    }
                    .toDomain()
            }
        }

    override suspend fun listPasskeys(): Result<List<Passkey>> =
        withContext(AppDispatchers.IO) {
            runCatching {
                val response = getText(PASSKEYS)
                json.decodeFromString(ListSerializer(PasskeyDto.serializer()), response).map { it.toDomain() }
            }
        }

    override suspend fun deletePasskey(id: String): Result<Unit> =
        withContext(AppDispatchers.IO) {
            runCatching {
                deleteText("$PASSKEYS/$id")
                Unit
            }
        }

    private suspend fun postJson(path: String, body: JsonObject, authenticated: Boolean): String {
        val token = currentToken()
        val response = http.post(baseUrl + path) {
            header("apikey", SynapseConfig.SUPABASE_ANON_KEY)
            if (authenticated) token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        return ensureSuccess(response)
    }

    private suspend fun getText(path: String): String {
        val token = currentToken()
        val response = http.get(baseUrl + path) {
            header("apikey", SynapseConfig.SUPABASE_ANON_KEY)
            token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
        return ensureSuccess(response)
    }

    private suspend fun deleteText(path: String): String {
        val token = currentToken()
        val response = http.delete(baseUrl + path) {
            header("apikey", SynapseConfig.SUPABASE_ANON_KEY)
            token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
        return ensureSuccess(response)
    }

    private fun currentToken(): String? = client.auth.currentSessionOrNull()?.accessToken

    private fun verifyBody(challengeId: String, credentialJson: String): JsonObject = buildJsonObject {
        put("challenge_id", challengeId)
        put("credential", json.parseToJsonElement(credentialJson))
    }

    private fun parseCeremony(text: String): PasskeyCeremonyOptions {
        val obj = json.parseToJsonElement(text).jsonObject
        val challengeId = firstString(obj, CHALLENGE_KEYS)
            ?: throw IllegalStateException("Passkey response is missing a challenge id")
        val options = OPTION_KEYS.firstNotNullOfOrNull { obj[it] }
            ?: throw IllegalStateException("Passkey response is missing WebAuthn options")
        // Some backends wrap the WebAuthn object under a `publicKey` key; unwrap to the plain
        // PublicKeyCredentialCreationOptions/RequestOptions that the platform managers expect.
        val publicKeyOptions = if (options is JsonObject && options.containsKey("publicKey")) {
            options["publicKey"] ?: options
        } else {
            options
        }
        return PasskeyCeremonyOptions(challengeId = challengeId, optionsJson = publicKeyOptions.toString())
    }

    private fun firstString(obj: JsonObject, keys: List<String>): String? =
        keys.firstNotNullOfOrNull { key ->
            obj[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        }

    private suspend fun ensureSuccess(response: io.ktor.client.statement.HttpResponse): String {
        val text = response.bodyAsText()
        val status = response.status.value
        if (status in 200..299) return text
        val message = runCatching { firstString(json.parseToJsonElement(text).jsonObject, ERROR_KEYS) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Server returned status $status"
        throw IllegalStateException(message)
    }
}
