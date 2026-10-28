package com.synapse.social.studioasinc.data.call

import com.synapse.social.studioasinc.data.call.dto.CallPeerDto
import com.synapse.social.studioasinc.data.call.dto.CallSessionDto
import com.synapse.social.studioasinc.data.call.mapper.toWireValue
import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.domain.model.call.CallEndReason
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * RPC arguments need one serializable type: a map mixing strings and numbers is inferred
 * as `Map<String, Any>`, which fails at runtime with "Serializer for class 'Any'".
 */
@Serializable
private data class CreateCallParams(
    @SerialName("p_chat_id") val chatId: String,
    @SerialName("p_callee_id") val calleeId: String,
    @SerialName("p_media_type") val mediaType: String,
    @SerialName("p_ringing_seconds") val ringingSeconds: Int
)

/**
 * PostgREST access to `call_sessions`.
 *
 * Every lifecycle change goes through an RPC because the table is not writable by
 * clients — the transitions, the role checks and the one-live-session-per-pair rule are
 * all decided in Postgres by the calling RPCs.
 * This class therefore never calls insert/update/delete on that table; doing so would
 * fail with a permission error and hide the real reason behind an exception.
 *
 * Exceptions deliberately propagate. The repository is what decides what a rejection
 * *means* for the call, and swallowing it here would turn "you are busy" into a silent
 * no-op.
 */
class CallSessionDataSource(private val client: SupabaseClientLib) {

    val currentUserId: String? get() = client.auth.currentUserOrNull()?.id

    /**
     * PostgREST renders a composite-returning RPC as a single JSON object (a set would be
     * an array), while supabase-kt's `decodeSingle` only accepts an array — accept both.
     */
    private fun decodeRow(body: String): CallSessionDto {
        val trimmed = body.trim()
        return if (trimmed.startsWith("[")) {
            callJson.decodeFromString<List<CallSessionDto>>(trimmed).first()
        } else {
            callJson.decodeFromString<CallSessionDto>(trimmed)
        }
    }

    suspend fun create(
        chatId: String,
        calleeId: String,
        mediaType: CallMediaType,
        ringingSeconds: Int
    ): CallSessionDto = withContext(AppDispatchers.IO) {
        decodeRow(
            client.postgrest.rpc(
                "create_call_session",
                CreateCallParams(
                    chatId = chatId,
                    calleeId = calleeId,
                    mediaType = mediaType.toWireValue(),
                    ringingSeconds = ringingSeconds
                )
            ).data
        )
    }

    suspend fun accept(callId: String): CallSessionDto = withContext(AppDispatchers.IO) {
        rpcRow("accept_call", callId)
    }

    suspend fun decline(callId: String): CallSessionDto = withContext(AppDispatchers.IO) {
        rpcRow("decline_call", callId)
    }

    suspend fun cancel(callId: String): CallSessionDto = withContext(AppDispatchers.IO) {
        rpcRow("cancel_call", callId)
    }

    /**
     * Claims a connected media session. The backend only accepts this once the callee has
     * answered, so a client that never received an answer cannot mark a call active.
     */
    suspend fun markActive(callId: String): CallSessionDto = withContext(AppDispatchers.IO) {
        rpcRow("mark_active", callId)
    }

    suspend fun markReconnecting(callId: String): CallSessionDto = withContext(AppDispatchers.IO) {
        rpcRow("mark_reconnecting", callId)
    }

    suspend fun switchMediaType(callId: String, mediaType: CallMediaType): CallSessionDto =
        withContext(AppDispatchers.IO) {
            decodeRow(
                client.postgrest.rpc(
                    "switch_call_media",
                    mapOf("p_call_id" to callId, "p_media_type" to mediaType.toWireValue())
                ).data
            )
        }

    /** `reason = null` means "the server decides", which is what an ordinary hang-up is. */
    suspend fun end(callId: String, reason: CallEndReason?): CallSessionDto =
        withContext(AppDispatchers.IO) {
            val params = if (reason == null) {
                mapOf("p_call_id" to callId)
            } else {
                mapOf("p_call_id" to callId, "p_reason" to reason.toWireValue())
            }
            decodeRow(client.postgrest.rpc("end_call", params).data)
        }

    suspend fun fetch(callId: String): CallSessionDto? = withContext(AppDispatchers.IO) {
        client.postgrest.from(TABLE)
            .select(columns = callColumns()) {
                filter { eq("id", callId) }
                limit(1)
            }.decodeList<CallSessionDto>().firstOrNull()
    }

    /**
     * Sessions this user is still a party to — the read that lets a cold start resume a
     * call instead of showing a dead screen while a live session exists server-side.
     */
    suspend fun getOpenSessions(userId: String): List<CallSessionDto> = withContext(AppDispatchers.IO) {
        client.postgrest.from(TABLE)
            .select(columns = callColumns()) {
                filter {
                    isIn("status", OPEN_STATUSES)
                    or {
                        eq("caller_id", userId)
                        eq("callee_id", userId)
                    }
                }
                order("created_at", Order.DESCENDING)
                limit(20)
            }.decodeList<CallSessionDto>()
    }

    /** Rings addressed to [userId] that the server has not yet let expire. */
    suspend fun getPendingInvitations(userId: String): List<CallSessionDto> = withContext(AppDispatchers.IO) {
        client.postgrest.from(TABLE)
            .select(columns = callColumns()) {
                filter {
                    eq("callee_id", userId)
                    eq("status", "ringing")
                    gt("ringing_expires_at", Instant.now().toString())
                }
                order("created_at", Order.DESCENDING)
                limit(5)
            }.decodeList<CallSessionDto>()
    }

    /**
     * Ended sessions [userId] took part in, newest first.
     *
     * The `or` filter repeats the participant check the domain already relies on: RLS
     * decides what is readable, but naming the user in the query keeps the request from
     * asking for rows the server would have to filter out anyway.
     */
    suspend fun getHistory(userId: String, limit: Int): List<CallSessionDto> = withContext(AppDispatchers.IO) {
        client.postgrest.from(TABLE)
            .select(columns = callColumns()) {
                filter {
                    eq("status", "ended")
                    or {
                        eq("caller_id", userId)
                        eq("callee_id", userId)
                    }
                }
                order("ended_at", Order.DESCENDING)
                order("id", Order.DESCENDING)
                limit(limit.toLong())
            }.decodeList<CallSessionDto>()
    }

    /** Presence beat for a live call; the server closes a session that stops beating. */
    suspend fun heartbeat(callId: String): CallSessionDto = withContext(AppDispatchers.IO) {
        rpcRow("call_heartbeat", callId)
    }

    /**
     * Resolves rings whose server deadline passed.
     *
     * PostgREST renders a scalar-returning RPC either bare (`3`) or wrapped (`[3]`)
     * depending on how it infers the return type, so the body is parsed for both instead
     * of pinning the call to one shape and failing on an upgrade.
     */
    suspend fun resolveStaleSessions(): Int = withContext(AppDispatchers.IO) {
        val body = client.postgrest.rpc("expire_stale_call_sessions").data.trim()
        val scalar = body.trim('[', ']', ' ', '\n').toIntOrNull()
        if (scalar != null) return@withContext scalar
        if (body.isNotEmpty() && body != "null") {
            Napier.w("expire_stale_call_sessions returned an unexpected body: $body", tag = "Call")
        }
        0
    }

    /**
     * Peer profiles for labelling a call.
     *
     * Needed because `call_sessions` stores ids only — copying a display name into the
     * session row would make a rename invisible to a call already in progress, and would
     * duplicate personal data the users table already owns.
     */
    suspend fun getPeers(userIds: List<String>): List<CallPeerDto> = withContext(AppDispatchers.IO) {
        val ids = userIds.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return@withContext emptyList()
        client.postgrest.from("users")
            .select(columns = Columns.list("uid", "username", "display_name", "avatar")) {
                filter { isIn("uid", ids) }
            }.decodeList<CallPeerDto>()
    }

    private suspend fun rpcRow(function: String, callId: String): CallSessionDto =
        decodeRow(client.postgrest.rpc(function, mapOf("p_call_id" to callId)).data)

    /**
     * Explicit column list. A bare `select()` asks for every column, so a migration that
     * adds one would silently widen every call query; naming them keeps the payload small
     * and the coupling visible.
     */
    private fun callColumns(): Columns = Columns.list(
        "id", "chat_id", "caller_id", "callee_id", "media_type", "status",
        "end_reason", "end_initiator", "created_at", "updated_at",
        "answered_at", "ended_at", "ringing_expires_at"
    )

    private companion object {
        const val TABLE = "call_sessions"
        val OPEN_STATUSES = listOf("ringing", "answered", "active", "reconnecting")
        val callJson = Json { ignoreUnknownKeys = true }
    }
}
