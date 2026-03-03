package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.data.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ReportPostUseCase @Inject constructor(private val repository: ReportRepository) {
    operator fun invoke(postId: String, reason: String, description: String?): Flow<Result<Unit>> = flow {
        emit(repository.createReport(postId, reason, description))
    }
}
