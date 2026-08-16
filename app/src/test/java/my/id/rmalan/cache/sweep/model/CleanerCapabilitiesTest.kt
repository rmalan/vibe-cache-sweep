package my.id.rmalan.cache.sweep.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanerCapabilitiesTest {

    @Test
    fun `unavailable constant has false for all capabilities`() {
        val caps = CleanerCapabilities.UNAVAILABLE

        assertFalse(caps.shizukuAvailable)
        assertFalse(caps.shizukuAuthorized)
        assertNull(caps.privilegedUid)
        assertFalse(caps.supportsSelectiveCacheClear)
        assertFalse(caps.supportsGlobalTrim)
        assertFalse(caps.isReady)
        assertFalse(caps.canCleanSelective)
        assertFalse(caps.canCleanGlobal)
        assertFalse(caps.canCleanAny)
    }

    @Test
    fun `isReady requires available, authorized, and non-null uid`() {
        val notAvailable = CleanerCapabilities(
            shizukuAvailable = false,
            shizukuAuthorized = true,
            privilegedUid = 2000,
            supportsSelectiveCacheClear = true,
            supportsGlobalTrim = true
        )
        assertFalse(notAvailable.isReady)
        assertFalse(notAvailable.canCleanSelective)
        assertFalse(notAvailable.canCleanGlobal)

        val notAuthorized = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = false,
            privilegedUid = 2000,
            supportsSelectiveCacheClear = true,
            supportsGlobalTrim = true
        )
        assertFalse(notAuthorized.isReady)

        val nullUid = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = true,
            privilegedUid = null,
            supportsSelectiveCacheClear = true,
            supportsGlobalTrim = true
        )
        assertFalse(nullUid.isReady)

        val ready = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = true,
            privilegedUid = 2000,
            supportsSelectiveCacheClear = true,
            supportsGlobalTrim = true
        )
        assertTrue(ready.isReady)
        assertTrue(ready.canCleanSelective)
        assertTrue(ready.canCleanGlobal)
        assertTrue(ready.canCleanAny)
    }

    @Test
    fun `selective only vs global trim only capabilities`() {
        val selectiveOnly = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = true,
            privilegedUid = 2000,
            supportsSelectiveCacheClear = true,
            supportsGlobalTrim = false
        )
        assertTrue(selectiveOnly.isReady)
        assertTrue(selectiveOnly.canCleanSelective)
        assertFalse(selectiveOnly.canCleanGlobal)
        assertTrue(selectiveOnly.canCleanAny)

        val globalOnly = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = true,
            privilegedUid = 2000,
            supportsSelectiveCacheClear = false,
            supportsGlobalTrim = true
        )
        assertTrue(globalOnly.isReady)
        assertFalse(globalOnly.canCleanSelective)
        assertTrue(globalOnly.canCleanGlobal)
        assertTrue(globalOnly.canCleanAny)
    }

    @Test
    fun `ready but neither selective nor global trim supported`() {
        val noneSupported = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = true,
            privilegedUid = 2000,
            supportsSelectiveCacheClear = false,
            supportsGlobalTrim = false
        )
        assertTrue(noneSupported.isReady)
        assertFalse(noneSupported.canCleanSelective)
        assertFalse(noneSupported.canCleanGlobal)
        assertFalse(noneSupported.canCleanAny)
    }
}
