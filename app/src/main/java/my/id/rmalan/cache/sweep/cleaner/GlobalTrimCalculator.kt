package my.id.rmalan.cache.sweep.cleaner

import my.id.rmalan.cache.sweep.model.DeviceStorageInfo

object GlobalTrimCalculator {

    /**
     * Calculates the desired free storage in bytes for `pm trim-caches <DESIRED_FREE_SPACE>`.
     *
     * As defined in TECH_SPEC Section 34:
     * desiredFree = minOf(device.totalBytes, device.availableBytes + estimatedCacheBytes)
     *
     * Boundary conditions:
     * - Clamped to [0, totalBytes]
     * - Protects against Long arithmetic overflow
     * - Coerces negative inputs safely to non-negative values
     */
    fun calculateDesiredFreeBytes(
        deviceStorage: DeviceStorageInfo,
        estimatedCacheBytes: Long
    ): Long {
        return calculateDesiredFreeBytes(
            availableBytes = deviceStorage.availableBytes,
            totalBytes = deviceStorage.totalBytes,
            estimatedCacheBytes = estimatedCacheBytes
        )
    }

    fun calculateDesiredFreeBytes(
        availableBytes: Long,
        totalBytes: Long,
        estimatedCacheBytes: Long
    ): Long {
        val safeTotal = maxOf(0L, totalBytes)
        val safeAvailable = maxOf(0L, availableBytes)
        val safeEstimated = maxOf(0L, estimatedCacheBytes)

        if (safeTotal == 0L) {
            return try {
                Math.addExact(safeAvailable, safeEstimated)
            } catch (e: ArithmeticException) {
                Long.MAX_VALUE
            }
        }

        val sum = try {
            Math.addExact(safeAvailable, safeEstimated)
        } catch (e: ArithmeticException) {
            safeTotal
        }

        return maxOf(0L, minOf(safeTotal, sum))
    }

    /**
     * Calculates maximum desired free storage (i.e. requesting Android to trim all reclaimable caches).
     */
    fun calculateMaxFreeBytes(deviceStorage: DeviceStorageInfo): Long {
        return maxOf(0L, deviceStorage.totalBytes)
    }
}
