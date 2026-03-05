package com.synapse.social.studioasinc.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for Supabase blocks table.
 * Maps directly to database schema.
 */
@Serializable
data class BlockDTO(
    @SerialName("id")
    val id: String,
    
    @SerialName("blocker_id")
    val blockerId: String,
    
    @SerialName("blocked_id")
    val blockedId: String,
    
    @SerialName("created_at")
    val createdAt: String
)

/**
 * Extended DTO with user profile information.
 * Used when fetching blocked users with their details.
 */
@Serializable
data class BlockWithUserDTO(
    @SerialName("id")
    val id: String,
    
    @SerialName("blocker_id")
    val blockerId: String,
    
    @SerialName("blocked_id")
    val blockedId: String,
    
    @SerialName("created_at")
    val createdAt: String,
    
    @SerialName("blocked_user")
    val blockedUser: UserProfileDTO?
)

/**
 * User profile DTO for joined data in blocking queries.
 */
@Serializable
data class UserProfileDTO(
    @SerialName("uid")
    val uid: String,
    
    @SerialName("username")
    val username: String?,
    
    @SerialName("avatar")
    val avatar: String?
)
