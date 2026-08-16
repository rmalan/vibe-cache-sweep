package my.id.rmalan.cache.sweep.util

import org.junit.Assert.assertNotNull
import org.junit.Test

class PackageShortcutsTest {

    @Test
    fun testCreateStorageSettingsIntent() {
        val packageName = "com.example.testapp"
        val intent = PackageShortcuts.createStorageSettingsIntent(packageName)

        assertNotNull(intent)
    }
}
