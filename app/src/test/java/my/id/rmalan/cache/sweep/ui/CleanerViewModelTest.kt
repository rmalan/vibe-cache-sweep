package my.id.rmalan.cache.sweep.ui

import android.os.UserHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import my.id.rmalan.cache.sweep.cleaner.CacheCleaner
import my.id.rmalan.cache.sweep.cleaner.CleanupCoordinator
import my.id.rmalan.cache.sweep.model.CleanerBatchResult
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.model.CleaningProgress
import my.id.rmalan.cache.sweep.model.CleaningState
import my.id.rmalan.cache.sweep.model.CleanupMode
import my.id.rmalan.cache.sweep.model.CleanupPlan
import my.id.rmalan.cache.sweep.model.DeviceStorageInfo
import my.id.rmalan.cache.sweep.model.PackageStorageStats
import my.id.rmalan.cache.sweep.storage.DeviceStorageRepository
import my.id.rmalan.cache.sweep.storage.StorageStatsRepository
import my.id.rmalan.cache.sweep.ui.viewmodel.CleanerEvent
import my.id.rmalan.cache.sweep.ui.viewmodel.CleanerViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class CleanerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeCleaner = object : CacheCleaner {
        var capabilitiesResult = CleanerCapabilities(
            shizukuAvailable = true,
            shizukuAuthorized = true,
            privilegedUid = 2000,
            supportsSelectiveCacheClear = true,
            supportsGlobalTrim = true
        )

        var executePlanBatchResult = CleanerBatchResult(
            totalAttempted = 2,
            successfulPackages = listOf("com.example.one", "com.example.two"),
            failedPackages = emptyList(),
            errors = emptyMap()
        )

        override suspend fun capabilities(): CleanerCapabilities = capabilitiesResult

        override suspend fun clearPackage(packageName: String, userId: Int): Boolean = true

        override suspend fun clearPackages(
            packages: List<String>,
            userId: Int,
            scannedPackageSet: Set<String>?,
            onProgress: (suspend (CleaningProgress) -> Unit)?
        ): CleanerBatchResult = executePlanBatchResult

        override suspend fun trimGlobally(desiredFreeBytes: Long): Boolean = true

        override suspend fun executePlan(
            plan: CleanupPlan,
            userId: Int,
            scannedPackageSet: Set<String>?,
            onProgress: (suspend (CleaningProgress) -> Unit)?
        ): CleanerBatchResult {
            onProgress?.invoke(
                CleaningProgress(
                    current = 1,
                    total = plan.selectedPackages.size,
                    currentPackageName = plan.selectedPackages.firstOrNull(),
                    currentAppName = "App 1"
                )
            )
            return executePlanBatchResult
        }
    }

    private val fakeStorage = object : DeviceStorageRepository() {
        var currentStorage = DeviceStorageInfo(totalBytes = 100_000_000_000L, availableBytes = 20_000_000_000L)
        override fun snapshot(): DeviceStorageInfo = currentStorage
    }

    private val fakeStatsRepo = object : StorageStatsRepository {
        override fun queryStats(packageName: String, storageUuid: UUID?, userHandle: UserHandle?): PackageStorageStats {
            return PackageStorageStats(
                cacheBytes = 500L,
                appBytes = 100L,
                dataBytes = 100L,
                measurementAvailable = true
            )
        }
    }

    private lateinit var coordinator: CleanupCoordinator
    private lateinit var viewModel: CleanerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coordinator = CleanupCoordinator(
            cleaner = fakeCleaner,
            storage = fakeStorage,
            storageStatsRepository = fakeStatsRepo,
            settlingDelayMillis = 0L, // instant for unit tests
            dispatcher = testDispatcher
        )
        viewModel = CleanerViewModel(coordinator = coordinator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertEquals(CleaningState.Idle, state.cleaningState)
        assertNull(state.pendingPlan)
        assertFalse(state.showConfirmation)
        assertFalse(state.isCleaning)
        assertFalse(state.isCompleted)
        assertFalse(state.isFailed)
    }

    @Test
    fun testRequestCleanShowsConfirmation() = runTest(testDispatcher) {
        val plan = CleanupPlan.selective(
            packages = listOf("com.example.one", "com.example.two"),
            estimatedCacheBytes = 1000L
        )

        viewModel.onEvent(CleanerEvent.RequestClean(plan))

        val state = viewModel.uiState.value
        assertTrue(state.showConfirmation)
        assertEquals(plan, state.pendingPlan)
        assertFalse(state.isCleaning)
    }

    @Test
    fun testDismissConfirmation() = runTest(testDispatcher) {
        val plan = CleanupPlan.selective(
            packages = listOf("com.example.one"),
            estimatedCacheBytes = 500L
        )

        viewModel.onEvent(CleanerEvent.RequestClean(plan))
        assertTrue(viewModel.uiState.value.showConfirmation)

        viewModel.onEvent(CleanerEvent.DismissConfirmation)
        assertFalse(viewModel.uiState.value.showConfirmation)
        assertNull(viewModel.uiState.value.pendingPlan)
    }

    @Test
    fun testConfirmCleanExecutesPlanAndCompletes() = runTest(testDispatcher) {
        val plan = CleanupPlan.selective(
            packages = listOf("com.example.one", "com.example.two"),
            estimatedCacheBytes = 1000L
        )

        // Storage increases after clean
        fakeStorage.currentStorage = DeviceStorageInfo(
            totalBytes = 100_000_000_000L,
            availableBytes = 25_000_000_000L
        )

        viewModel.onEvent(CleanerEvent.RequestClean(plan))
        viewModel.onEvent(CleanerEvent.ConfirmClean)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showConfirmation)
        assertFalse(state.isCleaning)
        assertTrue(state.isCompleted)
        assertNotNull(state.lastResult)

        val result = state.lastResult!!
        assertEquals(2, result.attemptedPackages)
        assertEquals(2, result.successfulPackages)
        assertTrue(result.failedPackages.isEmpty())
        assertEquals(CleanupMode.SELECTIVE, result.mode)
    }

    @Test
    fun testFailedCleanReportsError() = runTest(testDispatcher) {
        fakeCleaner.capabilitiesResult = CleanerCapabilities(
            shizukuAvailable = false,
            shizukuAuthorized = false,
            privilegedUid = null,
            supportsSelectiveCacheClear = false,
            supportsGlobalTrim = false
        )

        val plan = CleanupPlan.selective(
            packages = listOf("com.example.one"),
            estimatedCacheBytes = 500L
        )

        viewModel.onEvent(CleanerEvent.RequestClean(plan))
        viewModel.onEvent(CleanerEvent.ConfirmClean)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isFailed)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Shizuku service is not running"))
    }

    @Test
    fun testGlobalTrimFallbackRequest() = runTest(testDispatcher) {
        val deviceStorage = DeviceStorageInfo(
            totalBytes = 100_000_000_000L,
            availableBytes = 20_000_000_000L
        )

        viewModel.onEvent(
            CleanerEvent.RequestGlobalTrimFallback(
                deviceStorage = deviceStorage,
                estimatedCacheBytes = 5_000_000_000L
            )
        )

        val state = viewModel.uiState.value
        assertTrue(state.showConfirmation)
        assertNotNull(state.pendingPlan)
        assertEquals(CleanupMode.GLOBAL_TRIM, state.pendingPlan?.mode)
        assertEquals(5_000_000_000L, state.pendingPlan?.estimatedCacheBytes)
    }

    @Test
    fun testDismissResultResetsState() = runTest(testDispatcher) {
        val plan = CleanupPlan.selective(
            packages = listOf("com.example.one"),
            estimatedCacheBytes = 500L
        )

        viewModel.onEvent(CleanerEvent.RequestClean(plan))
        viewModel.onEvent(CleanerEvent.ConfirmClean)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCompleted)

        viewModel.onEvent(CleanerEvent.DismissResult)

        val state = viewModel.uiState.value
        assertEquals(CleaningState.Idle, state.cleaningState)
        assertNull(state.pendingPlan)
        assertNull(state.lastResult)
    }
}
