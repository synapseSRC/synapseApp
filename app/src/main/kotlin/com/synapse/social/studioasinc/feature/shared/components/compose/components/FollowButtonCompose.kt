package com.synapse.social.studioasinc.feature.shared.components.compose.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.profile.profile.components.AnimatedFollowButton
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.viewmodel.FollowButtonViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FollowButtonCompose(
    targetUserId: String,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FollowButtonViewModel = hiltViewModel(key = "follow-button-$targetUserId")
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var lastClickTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(targetUserId) {
        viewModel.initialize(targetUserId)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMessage ->
            onError(errorMessage)
            viewModel.clearError()
        }
    }

    val handleClick = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > 1000) {
            lastClickTime = currentTime
            viewModel.toggleFollow()
        }
    }

    // Same expressive follow control as the profile header: animated container colour,
    // morphing M3 Expressive shapes, haptics and an in-button spinner.
    AnimatedFollowButton(
        isFollowing = uiState.isFollowing,
        isFollowRequested = uiState.isFollowRequested,
        isPrivate = uiState.isPrivate,
        isLoading = uiState.isLoading,
        onClick = handleClick,
        modifier = modifier.height(Sizes.ButtonHeight)
    )
}
