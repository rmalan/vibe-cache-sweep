package my.id.rmalan.cache.sweep.model

sealed interface CleanerError {
    data object ShizukuUnavailable : CleanerError
    data object PermissionDenied : CleanerError
    data object SelectiveUnsupported : CleanerError
    data object GlobalTrimUnsupported : CleanerError
    data class PackageInvalid(val packageName: String) : CleanerError
    data class SelfCleanProhibited(val packageName: String) : CleanerError
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
}

data class CleanerBatchResult(
    val totalAttempted: Int,
    val successfulPackages: List<String>,
    val failedPackages: List<String>,
    val errors: Map<String, CleanerError> = emptyMap()
) {
    val isCompleteSuccess: Boolean
        get() = totalAttempted > 0 && failedPackages.isEmpty()

    val isPartialSuccess: Boolean
        get() = successfulPackages.isNotEmpty() && failedPackages.isNotEmpty()

    val isCompleteFailure: Boolean
        get() = totalAttempted > 0 && successfulPackages.isEmpty()

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
