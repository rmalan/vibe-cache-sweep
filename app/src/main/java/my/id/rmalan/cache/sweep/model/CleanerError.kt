package my.id.rmalan.cache.sweep.model

sealed interface CleanerError {
    data object ShizukuUnavailable : CleanerError
    data object PermissionDenied : CleanerError
    data object SelectiveUnsupported : CleanerError
    data object GlobalTrimUnsupported : CleanerError
    data class PackageInvalid(val packageName: String) : CleanerError
    data class SelfCleanProhibited(val packageName: String) : CleanerError
    data class PackageNotScanned(val packageName: String) : CleanerError
    data class CommandFailed(val exitCode: Int, val rawError: String) : CleanerError
    data class Unexpected(val cause: Throwable) : CleanerError

    val description: String
        get() = when (this) {
            is ShizukuUnavailable -> "Shizuku service is not running or unreachable"
            is PermissionDenied -> "Shizuku permission was not granted"
            is SelectiveUnsupported -> "Selective cache clearing is not supported or restricted on this device"
            is GlobalTrimUnsupported -> "Global cache trimming is not supported on this device"
            is PackageInvalid -> "Invalid package name: $packageName"
            is SelfCleanProhibited -> "Self-cleaning CacheSweep is prohibited: $packageName"
            is PackageNotScanned -> "Package was not found in scanned packages: $packageName"
            is CommandFailed -> "Command failed with exit code $exitCode: ${rawError.ifBlank { "unknown error" }}"
            is Unexpected -> cause.message ?: "An unexpected error occurred"
        }
}

data class CleaningProgress(
    val current: Int,
    val total: Int,
    val currentPackageName: String?,
    val currentAppName: String? = null
) {
    val progressFraction: Float
        get() = if (total > 0) current.toFloat() / total.toFloat() else 0f

    val displayText: String
        get() {
            val label = currentAppName ?: currentPackageName ?: "Cache"
            return if (total > 0) "Cleaning $current of $total ($label)" else "Cleaning $label"
        }
}

data class CleanerBatchResult(
    val totalAttempted: Int,
    val successfulPackages: List<String>,
    val failedPackages: List<String>,
    val errors: Map<String, CleanerError> = emptyMap()
) {
    val successCount: Int
        get() = successfulPackages.size

    val failureCount: Int
        get() = failedPackages.size

    val isCompleteSuccess: Boolean
        get() = totalAttempted > 0 && failedPackages.isEmpty()

    val isPartialSuccess: Boolean
        get() = successfulPackages.isNotEmpty() && failedPackages.isNotEmpty()

    val isCompleteFailure: Boolean
        get() = totalAttempted > 0 && successfulPackages.isEmpty()

    fun getError(packageName: String): CleanerError? = errors[packageName]

    fun errorMessageSummary(): String {
        if (errors.isEmpty()) return ""
        return errors.entries.joinToString("; ") { (pkg, err) ->
            "$pkg: ${err.description}"
        }
    }

    companion object {
        val EMPTY = CleanerBatchResult(
            totalAttempted = 0,
            successfulPackages = emptyList(),
            failedPackages = emptyList(),
            errors = emptyMap()
        )

        fun single(packageName: String, success: Boolean, error: CleanerError? = null): CleanerBatchResult {
            return if (success) {
                CleanerBatchResult(
                    totalAttempted = 1,
                    successfulPackages = listOf(packageName),
                    failedPackages = emptyList()
                )
            } else {
                CleanerBatchResult(
                    totalAttempted = 1,
                    successfulPackages = emptyList(),
                    failedPackages = listOf(packageName),
                    errors = if (error != null) mapOf(packageName to error) else emptyMap()
                )
            }
        }
    }
}
