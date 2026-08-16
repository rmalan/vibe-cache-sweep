package my.id.rmalan.cache.sweep.shizuku

import my.id.rmalan.cache.sweep.cleaner.PackageCommands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method

class CacheOpsUserServiceSecurityTest {

    private val service = CacheOpsUserService()

    private val allowedMethodNames = setOf(
        "destroy",
        "getProtocolVersion",
        "getPrivilegedUid",
        "supportsSelectiveCacheClear",
        "supportsGlobalTrim",
        "clearPackageCache",
        "trimCaches",
        "getLastError",
        "asBinder"
    )

    private val forbiddenSubstrings = listOf(
        "exec",
        "shell",
        "cmd",
        "runCommand",
        "runProcess",
        "eval",
        "system"
    )

    @Test
    fun aidlInterface_hasOnlyApprovedTypedMethods() {
        val declaredMethods: Array<Method> = ICacheOpsService::class.java.declaredMethods
        for (method in declaredMethods) {
            val name = method.name
            assertTrue(
                "Method '$name' on ICacheOpsService is not in the approved typed whitelist",
                allowedMethodNames.contains(name)
            )

            val lowerName = name.lowercase()
            for (forbidden in forbiddenSubstrings) {
                assertFalse(
                    "Method '$name' on ICacheOpsService contains forbidden shell keyword '$forbidden'",
                    lowerName.contains(forbidden)
                )
            }
        }
    }

    @Test
    fun getProtocolVersion_returnsExpectedVersion() {
        assertEquals(1, service.protocolVersion)
    }

    @Test
    fun clearPackageCache_rejectsInvalidPackageNames() {
        val result = service.clearPackageCache("invalid..pkg", 0)
        assertEquals(-1, result)
        assertEquals("Invalid package name", service.lastError)
    }

    @Test
    fun clearPackageCache_rejectsSelfPackage() {
        val result = service.clearPackageCache("my.id.rmalan.cache.sweep", 0)
        assertEquals(-1, result)
        assertEquals("Invalid package name", service.lastError)
    }

    @Test
    fun clearPackageCache_rejectsNegativeUserId() {
        val result = service.clearPackageCache("com.example.app", -1)
        assertEquals(-1, result)
        assertEquals("Invalid user ID", service.lastError)
    }

    @Test
    fun trimCaches_rejectsNegativeBytes() {
        val result = service.trimCaches(-100L)
        assertEquals(-1, result)
        assertEquals("Invalid desired free bytes", service.lastError)
    }
}
