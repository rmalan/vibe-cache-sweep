package my.id.rmalan.cache.sweep.ui

import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import my.id.rmalan.cache.sweep.ui.theme.DarkColorScheme
import my.id.rmalan.cache.sweep.ui.theme.LightColorScheme
import my.id.rmalan.cache.sweep.ui.theme.Typography
import my.id.rmalan.cache.sweep.util.ByteFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeAndAccessibilityTest {

    @Test
    fun `formatAccessible formats bytes into human and TalkBack readable units`() {
        assertEquals("0 bytes", ByteFormatter.formatAccessible(0L))
        assertEquals("0 bytes", ByteFormatter.formatAccessible(-100L))
        assertEquals("1 byte", ByteFormatter.formatAccessible(1L))
        assertEquals("500 bytes", ByteFormatter.formatAccessible(500L))
        assertEquals("1.0 kilobytes", ByteFormatter.formatAccessible(1024L))
        assertEquals("1.5 megabytes", ByteFormatter.formatAccessible((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.40 gigabytes", ByteFormatter.formatAccessible((2.4 * 1024 * 1024 * 1024).toLong()))
        assertEquals("3.14 terabytes", ByteFormatter.formatAccessible((3.14 * 1024L * 1024L * 1024L * 1024L).toLong()))
    }

    @Test
    fun `formatAccessible appends custom suffix correctly`() {
        assertEquals("0 bytes of cache", ByteFormatter.formatAccessible(0L, "of cache"))
        assertEquals("1.42 gigabytes of cache", ByteFormatter.formatAccessible((1.42 * 1024 * 1024 * 1024).toLong(), "of cache"))
        assertEquals("250.0 megabytes cleaned", ByteFormatter.formatAccessible((250 * 1024 * 1024).toLong(), "cleaned"))
    }

    @Test
    fun `theme colors provide adequate dark and light mode differentiation`() {
        // Light color scheme checks
        assertNotEquals(LightColorScheme.primary, LightColorScheme.onPrimary)
        assertNotEquals(LightColorScheme.background, LightColorScheme.onBackground)
        assertNotEquals(LightColorScheme.surface, LightColorScheme.onSurface)

        // Dark color scheme checks
        assertNotEquals(DarkColorScheme.primary, DarkColorScheme.onPrimary)
        assertNotEquals(DarkColorScheme.background, DarkColorScheme.onBackground)
        assertNotEquals(DarkColorScheme.surface, DarkColorScheme.onSurface)

        // Light background should be very light, Dark background very dark
        assertTrue(LightColorScheme.background.red > 0.8f)
        assertTrue(LightColorScheme.background.green > 0.8f)
        assertTrue(LightColorScheme.background.blue > 0.8f)

        assertTrue(DarkColorScheme.background.red < 0.2f)
        assertTrue(DarkColorScheme.background.green < 0.2f)
        assertTrue(DarkColorScheme.background.blue < 0.2f)
    }

    @Test
    fun `neobrutalism theme tokens provide high-contrast solid outlines and accents`() {
        // Light mode outlines must be dark/black
        assertTrue(LightColorScheme.outline.alpha > 0.9f)
        assertTrue(LightColorScheme.outline.red < 0.3f)
        assertTrue(LightColorScheme.outline.green < 0.3f)
        assertTrue(LightColorScheme.outline.blue < 0.3f)

        // Dark mode outlines must be crisp/light
        assertTrue(DarkColorScheme.outline.alpha > 0.8f)
        assertTrue(DarkColorScheme.outline.red > 0.7f)
        assertTrue(DarkColorScheme.outline.green > 0.7f)
        assertTrue(DarkColorScheme.outline.blue > 0.7f)
    }

    @Test
    fun `typography definitions use SP units for dynamic font scaling`() {
        assertEquals(TextUnitType.Sp, Typography.displayLarge.fontSize.type)
        assertEquals(TextUnitType.Sp, Typography.headlineMedium.fontSize.type)
        assertEquals(TextUnitType.Sp, Typography.titleLarge.fontSize.type)
        assertEquals(TextUnitType.Sp, Typography.bodyLarge.fontSize.type)
        assertEquals(TextUnitType.Sp, Typography.bodyMedium.fontSize.type)
        assertEquals(TextUnitType.Sp, Typography.bodySmall.fontSize.type)
        assertEquals(TextUnitType.Sp, Typography.labelSmall.fontSize.type)

        assertTrue(Typography.displayLarge.fontSize >= 50.sp)
        assertTrue(Typography.bodyLarge.fontSize >= 16.sp)
        assertTrue(Typography.bodySmall.fontSize >= 12.sp)
    }
}
