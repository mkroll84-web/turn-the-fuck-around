package com.ttfa.data.billing

import android.app.Application
import org.junit.Assert.*
import org.junit.Test

class ReleaseOverrideTest {
    @Test fun releaseCannotEnableDevelopmentAccess() {
        val override = DevelopmentUnlock(Application())
        assertFalse(override.available)
        assertFalse(override.enabled())
        override.set(true)
        assertFalse(override.enabled())
    }
}
