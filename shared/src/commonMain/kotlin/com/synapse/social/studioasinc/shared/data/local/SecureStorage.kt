package com.synapse.social.studioasinc.shared.data.local

interface SecureStorage {
    fun save(key: String, value: String)
    fun getString(key: String): String?
    fun clear(key: String)
}
