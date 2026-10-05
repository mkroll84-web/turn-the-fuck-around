package com.ttfa.data.billing

import android.content.Context

/** Variant-specific: release cannot read or enable the development entitlement. */
class DevelopmentUnlock(context: Context) {
    private val prefs = context.getSharedPreferences("development_only", 0)
    val available = true
    fun enabled() = prefs.getBoolean("voo", false)
    fun set(enabled: Boolean) { prefs.edit().putBoolean("voo", enabled).apply() }
}
