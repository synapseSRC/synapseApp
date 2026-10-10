package com.synapse.social.studioasinc

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.ui.createpost.CreatePostScreen
import com.synapse.social.studioasinc.ui.createpost.CreatePostViewModel
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreatePostActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PREFILL_IMAGE_URL = "extra_prefill_image_url"
    }

    private val viewModel: CreatePostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)



        intent.getStringExtra("type")?.let {
            viewModel.setCompositionType(it)
        }


        intent.getStringExtra("edit_post_id")?.let {
             viewModel.loadPostForEdit(it)
        }

        intent.getStringExtra("scheduled_post_id")?.let {
            viewModel.loadScheduledPostForEditById(it)
        }

        intent.getStringExtra("reply_to_post_id")?.let {
            viewModel.setReplyToPostId(it)
        }

        intent.getStringExtra(EXTRA_PREFILL_IMAGE_URL)?.let {
            viewModel.prefillImageUrl(it)
        }

        setContent {
            SynapseTheme {
                @OptIn(ExperimentalSharedTransitionApi::class)
                SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.animation.AnimatedVisibility(visible = true) {
                        CreatePostScreen(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                            viewModel = viewModel,
                            onNavigateUp = { finish() }
                        )
                    }
                }
            }
        }
    }
}
