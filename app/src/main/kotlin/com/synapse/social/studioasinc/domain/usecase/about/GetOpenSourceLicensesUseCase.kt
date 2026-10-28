package com.synapse.social.studioasinc.domain.usecase.about

import com.synapse.social.studioasinc.domain.model.OpenSourceLicense
import com.synapse.social.studioasinc.domain.repository.OpenSourceLicenseRepository
import javax.inject.Inject

/** Provides the list of third-party libraries that the open-source licences screen displays. */
class GetOpenSourceLicensesUseCase @Inject constructor(
    private val repository: OpenSourceLicenseRepository
) {
    suspend operator fun invoke(): Result<List<OpenSourceLicense>> = repository.getLicenses()
}
