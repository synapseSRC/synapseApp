package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.domain.model.FeedbackSubmission
import com.synapse.social.studioasinc.domain.repository.FeedbackRepository
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class FeedbackRepositoryImpl @Inject constructor() : FeedbackRepository {
    private val client = SupabaseClient.client

    @Serializable
    data class Feedback(
        @SerialName("user_id") val userId: String,
        val category: String,
        val description: String,
        @SerialName("app_version") val appVersion: String,
        @SerialName("build_number") val buildNumber: String,
        @SerialName("device_info") val deviceInfo: String
    )

    override suspend fun submitFeedback(submission: FeedbackSubmission): Result<Unit> = try {
        check(SupabaseClient.isConfigured()) { "Feedback service is not configured" }
        check(SupabaseClient.getUrl().startsWith("https://", ignoreCase = true)) {
            "Feedback submission requires encrypted HTTPS transport"
        }
        val userId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Not authenticated")
        val contactDescription = if (submission.email.isBlank()) {
            submission.description
        } else {
            submission.description + "\n\nContact email: " + submission.email
        }
        client.from("feedback")
            .insert(
                Feedback(
                    userId = userId,
                    category = submission.category,
                    description = contactDescription,
                    appVersion = submission.appVersion,
                    buildNumber = submission.buildNumber,
                    deviceInfo = submission.deviceInfo
                )
            )
        Log.d(TAG, "Feedback submitted: category=" + submission.category)
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    companion object {
        private const val TAG = "FeedbackRepositoryImpl"
    }
}
