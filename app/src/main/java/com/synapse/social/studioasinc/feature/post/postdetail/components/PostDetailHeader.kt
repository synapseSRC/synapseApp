package com.synapse.social.studioasinc.feature.post.postdetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.feature.shared.components.post.PostHeader








@Composable
fun PostDetailHeader(
    user: User,
    timestamp: String,
    onUserClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PostHeader(
        user = user,
        timestamp = timestamp,
        onUserClick = onUserClick,
        onOptionsClick = onOptionsClick,
        modifier = modifier
    )
}
