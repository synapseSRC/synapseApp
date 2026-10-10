package com.synapse.social.studioasinc.core.network

import android.util.Log

object SupabaseErrorHandler {

    fun mapExceptionToMessage(e: Exception): String {
        return when {
            e.message?.contains("relation \"users\" does not exist", ignoreCase = true) == true ->
                "Database table 'users' does not exist. Please create the users table in your Supabase database."
            e.message?.contains("connection", ignoreCase = true) == true ->
                "Cannot connect to Supabase. Check your internet connection and Supabase configuration."
            e.message?.contains("unauthorized", ignoreCase = true) == true ->
                "Unauthorized access to Supabase. Check your API key and RLS policies."
            else -> "Database error: ${e.message}"
        }
    }

    fun <T> toResult(e: Exception, logTag: String, logMessage: String): Result<T> {
        Log.e(logTag, logMessage, e)
        val userFriendlyMessage = mapExceptionToMessage(e)
        return Result.failure(Exception(userFriendlyMessage, e))
    }
}
