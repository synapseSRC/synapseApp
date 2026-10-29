package com.synapse.social.studioasinc.domain.usecase.chat
import javax.inject.Inject

import com.synapse.social.studioasinc.domain.model.ChatVerificationInfo
import com.synapse.social.studioasinc.domain.repository.ChatVerificationRepository

class LoadChatVerificationUseCase @Inject constructor(private val repository: ChatVerificationRepository) {
    suspend operator fun invoke(chatId: String): Result<ChatVerificationInfo> = repository.load(chatId)
}

class SetChatVerificationUseCase @Inject constructor(private val repository: ChatVerificationRepository) {
    suspend operator fun invoke(info: ChatVerificationInfo, verified: Boolean): Result<Unit> = repository.setVerified(info, verified)
}

class VerifyScannedChatCodeUseCase @Inject constructor(private val repository: ChatVerificationRepository) {
    suspend operator fun invoke(info: ChatVerificationInfo, scannedCode: String): Result<Boolean> =
        repository.verifyScannedCode(info, scannedCode)
}
