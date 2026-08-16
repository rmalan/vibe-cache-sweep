package my.id.rmalan.cache.sweep.shizuku

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityProbeTest {

    @Test
    fun parseCapabilities_withCacheOnlyAndTrimCaches() {
        val helpOutput = """
            Package manager (package) commands:
              help
                Print this help text.
              clear [--user USER_ID] [--cache-only] PACKAGE
                Deletes all data associated with a package.
              trim-caches DESIRED_FREE_SPACE [FLAGS]
                Trim cache files to reach the given free space.
        """.trimIndent()

        val capabilities = CapabilityProbe.parseCapabilities(helpOutput)
        assertTrue(capabilities.supportsSelectiveCacheClear)
        assertTrue(capabilities.supportsTrimCaches)
    }

    @Test
    fun parseCapabilities_withoutCacheOnly() {
        val helpOutput = """
            Package manager (package) commands:
              help
                Print this help text.
              clear [--user USER_ID] PACKAGE
                Deletes all data associated with a package.
              trim-caches DESIRED_FREE_SPACE [FLAGS]
                Trim cache files to reach the given free space.
        """.trimIndent()

        val capabilities = CapabilityProbe.parseCapabilities(helpOutput)
        assertFalse(capabilities.supportsSelectiveCacheClear)
        assertTrue(capabilities.supportsTrimCaches)
    }

    @Test
    fun parseCapabilities_emptyString() {
        val capabilities = CapabilityProbe.parseCapabilities("")
        assertFalse(capabilities.supportsSelectiveCacheClear)
        assertFalse(capabilities.supportsTrimCaches)
    }
}
