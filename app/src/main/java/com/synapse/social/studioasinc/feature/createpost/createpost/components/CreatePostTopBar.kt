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
fun CreatePostTopBar(
    isEditMode: Boolean,
    isLoading: Boolean,
    postText: String,
    mediaItemsCount: Int,
    hasPoll: Boolean,
    onNavigateUp: () -> Unit,
    onSubmitPost: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = if (isEditMode) stringResource(R.string.title_edit_post) else stringResource(R.string.title_create_post),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
            }
        },
        actions = {
            val isEnabled = !isLoading && (
                postText.isNotBlank() ||
                mediaItemsCount > 0 ||
                hasPoll
            )
            val buttonText = if (isLoading) stringResource(R.string.button_posting) else stringResource(R.string.post)
            ExpressiveButton(
                onClick = onSubmitPost,
                enabled = isEnabled,
                text = buttonText,
                variant = ButtonVariant.Filled,
                modifier = Modifier.height(Sizes.HeightMedium)
            )
            Spacer(modifier = Modifier.width(Spacing.Medium))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
