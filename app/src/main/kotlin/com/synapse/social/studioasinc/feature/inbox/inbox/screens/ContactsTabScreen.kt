package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.ContactsViewModel
import com.synapse.social.studioasinc.feature.inbox.inbox.components.ContactItem
import com.synapse.social.studioasinc.feature.inbox.inbox.components.InboxEmptyState
import com.synapse.social.studioasinc.feature.inbox.inbox.models.EmptyStateType
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.feature.shared.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsTabScreen(
    onNavigateToChat: (String, String?, String?, String?) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchContacts(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                placeholder = { Text(stringResource(R.string.search_contacts_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.settings_search_settings_placeholder)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (contacts.isEmpty() && searchQuery.isBlank()) {
                InboxEmptyState(
                    type = EmptyStateType.CONTACTS,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (contacts.isEmpty()) {
                InboxEmptyState(
                    type = EmptyStateType.SEARCH_NO_RESULTS,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val groupedContacts = contacts.groupBy {
                    (it.displayName ?: it.username)?.firstOrNull()?.uppercaseChar() ?: '#'
                }.toSortedMap(compareBy<Char> { if (it == '#') 1 else 0 }.thenBy { it })

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + Sizes.HeightLarge) // extra space for FAB
                ) {
                    groupedContacts.forEach { (initial, contactsForInitial) ->
                        stickyHeader {
                            Text(
                                text = initial.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.Large, vertical = Spacing.Small)
                            )
                        }

                        items(contactsForInitial, key = { it.uid }) { contact ->
                            ContactItem(
                                contact = contact,
                                onContactClick = { user ->
                                    onNavigateToChat(user.uid, user.uid, user.displayName ?: user.username, user.avatar)
                                },
                                onContactLongClick = { user ->
                                    // Placeholder for options
                                },
                                onCallClick = { user, isVideo ->
                                    // Placeholder for calling
                                }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onNavigateToSearch,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = contentPadding.calculateBottomPadding() + Spacing.Medium, end = Spacing.Medium)
        ) {
            Icon(Icons.Default.GroupAdd, contentDescription = stringResource(R.string.find_friends))
        }
    }
}
