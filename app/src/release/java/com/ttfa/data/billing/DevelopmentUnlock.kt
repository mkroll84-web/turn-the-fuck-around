package com.ttfa.data.billing

import android.content.Context

/** Variant-specific: release cannot read or enable the development entitlement. */
class DevelopmentUnlock(context: Context) {
    val available = false
    fun enabled() = false
    fun set(enabled: Boolean) = Unit
}
