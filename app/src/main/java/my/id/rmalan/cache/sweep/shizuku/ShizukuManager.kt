package my.id.rmalan.cache.sweep.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import my.id.rmalan.cache.sweep.BuildConfig
import my.id.rmalan.cache.sweep.model.CleanerCapabilities
import my.id.rmalan.cache.sweep.model.ShizukuState
import rikka.shizuku.Shizuku

class ShizukuManager(
    private val context: Context
) {
    companion object {
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }

    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.NotRunning)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    @Volatile
    private var cacheOpsService: ICacheOpsService? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        updateState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        cacheOpsService = null
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
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cacheOpsService = null
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
