package com.synapse.social.studioasinc.shared.data.repository

import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.datasource.EchoRoomRealtimeDataSource
import com.synapse.social.studioasinc.shared.data.dto.echo.EchoHeartbeatResultDto
import com.synapse.social.studioasinc.shared.data.dto.echo.EchoJoinResultDto
import com.synapse.social.studioasinc.shared.data.dto.echo.EchoLobbyRowDto
import com.synapse.social.studioasinc.shared.data.dto.echo.EchoRoomDetailRowDto
import com.synapse.social.studioasinc.shared.data.dto.echo.EchoRoomDto
import com.synapse.social.studioasinc.shared.data.mapper.echo.EchoMapper
import com.synapse.social.studioasinc.shared.data.mapper.echo.toWireValue
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoBackendException
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoErrorKind
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRole
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoom
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomDetail
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomStatus
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomSummary
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoVisibility
import com.synapse.social.studioasinc.shared.domain.repository.EchoRoomRepository
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.github.jan.supabase.postgrest.rpc
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Supabase implementation of the Echo Room state.
 *
 * Every mutation is an RPC: the tables are read-only for clients, and the role checks (host,
 * membership, stage size) live in Postgres where they cannot be skipped. Failures are translated
 * into [EchoBackendException] using the `SYNAPSE_ECHO_*` token the RPCs raise with.
 */
class SupabaseEchoRoomRepository(
    private val client: SupabaseClientLib = SupabaseClient.client,
    private val realtime: EchoRoomRealtimeDataSource = EchoRoomRealtimeDataSource(client)
) : EchoRoomRepository {

    override suspend fun lobby(limit: Int): Result<List<EchoRoomSummary>> = guarded("lobby") {
        client.postgrest.rpc("get_echo_lobby", buildJsonObject {
            put("p_limit", limit)
        }).decodeList<EchoLobbyRowDto>().map(EchoMapper::toSummary)
    }

    override suspend fun create(
        title: String,
        topic: String?,
        visibility: EchoVisibility,
        maxSpeakers: Int
    ): Result<EchoRoom> = guarded("create") {
        val row = client.postgrest.rpc("create_echo_room", buildJsonObject {
            put("p_title", title)
            put("p_topic", topic)
            put("p_visibility", visibility.toWireValue())
            put("p_max_speakers", maxSpeakers)
        }).decodeRow<EchoRoomDto>()
        EchoMapper.toDomain(row)
    }

    override suspend fun join(roomId: String): Result<EchoRole> = guarded("join") {
        val rows = client.postgrest.rpc("join_echo_room", buildJsonObject {
            put("p_room_id", roomId)
        }).decodeList<EchoJoinResultDto>()
        rows.firstOrNull()?.role?.let(EchoRole::fromRaw)
            ?: throw EchoBackendException(EchoErrorKind.UNKNOWN, "Echo join returned no role")
    }

    override suspend fun leave(roomId: String): Result<Unit> = guarded("leave") {
        client.postgrest.rpc("leave_echo_room", buildJsonObject {
            put("p_room_id", roomId)
        })
        Unit
    }

    override suspend fun heartbeat(roomId: String): Result<EchoRoomStatus> = guarded("heartbeat") {
        val rows = client.postgrest.rpc("echo_room_heartbeat", buildJsonObject {
            put("p_room_id", roomId)
        }).decodeList<EchoHeartbeatResultDto>()
        val raw = rows.firstOrNull()?.status
        EchoRoomStatus.fromRaw(raw)
            ?: throw EchoBackendException(EchoErrorKind.UNKNOWN, "Echo heartbeat returned no status")
    }

    override suspend fun end(roomId: String): Result<Unit> = guarded("end") {
        client.postgrest.rpc("end_echo_room", buildJsonObject {
            put("p_room_id", roomId)
        })
        Unit
    }

    override suspend fun setRole(roomId: String, userId: String, role: EchoRole): Result<Unit> =
        guarded("setRole") {
            client.postgrest.rpc("set_echo_participant_role", buildJsonObject {
                put("p_room_id", roomId)
                put("p_user_id", userId)
                put("p_role", role.toWireValue())
            })
            Unit
        }

    override suspend fun setHandRaised(roomId: String, raised: Boolean): Result<Unit> =
        guarded("setHandRaised") {
            client.postgrest.rpc("set_echo_hand_raised", buildJsonObject {
                put("p_room_id", roomId)
                put("p_raised", raised)
            })
            Unit
        }

    override suspend fun fetchRoom(roomId: String): Result<EchoRoomDetail> = guarded("fetchRoom") {
        val rows = client.postgrest.rpc("get_echo_room", buildJsonObject {
            put("p_room_id", roomId)
        }).decodeList<EchoRoomDetailRowDto>()
        val row = rows.firstOrNull()
            ?: throw EchoBackendException(EchoErrorKind.NOT_FOUND, "Echo room $roomId no longer exists")
        EchoMapper.toDetail(row)
    }

    override fun observeRoom(roomId: String): Flow<EchoRoomDetail> =
        realtime.subscribeToRoomChanges(roomId)
            .debounce(REFRESH_DEBOUNCE_MS)
            .onStart { emit(Unit) }
            .map { fetchRoom(roomId).getOrThrow() }
            .retryWhen { cause, attempt ->
                // Losing membership or the room itself is not transient; retrying would hide the
                // real outcome and keep a dead screen subscribed forever.
                if (cause is EchoBackendException && cause.kind in terminalKinds) {
                    return@retryWhen false
                }
                Napier.w(
                    "Echo room $roomId refresh failed (${cause.message}); retrying",
                    tag = TAG
                )
                delay(minOf(RETRY_BASE_DELAY_MS * (attempt + 1), RETRY_MAX_DELAY_MS))
                true
            }
            .distinctUntilChanged()

    override suspend fun expireStale(): Result<Int> = guarded("expireStale") {
        val body = client.postgrest.rpc("expire_echo_rooms").data.trim()
        body.trim('[', ']', ' ', '\n').toIntOrNull()
            ?: throw EchoBackendException(EchoErrorKind.UNKNOWN, "Echo expiry returned an unexpected body")
    }

    /**
     * PostgREST renders a composite-returning RPC as a single JSON object (a set would be an
     * array), while supabase-kt's `decodeSingle` only accepts an array — accept both.
     */
    private inline fun <reified T> PostgrestResult.decodeRow(): T {
        val trimmed = data.trim()
        return if (trimmed.startsWith("[")) decodeList<T>().first() else decodeAs<T>()
    }

    private suspend fun <T> guarded(action: String, block: suspend () -> T): Result<T> =
        try {
            withContext(AppDispatchers.IO) { Result.success(block()) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Echo $action failed: ${e.message}", tag = TAG)
            Result.failure(e.toEchoFailure(action))
        }

    private fun Throwable.toEchoFailure(action: String): EchoBackendException {
        if (this is EchoBackendException) return this
        val detail = message.orEmpty()
        val kind = when {
            detail.contains("SYNAPSE_ECHO_BANNED") -> EchoErrorKind.BANNED
            detail.contains("SYNAPSE_ECHO_BLOCKED") -> EchoErrorKind.BLOCKED
            detail.contains("SYNAPSE_ECHO_ENDED") -> EchoErrorKind.ENDED
            detail.contains("SYNAPSE_ECHO_NOT_FOUND") -> EchoErrorKind.NOT_FOUND
            detail.contains("SYNAPSE_ECHO_STAGE_FULL") -> EchoErrorKind.STAGE_FULL
            detail.contains("SYNAPSE_ECHO_NOT_AUDIENCE") -> EchoErrorKind.NOT_AUDIENCE
            detail.contains("SYNAPSE_ECHO_NOT_MEMBER") -> EchoErrorKind.FORBIDDEN
            detail.contains("SYNAPSE_ECHO_FORBIDDEN") -> EchoErrorKind.FORBIDDEN
            detail.contains("SYNAPSE_ECHO_PROFILE_REQUIRED") -> EchoErrorKind.FORBIDDEN
            detail.contains("SYNAPSE_ECHO_UNAUTHENTICATED") -> EchoErrorKind.NOT_AUTHENTICATED
            detail.contains("SYNAPSE_ECHO_INVALID") -> EchoErrorKind.INVALID_INPUT
            detail.contains("JWT", ignoreCase = true) -> EchoErrorKind.NOT_AUTHENTICATED
            this is HttpRequestTimeoutException || this is HttpRequestException ->
                EchoErrorKind.BACKEND_UNREACHABLE
            else -> EchoErrorKind.UNKNOWN
        }
        return EchoBackendException(kind, "Echo $action failed: ${detail.take(MAX_ERROR_CHARS)}")
    }

    private companion object {
        const val TAG = "EchoRepository"
        const val REFRESH_DEBOUNCE_MS = 300L
        const val RETRY_BASE_DELAY_MS = 2_000L
        const val RETRY_MAX_DELAY_MS = 10_000L
        const val MAX_ERROR_CHARS = 200

        val terminalKinds = setOf(
            EchoErrorKind.FORBIDDEN,
            EchoErrorKind.NOT_FOUND,
            EchoErrorKind.ENDED,
            EchoErrorKind.NOT_AUTHENTICATED,
            EchoErrorKind.BLOCKED,
            EchoErrorKind.BANNED
        )
    }
}
