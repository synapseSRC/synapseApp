package com.synapse.social.studioasinc.shared.data.source.remote

import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImgBBUploadServiceTest {

    @Test
    fun testUploadParsesExactDeleteUrl() = runTest {
        val deleteUrlValue = "https://imgbb.com/delete/12345/abcdef123456"
        val imageUrlValue = "https://i.ibb.co/12345/image.jpg"

        val mockEngine = MockEngine { request ->
            respond(
                content = """
                    {
                        "data": {
                            "url": "$imageUrlValue",
                            "delete_url": "$deleteUrlValue"
                        },
                        "success": true,
                        "status": 200
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val service = ImgBBUploadService(client)
        val config = StorageConfig(imgBBKey = "test_key")

        val result = service.upload(
            fileProvider = { ByteReadChannel("test_data".encodeToByteArray()) },
            fileSize = 9L,
            fileName = "test.jpg",
            config = config,
            onProgress = {}
        )

        assertEquals(imageUrlValue, result.url)
        assertEquals(deleteUrlValue, result.deleteUrl)
    }

    @Test
    fun testDeleteImgBbImageCallsExactDeleteUrl() = runTest {
        val targetDeleteUrl = "https://imgbb.com/delete/abcde/12345"
        val requestedUrls = mutableListOf<String>()

        val mockEngine = MockEngine { request ->
            requestedUrls.add(request.url.toString())
            respond(
                content = "OK",
                status = HttpStatusCode.OK
            )
        }

        val client = HttpClient(mockEngine)
        val service = ImgBBUploadService(client)

        val result = service.deleteImgBbImage(client, targetDeleteUrl)

        assertTrue(result.isSuccess)
        assertEquals(1, requestedUrls.size)
        assertEquals(targetDeleteUrl, requestedUrls.first())
    }

    @Test
    fun testDeleteImgBbImagesContinuesOnIndividualFailure() = runTest {
        val deleteUrls = listOf(
            "https://imgbb.com/delete/1/fail",
            "https://imgbb.com/delete/2/success"
        )
        val attemptedUrls = mutableListOf<String>()

        val mockEngine = MockEngine { request ->
            attemptedUrls.add(request.url.toString())
            if (request.url.toString().contains("fail")) {
                respond(content = "Error", status = HttpStatusCode.BadRequest)
            } else {
                respond(content = "OK", status = HttpStatusCode.OK)
            }
        }

        val client = HttpClient(mockEngine)
        val service = ImgBBUploadService(client)

        service.deleteImgBbImages(client, deleteUrls)

        assertEquals(2, attemptedUrls.size)
        assertEquals(deleteUrls, attemptedUrls)
    }
}
