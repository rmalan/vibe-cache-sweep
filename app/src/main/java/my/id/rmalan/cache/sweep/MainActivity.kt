package my.id.rmalan.cache.sweep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import my.id.rmalan.cache.sweep.ui.screens.AppCacheListScreen
import my.id.rmalan.cache.sweep.ui.screens.DashboardScreen
import my.id.rmalan.cache.sweep.ui.screens.DiagnosticScreen
import my.id.rmalan.cache.sweep.ui.screens.OnboardingScreen
import my.id.rmalan.cache.sweep.ui.theme.CacheSweepTheme
import my.id.rmalan.cache.sweep.ui.viewmodel.AppsViewModel
import my.id.rmalan.cache.sweep.ui.viewmodel.CleanerViewModel
import my.id.rmalan.cache.sweep.ui.viewmodel.DashboardViewModel
import my.id.rmalan.cache.sweep.ui.viewmodel.OnboardingViewModel

enum class MainDestination {
    ONBOARDING,
    DASHBOARD,
    DIAGNOSTIC,
    APP_CACHE_LIST
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as CacheSweepApp

        setContent {
            CacheSweepTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val settingsState by app.container.userSettingsRepository.settings.collectAsState(initial = null)

                    val initialSettings = settingsState
                    if (initialSettings == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                        return@Surface
                    }

                    var currentDestination by remember(initialSettings.onboardingCompleted) {
                        mutableStateOf(
                            if (initialSettings.onboardingCompleted) {
                                MainDestination.DASHBOARD
                            } else {
                                MainDestination.ONBOARDING
                            }
                        )
                    }

                    when (currentDestination) {
                        MainDestination.ONBOARDING -> {
                            val onboardingViewModel: OnboardingViewModel = viewModel(
                                factory = OnboardingViewModel.Factory(
                                    usageAccessManager = app.container.usageAccessManager,
                                    shizukuManager = app.container.shizukuManager,
                                    userSettingsRepository = app.container.userSettingsRepository
                                )
                            )
                            OnboardingScreen(
                                viewModel = onboardingViewModel,
                                usageAccessManager = app.container.usageAccessManager,
                                shizukuManager = app.container.shizukuManager,
                                onFinishOnboarding = {
                                    currentDestination = MainDestination.DASHBOARD
                                },
                                onSkipToDiagnostic = {
                                    currentDestination = MainDestination.DIAGNOSTIC
                                }
                            )
                        }

                        MainDestination.DASHBOARD -> {
                            val dashboardViewModel: DashboardViewModel = viewModel(
                                factory = DashboardViewModel.Factory(
                                    deviceStorageRepository = app.container.deviceStorageRepository,
                                    cacheScanner = app.container.cacheScanner,
                                    shizukuManager = app.container.shizukuManager
                                )
                            )
                            val cleanerViewModel: CleanerViewModel = viewModel(
                                factory = CleanerViewModel.Factory(
                                    coordinator = app.container.cleanupCoordinator,
                                    shizukuManager = app.container.shizukuManager
                                )
                            )
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                cleanerViewModel = cleanerViewModel,
                                shizukuManager = app.container.shizukuManager,
                                packageRepository = app.container.packageRepository,
                                onOpenAppList = {
                                    currentDestination = MainDestination.APP_CACHE_LIST
                                },
                                onOpenDiagnostic = {
                                    currentDestination = MainDestination.DIAGNOSTIC
                                }
                            )
                        }

                        MainDestination.DIAGNOSTIC -> {
                            BackHandler {
                                currentDestination = MainDestination.DASHBOARD
                            }
                            DiagnosticScreen(
                                container = app.container,
                                onOpenAppList = {
                                    currentDestination = MainDestination.APP_CACHE_LIST
                                },
                                onOpenOnboarding = {
                                    currentDestination = MainDestination.ONBOARDING
                                }
                            )
                        }

                        MainDestination.APP_CACHE_LIST -> {
                            BackHandler {
                                currentDestination = MainDestination.DASHBOARD
                            }
                            val appsViewModel: AppsViewModel = viewModel(
                                factory = AppsViewModel.Factory(
                                    cacheScanner = app.container.cacheScanner,
                                    shizukuManager = app.container.shizukuManager
                                )
                            )
                            val cleanerViewModel: CleanerViewModel = viewModel(
                                factory = CleanerViewModel.Factory(
                                    coordinator = app.container.cleanupCoordinator,
                                    shizukuManager = app.container.shizukuManager
                                )
                            )
                            AppCacheListScreen(
                                viewModel = appsViewModel,
                                cleanerViewModel = cleanerViewModel,
                                packageRepository = app.container.packageRepository,
                                onNavigateBack = {
                                    currentDestination = MainDestination.DASHBOARD
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
