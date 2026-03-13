package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.ui.search.SearchScreen
import com.synapse.social.studioasinc.ui.search.SearchViewModel
import com.synapse.social.studioasinc.core.ui.base.BaseComposeActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : BaseComposeActivity() {

    private var chatMode = false
    private var origin = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chatMode = intent.getBooleanExtra("mode", false) || intent.getStringExtra("mode") == "chat"
        origin = intent.getStringExtra("origin") ?: ""

        setSynapseContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                val viewModel: SearchViewModel = hiltViewModel()

                SearchScreen(
                    viewModel = viewModel,
                    onNavigateToProfile = { uid ->
                        if (chatMode) {
                            Toast.makeText(this@SearchActivity, "Chat feature not implemented", Toast.LENGTH_SHORT).show()
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("synapse://profile/$uid"))
                            intent.setClass(this@SearchActivity, com.synapse.social.studioasinc.feature.shared.main.MainActivity::class.java)
                            startActivity(intent)
                        }
                    },
                    onNavigateToPost = { postId ->
                        val intent = Intent(this, HomeActivity::class.java)
                        intent.putExtra("post_id", postId)
                        startActivity(intent)
                    },
                    onBack = {
                        finish()
                    }
                )
            }
        }
    }
}
