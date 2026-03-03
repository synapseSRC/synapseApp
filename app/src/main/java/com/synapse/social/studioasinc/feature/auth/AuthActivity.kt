package com.synapse.social.studioasinc

import android.content.Intent
import android.net.Uri
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import com.synapse.social.studioasinc.feature.shared.main.MainActivity
import androidx.lifecycle.ViewModelProvider
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UsernameRepository
import com.synapse.social.studioasinc.feature.auth.ui.AuthScreen
import com.synapse.social.studioasinc.feature.auth.presentation.viewmodel.AuthViewModel
import com.synapse.social.studioasinc.feature.shared.theme.AuthTheme
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint



@AndroidEntryPoint
class AuthActivity : ComponentActivity() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        enableEdgeToEdge()




        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]


        intent?.let { handleDeepLink(it) }

        setContent {
            AuthTheme(enableEdgeToEdge = true) {
                AuthScreen(
                    viewModel = viewModel,
                    onNavigateToMain = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null) {
            viewModel.handleDeepLink(data)
        }
    }
}
