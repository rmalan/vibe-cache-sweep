package my.id.rmalan.cache.sweep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import my.id.rmalan.cache.sweep.ui.screens.AppCacheListScreen
import my.id.rmalan.cache.sweep.ui.screens.DiagnosticScreen
import my.id.rmalan.cache.sweep.ui.theme.CacheSweepTheme
import my.id.rmalan.cache.sweep.ui.viewmodel.AppsViewModel

enum class MainDestination {
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
                    var currentDestination by remember { mutableStateOf(MainDestination.DIAGNOSTIC) }

                    when (currentDestination) {
                        MainDestination.DIAGNOSTIC -> {
                            DiagnosticScreen(
                                container = app.container,
                                onOpenAppList = {
                                    currentDestination = MainDestination.APP_CACHE_LIST
                                }
                            )
                        }
                        MainDestination.APP_CACHE_LIST -> {
                            BackHandler {
                                currentDestination = MainDestination.DIAGNOSTIC
                            }
                            val appsViewModel: AppsViewModel = viewModel(
                                factory = AppsViewModel.Factory(
                                    cacheScanner = app.container.cacheScanner,
                                    shizukuManager = app.container.shizukuManager
                                )
                            )
                            AppCacheListScreen(
                                viewModel = appsViewModel,
                                packageRepository = app.container.packageRepository,
                                onNavigateBack = {
                                    currentDestination = MainDestination.DIAGNOSTIC
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
