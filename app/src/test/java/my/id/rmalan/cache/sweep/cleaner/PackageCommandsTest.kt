package my.id.rmalan.cache.sweep.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageCommandsTest {

    @Test
    fun buildClearCacheArgs_alwaysIncludesCacheOnly() {
        val args = PackageCommands.buildClearCacheArgs("com.example.app", 0)

        assertTrue("Command must contain --cache-only", args.contains("--cache-only"))
        assertTrue("Command must contain clear", args.contains("clear"))
        assertEquals(
            listOf(
                "/system/bin/pm",
                "clear",
                "--user",
                "0",
                "--cache-only",
                "com.example.app"
            ),
            args
        )
    }

    @Test
    fun buildClearCacheArgs_rejectsInvalidPackage() {
        assertThrows(IllegalArgumentException::class.java) {
            PackageCommands.buildClearCacheArgs("invalid..pkg", 0)
        }
    }

    @Test
    fun buildClearCacheArgs_rejectsSelfPackage() {
        assertThrows(IllegalArgumentException::class.java) {
            PackageCommands.buildClearCacheArgs("my.id.rmalan.cache.sweep", 0)
        }
    }

    @Test
    fun buildClearCacheArgs_rejectsNegativeUserId() {
        assertThrows(IllegalArgumentException::class.java) {
            PackageCommands.buildClearCacheArgs("com.example.app", -1)
        }
    }

    @Test
    fun buildTrimCachesArgs_createsCorrectArguments() {
        val args = PackageCommands.buildTrimCachesArgs(104857600L)

        assertEquals(
            listOf(
                "/system/bin/pm",
                "trim-caches",
                "104857600"
            ),
            args
        )
    }

    @Test
    fun buildTrimCachesArgs_rejectsNegativeBytes() {
        assertThrows(IllegalArgumentException::class.java) {
            PackageCommands.buildTrimCachesArgs(-1L)
        }
    }

    @Test
    fun execute_refusesClearWithoutCacheOnly() {
        // Attempting to execute plain pm clear must throw IllegalStateException
        val unsafeArgs = listOf("/system/bin/pm", "clear", "com.example.app")
        assertThrows(IllegalStateException::class.java) {
            PackageCommands.execute(unsafeArgs)
        }
    }
}
