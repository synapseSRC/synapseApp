package com.synapse.social.studioasinc.domain.usecase.update

import com.synapse.social.studioasinc.shared.domain.exception.NetworkUnavailableException
import com.synapse.social.studioasinc.shared.domain.usecase.CheckForUpdatesUseCase
import javax.inject.Inject

class GetUpdateStateUseCase @Inject constructor(
    private val checkForUpdatesUseCase: CheckForUpdatesUseCase
) {
    suspend operator fun invoke(): UpdateState {
        return checkForUpdatesUseCase()
            .fold(
                onSuccess = { updateInfo ->
                    if (updateInfo != null) {
                        UpdateState.UpdateAvailable(
                            title = updateInfo.title,
                            versionName = updateInfo.versionName,
                            changelog = updateInfo.changelog,
                            updateLink = updateInfo.updateLink,
                            isCancelable = updateInfo.isCancelable
                        )
                    } else {
                        UpdateState.NoUpdate
                    }
                },
                onFailure = { e ->
                    if (e is NetworkUnavailableException) {
                        UpdateState.NoUpdate
                    } else {
                        UpdateState.Error("Update check failed: ${e.message}")
                    }
                }
            )
    }
}

sealed class UpdateState {
    data class UpdateAvailable(val title: String, val versionName: String, val changelog: String, val updateLink: String, val isCancelable: Boolean) : UpdateState()
    object NoUpdate : UpdateState()
    data class Error(val message: String) : UpdateState()
}
