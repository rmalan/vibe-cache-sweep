package my.id.rmalan.cache.sweep.model

sealed interface CleaningState {

    data object Idle : CleaningState

    data object Validating : CleaningState

    data object SnapshotBefore : CleaningState

    data class Clearing(
        val current: Int,
        val total: Int,
        val currentPackage: String?,
        val currentAppName: String? = null
    ) : CleaningState {
        val progressFraction: Float
            get() = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    }

    data object WaitingForStats : CleaningState

    data object SnapshotAfter : CleaningState

    data class Completed(
        val result: CleanupResult
    ) : CleaningState

    data class Failed(
        val error: CleanerError? = null,
        val message: String
    ) : CleaningState
}
