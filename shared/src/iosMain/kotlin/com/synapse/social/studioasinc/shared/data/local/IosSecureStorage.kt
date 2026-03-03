package com.synapse.social.studioasinc.shared.data.local

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.Security.*
import platform.darwin.OSStatus

@OptIn(ExperimentalForeignApi::class)
class IosSecureStorage : SecureStorage {

    private fun serviceName(): String = "com.synapse.social.studioasinc.shared"

    override fun save(key: String, value: String) {
        val query = keyChainQuery(key)
        val data = value.toNSData()

        val attributesToUpdate = NSMutableDictionary.create()
        attributesToUpdate.setObject(data, forKey = kSecValueData)

        val status = SecItemUpdate(query as CFDictionaryRef?, attributesToUpdate as CFDictionaryRef?)

        if (status == errSecItemNotFound) {
            val addQuery = keyChainQuery(key)
            addQuery.setObject(data, forKey = kSecValueData)
            val addStatus = SecItemAdd(addQuery as CFDictionaryRef?, null)
            if (addStatus != errSecSuccess) {
                 throw RuntimeException("Failed to add to Keychain: $addStatus")
            }
        } else if (status != errSecSuccess) {
             throw RuntimeException("Failed to update Keychain: $status")
        }
    }

    override fun getString(key: String): String? {
        val query = keyChainQuery(key)
        query.setObject(kCFBooleanTrue, forKey = kSecReturnData)
        query.setObject(kSecMatchLimitOne, forKey = kSecMatchLimit)

        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef?, result.ptr)
            if (status == errSecSuccess) {
                val data = result.value

                val nsData = data?.let { CFBridgingRelease(it) as? NSData }
                nsData?.let {
                    NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString()
                }
            } else {
                null
            }
        }
    }

    override fun clear(key: String) {
        val query = keyChainQuery(key)
        val status = SecItemDelete(query as CFDictionaryRef?)
        if (status != errSecSuccess && status != errSecItemNotFound) {
            throw RuntimeException("Failed to delete from Keychain: $status")
        }
    }

    private fun keyChainQuery(key: String): NSMutableDictionary {
        val query = NSMutableDictionary.create()
        query.setObject(kSecClassGenericPassword, forKey = kSecClass)
        query.setObject(serviceName(), forKey = kSecAttrService)
        query.setObject(key, forKey = kSecAttrAccount)
        return query
    }

    private fun String.toNSData(): NSData {
        return NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)!!
    }
}
