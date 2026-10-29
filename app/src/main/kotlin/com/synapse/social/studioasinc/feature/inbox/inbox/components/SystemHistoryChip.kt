package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.Message

@Composable
fun SystemHistoryChip(message: Message) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space8),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message.content,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = Spacing.space12, vertical = Spacing.space4)
        )
    }
}
