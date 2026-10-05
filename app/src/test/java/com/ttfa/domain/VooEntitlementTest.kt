package com.ttfa.domain

import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

class VooEntitlementTest {
    @Test fun signedPurchaseRejectsTamperingAndDifferentKeys() {
        val key = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val json = "{\"productId\":\"voo_mode_unlock\",\"purchaseState\":0}"
        val signature = Signature.getInstance("SHA1withRSA").run { initSign(key.private); update(json.toByteArray()); Base64.getEncoder().encodeToString(sign()) }
        val publicKey = Base64.getEncoder().encodeToString(key.public.encoded)
        assertTrue(usablePlayLicenseKey(publicKey))
        assertTrue(verifyPlaySignature(publicKey, json, signature))
        assertFalse(verifyPlaySignature(publicKey, json.replace("0", "1"), signature))
        val other = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        assertFalse(verifyPlaySignature(Base64.getEncoder().encodeToString(other.public.encoded), json, signature))
    }
    @Test fun invalidOrMissingVerificationConfigurationFailsClosed() {
        assertFalse(usablePlayLicenseKey("PASTE_YOUR_GOOGLE_PLAY_RSA_PUBLIC_KEY_HERE"))
        assertFalse(verifyPlaySignature("", "{}", ""))
        assertFalse(verifyPlaySignature("PLACEHOLDER", "{}", "broken"))
    }
    @Test fun lockedPersonalityNeverRunsVooEvenWithStoredSelection() {
        assertEquals(Personality.CALM, allowedPersonality(Personality.VOO, false))
        RoastIntensity.entries.forEach { intensity ->
            assertEquals(missedTurnMessage(Personality.CALM), coPilotMessage(Personality.VOO, intensity, false))
        }
        assertEquals(Personality.ROAST, allowedPersonality(Personality.ROAST, false))
    }
    @Test fun verifiedUnlockEnablesEveryVooLevel() {
        assertEquals(Personality.VOO, allowedPersonality(Personality.VOO, true))
        val lines = RoastIntensity.entries.map { coPilotMessage(Personality.VOO, it, true) }
        assertEquals(3, lines.toSet().size)
        assertTrue(lines.last().contains("safe route"))
    }
}
