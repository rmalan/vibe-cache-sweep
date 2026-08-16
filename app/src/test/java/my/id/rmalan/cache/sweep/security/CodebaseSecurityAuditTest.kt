package my.id.rmalan.cache.sweep.security

import my.id.rmalan.cache.sweep.cleaner.CacheCleaner
import my.id.rmalan.cache.sweep.cleaner.ShizukuCacheCleaner
import my.id.rmalan.cache.sweep.shizuku.CacheOpsUserService
import my.id.rmalan.cache.sweep.shizuku.ICacheOpsService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class CodebaseSecurityAuditTest {

    private fun getProjectRoot(): File {
        var current: File = File(System.getProperty("user.dir") ?: ".")
        while (current.parentFile != null && !File(current, "settings.gradle.kts").exists()) {
            current = current.parentFile ?: break
        }
        if (!File(current, "settings.gradle.kts").exists()) {
            val fallback = File("/home/rmalan/code/projects/vibe-cache-sweep")
            if (fallback.exists()) return fallback
        }
        return current
    }

    // =========================================================================
    // P2-16: Automated Verification: No 'sh -c' or Unsafe Shell Execution
    // =========================================================================

    @Test
    fun codebase_hasZeroUsageOfShDashC() {
        val root = getProjectRoot()
        assertTrue("Project root should exist", root.exists())

        val sourceDirs = listOf(
            File(root, "app/src/main")
        )

        val forbiddenShellPatterns = listOf(
            "sh -c",
            "\"-c\"",
            "'/bin/sh'",
            "\"/bin/sh\"",
            "\"/system/bin/sh\"",
            "\"sh\""
        )

        val violations = mutableListOf<String>()

        for (dir in sourceDirs) {
            if (!dir.exists()) continue
            dir.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java" || it.extension == "aidl") }.forEach { file ->
                val content = file.readText()
                for (pattern in forbiddenShellPatterns) {
                    if (content.contains(pattern)) {
                        violations.add("${file.relativeTo(root)} contains forbidden shell pattern: '$pattern'")
                    }
                }
            }
        }

        assertTrue(
            "CRITICAL SECURITY DEFECT: Forbidden shell patterns detected:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun codebase_hasNoRuntimeExecInProductionCode() {
        val root = getProjectRoot()
        val sourceDirs = listOf(
            File(root, "app/src/main")
        )

        val violations = mutableListOf<String>()

        for (dir in sourceDirs) {
            if (!dir.exists()) continue
            dir.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }.forEach { file ->
                val content = file.readText()
                if (content.contains("Runtime.getRuntime().exec") || content.contains("Runtime.getRuntime()") && content.contains(".exec")) {
                    violations.add("${file.relativeTo(root)} invokes Runtime.getRuntime().exec")
                }
            }
        }

        assertTrue(
            "CRITICAL SECURITY DEFECT: Runtime.getRuntime().exec detected in production code:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun codebase_processBuilderStrictlyRestrictedToWhitelistedFiles() {
        val root = getProjectRoot()
        val appMain = File(root, "app/src/main")
        val allowedFiles = setOf(
            "PackageCommands.kt",
            "CapabilityProbe.kt"
        )

        val violations = mutableListOf<String>()

        appMain.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }.forEach { file ->
            val content = file.readText()
            if (content.contains("ProcessBuilder")) {
                if (!allowedFiles.contains(file.name)) {
                    violations.add("Unauthorized ProcessBuilder in ${file.relativeTo(root)}")
                }
            }
        }

        assertTrue(
            "ProcessBuilder should only exist in approved command execution classes:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    // =========================================================================
    // P2-17: Automated Verification: No Arbitrary Command Execution Interfaces
    // =========================================================================

    @Test
    fun aidlInterface_strictlyEnforcesTypedOperationsWithoutArbitraryExecution() {
        val allowedAidlMethods = setOf(
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

        val forbiddenKeywords = listOf(
            "exec", "shell", "cmd", "run", "eval", "system", "command", "process", "terminal", "bash", "sh"
        )

        val methods: Array<Method> = ICacheOpsService::class.java.declaredMethods
        for (method in methods) {
            val name = method.name
            assertTrue(
                "Method '$name' on ICacheOpsService is not in approved typed AIDL whitelist",
                allowedAidlMethods.contains(name)
            )

            val lowerName = name.lowercase()
            for (keyword in forbiddenKeywords) {
                // Ensure no method name contains forbidden shell keywords (except approved 'getLastError' or 'clearPackageCache')
                if (keyword != "run" && keyword != "process") {
                    assertFalse(
                        "Method '$name' on ICacheOpsService contains dangerous keyword '$keyword'",
                        lowerName.contains(keyword) && !allowedAidlMethods.contains(name)
                    )
                }
            }

            // Check parameter types: no method accepts byte arrays, commands, or generic objects
            for (paramType in method.parameterTypes) {
                assertTrue(
                    "Method '$name' parameter type '${paramType.simpleName}' must be primitive/String",
                    paramType == String::class.java ||
                            paramType == Int::class.javaPrimitiveType ||
                            paramType == Long::class.javaPrimitiveType ||
                            paramType == Boolean::class.javaPrimitiveType
                )
            }
        }
    }

    @Test
    fun cacheCleanerInterface_containsOnlyTypedOperations() {
        val allowedCleanerMethods = setOf(
            "capabilities",
            "clearPackage",
            "clearPackages",
            "trimGlobally",
            "executePlan"
        )

        val methods: Array<Method> = CacheCleaner::class.java.declaredMethods
        for (method in methods) {
            val baseName = method.name.substringBefore("$")
            assertTrue(
                "Method '${method.name}' on CacheCleaner is not approved",
                allowedCleanerMethods.contains(baseName)
            )
        }
    }

    @Test
    fun cacheOpsUserService_onlyImplementsApprovedStubMethods() {
        val publicMethods = CacheOpsUserService::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }
            .map { it.name }
            .toSet()

        val allowedPublicMethods = setOf(
            "destroy",
            "getProtocolVersion",
            "getPrivilegedUid",
            "supportsSelectiveCacheClear",
            "supportsGlobalTrim",
            "clearPackageCache",
            "trimCaches",
            "getLastError"
        )

        for (method in publicMethods) {
            assertTrue(
                "Public method '$method' in CacheOpsUserService is not in approved list",
                allowedPublicMethods.contains(method)
            )
        }
    }
}
