package com.synapse.social.studioasinc.feature.shared.components

import androidx.lifecycle.ViewModel
import com.synapse.social.studioasinc.shared.domain.usecase.GetLinkMetadataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LinkPreviewViewModel @Inject constructor(
    val getLinkMetadataUseCase: GetLinkMetadataUseCase
) : ViewModel()
