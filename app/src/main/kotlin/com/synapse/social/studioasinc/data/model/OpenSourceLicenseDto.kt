package com.synapse.social.studioasinc.data.model

import kotlinx.serialization.Serializable

/** Wrapper for `assets/about_open_source_licenses.json`; the asset's `comment` key is not modelled. */
@Serializable
data class OpenSourceLicensesAssetDto(
    val schemaVersion: Int = 1,
    val libraries: List<OpenSourceLicenseDto> = emptyList()
)

/** One library notice as authored in the bundled asset. Never leaves the data layer. */
@Serializable
data class OpenSourceLicenseDto(
    val name: String,
    val author: String,
    val licenseId: String,
    val licenseNote: String? = null,
    val repositoryUrl: String
)
