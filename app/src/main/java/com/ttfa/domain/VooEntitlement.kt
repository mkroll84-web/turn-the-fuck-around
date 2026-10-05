package com.ttfa.domain

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.security.interfaces.RSAPublicKey
import java.util.Base64

const val VOO_PRODUCT_ID = "voo_mode_unlock"
enum class RoastIntensity(val label: String) { LIGHT("Light"), SPICY("Spicy"), FOUL("Absolutely Foul") }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

fun usablePlayLicenseKey(value: String): Boolean = try {
    val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(value))) as RSAPublicKey
    key.modulus.bitLength() >= 2048
} catch (_: Exception) { false }

/** Google Play's RSA signature covers the original purchase JSON; never trust a cached Boolean. */
fun verifyPlaySignature(publicKey: String, json: String, signature: String): Boolean = try {
    val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)))
    Signature.getInstance("SHA1withRSA").run {
        initVerify(key); update(json.toByteArray(Charsets.UTF_8)); verify(Base64.getDecoder().decode(signature))
    }
} catch (_: Exception) { false }

fun allowedPersonality(requested: Personality, unlocked: Boolean) =
    if (requested == Personality.VOO && !unlocked) Personality.CALM else requested

fun coPilotMessage(personality: Personality, intensity: RoastIntensity, unlocked: Boolean): String {
    if (allowedPersonality(personality, unlocked) != Personality.VOO) return missedTurnMessage(allowedPersonality(personality, unlocked))
    return when (intensity) {
        RoastIntensity.LIGHT -> "Darling, that turn wasn’t playing hard to get. Recalculating safely."
        RoastIntensity.SPICY -> "Deep breath, hot mess. You fucked that turn up beautifully. Recalculating safely."
        RoastIntensity.FOUL -> "You absolute fucking disaster. The turn was right there. Let’s get your gorgeous dumb ass back on a safe route."
    }
}
