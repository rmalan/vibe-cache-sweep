package my.id.rmalan.cache.sweep.cleaner

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.UserHandle
import kotlinx.coroutines.test.runTest
import my.id.rmalan.cache.sweep.model.CleanerBatchResult
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.model.CleanerError
import my.id.rmalan.cache.sweep.model.CleaningProgress
import my.id.rmalan.cache.sweep.model.CleaningState
import my.id.rmalan.cache.sweep.model.CleanupMode
import my.id.rmalan.cache.sweep.model.CleanupPlan
import my.id.rmalan.cache.sweep.model.CleanupResult
import my.id.rmalan.cache.sweep.model.DeviceStorageInfo
import my.id.rmalan.cache.sweep.model.DiscoveredPackage
import my.id.rmalan.cache.sweep.model.PackageStorageStats
import my.id.rmalan.cache.sweep.scanner.PackageRepository
import my.id.rmalan.cache.sweep.storage.DeviceStorageRepository
import my.id.rmalan.cache.sweep.storage.StorageStatsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CleanupCoordinatorTest {

    private class FakeCacheCleaner(
        var capabilitiesResult: CleanerCapabilities = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = true,
            privilegedUid = 2000,
            supportsSelectiveCacheClear = true,
            supportsGlobalTrim = true
        ),
        var batchResultToReturn: CleanerBatchResult? = null,
        var exceptionToThrow: Exception? = null
    ) : CacheCleaner {
        val executedPlans = mutableListOf<CleanupPlan>()

        override suspend fun capabilities(): CleanerCapabilities = capabilitiesResult

        override suspend fun clearPackage(packageName: String, userId: Int): Boolean = true

        override suspend fun clearPackages(
            packages: List<String>,
            userId: Int,
            scannedPackageSet: Set<String>?,
            onProgress: (suspend (CleaningProgress) -> Unit)?
        ): CleanerBatchResult {
            packages.forEachIndexed { index, pkg ->
                onProgress?.invoke(
                    CleaningProgress(
                        current = index + 1,
                        total = packages.size,
                        currentPackageName = pkg
                    )
                )
            }
            return batchResultToReturn ?: CleanerBatchResult(
                totalAttempted = packages.size,
                successfulPackages = packages,
                failedPackages = emptyList()
            )
        }

        override suspend fun trimGlobally(desiredFreeBytes: Long): Boolean = true

        override suspend fun executePlan(
            plan: CleanupPlan,
            userId: Int,
            scannedPackageSet: Set<String>?,
            onProgress: (suspend (CleaningProgress) -> Unit)?
        ): CleanerBatchResult {
            exceptionToThrow?.let { throw it }
            executedPlans.add(plan)

            if (plan.mode == CleanupMode.GLOBAL_TRIM) {
                onProgress?.invoke(
                    CleaningProgress(
                        current = 1,
                        total = 1,
                        currentPackageName = null,
                        currentAppName = "Global Cache Trim"
                    )
                )
                return batchResultToReturn ?: CleanerBatchResult(
                    totalAttempted = 1,
                    successfulPackages = listOf("global_trim"),
                    failedPackages = emptyList()
                )
            }

            return clearPackages(
                packages = plan.selectedPackages,
                userId = userId,
                scannedPackageSet = scannedPackageSet,
                onProgress = onProgress
            )
        }
    }

    private class FakeDeviceStorageRepository(
        private val snapshots: MutableList<DeviceStorageInfo>
    ) : DeviceStorageRepository() {
        override fun snapshot(): DeviceStorageInfo {
            return if (snapshots.isNotEmpty()) {
                snapshots.removeAt(0)
            } else {
                DeviceStorageInfo(100_000_000_000L, 50_000_000_000L)
            }
        }
    }

    private class FakeStorageStatsRepository(
        var statsMap: MutableMap<String, PackageStorageStats> = mutableMapOf()
    ) : StorageStatsRepository {
        override fun queryStats(
            packageName: String,
            storageUuid: UUID?,
            userHandle: UserHandle?
        ): PackageStorageStats {
            return statsMap[packageName] ?: PackageStorageStats.ZERO
        }
    }

    private class FakePackageRepository : PackageRepository {
        val packages = mutableMapOf<String, DiscoveredPackage>()

        override fun getInstalledPackages(
            includeSelf: Boolean,
            includeSystem: Boolean
        ): List<DiscoveredPackage> = packages.values.toList()

        override fun getPackage(packageName: String): DiscoveredPackage? = packages[packageName]

        override fun loadApplicationIcon(packageName: String): Drawable? = null

        override fun loadIconThumbnail(packageName: String, sizePx: Int): Bitmap? = null
    }

    @Test
    fun `successful selective cleanup follows full state progression and calculates metrics`() = runTest {
        val fakeCleaner = FakeCacheCleaner()
        val fakeStorage = FakeDeviceStorageRepository(
            snapshots = mutableListOf(
                DeviceStorageInfo(totalBytes = 100_000_000_000L, availableBytes = 50_000_000_000L), // before
                DeviceStorageInfo(totalBytes = 100_000_000_000L, availableBytes = 52_000_000_000L)  // after (+2GB)
            )
        )
        val fakeStats = FakeStorageStatsRepository(
            statsMap = mutableMapOf(
                "com.example.app1" to PackageStorageStats(cacheBytes = 1_500_000_000L, appBytes = 100L, dataBytes = 200L),
                "com.example.app2" to PackageStorageStats(cacheBytes = 500_000_000L, appBytes = 100L, dataBytes = 200L)
            )
        )

        val coordinator = CleanupCoordinator(
            cleaner = fakeCleaner,
            storage = fakeStorage,
            storageStatsRepository = fakeStats,
            settlingDelayMillis = 0L // No delay in tests
        )

        val statesEmitted = mutableListOf<CleaningState>()
        val plan = CleanupPlan.selective(
            packages = listOf("com.example.app1", "com.example.app2"),
            estimatedCacheBytes = 2_000_000_000L
        )

        // After cleaning, stats report 0 bytes cache
        fakeCleaner.batchResultToReturn = CleanerBatchResult(
            totalAttempted = 2,
            successfulPackages = listOf("com.example.app1", "com.example.app2"),
            failedPackages = emptyList()
        )

        val result = coordinator.clean(
            plan = plan,
            onProgress = { statesEmitted.add(it) }
        )

        // Verify state progression
        assertTrue(statesEmitted.any { it is CleaningState.Validating })
        assertTrue(statesEmitted.any { it is CleaningState.SnapshotBefore })
        assertTrue(statesEmitted.any { it is CleaningState.Clearing })
        assertTrue(statesEmitted.any { it is CleaningState.WaitingForStats })
        assertTrue(statesEmitted.any { it is CleaningState.SnapshotAfter })
        assertTrue(statesEmitted.last() is CleaningState.Completed)

        // Verify metrics
        assertEquals(50_000_000_000L, result.physicalFreeBefore)
        assertEquals(52_000_000_000L, result.physicalFreeAfter)
        assertEquals(2_000_000_000L, result.measuredFreedBytes)
        assertEquals(2, result.attemptedPackages)
        assertEquals(2, result.successfulPackages)
        assertEquals(0, result.failedPackages.size)
        assertTrue(result.isCompleteSuccess)
        assertTrue(result.isSignificantReclaim)
    }

    @Test
    fun `selective cleanup aborted when Shizuku is not running`() = runTest {
        val fakeCleaner = FakeCacheCleaner(
            capabilitiesResult = CleanerCapabilities.UNAVAILABLE
        )
        val fakeStorage = FakeDeviceStorageRepository(mutableListOf())
        val fakeStats = FakeStorageStatsRepository()

        val coordinator = CleanupCoordinator(
            cleaner = fakeCleaner,
            storage = fakeStorage,
            storageStatsRepository = fakeStats,
            settlingDelayMillis = 0L
        )

        val statesEmitted = mutableListOf<CleaningState>()
        val plan = CleanupPlan.selective(listOf("com.example.app"))

        val result = coordinator.clean(
            plan = plan,
            onProgress = { statesEmitted.add(it) }
        )

        val lastState = statesEmitted.last()
        assertTrue(lastState is CleaningState.Failed)
        val failedState = lastState as CleaningState.Failed
        assertEquals(CleanerError.ShizukuUnavailable, failedState.error)
        assertEquals(0, result.successfulPackages)
        assertEquals(CleanerError.ShizukuUnavailable, result.errors["cleanup"])
    }

    @Test
    fun `selective cleanup aborted when Shizuku permission is denied`() = runTest {
        val fakeCleaner = FakeCacheCleaner(
            capabilitiesResult = CleanerCapabilities(
                shizukuAvailable = true,
                shizukuAuthorized = false,
                privilegedUid = null,
                supportsSelectiveCacheClear = false,
                supportsGlobalTrim = false
            )
        )
        val fakeStorage = FakeDeviceStorageRepository(mutableListOf())
        val fakeStats = FakeStorageStatsRepository()

        val coordinator = CleanupCoordinator(
            cleaner = fakeCleaner,
            storage = fakeStorage,
            storageStatsRepository = fakeStats,
            settlingDelayMillis = 0L
        )

        val statesEmitted = mutableListOf<CleaningState>()
        val plan = CleanupPlan.selective(listOf("com.example.app"))

        val result = coordinator.clean(
            plan = plan,
            onProgress = { statesEmitted.add(it) }
        )

        val lastState = statesEmitted.last()
        assertTrue(lastState is CleaningState.Failed)
        assertEquals(CleanerError.PermissionDenied, (lastState as CleaningState.Failed).error)
        assertEquals(0, result.successfulPackages)
    }

    @Test
    fun `selective cleanup aborted when selective capability is unsupported`() = runTest {
        val fakeCleaner = FakeCacheCleaner(
            capabilitiesResult = CleanerCapabilities(
                shizukuAvailable = true,
                shizukuAuthorized = true,
                privilegedUid = 2000,
                supportsSelectiveCacheClear = false, // Unsupported!
                supportsGlobalTrim = true
            )
        )
        val fakeStorage = FakeDeviceStorageRepository(mutableListOf())
        val fakeStats = FakeStorageStatsRepository()

        val coordinator = CleanupCoordinator(
            cleaner = fakeCleaner,
            storage = fakeStorage,
            storageStatsRepository = fakeStats,
            settlingDelayMillis = 0L
        )

        val statesEmitted = mutableListOf<CleaningState>()
        val plan = CleanupPlan.selective(listOf("com.example.app"))

        val result = coordinator.clean(
            plan = plan,
            onProgress = { statesEmitted.add(it) }
        )

        val lastState = statesEmitted.last()
        assertTrue(lastState is CleaningState.Failed)
        assertEquals(CleanerError.SelectiveUnsupported, (lastState as CleaningState.Failed).error)
        assertEquals(0, result.successfulPackages)
    }

    @Test
    fun `global trim aborted when global trim capability is unsupported`() = runTest {
        val fakeCleaner = FakeCacheCleaner(
            capabilitiesResult = CleanerCapabilities(
                shizukuAvailable = true,
                shizukuAuthorized = true,
                privilegedUid = 2000,
                supportsSelectiveCacheClear = true,
                supportsGlobalTrim = false // Unsupported!
            )
        )
        val fakeStorage = FakeDeviceStorageRepository(mutableListOf())
        val fakeStats = FakeStorageStatsRepository()

        val coordinator = CleanupCoordinator(
            cleaner = fakeCleaner,
            storage = fakeStorage,
            storageStatsRepository = fakeStats,
            settlingDelayMillis = 0L
        )

        val statesEmitted = mutableListOf<CleaningState>()
        val plan = CleanupPlan.globalTrim(desiredFreeBytes = 50_000_000_000L)

        val result = coordinator.clean(
            plan = plan,
            onProgress = { statesEmitted.add(it) }
        )

        val lastState = statesEmitted.last()
        assertTrue(lastState is CleaningState.Failed)
        assertEquals(CleanerError.GlobalTrimUnsupported, (lastState as CleaningState.Failed).error)
    }

    @Test
    fun `partial failures are captured in result without failing coordinator`() = runTest {
        val fakeCleaner = FakeCacheCleaner(
            batchResultToReturn = CleanerBatchResult(
                totalAttempted = 3,
                successfulPackages = listOf("com.app.ok1", "com.app.ok2"),
                failedPackages = listOf("com.app.failed"),
                errors = mapOf("com.app.failed" to CleanerError.CommandFailed(1, "Permission denied"))
            )
        )
        val fakeStorage = FakeDeviceStorageRepository(
            snapshots = mutableListOf(
                DeviceStorageInfo(100_000_000_000L, 50_000_000_000L),
                DeviceStorageInfo(100_000_000_000L, 51_000_000_000L)
            )
        )
        val fakeStats = FakeStorageStatsRepository(
            statsMap = mutableMapOf(
                "com.app.ok1" to PackageStorageStats(cacheBytes = 500_000_000L, appBytes = 0L, dataBytes = 0L),
                "com.app.ok2" to PackageStorageStats(cacheBytes = 500_000_000L, appBytes = 0L, dataBytes = 0L),
                "com.app.failed" to PackageStorageStats(cacheBytes = 200_000_000L, appBytes = 0L, dataBytes = 0L)
            )
        )

        val coordinator = CleanupCoordinator(
            cleaner = fakeCleaner,
            storage = fakeStorage,
            storageStatsRepository = fakeStats,
            settlingDelayMillis = 0L
        )

        val statesEmitted = mutableListOf<CleaningState>()
        val plan = CleanupPlan.selective(listOf("com.app.ok1", "com.app.ok2", "com.app.failed"))

        val result = coordinator.clean(
            plan = plan,
            onProgress = { statesEmitted.add(it) }
        )

        assertTrue(statesEmitted.last() is CleaningState.Completed)
        assertEquals(3, result.attemptedPackages)
        assertEquals(2, result.successfulPackages)
        assertEquals(1, result.failedPackages.size)
        assertEquals("com.app.failed", result.failedPackages.first())
        assertTrue(result.isPartialSuccess)
        assertFalse(result.isCompleteSuccess)
        assertNotNull(result.errors["com.app.failed"])
    }

    @Test
    fun `successful global trim flow executes and records storage change`() = runTest {
        val fakeCleaner = FakeCacheCleaner()
        val fakeStorage = FakeDeviceStorageRepository(
            snapshots = mutableListOf(
                DeviceStorageInfo(100_000_000_000L, 50_000_000_000L), // before
                DeviceStorageInfo(100_000_000_000L, 55_000_000_000L)  // after (+5GB)
            )
        )
        val fakeStats = FakeStorageStatsRepository()

        val coordinator = CleanupCoordinator(
            cleaner = fakeCleaner,
            storage = fakeStorage,
            storageStatsRepository = fakeStats,
            settlingDelayMillis = 0L
        )

        val statesEmitted = mutableListOf<CleaningState>()
        val plan = CleanupPlan.globalTrim(
            desiredFreeBytes = 55_000_000_000L,
            estimatedCacheBytes = 5_000_000_000L
        )

        val result = coordinator.clean(
            plan = plan,
            onProgress = { statesEmitted.add(it) }
        )

        assertTrue(statesEmitted.last() is CleaningState.Completed)
        assertEquals(CleanupMode.GLOBAL_TRIM, result.mode)
        assertEquals(5_000_000_000L, result.measuredFreedBytes)
        assertEquals(1, result.attemptedPackages)
        assertEquals(1, result.successfulPackages)
    }

    @Test
    fun `negative storage delta is clamped to zero`() = runTest {
        val fakeCleaner = FakeCacheCleaner()
        // Available space decreased from 50GB to 48GB during cleanup (e.g. background writes)
        val fakeStorage = FakeDeviceStorageRepository(
            snapshots = mutableListOf(
                DeviceStorageInfo(100_000_000_000L, 50_000_000_000L),
                DeviceStorageInfo(100_000_000_000L, 48_000_000_000L)
            )
        )
        val fakeStats = FakeStorageStatsRepository()

        val coordinator = CleanupCoordinator(
            cleaner = fakeCleaner,
            storage = fakeStorage,
            storageStatsRepository = fakeStats,
            settlingDelayMillis = 0L
        )

        val plan = CleanupPlan.selective(listOf("com.example.app"))
        val result = coordinator.clean(plan)

        assertEquals(0L, result.measuredFreedBytes)
        assertFalse(result.isSignificantReclaim)
    }

    @Test
    fun `cleaner exception during execution emits Failed and returns failed result`() = runTest {
        val fakeCleaner = FakeCacheCleaner(
            exceptionToThrow = RuntimeException("Binder transaction died")
        )
        val fakeStorage = FakeDeviceStorageRepository(mutableListOf())
        val fakeStats = FakeStorageStatsRepository()

        val coordinator = CleanupCoordinator(
            cleaner = fakeCleaner,
            storage = fakeStorage,
            storageStatsRepository = fakeStats,
            settlingDelayMillis = 0L
        )

        val statesEmitted = mutableListOf<CleaningState>()
        val plan = CleanupPlan.selective(listOf("com.example.app"))

        val result = coordinator.clean(
            plan = plan,
            onProgress = { statesEmitted.add(it) }
        )

        val lastState = statesEmitted.last()
        assertTrue(lastState is CleaningState.Failed)
        assertTrue((lastState as CleaningState.Failed).message.contains("Binder transaction died"))
        assertEquals(0, result.successfulPackages)
    }
}
