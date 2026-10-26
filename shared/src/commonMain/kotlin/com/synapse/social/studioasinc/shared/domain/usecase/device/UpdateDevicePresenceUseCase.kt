package com.synapse.social.studioasinc.shared.domain.usecase.device

import com.synapse.social.studioasinc.shared.data.datasource.DeviceRegistrationDataSource

class UpdateDevicePresenceUseCase(
    private val deviceRegistrationDataSource: DeviceRegistrationDataSource
) {
    suspend operator fun invoke(deviceId: String): Result<Unit> {
        return deviceRegistrationDataSource.updateDevicePresence(deviceId)
    }
}
