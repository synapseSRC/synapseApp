package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkPreview(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("site_name")
    val siteName: String? = null,
    val domain: String? = null
)
