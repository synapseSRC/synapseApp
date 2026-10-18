package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import android.os.Build
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.repository.FeedbackRepositoryImpl
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class FeedbackSubmissionState {
    Idle,
    InvalidDescription,
    InvalidEmail,
    Sending,
    Sent,
    Failed
}

class FeedbackViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FeedbackRepositoryImpl()
    private val _submissionState = MutableStateFlow(FeedbackSubmissionState.Idle)
    val submissionState: StateFlow<FeedbackSubmissionState> = _submissionState.asStateFlow()

    fun clearValidationError() {
        if (_submissionState.value == FeedbackSubmissionState.InvalidDescription ||
            _submissionState.value == FeedbackSubmissionState.InvalidEmail
        ) _submissionState.value = FeedbackSubmissionState.Idle
    }

    fun submitFeedback(category: String, email: String, description: String, includeDiagnostics: Boolean) {
        if (_submissionState.value == FeedbackSubmissionState.Sending) return
        val cleanedEmail = email.trim()
        if (description.isBlank()) {
            _submissionState.value = FeedbackSubmissionState.InvalidDescription
            return
        }
        if (cleanedEmail.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(cleanedEmail).matches()) {
            _submissionState.value = FeedbackSubmissionState.InvalidEmail
            return
        }

        _submissionState.value = FeedbackSubmissionState.Sending
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    check(SupabaseClient.isConfigured()) { "Feedback service is not configured" }
                    val app = getApplication<Application>()
                    val packageInfo = app.packageManager.getPackageInfo(app.packageName, 0)
                    val appVersion = packageInfo.versionName ?: "Unknown"
                    val buildNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toString()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toString()
                    }
                    val packet = buildDeviceInfoPacket(cleanedEmail, includeDiagnostics)
                    val descriptionWithContact = if (cleanedEmail.isBlank()) {
                        description.trim()
                    } else {
                        getApplication<Application>().getString(R.string.about_feedback_contact_line, description.trim(), cleanedEmail)
                    }
                    repository.submitFeedback(
                        category = category,
                        description = descriptionWithContact,
                        appVersion = appVersion,
                        buildNumber = buildNumber,
                        deviceInfo = packet
                    ).getOrThrow()
                }
            }
            _submissionState.value = if (result.isSuccess) FeedbackSubmissionState.Sent else FeedbackSubmissionState.Failed
        }
    }

    private fun buildDeviceInfoPacket(email: String, includeDiagnostics: Boolean): String {
        val packet = JSONObject()
        if (email.isNotBlank()) packet.put("contact_email", email)
        packet.put("diagnostics_included", includeDiagnostics)
        if (includeDiagnostics) {
            packet.put("device_model", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
            packet.put("os_version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            packet.put("stack_traces", captureAppThreadTraces())
        }
        return packet.toString()
    }

    private fun captureAppThreadTraces(): String = Thread.getAllStackTraces()
        .entries
        .asSequence()
        .filter { (_, frames) -> frames.any { it.className.startsWith("com.synapse.social") } }
        .sortedBy { (thread, _) -> thread.name }
        .take(MAX_DIAGNOSTIC_THREADS)
        .joinToString(separator = "\n\n") { (thread, frames) ->
            buildString {
                append("Thread: ").append(thread.name).append('\n')
                frames.take(MAX_FRAMES_PER_THREAD).forEach { frame ->
                    append("  at ").append(frame.toString()).append('\n')
                }
            }
        }
        .take(MAX_DIAGNOSTIC_CHARACTERS)

    companion object {
        private const val MAX_DIAGNOSTIC_THREADS = 12
        private const val MAX_FRAMES_PER_THREAD = 30
        private const val MAX_DIAGNOSTIC_CHARACTERS = 24_000
    }
}
