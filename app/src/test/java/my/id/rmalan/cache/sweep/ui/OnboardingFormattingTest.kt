package my.id.rmalan.cache.sweep.ui

import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.ShizukuState
import my.id.rmalan.cache.sweep.model.ThemeMode
import my.id.rmalan.cache.sweep.model.UserSettings
import my.id.rmalan.cache.sweep.ui.viewmodel.OnboardingStep
import my.id.rmalan.cache.sweep.ui.viewmodel.OnboardingUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFormattingTest {

    @Test
    fun onboardingSteps_haveCorrectNumbersAndTotal() {
        assertEquals(1, OnboardingStep.WELCOME.stepNumber)
        assertEquals(2, OnboardingStep.USAGE_ACCESS.stepNumber)
        assertEquals(3, OnboardingStep.SHIZUKU.stepNumber)
        assertEquals(4, OnboardingStep.FIRST_SCAN.stepNumber)

        assertEquals(4, OnboardingStep.entries.size)
        assertTrue(OnboardingStep.WELCOME.isFirst)
        assertFalse(OnboardingStep.WELCOME.isLast)
        assertTrue(OnboardingStep.FIRST_SCAN.isLast)
        assertFalse(OnboardingStep.FIRST_SCAN.isFirst)
    }

    @Test
    fun onboardingUiState_calculatesProgressFractionAccurately() {
        val welcomeState = OnboardingUiState(currentStep = OnboardingStep.WELCOME)
        assertEquals(0.25f, welcomeState.progressFraction, 0.001f)

        val usageState = OnboardingUiState(currentStep = OnboardingStep.USAGE_ACCESS)
        assertEquals(0.50f, usageState.progressFraction, 0.001f)

        val shizukuState = OnboardingUiState(currentStep = OnboardingStep.SHIZUKU)
        assertEquals(0.75f, shizukuState.progressFraction, 0.001f)

        val scanState = OnboardingUiState(currentStep = OnboardingStep.FIRST_SCAN)
        assertEquals(1.00f, scanState.progressFraction, 0.001f)
    }

    @Test
    fun userSettings_defaultValuesAndCopyWorkCorrectly() {
        val defaults = UserSettings()
        assertFalse(defaults.onboardingCompleted)
        assertFalse(defaults.showSystemApps)
        assertTrue(defaults.showZeroCacheApps)
        assertEquals(AppSort.CACHE_DESC, defaults.sortMode)
        assertEquals(ThemeMode.SYSTEM, defaults.themeMode)

        val modified = defaults.copy(
            onboardingCompleted = true,
            showSystemApps = true,
            themeMode = ThemeMode.DARK
        )
        assertTrue(modified.onboardingCompleted)
        assertTrue(modified.showSystemApps)
        assertTrue(modified.showZeroCacheApps)
        assertEquals(ThemeMode.DARK, modified.themeMode)
    }

    @Test
    fun shizukuState_correctlyClassifiedInOnboardingState() {
        val readyState = OnboardingUiState(shizukuState = ShizukuState.Ready(2000))
        assertEquals(ShizukuState.Ready(2000), readyState.shizukuState)

        val permState = OnboardingUiState(shizukuState = ShizukuState.PermissionRequired)
        assertEquals(ShizukuState.PermissionRequired, permState.shizukuState)

        val notRunningState = OnboardingUiState(shizukuState = ShizukuState.NotRunning)
        assertEquals(ShizukuState.NotRunning, notRunningState.shizukuState)
    }
}
