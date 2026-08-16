package my.id.rmalan.cache.sweep.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import my.id.rmalan.cache.sweep.model.AppCacheInfo
import my.id.rmalan.cache.sweep.model.DeviceStorageInfo
import my.id.rmalan.cache.sweep.model.ScanResult
import my.id.rmalan.cache.sweep.model.ScanState
import my.id.rmalan.cache.sweep.scanner.CacheScanner
import my.id.rmalan.cache.sweep.storage.DeviceStorageRepository
import my.id.rmalan.cache.sweep.ui.viewmodel.DashboardEvent
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
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val app1 = AppCacheInfo("com.app1", "App 1", cacheBytes = 1000L, appBytes = 100L, dataBytes = 100L, isSystemApp = false, measurementAvailable = true)
    private val app2 = AppCacheInfo("com.app2", "App 2", cacheBytes = 5000L, appBytes = 100L, dataBytes = 100L, isSystemApp = false, measurementAvailable = true)
    private val app3 = AppCacheInfo("com.app3", "App 3", cacheBytes = 2000L, appBytes = 100L, dataBytes = 100L, isSystemApp = false, measurementAvailable = true)
    private val app4 = AppCacheInfo("com.app4", "App 4", cacheBytes = 8000L, appBytes = 100L, dataBytes = 100L, isSystemApp = false, measurementAvailable = true)
    private val app5 = AppCacheInfo("com.app5", "App 5", cacheBytes = 3000L, appBytes = 100L, dataBytes = 100L, isSystemApp = false, measurementAvailable = true)
    private val app6 = AppCacheInfo("com.app6", "App 6", cacheBytes = 500L, appBytes = 100L, dataBytes = 100L, isSystemApp = false, measurementAvailable = true)
    private val zeroCacheApp = AppCacheInfo("com.zero", "Zero App", cacheBytes = 0L, appBytes = 100L, dataBytes = 100L, isSystemApp = false, measurementAvailable = true)

    private val fakeStorageRepo = object : DeviceStorageRepository() {
        var total = 100_000L
        var available = 40_000L

        override fun snapshot(): DeviceStorageInfo {
            return DeviceStorageInfo(totalBytes = total, availableBytes = available)
        }
    }

    private val fakeScanner = object : CacheScanner {
        var appsToReturn = listOf(app1, app2, app3, app4, app5, app6, zeroCacheApp)
        var attempted = 7
        var successful = 7

        override suspend fun scan(includeSelf: Boolean, includeSystem: Boolean): ScanResult {
            return ScanResult(
                apps = appsToReturn,
                attemptedApps = attempted,
                successfulApps = successful,
                totalReportedCacheBytes = appsToReturn.sumOf { it.cacheBytes },
                durationMillis = 120L
            )
        }

        override fun scanFlow(includeSelf: Boolean, includeSystem: Boolean): Flow<ScanState> = flow {
            emit(ScanState.Discovering)
            emit(ScanState.Complete(scan(includeSelf, includeSystem)))
        }

        override suspend fun scanPackage(packageName: String): AppCacheInfo? = null
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialDashboardStateAndStorage() = runTest(testDispatcher) {
        val viewModel = DashboardViewModel(
            deviceStorageRepository = fakeStorageRepo,
            cacheScanner = fakeScanner
        )

        // Storage snapshot is available immediately upon initialization
        val initialState = viewModel.uiState.value
        assertNotNull(initialState.deviceStorage)
        assertEquals(100_000L, initialState.deviceStorage?.totalBytes)
        assertEquals(40_000L, initialState.deviceStorage?.availableBytes)
        assertEquals(60_000L, initialState.deviceStorage?.usedBytes)
        assertEquals(0.6f, initialState.usedStoragePercentage, 0.001f)

        advanceUntilIdle()

        val loadedState = viewModel.uiState.value
        assertFalse(loadedState.isLoading)
        assertFalse(loadedState.isScanning)
        assertEquals(19500L, loadedState.totalReportedCacheBytes)
        assertEquals(7, loadedState.scannedAppsCount)
        assertEquals(7, loadedState.measuredAppsCount)
        assertFalse(loadedState.hasPartialFailures)

        // Top 5 largest apps with cache > 0:
        // app4 (8000), app2 (5000), app5 (3000), app3 (2000), app1 (1000)
        assertEquals(5, loadedState.largestApps.size)
        assertEquals("App 4", loadedState.largestApps[0].appName)
        assertEquals("App 2", loadedState.largestApps[1].appName)
        assertEquals("App 5", loadedState.largestApps[2].appName)
        assertEquals("App 3", loadedState.largestApps[3].appName)
        assertEquals("App 1", loadedState.largestApps[4].appName)
        assertEquals(120L, loadedState.scanDurationMillis)
        assertTrue(loadedState.lastScanTimeMillis > 0L)
    }

    @Test
    fun testPartialFailuresDetected() = runTest(testDispatcher) {
        fakeScanner.attempted = 10
        fakeScanner.successful = 7

        val viewModel = DashboardViewModel(
            deviceStorageRepository = fakeStorageRepo,
            cacheScanner = fakeScanner
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasPartialFailures)
        assertEquals(10, state.scannedAppsCount)
        assertEquals(7, state.measuredAppsCount)
    }

    @Test
    fun testAppDetailBottomsheetEvents() = runTest(testDispatcher) {
        val viewModel = DashboardViewModel(
            deviceStorageRepository = fakeStorageRepo,
            cacheScanner = fakeScanner
        )
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedAppDetail)

        viewModel.onEvent(DashboardEvent.AppClicked(app4))
        assertEquals(app4, viewModel.uiState.value.selectedAppDetail)

        viewModel.onEvent(DashboardEvent.DismissDetail)
        assertNull(viewModel.uiState.value.selectedAppDetail)
    }

    @Test
    fun testRefreshUpdatesStorageAndCache() = runTest(testDispatcher) {
        val viewModel = DashboardViewModel(
            deviceStorageRepository = fakeStorageRepo,
            cacheScanner = fakeScanner
        )
        advanceUntilIdle()

        // Modify fake storage & scan data
        fakeStorageRepo.available = 50_000L
        fakeScanner.appsToReturn = listOf(app1)
        fakeScanner.attempted = 1
        fakeScanner.successful = 1

        viewModel.onEvent(DashboardEvent.Refresh)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(50_000L, state.deviceStorage?.availableBytes)
        assertEquals(0.5f, state.usedStoragePercentage, 0.001f)
        assertEquals(1000L, state.totalReportedCacheBytes)
        assertEquals(1, state.scannedAppsCount)
        assertEquals(1, state.largestApps.size)
    }

    @Test
    fun testScannerFailureHandling() = runTest(testDispatcher) {
        val errorScanner = object : CacheScanner {
            override suspend fun scan(includeSelf: Boolean, includeSystem: Boolean): ScanResult {
                throw RuntimeException("Scanner failed")
            }

            override fun scanFlow(includeSelf: Boolean, includeSystem: Boolean): Flow<ScanState> = flow {
                emit(ScanState.Failed(RuntimeException("Scanner failed"), "Scanner failed"))
            }

            override suspend fun scanPackage(packageName: String): AppCacheInfo? = null
        }

        val viewModel = DashboardViewModel(
            deviceStorageRepository = fakeStorageRepo,
            cacheScanner = errorScanner
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isScanning)
        assertEquals("Scanner failed", state.errorMessage)

        viewModel.onEvent(DashboardEvent.ClearError)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
