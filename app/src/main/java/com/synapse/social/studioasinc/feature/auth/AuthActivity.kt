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
import com.synapse.social.studioasinc.core.auth.GoogleAuthHelper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.aakira.napier.Napier



@AndroidEntryPoint
class AuthActivity : ComponentActivity() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var googleAuthHelper: GoogleAuthHelper
    
    // TODO: Replace with your actual Google OAuth 2.0 Web Client ID from Google Cloud Console
    private val GOOGLE_SERVER_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        googleAuthHelper = GoogleAuthHelper(this)

        intent?.let { handleDeepLink(it) }
        
        observeNavigationEvents()

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
    
    private fun observeNavigationEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { event ->
                    when (event) {
                        is AuthNavigationEvent.InitiateGoogleSignIn -> {
                            handleGoogleSignIn()
                        }
                        else -> {
                            // Other navigation events handled in AuthScreen
                        }
                    }
                }
            }
        }
    }
    
    private fun handleGoogleSignIn() {
        lifecycleScope.launch {
            googleAuthHelper.signIn(GOOGLE_SERVER_CLIENT_ID).fold(
                onSuccess = { idToken ->
                    Napier.d("Google ID token received, signing in...")
                    viewModel.handleGoogleIdToken(idToken)
                },
                onFailure = { error ->
                    Napier.e("Google Sign-In failed: ${error.message}")
                    // Error is handled in ViewModel
                }
            )
        }
    }
}
