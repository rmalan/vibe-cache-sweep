package my.id.rmalan.cache.sweep.ui

import my.id.rmalan.cache.sweep.model.AppSort
import my.id.rmalan.cache.sweep.model.CleanupHistoryEntry
import my.id.rmalan.cache.sweep.model.CleanupMode
import my.id.rmalan.cache.sweep.model.ThemeMode
import my.id.rmalan.cache.sweep.util.SettingsFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsFormattingTest {

    @Test
    fun sortModeLabels_areDescriptiveAndAccurate() {
        assertEquals("Largest cache first", SettingsFormatter.sortModeLabel(AppSort.CACHE_DESC))
        assertEquals("Largest total footprint", SettingsFormatter.sortModeLabel(AppSort.TOTAL_DESC))
        assertEquals("Alphabetical (A to Z)", SettingsFormatter.sortModeLabel(AppSort.NAME_ASC))

        assertTrue(SettingsFormatter.sortModeDescription(AppSort.CACHE_DESC).contains("cache"))
        assertTrue(SettingsFormatter.sortModeDescription(AppSort.TOTAL_DESC).contains("total"))
        assertTrue(SettingsFormatter.sortModeDescription(AppSort.NAME_ASC).contains("alphabetical"))
    }

    @Test
    fun themeModeLabels_areDescriptiveAndAccurate() {
        assertEquals("System default", SettingsFormatter.themeModeLabel(ThemeMode.SYSTEM))
        assertEquals("Light theme", SettingsFormatter.themeModeLabel(ThemeMode.LIGHT))
        assertEquals("Dark theme", SettingsFormatter.themeModeLabel(ThemeMode.DARK))

        assertTrue(SettingsFormatter.themeModeDescription(ThemeMode.SYSTEM).contains("system"))
        assertTrue(SettingsFormatter.themeModeDescription(ThemeMode.LIGHT).contains("light"))
        assertTrue(SettingsFormatter.themeModeDescription(ThemeMode.DARK).contains("dark"))
    }

    @Test
    fun historyCountLabels_formatPluralizationCorrectly() {
        assertEquals("No cleanup records", SettingsFormatter.historyCountLabel(0))
        assertEquals("1 cleanup record saved locally", SettingsFormatter.historyCountLabel(1))
        assertEquals("5 cleanup records saved locally", SettingsFormatter.historyCountLabel(5))
        assertEquals("25 cleanup records saved locally", SettingsFormatter.historyCountLabel(25))
    }

    @Test
    fun cleanupHistoryEntry_successAndFailureFlags() {
        val completeSuccess = CleanupHistoryEntry(
            mode = CleanupMode.SELECTIVE,
            packagesAttempted = 10,
            packagesSucceeded = 10
        )
        assertTrue(completeSuccess.isCompleteSuccess)
        assertFalse(completeSuccess.hasFailures)

        val partialFailure = CleanupHistoryEntry(
            mode = CleanupMode.SELECTIVE,
            packagesAttempted = 10,
            packagesSucceeded = 8
        )
        assertFalse(partialFailure.isCompleteSuccess)
        assertTrue(partialFailure.hasFailures)
    }
}
