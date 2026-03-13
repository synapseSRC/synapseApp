package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.synapse.social.studioasinc.feature.shared.main.MainActivity
import com.synapse.social.studioasinc.feature.auth.ui.AuthScreen
import com.synapse.social.studioasinc.feature.auth.presentation.viewmodel.SignInViewModel
import com.synapse.social.studioasinc.core.ui.base.BaseComposeActivity
import dagger.hilt.android.AndroidEntryPoint
import com.synapse.social.studioasinc.core.auth.GoogleAuthHelper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import io.github.aakira.napier.Napier
import com.synapse.social.studioasinc.BuildConfig

@AndroidEntryPoint
class AuthActivity : BaseComposeActivity() {

    private lateinit var viewModel: SignInViewModel
    private lateinit var googleAuthHelper: GoogleAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[SignInViewModel::class.java]
        googleAuthHelper = GoogleAuthHelper(this)

        intent?.let { handleDeepLink(it) }
        
        setAuthContent {
            AuthScreen(
                signInViewModel = viewModel,
                onInitiateGoogleSignIn = {
                    handleGoogleSignIn()
                },
                onNavigateToMain = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            )
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
    
    private fun handleGoogleSignIn() {
        lifecycleScope.launch {
            val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            
            // Check for both blank and placeholder values
            if (clientId.isBlank() || clientId.contains("your-google-web-client-id-here")) {
                val error = "Google Web Client ID not configured. Please set GOOGLE_WEB_CLIENT_ID in gradle.properties to your actual Client ID from Google Cloud Console."
                Napier.e(error, tag = "AuthActivity")
                // Use fallback Log in case Napier isn't fully initialized
                android.util.Log.e("AuthActivity", error)
                android.widget.Toast.makeText(this@AuthActivity, error, android.widget.Toast.LENGTH_LONG).show()
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
                    android.widget.Toast.makeText(this@AuthActivity, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
