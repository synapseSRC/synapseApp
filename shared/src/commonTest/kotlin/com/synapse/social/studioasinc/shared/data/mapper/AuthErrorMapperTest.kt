package com.synapse.social.studioasinc.shared.data.mapper

import com.synapse.social.studioasinc.shared.core.network.ConfigurationException
import com.synapse.social.studioasinc.shared.domain.model.AuthError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class AuthErrorMapperTest {

    @Test
    fun testUnknownHostExceptionMapsToDnsResolutionError() {
        val originalCause = RuntimeException("java.net.UnknownHostException: Unable to resolve host")
        val authError = AuthErrorMapper.mapException(originalCause)

        assertIs<AuthError.DnsResolutionError>(authError)
        assertEquals("Unable to reach the authentication server. Please check your connection and try again.", authError.message)
        assertSame(originalCause, authError.cause)
    }

    @Test
    fun testTimeoutExceptionMapsToTimeoutError() {
        val originalCause = RuntimeException("java.net.SocketTimeoutException: Read timed out")
        val authError = AuthErrorMapper.mapException(originalCause)

        assertIs<AuthError.Timeout>(authError)
        assertEquals("The authentication server took too long to respond. Please try again.", authError.message)
        assertSame(originalCause, authError.cause)
    }

    @Test
    fun testConnectionExceptionMapsToConnectionError() {
        val originalCause = RuntimeException("java.net.ConnectException: Failed to connect to server")
        val authError = AuthErrorMapper.mapException(originalCause)

        assertIs<AuthError.ConnectionError>(authError)
        assertEquals("Unable to reach the authentication server. Please check your connection and try again.", authError.message)
        assertSame(originalCause, authError.cause)
    }

    @Test
    fun testInvalidCredentialsMessageMapsToInvalidCredentials() {
        val exception = RuntimeException("Invalid login credentials")
        val authError = AuthErrorMapper.mapException(exception)

        assertIs<AuthError.InvalidCredentials>(authError)
        assertEquals("Invalid email or password.", authError.message)
        assertSame(exception, authError.cause)
    }

    @Test
    fun testEmailNotConfirmedMessageMapsToEmailNotVerified() {
        val exception = RuntimeException("Email not confirmed")
        val authError = AuthErrorMapper.mapException(exception)

        assertIs<AuthError.EmailNotVerified>(authError)
        assertEquals("Please verify your email address before signing in.", authError.message)
        assertSame(exception, authError.cause)
    }

    @Test
    fun testServerError500MessageMapsToServerError() {
        val exception = RuntimeException("500 Internal Server Error")
        val authError = AuthErrorMapper.mapException(exception)

        assertIs<AuthError.ServerError>(authError)
        assertEquals("The authentication service is temporarily unavailable. Please try again later.", authError.message)
        assertSame(exception, authError.cause)
    }

    @Test
    fun testConfigurationExceptionMapsToConfigurationError() {
        val configException = ConfigurationException("Supabase credentials not configured")
        val authError = AuthErrorMapper.mapException(configException)

        assertIs<AuthError.ConfigurationError>(authError)
        assertEquals("Authentication service is unavailable due to a configuration problem.", authError.message)
        assertSame(configException, authError.cause)
    }

    @Test
    fun testHttpRequestExceptionUnwrapsCause() {
        val innerDnsCause = RuntimeException("java.net.UnknownHostException: Unable to resolve host")
        val outerException = RuntimeException("HttpRequestException failed", innerDnsCause)
        val authError = AuthErrorMapper.mapException(outerException)

        assertIs<AuthError.DnsResolutionError>(authError)
        assertEquals("Unable to reach the authentication server. Please check your connection and try again.", authError.message)
        assertSame(innerDnsCause, authError.cause)
    }
}
