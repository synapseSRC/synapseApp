package com.synapse.social.studioasinc.ui.createpost

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.LocationData
import com.synapse.social.studioasinc.domain.model.User
import com.synapse.social.studioasinc.domain.model.FeelingActivity

private const val SEARCH_BAR_BACKGROUND_ALPHA = 0.3f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenSearchDialog(
    onDismiss: () -> Unit,
    title: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String = "Search...",
    onDone: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text(title) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (onDone != null) {
                                TextButton(onClick = onDone) {
                                    Text("Done", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    )


                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text(searchPlaceholder) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=SEARCH_BAR_BACKGROUND_ALPHA),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=SEARCH_BAR_BACKGROUND_ALPHA),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { padding ->
            content(padding)
        }
    }
}

@Composable
fun TagPeopleScreen(
    onDismiss: () -> Unit,
    onDone: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<User>,
    selectedUsers: List<User>,
    onToggleUser: (User) -> Unit,
    isLoading: Boolean
) {

    val selectedUserIds = remember(selectedUsers) { selectedUsers.map { it.uid }.toSet() }

    FullScreenSearchDialog(
        onDismiss = onDismiss,
        title = "Tag People",
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onDone = onDone,
        searchPlaceholder = "Search for friends..."
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading) {
                items(5) {
                    UserSkeletonItem()
                }
            } else if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No people found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {


                items(
                    items = searchResults,
                    key = { user -> user.uid }
                ) { user ->
                    val isSelected = selectedUserIds.contains(user.uid)
                    UserSelectionItem(
                        user = user,
                        isSelected = isSelected,
                        onClick = { onToggleUser(user) }
                    )
                }
            }
        }
    }
}

@Composable
fun LocationSelectScreen(
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<LocationData>,
    onLocationSelected: (LocationData) -> Unit,
    isLoading: Boolean
) {
    FullScreenSearchDialog(
        onDismiss = onDismiss,
        title = "Add Location",
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        searchPlaceholder = "Search for places..."
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading) {
                 items(5) {
                     LocationSkeletonItem()
                 }
            } else if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                 item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No locations found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(
                    items = searchResults,
                    key = { location -> location.hashCode() }
                ) { location ->
                    LocationItem(
                        location = location,
                        onClick = {
                            onLocationSelected(location)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FeelingSelectScreen(
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    feelings: List<FeelingActivity>,
    onFeelingSelected: (FeelingActivity) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Moods", "Activities")


    val currentTabType = if (selectedTab == 0) com.synapse.social.studioasinc.domain.model.FeelingType.MOOD else com.synapse.social.studioasinc.domain.model.FeelingType.ACTIVITY
    val filteredFeelings = remember(feelings, selectedTab, searchQuery) {
        feelings.filter {
            it.type == currentTabType &&
            (searchQuery.isBlank() || it.text.contains(searchQuery, ignoreCase = true))
        }
    }

    FullScreenSearchDialog(
        onDismiss = onDismiss,
        title = "How are you feeling?",
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        searchPlaceholder = "Search feelings..."
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                 items(
                     items = filteredFeelings,
                     key = { feeling -> feeling.hashCode() }
                 ) { feeling ->
                     Surface(
                         shape = RoundedCornerShape(8.dp),
                         color = MaterialTheme.colorScheme.surfaceContainer,
                         onClick = {
                             onFeelingSelected(feeling)
                             onDismiss()
                         }
                     ) {
                         Row(
                             modifier = Modifier.padding(16.dp),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             Text(feeling.emoji, style = MaterialTheme.typography.headlineSmall)
                             Spacer(modifier = Modifier.width(12.dp))
                             Text(feeling.text, style = MaterialTheme.typography.bodyMedium)
                         }
                     }
                 }
            }
        }
    }
}





@Composable
fun UserSelectionItem(
    user: User,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (user.avatar != null) {
            AsyncImage(
                model = user.avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_person),
                error = painterResource(R.drawable.ic_person)
            )
        } else {
             Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName ?: user.username ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (user.displayName != null && user.username != null) {
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LocationItem(
    location: LocationData,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = location.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            location.address?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



@Composable
fun UserSkeletonItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(0.6f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .fillMaxWidth(0.4f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun LocationSkeletonItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
         Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(0.5f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .fillMaxWidth(0.8f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            )
        }
    }
}
