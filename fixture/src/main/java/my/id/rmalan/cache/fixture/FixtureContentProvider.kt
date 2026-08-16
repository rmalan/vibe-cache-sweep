package my.id.rmalan.cache.fixture

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

class FixtureContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "my.id.rmalan.cache.fixture.provider"
        const val METHOD_POPULATE = "populate"
        const val METHOD_STATUS = "status"

        const val KEY_CACHE_BYTES = "cache_bytes"
        const val KEY_CACHE_FILES_COUNT = "cache_files_count"
        const val KEY_PREFS_INTACT = "prefs_intact"
        const val KEY_FILES_INTACT = "files_intact"
        const val KEY_DB_INTACT = "db_intact"
        const val KEY_IS_FULLY_POPULATED = "is_fully_populated"
        const val KEY_SUMMARY = "summary"
    }

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val ctx = context ?: return Bundle()
        val fixture = SafetyTestFixture(ctx)

        val status = when (method) {
            METHOD_POPULATE -> fixture.populateTestData()
            METHOD_STATUS -> fixture.verifyState()
            else -> fixture.verifyState()
        }

        return Bundle().apply {
            putLong(KEY_CACHE_BYTES, status.cacheBytes)
            putInt(KEY_CACHE_FILES_COUNT, status.cacheFilesCount)
            putBoolean(KEY_PREFS_INTACT, status.prefsIntact)
            putBoolean(KEY_FILES_INTACT, status.filesIntact)
            putBoolean(KEY_DB_INTACT, status.dbIntact)
            putBoolean(KEY_IS_FULLY_POPULATED, status.isFullyPopulated)
            putString(KEY_SUMMARY, status.summary)
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
