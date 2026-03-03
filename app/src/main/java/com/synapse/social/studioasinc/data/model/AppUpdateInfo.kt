package com.synapse.social.studioasinc.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



@Serializable
data class AppUpdateInfo(
    @SerialName("version_code")
    val versionCode: Long,

    @SerialName("version_name")
    val versionName: String,

    @SerialName("release_notes")
    val releaseNotes: String? = null,

    @SerialName("mandatory")
    val isMandatory: Boolean = false,

    @SerialName("download_url")
    val downloadUrl: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
)
