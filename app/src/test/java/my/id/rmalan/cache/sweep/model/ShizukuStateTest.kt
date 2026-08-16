package my.id.rmalan.cache.sweep.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuStateTest {

    @Test
    fun shizukuState_readyWithShellUid() {
        val state = ShizukuState.Ready(uid = 2000)
        assertEquals(2000, state.uid)
    }

    @Test
    fun shizukuState_readyWithRootUid() {
        val state = ShizukuState.Ready(uid = 0)
        assertEquals(0, state.uid)
    }

    @Test
    fun shizukuState_errorHoldsReason() {
        val state = ShizukuState.Error("Permission denied")
        assertEquals("Permission denied", state.reason)
    }

    @Test
    fun cleanerCapabilities_authorizedWithUidAndAllCapabilities() {
        val capabilities = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = true,
            privilegedUid = 2000,
            supportsSelectiveCacheClear = true,
            supportsGlobalTrim = true
        )

        assertTrue(capabilities.shizukuAvailable)
        assertTrue(capabilities.shizukuAuthorized)
        assertEquals(2000, capabilities.privilegedUid)
        assertTrue(capabilities.supportsSelectiveCacheClear)
        assertTrue(capabilities.supportsGlobalTrim)
    }

    @Test
    fun cleanerCapabilities_unauthorized_nullUid() {
        val capabilities = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = false,
            privilegedUid = null,
            supportsSelectiveCacheClear = false,
            supportsGlobalTrim = false
        )

        assertTrue(capabilities.shizukuAvailable)
        assertFalse(capabilities.shizukuAuthorized)
        assertNull(capabilities.privilegedUid)
        assertFalse(capabilities.supportsSelectiveCacheClear)
        assertFalse(capabilities.supportsGlobalTrim)
    }
}
