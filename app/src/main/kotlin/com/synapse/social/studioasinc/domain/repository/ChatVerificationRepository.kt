package com.synapse.social.studioasinc.domain.repository

import com.synapse.social.studioasinc.domain.model.ChatVerificationInfo

interface ChatVerificationRepository {
    suspend fun load(chatId: String): Result<ChatVerificationInfo>
    suspend fun setVerified(info: ChatVerificationInfo, verified: Boolean): Result<Unit>
    suspend fun verifyScannedCode(info: ChatVerificationInfo, scannedCode: String): Result<Boolean>
}
