package com.synapse.social.studioasinc.shared.domain.usecase

import com.synapse.social.studioasinc.shared.domain.exception.NetworkUnavailableException
import com.synapse.social.studioasinc.shared.domain.model.AppUpdateInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheckForUpdatesUseCaseTest {

    private lateinit var appRepository: FakeAppRepository
    private lateinit var platformInfoProvider: FakePlatformInfoProvider
    private lateinit var useCase: CheckForUpdatesUseCase

    @BeforeTest
    fun setup() {
        appRepository = FakeAppRepository()
        platformInfoProvider = FakePlatformInfoProvider()
        useCase = CheckForUpdatesUseCase(appRepository, platformInfoProvider)
    }

    @Test
    fun invoke_returnsNetworkUnavailableException_whenNetworkIsNotAvailable() = runTest {
        platformInfoProvider.networkAvailableFlag = false
        val result = useCase()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkUnavailableException)
    }

    @Test
    fun invoke_returnsNull_whenAppVersionCodeIsGreaterThanUpdateInfoVersionCode() = runTest {
        platformInfoProvider.currentAppVersionCode = 10
        appRepository.updateInfoResult = Result.success(AppUpdateInfo(versionCode = 5.0))
        val result = useCase()
        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
    }

    @Test
    fun invoke_returnsNull_whenAppVersionCodeIsEqualToUpdateInfoVersionCode() = runTest {
        platformInfoProvider.currentAppVersionCode = 10
        appRepository.updateInfoResult = Result.success(AppUpdateInfo(versionCode = 10.0))
        val result = useCase()
        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
    }

    @Test
    fun invoke_returnsUpdateInfo_whenAppVersionCodeIsLessThanUpdateInfoVersionCode() = runTest {
        platformInfoProvider.currentAppVersionCode = 5
        val updateInfo = AppUpdateInfo(versionCode = 10.0)
        appRepository.updateInfoResult = Result.success(updateInfo)
        val result = useCase()
        assertTrue(result.isSuccess)
        assertEquals(updateInfo, result.getOrNull())
    }

    @Test
    fun invoke_propagatesExceptionFromAppRepository() = runTest {
        val exception = RuntimeException("Repository error")
        appRepository.updateInfoResult = Result.failure(exception)
        val result = useCase()
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

}
