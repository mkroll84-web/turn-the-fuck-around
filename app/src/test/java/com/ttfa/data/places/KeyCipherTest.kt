package com.ttfa.data.places

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.KeyGenerator

class KeyCipherTest {
    private fun key() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    @Test fun encryptionRoundTripDoesNotStorePlaintext() {
        val key = key(); val value = "unit-test-value-not-a-google-key"
        val encrypted = KeyCipher.encrypt(value, key)
        assertFalse(encrypted.contains(value)); assertEquals(value, KeyCipher.decrypt(encrypted, key))
        assertNotEquals(encrypted, KeyCipher.encrypt(value, key)) // Fresh nonce for each save.
    }
    @Test fun aDifferentDeviceKeyCannotDecrypt() {
        val encrypted = KeyCipher.encrypt("unit-test-value", key())
        try { KeyCipher.decrypt(encrypted, key()); fail("Wrong device key must not decrypt") } catch (_: Exception) { }
    }
    @Test fun malformedOrTamperedCiphertextIsRejected() {
        val key = key(); val encrypted = KeyCipher.encrypt("unit-test-value", key)
        try { KeyCipher.decrypt("not ciphertext", key); fail("Malformed payload") } catch (_: Exception) { }
        val parts = encrypted.split(":"); val bytes = java.util.Base64.getDecoder().decode(parts[1]); bytes[0] = (bytes[0].toInt() xor 1).toByte()
        try { KeyCipher.decrypt(parts[0] + ":" + java.util.Base64.getEncoder().encodeToString(bytes), key); fail("Authentication failed") } catch (_: Exception) { }
    }
}
