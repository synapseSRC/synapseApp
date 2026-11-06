package com.synapse.social.studioasinc.data.local.database

import android.content.Context
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.synapse.social.studioasinc.shared.domain.repository.MessageDeliveryCoordinator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Persists push catch-up beyond the short notification callback/process lifetime. */
class MessageDeliveryWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DeliveryEntryPoint {
        fun messageDeliveryCoordinator(): MessageDeliveryCoordinator
    }

    override suspend fun doWork(): Result {
        val result = coordinator(applicationContext).synchronize()
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "message_delivery_catch_up"

        fun enqueueIfMessage(context: Context, data: JSONObject?) {
            if (data == null) return
            val navigation = data.optJSONObject("navigation")
            val type = data.optString("type")
            val target = navigation?.optString("target")
            val hasMessageId = listOf("messageId", "message_id").any {
                !data.optString(it).isNullOrBlank() || !navigation?.optString(it).isNullOrBlank()
            }
            if (type.equals("new_message", ignoreCase = true) ||
                type.equals("message", ignoreCase = true) ||
                type.equals("message_delivery_sync", ignoreCase = true) ||
                target.equals("CHAT", ignoreCase = true) || hasMessageId
            ) {
                enqueue(context)
            }
        }

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<MessageDeliveryWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .apply {
                    // Older Android versions need a foreground notification for expedited work.
                    // https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    }
                }
                .build()
            // KEEP would lose a push that arrives after the running worker's final server query.
            // Persist a trailing pass; do not cancel an in-flight receipt with REPLACE.
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            coordinator(context).requestSync()
        }

        private fun coordinator(context: Context): MessageDeliveryCoordinator =
            EntryPointAccessors.fromApplication(context.applicationContext, DeliveryEntryPoint::class.java)
                .messageDeliveryCoordinator()
    }
}
