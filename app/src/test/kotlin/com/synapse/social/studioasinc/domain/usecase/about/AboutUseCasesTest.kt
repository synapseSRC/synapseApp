package com.synapse.social.studioasinc.domain.usecase.about

import com.synapse.social.studioasinc.domain.model.AppReleaseInfo
import com.synapse.social.studioasinc.domain.model.AppUpdateCheckResult
import com.synapse.social.studioasinc.domain.repository.AppUpdateRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutUseCasesTest {
    @Test
    fun validationRejectsBlankDescriptionAndInvalidEmail() {
        val useCase = ValidateFeedbackUseCase()
        val emptyDescription = useCase(email = "", description = "  ")
        val badEmail = useCase(email = "not-an-email", description = "A report")

        assertEquals(
            FeedbackValidationIssue.DescriptionRequired,
            (emptyDescription.exceptionOrNull() as FeedbackValidationException).issue
        )
        assertEquals(
            FeedbackValidationIssue.InvalidEmail,
            (badEmail.exceptionOrNull() as FeedbackValidationException).issue
        )
    }

    @Test
    fun validationAcceptsOptionalEmailAndValidDescription() {
        assertTrue(ValidateFeedbackUseCase()(email = "", description = "A report").isSuccess)
        assertTrue(ValidateFeedbackUseCase()(email = "person@example.com", description = "A report").isSuccess)
    }

    @Test
    fun updateCheckDistinguishesCurrentAndNewerRelease() = runTest {
        val current = CheckForUpdatesUseCase(FakeAppUpdateRepository(Result.success(AppReleaseInfo("1.0.0", "https://example.test/current"))))
        val newer = CheckForUpdatesUseCase(FakeAppUpdateRepository(Result.success(AppReleaseInfo("1.1.0", "https://example.test/new"))))

        assertEquals(AppUpdateCheckResult.UpToDate, current("1.0.0").getOrThrow())
        assertEquals(
            AppUpdateCheckResult.UpdateAvailable(AppReleaseInfo("1.1.0", "https://example.test/new")),
            newer("1.0.0").getOrThrow()
        )
    }

    @Test
    fun updateCheckPreservesRepositoryFailure() = runTest {
        val result = CheckForUpdatesUseCase(FakeAppUpdateRepository(Result.failure(IllegalStateException("offline"))))("1.0.0")
        assertTrue(result.isFailure)
    }

    private class FakeAppUpdateRepository(
        private val result: Result<AppReleaseInfo>
    ) : AppUpdateRepository {
        override suspend fun getLatestRelease(): Result<AppReleaseInfo> = result
    }
}
