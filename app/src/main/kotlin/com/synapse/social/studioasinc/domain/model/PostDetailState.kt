package com.synapse.social.studioasinc.domain.model



sealed class PostDetailState {


    object Loading : PostDetailState()



    data class Success(val postDetail: PostDetail) : PostDetailState()



    data class Error(val message: String, val throwable: Throwable? = null) : PostDetailState()



    object NotFound : PostDetailState()
}
