package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.AccountInfo
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountInfoScreen(
    viewModel: AccountInfoViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadAccountInfo()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Account Info") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is AccountInfoState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is AccountInfoState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                is AccountInfoState.Success -> {
                    AccountInfoContent(
                        data = state.data,
                        onCopyUserId = { userId ->
                            viewModel.copyUserId(context, userId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AccountInfoContent(
    data: AccountInfo,
    onCopyUserId: (String) -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withZone(ZoneId.systemDefault())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {

        item {
            SettingsSection(title = "Basic Information") {
                SettingsInfoItem(
                    title = "Username",
                    value = "@${data.username}",
                    icon = R.drawable.ic_person
                )
                SettingsDivider()
                SettingsInfoItem(
                    title = "Display Name",
                    value = data.displayName
                )
                SettingsDivider()
                SettingsInfoItem(
                    title = "Email",
                    value = data.email,
                    icon = R.drawable.ic_email_enhanced
                )
                if (!data.phoneNumber.isNullOrBlank()) {
                    SettingsDivider()
                    SettingsInfoItem(
                        title = "Phone",
                        value = data.phoneNumber,
                        icon = R.drawable.ic_phone
                    )
                }
                if (!data.bio.isNullOrBlank()) {
                    SettingsDivider()
                    SettingsInfoItem(
                        title = "Bio",
                        value = data.bio
                    )
                }
            }
        }


        item {
            SettingsSection(title = "Account Status") {
                SettingsInfoItem(
                    title = "Account Type",
                    value = data.accountType.name

                )
                SettingsDivider()
                if (data.isVerified) {
                    SettingsInfoItem(
                        title = "Verification Status",
                        value = "Verified"
                    )
                    SettingsDivider()
                }

                val createdDate = try {
                    if (data.createdAt != null) {

                         java.time.Instant.parse(data.createdAt).atZone(ZoneId.systemDefault()).format(dateFormatter)
                    } else "Unknown"
                } catch (e: Exception) {
                    data.createdAt ?: "Unknown"
                }

                SettingsInfoItem(
                    title = "Joined",
                    value = createdDate
                )

                SettingsDivider()

                val loginDate = data.lastLoginAt?.let {
                    dateFormatter.format(java.time.Instant.ofEpochMilli(it.toEpochMilliseconds()))
                } ?: "Never"

                SettingsInfoItem(
                    title = "Last Login",
                    value = loginDate
                )
            }
        }


        item {
            SettingsSection(title = "Statistics") {
                SettingsInfoItem(title = "Posts", value = data.postsCount.toString())
                SettingsDivider()
                SettingsInfoItem(title = "Followers", value = data.followersCount.toString())
                SettingsDivider()
                SettingsInfoItem(title = "Following", value = data.followingCount.toString())
                SettingsDivider()
                SettingsInfoItem(title = "Stories", value = data.storiesCount.toString())
                SettingsDivider()
                SettingsInfoItem(title = "Reels", value = data.reelsCount.toString())
            }
        }


        item {
            SettingsSection(title = "Technical Info") {
                SettingsInfoItem(
                    title = "User ID",
                    value = data.userId,
                    icon = R.drawable.ic_info,
                    onCopy = { onCopyUserId(data.userId) }
                )
                SettingsDivider()
                SettingsInfoItem(
                    title = "Region",
                    value = data.region
                )
                SettingsDivider()
                SettingsInfoItem(
                    title = "Language",
                    value = data.language
                )
            }
        }
    }
}
