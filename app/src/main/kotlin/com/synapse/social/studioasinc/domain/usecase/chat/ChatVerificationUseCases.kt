package com.synapse.social.studioasinc.domain.usecase.chat

import com.synapse.social.studioasinc.domain.model.ChatVerificationInfo
import com.synapse.social.studioasinc.domain.repository.ChatVerificationRepository

class LoadChatVerificationUseCase(private val repository: ChatVerificationRepository) {
    suspend operator fun invoke(chatId: String): Result<ChatVerificationInfo> = repository.load(chatId)
}

class SetChatVerificationUseCase(private val repository: ChatVerificationRepository) {
    suspend operator fun invoke(info: ChatVerificationInfo, verified: Boolean): Result<Unit> = repository.setVerified(info, verified)
}

class VerifyScannedChatCodeUseCase(private val repository: ChatVerificationRepository) {
    suspend operator fun invoke(info: ChatVerificationInfo, scannedCode: String): Result<Boolean> =
        repository.verifyScannedCode(info, scannedCode)
}
