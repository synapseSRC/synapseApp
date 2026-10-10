package com.synapse.social.studioasinc.shared.di

import com.synapse.social.studioasinc.shared.data.repository.SupabaseCommentRepository
import com.synapse.social.studioasinc.shared.domain.repository.CommentRepository
import com.synapse.social.studioasinc.shared.domain.usecase.comment.AddCommentUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.comment.GetCommentsUseCase
import org.koin.dsl.module

val commentModule = module {
    single<CommentRepository> { SupabaseCommentRepository(get()) }
    factory { GetCommentsUseCase(get()) }
    factory { AddCommentUseCase(get()) }
}
