package my.id.rmalan.cache.sweep.cleaner

import android.os.DeadObjectException
import android.os.IBinder
import android.os.RemoteException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.model.CleanerError
import my.id.rmalan.cache.sweep.model.CleaningState
import my.id.rmalan.cache.sweep.model.CleanupMode
import my.id.rmalan.cache.sweep.model.CleanupPlan
import my.id.rmalan.cache.sweep.model.DeviceStorageInfo
import my.id.rmalan.cache.sweep.model.PackageStorageStats
import my.id.rmalan.cache.sweep.model.ScanResult
import my.id.rmalan.cache.sweep.model.ScanState
import my.id.rmalan.cache.sweep.model.ShizukuState
import my.id.rmalan.cache.sweep.permissions.UsageAccessManager
import my.id.rmalan.cache.sweep.scanner.CacheScanner
import my.id.rmalan.cache.sweep.shizuku.ICacheOpsService
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager
import my.id.rmalan.cache.sweep.storage.AndroidStorageStatsRepository
import my.id.rmalan.cache.sweep.storage.DeviceStorageRepository
import my.id.rmalan.cache.sweep.storage.StorageStatsRepository
import my.id.rmalan.cache.sweep.ui.viewmodel.AppsViewModel
import my.id.rmalan.cache.sweep.ui.viewmodel.CleanerViewModel
import my.id.rmalan.cache.sweep.ui.viewmodel.DashboardViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FailureScenariosHardeningTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==========================================
    // P5-01: Shizuku Absent (Not Installed)
    // ==========================================

    @Test
    fun `P5-01 - Shizuku absent handling in cleaner and dashboard viewmodel`() = runTest(testDispatcher) {
        val uninstalledManager = object : ShizukuManager(android.app.Application()) {
            private val stateFlow = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
            override val state = stateFlow
            override fun isShizukuInstalled(): Boolean = false
            override suspend fun fetchCapabilities(timeoutMs: Long): CleanerCapabilities = CleanerCapabilities.UNAVAILABLE
            override fun getService(): ICacheOpsService? = null
            override suspend fun getOrAwaitService(timeoutMs: Long): ICacheOpsService? = null
        }

        val cleaner = ShizukuCacheCleaner(uninstalledManager)
        val plan = CleanupPlan.selective(listOf("com.android.chrome"))

        val result = cleaner.executePlan(plan)
        assertTrue(result.isCompleteFailure)
        assertEquals(CleanerError.ShizukuUnavailable, result.errors["com.android.chrome"])

        val fakeStorage = object : DeviceStorageRepository() {
            override fun snapshot(): DeviceStorageInfo = DeviceStorageInfo(100_000L, 40_000L)
        }
        val fakeScanner = object : CacheScanner {
            override suspend fun scan(includeSelf: Boolean, includeSystem: Boolean): ScanResult = ScanResult.EMPTY
            override fun scanFlow(includeSelf: Boolean, includeSystem: Boolean): Flow<ScanState> = flow { emit(ScanState.Complete(ScanResult.EMPTY)) }
            override suspend fun scanPackage(packageName: String): AppCacheInfo? = null
        }

        val dashboardVm = DashboardViewModel(
            deviceStorageRepository = fakeStorage,
            cacheScanner = fakeScanner,
            shizukuManager = uninstalledManager
        )
        advanceUntilIdle()

        assertFalse(dashboardVm.uiState.value.isShizukuInstalled)
        assertFalse(dashboardVm.uiState.value.isShizukuReady)
        assertEquals(ShizukuState.NotRunning, dashboardVm.uiState.value.shizukuState)
    }

    // ==========================================
    // P5-02: Shizuku Stopped / Process Killed
    // ==========================================

    @Test
    fun `P5-02 - Shizuku stopped updates reactive state and invalidates capabilities`() = runTest(testDispatcher) {
        val stateFlow = MutableStateFlow<ShizukuState>(ShizukuState.Ready(2000))
        var capsToReturn = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = true,
            privilegedUid = 2000,
            supportsSelectiveCacheClear = true,
            supportsGlobalTrim = true
        )

        val reactiveManager = object : ShizukuManager(android.app.Application()) {
            override val state = stateFlow
            override suspend fun fetchCapabilities(timeoutMs: Long): CleanerCapabilities = capsToReturn
            override fun isShizukuInstalled(): Boolean = true
        }

        val fakeCleaner = object : CacheCleaner {
            override suspend fun capabilities(): CleanerCapabilities = reactiveManager.fetchCapabilities()
            override suspend fun clearPackage(packageName: String, userId: Int): Boolean = true
            override suspend fun clearPackages(packages: List<String>, userId: Int, scannedPackageSet: Set<String>?, onProgress: (suspend (my.id.rmalan.cache.sweep.model.CleaningProgress) -> Unit)?): my.id.rmalan.cache.sweep.model.CleanerBatchResult = my.id.rmalan.cache.sweep.model.CleanerBatchResult.EMPTY
            override suspend fun trimGlobally(desiredFreeBytes: Long): Boolean = true
            override suspend fun executePlan(plan: CleanupPlan, userId: Int, scannedPackageSet: Set<String>?, onProgress: (suspend (my.id.rmalan.cache.sweep.model.CleaningProgress) -> Unit)?): my.id.rmalan.cache.sweep.model.CleanerBatchResult = my.id.rmalan.cache.sweep.model.CleanerBatchResult.EMPTY
        }

        val fakeStorage = object : DeviceStorageRepository() {
            override fun snapshot(): DeviceStorageInfo = DeviceStorageInfo(100_000L, 40_000L)
        }
        val fakeStats = object : StorageStatsRepository {
            override fun queryStats(packageName: String, storageUuid: java.util.UUID?, userHandle: android.os.UserHandle?): PackageStorageStats = PackageStorageStats.ZERO
        }

        val coordinator = CleanupCoordinator(
            cleaner = fakeCleaner,
            storage = fakeStorage,
            storageStatsRepository = fakeStats
        )

        val cleanerVm = CleanerViewModel(
            coordinator = coordinator,
            shizukuManager = reactiveManager
        )
        advanceUntilIdle()

        assertTrue(cleanerVm.uiState.value.capabilities?.isReady == true)

        // Simulate Shizuku stopping
        capsToReturn = CleanerCapabilities.UNAVAILABLE
        stateFlow.value = ShizukuState.NotRunning
        advanceUntilIdle()

        assertFalse(cleanerVm.uiState.value.capabilities?.isReady == true)
    }

    // ==========================================
    // P5-03: Shizuku Dies Mid-Cleanup
    // ==========================================

    @Test
    fun `P5-03 - Shizuku binder dies mid-batch cleanup fails remaining packages immediately`() = runBlocking {
        val dyingService = object : ICacheOpsService {
            var callCount = 0
            override fun destroy() {}
            override fun getProtocolVersion(): Int = 1
            override fun getPrivilegedUid(): Int = 2000
            override fun supportsSelectiveCacheClear(): Boolean = true
            override fun supportsGlobalTrim(): Boolean = true
            override fun trimCaches(desiredFreeBytes: Long): Int = 0
            override fun getLastError(): String = ""
            override fun asBinder(): IBinder? = null

            override fun clearPackageCache(packageName: String, userId: Int): Int {
                callCount++
                if (callCount == 1) {
                    return 0 // first app succeeds
                }
                // Second app triggers dead binder
                throw DeadObjectException()
            }
        }

        val fakeManager = object : ShizukuManager(android.app.Application()) {
            override suspend fun fetchCapabilities(timeoutMs: Long): CleanerCapabilities =
                CleanerCapabilities(true, true, 2000, true, true)
            override suspend fun getOrAwaitService(timeoutMs: Long): ICacheOpsService? = dyingService
            override fun getService(): ICacheOpsService? = dyingService
        }

        val cleaner = ShizukuCacheCleaner(fakeManager)
        val packages = listOf("com.app.first", "com.app.second", "com.app.third", "com.app.fourth")

        val result = cleaner.clearPackages(packages, userId = 0)

        assertEquals(4, result.totalAttempted)
        assertEquals(listOf("com.app.first"), result.successfulPackages)
        assertEquals(listOf("com.app.second", "com.app.third", "com.app.fourth"), result.failedPackages)
        assertEquals(CleanerError.ShizukuUnavailable, result.errors["com.app.second"])
        assertEquals(CleanerError.ShizukuUnavailable, result.errors["com.app.third"])
        assertEquals(CleanerError.ShizukuUnavailable, result.errors["com.app.fourth"])
        // Verify IPC was only called twice, not 4 times (failed immediately upon dead object)
        assertEquals(2, dyingService.callCount)
    }

    @Test
    fun `P5-03 - CleanupCoordinator handles dying cleaner safely without hanging`() = runBlocking {
        val dyingCleaner = object : CacheCleaner {
            override suspend fun capabilities(): CleanerCapabilities =
                CleanerCapabilities(true, true, 2000, true, true)
            override suspend fun clearPackage(packageName: String, userId: Int): Boolean = false
            override suspend fun clearPackages(packages: List<String>, userId: Int, scannedPackageSet: Set<String>?, onProgress: (suspend (my.id.rmalan.cache.sweep.model.CleaningProgress) -> Unit)?): my.id.rmalan.cache.sweep.model.CleanerBatchResult =
                my.id.rmalan.cache.sweep.model.CleanerBatchResult(
                    totalAttempted = packages.size,
                    successfulPackages = emptyList(),
                    failedPackages = packages,
                    errors = packages.associateWith { CleanerError.ShizukuUnavailable }
                )
            override suspend fun trimGlobally(desiredFreeBytes: Long): Boolean = false
            override suspend fun executePlan(plan: CleanupPlan, userId: Int, scannedPackageSet: Set<String>?, onProgress: (suspend (my.id.rmalan.cache.sweep.model.CleaningProgress) -> Unit)?): my.id.rmalan.cache.sweep.model.CleanerBatchResult =
                clearPackages(plan.selectedPackages, userId, scannedPackageSet, onProgress)
        }

        val fakeStorage = object : DeviceStorageRepository() {
            override fun snapshot(): DeviceStorageInfo = DeviceStorageInfo(100_000L, 40_000L)
        }
        val fakeStats = object : StorageStatsRepository {
            override fun queryStats(packageName: String, storageUuid: java.util.UUID?, userHandle: android.os.UserHandle?): PackageStorageStats =
                PackageStorageStats(1000L, 100L, 100L, true, null)
        }

        val coordinator = CleanupCoordinator(
            cleaner = dyingCleaner,
            storage = fakeStorage,
            storageStatsRepository = fakeStats,
            settlingDelayMillis = 0L
        )

        val states = mutableListOf<CleaningState>()
        val plan = CleanupPlan.selective(listOf("com.app.one", "com.app.two"))

        val result = coordinator.clean(plan) { state -> states.add(state) }

        assertEquals(2, result.attemptedPackages)
        assertEquals(0, result.successfulPackages)
        assertEquals(2, result.failedPackages.size)
        assertTrue(states.any { it is CleaningState.SnapshotBefore })
        assertTrue(states.any { it is CleaningState.Completed })
    }

    // ==========================================
    // P5-04 & P5-05: Permission Denied & Revoked
    // ==========================================

    @Test
    fun `P5-04 and P5-05 - Shizuku permission denied or revoked blocks cleaning with typed error`() = runBlocking {
        val deniedCapabilities = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = false,
            privilegedUid = null,
            supportsSelectiveCacheClear = false,
            supportsGlobalTrim = false
        )

        val deniedCleaner = object : CacheCleaner {
            override suspend fun capabilities(): CleanerCapabilities = deniedCapabilities
            override suspend fun clearPackage(packageName: String, userId: Int): Boolean = false
            override suspend fun clearPackages(packages: List<String>, userId: Int, scannedPackageSet: Set<String>?, onProgress: (suspend (my.id.rmalan.cache.sweep.model.CleaningProgress) -> Unit)?): my.id.rmalan.cache.sweep.model.CleanerBatchResult =
                my.id.rmalan.cache.sweep.model.CleanerBatchResult(
                    totalAttempted = packages.size,
                    successfulPackages = emptyList(),
                    failedPackages = packages,
                    errors = packages.associateWith { CleanerError.PermissionDenied }
                )
            override suspend fun trimGlobally(desiredFreeBytes: Long): Boolean = false
            override suspend fun executePlan(plan: CleanupPlan, userId: Int, scannedPackageSet: Set<String>?, onProgress: (suspend (my.id.rmalan.cache.sweep.model.CleaningProgress) -> Unit)?): my.id.rmalan.cache.sweep.model.CleanerBatchResult =
                clearPackages(plan.selectedPackages, userId, scannedPackageSet, onProgress)
        }

        val fakeStorage = object : DeviceStorageRepository() {
            override fun snapshot(): DeviceStorageInfo = DeviceStorageInfo(100_000L, 40_000L)
        }
        val fakeStats = object : StorageStatsRepository {
            override fun queryStats(packageName: String, storageUuid: java.util.UUID?, userHandle: android.os.UserHandle?): PackageStorageStats = PackageStorageStats.ZERO
        }

        val coordinator = CleanupCoordinator(
            cleaner = deniedCleaner,
            storage = fakeStorage,
            storageStatsRepository = fakeStats,
            settlingDelayMillis = 0L
        )

        val plan = CleanupPlan.selective(listOf("com.android.chrome"))
        val result = coordinator.clean(plan)

        assertEquals(0, result.successfulPackages)
        assertEquals(CleanerError.PermissionDenied, result.errors["cleanup"])
    }

    // ==========================================
    // P5-06: Usage Access Revoked
    // ==========================================

    @Test
    fun `P5-06 - Usage Access revoked handling in storage repository and viewmodels`() = runTest(testDispatcher) {
        val fakeUsageManager = object : UsageAccessManager {
            var access: Boolean = false
            override fun hasAccess(): Boolean = access
            override fun createSettingsIntent(): android.content.Intent = android.content.Intent()
        }

        val fakeStorage = object : DeviceStorageRepository() {
            override fun snapshot(): DeviceStorageInfo = DeviceStorageInfo(100_000L, 40_000L)
        }
        val fakeScanner = object : CacheScanner {
            override suspend fun scan(includeSelf: Boolean, includeSystem: Boolean): ScanResult = ScanResult.EMPTY
            override fun scanFlow(includeSelf: Boolean, includeSystem: Boolean): Flow<ScanState> = flow { emit(ScanState.Complete(ScanResult.EMPTY)) }
            override suspend fun scanPackage(packageName: String): AppCacheInfo? = null
        }

        // Dashboard ViewModel handles missing Usage Access
        val dashboardVm = DashboardViewModel(
            deviceStorageRepository = fakeStorage,
            cacheScanner = fakeScanner,
            usageAccessManager = fakeUsageManager
        )
        advanceUntilIdle()

        assertFalse(dashboardVm.uiState.value.hasUsageAccess)
        assertNotNull(dashboardVm.uiState.value.errorMessage)
        assertTrue(dashboardVm.uiState.value.errorMessage?.contains("Usage Access") == true)

        // Apps ViewModel handles missing Usage Access
        val appsVm = AppsViewModel(
            cacheScanner = fakeScanner,
            usageAccessManager = fakeUsageManager
        )
        appsVm.scan()
        advanceUntilIdle()

        assertFalse(appsVm.uiState.value.hasUsageAccess)
        assertNotNull(appsVm.uiState.value.errorMessage)
        assertTrue(appsVm.uiState.value.errorMessage?.contains("Usage Access") == true)

        // Granted again
        fakeUsageManager.access = true
        dashboardVm.refreshStatus()
        dashboardVm.scan()
        advanceUntilIdle()

        assertTrue(dashboardVm.uiState.value.hasUsageAccess)
        assertNull(dashboardVm.uiState.value.errorMessage)
    }
}
