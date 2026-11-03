package com.synapse.social.studioasinc.shared.domain.usecase.echo

import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRole
import com.synapse.social.studioasinc.shared.domain.repository.EchoRoomRepository

/** Moves a member between the audience and the stage. Host-only, enforced by the RPC. */
class SetEchoRoleUseCase(
    private val repository: EchoRoomRepository
) {
    suspend operator fun invoke(roomId: String, userId: String, role: EchoRole): Result<Unit> {
        if (role == EchoRole.HOST) {
            return Result.failure(IllegalArgumentException("The host role cannot be assigned"))
        }
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("A role change needs a member"))
        }
        return repository.setRole(roomId, userId, role)
    }
}
