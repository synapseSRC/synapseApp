package com.synapse.social.studioasinc.ui.createpost

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

@Composable
fun CreatePostContent(
    uiState: CreatePostUiState,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
    padding: PaddingValues,
    onPrivacyClick: () -> Unit,
    onUpdateText: (String) -> Unit,
    onRemoveMedia: (Int) -> Unit,
    onEditMedia: (Int) -> Unit,
    onRemovePoll: () -> Unit,
    onRemoveYoutube: () -> Unit,
    onRemoveLocation: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            UserHeader(
                user = uiState.currentUserProfile,
                privacy = uiState.privacy,
                onPrivacyClick = onPrivacyClick,
                taggedPeople = uiState.taggedPeople,
                feeling = uiState.feeling,
                location = uiState.location
            )
        }


        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 120.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    if (uiState.postText.isEmpty()) {
                        Text(
                            text = stringResource(R.string.hint_whats_on_your_mind),
                            style = if (uiState.mediaItems.isEmpty()) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }


                    val textSize = if (uiState.postText.length < 80 && uiState.mediaItems.isEmpty() && uiState.pollData == null) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.bodyLarge
                    }

                    BasicTextField(
                        value = uiState.postText,
                        onValueChange = onUpdateText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = textSize.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = textSize.lineHeight * 1.2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }
            }
        }



        item {

             if (uiState.mediaItems.isNotEmpty()) {
                 MediaPreviewGrid(
                     mediaItems = uiState.mediaItems,
                     onRemove = onRemoveMedia,
                     onEdit = onEditMedia
                 )
             }


             uiState.pollData?.let { poll ->
                 PollPreviewCard(poll = poll, onDelete = onRemovePoll)
             }


             uiState.youtubeUrl?.let { url ->
                  YoutubePreviewCard(url = url, onDelete = onRemoveYoutube)
             }


             uiState.location?.let { loc ->
                 LocationPreviewCard(location = loc, onDelete = onRemoveLocation)
             }
        }



        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
