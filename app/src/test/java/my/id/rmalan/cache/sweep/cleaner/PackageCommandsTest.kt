package my.id.rmalan.cache.sweep.cleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageCommandsTest {

    // =========================================================================
    // P2-18: Command Argument Generation Tests
    // =========================================================================

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
    fun buildClearCacheArgs_withVariousValidPackagesAndUsers() {
        val testCases = listOf(
            Pair("com.google.android.youtube", 0),
            Pair("org.mozilla.firefox", 10),
            Pair("a.b.c", 999),
            Pair("com.example.app", 0),
            Pair("com.test_app.foo_bar", Int.MAX_VALUE)
        )

        for ((pkg, user) in testCases) {
            val args = PackageCommands.buildClearCacheArgs(pkg, user)
            assertEquals(6, args.size)
            assertEquals("/system/bin/pm", args[0])
            assertEquals("clear", args[1])
            assertEquals("--user", args[2])
            assertEquals(user.toString(), args[3])
            assertEquals("--cache-only", args[4])
            assertEquals(pkg, args[5])
        }
    }

    @Test
    fun buildClearCacheArgs_rejectsShellInjectionMetacharacters() {
        val injectionPayloads = listOf(
            "com.example.app; rm -rf /",
            "com.example.app && reboot",
            "com.example.app || echo pwned",
            "com.example.app | cat",
            "com.example.app > /sdcard/leak",
            "com.example.app < /dev/null",
            "$(reboot)",
            "`reboot`",
            "\${PATH}",
            "com.example.app\nrm -rf /",
            "com.example.app\tclear",
            " com.example.app",
            "com.example.app ",
            "com.example.app'--all",
            "com.example.app\"--all"
        )

        for (payload in injectionPayloads) {
            val ex = assertThrows(IllegalArgumentException::class.java) {
                PackageCommands.buildClearCacheArgs(payload, 0)
            }
            assertTrue(ex.message!!.contains("Invalid package name"))
        }
    }

    @Test
    fun buildClearCacheArgs_rejectsDirectoryTraversalAndMalformedPackages() {
        val malformedPackages = listOf(
            "../../data",
            "com.example.app/../../data",
            "standalone",
            "1com.example.app",
            "com..example",
            ".com.example",
            "com.example.",
            "com.example-app",
            "",
            "   "
        )

        for (pkg in malformedPackages) {
            assertThrows(IllegalArgumentException::class.java) {
                PackageCommands.buildClearCacheArgs(pkg, 0)
            }
        }
    }

    @Test
    fun buildClearCacheArgs_rejectsSelfPackage() {
        val selfVariants = listOf(
            "my.id.rmalan.cache.sweep",
            "my.id.rmalan.cache.sweep.ui",
            "my.id.rmalan.cache.sweep.debug"
        )

        for (selfPkg in selfVariants) {
            assertThrows(IllegalArgumentException::class.java) {
                PackageCommands.buildClearCacheArgs(selfPkg, 0)
            }
        }
    }

    @Test
    fun buildClearCacheArgs_rejectsNegativeUserId() {
        val negativeUserIds = listOf(-1, -10, -999, Int.MIN_VALUE)

        for (userId in negativeUserIds) {
            assertThrows(IllegalArgumentException::class.java) {
                PackageCommands.buildClearCacheArgs("com.example.app", userId)
            }
        }
    }

    @Test
    fun buildTrimCachesArgs_createsCorrectArguments() {
        val testBytes = listOf(0L, 1L, 104857600L, 5368709120L, Long.MAX_VALUE)

        for (bytes in testBytes) {
            val args = PackageCommands.buildTrimCachesArgs(bytes)
            assertEquals(3, args.size)
            assertEquals("/system/bin/pm", args[0])
            assertEquals("trim-caches", args[1])
            assertEquals(bytes.toString(), args[2])
        }
    }

    @Test
    fun buildTrimCachesArgs_rejectsNegativeBytes() {
        val negativeBytes = listOf(-1L, -1024L, -5000000L, Long.MIN_VALUE)

        for (bytes in negativeBytes) {
            assertThrows(IllegalArgumentException::class.java) {
                PackageCommands.buildTrimCachesArgs(bytes)
            }
        }
    }

    // =========================================================================
    // P2-19: Invariant Tests: Plain 'pm clear PACKAGE' Cannot Be Generated/Executed
    // =========================================================================

    @Test
    fun commandBuilder_neverCreatesPlainPmClear() {
        val command = PackageCommands
            .buildClearCacheArgs("com.example.test", 0)
            .joinToString(" ")

        assertNotEquals("pm clear com.example.test", command)
        assertNotEquals("/system/bin/pm clear com.example.test", command)
        assertNotEquals("/system/bin/pm clear --user 0 com.example.test", command)
        assertTrue(command.contains("--cache-only"))
    }

    @Test
    fun execute_refusesEmptyOrBlankArguments() {
        assertThrows(IllegalArgumentException::class.java) {
            PackageCommands.execute(emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            PackageCommands.execute(listOf(""))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PackageCommands.execute(listOf("   "))
        }
    }

    @Test
    fun execute_refusesClearWithoutCacheOnly_inAnyForm() {
        val unsafeVariations = listOf(
            listOf("/system/bin/pm", "clear", "com.example.app"),
            listOf("pm", "clear", "com.example.app"),
            listOf("clear", "com.example.app"),
            listOf("/system/bin/pm", "clear", "--user", "0", "com.example.app"),
            listOf("cmd", "package", "clear", "com.example.app"),
            listOf("pm clear", "com.example.app"),
            listOf("/system/bin/pm", "clear --all", "com.example.app")
        )

        for (unsafeArgs in unsafeVariations) {
            val ex = assertThrows(IllegalStateException::class.java) {
                PackageCommands.execute(unsafeArgs)
            }
            assertTrue(
                "Exception message must identify critical defect, got: ${ex.message}",
                ex.message!!.startsWith("CRITICAL DEFECT")
            )
        }
    }
}
