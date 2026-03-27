package com.synapse.social.studioasinc

import kotlinx.serialization.Serializable



data class UserMention(
    val uid: String,
    val username: String,
    val displayName: String? = null,
    val avatar: String? = null,
    val startIndex: Int = 0,
    val endIndex: Int = 0
) {


    fun getMentionText(): String = "@$username"



    fun getDisplayText(): String = displayName ?: username
}
