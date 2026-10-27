package com.synapse.social.studioasinc.feature.shared.reels

import android.content.Context
import android.net.Uri
import com.synapse.social.studioasinc.shared.data.repository.ReelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReelUploadManager @Inject constructor(
    private val reelRepository: ReelRepository,
    private val uploadMediaUseCase: com.synapse.social.studioasinc.shared.domain.usecase.UploadMediaUseCase,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress = _uploadProgress.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError = _uploadError.asStateFlow()

    fun clearError() {
        _uploadError.value = null
    }
}
