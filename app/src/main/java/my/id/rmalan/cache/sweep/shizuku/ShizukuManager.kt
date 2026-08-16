package my.id.rmalan.cache.sweep.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import my.id.rmalan.cache.sweep.BuildConfig
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.model.ShizukuState
import rikka.shizuku.Shizuku

data class PrivilegedBackendInfo(
    val connected: Boolean,
    val protocolVersion: Int? = null,
    val privilegedUid: Int? = null,
    val selectiveClearSupported: Boolean = false,
    val globalTrimSupported: Boolean = false,
    val lastError: String? = null
)

class ShizukuManager(
    private val context: Context
) {
    companion object {
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }

    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private val _userServiceConnected = MutableStateFlow(false)
    val userServiceConnected: StateFlow<Boolean> = _userServiceConnected.asStateFlow()

    @Volatile
    private var cacheOpsService: ICacheOpsService? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        updateState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        cacheOpsService = null
        _userServiceConnected.value = false
        _state.value = ShizukuState.NotRunning
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    updateState()
                } else {
                    _state.value = ShizukuState.Error("Shizuku permission denied")
                }
            }
        }

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service != null) {
                cacheOpsService = ICacheOpsService.Stub.asInterface(service)
                _userServiceConnected.value = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cacheOpsService = null
            _userServiceConnected.value = false
        }
    }

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(context.packageName, CacheOpsUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("privileged")
            .debuggable(BuildConfig.DEBUG)
            .version(1)
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
            updateState()
        } catch (e: Exception) {
            _state.value = ShizukuState.Error(e.message ?: "Failed to initialize Shizuku listeners")
        }
    }

    fun updateState() {
        if (!Shizuku.pingBinder()) {
            _state.value = ShizukuState.NotRunning
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            val uid = try {
                Shizuku.getUid()
            } catch (e: Exception) {
                -1
            }
            _state.value = ShizukuState.Ready(uid)
            ensureServiceBound()
        } else {
            _state.value = ShizukuState.PermissionRequired
        }
    }

    fun requestPermission() {
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun ensureServiceBound() {
        if (cacheOpsService == null && Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            try {
                Shizuku.bindUserService(userServiceArgs, userServiceConnection)
            } catch (e: Exception) {
                // Handled gracefully
            }
        }
    }

    fun getService(): ICacheOpsService? {
        if (cacheOpsService == null) {
            ensureServiceBound()
        }
        return cacheOpsService
    }

    suspend fun getOrAwaitService(timeoutMs: Long = 3000): ICacheOpsService? {
        if (cacheOpsService != null) return cacheOpsService
        ensureServiceBound()
        if (cacheOpsService != null) return cacheOpsService

        val start = System.currentTimeMillis()
        while (cacheOpsService == null && (System.currentTimeMillis() - start) < timeoutMs) {
            delay(50)
        }
        return cacheOpsService
    }

    suspend fun fetchCapabilities(timeoutMs: Long = 3000): CleanerCapabilities = withContext(Dispatchers.IO) {
        val isPing = try { Shizuku.pingBinder() } catch (e: Exception) { false }
        val isGranted = isPing && try { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED } catch (e: Exception) { false }
        val uid = if (isGranted) {
            try { Shizuku.getUid() } catch (e: Exception) { null }
        } else null

        val service = if (isGranted) getOrAwaitService(timeoutMs) else null
        val supportsSelective = try { service?.supportsSelectiveCacheClear() ?: false } catch (e: Exception) { false }
        val supportsTrim = try { service?.supportsGlobalTrim() ?: false } catch (e: Exception) { false }

        CleanerCapabilities(
            shizukuAvailable = isPing,
            shizukuAuthorized = isGranted,
            privilegedUid = uid,
            supportsSelectiveCacheClear = supportsSelective,
            supportsGlobalTrim = supportsTrim
        )
    }

    suspend fun pingPrivilegedBackend(timeoutMs: Long = 3000): PrivilegedBackendInfo = withContext(Dispatchers.IO) {
        val service = getOrAwaitService(timeoutMs)
        if (service == null) {
            PrivilegedBackendInfo(connected = false, lastError = "UserService not bound or not responding")
        } else {
            try {
                PrivilegedBackendInfo(
                    connected = true,
                    protocolVersion = service.protocolVersion,
                    privilegedUid = service.privilegedUid,
                    selectiveClearSupported = service.supportsSelectiveCacheClear(),
                    globalTrimSupported = service.supportsGlobalTrim(),
                    lastError = service.lastError.ifBlank { null }
                )
            } catch (e: Exception) {
                PrivilegedBackendInfo(
                    connected = false,
                    lastError = e.message ?: "IPC call failed"
                )
            }
        }
    }

    fun getCapabilities(): CleanerCapabilities {
        val isPing = try { Shizuku.pingBinder() } catch (e: Exception) { false }
        val isGranted = isPing && try { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED } catch (e: Exception) { false }
        val uid = if (isGranted) {
            try { Shizuku.getUid() } catch (e: Exception) { null }
        } else null

        val service = getService()
        val supportsSelective = try { service?.supportsSelectiveCacheClear() ?: false } catch (e: Exception) { false }
        val supportsTrim = try { service?.supportsGlobalTrim() ?: false } catch (e: Exception) { false }

        return CleanerCapabilities(
            shizukuAvailable = isPing,
            shizukuAuthorized = isGranted,
            privilegedUid = uid,
            supportsSelectiveCacheClear = supportsSelective,
            supportsGlobalTrim = supportsTrim
        )
    }

    fun cleanup() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
            if (cacheOpsService != null) {
                Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
            }
        } catch (e: Exception) {
            // Ignore on cleanup
        }
    }
}

