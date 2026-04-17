package com.synapse.social.studioasinc.shared.data.local

import java.io.File
import java.util.Properties

class JvmSecureStorage : SecureStorage {
    private val file = File(System.getProperty("user.home"), ".synapse/prefs.properties")
    private val props = Properties()

    init {
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        if (file.exists()) {
            file.inputStream().use { props.load(it) }
        }
    }

    private fun saveProps() {
        file.outputStream().use { props.store(it, "Synapse Secure Storage") }
    }

    override fun save(key: String, value: String) {
        props.setProperty(key, value)
        saveProps()
    }

    override fun getString(key: String): String? = props.getProperty(key)

    override fun clear(key: String) {
        props.remove(key)
        saveProps()
    }
}
