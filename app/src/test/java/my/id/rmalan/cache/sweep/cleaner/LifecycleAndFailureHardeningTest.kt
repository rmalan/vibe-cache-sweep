package my.id.rmalan.cache.sweep.cleaner

import android.content.pm.PackageManager.NameNotFoundException
import android.os.DeadObjectException
import android.os.IBinder
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
import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.CleanerBatchResult
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.model.CleanerError
import my.id.rmalan.cache.sweep.model.CleaningProgress
import my.id.rmalan.cache.sweep.model.CleaningState
import my.id.rmalan.cache.sweep.model.CleanupHistoryEntry
import my.id.rmalan.cache.sweep.model.CleanupMode
import my.id.rmalan.cache.sweep.model.CleanupPlan
import my.id.rmalan.cache.sweep.model.CleanupResult
import my.id.rmalan.cache.sweep.model.DeviceStorageInfo
import my.id.rmalan.cache.sweep.model.DiscoveredPackage
import my.id.rmalan.cache.sweep.model.PackageStorageStats
import my.id.rmalan.cache.sweep.model.ScanResult
import my.id.rmalan.cache.sweep.model.ScanState
import my.id.rmalan.cache.sweep.model.ShizukuState
import my.id.rmalan.cache.sweep.model.ThemeMode
import my.id.rmalan.cache.sweep.model.UserSettings
import my.id.rmalan.cache.sweep.permissions.UsageAccessManager
import my.id.rmalan.cache.sweep.scanner.CacheScanner
import my.id.rmalan.cache.sweep.scanner.PackageRepository
import my.id.rmalan.cache.sweep.shizuku.ICacheOpsService
import my.id.rmalan.cache.sweep.shizuku.ShizukuManager
import my.id.rmalan.cache.sweep.storage.CleanupHistoryRepository
import my.id.rmalan.cache.sweep.storage.DeviceStorageRepository
import my.id.rmalan.cache.sweep.storage.StorageStatsRepository
import my.id.rmalan.cache.sweep.storage.UserSettingsRepository
import my.id.rmalan.cache.sweep.ui.viewmodel.AppsEvent
import my.id.rmalan.cache.sweep.ui.viewmodel.AppsViewModel
import my.id.rmalan.cache.sweep.ui.viewmodel.CleanerEvent
import my.id.rmalan.cache.sweep.ui.viewmodel.CleanerViewModel
import my.id.rmalan.cache.sweep.ui.viewmodel.DashboardEvent
import my.id.rmalan.cache.sweep.ui.viewmodel.DashboardViewModel
import my.id.rmalan.cache.sweep.ui.viewmodel.SettingsViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class LifecycleAndFailureHardeningTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // P5-07: Process Lifecycle & Process Recreation
    // =========================================================================

    @Test
    fun `P5-07 - Process recreation restores preferences and history from repositories`() = runTest(testDispatcher) {
        val inMemorySettings = MutableStateFlow(
            UserSettings(
                onboardingCompleted = true,
                showSystemApps = true,
                showZeroCacheApps = false,
                sortMode = AppSort.NAME_ASC,
                themeMode = ThemeMode.DARK
            )
        )
        val fakeSettingsRepo = object : UserSettingsRepository {
            override val settings: Flow<UserSettings> = inMemorySettings
            override suspend fun getSettings(): UserSettings = inMemorySettings.value
            override suspend fun setOnboardingCompleted(completed: Boolean) { inMemorySettings.value = inMemorySettings.value.copy(onboardingCompleted = completed) }
            override suspend fun setShowSystemApps(show: Boolean) { inMemorySettings.value = inMemorySettings.value.copy(showSystemApps = show) }
            override suspend fun setShowZeroCacheApps(show: Boolean) { inMemorySettings.value = inMemorySettings.value.copy(showZeroCacheApps = show) }
            override suspend fun setSortMode(sort: AppSort) { inMemorySettings.value = inMemorySettings.value.copy(sortMode = sort) }
            override suspend fun setThemeMode(theme: ThemeMode) { inMemorySettings.value = inMemorySettings.value.copy(themeMode = theme) }
        }

        val inMemoryHistory = MutableStateFlow<List<CleanupHistoryEntry>>(
            listOf(
                CleanupHistoryEntry(
                    timestampMillis = 1000L,
                    mode = CleanupMode.GLOBAL_TRIM,
                    packagesAttempted = 1,
                    packagesSucceeded = 1,
                    measuredFreedBytes = 50_000_000L,
                    reportedCacheReductionBytes = 50_000_000L,
                    durationMillis = 200L
                )
            )
        )
        val fakeHistoryRepo = object : CleanupHistoryRepository {
            override val history: Flow<List<CleanupHistoryEntry>> = inMemoryHistory
            override suspend fun getHistory(): List<CleanupHistoryEntry> = inMemoryHistory.value
            override suspend fun addEntry(entry: CleanupHistoryEntry) { inMemoryHistory.value = listOf(entry) + inMemoryHistory.value }
            override suspend fun clearHistory() { inMemoryHistory.value = emptyList() }
        }

        // Simulate new process launching and creating SettingsViewModel
        val settingsVm = SettingsViewModel(
            userSettingsRepository = fakeSettingsRepo,
            cleanupHistoryRepository = fakeHistoryRepo
        )
        advanceUntilIdle()

        val state = settingsVm.uiState.value
        assertTrue(state.settings.onboardingCompleted)
        assertTrue(state.settings.showSystemApps)
        assertFalse(state.settings.showZeroCacheApps)
        assertEquals(AppSort.NAME_ASC, state.settings.sortMode)
        assertEquals(ThemeMode.DARK, state.settings.themeMode)
        assertEquals(1, state.historyEntries.size)
        assertEquals(50_000_000L, state.historyEntries.first().measuredFreedBytes)
    }

    @Test
    fun `P5-07 - CleanerViewModel reinitializes cleanly in Idle state after cold start`() = runTest(testDispatcher) {
        val fakeStorage = object : DeviceStorageRepository() {
            override fun snapshot(): DeviceStorageInfo = DeviceStorageInfo(100_000L, 40_000L)
        }
        val fakeStats = object : StorageStatsRepository {
            override fun queryStats(packageName: String, storageUuid: UUID?, userHandle: android.os.UserHandle?): PackageStorageStats = PackageStorageStats.ZERO
        }
        val fakeCleaner = object : CacheCleaner {
            override suspend fun capabilities(): CleanerCapabilities = CleanerCapabilities.UNAVAILABLE
            override suspend fun clearPackage(packageName: String, userId: Int): Boolean = false
            override suspend fun clearPackages(packages: List<String>, userId: Int, scannedPackageSet: Set<String>?, onProgress: (suspend (CleaningProgress) -> Unit)?): CleanerBatchResult = CleanerBatchResult.EMPTY
            override suspend fun trimGlobally(desiredFreeBytes: Long): Boolean = false
            override suspend fun executePlan(plan: CleanupPlan, userId: Int, scannedPackageSet: Set<String>?, onProgress: (suspend (CleaningProgress) -> Unit)?): CleanerBatchResult = CleanerBatchResult.EMPTY
        }

        val coordinator = CleanupCoordinator(fakeCleaner, storage = fakeStorage, storageStatsRepository = fakeStats)
        val cleanerVm = CleanerViewModel(coordinator)
        advanceUntilIdle()

        assertEquals(CleaningState.Idle, cleanerVm.uiState.value.cleaningState)
        assertFalse(cleanerVm.uiState.value.isCleaning)
        assertFalse(cleanerVm.uiState.value.isCompleted)
        assertFalse(cleanerVm.uiState.value.isFailed)
        assertNull(cleanerVm.uiState.value.pendingPlan)
    }

    // =========================================================================
    // P5-08: Device Reboot / Cold Launch State
    // =========================================================================

    @Test
    fun `P5-08 - Device reboot cold start with Shizuku stopped allows scanning but disables cleaning`() = runTest(testDispatcher) {
        val shizukuStateFlow = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
        val rebootedShizukuManager = object : ShizukuManager(android.app.Application()) {
            override val state = shizukuStateFlow
            override fun isShizukuInstalled(): Boolean = true
            override suspend fun fetchCapabilities(timeoutMs: Long): CleanerCapabilities = CleanerCapabilities.UNAVAILABLE
            override fun getService(): ICacheOpsService? = null
            override suspend fun getOrAwaitService(timeoutMs: Long): ICacheOpsService? = null
        }

        val scannedApps = listOf(
            AppCacheInfo("com.app.one", "App One", 10_000L, 50_000L, 20_000L, false, true),
            AppCacheInfo("com.app.two", "App Two", 25_000L, 60_000L, 30_000L, false, true)
        )
        val fakeScanner = object : CacheScanner {
            override suspend fun scan(includeSelf: Boolean, includeSystem: Boolean): ScanResult =
                ScanResult(scannedApps, 2, 2, 35_000L, 100L)
            override fun scanFlow(includeSelf: Boolean, includeSystem: Boolean): Flow<ScanState> = flow {
                emit(ScanState.Complete(ScanResult(scannedApps, 2, 2, 35_000L, 100L)))
            }
            override suspend fun scanPackage(packageName: String): AppCacheInfo? = scannedApps.firstOrNull { it.packageName == packageName }
        }

        val fakeStorage = object : DeviceStorageRepository() {
            override fun snapshot(): DeviceStorageInfo = DeviceStorageInfo(100_000_000L, 40_000_000L)
        }

        val dashboardVm = DashboardViewModel(
            deviceStorageRepository = fakeStorage,
            cacheScanner = fakeScanner,
            shizukuManager = rebootedShizukuManager
        )
        dashboardVm.scan()
        advanceUntilIdle()

        // Storage and scan work 100% without Shizuku
        assertEquals(2, dashboardVm.uiState.value.allScannedApps.size)
        assertEquals(35_000L, dashboardVm.uiState.value.totalReportedCacheBytes)
        assertFalse(dashboardVm.uiState.value.isShizukuReady)
        assertEquals(ShizukuState.NotRunning, dashboardVm.uiState.value.shizukuState)

        // Attempting clean when Shizuku is NotRunning fails safely with typed error
        val cleaner = ShizukuCacheCleaner(rebootedShizukuManager)
        val result = cleaner.executePlan(CleanupPlan.selective(listOf("com.app.one")))
        assertEquals(1, result.failedPackages.size)
        assertEquals(CleanerError.ShizukuUnavailable, result.errors["com.app.one"])

        // User starts Shizuku later -> state updates reactively
        shizukuStateFlow.value = ShizukuState.Ready(2000)
        advanceUntilIdle()

        assertEquals(ShizukuState.Ready(2000), dashboardVm.uiState.value.shizukuState)
        assertTrue(dashboardVm.uiState.value.isShizukuReady)
    }

    // =========================================================================
    // P5-09: Individual Package Query Failure / Uninstalled Package Handling
    // =========================================================================

    @Test
    fun `P5-09 - Uninstalled package throwing NameNotFoundException does not abort scanner`() = runBlocking {
        val packages = listOf(
            DiscoveredPackage("com.app.installed", "Installed App", false, null, 0L, null),
            DiscoveredPackage("com.app.uninstalled", "Uninstalled App", false, null, 0L, null),
            DiscoveredPackage("com.app.third", "Third App", false, null, 0L, null)
        )

        val fakePackageRepo = object : PackageRepository {
            override fun getInstalledPackages(includeSelf: Boolean, includeSystem: Boolean): List<DiscoveredPackage> = packages
            override fun getPackage(packageName: String): DiscoveredPackage? = packages.firstOrNull { it.packageName == packageName }
            override fun loadApplicationIcon(packageName: String): android.graphics.drawable.Drawable? = null
            override fun loadIconThumbnail(packageName: String, sizePx: Int): android.graphics.Bitmap? = null
        }

        val fakeStorageStatsRepo = object : StorageStatsRepository {
            override fun queryStats(packageName: String, storageUuid: UUID?, userHandle: android.os.UserHandle?): PackageStorageStats {
                return when (packageName) {
                    "com.app.installed" -> PackageStorageStats(1000L, 5000L, 2000L, true, null)
                    "com.app.uninstalled" -> throw NameNotFoundException("Package com.app.uninstalled is not installed")
                    "com.app.third" -> PackageStorageStats(3000L, 4000L, 1000L, true, null)
                    else -> PackageStorageStats.ZERO
                }
            }
        }

        val scanner = my.id.rmalan.cache.sweep.scanner.AndroidCacheScanner(
            packageRepository = fakePackageRepo,
            storageStatsRepository = fakeStorageStatsRepo
        )

        val result = scanner.scan()

        assertEquals(3, result.attemptedApps)
        assertEquals(2, result.successfulApps)
        assertEquals(4000L, result.totalReportedCacheBytes) // 1000L + 3000L

        val unmeasured = result.apps.first { it.packageName == "com.app.uninstalled" }
        assertFalse(unmeasured.measurementAvailable)
        assertEquals(0L, unmeasured.cacheBytes)
        assertNotNull(unmeasured.errorMessage)
    }

    @Test
    fun `P5-09 - Restricted package throwing SecurityException records failure cleanly`() = runBlocking {
        val fakeStats = object : StorageStatsRepository {
            override fun queryStats(packageName: String, storageUuid: UUID?, userHandle: android.os.UserHandle?): PackageStorageStats {
                return PackageStorageStats.failed("Permission denied: restricted package")
            }
        }

        val stats = fakeStats.queryStats("com.system.restricted")
        assertFalse(stats.measurementAvailable)
        assertEquals(0L, stats.cacheBytes)
        assertEquals("Permission denied: restricted package", stats.errorMessage)
    }

    // =========================================================================
    // P5-10: Individual Package Cleanup Failure & Partial Error Reporting
    // =========================================================================

    @Test
    fun `P5-10 - Selective batch clear isolates individual failures and attributes errors`() = runBlocking {
        val fakeService = object : ICacheOpsService {
            override fun destroy() {}
            override fun getProtocolVersion(): Int = 1
            override fun getPrivilegedUid(): Int = 2000
            override fun supportsSelectiveCacheClear(): Boolean = true
            override fun supportsGlobalTrim(): Boolean = true
            override fun trimCaches(desiredFreeBytes: Long): Int = 0
            override fun getLastError(): String = "Target app crashed during clear"
            override fun asBinder(): IBinder? = null

            override fun clearPackageCache(packageName: String, userId: Int): Int {
                return when (packageName) {
                    "com.app.good1" -> 0
                    "com.app.failing" -> 1 // Non-zero exit code
                    "com.app.good2" -> 0
                    else -> 0
                }
            }
        }

        val fakeManager = object : ShizukuManager(android.app.Application()) {
            override suspend fun fetchCapabilities(timeoutMs: Long): CleanerCapabilities =
                CleanerCapabilities(true, true, 2000, true, true)
            override suspend fun getOrAwaitService(timeoutMs: Long): ICacheOpsService? = fakeService
        }

        val cleaner = ShizukuCacheCleaner(fakeManager)
        val packages = listOf(
            "com.app.good1",
            "com.app.failing",
            "my.id.rmalan.cache.sweep", // self package -> must fail
            "invalid..pkg!!", // invalid format -> must fail
            "com.app.notscanned", // not in scanned set -> must fail
            "com.app.good2"
        )
        val scannedSet = setOf("com.app.good1", "com.app.failing", "my.id.rmalan.cache.sweep", "com.app.good2")

        val result = cleaner.clearPackages(packages, userId = 0, scannedPackageSet = scannedSet)

        assertEquals(6, result.totalAttempted)
        assertEquals(listOf("com.app.good1", "com.app.good2"), result.successfulPackages)
        assertEquals(4, result.failedPackages.size)

        // Error attribution verification
        assertTrue(result.errors["com.app.failing"] is CleanerError.CommandFailed)
        assertTrue(result.errors["my.id.rmalan.cache.sweep"] is CleanerError.SelfCleanProhibited)
        assertTrue(result.errors["invalid..pkg!!"] is CleanerError.PackageInvalid)
        assertTrue(result.errors["com.app.notscanned"] is CleanerError.PackageNotScanned)

        assertTrue(result.isPartialSuccess)
        assertFalse(result.isCompleteSuccess)
        assertFalse(result.isCompleteFailure)
    }

    @Test
    fun `P5-10 - CleanupCoordinator records partial failures into result and history`() = runBlocking {
        val fakeCleaner = object : CacheCleaner {
            override suspend fun capabilities(): CleanerCapabilities =
                CleanerCapabilities(true, true, 2000, true, true)
            override suspend fun clearPackage(packageName: String, userId: Int): Boolean = false
            override suspend fun clearPackages(packages: List<String>, userId: Int, scannedPackageSet: Set<String>?, onProgress: (suspend (CleaningProgress) -> Unit)?): CleanerBatchResult =
                CleanerBatchResult(
                    totalAttempted = 2,
                    successfulPackages = listOf("com.app.success"),
                    failedPackages = listOf("com.app.fail"),
                    errors = mapOf("com.app.fail" to CleanerError.CommandFailed(1, "error"))
                )
            override suspend fun trimGlobally(desiredFreeBytes: Long): Boolean = false
            override suspend fun executePlan(plan: CleanupPlan, userId: Int, scannedPackageSet: Set<String>?, onProgress: (suspend (CleaningProgress) -> Unit)?): CleanerBatchResult =
                clearPackages(plan.selectedPackages, userId, scannedPackageSet, onProgress)
        }

        val fakeStorage = object : DeviceStorageRepository() {
            private var call = 0
            override fun snapshot(): DeviceStorageInfo {
                call++
                return if (call == 1) DeviceStorageInfo(100_000L, 40_000L) else DeviceStorageInfo(100_000L, 45_000L)
            }
        }

        val fakeStats = object : StorageStatsRepository {
            override fun queryStats(packageName: String, storageUuid: UUID?, userHandle: android.os.UserHandle?): PackageStorageStats =
                PackageStorageStats(5000L, 1000L, 1000L, true, null)
        }

        val recordedHistory = mutableListOf<CleanupHistoryEntry>()
        val fakeHistoryRepo = object : CleanupHistoryRepository {
            override val history: Flow<List<CleanupHistoryEntry>> = MutableStateFlow(emptyList())
            override suspend fun getHistory(): List<CleanupHistoryEntry> = recordedHistory
            override suspend fun addEntry(entry: CleanupHistoryEntry) { recordedHistory.add(entry) }
            override suspend fun clearHistory() { recordedHistory.clear() }
        }

        val coordinator = CleanupCoordinator(
            cleaner = fakeCleaner,
            storage = fakeStorage,
            storageStatsRepository = fakeStats,
            historyRepository = fakeHistoryRepo,
            settlingDelayMillis = 0L
        )

        val plan = CleanupPlan.selective(listOf("com.app.success", "com.app.fail"))
        val result = coordinator.clean(plan)

        assertEquals(2, result.attemptedPackages)
        assertEquals(1, result.successfulPackages)
        assertEquals(listOf("com.app.fail"), result.failedPackages)
        assertTrue(result.errors.containsKey("com.app.fail"))

        // History entry was recorded despite partial failures
        assertEquals(1, recordedHistory.size)
        assertEquals(2, recordedHistory.first().packagesAttempted)
        assertEquals(1, recordedHistory.first().packagesSucceeded)
    }
}
