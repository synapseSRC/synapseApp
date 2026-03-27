package com.synapse.social.studioasinc.domain.repository

interface PasskeyManager {
    suspend fun createPasskey(requestJson: String): String?
}
