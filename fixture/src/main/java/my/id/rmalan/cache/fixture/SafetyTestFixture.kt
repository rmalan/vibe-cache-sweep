package my.id.rmalan.cache.fixture

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

data class FixtureStatus(
    val cacheBytes: Long,
    val cacheFilesCount: Int,
    val prefsIntact: Boolean,
    val filesIntact: Boolean,
    val dbIntact: Boolean,
    val isFullyPopulated: Boolean,
    val summary: String
)

class SafetyTestFixture(private val context: Context) {

    companion object {
        const val PREFS_NAME = "safety_fixture_prefs"
        const val PREF_AUTH_TOKEN = "auth_token"
        const val PREF_AUTH_TOKEN_VALUE = "SECRET_PERSISTENT_AUTH_TOKEN_998877"
        const val PREF_THEME = "theme_preference"
        const val PREF_THEME_VALUE = "dark_sapphire"
        const val PREF_COUNTER = "user_counter"
        const val PREF_COUNTER_VALUE = 1337
        const val PREF_TIMESTAMP = "created_timestamp"
        const val PREF_TIMESTAMP_VALUE = 1771122334455L

        const val FILE_NAME = "user_profile.json"
        const val FILE_CONTENT = "{\"userId\": 42, \"role\": \"admin\", \"bio\": \"CacheSweep Safety Invariant Test User\"}"

        const val DB_NAME = "fixture_user_data.db"
        const val DB_VERSION = 1
        const val TABLE_RECORDS = "test_records"

        const val TARGET_CACHE_FILE_COUNT = 4
        const val BYTES_PER_FILE = 5 * 1024 * 1024 // 5 MB each -> 20 MB total
    }

    private class DbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE $TABLE_RECORDS (id INTEGER PRIMARY KEY, key TEXT, value TEXT)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_RECORDS")
            onCreate(db)
        }
    }

    fun populateTestData(): FixtureStatus {
        // 1. Generate Cache files (20MB)
        val cacheDir = context.cacheDir
        val buffer = ByteArray(64 * 1024) { (it % 256).toByte() }

        for (i in 1..TARGET_CACHE_FILE_COUNT) {
            val file = File(cacheDir, "cache_payload_$i.bin")
            file.outputStream().buffered().use { out ->
                var written = 0
                while (written < BYTES_PER_FILE) {
                    val toWrite = minOf(buffer.size, BYTES_PER_FILE - written)
                    out.write(buffer, 0, toWrite)
                    written += toWrite
                }
            }
        }

        // 2. Store SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(PREF_AUTH_TOKEN, PREF_AUTH_TOKEN_VALUE)
            .putString(PREF_THEME, PREF_THEME_VALUE)
            .putInt(PREF_COUNTER, PREF_COUNTER_VALUE)
            .putLong(PREF_TIMESTAMP, PREF_TIMESTAMP_VALUE)
            .commit()

        // 3. Store Persistent File in filesDir
        val dataFile = File(context.filesDir, FILE_NAME)
        dataFile.writeText(FILE_CONTENT)

        // 4. Store SQLite Database rows
        val helper = DbHelper(context)
        val db = helper.writableDatabase
        try {
            db.execSQL("DELETE FROM $TABLE_RECORDS")
            val rows = listOf(
                Pair("session_id", "sess_abc_123_xyz"),
                Pair("encrypted_vault_key", "vault_9876543210"),
                Pair("offline_document", "Important customer purchase agreement record")
            )
            for ((key, value) in rows) {
                val values = ContentValues().apply {
                    put("key", key)
                    put("value", value)
                }
                db.insert(TABLE_RECORDS, null, values)
            }
        } finally {
            db.close()
            helper.close()
        }

        return verifyState()
    }

    fun verifyState(): FixtureStatus {
        // 1. Check Cache
        val cacheDir = context.cacheDir
        val cacheFiles = cacheDir.listFiles()?.filter { it.isFile } ?: emptyList()
        val cacheBytes = cacheFiles.sumOf { it.length() }
        val cacheFilesCount = cacheFiles.size

        // 2. Check SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(PREF_AUTH_TOKEN, null)
        val theme = prefs.getString(PREF_THEME, null)
        val counter = prefs.getInt(PREF_COUNTER, -1)
        val timestamp = prefs.getLong(PREF_TIMESTAMP, -1L)
        val prefsIntact = token == PREF_AUTH_TOKEN_VALUE &&
                theme == PREF_THEME_VALUE &&
                counter == PREF_COUNTER_VALUE &&
                timestamp == PREF_TIMESTAMP_VALUE

        // 3. Check Persistent File
        val dataFile = File(context.filesDir, FILE_NAME)
        val filesIntact = dataFile.exists() && dataFile.readText() == FILE_CONTENT

        // 4. Check SQLite Database
        var dbIntact = false
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (dbFile.exists()) {
                val helper = DbHelper(context)
                val db = helper.readableDatabase
                try {
                    val cursor = db.rawQuery("SELECT key, value FROM $TABLE_RECORDS ORDER BY id ASC", null)
                    cursor.use {
                        val expected = mapOf(
                            "session_id" to "sess_abc_123_xyz",
                            "encrypted_vault_key" to "vault_9876543210",
                            "offline_document" to "Important customer purchase agreement record"
                        )
                        var matchedCount = 0
                        while (it.moveToNext()) {
                            val key = it.getString(0)
                            val value = it.getString(1)
                            if (expected[key] == value) {
                                matchedCount++
                            }
                        }
                        dbIntact = (matchedCount == expected.size)
                    }
                } finally {
                    db.close()
                    helper.close()
                }
            }
        } catch (e: Exception) {
            dbIntact = false
        }

        val isFullyPopulated = cacheBytes > 0 && prefsIntact && filesIntact && dbIntact
        val summary = buildString {
            append("Cache: $cacheFilesCount files ($cacheBytes bytes) | ")
            append("Prefs: ${if (prefsIntact) "INTACT" else "MISSING/CORRUPT"} | ")
            append("Files: ${if (filesIntact) "INTACT" else "MISSING/CORRUPT"} | ")
            append("Database: ${if (dbIntact) "INTACT" else "MISSING/CORRUPT"}")
        }

        return FixtureStatus(
            cacheBytes = cacheBytes,
            cacheFilesCount = cacheFilesCount,
            prefsIntact = prefsIntact,
            filesIntact = filesIntact,
            dbIntact = dbIntact,
            isFullyPopulated = isFullyPopulated,
            summary = summary
        )
    }
}
