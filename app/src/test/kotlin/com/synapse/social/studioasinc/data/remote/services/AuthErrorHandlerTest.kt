package com.synapse.social.studioasinc.data.remote.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException
import java.net.SocketTimeoutException

class AuthErrorHandlerTest {

    @Test
    fun testHandleAuthErrorClassification() {
        val dnsError = AuthErrorHandler.handleAuthError(UnknownHostException("Unable to resolve host"))
        assertEquals(AuthError.DNS_ERROR, dnsError)
        assertEquals("Unable to reach the authentication server. Please check your connection and try again.", AuthErrorHandler.getErrorMessage(dnsError))
        assertTrue(AuthErrorHandler.isRecoverableError(dnsError))

        val timeoutError = AuthErrorHandler.handleAuthError(SocketTimeoutException("Read timed out"))
        assertEquals(AuthError.TIMEOUT_ERROR, timeoutError)
        assertEquals("The authentication server took too long to respond. Please try again.", AuthErrorHandler.getErrorMessage(timeoutError))
        assertTrue(AuthErrorHandler.isRecoverableError(timeoutError))

        val invalidCreds = AuthErrorHandler.handleAuthError(RuntimeException("io.github.jan.supabase.exceptions.HttpRequestException: Invalid login credentials"))
        assertEquals(AuthError.INVALID_CREDENTIALS, invalidCreds)
        assertEquals("Invalid email or password.", AuthErrorHandler.getErrorMessage(invalidCreds))
        assertFalse(AuthErrorHandler.isRecoverableError(invalidCreds))

        val emailUnverified = AuthErrorHandler.handleAuthError(RuntimeException("Email not confirmed"))
        assertEquals(AuthError.EMAIL_NOT_VERIFIED, emailUnverified)
        assertEquals("Please verify your email address before signing in.", AuthErrorHandler.getErrorMessage(emailUnverified))
        assertFalse(AuthErrorHandler.isRecoverableError(emailUnverified))

        val serverError = AuthErrorHandler.handleAuthError(RuntimeException("500 Internal Server Error"))
        assertEquals(AuthError.SERVER_ERROR, serverError)
        assertEquals("The authentication service is temporarily unavailable. Please try again later.", AuthErrorHandler.getErrorMessage(serverError))
        assertTrue(AuthErrorHandler.isRecoverableError(serverError))

        val configError = AuthErrorHandler.handleAuthError(RuntimeException("Supabase credentials not configured"))
        assertEquals(AuthError.SUPABASE_NOT_CONFIGURED, configError)
        assertEquals("Authentication service is unavailable due to a configuration problem.", AuthErrorHandler.getErrorMessage(configError))
        assertFalse(AuthErrorHandler.isRecoverableError(configError))
    }
}
