package my.id.rmalan.cache.sweep.permissions

import android.content.Intent
import android.provider.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageAccessManagerTest {

    private class FakeUsageAccessManager(
        var accessGranted: Boolean = false
    ) : UsageAccessManager {
        override fun hasAccess(): Boolean = accessGranted
        override fun createSettingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    @Test
    fun hasAccess_reflectsPermissionState() {
        val manager = FakeUsageAccessManager(accessGranted = false)
        assertFalse(manager.hasAccess())

        manager.accessGranted = true
        assertTrue(manager.hasAccess())
    }

    @Test
    fun createSettingsIntent_returnsNonNullIntent() {
        val manager = FakeUsageAccessManager()
        val intent = manager.createSettingsIntent()
        assertNotNull(intent)
    }
}
