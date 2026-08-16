package my.id.rmalan.cache.sweep.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AndroidManifestSecurityAuditTest {

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
    // P2-20: Android Manifest Exported Component & Permission Security Audit
    // =========================================================================

    @Test
    fun appManifest_doesNotRequestInternetPermission() {
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
        }
    }

    @Test
    fun appManifest_requestsOnlyApprovedPermissions() {
        val root = getProjectRoot()
        val manifestFile = File(root, "app/src/main/AndroidManifest.xml")
        val doc = parseManifest(manifestFile)

        val approvedPermissions = setOf(
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.QUERY_ALL_PACKAGES"
        )

        val permissionNodes = doc.getElementsByTagName("uses-permission")
        for (i in 0 until permissionNodes.length) {
            val element = permissionNodes.item(i) as Element
            val permName = element.getAttribute("android:name")
            assertTrue(
                "Permission '$permName' is not in the approved permissions whitelist",
                approvedPermissions.contains(permName)
            )
        }
    }

    @Test
    fun appManifest_applicationDisablesBackup() {
        val root = getProjectRoot()
        val manifestFile = File(root, "app/src/main/AndroidManifest.xml")
        val doc = parseManifest(manifestFile)

        val appNodes = doc.getElementsByTagName("application")
        assertEquals(1, appNodes.length)
        val appElement = appNodes.item(0) as Element

        val allowBackup = appElement.getAttribute("android:allowBackup")
        assertEquals("allowBackup must be explicitly set to false", "false", allowBackup)
    }

    @Test
    fun appManifest_onlyMainActivityIsExportedActivity() {
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

                // Verify launcher intent filter
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
    fun appManifest_noExportedServicesOrReceivers() {
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

        val receiverNodes = doc.getElementsByTagName("receiver")
        for (i in 0 until receiverNodes.length) {
            val element = receiverNodes.item(i) as Element
            val name = element.getAttribute("android:name")
            val exported = element.getAttribute("android:exported")
            assertFalse("Broadcast receiver '$name' must not be exported", exported == "true")
        }
    }

    @Test
    fun appManifest_shizukuProviderIsProperlyProtected() {
        val root = getProjectRoot()
        val manifestFile = File(root, "app/src/main/AndroidManifest.xml")
        val doc = parseManifest(manifestFile)

        val providerNodes = doc.getElementsByTagName("provider")
        assertEquals("Only ShizukuProvider should be declared", 1, providerNodes.length)

        val providerElement = providerNodes.item(0) as Element
        val name = providerElement.getAttribute("android:name")
        val permission = providerElement.getAttribute("android:permission")
        val multiprocess = providerElement.getAttribute("android:multiprocess")

        assertEquals("rikka.shizuku.ShizukuProvider", name)
        assertEquals("android.permission.INTERACT_ACROSS_USERS_FULL", permission)
        assertEquals("false", multiprocess)
    }

    @Test
    fun fixtureManifest_doesNotRequestInternetAndDisablesBackup() {
        val root = getProjectRoot()
        val manifestFile = File(root, "fixture/src/main/AndroidManifest.xml")
        val doc = parseManifest(manifestFile)

        val permissionNodes = doc.getElementsByTagName("uses-permission")
        for (i in 0 until permissionNodes.length) {
            val element = permissionNodes.item(i) as Element
            val permName = element.getAttribute("android:name")
            assertFalse(
                "Fixture must not request INTERNET permission",
                permName.equals("android.permission.INTERNET", ignoreCase = true)
            )
        }

        val appNodes = doc.getElementsByTagName("application")
        val appElement = appNodes.item(0) as Element
        assertEquals("false", appElement.getAttribute("android:allowBackup"))
    }
}
