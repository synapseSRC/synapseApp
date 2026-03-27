package com.synapse.social.studioasinc.shared.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateInfo(
    @SerialName("versionCode")
    val versionCode: Double = 0.0,
    val versionName: String = "",
    val title: String = "",
    @SerialName("whatsNew")
    val changelog: String = "",
    val updateLink: String = "",
    val isCancelable: Boolean = false
)
