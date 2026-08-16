package my.id.rmalan.cache.sweep.cleaner

import android.os.IBinder
import kotlinx.coroutines.runBlocking
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.model.CleanerError
import my.id.rmalan.cache.sweep.model.CleaningProgress
import my.id.rmalan.cache.sweep.model.CleanupPlan
import my.id.rmalan.cache.sweep.shizuku.ICacheOpsService
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShizukuCacheCleanerTest {

    private class FakeCacheOpsService(
        var selectiveSupported: Boolean = true,
        var globalTrimSupported: Boolean = true,
        var clearResult: Int = 0,
        var trimResult: Int = 0,
        var lastErrorMsg: String = "",
        var throwOnClear: Boolean = false
    ) : ICacheOpsService {
        val clearedPackages = mutableListOf<String>()
        var trimmedBytes: Long = 0L

        override fun destroy() {}
        override fun getProtocolVersion(): Int = 1
        override fun getPrivilegedUid(): Int = 2000
        override fun supportsSelectiveCacheClear(): Boolean = selectiveSupported
        override fun supportsGlobalTrim(): Boolean = globalTrimSupported

        override fun clearPackageCache(packageName: String, userId: Int): Int {
            if (throwOnClear) throw RuntimeException("IPC error during clear")
            clearedPackages.add(packageName)
            return clearResult
        }

        override fun trimCaches(desiredFreeBytes: Long): Int {
            trimmedBytes = desiredFreeBytes
            return trimResult
        }

        override fun getLastError(): String = lastErrorMsg
        override fun asBinder(): IBinder? = null
    }

    private class FakeShizukuManager(
        var fakeCapabilities: CleanerCapabilities = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = true,
            privilegedUid = 2000,
            supportsSelectiveCacheClear = true,
            supportsGlobalTrim = true
        ),
        var fakeService: ICacheOpsService? = null
    ) : ShizukuManager(android.app.Application()) {

        override suspend fun fetchCapabilities(timeoutMs: Long): CleanerCapabilities {
            return fakeCapabilities
        }

        override suspend fun getOrAwaitService(timeoutMs: Long): ICacheOpsService? {
            return fakeService
        }

        override fun getService(): ICacheOpsService? {
            return fakeService
        }
    }

    @Test
    fun `capabilities returns status from ShizukuManager`() = runBlocking {
        val fakeManager = FakeShizukuManager()
        val cleaner = ShizukuCacheCleaner(fakeManager)

        val caps = cleaner.capabilities()
        assertTrue(caps.isReady)
        assertTrue(caps.canCleanSelective)
        assertTrue(caps.canCleanGlobal)
    }

    @Test
    fun `clearPackage rejects invalid package, self-package, or negative userId`() = runBlocking {
        val fakeService = FakeCacheOpsService()
        val fakeManager = FakeShizukuManager(fakeService = fakeService)
        val cleaner = ShizukuCacheCleaner(fakeManager)

        // Invalid package
        assertFalse(cleaner.clearPackage("invalid;package", 0))

        // Self package
        assertFalse(cleaner.clearPackage("my.id.rmalan.cache.sweep", 0))

        // Negative userId
        assertFalse(cleaner.clearPackage("com.android.chrome", -1))

        assertTrue(fakeService.clearedPackages.isEmpty())
    }

    @Test
    fun `clearPackage returns false when service unavailable or selective unsupported`() = runBlocking {
        val fakeManagerNoService = FakeShizukuManager(fakeService = null)
        val cleanerNoService = ShizukuCacheCleaner(fakeManagerNoService)
        assertFalse(cleanerNoService.clearPackage("com.android.chrome", 0))

        val fakeServiceUnsupported = FakeCacheOpsService(selectiveSupported = false)
        val fakeManagerUnsupported = FakeShizukuManager(fakeService = fakeServiceUnsupported)
        val cleanerUnsupported = ShizukuCacheCleaner(fakeManagerUnsupported)
        assertFalse(cleanerUnsupported.clearPackage("com.android.chrome", 0))
    }

    @Test
    fun `clearPackage executes successfully on valid package`() = runBlocking {
        val fakeService = FakeCacheOpsService(clearResult = 0)
        val fakeManager = FakeShizukuManager(fakeService = fakeService)
        val cleaner = ShizukuCacheCleaner(fakeManager)

        assertTrue(cleaner.clearPackage("com.android.chrome", 0))
        assertEquals(listOf("com.android.chrome"), fakeService.clearedPackages)
    }

    @Test
    fun `clearPackages returns empty batch on empty package list`() = runBlocking {
        val fakeManager = FakeShizukuManager()
        val cleaner = ShizukuCacheCleaner(fakeManager)

        val result = cleaner.clearPackages(emptyList())
        assertEquals(0, result.totalAttempted)
        assertTrue(result.successfulPackages.isEmpty())
        assertTrue(result.failedPackages.isEmpty())
    }

    @Test
    fun `clearPackages handles Shizuku unavailable or unauthorized`() = runBlocking {
        val unavailCaps = CleanerCapabilities.UNAVAILABLE
        val fakeManagerUnavail = FakeShizukuManager(fakeCapabilities = unavailCaps)
        val cleanerUnavail = ShizukuCacheCleaner(fakeManagerUnavail)

        val pkgs = listOf("com.app.one", "com.app.two")
        val resultUnavail = cleanerUnavail.clearPackages(pkgs)
        assertEquals(2, resultUnavail.totalAttempted)
        assertEquals(2, resultUnavail.failedPackages.size)
        assertTrue(resultUnavail.isCompleteFailure)
        assertEquals(CleanerError.ShizukuUnavailable, resultUnavail.errors["com.app.one"])

        val unauthCaps = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = false,
            privilegedUid = null,
            supportsSelectiveCacheClear = true,
            supportsGlobalTrim = true
        )
        val fakeManagerUnauth = FakeShizukuManager(fakeCapabilities = unauthCaps)
        val cleanerUnauth = ShizukuCacheCleaner(fakeManagerUnauth)

        val resultUnauth = cleanerUnauth.clearPackages(pkgs)
        assertEquals(CleanerError.PermissionDenied, resultUnauth.errors["com.app.one"])
    }

    @Test
    fun `clearPackages handles selective clear unsupported`() = runBlocking {
        val noSelectiveCaps = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = true,
            privilegedUid = 2000,
            supportsSelectiveCacheClear = false,
            supportsGlobalTrim = true
        )
        val fakeManager = FakeShizukuManager(fakeCapabilities = noSelectiveCaps)
        val cleaner = ShizukuCacheCleaner(fakeManager)

        val pkgs = listOf("com.app.one")
        val result = cleaner.clearPackages(pkgs)
        assertEquals(CleanerError.SelectiveUnsupported, result.errors["com.app.one"])
    }

    @Test
    fun `clearPackages emits progress and isolates partial failures`() = runBlocking {
        val fakeService = object : ICacheOpsService {
            val cleared = mutableListOf<String>()
            override fun destroy() {}
            override fun getProtocolVersion(): Int = 1
            override fun getPrivilegedUid(): Int = 2000
            override fun supportsSelectiveCacheClear(): Boolean = true
            override fun supportsGlobalTrim(): Boolean = true
            override fun trimCaches(desiredFreeBytes: Long): Int = 0
            override fun getLastError(): String = "Protected system package"
            override fun asBinder(): IBinder? = null

            override fun clearPackageCache(packageName: String, userId: Int): Int {
                cleared.add(packageName)
                return if (packageName == "com.protected.system") 1 else 0
            }
        }

        val fakeManager = FakeShizukuManager(fakeService = fakeService)
        val cleaner = ShizukuCacheCleaner(fakeManager)

        val progressUpdates = mutableListOf<CleaningProgress>()
        val packagesToClean = listOf(
            "com.android.chrome",
            "my.id.rmalan.cache.sweep", // self-package (prohibited)
            "com.protected.system",     // fails with code 1
            "bad..package.name",        // invalid format
            "com.google.android.youtube" // succeeds
        )

        val result = cleaner.clearPackages(packagesToClean, userId = 0) { progress ->
            progressUpdates.add(progress)
        }

        assertEquals(5, result.totalAttempted)
        assertEquals(5, progressUpdates.size)
        assertEquals(listOf("com.android.chrome", "com.google.android.youtube"), result.successfulPackages)
        assertEquals(listOf("my.id.rmalan.cache.sweep", "com.protected.system", "bad..package.name"), result.failedPackages)
        assertTrue(result.isPartialSuccess)

        // Error attribution verification
        assertTrue(result.errors["my.id.rmalan.cache.sweep"] is CleanerError.SelfCleanProhibited)
        val cmdFailed = result.errors["com.protected.system"] as? CleanerError.CommandFailed
        assertNotNull(cmdFailed)
        assertEquals(1, cmdFailed?.exitCode)
        assertEquals("Protected system package", cmdFailed?.rawError)
        assertTrue(result.errors["bad..package.name"] is CleanerError.PackageInvalid)
    }

    @Test
    fun `trimGlobally executes correctly and handles invalid bytes or unsupported`() = runBlocking {
        val fakeService = FakeCacheOpsService(trimResult = 0)
        val fakeManager = FakeShizukuManager(fakeService = fakeService)
        val cleaner = ShizukuCacheCleaner(fakeManager)

        // Negative bytes rejected
        assertFalse(cleaner.trimGlobally(-1L))

        // Normal execution
        assertTrue(cleaner.trimGlobally(5000000000L))
        assertEquals(5000000000L, fakeService.trimmedBytes)

        // Trim unsupported
        fakeService.globalTrimSupported = false
        assertFalse(cleaner.trimGlobally(5000000000L))
    }

    @Test
    fun `executePlan executes selective plan and global trim plan`() = runBlocking {
        val fakeService = FakeCacheOpsService()
        val fakeManager = FakeShizukuManager(fakeService = fakeService)
        val cleaner = ShizukuCacheCleaner(fakeManager)

        // Selective Plan
        val selectivePlan = CleanupPlan.selective(listOf("com.android.chrome", "org.mozilla.firefox"))
        val selectiveResult = cleaner.executePlan(selectivePlan)
        assertTrue(selectiveResult.isCompleteSuccess)
        assertEquals(2, selectiveResult.successfulPackages.size)

        // Global Trim Plan
        val globalPlan = CleanupPlan.globalTrim(desiredFreeBytes = 3000000000L)
        val globalResult = cleaner.executePlan(globalPlan)
        assertTrue(globalResult.isCompleteSuccess)
        assertEquals(listOf("global_trim"), globalResult.successfulPackages)
    }
}
