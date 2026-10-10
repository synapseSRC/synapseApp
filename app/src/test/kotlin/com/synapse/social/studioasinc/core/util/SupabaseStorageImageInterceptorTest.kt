package com.synapse.social.studioasinc.core.util

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SupabaseStorageImageInterceptorTest {
    private val supabaseUrl = "https://project.supabase.co"
    private val anonKey = "test-anon-key"

    @Test
    fun authenticatedObjectRequestsIncludeSessionAndApiKeyHeaders() {
        val request = imageRequest("/storage/v1/object/authenticated/chat-attachments/message.png")

        val authenticated = request.withSupabaseStorageAuthHeaders(
            supabaseUrl = supabaseUrl,
            accessToken = "user-session-token",
            anonKey = anonKey
        )

        assertNotNull(authenticated)
        assertEquals("Bearer user-session-token", authenticated?.header("Authorization"))
        assertEquals(anonKey, authenticated?.header("apikey"))
    }

    @Test
    fun authenticatedObjectRequestsUseAnonKeyWhenThereIsNoSession() {
        val request = imageRequest("/storage/v1/object/authenticated/chat-attachments/message.png")

        val authenticated = request.withSupabaseStorageAuthHeaders(
            supabaseUrl = supabaseUrl,
            accessToken = null,
            anonKey = anonKey
        )

        assertEquals("Bearer $anonKey", authenticated?.header("Authorization"))
        assertEquals(anonKey, authenticated?.header("apikey"))
    }

    @Test
    fun privateBucketPublicUrlRetriesWithAuthenticatedEndpointAndSessionHeaders() {
        val request = imageRequest("/storage/v1/object/public/avatars/user/profile.png")
        val publicFailure = response(request, code = 400)
        val authenticatedSuccess = response(request, code = 200)
        val chain = mock<Interceptor.Chain>()
        whenever(chain.request()).thenReturn(request)
        whenever(chain.proceed(any())).thenReturn(publicFailure, authenticatedSuccess)

        val interceptor = SupabaseStorageImageInterceptor(supabaseUrl, anonKey) {
            "user-session-token"
        }
        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        val requests = argumentCaptor<Request>()
        verify(chain, times(2)).proceed(requests.capture())
        assertTrue(requests.allValues[0].url.encodedPath.contains("/object/public/"))
        assertTrue(requests.allValues[1].url.encodedPath.contains("/object/authenticated/"))
        assertEquals("Bearer user-session-token", requests.allValues[1].header("Authorization"))
        assertEquals(anonKey, requests.allValues[1].header("apikey"))
    }

    @Test
    fun publicBucketSuccessDoesNotTriggerAuthenticatedRetry() {
        val request = imageRequest("/storage/v1/object/public/avatars/user/profile.png")
        val publicSuccess = response(request, code = 200)
        val chain = mock<Interceptor.Chain>()
        whenever(chain.request()).thenReturn(request)
        whenever(chain.proceed(any())).thenReturn(publicSuccess)

        val interceptor = SupabaseStorageImageInterceptor(supabaseUrl, anonKey) { null }
        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        verify(chain, times(1)).proceed(request)
    }

    @Test
    fun otherHostsAndNonStorageUrlsAreNeverGivenSupabaseCredentials() {
        val externalRequest = Request.Builder()
            .url("https://images.example.com/storage/v1/object/authenticated/bucket/file.png")
            .build()
        val nonStorageRequest = imageRequest("/storage/v1/render/image/public/avatars/user.png")

        assertNull(externalRequest.withSupabaseStorageAuthHeaders(supabaseUrl, "user-token", anonKey))
        assertNull(nonStorageRequest.withSupabaseStorageAuthHeaders(supabaseUrl, "user-token", anonKey))
        assertNull(externalRequest.url.supabaseStorageObjectEndpoint(supabaseUrl))
    }

    private fun imageRequest(path: String): Request = Request.Builder()
        .url("$supabaseUrl$path")
        .build()

    private fun response(request: Request, code: Int): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(if (code == 200) "OK" else "Storage access denied")
        .body(ByteArray(0).toResponseBody())
        .build()
}
