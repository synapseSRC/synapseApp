package com.synapse.social.studioasinc.shared.core.util

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess

class SharedImageLoader(private val httpClient: HttpClient = HttpClient()) {

    suspend fun loadImageBytes(url: String): ByteArray {
        val response = httpClient.get(url)

        if (response.status.isSuccess()) {
            return response.readRawBytes()
        } else {
            throw Exception("Failed to load image from url: $url, status: ${response.status}")
        }
    }
}
