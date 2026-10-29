package com.synapse.social.studioasinc.shared.domain.model.device

/**
 * Represents a device registered to a user account for multi-device synchronization.
 */
data class Device(
    val id: String,
    val userId: String,
    val deviceName: String?,
    val deviceType: DeviceType,
    val deviceToken: String,
    val isActive: Boolean = true,
    val lastSeenAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Type of device platform.
 */
enum class DeviceType {
    ANDROID,
    IOS,
    WEB,
    DESKTOP,
    UNKNOWN
}
