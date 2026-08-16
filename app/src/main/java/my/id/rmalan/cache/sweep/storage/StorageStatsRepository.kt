package my.id.rmalan.cache.sweep.storage

import android.os.UserHandle
import my.id.rmalan.cache.sweep.model.DiscoveredPackage
import my.id.rmalan.cache.sweep.model.PackageStorageStats
import java.util.UUID

/**
 * Repository interface for retrieving storage and cache statistics for installed packages.
 */
interface StorageStatsRepository {

    /**
     * Queries storage breakdown (cache, code, data) for a given package name and volume UUID.
     *
     * @param packageName The package name to inspect.
     * @param storageUuid The storage volume UUID (defaults to default internal storage if null).
     * @param userHandle The user handle (defaults to current process user if null).
     * @return [PackageStorageStats] with measurement outcome.
     */
    fun queryStats(
        packageName: String,
        storageUuid: UUID? = null,
        userHandle: UserHandle? = null
    ): PackageStorageStats

    /**
     * Queries storage breakdown (cache, code, data) for a discovered package.
     *
     * @param pkg The discovered package containing metadata and volume UUID.
     * @param userHandle The user handle (defaults to current process user if null).
     * @return [PackageStorageStats] with measurement outcome.
     */
    fun queryStats(
        pkg: DiscoveredPackage,
        userHandle: UserHandle? = null
    ): PackageStorageStats = queryStats(
        packageName = pkg.packageName,
        storageUuid = pkg.storageUuid,
        userHandle = userHandle
    )
}
