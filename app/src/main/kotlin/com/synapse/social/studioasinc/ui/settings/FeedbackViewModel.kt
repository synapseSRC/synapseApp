package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.model.FeedbackSubmission
import com.synapse.social.studioasinc.domain.usecase.about.FeedbackValidationException
import com.synapse.social.studioasinc.domain.usecase.about.FeedbackValidationIssue
import com.synapse.social.studioasinc.domain.usecase.about.SubmitFeedbackUseCase
import com.synapse.social.studioasinc.domain.usecase.about.ValidateFeedbackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

enum class FeedbackSubmissionState {
    Idle,
    InvalidDescription,
    InvalidEmail,
    Sending,
    Sent,
    Failed
}

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    application: Application,
    private val validateFeedbackUseCase: ValidateFeedbackUseCase,
    private val submitFeedbackUseCase: SubmitFeedbackUseCase
) : AndroidViewModel(application) {
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
        val validation = validateFeedbackUseCase(cleanedEmail, description)
        val issue = (validation.exceptionOrNull() as? FeedbackValidationException)?.issue
        if (issue != null) {
            _submissionState.value = when (issue) {
                FeedbackValidationIssue.DescriptionRequired -> FeedbackSubmissionState.InvalidDescription
                FeedbackValidationIssue.InvalidEmail -> FeedbackSubmissionState.InvalidEmail
            }
            return
        }
        if (validation.isFailure) {
            _submissionState.value = FeedbackSubmissionState.Failed
            return
        }

        _submissionState.value = FeedbackSubmissionState.Sending
        viewModelScope.launch {
            try {
                val submission = withContext(Dispatchers.IO) {
                    val app = getApplication<Application>()
                    val packageInfo = app.packageManager.getPackageInfo(app.packageName, 0)
                    val appVersion = packageInfo.versionName ?: app.getString(com.synapse.social.studioasinc.R.string.about_version_unknown)
                    val buildNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toString()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toString()
                    }
                    FeedbackSubmission(
                        category = category,
                        email = cleanedEmail,
                        description = description.trim(),
                        appVersion = appVersion,
                        buildNumber = buildNumber,
                        deviceInfo = buildDeviceInfoPacket(includeDiagnostics)
                    )
                }
                val result = submitFeedbackUseCase(submission)
                _submissionState.value = if (result.isSuccess) FeedbackSubmissionState.Sent else FeedbackSubmissionState.Failed
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _submissionState.value = FeedbackSubmissionState.Failed
            }
        }
    }

    private fun buildDeviceInfoPacket(includeDiagnostics: Boolean): String {
        val packet = JSONObject()
        packet.put("diagnostics_included", includeDiagnostics)
        if (includeDiagnostics) {
            packet.put("device_model", (Build.MANUFACTURER + " " + Build.MODEL).trim())
            packet.put("os_version", "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")")
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
