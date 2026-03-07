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
import com.synapse.social.studioasinc.BuildConfig



@AndroidEntryPoint
class AuthActivity : ComponentActivity() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var googleAuthHelper: GoogleAuthHelper

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
            val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            
            // Check for both blank and placeholder values
            if (clientId.isBlank() || clientId.contains("your-google-web-client-id-here")) {
                val error = "Google Web Client ID not configured. Please set GOOGLE_WEB_CLIENT_ID in gradle.properties to your actual Client ID from Google Cloud Console."
                Napier.e(error, tag = "AuthActivity")
                // Use fallback Log in case Napier isn't fully initialized
                android.util.Log.e("AuthActivity", error)
                viewModel.handleGoogleSignInError(error)
                return@launch
            }
            
            Napier.d("Initiating Google Sign-In with Credential Manager. Client ID: ${clientId.take(10)}...", tag = "AuthActivity")
            android.util.Log.d("AuthActivity", "Handling Google Sign-In with Credential Manager...")
            
            googleAuthHelper.signIn(clientId).fold(
                onSuccess = { idToken ->
                    Napier.d("Google ID token received, signing in with Supabase...", tag = "AuthActivity")
                    android.util.Log.d("AuthActivity", "Google ID token received, delegating to ViewModel...")
                    viewModel.handleGoogleIdToken(idToken)
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Google Sign-In failed"
                    Napier.e("Google Sign-In failed: $errorMsg", error, tag = "AuthActivity")
                    android.util.Log.e("AuthActivity", "Google Sign-In failed: $errorMsg", error)
                    viewModel.handleGoogleSignInError(errorMsg)
                }
            )
        }
    }
}
