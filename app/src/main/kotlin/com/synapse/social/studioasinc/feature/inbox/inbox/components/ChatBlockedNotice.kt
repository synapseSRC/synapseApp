package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * Stands in for the chat composer while a block is active.
 *
 * The two sides deliberately do not read the same copy. The account that created the block keeps an
 * unblock control; the other participant only learns that messaging is unavailable and is never told
 * who blocked whom. Hiding the composer is a UI safeguard - the messaging layer still refuses the
 * message on its own.
 *
 * @param blockedByMe True when the signed-in account created the block
 * @param onUnblock Invoked when the blocker chooses to unblock; unused for the other side
 * @param modifier Optional modifier for the composable
 */
@Composable
fun ChatBlockedNotice(
    blockedByMe: Boolean,
    onUnblock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Spacing.space16,
                    vertical = Spacing.space12
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    if (blockedByMe) R.string.chat_blocked_notice_blocker
                    else R.string.chat_blocked_notice_other
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (blockedByMe) {
                Spacer(modifier = Modifier.height(Spacing.space8))
                OutlinedButton(onClick = onUnblock) {
                    Text(
                        text = stringResource(R.string.unblock),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatBlockedNoticeBlockerPreview() {
    MaterialTheme {
        ChatBlockedNotice(blockedByMe = true, onUnblock = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatBlockedNoticeBlockedParticipantPreview() {
    MaterialTheme {
        ChatBlockedNotice(blockedByMe = false, onUnblock = {})
    }
}
