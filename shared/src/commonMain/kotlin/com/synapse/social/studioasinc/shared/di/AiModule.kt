package com.synapse.social.studioasinc.shared.di

import com.synapse.social.studioasinc.shared.data.repository.ai.GeminiAiRepository
import com.synapse.social.studioasinc.shared.domain.repository.ai.AiRepository
import com.synapse.social.studioasinc.shared.domain.usecase.ai.GenerateSmartRepliesUseCase
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val aiModule = module {
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                })
            }
        }
    }
    single<AiRepository> { GeminiAiRepository(get()) }
    single { GenerateSmartRepliesUseCase(get()) }
}
