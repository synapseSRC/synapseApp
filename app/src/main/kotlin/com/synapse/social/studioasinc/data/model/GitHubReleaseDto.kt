package com.synapse.social.studioasinc.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One entry of the GitHub Releases list response, named exactly as the API spells it.
 *
 * Assets, author, tarball links and the node ids are not modelled because the changelog never uses
 * them; unknown keys are ignored by the shared `json` parser. Defaults keep a partial or renamed
 * response from failing the whole decode.
 */
@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("name") val name: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("draft") val draft: Boolean = false,
    @SerialName("prerelease") val prerelease: Boolean = false
)
