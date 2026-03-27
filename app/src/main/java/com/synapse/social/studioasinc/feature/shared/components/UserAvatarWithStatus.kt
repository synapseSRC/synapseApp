package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.feature.shared.viewmodel.UserPresenceViewModel

@Composable
fun UserAvatarWithStatus(
    userId: String,
    avatarUrl: String?,
    size: Dp = 40.dp,
    showActiveStatus: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: UserPresenceViewModel = hiltViewModel()
) {
    val isActive by viewModel.observeUserStatus(userId).collectAsState(initial = false)
    
    Box(modifier = modifier) {
        // Avatar
        UserAvatar(
            avatarUrl = avatarUrl,
            size = size,
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Active status indicator
        if (showActiveStatus) {
            ActiveStatusIndicator(
                isActive = isActive,
                modifier = Modifier.align(Alignment.BottomEnd),
                size = (size.value * 0.3f).dp
            )
        }
    }
}
