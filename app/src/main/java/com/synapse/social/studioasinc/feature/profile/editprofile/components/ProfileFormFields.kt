package com.synapse.social.studioasinc.presentation.editprofile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.presentation.editprofile.UsernameValidation
import com.synapse.social.studioasinc.ui.settings.SettingsColors
import com.synapse.social.studioasinc.ui.settings.SettingsShapes
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun ProfileFormFields(
    username: String,
    onUsernameChange: (String) -> Unit,
    usernameValidation: UsernameValidation,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    nicknameError: String?,
    bio: String,
    onBiographyChange: (String) -> Unit,
    bioError: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Sizes.CornerExtraLarge),
        color = SettingsColors.cardBackground,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium)
        ) {

            UsernameField(
                value = username,
                onValueChange = onUsernameChange,
                validation = usernameValidation
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))


            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                label = { Text(stringResource(R.string.display_name)) },
                placeholder = { Text(stringResource(R.string.display_name_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = null)
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
                shape = SettingsShapes.inputShape,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))


            OutlinedTextField(
                value = bio,
                onValueChange = onBiographyChange,
                label = { Text(stringResource(R.string.biography)) },
                placeholder = { Text(stringResource(R.string.bio_hint)) },
                supportingText = {
                    val currentLength = bio.length
                    Text(
                        text = if (bioError != null) bioError else "$currentLength/250",
                        color = if (bioError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                isError = bioError != null,
                modifier = Modifier.fillMaxWidth(),
                shape = SettingsShapes.inputShape,
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text
                )
            )
        }
    }
}

@Composable
fun UsernameField(
    value: String,
    onValueChange: (String) -> Unit,
    validation: UsernameValidation
) {
    val isError = validation is UsernameValidation.Error
    val errorMessage = if (validation is UsernameValidation.Error) validation.message else null

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.nickname)) },
        placeholder = { Text(stringResource(R.string.username_hint)) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Person, contentDescription = null)
        },
        trailingIcon = {
            when (validation) {
                is UsernameValidation.Checking -> {
                    ExpressiveLoadingIndicator(
                        modifier = Modifier.size(Sizes.IconDefault)
                    )
                }
                is UsernameValidation.Valid -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Valid",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Sizes.IconDefault)
                    )
                }
                is UsernameValidation.Error -> {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Sizes.IconDefault)
                    )
                }
            }
        },
        supportingText = {
            if (errorMessage != null) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            } else {
                Text(stringResource(R.string.username_helper))
            }
        },
        isError = isError,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsShapes.inputShape,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            keyboardType = KeyboardType.Ascii
        ),
        singleLine = true
    )
}

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = icon?.let { { Icon(imageVector = it, contentDescription = null) } },
        modifier = modifier.fillMaxWidth(),
        shape = SettingsShapes.inputShape,
        singleLine = true
    )
}

@Composable
fun LocationFields(
    currentCity: String, onCurrentCityChange: (String) -> Unit,
    hometown: String, onHometownChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Sizes.CornerExtraLarge),
        color = SettingsColors.cardBackground,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(stringResource(R.string.section_location), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = Spacing.Medium))
            ProfileTextField(currentCity, onCurrentCityChange, "Current City", "e.g., New York, NY", Icons.Filled.LocationOn)
            Spacer(modifier = Modifier.height(Spacing.Medium))
            ProfileTextField(hometown, onHometownChange, "Hometown", "e.g., Los Angeles, CA", Icons.Filled.LocationOn)
        }
    }
}

@Composable
fun WorkAndEducationFields(
    occupation: String, onOccupationChange: (String) -> Unit,
    workplace: String, onWorkplaceChange: (String) -> Unit,
    education: String, onEducationChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Sizes.CornerExtraLarge),
        color = SettingsColors.cardBackground,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(stringResource(R.string.section_work_education), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = Spacing.Medium))
            ProfileTextField(occupation, onOccupationChange, "Occupation", "e.g., Software Engineer")
            Spacer(modifier = Modifier.height(Spacing.Medium))
            ProfileTextField(workplace, onWorkplaceChange, "Workplace", "e.g., Google")
            Spacer(modifier = Modifier.height(Spacing.Medium))
            ProfileTextField(education, onEducationChange, "Education", "e.g., Stanford University")
        }
    }
}

@Composable
fun SocialLinksFields(
    discordTag: String, onDiscordTagChange: (String) -> Unit,
    githubProfile: String, onGithubProfileChange: (String) -> Unit,
    personalWebsite: String, onPersonalWebsiteChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Sizes.CornerExtraLarge),
        color = SettingsColors.cardBackground,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(stringResource(R.string.section_social_web), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = Spacing.Medium))
            ProfileTextField(discordTag, onDiscordTagChange, "Discord Tag", "e.g., User#1234")
            Spacer(modifier = Modifier.height(Spacing.Medium))
            ProfileTextField(githubProfile, onGithubProfileChange, "GitHub", "e.g., github.com/username")
            Spacer(modifier = Modifier.height(Spacing.Medium))
            ProfileTextField(personalWebsite, onPersonalWebsiteChange, "Website", "e.g., https://mysite.com")
        }
    }
}
