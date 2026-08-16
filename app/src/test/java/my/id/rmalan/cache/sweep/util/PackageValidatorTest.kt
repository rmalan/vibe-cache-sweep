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
    }
}
