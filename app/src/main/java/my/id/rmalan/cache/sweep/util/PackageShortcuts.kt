package my.id.rmalan.cache.sweep.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object PackageShortcuts {

    /**
     * Creates an Intent to navigate directly to the application details / storage settings for a package.
     */
    fun createStorageSettingsIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Attempts to launch the application details / storage settings screen for a package.
     *
     * @return true if the activity was launched successfully, false otherwise.
     */
    fun openStorageSettings(context: Context, packageName: String): Boolean {
        return try {
            val intent = createStorageSettingsIntent(packageName)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
