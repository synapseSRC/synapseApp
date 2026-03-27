package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.data.model.PasskeyDto
import com.synapse.social.studioasinc.shared.domain.model.auth.Passkey
import com.synapse.social.studioasinc.shared.domain.repository.PasskeyRepository
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PasskeyRepositoryImpl(
    private val client: SupabaseClientType = SupabaseClient.client
) : PasskeyRepository {

    companion object {
        private const val PASSKEY_VERIFICATION_FUNCTION = "verify-passkey-registration"
    }

    override suspend fun getPasskeys(userId: String): Result<List<Passkey>> = withContext(Dispatchers.Default) {
        runCatching {
            val result = client.postgrest["user_passkeys"]
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<PasskeyDto>()

            result.map {
                Passkey(
                    id = it.id,
                    deviceName = it.device_name,
                    dateAdded = it.date_added,
                    lastUsed = it.last_used
                )
            }
        }
    }

    override suspend fun deletePasskey(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            client.postgrest["user_passkeys"].delete {
                filter {
                    eq("id", id)
                }
            }
            Unit
        }
    }

    override suspend fun generatePasskeyChallenge(): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            val response = client.functions.invoke("generate-passkey-challenge")
            val body = response.body<Map<String, String>>()
            body["challenge"] ?: throw IllegalStateException("Invalid challenge response")
        }
    }

    override suspend fun registerPasskey(registrationResponseJson: String, deviceName: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val result = client.functions.invoke(
                function = PASSKEY_VERIFICATION_FUNCTION,
                body = mapOf(
                    "registrationResponse" to registrationResponseJson,
                    "deviceName" to deviceName
                )
            )

            if (result.status.value !in 200..299) {
                val errorBody = result.body<Map<String, String>>()
                val errorMessage = errorBody["error"] ?: "Server returned status ${result.status.value}"
                throw IllegalStateException(errorMessage)
            }
            Unit
        }
    }
}
