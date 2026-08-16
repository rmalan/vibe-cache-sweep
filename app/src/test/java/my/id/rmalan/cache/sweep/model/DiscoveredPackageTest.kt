package my.id.rmalan.cache.sweep.model

import android.os.storage.StorageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveredPackageTest {

    @Test
    fun `discovered package holds expected values with defaults`() {
        val pkg = DiscoveredPackage(
            packageName = "com.example.app",
            appName = "Example App",
            isSystemApp = false
        )

        assertEquals("com.example.app", pkg.packageName)
        assertEquals("Example App", pkg.appName)
        assertFalse(pkg.isSystemApp)
        assertNull(pkg.versionName)
        assertEquals(0L, pkg.versionCode)
        assertEquals(StorageManager.UUID_DEFAULT, pkg.storageUuid)
    }

    @Test
    fun `discovered package holds system app and version data`() {
        val pkg = DiscoveredPackage(
            packageName = "com.android.settings",
            appName = "Settings",
            isSystemApp = true,
            versionName = "14.0",
            versionCode = 34001L
        )

        assertEquals("com.android.settings", pkg.packageName)
        assertEquals("Settings", pkg.appName)
        assertTrue(pkg.isSystemApp)
        assertEquals("14.0", pkg.versionName)
        assertEquals(34001L, pkg.versionCode)
    }
}
