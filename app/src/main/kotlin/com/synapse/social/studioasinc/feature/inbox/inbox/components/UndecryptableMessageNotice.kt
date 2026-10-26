package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.domain.model.chat.Message
import com.synapse.social.studioasinc.shared.domain.util.EncryptedPayloadDetector

/** Returns true for legacy encrypted placeholders and ciphertext payloads. */
internal fun isUndecryptableMessageContent(content: String): Boolean =
    EncryptedPayloadDetector.isUndecryptable(content)

internal fun Message.isUndecryptableMessage(): Boolean =
    EncryptedPayloadDetector.isUndecryptable(content, encryptionFailureReason)

@Composable
internal fun UndecryptableMessageNotice(
    fontScale: Float,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    var showLearnMore by remember { mutableStateOf(false) }
    val linkColor = MaterialTheme.colorScheme.primary
    val notice = buildAnnotatedString {
        append(stringResource(R.string.chat_message_decryption_failed))
        append(" ")
        pushLink(
            LinkAnnotation.Clickable(
                tag = "decryption-help",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline
                    )
                ),
                linkInteractionListener = { showLearnMore = true }
            )
        )
        append(stringResource(R.string.chat_message_decryption_learn_more))
        pop()
    }

    Text(
        text = notice,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = contentColor,
            fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale
        ),
        modifier = modifier
    )

    if (showLearnMore) {
        AlertDialog(
            onDismissRequest = { showLearnMore = false },
            title = { Text(stringResource(R.string.chat_message_decryption_details_title)) },
            text = { Text(stringResource(R.string.chat_message_decryption_details_body)) },
            confirmButton = {
                TextButton(onClick = { showLearnMore = false }) {
                    Text(stringResource(R.string.chat_message_decryption_details_dismiss))
                }
            }
        )
    }
}
