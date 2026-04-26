package com.synapse.social.studioasinc.shared.data.crypto

import com.synapse.social.studioasinc.shared.data.crypto.models.EncryptedMessage
import com.synapse.social.studioasinc.shared.data.crypto.models.PreKeyBundle
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosSignalProtocolManagerTest {

    @Test
    fun testKeyGenerationAndRoundtrip() = runBlocking {
        val alice = IosSignalProtocolManager()
        val bob = IosSignalProtocolManager()

        // 1. Generate keys
        val aliceKeys = alice.generateIdentityAndKeys()
        val bobKeys = bob.generateIdentityAndKeys()

        assertNotNull(aliceKeys.identityKey)
        assertNotNull(bobKeys.identityKey)

        // 2. Create bundle for Bob
        val bobBundle = PreKeyBundle(
            registrationId = bobKeys.registrationId,
            deviceId = 1,
            preKeyId = 1,
            preKeyPublic = bob.generateOneTimePreKeys(1, 1).first().publicKey,
            signedPreKeyId = bobKeys.signedPreKeyId,
            signedPreKeyPublic = bobKeys.signedPreKey,
            signedPreKeySignature = bobKeys.signedPreKeySignature,
            identityKey = bobKeys.identityKey
        )

        // 3. Alice processes Bob's bundle
        alice.processPreKeyBundle("bob", bobBundle)
        assertTrue(alice.hasSession("bob"))

        // Create bundle for Alice
        val aliceBundle = PreKeyBundle(
            registrationId = aliceKeys.registrationId,
            deviceId = 1,
            preKeyId = 1,
            preKeyPublic = alice.generateOneTimePreKeys(1, 1).first().publicKey,
            signedPreKeyId = aliceKeys.signedPreKeyId,
            signedPreKeyPublic = aliceKeys.signedPreKey,
            signedPreKeySignature = aliceKeys.signedPreKeySignature,
            identityKey = aliceKeys.identityKey
        )

        // Bob processes Alice's bundle
        bob.processPreKeyBundle("alice", aliceBundle)
        assertTrue(bob.hasSession("alice"))

        // 4. Encrypt and Decrypt message
        val message = "Hello, Bob!".encodeToByteArray()
        val encryptedMessage = alice.encryptMessage("bob", message)

        val decryptedBytes = bob.decryptMessage("alice", encryptedMessage)
        val decryptedString = decryptedBytes.decodeToString()

        assertEquals("Hello, Bob!", decryptedString)

        // Cleanup
        alice.deleteAllSessions()
        bob.deleteAllSessions()
    }
}
