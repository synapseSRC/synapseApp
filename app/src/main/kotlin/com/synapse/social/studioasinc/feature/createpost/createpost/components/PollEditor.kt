package com.synapse.social.studioasinc.ui.createpost

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.MediaItem
import com.synapse.social.studioasinc.domain.model.MediaType
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.model.FeelingActivity
import com.synapse.social.studioasinc.domain.model.FeelingType
import com.synapse.social.studioasinc.domain.model.LocationData
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.feature.shared.components.ExpressiveButton
import com.synapse.social.studioasinc.feature.shared.components.ButtonVariant
import com.synapse.social.studioasinc.feature.shared.theme.AccentBlue
import com.synapse.social.studioasinc.feature.shared.theme.AccentYellow
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollCreationSheet(
    onDismiss: () -> Unit,
    onCreatePoll: (PollData) -> Unit
) {
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    var duration by remember { mutableIntStateOf(24) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.Large)
                .padding(bottom = Spacing.ExtraLarge)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Text(stringResource(R.string.poll_create_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text(stringResource(R.string.poll_question_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Sizes.CornerDefault)
            )

            options.forEachIndexed { index, option ->
                OutlinedTextField(
                    value = option,
                    onValueChange = { options[index] = it },
                    label = { Text(stringResource(R.string.poll_option_label, index + 1)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Sizes.CornerDefault),
                    trailingIcon = if (options.size > 2) {
                        {
                            IconButton(onClick = { options.removeAt(index) }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove))
                            }
                        }
                    } else null
                )
            }

            if (options.size < 4) {
                TextButton(
                    onClick = { options.add("") },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(stringResource(R.string.poll_add_option))
                }
            }

            Button(
                onClick = {
                    val validOptions = options.filter { it.isNotBlank() }
                    if (question.isNotBlank() && validOptions.size >= 2) {
                        onCreatePoll(PollData(question, validOptions, duration))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = question.isNotBlank() && options.count { it.isNotBlank() } >= 2,
                shape = RoundedCornerShape(Sizes.CornerDefault)
            ) {
                Text(stringResource(R.string.poll_add_to_post), modifier = Modifier.padding(vertical = Spacing.ExtraSmallMedium))
            }
        }
    }
}
