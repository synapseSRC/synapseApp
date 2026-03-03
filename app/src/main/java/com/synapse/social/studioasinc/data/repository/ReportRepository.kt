package com.synapse.social.studioasinc.data.repository

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject



class ReportRepository @Inject constructor(
    private val client: SupabaseClient = com.synapse.social.studioasinc.shared.core.network.SupabaseClient.client
) {

    @Serializable
    private data class PostReport(
        val id: String? = null,
        @SerialName("post_id") val postId: String,
        @SerialName("reporter_id") val reporterId: String,
        val reason: String,
        val description: String? = null
    )



    suspend fun createReport(
        postId: String,
        reason: String,
        description: String? = null
    ): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return Result.failure(Exception("Not authenticated"))

        client.from("post_reports")
            .insert(PostReport(
                postId = postId,
                reporterId = userId,
                reason = reason,
                description = description
            ))

        Log.d(TAG, "Report created: post=$postId, reason=$reason")
    }

    companion object {
        private const val TAG = "ReportRepository"
    }
}
