package my.id.rmalan.cache.sweep.model

sealed interface ShizukuState {
    data object NotRunning : ShizukuState
    data object PermissionRequired : ShizukuState
    data object Connecting : ShizukuState
    data class Ready(val uid: Int) : ShizukuState
    data class Error(val reason: String) : ShizukuState
}
