package my.id.rmalan.cache.sweep.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageValidatorTest {

    @Test
    fun isValid_validPackages() {
        assertTrue(PackageValidator.isValid("com.example.app"))
        assertTrue(PackageValidator.isValid("org.videolan.vlc"))
        assertTrue(PackageValidator.isValid("com.google.android.youtube"))
        assertTrue(PackageValidator.isValid("a.b"))
    }

    @Test
    fun isValid_invalidPackages() {
        assertFalse(PackageValidator.isValid(null))
        assertFalse(PackageValidator.isValid(""))
        assertFalse(PackageValidator.isValid("   "))
        assertFalse(PackageValidator.isValid("invalid..package"))
        assertFalse(PackageValidator.isValid(".leading.dot"))
        assertFalse(PackageValidator.isValid("trailing.dot."))
        assertFalse(PackageValidator.isValid("123.numeric.start"))
        assertFalse(PackageValidator.isValid("com.example;rm -rf /"))
        assertFalse(PackageValidator.isValid("com.example|calc"))
    }

    @Test
    fun isValid_rejectsSelfPackage() {
        assertFalse(PackageValidator.isValid("my.id.rmalan.cache.sweep"))
        assertFalse(PackageValidator.isValid("my.id.rmalan.cache.sweep.debug"))
        assertTrue(PackageValidator.isSelfPackage("my.id.rmalan.cache.sweep"))
        assertTrue(PackageValidator.isSelfPackage("my.id.rmalan.cache.sweep.test"))
        assertFalse(PackageValidator.isSelfPackage("com.other.app"))
    }

    @Test
    fun isKnownPackage_validation() {
        val known = setOf("com.example.app", "com.google.android.youtube")
        assertTrue(PackageValidator.isKnownPackage("com.example.app", known))
        assertFalse(PackageValidator.isKnownPackage("com.unknown.app", known))
        assertFalse(PackageValidator.isKnownPackage(null, known))
        assertFalse(PackageValidator.isKnownPackage("", known))
    }

    @Test
    fun validatePackage_successAndFailures() {
        val known = setOf("com.example.app", "com.google.android.youtube")

        // Success without known set
        val res1 = PackageValidator.validatePackage("com.example.app")
        assertTrue(res1.isSuccess)
        org.junit.Assert.assertEquals("com.example.app", res1.getOrNull())

        // Success with known set
        val res2 = PackageValidator.validatePackage("com.example.app", known)
        assertTrue(res2.isSuccess)

        // Failure on null/blank
        assertTrue(PackageValidator.validatePackage(null).isFailure)
        assertTrue(PackageValidator.validatePackage("").isFailure)

        // Failure on self package
        val selfRes = PackageValidator.validatePackage("my.id.rmalan.cache.sweep")
        assertTrue(selfRes.isFailure)
        assertTrue(selfRes.exceptionOrNull()?.message?.contains("self-package") == true)

        // Failure on invalid format
        val formatRes = PackageValidator.validatePackage("bad..format")
        assertTrue(formatRes.isFailure)
        assertTrue(formatRes.exceptionOrNull()?.message?.contains("format") == true)

        // Failure on not in known set
        val notKnownRes = PackageValidator.validatePackage("com.other.app", known)
        assertTrue(notKnownRes.isFailure)
        assertTrue(notKnownRes.exceptionOrNull()?.message?.contains("known scanned packages") == true)
    }
}
