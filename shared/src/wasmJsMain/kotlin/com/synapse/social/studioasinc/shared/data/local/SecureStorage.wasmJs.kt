package com.synapse.social.studioasinc.shared.data.local

import org.w3c.dom.Storage
import kotlinx.browser.window

class WasmJsSecureStorage : SecureStorage {
    private val storage: Storage = window.localStorage

    override fun save(key: String, value: String) {
        storage.setItem(key, value)
    }

    override fun getString(key: String): String? {
        return storage.getItem(key)
    }

    override fun clear(key: String) {
        storage.removeItem(key)
    }
}
