package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Gender {
    @SerialName("male")
    Male,
    @SerialName("female")
    Female,
    @SerialName("hidden")
    Hidden;

    companion object {
        fun fromString(value: String?): Gender {
            return when (value?.lowercase()) {
                "male" -> Male
                "female" -> Female
                else -> Hidden
            }
        }
    }
}
