package com.synapse.social.studioasinc.presentation.editprofile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.presentation.editprofile.UsernameValidation
import com.synapse.social.studioasinc.ui.settings.SettingsCard
import com.synapse.social.studioasinc.ui.settings.SettingsHeaderItem
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun BasicInformationSection(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    nicknameError: String?,
    username: String,
    onUsernameChange: (String) -> Unit,
    usernameValidation: UsernameValidation,
    bio: String,
    onBiographyChange: (String) -> Unit,
    bioError: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsHeaderItem(title = stringResource(R.string.section_basic_information))

        SettingsCard {
            Column(modifier = Modifier.padding(Spacing.Medium)) {
                // Display Name
                OutlinedTextField(
                    value = nickname,
                    onValueChange = onNicknameChange,
                    label = { Text(stringResource(R.string.display_name)) },
                    placeholder = { Text(stringResource(R.string.display_name_hint)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Badge, contentDescription = null)
                    },
                    supportingText = {
                        if (nicknameError != null) {
                            Text(nicknameError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(stringResource(R.string.display_name_helper))
                        }
                    },
                    isError = nicknameError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(Spacing.Small))

                // Username
                UsernameField(
                    value = username,
                    onValueChange = onUsernameChange,
                    validation = usernameValidation
                )

                Spacer(modifier = Modifier.height(Spacing.Small))

                // Bio
                OutlinedTextField(
                    value = bio,
                    onValueChange = onBiographyChange,
                    label = { Text(stringResource(R.string.biography)) },
                    placeholder = { Text(stringResource(R.string.bio_hint)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Notes, contentDescription = null)
                    },
                    supportingText = {
                        val currentLength = bio.length
                        Text(
                            text = bioError ?: "$currentLength/250",
                            color = if (bioError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    isError = bioError != null,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        }
    }
}
