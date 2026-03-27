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

@Composable
fun UserHeader(
    user: User?,
    privacy: String,
    onPrivacyClick: () -> Unit,
    taggedPeople: List<User> = emptyList(),
    feeling: FeelingActivity? = null,
    location: LocationData? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Medium)
    ) {

        if (user?.avatar != null) {
            AsyncImage(
                model = user.avatar,
                contentDescription = stringResource(R.string.cd_profile_picture),
                modifier = Modifier
                    .size(Sizes.IconGiant)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Filled.Person),
                error = rememberVectorPainter(Icons.Filled.Person)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(Sizes.IconGiant)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.SmallMedium))


        Column {

            val annotatedText = buildAnnotatedString {

                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                    append(user?.displayName ?: user?.username ?: "You")
                }


                if (feeling != null) {
                    append(" is ")
                    append(feeling.emoji)
                    append(" feeling ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(feeling.text)
                    }
                }


                if (taggedPeople.isNotEmpty()) {
                    if (feeling == null) {
                        append(" \u2014 with ")
                    } else {
                        append(" with ")
                    }

                    if (taggedPeople.size == 1) {
                         withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append(taggedPeople[0].displayName ?: taggedPeople[0].username)
                         }
                    } else if (taggedPeople.size == 2) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append(taggedPeople[0].displayName ?: taggedPeople[0].username)
                        }
                        append(" and ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append(taggedPeople[1].displayName ?: taggedPeople[1].username)
                        }
                    } else {
                         withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append(taggedPeople[0].displayName ?: taggedPeople[0].username)
                        }
                        append(" and ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                             append("${taggedPeople.size - 1} others")
                        }
                    }
                }


                if (location != null) {
                    if (feeling == null && taggedPeople.isEmpty()) {
                        append(" is at ")
                    } else {
                        append(" at ")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(location.name)
                    }
                }
            }

            Text(
                text = annotatedText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.Tiny))


            Surface(
                onClick = onPrivacyClick,
                shape = RoundedCornerShape(Sizes.CornerSmall),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.height(Sizes.HeightSmall)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = Spacing.ExtraSmall)
                ) {
                    Icon(
                        imageVector = when(privacy) {
                            "followers" -> Icons.Default.Group
                            "private" -> Icons.Default.Lock
                            else -> Icons.Default.Public
                        },
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.IconSmall),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                    Text(
                        text = privacy.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(Spacing.Tiny))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.IconSemiSmall),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
