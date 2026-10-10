package com.synapse.social.studioasinc.shared.data.adapter

import app.cash.sqldelight.ColumnAdapter
import com.synapse.social.studioasinc.shared.data.model.EncryptedString
import com.synapse.social.studioasinc.shared.security.SecurityCipher

class EncryptedStringAdapter(private val cipher: SecurityCipher) : ColumnAdapter<EncryptedString, String> {
    override fun decode(databaseValue: String): EncryptedString {
        return EncryptedString(cipher.decrypt(databaseValue))
    }

    override fun encode(value: EncryptedString): String {
        return cipher.encrypt(value.value)
    }
}
