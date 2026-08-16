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
import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.ScanResult
import my.id.rmalan.cache.sweep.model.ScanState
import my.id.rmalan.cache.sweep.scanner.CacheScanner
import my.id.rmalan.cache.sweep.ui.viewmodel.AppsEvent
import my.id.rmalan.cache.sweep.ui.viewmodel.AppsViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val testApp1 = AppCacheInfo(
        packageName = "com.test.app1",
        appName = "App One",
        cacheBytes = 500L,
        appBytes = 100L,
        dataBytes = 100L,
        isSystemApp = false,
        measurementAvailable = true
    )

    private val testApp2 = AppCacheInfo(
        packageName = "com.test.app2",
        appName = "App Two",
        cacheBytes = 2000L,
        appBytes = 100L,
        dataBytes = 100L,
        isSystemApp = true,
        measurementAvailable = true
    )

    private val fakeScanner = object : CacheScanner {
        override suspend fun scan(includeSelf: Boolean, includeSystem: Boolean): ScanResult {
            return ScanResult(
                apps = listOf(testApp1, testApp2),
                attemptedApps = 2,
                successfulApps = 2,
                totalReportedCacheBytes = 2500L,
                durationMillis = 50L
            )
        }

        override fun scanFlow(includeSelf: Boolean, includeSystem: Boolean): Flow<ScanState> = flow {
            emit(ScanState.Discovering)
            emit(ScanState.Scanning(scannedCount = 1, totalCount = 2, currentPackageName = testApp1.packageName, currentAppName = testApp1.appName, runningReportedCacheBytes = 500L))
            emit(ScanState.Scanning(scannedCount = 2, totalCount = 2, currentPackageName = testApp2.packageName, currentAppName = testApp2.appName, runningReportedCacheBytes = 2500L))
            emit(ScanState.Complete(scan(includeSelf, includeSystem)))
        }

        override suspend fun scanPackage(packageName: String): AppCacheInfo? {
            return if (packageName == testApp1.packageName) testApp1 else null
        }
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
    fun testInitialScanAndSorting() = runTest(testDispatcher) {
        val viewModel = AppsViewModel(cacheScanner = fakeScanner)
        viewModel.scan()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isScanning)
        assertEquals(2, state.rawApps.size)
        assertEquals(2, state.displayedApps.size)
        // Default sort is CACHE_DESC: testApp2 (2000) then testApp1 (500)
        assertEquals("App Two", state.displayedApps[0].appName)
        assertEquals("App One", state.displayedApps[1].appName)
        assertEquals(2500L, state.totalReportedCacheBytes)
    }

    @Test
    fun testSearchFiltering() = runTest(testDispatcher) {
        val viewModel = AppsViewModel(cacheScanner = fakeScanner)
        viewModel.scan()
        advanceUntilIdle()

        viewModel.onEvent(AppsEvent.SearchChanged("One"))
        val state = viewModel.uiState.value
        assertEquals("One", state.query)
        assertEquals(1, state.displayedApps.size)
        assertEquals("App One", state.displayedApps[0].appName)

        viewModel.onEvent(AppsEvent.SearchChanged(""))
        assertEquals(2, viewModel.uiState.value.displayedApps.size)
    }

    @Test
    fun testSortChange() = runTest(testDispatcher) {
        val viewModel = AppsViewModel(cacheScanner = fakeScanner)
        viewModel.scan()
        advanceUntilIdle()

        viewModel.onEvent(AppsEvent.SortChanged(AppSort.NAME_ASC))
        val state = viewModel.uiState.value
        assertEquals(AppSort.NAME_ASC, state.sort)
        // NAME_ASC: App One, then App Two
        assertEquals("App One", state.displayedApps[0].appName)
        assertEquals("App Two", state.displayedApps[1].appName)
    }

    @Test
    fun testSystemAppsToggle() = runTest(testDispatcher) {
        val viewModel = AppsViewModel(cacheScanner = fakeScanner)
        viewModel.scan()
        advanceUntilIdle()

        viewModel.onEvent(AppsEvent.ToggleShowSystem(false))
        val state = viewModel.uiState.value
        assertFalse(state.showSystemApps)
        assertEquals(1, state.displayedApps.size)
        assertEquals("App One", state.displayedApps[0].appName)
    }

    @Test
    fun testSelectionEvents() = runTest(testDispatcher) {
        val viewModel = AppsViewModel(cacheScanner = fakeScanner)
        viewModel.scan()
        advanceUntilIdle()

        viewModel.onEvent(AppsEvent.ToggleSelected(testApp1.packageName))
        assertTrue(testApp1.packageName in viewModel.uiState.value.selectedPackages)
        assertEquals(500L, viewModel.uiState.value.selectedCacheBytes)

        viewModel.onEvent(AppsEvent.SelectAll)
        assertEquals(2, viewModel.uiState.value.selectedPackages.size)
        assertEquals(2500L, viewModel.uiState.value.selectedCacheBytes)

        viewModel.onEvent(AppsEvent.ClearSelection)
        assertTrue(viewModel.uiState.value.selectedPackages.isEmpty())
        assertEquals(0L, viewModel.uiState.value.selectedCacheBytes)
    }

    @Test
    fun testAppDetailEvents() = runTest(testDispatcher) {
        val viewModel = AppsViewModel(cacheScanner = fakeScanner)

        assertNull(viewModel.uiState.value.selectedAppDetail)
        viewModel.onEvent(AppsEvent.AppClicked(testApp1))
        assertNotNull(viewModel.uiState.value.selectedAppDetail)
        assertEquals(testApp1.packageName, viewModel.uiState.value.selectedAppDetail?.packageName)

        viewModel.onEvent(AppsEvent.DismissDetail)
        assertNull(viewModel.uiState.value.selectedAppDetail)
    }
}
