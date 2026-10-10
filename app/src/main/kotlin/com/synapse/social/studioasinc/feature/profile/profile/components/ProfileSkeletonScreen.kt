package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ProfileSkeletonScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        item {
            ProfileHeaderShimmer()
            ProfileBioShimmer()
        }
        items(3) {
            PostCardSkeleton()
        }
    }
}
