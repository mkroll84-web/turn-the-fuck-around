package com.ttfa.data.places

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.ttfa.BuildConfig
import com.ttfa.domain.usablePlacesKey
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** Runtime keys are encrypted locally. Never logged, exported or put in saved UI state. */
class GooglePlacesKeyStore(context: Context) {
    private val preferences = context.getSharedPreferences("google_places_config", Context.MODE_PRIVATE)
    private val alias = "ttfa.googlePlacesKey"
    val isConfigured: Boolean get() = usablePlacesKey(apiKey())
    fun apiKey(): String {
        val encrypted = preferences.getString("encrypted_key", null)
        if (encrypted != null) {
            try { return KeyCipher.decrypt(encrypted, encryptionKey()) }
            catch (_: Exception) { return "" } // Unavailable device key: ask to re-enter, never expose ciphertext.
        }
        return BuildConfig.GOOGLE_PLACES_API_KEY.trim()
    }
    fun save(value: String) {
        require(usablePlacesKey(value))
        val encrypted = KeyCipher.encrypt(value, encryptionKey())
        check(preferences.edit().putString("encrypted_key", encrypted).commit()) { "Could not save Google key" }
    }
    fun clear() { preferences.edit().remove("encrypted_key").apply() }
    private fun encryptionKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}
