package com.synapse.social.studioasinc.shared.security

class WasmJsSecurityCipher : SecurityCipher {
    override fun encrypt(data: String): String = data
    override fun decrypt(data: String): String = data
}
