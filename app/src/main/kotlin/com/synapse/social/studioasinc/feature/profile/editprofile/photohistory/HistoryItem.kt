package com.synapse.social.studioasinc.presentation.editprofile.photohistory

data class HistoryItem(
    val id: String,
    val userId: String,
    val imageUrl: String,
    val createdAt: String? = null
)

enum class PhotoType {
    PROFILE, COVER
}
