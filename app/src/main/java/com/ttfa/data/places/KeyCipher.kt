package com.ttfa.data.places

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object KeyCipher {
    fun encrypt(value: String, key: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val bytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(cipher.iv) + ":" + Base64.getEncoder().encodeToString(bytes)
    }
    fun decrypt(value: String, key: SecretKey): String {
        val parts = value.split(":")
        require(parts.size == 2)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.getDecoder().decode(parts[0])))
        return cipher.doFinal(Base64.getDecoder().decode(parts[1])).toString(Charsets.UTF_8)
    }
}
