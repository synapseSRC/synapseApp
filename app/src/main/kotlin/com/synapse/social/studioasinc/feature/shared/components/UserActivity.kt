package com.synapse.social.studioasinc

import com.synapse.social.studioasinc.data.remote.services.SupabaseDatabaseService
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch



object UserActivity {

    private val dbService = SupabaseDatabaseService()



    @JvmStatic
    fun setActivity(uid: String, activity: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updateData = mapOf(
                    "status" to activity,
                    "last_seen" to System.currentTimeMillis().toString()
                )
                dbService.update("users", updateData, "uid", uid)
            } catch (e: Exception) {

            }
        }
    }



    @JvmStatic
    fun clearActivity(uid: String) {
        setActivity(uid, "online")
    }



    @JvmStatic
    fun setOffline(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updateData = mapOf(
                    "status" to "offline",
                    "last_seen" to System.currentTimeMillis().toString()
                )
                dbService.update("users", updateData, "uid", uid)
            } catch (e: Exception) {

            }
        }
    }



    suspend fun getActivity(uid: String): String? {
        return try {
            val result = dbService.getSingle("users", "uid", uid).getOrNull()
            result?.get("status") as? String
        } catch (e: Exception) {
            null
        }
    }



    suspend fun isOnline(uid: String): Boolean {
        val status = getActivity(uid)
        return status == "online" || status?.startsWith("chatting_with_") == true
    }



    suspend fun isChattingWith(uid: String, otherUid: String): Boolean {
        val status = getActivity(uid)
        return status == "chatting_with_$otherUid"
    }
}
