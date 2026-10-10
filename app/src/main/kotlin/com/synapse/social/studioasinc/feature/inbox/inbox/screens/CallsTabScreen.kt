package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.feature.inbox.inbox.CallsViewModel
import com.synapse.social.studioasinc.feature.inbox.inbox.components.CallRecordItem
import com.synapse.social.studioasinc.feature.inbox.inbox.components.InboxEmptyState
import com.synapse.social.studioasinc.feature.inbox.inbox.models.EmptyStateType

@Composable
fun CallsTabScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: CallsViewModel = hiltViewModel()
) {
    val callHistory by viewModel.callHistory.collectAsState()

    if (callHistory.isEmpty()) {
        InboxEmptyState(
            type = EmptyStateType.CALLS,
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding
        ) {
            items(callHistory) { record ->
                CallRecordItem(
                    callRecord = record,
                    onCallClick = { userId, isVideo ->
                        viewModel.initiateCall(userId, isVideo)
                    }
                )
            }
        }
    }
}
