package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.ChatInfoViewModel
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedContentScreen(
    chatId: String,
    initialTab: Int = 0,
    onNavigateBack: () -> Unit,
    viewModel: ChatInfoViewModel = hiltViewModel()
) {
    LaunchedEffect(chatId) {
        viewModel.loadChatInfo(chatId, "")
    }

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf(
        stringResource(R.string.tab_media),
        stringResource(R.string.tab_links),
        stringResource(R.string.tab_files)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shared_content_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.Large),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_shared_content),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
