package my.id.rmalan.cache.sweep.cleaner

import my.id.rmalan.cache.sweep.model.DeviceStorageInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class GlobalTrimCalculatorTest {

    @Test
    fun `calculateDesiredFreeBytes calculates sum of available and estimated cache`() {
        val storage = DeviceStorageInfo(
            totalBytes = 128L * 1024L * 1024L * 1024L,     // 128 GB
            availableBytes = 20L * 1024L * 1024L * 1024L   // 20 GB
        )
        val estimatedCache = 5L * 1024L * 1024L * 1024L    // 5 GB

        val target = GlobalTrimCalculator.calculateDesiredFreeBytes(storage, estimatedCache)
        assertEquals(25L * 1024L * 1024L * 1024L, target) // 25 GB
    }

    @Test
    fun `calculateDesiredFreeBytes clamps to total storage when sum exceeds totalBytes`() {
        val storage = DeviceStorageInfo(
            totalBytes = 64L * 1024L * 1024L * 1024L,     // 64 GB
            availableBytes = 50L * 1024L * 1024L * 1024L  // 50 GB
        )
        val estimatedCache = 20L * 1024L * 1024L * 1024L  // 20 GB (sum = 70 GB > 64 GB)

        val target = GlobalTrimCalculator.calculateDesiredFreeBytes(storage, estimatedCache)
        assertEquals(64L * 1024L * 1024L * 1024L, target) // Clamped to 64 GB total
    }

    @Test
    fun `calculateDesiredFreeBytes handles zero and negative inputs safely`() {
        val storage = DeviceStorageInfo(
            totalBytes = 1000L,
            availableBytes = 200L
        )

        // Zero cache
        assertEquals(200L, GlobalTrimCalculator.calculateDesiredFreeBytes(storage, 0L))

        // Negative cache clamped to 0
        assertEquals(200L, GlobalTrimCalculator.calculateDesiredFreeBytes(storage, -500L))

        // Negative available and negative total safely handled
        val rawTarget = GlobalTrimCalculator.calculateDesiredFreeBytes(
            availableBytes = -100L,
            totalBytes = -500L,
            estimatedCacheBytes = -200L
        )
        assertEquals(0L, rawTarget)
    }

    @Test
    fun `calculateDesiredFreeBytes handles arithmetic overflow without crash`() {
        val total = Long.MAX_VALUE
        val available = Long.MAX_VALUE - 1000L
        val estimatedCache = 2000L // available + estimated > Long.MAX_VALUE

        val target = GlobalTrimCalculator.calculateDesiredFreeBytes(
            availableBytes = available,
            totalBytes = total,
            estimatedCacheBytes = estimatedCache
        )
        assertEquals(Long.MAX_VALUE, target)
    }

    @Test
    fun `calculateMaxFreeBytes returns totalBytes clamped to non-negative`() {
        val storage = DeviceStorageInfo(
            totalBytes = 256L * 1024L * 1024L * 1024L,
            availableBytes = 10L * 1024L * 1024L * 1024L
        )

        assertEquals(256L * 1024L * 1024L * 1024L, GlobalTrimCalculator.calculateMaxFreeBytes(storage))

        val zeroStorage = DeviceStorageInfo(totalBytes = 0L, availableBytes = 0L)
        assertEquals(0L, GlobalTrimCalculator.calculateMaxFreeBytes(zeroStorage))
    }
}
