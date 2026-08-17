package my.id.rmalan.cache.sweep.security

import my.id.rmalan.cache.sweep.cleaner.CacheCleaner
import my.id.rmalan.cache.sweep.cleaner.PackageCommands
import my.id.rmalan.cache.sweep.shizuku.CacheOpsUserService
import my.id.rmalan.cache.sweep.shizuku.ICacheOpsService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Automated Security, Privacy & Component Audit Test Suite.
 *
 * Verifies Roadmap tasks P5-15 through P5-20:
 * - P5-15: Confirm no INTERNET permission in manifest, merged manifests, dependencies, or codebase
 * - P5-16: Confirm no analytics tracking libraries or analytics code
 * - P5-17: Confirm no telemetry, remote crash reporters, or background uploading
 * - P5-18: Confirm no arbitrary shell execution interfaces, command injection, or untyped APIs
 * - P5-19: Confirm zero sensitive information leakage and clean production logging (zero Log.* / stdout)
 * - P5-20: Audit exported Android components (MainActivity as sole exported entry, allowBackup=false, protected ShizukuProvider)
 */
class SecurityPrivacyComponentAuditTest {

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

    private fun parseManifest(manifestFile: File): Document {
        assertTrue("Manifest file must exist: ${manifestFile.absolutePath}", manifestFile.exists())
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        return builder.parse(manifestFile)
    }

    // =========================================================================
    // P5-15: Confirm No INTERNET Permission
    // =========================================================================

    @Test
    fun p5_15_manifest_hasZeroInternetPermissionDeclarations() {
        val root = getProjectRoot()
        val manifestFile = File(root, "app/src/main/AndroidManifest.xml")
        val doc = parseManifest(manifestFile)

        val permissionNodes = doc.getElementsByTagName("uses-permission")
        for (i in 0 until permissionNodes.length) {
            val element = permissionNodes.item(i) as Element
            val permName = element.getAttribute("android:name")
            assertFalse(
                "CRITICAL SECURITY DEFECT: INTERNET permission found in AndroidManifest.xml",
                permName.equals("android.permission.INTERNET", ignoreCase = true)
            )
            assertFalse(
                "CRITICAL SECURITY DEFECT: ACCESS_NETWORK_STATE permission found in AndroidManifest.xml",
                permName.equals("android.permission.ACCESS_NETWORK_STATE", ignoreCase = true)
            )
            assertFalse(
                "CRITICAL SECURITY DEFECT: ACCESS_WIFI_STATE permission found in AndroidManifest.xml",
                permName.equals("android.permission.ACCESS_WIFI_STATE", ignoreCase = true)
            )
        }
    }

    @Test
    fun p5_15_mergedManifests_haveZeroInternetPermissionDeclarations() {
        val root = getProjectRoot()
        val buildDir = File(root, "app/build/intermediates")
        if (!buildDir.exists()) return

        val mergedManifestFiles = buildDir.walkTopDown().filter {
            it.isFile && it.name == "AndroidManifest.xml"
        }.toList()

        for (file in mergedManifestFiles) {
            val doc = parseManifest(file)
            val permissionNodes = doc.getElementsByTagName("uses-permission")
            for (i in 0 until permissionNodes.length) {
                val element = permissionNodes.item(i) as Element
                val permName = element.getAttribute("android:name")
                assertFalse(
                    "CRITICAL SECURITY DEFECT: INTERNET permission found in merged manifest: ${file.path}",
                    permName.equals("android.permission.INTERNET", ignoreCase = true)
                )
            }
        }
    }

    @Test
    fun p5_15_buildConfiguration_hasZeroNetworkOrHttpLibraries() {
        val root = getProjectRoot()
        val tomlFile = File(root, "gradle/libs.versions.toml")
        val buildGradleFile = File(root, "app/build.gradle.kts")

        val forbiddenNetworkKeywords = listOf(
            "okhttp",
            "retrofit",
            "ktor",
            "cronet",
            "volley",
            "apache.http",
            "fuel",
            "grpc"
        )

        val tomlContent = tomlFile.readText().lowercase()
        val buildGradleContent = buildGradleFile.readText().lowercase()

        for (kw in forbiddenNetworkKeywords) {
            assertFalse(
                "libs.versions.toml contains forbidden networking dependency keyword: '$kw'",
                tomlContent.contains(kw)
            )
            assertFalse(
                "app/build.gradle.kts contains forbidden networking dependency keyword: '$kw'",
                buildGradleContent.contains(kw)
            )
        }
    }

    @Test
    fun p5_15_codebase_hasZeroNetworkClientsOrSockets() {
        val root = getProjectRoot()
        val appMain = File(root, "app/src/main")

        val forbiddenNetworkImports = listOf(
            "java.net.Socket",
            "java.net.ServerSocket",
            "java.net.HttpURLConnection",
            "java.net.URLClassLoader",
            "javax.net.ssl",
            "java.net.DatagramSocket",
            "java.net.InetAddress",
            "java.net.http.HttpClient"
        )

        val violations = mutableListOf<String>()

        appMain.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }.forEach { file ->
            val content = file.readText()
            for (pattern in forbiddenNetworkImports) {
                if (content.contains(pattern)) {
                    violations.add("${file.relativeTo(root)} imports network class: '$pattern'")
                }
            }
        }

        assertTrue(
            "CRITICAL SECURITY DEFECT: Network client/socket usage detected in production code:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    // =========================================================================
    // P5-16: Confirm No Analytics
    // =========================================================================

    @Test
    fun p5_16_buildConfiguration_hasZeroAnalyticsLibraries() {
        val root = getProjectRoot()
        val tomlFile = File(root, "gradle/libs.versions.toml")
        val buildGradleFile = File(root, "app/build.gradle.kts")

        val forbiddenAnalyticsKeywords = listOf(
            "firebase-analytics",
            "google-analytics",
            "play-services-analytics",
            "mixpanel",
            "segment",
            "amplitude",
            "flurry",
            "appcenter-analytics",
            "matomo",
            "posthog",
            "countly",
            "umeng",
            "adjust",
            "appsflyer",
            "facebook-core",
            "facebook-android-sdk"
        )

        val tomlContent = tomlFile.readText().lowercase()
        val buildGradleContent = buildGradleFile.readText().lowercase()

        for (kw in forbiddenAnalyticsKeywords) {
            assertFalse(
                "libs.versions.toml contains forbidden analytics library keyword: '$kw'",
                tomlContent.contains(kw)
            )
            assertFalse(
                "app/build.gradle.kts contains forbidden analytics library keyword: '$kw'",
                buildGradleContent.contains(kw)
            )
        }
    }

    @Test
    fun p5_16_codebase_hasZeroAnalyticsTrackingCode() {
        val root = getProjectRoot()
        val appMain = File(root, "app/src/main")

        val forbiddenAnalyticsPatterns = listOf(
            "FirebaseAnalytics",
            "GoogleAnalytics",
            "MixpanelAPI",
            "AmplitudeClient",
            "trackEvent",
            "logEvent",
            "sendEvent",
            "recordEvent",
            "AnalyticsTracker",
            "AppsFlyerLib"
        )

        val violations = mutableListOf<String>()

        appMain.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }.forEach { file ->
            val content = file.readText()
            for (pattern in forbiddenAnalyticsPatterns) {
                if (content.contains(pattern)) {
                    violations.add("${file.relativeTo(root)} contains analytics pattern: '$pattern'")
                }
            }
        }

        assertTrue(
            "CRITICAL PRIVACY DEFECT: Analytics tracking code detected in production source:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    // =========================================================================
    // P5-17: Confirm No Telemetry & Remote Crash Reporters
    // =========================================================================

    @Test
    fun p5_17_buildConfiguration_hasZeroTelemetryOrCrashReporters() {
        val root = getProjectRoot()
        val tomlFile = File(root, "gradle/libs.versions.toml")
        val buildGradleFile = File(root, "app/build.gradle.kts")

        val forbiddenTelemetryKeywords = listOf(
            "crashlytics",
            "sentry",
            "bugsnag",
            "datadog",
            "acra",
            "hockeyapp",
            "rollbar",
            "raygun",
            "instabug",
            "firebase-messaging",
            "onesignal",
            "pusher"
        )

        val tomlContent = tomlFile.readText().lowercase()
        val buildGradleContent = buildGradleFile.readText().lowercase()

        for (kw in forbiddenTelemetryKeywords) {
            assertFalse(
                "libs.versions.toml contains forbidden telemetry/crash reporting keyword: '$kw'",
                tomlContent.contains(kw)
            )
            assertFalse(
                "app/build.gradle.kts contains forbidden telemetry/crash reporting keyword: '$kw'",
                buildGradleContent.contains(kw)
            )
        }
    }

    @Test
    fun p5_17_codebase_hasZeroRemoteLoggingOrCloudSync() {
        val root = getProjectRoot()
        val appMain = File(root, "app/src/main")

        val forbiddenTelemetryPatterns = listOf(
            "Crashlytics",
            "Sentry.captureException",
            "Bugsnag.notify",
            "FirebaseCrashlytics",
            "WorkManager.getInstance",
            "PeriodicWorkRequest",
            "OneTimeWorkRequest",
            "FirebaseFirestore",
            "FirebaseDatabase"
        )

        val violations = mutableListOf<String>()

        appMain.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }.forEach { file ->
            val content = file.readText()
            for (pattern in forbiddenTelemetryPatterns) {
                if (content.contains(pattern)) {
                    violations.add("${file.relativeTo(root)} contains telemetry pattern: '$pattern'")
                }
            }
        }

        assertTrue(
            "CRITICAL PRIVACY DEFECT: Telemetry or remote logging detected in production source:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    // =========================================================================
    // P5-18: Confirm No Arbitrary Shell Execution Interfaces
    // =========================================================================

    @Test
    fun p5_18_codebase_hasZeroShDashCOrShellInterpreters() {
        val root = getProjectRoot()
        val appMain = File(root, "app/src/main")

        val forbiddenShellPatterns = listOf(
            "sh -c",
            "\"-c\"",
            "'/bin/sh'",
            "\"/bin/sh\"",
            "\"/system/bin/sh\"",
            "\"su\"",
            "\"/system/bin/su\"",
            "\"/system/xbin/su\""
        )

        val violations = mutableListOf<String>()

        appMain.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java" || it.extension == "aidl") }.forEach { file ->
            val content = file.readText()
            for (pattern in forbiddenShellPatterns) {
                if (content.contains(pattern)) {
                    violations.add("${file.relativeTo(root)} contains forbidden shell pattern: '$pattern'")
                }
            }
        }

        assertTrue(
            "CRITICAL SECURITY DEFECT: Forbidden shell interpreter patterns detected:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun p5_18_codebase_hasZeroRuntimeExec() {
        val root = getProjectRoot()
        val appMain = File(root, "app/src/main")

        val violations = mutableListOf<String>()

        appMain.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }.forEach { file ->
            val content = file.readText()
            if (content.contains("Runtime.getRuntime().exec") ||
                (content.contains("Runtime.getRuntime()") && content.contains(".exec"))
            ) {
                violations.add("${file.relativeTo(root)} invokes Runtime.getRuntime().exec")
            }
        }

        assertTrue(
            "CRITICAL SECURITY DEFECT: Runtime.getRuntime().exec detected in production source:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun p5_18_processBuilderRestrictedToApprovedCommandClasses() {
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
            "ProcessBuilder must only exist in approved command classes:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun p5_18_aidlInterface_exposesOnlyStronglyTypedDomainMethods() {
        val allowedMethods = setOf(
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

        val methods: Array<Method> = ICacheOpsService::class.java.declaredMethods
        for (method in methods) {
            val name = method.name
            assertTrue(
                "Method '$name' on ICacheOpsService is not in approved typed AIDL whitelist",
                allowedMethods.contains(name)
            )

            // Strictly check parameter types: only primitives or String
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
    fun p5_18_commandBuilder_strictlyEnforcesCacheOnlyAndPackageValidation() {
        // Must throw on plain/unvalidated commands
        assertThrows(IllegalArgumentException::class.java) {
            PackageCommands.buildClearCacheArgs("com.bad package; rm -rf /", 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PackageCommands.buildClearCacheArgs("my.id.rmalan.cache.sweep", 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PackageCommands.buildClearCacheArgs("com.example.app", -1)
        }

        // Valid command MUST contain --cache-only
        val command = PackageCommands.buildClearCacheArgs("com.example.app", 0)
        assertTrue("Command must contain --cache-only flag", command.contains("--cache-only"))
        assertFalse("Command must never contain plain pm clear without --cache-only", command.joinToString(" ") == "pm clear com.example.app")

        // Direct execution of unsafe command must throw IllegalStateException
        assertThrows(IllegalStateException::class.java) {
            PackageCommands.execute(listOf("/system/bin/pm", "clear", "com.example.app"))
        }
    }

    // =========================================================================
    // P5-19: Review Production Logging for Zero Sensitive Info Leakage
    // =========================================================================

    @Test
    fun p5_19_codebase_hasZeroAndroidUtilLogCalls() {
        val root = getProjectRoot()
        val appMain = File(root, "app/src/main")

        val violations = mutableListOf<String>()

        appMain.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }.forEach { file ->
            val content = file.readText()
            if (content.contains("android.util.Log") ||
                content.contains("Log.v(") ||
                content.contains("Log.d(") ||
                content.contains("Log.i(") ||
                content.contains("Log.w(") ||
                content.contains("Log.e(") ||
                content.contains("Log.wtf(")
            ) {
                violations.add("${file.relativeTo(root)} invokes android.util.Log")
            }
        }

        assertTrue(
            "CRITICAL PRIVACY DEFECT: android.util.Log detected in production code:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun p5_19_codebase_hasZeroSystemOutOrErrPrintln() {
        val root = getProjectRoot()
        val appMain = File(root, "app/src/main")

        val violations = mutableListOf<String>()

        appMain.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }.forEach { file ->
            val content = file.readText()
            if (content.contains("System.out.print") ||
                content.contains("System.err.print") ||
                content.contains("println(") ||
                content.contains("print(")
            ) {
                violations.add("${file.relativeTo(root)} contains stdout/stderr print calls")
            }
        }

        assertTrue(
            "CRITICAL PRIVACY DEFECT: stdout/stderr print calls detected in production code:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun p5_19_codebase_hasZeroPrintStackTrace() {
        val root = getProjectRoot()
        val appMain = File(root, "app/src/main")

        val violations = mutableListOf<String>()

        appMain.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "java") }.forEach { file ->
            val content = file.readText()
            if (content.contains(".printStackTrace()")) {
                violations.add("${file.relativeTo(root)} invokes printStackTrace()")
            }
        }

        assertTrue(
            "CRITICAL PRIVACY DEFECT: printStackTrace() detected in production code:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    // =========================================================================
    // P5-20: Review Exported Android Components
    // =========================================================================

    @Test
    fun p5_20_manifest_onlyMainActivityIsExportedActivity() {
        val root = getProjectRoot()
        val manifestFile = File(root, "app/src/main/AndroidManifest.xml")
        val doc = parseManifest(manifestFile)

        val activityNodes = doc.getElementsByTagName("activity")
        for (i in 0 until activityNodes.length) {
            val element = activityNodes.item(i) as Element
            val name = element.getAttribute("android:name")
            val exported = element.getAttribute("android:exported")

            if (name == ".MainActivity" || name == "my.id.rmalan.cache.sweep.MainActivity") {
                assertEquals("MainActivity must be exported for launcher", "true", exported)

                val actionNodes = element.getElementsByTagName("action")
                val categoryNodes = element.getElementsByTagName("category")
                var hasMain = false
                var hasLauncher = false

                for (j in 0 until actionNodes.length) {
                    val action = (actionNodes.item(j) as Element).getAttribute("android:name")
                    if (action == "android.intent.action.MAIN") hasMain = true
                }
                for (j in 0 until categoryNodes.length) {
                    val category = (categoryNodes.item(j) as Element).getAttribute("android:name")
                    if (category == "android.intent.category.LAUNCHER") hasLauncher = true
                }

                assertTrue("MainActivity must have MAIN action", hasMain)
                assertTrue("MainActivity must have LAUNCHER category", hasLauncher)
            } else {
                assertEquals("Non-main activity '$name' must not be exported", "false", exported)
            }
        }
    }

    @Test
    fun p5_20_manifest_hasZeroExportedServices() {
        val root = getProjectRoot()
        val manifestFile = File(root, "app/src/main/AndroidManifest.xml")
        val doc = parseManifest(manifestFile)

        val serviceNodes = doc.getElementsByTagName("service")
        for (i in 0 until serviceNodes.length) {
            val element = serviceNodes.item(i) as Element
            val name = element.getAttribute("android:name")
            val exported = element.getAttribute("android:exported")
            assertFalse("Service '$name' must not be exported", exported == "true")
        }
    }

    @Test
    fun p5_20_manifest_hasZeroExportedBroadcastReceivers() {
        val root = getProjectRoot()
        val manifestFile = File(root, "app/src/main/AndroidManifest.xml")
        val doc = parseManifest(manifestFile)

        val receiverNodes = doc.getElementsByTagName("receiver")
        for (i in 0 until receiverNodes.length) {
            val element = receiverNodes.item(i) as Element
            val name = element.getAttribute("android:name")
            val exported = element.getAttribute("android:exported")
            assertFalse("Broadcast receiver '$name' must not be exported", exported == "true")
        }
    }

    @Test
    fun p5_20_manifest_shizukuProviderIsProperlyProtected() {
        val root = getProjectRoot()
        val manifestFile = File(root, "app/src/main/AndroidManifest.xml")
        val doc = parseManifest(manifestFile)

        val providerNodes = doc.getElementsByTagName("provider")
        assertEquals("Only ShizukuProvider should be declared in main manifest", 1, providerNodes.length)

        val providerElement = providerNodes.item(0) as Element
        val name = providerElement.getAttribute("android:name")
        val permission = providerElement.getAttribute("android:permission")
        val multiprocess = providerElement.getAttribute("android:multiprocess")

        assertEquals("rikka.shizuku.ShizukuProvider", name)
        assertEquals("android.permission.INTERACT_ACROSS_USERS_FULL", permission)
        assertEquals("false", multiprocess)
    }

    @Test
    fun p5_20_manifest_applicationDisablesAdbBackup() {
        val root = getProjectRoot()
        val manifestFile = File(root, "app/src/main/AndroidManifest.xml")
        val doc = parseManifest(manifestFile)

        val appNodes = doc.getElementsByTagName("application")
        assertEquals(1, appNodes.length)
        val appElement = appNodes.item(0) as Element

        val allowBackup = appElement.getAttribute("android:allowBackup")
        assertEquals("allowBackup must be explicitly set to false to prevent adb backup extraction", "false", allowBackup)
    }
}
