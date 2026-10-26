package com.synapse.social.studioasinc.data.repository

import android.util.LruCache
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject

class UsernameRepositoryImpl {
    private val client = SupabaseClient.client
    private val usernameCache = LruCache<String, Boolean>(100)

    fun clearCache() {
        usernameCache.evictAll()
    }
}
