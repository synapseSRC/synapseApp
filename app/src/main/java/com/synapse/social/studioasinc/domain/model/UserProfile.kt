package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val uid: String,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    val email: String? = null,
    val bio: String? = null,
    val avatar: String? = null,
    @SerialName("profile_cover_image") val profileCoverImage: String? = null,
    @SerialName("followers_count") val followersCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("posts_count") val postsCount: Int = 0,
    @SerialName("status") val status: UserStatus = UserStatus.OFFLINE,
    @SerialName("account_type") val account_type: String = "user",
    val gender: Gender? = null,
    val region: String? = null,
    val verify: Boolean = false,
    val banned: Boolean = false,
    
    // Address & Location
    @SerialName("current_city") val currentCity: String? = null,
    @SerialName("hometown") val hometown: String? = null,
    
    // Work & Education
    @SerialName("occupation") val occupation: String? = null,
    @SerialName("workplace") val workplace: String? = null,
    @SerialName("education") val education: String? = null,
    @SerialName("professional_skills") val professionalSkills: List<String> = emptyList(),
    
    // Identity & Personal
    @SerialName("pronouns") val pronouns: String? = null,
    @SerialName("birthday") val birthday: String? = null,
    @SerialName("zodiac_sign") val zodiacSign: String? = null,
    @SerialName("languages_spoken") val languagesSpoken: List<String> = emptyList(),
    @SerialName("relationship_status") val relationshipStatus: String? = null,
    
    // Social Links & Gaming
    @SerialName("discord_tag") val discordTag: String? = null,
    @SerialName("github_profile") val githubProfile: String? = null,
    @SerialName("gaming_ids") val gamingIds: Map<String, String> = emptyMap(),
    @SerialName("social_links") val socialLinks: Map<String, String> = emptyMap(),
    @SerialName("personal_website") val personalWebsite: String? = null,

    // Travel & Lifestyle
    @SerialName("bucket_list") val bucketList: List<String> = emptyList(),
    @SerialName("visited_places") val visitedPlaces: List<String> = emptyList(),
    @SerialName("pet_info") val petInfo: String? = null,
    @SerialName("current_learning") val currentLearning: String? = null,
    
    // Contact & Privacy Preferences
    @SerialName("public_email") val publicEmail: String? = null,
    @SerialName("public_phone") val publicPhone: String? = null,
    @SerialName("preferred_communication") val preferredCommunication: String? = null,

    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val isVerified: Boolean get() = verify
    val isPremium: Boolean get() = account_type == "premium"
    val safeGender: Gender get() = gender ?: Gender.Hidden
}
