package com.ttfa.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.ttfa.BuildConfig
import com.ttfa.domain.VOO_PRODUCT_ID
import com.ttfa.domain.verifyPlaySignature
import com.ttfa.domain.usablePlayLicenseKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

/** Application-context billing owner. Never consumes the non-consumable Voo entitlement. */
data class VooBillingState(
    val purchased: Boolean = false, val development: Boolean = false,
    val developmentAvailable: Boolean = false, val price: String = "US $2.00",
    val ready: Boolean = false, val pending: Boolean = false,
    val message: String = "Connect to Google Play to check purchases.",
) { val unlocked get() = purchased || development }

class VooBillingRepository(context: Context) : PurchasesUpdatedListener {
    private val development = DevelopmentUnlock(context)
    private val cache = context.getSharedPreferences("verified_play_purchase", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(VooBillingState(development = development.enabled(), developmentAvailable = development.available))
    val state = _state.asStateFlow()
    private var details: ProductDetails? = null
    private var offer: ProductDetails.OneTimePurchaseOfferDetails? = null
    private var connecting = false
    private val acknowledging = mutableSetOf<String>()
    private val client = BillingClient.newBuilder(context.applicationContext).setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection().build()

    init {
        // Offline entitlement is reconstructed from a signed, acknowledged purchase, not a Boolean.
        val json = cache.getString("json", null); val signature = cache.getString("signature", null)
        if (json != null && signature != null) runCatching { Purchase(json, signature) }.getOrNull()?.let {
            if (valid(it) && it.isAcknowledged) _state.update { s -> s.copy(purchased = true) }
        }
        refresh()
    }
    fun development(enabled: Boolean) {
        if (!development.available) return
        development.set(enabled)
        _state.update { it.copy(development = development.enabled()) }
    }
    fun refresh() {
        if (client.isReady) { query(); return }
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) query()
                else unavailable()
            }
            override fun onBillingServiceDisconnected() { connecting = false; _state.update { it.copy(ready = false, message = "Google Play disconnected. Try Restore purchases to reconnect.") } }
        })
    }
    private fun unavailable() { _state.update { it.copy(ready = false, message = "Google Play purchases are unavailable. Install from the Play Internal Testing track to test real purchases.") } }
    private fun query() {
        // ProductDetails is refreshed each connection/resume; no persisted pricing or stale offer token.
        _state.update { it.copy(ready = false) }
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(VOO_PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()
        )).build()) { result, response ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                details = response.productDetailsList.firstOrNull { it.productId == VOO_PRODUCT_ID }
                // Only a permanent buy option. Never offer a rental or preorder as permanent access.
                offer = details?.oneTimePurchaseOfferDetailsList?.firstOrNull { it.rentalDetails == null && it.preorderDetails == null && !it.offerToken.isNullOrBlank() }
                _state.update { it.copy(ready = offer != null && usablePlayLicenseKey(BuildConfig.GOOGLE_PLAY_LICENSE_KEY), price = offer?.formattedPrice ?: "US $2.00",
                    message = if (offer == null) "Voo is not available in this Play installation yet."
                    else if (!usablePlayLicenseKey(BuildConfig.GOOGLE_PLAY_LICENSE_KEY)) "Purchase verification is not configured in this development build."
                    else "One-time purchase. No subscription.") }
            } else unavailable()
        }
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) process(purchases, authoritative = true)
            else _state.update { it.copy(message = "Couldn’t restore purchases. Check Google Play and your connection, then retry.") }
        }
    }
    fun purchase(activity: Activity) {
        val product = details; val selectedOffer = offer
        if (!_state.value.ready || product == null || selectedOffer == null || _state.value.purchased || _state.value.pending) return
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).setOfferToken(requireNotNull(selectedOffer.offerToken)).build()
        val result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build())
        handleResult(result, null)
    }
    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) = handleResult(result, purchases)
    private fun handleResult(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> if (purchases != null) process(purchases, false)
            BillingClient.BillingResponseCode.USER_CANCELED -> _state.update { it.copy(message = "Purchase cancelled. No new unlock was added.") }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            else -> _state.update { it.copy(message = "Purchase couldn’t complete. You can retry or restore purchases.") }
        }
    }
    private fun valid(purchase: Purchase): Boolean =
        purchase.purchaseState == Purchase.PurchaseState.PURCHASED && VOO_PRODUCT_ID in purchase.products && purchase.purchaseToken.isNotBlank() &&
        verifyPlaySignature(BuildConfig.GOOGLE_PLAY_LICENSE_KEY, purchase.originalJson, purchase.signature) &&
        runCatching { JSONObject(purchase.originalJson).getString("packageName") == BuildConfig.APPLICATION_ID }.getOrDefault(false)

    private fun process(purchases: List<Purchase>, authoritative: Boolean) {
        val owned = purchases.filter { VOO_PRODUCT_ID in it.products }
        val pending = owned.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        val verified = owned.filter(::valid)
        if (authoritative && verified.isEmpty()) {
            cache.edit().clear().apply()
            _state.update { it.copy(purchased = false) }
        }
        _state.update { it.copy(pending = pending, message = if (pending) "Payment pending. Voo unlocks after Google Play confirms payment." else it.message) }
        verified.forEach { purchase ->
            if (purchase.isAcknowledged) grant(purchase)
            else if (acknowledging.add(purchase.purchaseToken)) {
                client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { result ->
                    acknowledging.remove(purchase.purchaseToken)
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        // Refresh signed JSON with Play's acknowledged state before caching for offline use.
                        _state.update { it.copy(purchased = true, pending = false, message = "Voo Mode unlocked. Every roast level is yours.") }
                        refresh()
                    } else _state.update { it.copy(message = "Payment received; confirmation needs a retry. Tap Restore purchases.") }
                }
            }
        }
    }
    private fun grant(purchase: Purchase) {
        cache.edit().putString("json", purchase.originalJson).putString("signature", purchase.signature).apply()
        _state.update { it.copy(purchased = true, pending = false, message = "Voo Mode restored. Every roast level is yours.") }
    }
    fun close() { client.endConnection() }
}
