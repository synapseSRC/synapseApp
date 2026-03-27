package com.synapse.social.studioasinc.presentation.editprofile.components

import androidx.compose.foundation.layout.padding
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileTopBar(
    isSaving: Boolean,
    canSave: Boolean,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    MediumTopAppBar(
        title = { Text(stringResource(R.string.edit_profile_title)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            if (isSaving) {
                ExpressiveLoadingIndicator(
                    modifier = Modifier.padding(end = Spacing.Medium).size(Sizes.IconLarge)
                )
            } else {
                TextButton(
                    onClick = onSaveClick,
                    enabled = canSave,
                    modifier = Modifier.padding(end = Spacing.Small)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}
