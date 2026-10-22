package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import org.json.JSONObject

private val recipientIdPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
private val encryptedBodyPattern = Regex("^[A-Za-z0-9+/_-]+={0,2}$")

/** Returns true only for the serialized Signal pre-key envelope currently leaking into chat text. */
internal fun isUndecryptableSignalEnvelope(content: String): Boolean {
    val root = runCatching { JSONObject(content) }.getOrNull() ?: return false
    if (root.length() == 0) return false

    val recipients = root.keys()
    while (recipients.hasNext()) {
        val recipientId = recipients.next()
        if (!recipientIdPattern.matches(recipientId)) return false

        val envelope = root.optJSONObject(recipientId) ?: return false
        if (envelope.optInt("type", -1) != 2) return false
        if (!encryptedBodyPattern.matches(envelope.optString("body"))) return false
        if (envelope.optInt("registrationId", -1) <= 0) return false
    }
    return true
}

@Composable
internal fun UndecryptableMessageNotice(
    fontScale: Float,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    var showLearnMore by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.chat_message_decryption_failed),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = contentColor,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale
            )
        )
        TextButton(
            onClick = { showLearnMore = true },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = stringResource(R.string.chat_message_decryption_learn_more),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = MaterialTheme.typography.labelLarge.fontSize * fontScale
                )
            )
        }
    }

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
