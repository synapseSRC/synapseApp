package com.synapse.social.studioasinc.feature.post.postdetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReplyingToBottomSheet(
    usernames: List<String>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.space16)
                .padding(bottom = Spacing.space24)
        ) {
            Text(
                text = stringResource(R.string.post_replying_to),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = Spacing.space8)
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.space8),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space8),
                verticalArrangement = Arrangement.spacedBy(Spacing.space8)
            ) {
                usernames.forEach { username ->
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = stringResource(R.string.common_at_username, username),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        border = null
                    )
                }
            }
        }
    }
}