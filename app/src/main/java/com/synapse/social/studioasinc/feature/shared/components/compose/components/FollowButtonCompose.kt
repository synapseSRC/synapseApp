package com.synapse.social.studioasinc.feature.shared.components.compose.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.viewmodel.FollowButtonViewModel

@Composable
fun FollowButtonCompose(
    targetUserId: String,
    modifier: Modifier = Modifier,
    viewModel: FollowButtonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var lastClickTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(targetUserId) {
        viewModel.initialize(targetUserId)
    }

    val handleClick = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > 1000) {
            lastClickTime = currentTime
            viewModel.toggleFollow()
        }
    }

    when {
        uiState.isLoading -> {
            OutlinedButton(
                onClick = { },
                enabled = false,
                modifier = modifier
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp
                )
            }
        }

        uiState.isFollowing -> {
            OutlinedButton(
                onClick = handleClick,
                modifier = modifier,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(stringResource(R.string.following))
            }
        }

        else -> {
            Button(
                onClick = handleClick,
                modifier = modifier
            ) {
                Text(stringResource(R.string.follow))
            }
        }
    }
}
