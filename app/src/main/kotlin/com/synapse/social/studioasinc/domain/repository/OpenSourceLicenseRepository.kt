package com.synapse.social.studioasinc.domain.repository

import com.synapse.social.studioasinc.domain.model.OpenSourceLicense

interface OpenSourceLicenseRepository {
    suspend fun getLicenses(): Result<List<OpenSourceLicense>>
}
