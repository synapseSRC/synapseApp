package com.synapse.social.studioasinc.shared.data.datasource

import com.synapse.social.studioasinc.shared.core.util.AppDispatchers
import com.synapse.social.studioasinc.shared.data.dto.device.DeviceDto
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient as SupabaseClientLib
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Data source for device registration and management.
 * Handles multi-device synchronization by registering devices and tracking their presence.
 */
class DeviceRegistrationDataSource(private val client: SupabaseClientLib) {
    private fun getCurrentUserId(): String? = runCatching {
        client.pluginManager.getPluginOrNull(io.github.jan.supabase.auth.Auth)?.currentUserOrNull()?.id
    }.getOrNull()

    /**
     * Registers or retrieves an existing device for the current user.
     * Uses the Supabase RPC function get_or_register_device.
     *
     * @param deviceToken Persistent unique identifier for this device
     * @param deviceName Human-readable device name (e.g., "John's iPhone")
     * @param deviceType Platform type (android, ios, web, desktop)
     * @return The device ID (existing or newly created)
     */
    suspend fun registerDevice(
        deviceToken: String,
        deviceName: String? = null,
        deviceType: String? = null
    ): Result<String> = withContext(AppDispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")

            val result = client.postgrest.rpc(
                function = "get_or_register_device",
                parameters = buildJsonObject {
                    put("p_user_id", userId)
                    put("p_device_token", deviceToken)
                    put("p_device_name", deviceName)
                    put("p_device_type", deviceType)
                }
            )

            val deviceId = result.decodeSingleOrNull<String>()
            if (deviceId != null) {
                Napier.d("Device registered successfully: $deviceId", tag = "DeviceRegistration")
                Result.success(deviceId)
            } else {
                Result.failure(Exception("Failed to register device: no ID returned"))
            }
        } catch (e: Exception) {
            Napier.e("Error registering device", e, tag = "DeviceRegistration")
            Result.failure(e)
        }
    }

    /**
     * Updates the last_seen_at timestamp for a device to indicate presence.
     *
     * @param deviceId The device ID to update
     */
    suspend fun updateDevicePresence(deviceId: String): Result<Unit> = withContext(AppDispatchers.IO) {
        try {
            client.postgrest.rpc(
                function = "update_device_presence",
                parameters = buildJsonObject {
                    put("p_device_id", deviceId)
                }
            )
            Napier.d("Device presence updated: $deviceId", tag = "DeviceRegistration")
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error updating device presence", e, tag = "DeviceRegistration")
            Result.failure(e)
        }
    }

    /**
     * Retrieves all devices for the current user.
     *
     * @return List of devices registered to the current user
     */
    suspend fun getUserDevices(): Result<List<DeviceDto>> = withContext(AppDispatchers.IO) {
        try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")

            val devices = client.postgrest.from("user_devices")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<DeviceDto>()

            Napier.d("Retrieved ${devices.size} devices for user", tag = "DeviceRegistration")
            Result.success(devices)
        } catch (e: Exception) {
            Napier.e("Error retrieving user devices", e, tag = "DeviceRegistration")
            Result.failure(e)
        }
    }
}
