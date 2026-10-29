package com.synapse.social.studioasinc.feature.createpost.scheduling

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.domain.repository.ScheduledPostRepository
import com.synapse.social.studioasinc.domain.usecase.post.SubmitPostUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.datetime.Instant
import java.util.concurrent.TimeUnit

/**
 * Publishes scheduled_posts rows once their scheduled_at has passed.
 * The periodic pass is the safety net for a powered-off device or a killed app;
 * scheduleAt() additionally arms a precise one-off pass for a single post.
 */
class ScheduledPostPublisherWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PublisherEntryPoint {
        fun scheduledPostRepository(): ScheduledPostRepository
        fun submitPostUseCase(): SubmitPostUseCase
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            PublisherEntryPoint::class.java
        )
        val userId = SupabaseAuthenticationService.getInstance(applicationContext).getCurrentUserId()
            ?: return Result.success()

        val repository = entryPoint.scheduledPostRepository()
        val due = repository.getDueScheduledPosts(userId).getOrElse { error ->
            Log.e(TAG, "Could not load due scheduled posts", error)
            return if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        }
        if (due.isEmpty()) return Result.success()

        val submitPost = entryPoint.submitPostUseCase()
        var failures = 0
        due.forEach { scheduled ->
            val result = submitPost(
                requests = listOf(scheduled.postRequest.copy(replyToPostId = null)),
                currentUserId = userId,
                onProgress = {}
            )
            val postId = result.getOrNull()?.firstOrNull()?.id
            if (result.isSuccess && postId != null) {
                repository.markPublished(scheduled.id, postId)
                Log.i(TAG, "Published scheduled post ${scheduled.id} as $postId")
            } else {
                failures++
                val reason = result.exceptionOrNull()?.message ?: "The post was not returned by the server."
                Log.e(TAG, "Failed to publish scheduled post ${scheduled.id}: $reason")
                repository.markFailed(scheduled.id, reason)
            }
        }

        return if (failures > 0 && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "ScheduledPostPublisher"
        private const val PERIODIC_WORK_NAME = "scheduled_post_publisher_periodic"
        private const val ONE_TIME_WORK_PREFIX = "scheduled_post_publisher_"
        private const val PUBLISH_INTERVAL_MINUTES = 15L
        private const val MAX_ATTEMPTS = 3

        fun ensurePeriodicPublish(context: Context) {
            val request = PeriodicWorkRequestBuilder<ScheduledPostPublisherWorker>(
                PUBLISH_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(networkConstraints())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleAt(context: Context, scheduledAt: String) {
            val dueMillis = runCatching { Instant.parse(scheduledAt).toEpochMilliseconds() }.getOrNull() ?: return
            val request = OneTimeWorkRequestBuilder<ScheduledPostPublisherWorker>()
                .setInitialDelay((dueMillis - System.currentTimeMillis()).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                .setConstraints(networkConstraints())
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_PREFIX + scheduledAt,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        private fun networkConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
