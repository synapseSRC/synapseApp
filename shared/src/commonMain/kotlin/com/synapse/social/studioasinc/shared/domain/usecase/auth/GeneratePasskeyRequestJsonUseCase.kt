package com.synapse.social.studioasinc.shared.domain.usecase.auth

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class GeneratePasskeyRequestJsonUseCase {
    @OptIn(ExperimentalEncodingApi::class)
    operator fun invoke(
        challenge: String,
        userId: String,
        userEmail: String,
        rpId: String = "synapse-social.com"
    ): String {
        val userIdEncoded = Base64.UrlSafe.encode(userId.encodeToByteArray()).trim('=')

        return """
        {
            "challenge": "$challenge",
            "rp": {
                "name": "Synapse Social",
                "id": "$rpId"
            },
            "user": {
                "id": "$userIdEncoded",
                "name": "$userEmail",
                "displayName": "$userEmail"
            },
            "pubKeyCredParams": [
                { "type": "public-key", "alg": -7 },
                { "type": "public-key", "alg": -257 }
            ],
            "timeout": 60000,
            "attestation": "direct",
            "authenticatorSelection": {
                "authenticatorAttachment": "platform",
                "requireResidentKey": true,
                "residentKey": "required",
                "userVerification": "required"
            }
        }
        """.trimIndent()
    }
}
