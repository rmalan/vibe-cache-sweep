package my.id.rmalan.cache.sweep.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupPlanTest {

    @Test
    fun `selective factory deduplicates packages and sets mode`() {
        val plan = CleanupPlan.selective(
            packages = listOf("com.android.chrome", "com.android.chrome", "org.mozilla.firefox"),
            estimatedCacheBytes = 1024L * 1024L * 50L
        )

        assertEquals(CleanupMode.SELECTIVE, plan.mode)
        assertTrue(plan.isSelective)
        assertFalse(plan.isGlobalTrim)
        assertEquals(listOf("com.android.chrome", "org.mozilla.firefox"), plan.selectedPackages)
        assertEquals(2, plan.packageCount)
        assertEquals(52428800L, plan.estimatedCacheBytes)
        assertNull(plan.desiredFreeBytes)
    }

    @Test
    fun `selective validation succeeds for valid packages`() {
        val plan = CleanupPlan.selective(
            packages = listOf("com.android.chrome", "com.google.android.youtube"),
            estimatedCacheBytes = 1000L
        )
        val result = plan.validate()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `selective validation fails when packages list is empty`() {
        val plan = CleanupPlan.selective(
            packages = emptyList(),
            estimatedCacheBytes = 0L
        )
        val result = plan.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("at least one package") == true)
    }

    @Test
    fun `selective validation fails when self package is included`() {
        val plan = CleanupPlan.selective(
            packages = listOf("com.android.chrome", "my.id.rmalan.cache.sweep"),
            estimatedCacheBytes = 500L
        )
        val result = plan.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("self-package") == true)
    }

    @Test
    fun `selective validation fails for invalid package name format`() {
        val plan = CleanupPlan.selective(
            packages = listOf("invalid;rm -rf /", "com.valid.app"),
            estimatedCacheBytes = 500L
        )
        val result = plan.validate()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid package name") == true)
    }

    @Test
    fun `global trim factory sets mode and parameters`() {
        val plan = CleanupPlan.globalTrim(
            desiredFreeBytes = 1024L * 1024L * 1024L * 5L,
            estimatedCacheBytes = 1024L * 1024L * 1024L * 2L
        )

        assertEquals(CleanupMode.GLOBAL_TRIM, plan.mode)
        assertTrue(plan.isGlobalTrim)
        assertFalse(plan.isSelective)
        assertTrue(plan.selectedPackages.isEmpty())
        assertEquals(0, plan.packageCount)
        assertEquals(5368709120L, plan.desiredFreeBytes)
        assertEquals(2147483648L, plan.estimatedCacheBytes)
    }

    @Test
    fun `global trim validation succeeds for positive target`() {
        val plan = CleanupPlan.globalTrim(desiredFreeBytes = 5000000L)
        val result = plan.validate()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `global trim validation fails for zero target`() {
        val plan = CleanupPlan(
            mode = CleanupMode.GLOBAL_TRIM,
            desiredFreeBytes = 0L
        )
        val result = plan.validate()
        assertTrue(result.isFailure)
    }

    @Test
    fun `selective validation checks against scanned package set when provided`() {
        val scanned = setOf("com.android.chrome", "org.mozilla.firefox")
        val validPlan = CleanupPlan.selective(listOf("com.android.chrome"))
        assertTrue(validPlan.validate(scanned).isSuccess)

        val invalidPlan = CleanupPlan.selective(listOf("com.unscanned.app"))
        val result = invalidPlan.validate(scanned)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("known scanned packages") == true)
    }

    @Test
    fun `fromApps creates selective plan from AppCacheInfo items`() {
        val apps = listOf(
            AppCacheInfo(
                packageName = "com.android.chrome",
                appName = "Chrome",
                cacheBytes = 1000L,
                appBytes = 2000L,
                dataBytes = 3000L,
                isSystemApp = false,
                measurementAvailable = true
            ),
            AppCacheInfo(
                packageName = "my.id.rmalan.cache.sweep", // self-app filtered out
                appName = "CacheSweep",
                cacheBytes = 500L,
                appBytes = 1000L,
                dataBytes = 1000L,
                isSystemApp = false,
                measurementAvailable = true
            ),
            AppCacheInfo(
                packageName = "org.mozilla.firefox",
                appName = "Firefox",
                cacheBytes = 2000L,
                appBytes = 3000L,
                dataBytes = 4000L,
                isSystemApp = false,
                measurementAvailable = true
            )
        )

        val plan = CleanupPlan.fromApps(apps)
        assertEquals(CleanupMode.SELECTIVE, plan.mode)
        assertEquals(listOf("com.android.chrome", "org.mozilla.firefox"), plan.selectedPackages)
        assertEquals(3000L, plan.estimatedCacheBytes)
    }
}
