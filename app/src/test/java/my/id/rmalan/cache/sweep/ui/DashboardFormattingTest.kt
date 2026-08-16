package my.id.rmalan.cache.sweep.ui

import my.id.rmalan.cache.sweep.model.DeviceStorageInfo
import my.id.rmalan.cache.sweep.ui.viewmodel.DashboardUiState
import my.id.rmalan.cache.sweep.util.ByteFormatter
import my.id.rmalan.cache.sweep.util.DashboardTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardFormattingTest {

    @Test
    fun testFormatLastScanned() {
        val now = 1_700_000_000_000L

        assertEquals("Not scanned yet", DashboardTimeFormatter.formatLastScanned(0L, now))
        assertEquals("Not scanned yet", DashboardTimeFormatter.formatLastScanned(-100L, now))

        // 10 seconds ago
        assertEquals("Just now", DashboardTimeFormatter.formatLastScanned(now - 10_000L, now))

        // 1 minute ago
        assertEquals("1 minute ago", DashboardTimeFormatter.formatLastScanned(now - 65_000L, now))

        // 5 minutes ago
        assertEquals("5 minutes ago", DashboardTimeFormatter.formatLastScanned(now - 300_000L, now))

        // 1 hour ago
        assertEquals("1 hour ago", DashboardTimeFormatter.formatLastScanned(now - 3600_000L, now))

        // 4 hours ago
        assertEquals("4 hours ago", DashboardTimeFormatter.formatLastScanned(now - 14_400_000L, now))

        // 1 day ago
        assertEquals("Yesterday", DashboardTimeFormatter.formatLastScanned(now - 86_400_000L, now))

        // 3 days ago
        assertEquals("3 days ago", DashboardTimeFormatter.formatLastScanned(now - 3 * 86_400_000L, now))
    }

    @Test
    fun testFormatDuration() {
        assertEquals("< 1s", DashboardTimeFormatter.formatDuration(0L))
        assertEquals("< 1s", DashboardTimeFormatter.formatDuration(-50L))
        assertEquals("120ms", DashboardTimeFormatter.formatDuration(120L))
        assertEquals("840ms", DashboardTimeFormatter.formatDuration(840L))
        assertEquals("1.5s", DashboardTimeFormatter.formatDuration(1500L))
        assertEquals("4.2s", DashboardTimeFormatter.formatDuration(4200L))
    }

    @Test
    fun testStoragePercentageCalculation() {
        val stateZero = DashboardUiState(deviceStorage = DeviceStorageInfo(0L, 0L))
        assertEquals(0f, stateZero.usedStoragePercentage, 0.001f)

        val stateHalf = DashboardUiState(deviceStorage = DeviceStorageInfo(100_000L, 50_000L))
        assertEquals(0.5f, stateHalf.usedStoragePercentage, 0.001f)

        val stateFull = DashboardUiState(deviceStorage = DeviceStorageInfo(100_000L, 0L))
        assertEquals(1.0f, stateFull.usedStoragePercentage, 0.001f)

        val stateNull = DashboardUiState(deviceStorage = null)
        assertEquals(0f, stateNull.usedStoragePercentage, 0.001f)
    }

    @Test
    fun testByteFormattingForDashboard() {
        assertEquals("0 B", ByteFormatter.format(0L))
        assertEquals("512 B", ByteFormatter.format(512L))
        assertEquals("1.4 KB", ByteFormatter.format(1434L))
        assertEquals("312.0 MB", ByteFormatter.format(312L * 1024L * 1024L))
        assertEquals("1.42 GB", ByteFormatter.format(1524713370L))
        assertEquals("128.00 GB", ByteFormatter.format(128L * 1024L * 1024L * 1024L))
    }
}
