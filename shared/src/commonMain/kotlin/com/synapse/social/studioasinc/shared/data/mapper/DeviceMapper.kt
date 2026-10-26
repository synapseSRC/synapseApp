package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.data.dto.device.DeviceDto
import com.synapse.social.studioasinc.shared.domain.model.device.Device
import com.synapse.social.studioasinc.shared.domain.model.device.DeviceType

object DeviceMapper {
    fun DeviceDto.toDomain(): Device {
        return Device(
            id = id ?: "",
            userId = userId,
            deviceName = deviceName,
            deviceType = deviceType?.let { parseDeviceType(it) } ?: DeviceType.UNKNOWN,
            deviceToken = deviceToken,
            isActive = isActive,
            lastSeenAt = lastSeenAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun parseDeviceType(type: String): DeviceType {
        return when (type.lowercase()) {
            "android" -> DeviceType.ANDROID
            "ios" -> DeviceType.IOS
            "web" -> DeviceType.WEB
            "desktop" -> DeviceType.DESKTOP
            else -> DeviceType.UNKNOWN
        }
    }

    fun DeviceType.toDtoString(): String {
        return when (this) {
            DeviceType.ANDROID -> "android"
            DeviceType.IOS -> "ios"
            DeviceType.WEB -> "web"
            DeviceType.DESKTOP -> "desktop"
            DeviceType.UNKNOWN -> "unknown"
        }
    }
}
