package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.data.model.PasskeyDto
import com.synapse.social.studioasinc.shared.data.model.PasskeySignInSessionDto
import com.synapse.social.studioasinc.shared.domain.model.auth.Passkey
import com.synapse.social.studioasinc.shared.domain.model.auth.PasskeySignInSession
import kotlin.time.Instant

/**
 * Translates backend passkey DTOs into pure Domain models. DTOs never reach Domain or UI.
 */
object PasskeyMapper {

    fun PasskeyDto.toDomain(): Passkey = Passkey(
        id = id,
        deviceName = friendly_name.orBlank(),
        dateAdded = created_at.toEpochMillis(),
        lastUsed = last_used_at?.let { parseEpochMillis(it) }
    )

    fun PasskeySignInSessionDto.toDomain(): PasskeySignInSession = PasskeySignInSession(
        accessToken = access_token,
        refreshToken = refresh_token
    )

    private fun String?.orBlank(): String = this ?: ""

    private fun String?.toEpochMillis(): Long = this
        ?.let { parseEpochMillis(it) }
        ?: 0L

    private fun parseEpochMillis(value: String): Long? =
        Instant.parse(value).toEpochMilliseconds()
}
